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
 * rodam no fim (via {@link SpellEvents}). Limites: 2 zonas por conjurador (a mais antiga acaba), 64 no servidor,
 * 60 s de duracao, intervalo minimo de 0,5 s. Zonas vivem so na memoria (somem ao fechar o mundo).
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class Zones {
  public static final int MAX_PER_CASTER = 2, MAX_TOTAL = 64;
  public static final double MAX_SECONDS = 60.0, MIN_INTERVAL = 0.5;
  public static final String END_CONDITION = "on_zone_end";

  public static final class Zone {
    final UUID id = UUID.randomUUID();
    final ExecutionContext context;
    final Vec3 origin, look, position;
    final long endsAt, interval;
    long nextAt;
    final List<CompiledExecutionPlan.ExecutionSegment> plain = new ArrayList<>();

    Zone(ExecutionContext context, long now, long duration, long interval) {
      this.context = context;
      this.origin = context.origin();
      this.look = context.look();
      this.position = context.position();
      this.endsAt = now + duration;
      this.interval = interval;
      this.nextAt = now + interval;
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
        if (mine) SpellEvents.drop(z.endKey());
        return mine;
      });
    }
  }

  public static void clearAll() {
    synchronized (ZONES) { ZONES.forEach(z -> SpellEvents.drop(z.endKey())); ZONES.clear(); }
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
      if (!isEndSegment(segment) && !ArmedSpells.isTriggerSegment(segment) && segment.delayTicks() <= 0) zone.plain.add(segment);
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

  private static void pulse(Zone zone) {
    ExecutionContext c = fresh(zone, "zone_tick");
    deliver(c);
    for (var segment : zone.plain) SpellPipeline.INSTANCE.executeSegment(c, segment);
    c.level().sendParticles(SpellElement.of(c.plan()).particle(), zone.position.x, zone.position.y + 0.2, zone.position.z, 4, 0.4, 0.1, 0.4, 0.01);
  }

  private static void finish(Zone zone) {
    SpellEvents.emit(new SpellEvents.Event(zone.endKey(), zone.context.level(), zone.position));
  }

  @SubscribeEvent
  public static void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    List<Zone> due = new ArrayList<>();
    List<Zone> ended = new ArrayList<>();
    synchronized (ZONES) {
      if (ZONES.isEmpty()) return;
      for (Zone z : new ArrayList<>(ZONES)) {
        long now = z.context.level().getGameTime();
        if (!z.context.caster().isAlive() || z.context.caster().isRemoved()) {
          ZONES.remove(z);
          SpellEvents.drop(z.endKey());
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
      } catch (RuntimeException e) {
        LOG.warn("Zona falhou e foi removida", e);
        synchronized (ZONES) { ZONES.remove(z); }
        SpellEvents.drop(z.endKey());
      }
    }
    ended.forEach(Zones::finish);
  }
}
