package com.arcane.execution;

import com.arcane.ArcaneMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Marcas que ficam no mundo ou nas criaturas, dos glifos da rodada de 23 nov:
 * <ul>
 *   <li>Marcar Ponto: ate 3 pontos por jogador (nbt persistente do jogador);</li>
 *   <li>Zona de Descanso: quem deu ou levou dano nos ultimos 2 min esta "em combate";</li>
 *   <li>Folego Final: uma vez por dia de jogo, a morte vira 1 de vida;</li>
 *   <li>Eco do Passado: onde os jogadores passaram nos ultimos 10 min (memoria, a cada 2 s);</li>
 *   <li>Trancar: portas, alcapoes, portoes e baus que so o dono (e os convidados dele) abrem; ficam 20x mais duros;</li>
 *   <li>Selar Bancada: a bancada trancada contra quem nao e o dono (nao abre, nao rouba o grimorio, nao estuda);</li>
 *   <li>Batizar: areas com nome, mostrado como titulo a quem entra.</li>
 * </ul>
 * Trancas, selos e nomes sao salvos no mundo ({@link Store}).
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class WorldMarks {
  public static final long COMBAT_TICKS = 2400L, FOOTPRINT_TICKS = 12000L;
  public static final int MAX_WAYPOINTS = 3, MAX_AREAS = 64;

  private WorldMarks() {}

  // ------------------------------------------------------------------ pontos marcados
  private static CompoundTag persisted(Player p) {
    return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
  }

  public static void addWaypoint(ServerPlayer p, BlockPos pos) {
    CompoundTag data = persisted(p);
    ListTag list = data.getList("arcane_waypoints", Tag.TAG_LONG);
    while (list.size() >= MAX_WAYPOINTS) list.remove(0);
    list.add(LongTag.valueOf(pos.asLong()));
    data.put("arcane_waypoints", list);
    p.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
  }

  public static List<BlockPos> waypoints(Player p) {
    List<BlockPos> out = new ArrayList<>();
    for (Tag t : persisted(p).getList("arcane_waypoints", Tag.TAG_LONG)) out.add(BlockPos.of(((LongTag) t).getAsLong()));
    return out;
  }

  // ------------------------------------------------------------------ combate (Zona de Descanso)
  private static final Map<UUID, Long> LAST_FIGHT = new HashMap<>();

  public static boolean inCombat(LivingEntity e) {
    Long t = LAST_FIGHT.get(e.getUUID());
    return t != null && e.level().getGameTime() - t < COMBAT_TICKS;
  }

  public static void markFight(LivingEntity e) { LAST_FIGHT.put(e.getUUID(), e.level().getGameTime()); }

  @SubscribeEvent
  public static void onHurt(LivingHurtEvent event) {
    if (event.getEntity().level().isClientSide) return;
    markFight(event.getEntity());
    if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != event.getEntity()) markFight(attacker);
  }

  // ------------------------------------------------------------------ Folego Final
  private static final String LAST_BREATH = "arcane_last_breath_until", LAST_BREATH_DAY = "arcane_last_breath_day";

  public static void grantLastBreath(LivingEntity e, long until) { e.getPersistentData().putLong(LAST_BREATH, until); }

  public static boolean hasLastBreath(LivingEntity e) { return e.getPersistentData().getLong(LAST_BREATH) > e.level().getGameTime(); }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onDeath(LivingDeathEvent event) {
    LivingEntity e = event.getEntity();
    if (!(e.level() instanceof ServerLevel level) || !hasLastBreath(e)) return;
    long day = level.getDayTime() / 24000L;
    var data = e.getPersistentData();
    data.remove(LAST_BREATH);
    if (data.contains(LAST_BREATH_DAY) && data.getLong(LAST_BREATH_DAY) == day) return; // ja usado hoje
    data.putLong(LAST_BREATH_DAY, day);
    event.setCanceled(true);
    e.setHealth(1.0f);
    e.removeAllEffects();
    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100, 1));
    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 60, 3));
    level.broadcastEntityEvent(e, (byte) 35); // a animacao do totem
    level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 80, 0.6, 1.0, 0.6, 0.25);
    level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
    level.playSound(null, e.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.2f, 0.8f);
    level.playSound(null, e.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, 1.4f);
  }

  // ------------------------------------------------------------------ Eco do Passado
  private record Footprint(Vec3 pos, long at) {}
  private static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, ArrayDeque<Footprint>> STEPS = new HashMap<>();

  public static List<Vec3> footprints(ServerLevel level, Vec3 around, double r) {
    List<Vec3> out = new ArrayList<>();
    var list = STEPS.get(level.dimension());
    if (list == null) return out;
    long now = level.getGameTime();
    for (Footprint f : list) if (now - f.at() < FOOTPRINT_TICKS && f.pos().distanceTo(around) <= r) out.add(f.pos());
    return out;
  }

  /** Registra uma pegada (para os testes e para o tick). */
  public static void step(ServerLevel level, Vec3 pos) {
    var list = STEPS.computeIfAbsent(level.dimension(), k -> new ArrayDeque<>());
    list.add(new Footprint(pos, level.getGameTime()));
    while (list.size() > 6000 || (!list.isEmpty() && level.getGameTime() - list.peekFirst().at() > FOOTPRINT_TICKS)) list.pollFirst();
  }

  // ------------------------------------------------------------------ trancas, selos e areas (salvos)
  public static final class Store extends SavedData {
    final Map<String, UUID> locks = new HashMap<>();
    final Map<String, UUID> seals = new HashMap<>();
    final List<Area> areas = new ArrayList<>();

    static Store load(CompoundTag tag) {
      Store s = new Store();
      for (Tag t : tag.getList("locks", Tag.TAG_COMPOUND)) s.locks.put(((CompoundTag) t).getString("k"), ((CompoundTag) t).getUUID("o"));
      for (Tag t : tag.getList("seals", Tag.TAG_COMPOUND)) s.seals.put(((CompoundTag) t).getString("k"), ((CompoundTag) t).getUUID("o"));
      for (Tag t : tag.getList("areas", Tag.TAG_COMPOUND)) {
        CompoundTag c = (CompoundTag) t;
        s.areas.add(new Area(c.getString("dim"), new Vec3(c.getDouble("x"), c.getDouble("y"), c.getDouble("z")), c.getDouble("r"), c.getString("name"), c.getUUID("o")));
      }
      return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
      ListTag l = new ListTag(), s = new ListTag(), a = new ListTag();
      locks.forEach((k, o) -> { CompoundTag c = new CompoundTag(); c.putString("k", k); c.putUUID("o", o); l.add(c); });
      seals.forEach((k, o) -> { CompoundTag c = new CompoundTag(); c.putString("k", k); c.putUUID("o", o); s.add(c); });
      for (Area ar : areas) {
        CompoundTag c = new CompoundTag();
        c.putString("dim", ar.dim()); c.putDouble("x", ar.centre().x); c.putDouble("y", ar.centre().y); c.putDouble("z", ar.centre().z);
        c.putDouble("r", ar.radius()); c.putString("name", ar.name()); c.putUUID("o", ar.owner());
        a.add(c);
      }
      tag.put("locks", l); tag.put("seals", s); tag.put("areas", a);
      return tag;
    }
  }

  public record Area(String dim, Vec3 centre, double radius, String name, UUID owner) {}

  private static Store store;

  public static Store store(MinecraftServer server) {
    if (store == null) store = server.overworld().getDataStorage().computeIfAbsent(Store::load, Store::new, "arcane_world_marks");
    return store;
  }

  private static String key(ServerLevel level, BlockPos pos) { return level.dimension().location() + "@" + pos.asLong(); }

  /** Tranca de uma porta/bau: so o dono e os convidados dele abrem. Portas duplas trancam as duas metades. */
  public static void lock(ServerLevel level, BlockPos pos, UUID owner) {
    store(level.getServer()).locks.put(key(level, pos), owner);
    var state = level.getBlockState(pos);
    if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)) {
      boolean lower = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
          == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER;
      store(level.getServer()).locks.put(key(level, lower ? pos.above() : pos.below()), owner);
    }
    store.setDirty();
  }

  public static UUID lockOwner(ServerLevel level, BlockPos pos) { return store(level.getServer()).locks.get(key(level, pos)); }

  public static boolean mayOpen(ServerLevel level, BlockPos pos, Player who) {
    UUID owner = lockOwner(level, pos);
    if (owner == null || owner.equals(who.getUUID()) || who.isCreative()) return true;
    return GuestLists.get(level.getServer()).of(owner).contains(who.getUUID());
  }

  public static void sealBench(ServerLevel level, BlockPos pos, UUID owner) {
    store(level.getServer()).seals.put(key(level, pos), owner);
    store.setDirty();
  }

  public static UUID benchSeal(ServerLevel level, BlockPos pos) { return store(level.getServer()).seals.get(key(level, pos)); }

  /** Dissipar sobre um bloco tirando tranca ou selo; o dono tira de graca, os outros precisam de forca (mana) 30+. */
  public static boolean unseal(ServerLevel level, BlockPos pos, UUID by, double spellMana) {
    Store s = store(level.getServer());
    String k = key(level, pos);
    boolean done = false;
    for (Map<String, UUID> m : List.of(s.locks, s.seals)) {
      UUID o = m.get(k);
      if (o != null && (o.equals(by) || spellMana >= 30)) { m.remove(k); done = true; }
    }
    if (done) s.setDirty();
    return done;
  }

  /** Batizar: nomeia a area (o nome e o nome da propria magia). Cada mago tem ate 5 areas; o mundo, ate 64. */
  public static void baptize(ServerLevel level, Vec3 centre, double radius, String name, UUID owner) {
    Store s = store(level.getServer());
    List<Area> mine = s.areas.stream().filter(a -> a.owner().equals(owner)).toList();
    if (mine.size() >= 5) s.areas.remove(mine.get(0));
    while (s.areas.size() >= MAX_AREAS) s.areas.remove(0);
    s.areas.add(new Area(level.dimension().location().toString(), centre, radius, name, owner));
    s.setDirty();
  }

  public static Area areaAt(ServerLevel level, Vec3 pos) {
    String dim = level.dimension().location().toString();
    Area best = null;
    for (Area a : store(level.getServer()).areas) {
      if (!a.dim().equals(dim) || a.centre().distanceTo(pos) > a.radius()) continue;
      if (best == null || a.radius() < best.radius()) best = a; // a menor area que contem o ponto vence
    }
    return best;
  }

  private static final Map<UUID, String> CURRENT_AREA = new HashMap<>();

  @SubscribeEvent
  public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
    ServerLevel level = p.serverLevel();
    if (p.tickCount % 40 == 0 && !p.isSpectator()) step(level, p.position());
    if (p.tickCount % 20 != 5) return;
    Area a = areaAt(level, p.position());
    String now = a == null ? "" : a.name() + "|" + a.centre();
    String before = CURRENT_AREA.put(p.getUUID(), now);
    if (a != null && !now.equals(before)) {
      p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
      p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.arcane.baptize.subtitle").withStyle(ChatFormatting.GRAY)));
      p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(a.name()).withStyle(ChatFormatting.GOLD)));
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
    if (!(event.getLevel() instanceof ServerLevel level)) return;
    if (!mayOpen(level, event.getPos(), event.getEntity())) {
      event.setCanceled(true);
      event.getEntity().displayClientMessage(Component.translatable("message.arcane.lock.locked").withStyle(ChatFormatting.RED), true);
      level.playSound(null, event.getPos(), SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1f, 1f);
    }
  }

  @SubscribeEvent
  public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
    var pos = event.getPosition().orElse(null);
    if (pos == null || !(event.getEntity().level() instanceof ServerLevel level)) return;
    if (!mayOpen(level, pos, event.getEntity())) event.setNewSpeed(event.getNewSpeed() / 20f);
  }

  // ------------------------------------------------------------------ Ilusao: criaturas nao sabem quem e o real
  @SubscribeEvent
  public static void onChangeTarget(net.minecraftforge.event.entity.living.LivingChangeTargetEvent event) {
    if (!(event.getNewTarget() instanceof Player owner) || event.getEntity().level().isClientSide) return;
    LivingEntity pick = com.arcane.entity.IllusionEntity.confuse(owner, event.getEntity().getRandom());
    if (pick != owner) event.setNewTarget(pick);
  }

  // ------------------------------------------------------------------ Passo Surdo
  private static final String SILENCED = "arcane_silent_step";

  @SubscribeEvent
  public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
    LivingEntity e = event.getEntity();
    if (e.level().isClientSide || e.tickCount % 5 != 0) return;
    boolean on = e.hasEffect(com.arcane.registry.ModEffects.SILENT_STEP.get());
    if (on && !e.isSilent()) { e.setSilent(true); e.getPersistentData().putBoolean(SILENCED, true); }
    else if (!on && e.getPersistentData().getBoolean(SILENCED)) { e.setSilent(false); e.getPersistentData().remove(SILENCED); }
  }

  /** Passo Surdo tambem apaga as vibracoes de movimento (o Warden e os sensores sculk nao percebem). */
  @SubscribeEvent
  public static void onVibration(net.minecraftforge.event.VanillaGameEvent event) {
    if (!(event.getCause() instanceof LivingEntity e) || !e.hasEffect(com.arcane.registry.ModEffects.SILENT_STEP.get())) return;
    var ev = event.getVanillaEvent();
    if (ev == net.minecraft.world.level.gameevent.GameEvent.STEP || ev == net.minecraft.world.level.gameevent.GameEvent.HIT_GROUND
        || ev == net.minecraft.world.level.gameevent.GameEvent.SWIM || ev == net.minecraft.world.level.gameevent.GameEvent.SPLASH
        || ev == net.minecraft.world.level.gameevent.GameEvent.ELYTRA_GLIDE || ev == net.minecraft.world.level.gameevent.GameEvent.FLAP) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onStopped(ServerStoppedEvent event) {
    store = null;
    LAST_FIGHT.clear();
    STEPS.clear();
    CURRENT_AREA.clear();
  }
}
