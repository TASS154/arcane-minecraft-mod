package com.arcane.execution;

import com.arcane.ArcaneMod;
import com.arcane.api.form.FormHandler;
import com.arcane.glyph.GlyphHandlerRegistry;
import com.arcane.inscription.CompiledExecutionPlan;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Zonas: o glifo Zona torna QUALQUER forma persistente. A forma e entregue de novo, do mesmo lugar, a cada intervalo,
 * durante alguns segundos, reaplicando os efeitos em quem esta dentro. Trechos com a condicao "Quando a zona acabar"
 * rodam no fim (via {@link SpellEvents}); "Ao entrar" e "Ao sair da zona" rodam so nas criaturas que entraram ou sairam
 * entre um pulso e o outro. Limites: 2 zonas por conjurador (a mais antiga acaba), 64 no servidor,
 * 60 s de duracao, intervalo minimo de 0,5 s. Zonas sao salvas no mundo ({@link Store}) e voltam depois de reiniciar, assim que o conjurador esta presente.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class Zones {
  public static final int MAX_PER_CASTER = 2, MAX_TOTAL = 64;
  public static final double MAX_SECONDS = 60.0, MIN_INTERVAL = 0.5;
  /** Fracao da casca que pode faltar antes de o Dominio quebrar. */
  public static final double DOMAIN_BREAK_FRACTION = 0.10;
  public static final String END_CONDITION = "on_zone_end";
  public static final String ENTER_CONDITION = "on_zone_enter", EXIT_CONDITION = "on_zone_exit";

  public static final class Zone {
    final UUID id;
    /** Quem estava dentro no ultimo pulso (para detectar entrada e saida). */
    final java.util.Set<UUID> inside = new java.util.HashSet<>();
    final ExecutionContext context;
    final Vec3 origin, look, position;
    final long endsAt, interval;
    long nextAt;
    /** Dominio: a zona acaba quando a casca de barreira quebra. {@code shell} = posicoes que eram barreira. */
    final boolean domain;
    java.util.List<net.minecraft.core.BlockPos> shell;
    boolean broken;
    final List<CompiledExecutionPlan.ExecutionSegment> plain = new ArrayList<>();
    final List<CompiledExecutionPlan.ExecutionSegment> enter = new ArrayList<>(), exit = new ArrayList<>();

    Zone(ExecutionContext context, long now, long duration, long interval) {
      this(UUID.randomUUID(), context, now + duration, interval, now + interval);
    }

    Zone(UUID id, ExecutionContext context, long endsAt, long interval, long nextAt) {
      this.id = id;
      this.context = context;
      this.origin = context.origin();
      this.look = context.look();
      this.position = context.position();
      this.endsAt = endsAt;
      this.interval = interval;
      this.nextAt = nextAt;
      this.domain = context.plan().delivery().parameters().getOrDefault("domain", 0.0) > 0.0
          && context.plan().delivery().formId().getPath().equals("barrier");
    }

    public String endKey() { return "zone_end:" + id; }
  }

  private static final List<Zone> ZONES = new ArrayList<>();

  private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

  private Zones() {}

  public static int active() { synchronized (ZONES) { return ZONES.size(); } }

  public static int activeFor(UUID caster) {
    synchronized (ZONES) { return (int) ZONES.stream().filter(z -> z.context.caster().getUUID().equals(caster)).count(); }
  }

  /** Encerra, sem disparar eventos de fim, as zonas de um conjurador (limpeza e testes). */
  public static void clearFor(UUID caster) {
    synchronized (ZONES) {
      ZONES.removeIf(z -> {
        boolean mine = z.context.caster().getUUID().equals(caster);
        if (mine) { SpellEvents.drop(z.endKey()); forget(z); }
        return mine;
      });
    }
  }

  public static void clearAll() {
    synchronized (ZONES) { ZONES.forEach(z -> SpellEvents.drop(z.endKey())); ZONES.clear(); }
    PENDING.clear();
    if (store != null) { store.records.clear(); store.setDirty(); }
  }

  private static boolean hasCondition(CompiledExecutionPlan.ExecutionSegment segment, String path) {
    return segment.condition().map(c -> c.conditionId().getPath().equals(path)).orElse(false);
  }

  public static boolean isEnterSegment(CompiledExecutionPlan.ExecutionSegment segment) { return hasCondition(segment, ENTER_CONDITION); }

  public static boolean isExitSegment(CompiledExecutionPlan.ExecutionSegment segment) { return hasCondition(segment, EXIT_CONDITION); }

  /** Segmentos de zona: nao rodam a cada pulso, so no evento proprio. */
  public static boolean isZoneEventSegment(CompiledExecutionPlan.ExecutionSegment segment) {
    return isEndSegment(segment) || isEnterSegment(segment) || isExitSegment(segment);
  }

  public static boolean isEndSegment(CompiledExecutionPlan.ExecutionSegment segment) {
    return segment.condition().map(c -> c.conditionId().getPath().equals(END_CONDITION)).orElse(false);
  }

  /** A forma e persistente? Invocacoes (lobos, golem) nao: repeti-las criaria um enxame. */
  public static boolean zoneable(CompiledExecutionPlan plan) {
    return !plan.delivery().formId().getPath().startsWith("summon_") && plan.delivery().parameters().getOrDefault("zone_seconds", 0.0) > 0.0;
  }

  /** Chamada ao conjurar, depois da primeira entrega: registra a zona (se a magia tiver o glifo Zona). */
  public static void begin(ExecutionContext context) {
    CompiledExecutionPlan plan = context.plan();
    if (!zoneable(plan)) return;
    long now = context.level().getGameTime();
    double seconds = Math.min(MAX_SECONDS, plan.delivery().parameters().get("zone_seconds"));
    double every = Math.max(MIN_INTERVAL, plan.delivery().parameters().getOrDefault("zone_interval", 1.0));
    Zone zone = new Zone(context, now, Math.round(seconds * 20), Math.max(10, Math.round(every * 20)));
    for (var segment : plan.segments()) {
      classify(zone, segment);
    }
    List<Zone> evicted = new ArrayList<>();
    synchronized (ZONES) {
      UUID caster = context.caster().getUUID();
      while (ZONES.size() >= MAX_TOTAL) evicted.add(ZONES.remove(0));
      List<Zone> mine = ZONES.stream().filter(z -> z.context.caster().getUUID().equals(caster)).toList();
      for (int i = 0; i <= mine.size() - MAX_PER_CASTER; i++) {
        ZONES.remove(mine.get(i));
        evicted.add(mine.get(i));
      }
      ZONES.add(zone);
    }
    evicted.forEach(Zones::finish);
    context.setZone(zone);
    if (zone.domain) context.level().playSound(null, zone.position.x, zone.position.y, zone.position.z, com.arcane.registry.ModSounds.DOMAIN_OPEN.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.0f);
    if (zone.domain && context.resolvedTargets() != null) {
      var shell = new ArrayList<net.minecraft.core.BlockPos>();
      for (var b : context.resolvedTargets().blocks()) if (b.state().is(com.arcane.registry.ModBlocks.ARCANE_BARRIER.get())) shell.add(b.pos());
      zone.shell = shell;
    }
    if (context.resolvedTargets() != null) {
      for (var e : context.resolvedTargets().entities()) zone.inside.add(e.getUUID());
    }
    persist(zone);
  }

  private static void classify(Zone zone, CompiledExecutionPlan.ExecutionSegment segment) {
    if (isEnterSegment(segment)) zone.enter.add(segment);
    else if (isExitSegment(segment)) zone.exit.add(segment);
    else if (!isEndSegment(segment) && !ArmedSpells.isTriggerSegment(segment) && segment.delayTicks() <= 0) zone.plain.add(segment);
  }

  /** Um trecho "Quando a zona acabar": roda uma vez no fim da zona desta conjuracao. */
  public static void attachEnd(ExecutionContext context, CompiledExecutionPlan.ExecutionSegment segment) {
    if (!(context.zone() instanceof Zone zone)) return;
    SpellEvents.once(zone.endKey(), event -> {
      ExecutionContext fresh = fresh(zone, "zone_end");
      deliver(fresh);
      SpellPipeline.INSTANCE.executeSegment(fresh, segment);
    });
  }

  private static ExecutionContext fresh(Zone zone, String type) {
    ExecutionContext old = zone.context;
    ExecutionContext c = new ExecutionContext(old.level(), old.caster(), old.inscription(), old.plan(), old.environment(),
        old.precision(), new ExecutionContext.TriggerEvent(type, Optional.empty(), Optional.empty()));
    c.setGeometryBonus(old.geometryBonus());
    c.setAreaScale(old.areaScale());
    c.setRitual(old.ritualRadius(), old.ritualCenter());
    c.setRitualLevel(old.ritualLevel());
    c.freeze(zone.origin, zone.look, zone.position);
    return c;
  }

  private static void deliver(ExecutionContext context) {
    DeliverySpec spec = DeliverySpec.fromPlan(context.plan().delivery());
    FormHandler form = GlyphHandlerRegistry.getForm(spec.handler()).orElse(null);
    if (form == null) return;
    context.setResolvedTargets(form.deliver(context, spec).targets());
  }

  /** Mantem a casca do Dominio (renova a vida dos blocos) e diz se ela ainda esta inteira (perdeu menos de DOMAIN_BREAK_FRACTION). */
  private static boolean domainHolds(Zone zone, ExecutionContext c) {
    var barrier = com.arcane.registry.ModBlocks.ARCANE_BARRIER.get();
    if (zone.shell == null) { // zona restaurada de um save: recalcula a casca com o que ainda e barreira
      zone.shell = new ArrayList<>();
      DeliverySpec spec = DeliverySpec.fromPlan(c.plan().delivery());
      for (var pos : com.arcane.execution.handlers.BarrierFormHandler.regionOf(c, spec).shell()) {
        if (c.level().getBlockState(pos).is(barrier)) zone.shell.add(pos);
      }
    }
    if (zone.shell.isEmpty()) return false;
    int intact = 0;
    for (var pos : zone.shell) {
      var st = c.level().getBlockState(pos);
      if (!st.is(barrier)) continue;
      intact++;
      if (st.getValue(com.arcane.block.ArcaneBarrierBlock.LIFE) < com.arcane.block.ArcaneBarrierBlock.MAX_LIFE) {
        c.level().setBlock(pos, st.setValue(com.arcane.block.ArcaneBarrierBlock.LIFE, com.arcane.block.ArcaneBarrierBlock.MAX_LIFE), 2);
      }
    }
    return intact >= zone.shell.size() * (1.0 - DOMAIN_BREAK_FRACTION);
  }

  /** A casca some (com faiscas) quando o Dominio acaba, quebrado ou por tempo. */
  private static void shatter(Zone zone) {
    if (!zone.domain || zone.shell == null) return;
    var level = zone.context.level();
    var barrier = com.arcane.registry.ModBlocks.ARCANE_BARRIER.get();
    level.playSound(null, zone.position.x, zone.position.y, zone.position.z, com.arcane.registry.ModSounds.DOMAIN_BREAK.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.3f, 1.0f);
    for (var pos : zone.shell) {
      if (level.getBlockState(pos).is(barrier)) {
        level.sendParticles(com.arcane.registry.ModParticles.ARCANE_SPARK.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.02);
        level.removeBlock(pos, false);
      }
    }
  }

  private static void pulse(Zone zone) {
    ExecutionContext c = fresh(zone, "zone_tick");
    if (zone.domain && !domainHolds(zone, c)) { zone.broken = true; return; }
    deliver(c);
    zoneEvents(zone, c);
    for (var segment : zone.plain) SpellPipeline.INSTANCE.executeSegment(c, segment);
    c.level().sendParticles(SpellElement.of(c.plan()).particle(), zone.position.x, zone.position.y + 0.2, zone.position.z, 4, 0.4, 0.1, 0.4, 0.01);
  }

  /** Compara quem esta dentro agora com o pulso anterior e roda os trechos "Ao entrar" e "Ao sair da zona". */
  private static void zoneEvents(Zone zone, ExecutionContext c) {
    java.util.Set<UUID> now = new java.util.HashSet<>();
    List<net.minecraft.world.entity.Entity> entered = new ArrayList<>();
    if (c.resolvedTargets() != null) {
      for (var e : c.resolvedTargets().entities()) {
        now.add(e.getUUID());
        if (!zone.inside.contains(e.getUUID())) entered.add(e);
      }
    }
    List<net.minecraft.world.entity.Entity> exited = new ArrayList<>();
    for (UUID id : zone.inside) {
      if (now.contains(id)) continue;
      var e = zone.context.level().getEntity(id);
      if (e != null && e.isAlive()) exited.add(e);
    }
    zone.inside.clear();
    zone.inside.addAll(now);
    runOn(zone, "zone_enter", zone.enter, entered);
    runOn(zone, "zone_exit", zone.exit, exited);
  }

  private static void runOn(Zone zone, String type, List<CompiledExecutionPlan.ExecutionSegment> segments, List<net.minecraft.world.entity.Entity> who) {
    if (segments.isEmpty() || who.isEmpty()) return;
    ExecutionContext c = fresh(zone, type);
    c.setResolvedTargets(new TargetList(who, List.of()));
    for (var segment : segments) SpellPipeline.INSTANCE.executeSegment(c, segment);
  }

  private static void finish(Zone zone) {
    forget(zone);
    shatter(zone);
    SpellEvents.emit(new SpellEvents.Event(zone.endKey(), zone.context.level(), zone.position));
  }

  @SubscribeEvent
  public static void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    if (!PENDING.isEmpty() && event.getServer().getTickCount() % 20 == 0) restorePending(event.getServer());
    List<Zone> due = new ArrayList<>();
    List<Zone> ended = new ArrayList<>();
    synchronized (ZONES) {
      if (ZONES.isEmpty()) return;
      for (Zone z : new ArrayList<>(ZONES)) {
        long now = z.context.level().getGameTime();
        if (!z.context.caster().isAlive() || z.context.caster().isRemoved()) {
          ZONES.remove(z);
          SpellEvents.drop(z.endKey());
          forget(z);
        } else if (now >= z.endsAt) {
          ZONES.remove(z);
          ended.add(z);
        } else if (now >= z.nextAt) {
          z.nextAt = now + z.interval;
          due.add(z);
        }
      }
    }
    for (Zone z : due) {
      try {
        pulse(z);
        if (z.broken) {
          synchronized (ZONES) { ZONES.remove(z); }
          ended.add(z);
        }
      } catch (RuntimeException e) {
        LOG.warn("Zona falhou e foi removida", e);
        synchronized (ZONES) { ZONES.remove(z); }
        SpellEvents.drop(z.endKey());
        forget(z);
      }
    }
    ended.forEach(Zones::finish);
  }

  // ------------------------------------------------------------------ persistencia
  /** Zonas salvas no mundo: uma por registro NBT, chaveadas pelo id da zona. */
  public static final class Store extends net.minecraft.world.level.saveddata.SavedData {
    final java.util.Map<UUID, net.minecraft.nbt.CompoundTag> records = new java.util.LinkedHashMap<>();

    public static Store load(net.minecraft.nbt.CompoundTag tag) {
      Store st = new Store();
      for (net.minecraft.nbt.Tag t : tag.getList("zones", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
        net.minecraft.nbt.CompoundTag c = (net.minecraft.nbt.CompoundTag) t;
        if (c.hasUUID("id")) st.records.put(c.getUUID("id"), c);
      }
      return st;
    }

    @Override
    public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag) {
      net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
      list.addAll(records.values());
      tag.put("zones", list);
      return tag;
    }

    public int size() { return records.size(); }
  }

  private static Store store;
  /** Registros lidos do disco que ainda esperam o conjurador estar presente. */
  private static final java.util.Map<UUID, net.minecraft.nbt.CompoundTag> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

  public static int pendingRestore() { return PENDING.size(); }

  public static int savedCount() { return store == null ? 0 : store.size(); }

  /** Simula uma reinicializacao: esquece as zonas vivas e as relê do que foi salvo (usado nos testes). */
  public static void reloadFromStore(net.minecraft.server.MinecraftServer server) {
    synchronized (ZONES) { ZONES.forEach(z -> SpellEvents.drop(z.endKey())); ZONES.clear(); }
    PENDING.clear();
    if (store != null) PENDING.putAll(store.records);
    restorePending(server);
  }

  @SubscribeEvent
  public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
    var overworld = event.getServer().overworld();
    if (overworld == null) return;
    store = overworld.getDataStorage().computeIfAbsent(Store::load, Store::new, "arcane_zones");
    PENDING.clear();
    PENDING.putAll(store.records);
  }

  @SubscribeEvent
  public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
    synchronized (ZONES) { ZONES.clear(); }
    PENDING.clear();
    store = null;
  }

  private static void persist(Zone zone) {
    if (store == null) return;
    var enc = com.arcane.inscription.Inscription.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, zone.context.inscription()).result();
    if (enc.isEmpty()) return;
    net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
    c.putUUID("id", zone.id);
    c.putString("dim", zone.context.level().dimension().location().toString());
    c.putUUID("caster", zone.context.caster().getUUID());
    c.put("insc", enc.get());
    putVec(c, "o", zone.origin); putVec(c, "l", zone.look); putVec(c, "p", zone.position);
    c.putLong("ends", zone.endsAt);
    c.putLong("every", zone.interval);
    c.putDouble("geo", zone.context.geometryBonus());
    c.putDouble("area", zone.context.areaScale());
    c.putInt("rlevel", zone.context.ritualLevel());
    c.putDouble("rradius", zone.context.ritualRadius());
    if (zone.context.ritualCenter() != null) putVec(c, "rc", zone.context.ritualCenter());
    store.records.put(zone.id, c);
    store.setDirty();
  }

  private static void forget(Zone zone) {
    PENDING.remove(zone.id);
    if (store != null && store.records.remove(zone.id) != null) store.setDirty();
  }

  private static void putVec(net.minecraft.nbt.CompoundTag c, String k, Vec3 v) {
    c.putDouble(k + "x", v.x); c.putDouble(k + "y", v.y); c.putDouble(k + "z", v.z);
  }

  private static Vec3 getVec(net.minecraft.nbt.CompoundTag c, String k) {
    return new Vec3(c.getDouble(k + "x"), c.getDouble(k + "y"), c.getDouble(k + "z"));
  }

  /** Devolve ao jogo as zonas salvas cujo conjurador ja esta presente; descarta as que acabaram enquanto o mundo estava fechado. */
  public static void restorePending(net.minecraft.server.MinecraftServer server) {
    if (PENDING.isEmpty()) return;
    for (var entry : new ArrayList<>(PENDING.entrySet())) {
      net.minecraft.nbt.CompoundTag c = entry.getValue();
      var dim = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
          new net.minecraft.resources.ResourceLocation(c.getString("dim")));
      net.minecraft.server.level.ServerLevel level = server.getLevel(dim);
      if (level == null) continue;
      if (level.getGameTime() >= c.getLong("ends")) {
        PENDING.remove(entry.getKey());
        if (store != null && store.records.remove(entry.getKey()) != null) store.setDirty();
        continue;
      }
      UUID casterId = c.getUUID("caster");
      net.minecraft.world.entity.Entity caster = server.getPlayerList().getPlayer(casterId);
      if (caster == null) caster = level.getEntity(casterId);
      if (!(caster instanceof net.minecraft.world.entity.LivingEntity living) || !living.isAlive()) continue;
      var inscription = com.arcane.inscription.Inscription.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, c.get("insc")).result().orElse(null);
      PENDING.remove(entry.getKey());
      if (inscription == null) {
        if (store != null && store.records.remove(entry.getKey()) != null) store.setDirty();
        continue;
      }
      var plan = com.arcane.inscription.CompiledPlanCache.getOrCompile(inscription);
      Vec3 pos = getVec(c, "p");
      var env = com.arcane.ambient.AmbientFieldResolver.resolve(level, net.minecraft.core.BlockPos.containing(pos));
      ExecutionContext ctx = new ExecutionContext(level, living, inscription, plan, env, PrecisionSnapshot.of(inscription),
          ExecutionContext.TriggerEvent.cast());
      ctx.setGeometryBonus(c.getDouble("geo"));
      ctx.setAreaScale(c.getDouble("area"));
      ctx.setRitualLevel(c.getInt("rlevel"));
      ctx.setRitual(c.getDouble("rradius"), c.contains("rcx") ? getVec(c, "rc") : null);
      ctx.freeze(getVec(c, "o"), getVec(c, "l"), pos);
      Zone zone = new Zone(entry.getKey(), ctx, c.getLong("ends"), c.getLong("every"), level.getGameTime() + c.getLong("every"));
      for (var segment : plan.segments()) classify(zone, segment);
      ctx.setZone(zone);
      synchronized (ZONES) { ZONES.add(zone); }
      for (var segment : plan.segments()) {
        if (isEndSegment(segment)) attachEnd(ctx, segment);
      }
    }
  }
}
