package com.arcane.execution;

import com.arcane.ArcaneMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico da Fase 4: guarda os efeitos que duram (satelites, rastro, escalada, chamariz, vinculo, aliados reanimados,
 * condutores) e liga os eventos de dano, cura e projetil que os estados de {@link Phase4State} precisam.
 * O custo e proporcional so ao que esta ativo: sem nenhum efeito, os mapas estao vazios e nada roda.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class Phase4Service {
  public static final int MAX_SATELLITES = 4, MAX_TRAIL_POINTS = 24, MAX_MINIONS = 2, MAX_DECOYS = 8, MAX_BOUND = 64;
  private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);

  public record Satellites(ServerLevel level, long until, int count, float damage) {}
  public static final class Trail {
    final ServerLevel level; final long until; final float damage;
    final List<Vec3> points = new ArrayList<>(); final List<Long> expiry = new ArrayList<>();
    Trail(ServerLevel level, long until, float damage) { this.level = level; this.until = until; this.damage = damage; }
    public int size() { return points.size(); }
  }
  public record Decoy(ServerLevel level, UUID caster, Vec3 pos, long until) {}
  public record Bound(ServerLevel level, UUID caster, UUID target, MobEffect effect) {}
  public record Corpse(EntityType<?> type, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, Vec3 pos, long time) {}

  private static final Map<UUID, Satellites> SATS = new ConcurrentHashMap<>();
  private static final Map<UUID, Trail> TRAILS = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> CLIMBERS = new ConcurrentHashMap<>();
  private static final Map<UUID, ServerLevel> CLIMBER_LEVELS = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> CONDUCTORS = new ConcurrentHashMap<>();
  private static final Map<UUID, ServerLevel> CONDUCTOR_LEVELS = new ConcurrentHashMap<>();
  private static final Map<UUID, Decoy> DECOYS = new ConcurrentHashMap<>();
  private static final List<Bound> BOUND = new ArrayList<>();
  private static final Map<UUID, List<UUID>> MINIONS = new ConcurrentHashMap<>();
  private static final Map<UUID, ServerLevel> MINION_LEVELS = new ConcurrentHashMap<>();
  private static final LinkedList<Corpse> CORPSES = new LinkedList<>();

  private Phase4Service() {}

  // ---------------------------------------------------------------- registro
  public static void addSatellites(LivingEntity owner, int count, int ticks, float damage) {
    SATS.put(owner.getUUID(), new Satellites((ServerLevel) owner.level(), owner.level().getGameTime() + ticks, Math.max(1, Math.min(MAX_SATELLITES, count)), damage));
  }

  public static void addTrail(LivingEntity owner, int ticks, float damage) {
    TRAILS.put(owner.getUUID(), new Trail((ServerLevel) owner.level(), owner.level().getGameTime() + ticks, damage));
  }

  public static Trail trailOf(UUID owner) { return TRAILS.get(owner); }
  public static boolean hasSatellites(UUID owner) { return SATS.containsKey(owner); }

  public static void addClimber(LivingEntity e, int ticks) {
    CLIMBERS.put(e.getUUID(), e.level().getGameTime() + ticks);
    CLIMBER_LEVELS.put(e.getUUID(), (ServerLevel) e.level());
  }

  public static boolean isClimber(UUID id) { return CLIMBERS.containsKey(id); }

  public static void addConductor(LivingEntity e, int ticks) {
    CONDUCTORS.put(e.getUUID(), e.level().getGameTime() + ticks);
    CONDUCTOR_LEVELS.put(e.getUUID(), (ServerLevel) e.level());
  }

  public static void addDecoy(LivingEntity caster, Vec3 pos, int ticks) {
    if (DECOYS.size() >= MAX_DECOYS && !DECOYS.containsKey(caster.getUUID())) return;
    DECOYS.put(caster.getUUID(), new Decoy((ServerLevel) caster.level(), caster.getUUID(), pos, caster.level().getGameTime() + ticks));
  }

  public static boolean hasDecoy(UUID caster) { return DECOYS.containsKey(caster); }

  public static void bind(LivingEntity caster, LivingEntity target, MobEffect effect) {
    synchronized (BOUND) {
      if (BOUND.size() >= MAX_BOUND) BOUND.remove(0);
      BOUND.add(new Bound((ServerLevel) caster.level(), caster.getUUID(), target.getUUID(), effect));
    }
  }

  public static int boundCount() { synchronized (BOUND) { return BOUND.size(); } }

  public static List<UUID> minionsOf(UUID owner) {
    return MINIONS.getOrDefault(owner, List.of());
  }

  /** Registra um aliado reanimado; devolve falso se o dono ja tem o maximo. */
  public static boolean addMinion(LivingEntity owner, Mob minion, int ticks) {
    List<UUID> list = MINIONS.computeIfAbsent(owner.getUUID(), id -> new ArrayList<>());
    synchronized (list) {
      list.removeIf(id -> !(((ServerLevel) owner.level()).getEntity(id) instanceof Mob m) || !m.isAlive());
      if (list.size() >= MAX_MINIONS) return false;
      list.add(minion.getUUID());
    }
    MINION_LEVELS.put(owner.getUUID(), (ServerLevel) owner.level());
    minion.getPersistentData().putUUID(Phase4State.OWNER, owner.getUUID());
    minion.getPersistentData().putLong(Phase4State.MINION_UNTIL, owner.level().getGameTime() + ticks);
    return true;
  }

  public static void recordCorpse(LivingEntity e) {
    synchronized (CORPSES) {
      while (CORPSES.size() >= 16) CORPSES.removeFirst();
      CORPSES.add(new Corpse(e.getType(), e.level().dimension(), e.position(), e.level().getGameTime()));
    }
  }

  /** O corpo recente mais perto do ponto (ate 120 s, raio dado); remove da lista ao usar. */
  public static Optional<Corpse> takeCorpse(ServerLevel level, Vec3 near, double radius) {
    synchronized (CORPSES) {
      long now = level.getGameTime();
      Corpse best = null;
      for (Corpse c : CORPSES) {
        if (c.dimension() != level.dimension() || now - c.time() > 2400) continue;
        double d = c.pos().distanceToSqr(near);
        if (d <= radius * radius && (best == null || d < best.pos().distanceToSqr(near))) best = c;
      }
      if (best != null) CORPSES.remove(best);
      return Optional.ofNullable(best);
    }
  }

  public static void clearAll() {
    SATS.clear(); TRAILS.clear(); CLIMBERS.clear(); CONDUCTORS.clear(); DECOYS.clear(); MINIONS.clear();
    synchronized (BOUND) { BOUND.clear(); }
    synchronized (CORPSES) { CORPSES.clear(); }
  }

  /** Remove tudo o que Dissipar pode tirar de uma criatura. */
  public static void dispelRegistries(UUID id) {
    SATS.remove(id); TRAILS.remove(id); CLIMBERS.remove(id); CONDUCTORS.remove(id);
  }

  // ---------------------------------------------------------------- times
  public static UUID ownerOf(Entity e) {
    CompoundTag d = e.getPersistentData();
    return d.hasUUID(Phase4State.OWNER) ? d.getUUID(Phase4State.OWNER) : null;
  }

  /** Aliados reanimados nao atacam o dono nem os outros aliados do mesmo dono. */
  public static boolean sameTeam(Entity a, Entity b) {
    UUID oa = ownerOf(a), ob = ownerOf(b);
    if (oa != null && (oa.equals(b.getUUID()) || oa.equals(ob))) return true;
    return ob != null && ob.equals(a.getUUID());
  }

  // ---------------------------------------------------------------- eventos
  @SubscribeEvent
  public static void onHurt(LivingHurtEvent event) {
    LivingEntity victim = event.getEntity();
    if (victim.level().isClientSide) return;
    Entity attackerEntity = event.getSource().getEntity();
    if (Phase4State.active(victim, Phase4State.CURSE)) event.setAmount(event.getAmount() * 1.25f);
    if (Phase4State.active(victim, Phase4State.PETRIFY)) event.setAmount(event.getAmount() * 0.5f);

    // aliados reanimados: o dono apanha ou bate, eles vao atras de quem foi.
    if (victim instanceof LivingEntity && attackerEntity instanceof LivingEntity attacker) {
      assist(victim, attacker);
      assist(attacker, victim);
    }

    if (BUSY.get()) return;
    BUSY.set(true);
    try {
      ServerLevel level = (ServerLevel) victim.level();
      float amount = event.getAmount();
      // Elo: parte do dano passa ao parceiro.
      if (Phase4State.active(victim, Phase4State.LINK) && victim.getPersistentData().hasUUID(Phase4State.LINK_OF)) {
        Entity partner = level.getEntity(victim.getPersistentData().getUUID(Phase4State.LINK_OF));
        if (partner instanceof LivingEntity p && p.isAlive() && p != victim) {
          p.hurt(level.damageSources().magic(), amount * 0.5f);
          level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.0, p.getZ(), 8, 0.3, 0.4, 0.3, 0.2);
        }
      }
      // Eletrificado: o dano salta para quem esta perto.
      if (Phase4State.active(victim, Phase4State.CHARGED)) {
        int hops = 0;
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(4.0),
            e -> e != victim && e != attackerEntity && e.isAlive())) {
          if (hops++ >= 2) break;
          near.hurt(level.damageSources().lightningBolt(), amount * 0.35f);
          level.sendParticles(com.arcane.registry.ModParticles.ARCANE_BOLT.get(), near.getX(), near.getY() + 1.0, near.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
        }
      }
      // Refletir: o agressor sente parte do golpe.
      if (Phase4State.active(victim, Phase4State.REFLECT) && attackerEntity instanceof LivingEntity attacker && attacker != victim) {
        attacker.hurt(level.damageSources().indirectMagic(victim, victim), amount * 0.4f);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 8, 0.3, 0.4, 0.3, 0.2);
      }
      // Arma encantada pelo glifo Encantar.
      if (attackerEntity instanceof LivingEntity a && event.getSource().getDirectEntity() == a) imbueHit(level, a, victim, event);
    } finally {
      BUSY.set(false);
    }
  }

  private static void assist(LivingEntity ownerSide, LivingEntity target) {
    List<UUID> list = MINIONS.get(ownerSide.getUUID());
    if (list == null || list.isEmpty()) return;
    ServerLevel level = (ServerLevel) ownerSide.level();
    synchronized (list) {
      for (UUID id : list) {
        if (level.getEntity(id) instanceof Mob m && m.isAlive() && !sameTeam(m, target) && target != ownerSide) m.setTarget(target);
      }
    }
  }

  private static void imbueHit(ServerLevel level, LivingEntity attacker, LivingEntity victim, LivingHurtEvent event) {
    ItemStack stack = attacker.getMainHandItem();
    CompoundTag tag = stack.getTagElement("arcane_imbue");
    if (tag == null) return;
    int hits = tag.getInt("hits");
    if (tag.getLong("until") <= level.getGameTime() || hits <= 0) {
      stack.removeTagKey("arcane_imbue");
      return;
    }
    switch (tag.getInt("kind")) {
      case 1 -> victim.setSecondsOnFire(4);
      case 2 -> victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
      case 3 -> event.setAmount(event.getAmount() + 3.0f);
      default -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
    }
    event.setAmount(event.getAmount() + 2.0f);
    level.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 10, 0.3, 0.4, 0.3, 0.2);
    if (--hits <= 0) stack.removeTagKey("arcane_imbue"); else tag.putInt("hits", hits);
  }

  @SubscribeEvent
  public static void onHeal(LivingHealEvent event) {
    if (!event.getEntity().level().isClientSide && Phase4State.active(event.getEntity(), Phase4State.CURSE)) event.setCanceled(true);
  }

  @SubscribeEvent
  public static void onProjectile(ProjectileImpactEvent event) {
    if (event.getEntity().level().isClientSide) return;
    if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target
        && Phase4State.active(target, Phase4State.REFLECT)) {
      Projectile projectile = event.getProjectile();
      if (projectile.getOwner() == target) return;
      Vec3 back = projectile.getDeltaMovement().scale(-1.0);
      double speed = Math.max(0.6, back.length());
      event.setCanceled(true);
      projectile.setOwner(target);
      projectile.shoot(back.x, back.y, back.z, (float) speed, 0.0f);
      ((ServerLevel) target.level()).sendParticles(ParticleTypes.ENCHANTED_HIT, projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
    }
  }

  @SubscribeEvent
  public static void onJoin(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide || CONDUCTORS.isEmpty() || !(event.getEntity() instanceof LightningBolt bolt)) return;
    if (bolt.getPersistentData().contains(SpellImmunity.BOLT_TAG) || bolt.getPersistentData().getBoolean("arcane_redirected")) return;
    ServerLevel level = (ServerLevel) event.getLevel();
    long now = level.getGameTime();
    Entity best = null;
    double bestD = 24.0 * 24.0;
    for (Map.Entry<UUID, Long> e : CONDUCTORS.entrySet()) {
      if (e.getValue() < now || CONDUCTOR_LEVELS.get(e.getKey()) != level) continue;
      Entity c = level.getEntity(e.getKey());
      if (c == null || !c.isAlive()) continue;
      double d = c.position().distanceToSqr(bolt.position());
      if (d < bestD) { bestD = d; best = c; }
    }
    if (best != null) {
      bolt.getPersistentData().putBoolean("arcane_redirected", true);
      bolt.moveTo(best.getX(), best.getY(), best.getZ());
    }
  }

  @SubscribeEvent
  public static void onDeath(LivingDeathEvent event) {
    LivingEntity e = event.getEntity();
    if (e.level().isClientSide) return;
    if (e instanceof Mob mob && ownerOf(mob) == null && mob.getType().getCategory() != net.minecraft.world.entity.MobCategory.MISC
        && !mob.getType().is(net.minecraftforge.common.Tags.EntityTypes.BOSSES)) {
      recordCorpse(mob);
    }
    UUID id = e.getUUID();
    dispelRegistries(id);
    synchronized (BOUND) {
      for (Iterator<Bound> it = BOUND.iterator(); it.hasNext(); ) {
        Bound b = it.next();
        if (b.caster().equals(id)) { release(b); it.remove(); }
      }
    }
  }

  private static void release(Bound b) {
    if (b.level().getEntity(b.target()) instanceof LivingEntity t) t.removeEffect(b.effect());
  }

  // ---------------------------------------------------------------- tick
  public static void tick(MinecraftServer server) {
    int t = server.getTickCount();
    if (!CLIMBERS.isEmpty() && t % 2 == 0) tickClimbers();
    if (t % 4 == 0) {
      if (!SATS.isEmpty()) tickSatellites();
      if (!TRAILS.isEmpty()) tickTrails();
    }
    if (t % 20 == 0) tickSlow();
  }

  private static void tickClimbers() {
    for (Iterator<Map.Entry<UUID, Long>> it = CLIMBERS.entrySet().iterator(); it.hasNext(); ) {
      var entry = it.next();
      ServerLevel level = CLIMBER_LEVELS.get(entry.getKey());
      Entity e = level == null ? null : level.getEntity(entry.getKey());
      if (!(e instanceof LivingEntity living) || !living.isAlive() || entry.getValue() < level.getGameTime()) { it.remove(); continue; }
      if (living.isShiftKeyDown() || living.onGround()) continue;
      BlockPos ahead = living.blockPosition().relative(living.getDirection());
      boolean wall = !level.getBlockState(ahead).getCollisionShape(level, ahead).isEmpty()
          || !level.getBlockState(ahead.above()).getCollisionShape(level, ahead.above()).isEmpty();
      boolean pushing = living instanceof net.minecraft.world.entity.player.Player ? living.zza > 0 : living.horizontalCollision;
      if (wall && pushing) {
        Vec3 dm = living.getDeltaMovement();
        living.setDeltaMovement(dm.x * 0.5, 0.24, dm.z * 0.5);
        living.fallDistance = 0;
        living.hurtMarked = true;
      }
    }
  }

  private static void tickSatellites() {
    for (Iterator<Map.Entry<UUID, Satellites>> it = SATS.entrySet().iterator(); it.hasNext(); ) {
      var entry = it.next();
      Satellites s = entry.getValue();
      ServerLevel level = s.level();
      if (!(level.getEntity(entry.getKey()) instanceof LivingEntity owner) || !owner.isAlive() || s.until() < level.getGameTime()) { it.remove(); continue; }
      double base = level.getGameTime() * 0.25;
      var source = level.damageSources().indirectMagic(owner, owner);
      for (int i = 0; i < s.count(); i++) {
        double a = base + i * (2 * Math.PI / s.count());
        Vec3 pos = owner.position().add(Math.cos(a) * 2.0, 1.0 + Math.sin(base * 0.5 + i) * 0.3, Math.sin(a) * 2.0);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(0.8), e -> e != owner && e.isAlive() && !sameTeam(owner, e))) {
          long now = level.getGameTime();
          if (victim.getPersistentData().getLong("arcane_sat_cd") > now) continue;
          victim.getPersistentData().putLong("arcane_sat_cd", now + 10);
          victim.hurt(source, s.damage());
        }
      }
    }
  }

  private static void tickTrails() {
    for (Iterator<Map.Entry<UUID, Trail>> it = TRAILS.entrySet().iterator(); it.hasNext(); ) {
      var entry = it.next();
      Trail tr = entry.getValue();
      ServerLevel level = tr.level;
      long now = level.getGameTime();
      boolean over = !(level.getEntity(entry.getKey()) instanceof LivingEntity owner) || !owner.isAlive() || tr.until < now;
      for (int i = tr.points.size() - 1; i >= 0; i--) {
        if (tr.expiry.get(i) < now) { tr.points.remove(i); tr.expiry.remove(i); }
      }
      if (over) { if (tr.points.isEmpty()) it.remove(); continue; }
      LivingEntity own = (LivingEntity) level.getEntity(entry.getKey());
      Vec3 here = own.position();
      if (tr.points.isEmpty() || tr.points.get(tr.points.size() - 1).distanceToSqr(here) >= 1.0) {
        if (tr.points.size() >= MAX_TRAIL_POINTS) { tr.points.remove(0); tr.expiry.remove(0); }
        tr.points.add(here);
        tr.expiry.add(now + 120);
      }
      var source = level.damageSources().indirectMagic(own, own);
      for (Vec3 p : tr.points) {
        level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.1, p.z, 1, 0.2, 0.05, 0.2, 0.0);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(p.x - 0.9, p.y - 0.5, p.z - 0.9, p.x + 0.9, p.y + 1.5, p.z + 0.9),
            e -> e != own && e.isAlive() && !sameTeam(own, e))) {
          if (victim.getPersistentData().getLong("arcane_trail_cd") > now) continue;
          victim.getPersistentData().putLong("arcane_trail_cd", now + 12);
          victim.hurt(source, tr.damage);
          victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
      }
    }
  }

  private static void tickSlow() {
    // Aliados reanimados: somem no prazo ou quando o dono some.
    for (Iterator<Map.Entry<UUID, List<UUID>>> it = MINIONS.entrySet().iterator(); it.hasNext(); ) {
      var entry = it.next();
      ServerLevel level = MINION_LEVELS.get(entry.getKey());
      if (level == null) { it.remove(); continue; }
      boolean ownerGone = !(level.getEntity(entry.getKey()) instanceof LivingEntity o) || !o.isAlive();
      List<UUID> list = entry.getValue();
      synchronized (list) {
        list.removeIf(id -> {
          Entity m = level.getEntity(id);
          if (!(m instanceof Mob mob) || !mob.isAlive()) return true;
          if (ownerGone || mob.getPersistentData().getLong(Phase4State.MINION_UNTIL) < level.getGameTime()) {
            level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1.0, mob.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            mob.discard();
            return true;
          }
          return false;
        });
        if (list.isEmpty()) it.remove();
      }
    }
    // Chamariz: puxa as criaturas para o ponto enquanto durar.
    for (Iterator<Map.Entry<UUID, Decoy>> it = DECOYS.entrySet().iterator(); it.hasNext(); ) {
      Decoy d = it.next().getValue();
      ServerLevel level = d.level();
      if (d.until() < level.getGameTime()) { it.remove(); continue; }
      level.sendParticles(ParticleTypes.ENCHANT, d.pos().x, d.pos().y + 1.0, d.pos().z, 10, 0.3, 0.5, 0.3, 0.3);
      if (!(level.getEntity(d.caster()) instanceof LivingEntity caster)) continue;
      for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(d.pos(), d.pos()).inflate(20.0), m -> m.isAlive() && ownerOf(m) == null && m != caster)) {
        if (mob.getTarget() == caster) mob.setTarget(null);
        MindStates.distract(mob, caster, 40);
        mob.getNavigation().moveTo(d.pos().x, d.pos().y, d.pos().z, 1.0);
      }
    }
    // Vinculo: o efeito acaba se o conjurador morre, some ou se afasta demais.
    synchronized (BOUND) {
      for (Iterator<Bound> it = BOUND.iterator(); it.hasNext(); ) {
        Bound b = it.next();
        Entity c = b.level().getEntity(b.caster());
        Entity target = b.level().getEntity(b.target());
        if (target == null || !(target instanceof LivingEntity lt) || !lt.hasEffect(b.effect())) { it.remove(); continue; }
        if (c == null || !c.isAlive() || c.distanceToSqr(target) > 40.0 * 40.0) { release(b); it.remove(); }
      }
    }
    // Condutores expirados.
    for (var e : CONDUCTORS.entrySet()) {
      ServerLevel level = CONDUCTOR_LEVELS.get(e.getKey());
      if (level == null || e.getValue() < level.getGameTime()) { CONDUCTORS.remove(e.getKey()); CONDUCTOR_LEVELS.remove(e.getKey()); }
    }
  }
}
