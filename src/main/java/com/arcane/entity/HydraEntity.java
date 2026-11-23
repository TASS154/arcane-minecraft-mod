package com.arcane.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Hidra: guardia de um dos 7 covis. Tem 14 estagios de vida, de recem-nascida (1) a anciã (14); o estagio define o
 * tamanho, quantas cabecas tem (1 a 9), a vida, o dano e quais poderes usa. Os poderes ficam mais apocalipticos com o
 * estagio e a forca se ajusta a quantos jogadores lutam contra ela (os por perto pesam inteiro, os outros online pesam
 * pouco). Ao morrer, quem lutou aprende o glifo original do covil (ver HydraLairs).
 */
public class HydraEntity extends Monster {
  public static final int MAX_STAGE = 14;
  private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  /** Poder sendo usado agora (0 = nenhum), para o modelo abrir as bocas / erguer as cabecas. */
  private static final EntityDataAccessor<Integer> POWER = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  private static final UUID HEALTH_ID = UUID.fromString("6b1c33a1-6f0a-4c39-9c51-1f2d8d4e6a01");
  private static final UUID DAMAGE_ID = UUID.fromString("6b1c33a1-6f0a-4c39-9c51-1f2d8d4e6a02");

  public static final int BREATH = 1, SWEEP = 2, ROAR = 3, STORM = 4, METEOR = 5, CATACLYSM = 6;

  private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.arcane.hydra"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
  private final Set<UUID> fighters = new HashSet<>();
  private int lairIndex = -1;
  private int powerCooldown = 60;
  private int powerTicks;
  private double playerFactor = 1.0;
  /**
   * A vida do Minecraft tem teto de 1024. Acima disso a Hidra fica "mais dura": o dano recebido e dividido por este
   * fator, entao a vida efetiva e vida x dureza (a barra de chefe mostra a fracao, que e a mesma).
   */
  private double toughness = 1.0;

  public HydraEntity(EntityType<? extends Monster> type, Level level) {
    super(type, level);
    this.xpReward = 500;
    setPersistenceRequired();
  }

