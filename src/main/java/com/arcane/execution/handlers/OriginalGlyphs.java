package com.arcane.execution.handlers;

import com.arcane.ArcaneMod;
import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Os 7 glifos originais, guardados pelas Hidras (um por covil). Nao aparecem em baus: so quem derrota a Hidra aprende.
 * Todos agem em volta do "centro" do alvo (o ponto, bloco ou criatura que a forma achou; sem alvo, o conjurador).
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class OriginalGlyphs {
  private OriginalGlyphs() {}

  static Vec3 centre(ExecutionContext ctx, TargetList t) {
    if (!t.points().isEmpty()) return t.points().get(0);
    if (!t.blocks().isEmpty()) return Vec3.atCenterOf(t.blocks().get(0).pos());
    if (!t.entities().isEmpty()) return t.entities().get(0).position();
    return ctx.caster().position();
  }

  static double radius(ExecutionContext ctx, EffectCall call, double def) {
    return Math.min(96.0, ctx.amplify(call.parameters().getOrDefault("radius", def), call.magnitudeKeys(), "radius"));
  }

  /** Visual proprio do glifo original (ver OriginalFxRenderer). */
  static void fx(ServerLevel level, int kind, Vec3 pos, Vec3 dir, double radius, int ticks) {
    com.arcane.network.ModNetwork.sendShape(level, com.arcane.network.packet.S2CShapeFxPacket.ORIGINAL, pos, dir, (float) radius, 0xFFFFFF, kind, ticks);
  }

  static List<LivingEntity> living(ServerLevel level, Vec3 c, double r, Entity except) {
    return level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
        e -> e != except && e.isAlive() && e.position().distanceToSqr(c) <= r * r && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
  }

  // ================================================================== Tempo: Parar o Tempo
  private record Frozen(Vec3 pos, Vec3 motion, long until, boolean hadNoAi, boolean hadNoGravity) {}
  private static final Map<Entity, Frozen> FROZEN = new HashMap<>();

  /** Para o tempo numa esfera: tudo (criaturas, projeteis, blocos caindo) congela, menos o conjurador. */
  public static final class TimeStop implements EffectHandler {
    @Override
    public void apply(ExecutionContext ctx, EffectCall call, TargetList targets) {
      ServerLevel level = ctx.level();
      Vec3 c = centre(ctx, targets);
      double r = radius(ctx, call, 16);
      int ticks = (int) Math.min(1200, ctx.amplify(call.parameters().getOrDefault("duration", 100.0), call.magnitudeKeys(), "duration"));
      long until = level.getGameTime() + ticks;
      for (Entity e : level.getEntities((Entity) null, new AABB(c, c).inflate(r), e -> e != ctx.caster() && e.position().distanceToSqr(c) <= r * r)) {
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
        boolean noAi = e instanceof Mob m && m.isNoAi();
        FROZEN.putIfAbsent(e, new Frozen(e.position(), e.getDeltaMovement(), until, noAi, e.isNoGravity()));
        if (e instanceof Mob m) m.setNoAi(true);
        e.setNoGravity(true);
      }
      fx(level, com.arcane.network.packet.S2CShapeFxPacket.FX_TIME_STOP, c, Vec3.ZERO, r, ticks);
      level.playSound(null, c.x, c.y, c.z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 3f, 0.5f);
    }
  }

  public static boolean isFrozen(Entity e) { return FROZEN.containsKey(e); }

  // ================================================================== Tempo: Retroceder
  /** Fotos de cada criatura conjuradora, uma por segundo, dos ultimos 10 s. */
  private record Snapshot(Vec3 pos, float health, int food) {}
  private static final Map<UUID, ArrayDeque<Snapshot>> HISTORY = new HashMap<>();

  /** Volta o alvo (padrao: o conjurador) a onde estava, com a vida que tinha, ha N segundos (ate 10). Apaga fogo e males. */
  public static final class Rewind implements EffectHandler {
    @Override
    public void apply(ExecutionContext ctx, EffectCall call, TargetList targets) {
      int seconds = (int) Math.max(1, Math.min(10, Math.round(call.parameters().getOrDefault("seconds", 10.0))));
      List<Entity> who = targets.entities().isEmpty() ? List.of(ctx.caster()) : targets.entities();
      for (Entity e : who) {
        if (!(e instanceof LivingEntity le)) continue;
        ArrayDeque<Snapshot> h = HISTORY.get(le.getUUID());
        if (h == null || h.isEmpty()) continue;
        Snapshot s = null;
        int k = 0;
        for (Iterator<Snapshot> it = h.descendingIterator(); it.hasNext() && k < seconds; k++) s = it.next();
        if (s == null) continue;
        fx(ctx.level(), com.arcane.network.packet.S2CShapeFxPacket.FX_REWIND, s.pos, le.position(), 1, 30);
        le.teleportTo(s.pos.x, s.pos.y, s.pos.z);
        le.setHealth(Math.max(le.getHealth(), s.health));
        le.clearFire();
        le.fallDistance = 0;
        le.getActiveEffects().stream().filter(i -> !i.getEffect().isBeneficial()).map(MobEffectInstance::getEffect).toList().forEach(le::removeEffect);
        if (le instanceof Player p) p.getFoodData().setFoodLevel(Math.max(p.getFoodData().getFoodLevel(), s.food));
        ctx.level().sendParticles(ParticleTypes.END_ROD, s.pos.x, s.pos.y + 1, s.pos.z, 40, 0.4, 0.8, 0.4, 0.05);
        ctx.level().playSound(null, s.pos.x, s.pos.y, s.pos.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, 0.6f);
      }
    }
  }

  // ================================================================== Espaco: Colapso Espacial
  /** Dobra o espaco: todas as criaturas no raio sao arrancadas e esmagadas no ponto alvo. */
  public static final class SpaceCollapse implements EffectHandler {
    @Override
    public void apply(ExecutionContext ctx, EffectCall call, TargetList targets) {
      ServerLevel level = ctx.level();
      Vec3 c = centre(ctx, targets);
      double r = radius(ctx, call, 24);
      float dmg = (float) ctx.amplify(call.parameters().getOrDefault("damage", 14.0), call.magnitudeKeys(), "damage");
      for (LivingEntity e : living(level, c, r, ctx.caster())) {
        level.sendParticles(ParticleTypes.PORTAL, e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 0.6, 0.3, 0.5);
        e.teleportTo(c.x + (level.random.nextDouble() - 0.5), c.y, c.z + (level.random.nextDouble() - 0.5));
        e.hurt(ctx.level().damageSources().indirectMagic(ctx.caster(), ctx.caster()), dmg);
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3), ctx.caster());
      }
      fx(level, com.arcane.network.packet.S2CShapeFxPacket.FX_SPACE_COLLAPSE, c, Vec3.ZERO, r, 44);
      level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3f, 0.4f);
    }
  }

  // ================================================================== Destruicao: Aniquilacao
  /** Apaga da existencia uma esfera de blocos (sem soltar nada) e fere tudo dentro. Rocha-mae e baus ficam. */
  public static final class Annihilate implements EffectHandler {
    @Override
    public void apply(ExecutionContext ctx, EffectCall call, TargetList targets) {
      ServerLevel level = ctx.level();
      Vec3 c = centre(ctx, targets);
      double r = Math.min(48, radius(ctx, call, 10));
      float dmg = (float) ctx.amplify(call.parameters().getOrDefault("damage", 30.0), call.magnitudeKeys(), "damage");
      for (LivingEntity e : living(level, c, r, ctx.caster())) e.hurt(level.damageSources().indirectMagic(ctx.caster(), ctx.caster()), dmg);
      if (Targeting.mayBreakBlocks(ctx)) {
        var list = new it.unimi.dsi.fastutil.longs.LongArrayList();
        int n = (int) Math.ceil(r);
        BlockPos base = BlockPos.containing(c);
        for (int dx = -n; dx <= n; dx++) for (int dy = -n; dy <= n; dy++) for (int dz = -n; dz <= n; dz++) {
          if (dx * dx + dy * dy + dz * dz > r * r) continue;
          BlockPos p = base.offset(dx, dy, dz);
          var st = level.getBlockState(p);
          if (st.isAir() || st.getDestroySpeed(level, p) < 0 || st.hasBlockEntity()) continue;
          list.add(p.asLong());
        }
        com.arcane.execution.CraterQueue.submit(level, list);
      }
      fx(level, com.arcane.network.packet.S2CShapeFxPacket.FX_ANNIHILATE, c, Vec3.ZERO, r, 40);
      level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 6f, 0.3f);
    }
  }

  // ================================================================== Ceu: Eclipse
  /** Faz noite e tempestade na hora e raios caem sobre os monstros em volta (ate 12). */
  public static final class Eclipse implements EffectHandler {
    @Override
    public void apply(ExecutionContext ctx, EffectCall call, TargetList targets) {
      ServerLevel level = ctx.level();
      Vec3 c = centre(ctx, targets);
      double r = radius(ctx, call, 64);
      if (Targeting.mayAlterWorld(ctx)) {
        long day = level.getDayTime() / 24000L;
        level.setDayTime(day * 24000L + 18000L);
        level.setWeatherParameters(0, 2400, true, true);
      }
      int bolts = 0;
      for (LivingEntity e : living(level, c, r, ctx.caster())) {
        if (!(e instanceof Enemy) || bolts >= 12) continue;
        var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) continue;
        bolt.moveTo(e.getX(), e.getY(), e.getZ());
        if (ctx.caster() instanceof ServerPlayer sp) bolt.setCause(sp);
        level.addFreshEntity(bolt);
        bolts++;
      }
      fx(level, com.arcane.network.packet.S2CShapeFxPacket.FX_ECLIPSE, c, Vec3.ZERO, r, 240);
      level.playSound(null, c.x, c.y, c.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2f, 1.4f);
    }
  }

  // ================================================================== Vida: Genese
  /** Cura total e limpa os males de todos os jogadores e criaturas pacificas no raio, e faz tudo em volta florescer. */
  public static final class Genesis implements EffectHandler {
    @Override
    public void apply(ExecutionContext ctx, EffectCall call, TargetList targets) {
      ServerLevel level = ctx.level();
      Vec3 c = centre(ctx, targets);
      double r = radius(ctx, call, 24);
      for (LivingEntity e : living(level, c, r, null)) {
        if (e instanceof Enemy) continue;
        e.setHealth(e.getMaxHealth());
        e.clearFire();
        e.getActiveEffects().stream().filter(i -> !i.getEffect().isBeneficial()).map(MobEffectInstance::getEffect).toList().forEach(e::removeEffect);
        e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 2), ctx.caster());
        e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 3), ctx.caster());
        if (e instanceof Player p) p.getFoodData().eat(20, 1.0f);
        level.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 4, 0.3, 0.2, 0.3, 0);
      }
      var grow = new java.util.HashMap<String, Double>(call.parameters());
      grow.put("radius", Math.min(16.0, r * 0.5));
      grow.put("strength", 4.0);
      new GrowEffectHandler().apply(ctx, new EffectCall(call.glyphId(), call.handler(), grow, List.of()), TargetList.ofPoint(c));
      fx(level, com.arcane.network.packet.S2CShapeFxPacket.FX_GENESIS, c, Vec3.ZERO, r, 70);
      level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y + 1, c.z, 60, r * 0.3, 1.5, r * 0.3, 0.3);
      level.playSound(null, c.x, c.y, c.z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.5f, 1.2f);
    }
  }

  // ================================================================== Forca: Inversao da Gravidade
  /** Inverte a gravidade de tudo no raio: as criaturas sobem com forca e despencam quando o efeito acaba. */
  public static final class GravityInversion implements EffectHandler {
    @Override
    public void apply(ExecutionContext ctx, EffectCall call, TargetList targets) {
      ServerLevel level = ctx.level();
      Vec3 c = centre(ctx, targets);
      double r = radius(ctx, call, 20);
      int ticks = (int) Math.min(600, ctx.amplify(call.parameters().getOrDefault("duration", 80.0), call.magnitudeKeys(), "duration"));
      for (LivingEntity e : living(level, c, r, ctx.caster())) {
        e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ticks, 6), ctx.caster());
        e.hurtMarked = true;
      }
      for (var proj : level.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r))) proj.setDeltaMovement(proj.getDeltaMovement().multiply(1, -1, 1).add(0, 0.5, 0));
      fx(level, com.arcane.network.packet.S2CShapeFxPacket.FX_GRAVITY, c, Vec3.ZERO, r, Math.min(200, ticks));
      level.playSound(null, c.x, c.y, c.z, SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 3f, 0.4f);
    }
  }

  // ================================================================== ticks: congelados e historico
  @SubscribeEvent
  public static void onTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
    if (server == null) return;
    if (!FROZEN.isEmpty()) {
      for (Iterator<Map.Entry<Entity, Frozen>> it = FROZEN.entrySet().iterator(); it.hasNext(); ) {
        var en = it.next();
        Entity e = en.getKey();
        Frozen f = en.getValue();
        if (!e.isAlive() || e.level().getGameTime() >= f.until) {
          if (e instanceof Mob m) m.setNoAi(f.hadNoAi);
          e.setNoGravity(f.hadNoGravity);
          e.setDeltaMovement(f.motion);
          e.hurtMarked = true;
          it.remove();
          continue;
        }
        e.setDeltaMovement(Vec3.ZERO);
        if (e.position().distanceToSqr(f.pos) > 1.0e-4) {
          if (e instanceof ServerPlayer sp) sp.connection.teleport(f.pos.x, f.pos.y, f.pos.z, sp.getYRot(), sp.getXRot());
          else e.setPos(f.pos.x, f.pos.y, f.pos.z);
        }
        e.hurtMarked = true;
      }
    }
    if (server.getTickCount() % 20 == 0) {
      for (ServerPlayer p : server.getPlayerList().getPlayers()) {
        ArrayDeque<Snapshot> h = HISTORY.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        h.addLast(new Snapshot(p.position(), p.getHealth(), p.getFoodData().getFoodLevel()));
        while (h.size() > 10) h.removeFirst();
      }
    }
  }

  /** Para testes: grava uma foto agora (os jogadores sao gravados sozinhos a cada segundo). */
  public static void record(LivingEntity e) {
    ArrayDeque<Snapshot> h = HISTORY.computeIfAbsent(e.getUUID(), k -> new ArrayDeque<>());
    h.addLast(new Snapshot(e.position(), e.getHealth(), e instanceof Player p ? p.getFoodData().getFoodLevel() : 20));
    while (h.size() > 10) h.removeFirst();
  }

  @SubscribeEvent
  public static void onStopped(ServerStoppedEvent event) {
    FROZEN.clear();
    HISTORY.clear();
  }
}