  public static AttributeSupplier.Builder createAttributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 80.0)
        .add(Attributes.ATTACK_DAMAGE, 6.0)
        .add(Attributes.ARMOR, 4.0)
        .add(Attributes.MOVEMENT_SPEED, 0.24)
        .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
        .add(Attributes.FOLLOW_RANGE, 48.0);
  }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    entityData.define(STAGE, 1);
    entityData.define(POWER, 0);
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
    goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
    goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24f));
    goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    targetSelector.addGoal(1, new HurtByTargetGoal(this));
    targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
  }

  // ------------------------------------------------------------------ estagio
  public int stage() { return entityData.get(STAGE); }

  public int power() { return entityData.get(POWER); }

  public int lairIndex() { return lairIndex; }

  public void setLairIndex(int i) { this.lairIndex = i; }

  /** Cabecas visiveis: 1 no estagio 1, 9 no 14. */
  public static int headsFor(int stage) { return 1 + Math.round((Math.max(1, stage) - 1) * 8f / 13f); }

  public int heads() { return headsFor(stage()); }

  /** Escala do corpo: 1 no estagio 1, 4,25 no 14. */
  public static float scaleFor(int stage) { return 1.0f + (Math.max(1, stage) - 1) * 0.25f; }

  public static double baseHealth(int stage) { return 80.0 * Math.pow(Math.max(1, stage), 1.35); }

  public static double baseDamage(int stage) { return 4.0 + 2.2 * Math.max(1, stage); }

  public void setStage(int stage) {
    int s = Math.max(1, Math.min(MAX_STAGE, stage));
    entityData.set(STAGE, s);
    applyHealth(baseHealth(s));
    getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseDamage(s));
    getAttribute(Attributes.ARMOR).setBaseValue(Math.min(30, 2.0 * s));
    getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22 + 0.006 * s);
    setHealth(getMaxHealth());
    refreshDimensions();
    bossBar.setName(Component.translatable("entity.arcane.hydra.stage", Component.translatable("hydra.arcane.stage." + s), s));
  }

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
    super.onSyncedDataUpdated(key);
    if (STAGE.equals(key)) refreshDimensions();
  }

  @Override
  public EntityDimensions getDimensions(Pose pose) {
    return super.getDimensions(pose).scale(scaleFor(stage()));
  }

  /**
   * Peso dos jogadores: quem esta a ate 64 blocos conta 1, o resto online conta 0,3. Vida e dano sobem com o peso
   * (a vida guarda a mesma fracao ao mudar). 1 jogador sozinho = forca base.
   */
  public static double playerWeight(int near, int farOnline) {
    return Math.max(1.0, near + 0.3 * farOnline);
  }

  private void updatePlayerFactor() {
    if (!(level() instanceof ServerLevel sl)) return;
    int near = 0, far = 0;
    for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
      if (p.isSpectator() || p.isCreative()) continue;
      if (p.level() == level() && p.distanceToSqr(this) < 64 * 64) near++;
      else far++;
    }
    double f = playerWeight(near, far);
    if (Math.abs(f - playerFactor) < 1.0e-3) return;
    playerFactor = f;
    float frac = getHealth() / getMaxHealth();
    applyHealth(baseHealth(stage()) * (1.0 + 0.75 * (f - 1)));
    setModifier(getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, 0.35 * (f - 1));
    setHealth(frac * getMaxHealth());
  }

  private static void setModifier(AttributeInstance attr, UUID id, double amount) {
    if (attr == null) return;
    attr.removeModifier(id);
    if (amount > 0) attr.addTransientModifier(new AttributeModifier(id, "arcane hydra players", amount, AttributeModifier.Operation.MULTIPLY_BASE));
  }

  public double playerFactor() { return playerFactor; }

  /** Vida alvo: o que cabe no atributo (ate 1024) vira vida; o resto vira dureza. */
  private void applyHealth(double wanted) {
    double cap = 1024.0;
    getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.min(cap, wanted));
    toughness = Math.max(1.0, wanted / cap);
  }

  public double effectiveMaxHealth() { return getMaxHealth() * toughness; }

  // ------------------------------------------------------------------ poderes
  @Override
  public void aiStep() {
    super.aiStep();
    if (level().isClientSide) return;
    if (tickCount % 100 == 1) updatePlayerFactor();
    bossBar.setProgress(getHealth() / getMaxHealth());
    if (powerTicks > 0) {
      powerTicks--;
      if (power() == BREATH) breathTick();
      if (powerTicks == 0) entityData.set(POWER, 0);
      return;
    }
    LivingEntity target = getTarget();
    if (target == null || !target.isAlive()) return;
    if (--powerCooldown > 0) return;
    int s = stage();
    powerCooldown = Math.max(30, 110 - s * 5) + random.nextInt(40);
    double d = distanceTo(target);
    int choice;
    int roll = random.nextInt(100);
    if (s >= 13 && roll < 15) choice = CATACLYSM;
    else if (s >= 10 && roll < 35) choice = METEOR;
    else if (s >= 7 && roll < 50) choice = STORM;
    else if (s >= 5 && roll < 60) choice = ROAR;
    else if (s >= 3 && d < 5 + s * 0.5) choice = SWEEP;
    else choice = BREATH;
    use(choice, target);
  }

  public void use(int choice, LivingEntity target) {
    if (!(level() instanceof ServerLevel sl)) return;
    entityData.set(POWER, choice);
    int s = stage();
    double dmg = getAttributeValue(Attributes.ATTACK_DAMAGE);
    switch (choice) {
      case BREATH -> {
        powerTicks = 30;
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 1.6f - s * 0.06f);
      }
      case SWEEP -> {
        powerTicks = 10;
        double r = 4 + s * 0.45;
        for (LivingEntity e : enemiesWithin(r)) {
          e.hurt(damageSources().mobAttack(this), (float) (dmg * 0.9));
          Vec3 push = e.position().subtract(position()).normalize().scale(1.2 + s * 0.08);
          e.push(push.x, 0.5 + s * 0.03, push.z);
          e.hurtMarked = true;
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 0.5, getZ(), 12, r * 0.4, 0.2, r * 0.4, 0);
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 6, r * 0.3, 0.1, r * 0.3, 0);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5f, 0.6f);
      }
      case ROAR -> {
        powerTicks = 30;
        for (LivingEntity e : enemiesWithin(24 + s)) {
          e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1 + s / 6), this);
          e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, s / 5), this);
          e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0), this);
        }
        sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0f, 0.5f);
      }
      case STORM -> {
        powerTicks = 20;
        int bolts = 1 + s / 3;
        List<LivingEntity> foes = enemiesWithin(40);
        for (int i = 0; i < bolts; i++) {
          Vec3 at = !foes.isEmpty() && random.nextInt(3) > 0 ? foes.get(random.nextInt(foes.size())).position()
              : position().add(random.nextGaussian() * 10, 0, random.nextGaussian() * 10);
          var bolt = EntityType.LIGHTNING_BOLT.create(sl);
          if (bolt == null) continue;
          bolt.moveTo(at.x, at.y, at.z);
          bolt.setVisualOnly(false);
          sl.addFreshEntity(bolt);
        }
      }
      case METEOR -> {
        powerTicks = 20;
        int rocks = 2 + s / 3;
        for (int i = 0; i < rocks; i++) {
          Vec3 at = target.position().add(random.nextGaussian() * 6, 0, random.nextGaussian() * 6);
          LargeFireball ball = new LargeFireball(sl, this, 0, -1.6, 0, s >= 12 ? 2 : 1);
          ball.setPos(at.x, at.y + 22 + random.nextInt(8), at.z);
          sl.addFreshEntity(ball);
        }
        sl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0f, 0.4f);
      }
      case CATACLYSM -> {
        // O fim do mundo em miniatura: onda de choque enorme, cura com o dano causado e um anel de fogo.
        powerTicks = 40;
        float healed = 0;
        for (LivingEntity e : enemiesWithin(28)) {
          float before = e.getHealth();
          e.hurt(damageSources().indirectMagic(this, this), (float) (dmg * 1.6));
          healed += Math.max(0, before - e.getHealth());
          Vec3 push = e.position().subtract(position()).normalize().scale(2.5);
          e.push(push.x, 1.2, push.z);
          e.hurtMarked = true;
          e.setSecondsOnFire(8);
        }
        heal(healed * 0.5f);
        for (int i = 0; i < 72; i++) {
          double a = Math.PI * 2 * i / 72;
          sl.sendParticles(ParticleTypes.FLAME, getX() + Math.cos(a) * 10, getY() + 0.3, getZ() + Math.sin(a) * 10, 2, 0.2, 0.1, 0.2, 0.05);
          sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(a) * 18, getY() + 0.3, getZ() + Math.sin(a) * 18, 1, 0.2, 0.1, 0.2, 0.05);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 3, 3, 1, 3, 0);
        sl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0f, 0.6f);
      }
      default -> entityData.set(POWER, 0);
    }
  }

  /** Sopro: um cone de fogo (estagio 8+ vira sopro do dragao, que tambem seca) de cada cabeca na direcao do olhar. */
  private void breathTick() {
    if (!(level() instanceof ServerLevel sl)) return;
    int s = stage();
    LivingEntity target = getTarget();
    if (target != null) getLookControl().setLookAt(target, 30f, 30f);
    Vec3 look = target != null ? target.getEyePosition().subtract(getEyePosition()).normalize() : getLookAngle();
    double range = 6 + s * 0.9;
    Vec3 mouth = getEyePosition().add(look.scale(getBbWidth() * 0.6));
    var particle = s >= 8 ? ParticleTypes.DRAGON_BREATH : ParticleTypes.FLAME;
    for (int i = 0; i < 4 + heads(); i++) {
      double f = 0.6 + random.nextDouble() * 0.6;
      Vec3 v = look.add(random.nextGaussian() * 0.12, random.nextGaussian() * 0.12, random.nextGaussian() * 0.12).scale(f);
      sl.sendParticles(particle, mouth.x, mouth.y, mouth.z, 0, v.x, v.y, v.z, 1.0);
    }
    if (powerTicks % 5 != 0) return;
    double dmg = getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.35 * Math.max(1, heads() / 2.0);
    for (LivingEntity e : enemiesWithin(range)) {
      Vec3 to = e.getEyePosition().subtract(mouth);
      if (to.length() > range || to.normalize().dot(look) < 0.82) continue;
      e.hurt(damageSources().indirectMagic(this, this), (float) dmg);
      e.setSecondsOnFire(4);
      if (s >= 8) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, s >= 12 ? 1 : 0), this);
    }
  }

  private List<LivingEntity> enemiesWithin(double r) {
    return level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(r),
        e -> e != this && e.isAlive() && !(e instanceof HydraEntity) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
            && e.distanceToSqr(this) <= r * r);
  }

  // ------------------------------------------------------------------ luta, chefe e morte
  @Override
  public boolean hurt(DamageSource source, float amount) {
    if (source.getEntity() instanceof ServerPlayer p) fighters.add(p.getUUID());
    return super.hurt(source, (float) (amount / toughness));
  }

  public Set<UUID> fighters() { return fighters; }

  @Override
  public void die(DamageSource source) {
    super.die(source);
    if (level() instanceof ServerLevel sl) com.arcane.world.HydraLairs.onHydraDeath(sl, this);
  }

  @Override
  public void startSeenByPlayer(ServerPlayer player) {
    super.startSeenByPlayer(player);
    bossBar.addPlayer(player);
  }

  @Override
  public void stopSeenByPlayer(ServerPlayer player) {
    super.stopSeenByPlayer(player);
    bossBar.removePlayer(player);
  }

  @Override
  public boolean removeWhenFarAway(double d) { return false; }

  @Override
  public boolean fireImmune() { return stage() >= 6; }

  @Override
  public boolean causeFallDamage(float distance, float mult, DamageSource source) { return false; }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putInt("HydraStage", stage());
    tag.putInt("HydraLair", lairIndex);
    tag.putDouble("HydraTough", toughness);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    if (tag.contains("HydraStage")) {
      float hp = getHealth();
      setStage(tag.getInt("HydraStage"));
      if (tag.contains("Health")) setHealth(hp);
    }
    lairIndex = tag.contains("HydraLair") ? tag.getInt("HydraLair") : -1;
  }

  /** Volta para perto do covil se for arrastada para longe. */
  public void homeTo(BlockPos centre, int radius) {
    restrictTo(centre, radius);
  }
}
