package com.arcane.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;

/** Golem Runico: guardiao de pedra com nucleo luminoso, em 3 niveis (Guardiao, Colosso, Titan), cada um com modelo proprio. */
public class ArcaneGolemEntity extends IronGolem {
  private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(ArcaneGolemEntity.class, EntityDataSerializers.INT);

  public ArcaneGolemEntity(EntityType<? extends ArcaneGolemEntity> type, Level level) {
    super(type, level);
  }

  public int tier() { return this.entityData.get(TIER); }

  public float visualScale() { return SummonTier.golemScale(tier()); }

  public void setTier(int tier) {
    int t = SummonTier.clamp(tier);
    this.entityData.set(TIER, t);
    if (!level().isClientSide) SummonTier.apply(this, t, SummonTier.golemScale(t));
    refreshDimensions();
  }

  /**
   * Golem Runico: NAO luta como o golem de ferro. Cada golpe e um <b>impacto em area</b> (onda de pedra que atinge todo inimigo
   * em volta do alvo, com 70% do dano) e, ferido, levanta uma <b>runa de guarda</b> (resistencia forte por 5 s, recarga de 15 s).
   * Zumbe quando parado.
   */
  private static final int WARD_COOLDOWN = 300;
  private long wardReadyAt;

  @Override
  public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
    boolean hit = super.doHurtTarget(target);
    if (!hit || !(level() instanceof net.minecraft.server.level.ServerLevel sl)) return hit;
    double r = 2.5 * visualScale();
    float splash = (float) (getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.7);
    for (var e : sl.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, target.getBoundingBox().inflate(r),
        x -> x != this && x != target && x.isAlive() && x instanceof net.minecraft.world.entity.monster.Enemy)) {
      e.hurt(damageSources().mobAttack(this), splash);
      e.setDeltaMovement(e.getDeltaMovement().add(0, 0.35, 0));
    }
    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, target.getX(), target.getY() + 0.1, target.getZ(), 18, r * 0.4, 0.1, r * 0.4, 0.05);
    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, target.getX(), target.getY() + 0.2, target.getZ(), 10, r * 0.3, 0.2, r * 0.3, 0.1);
    sl.playSound(null, target.getX(), target.getY(), target.getZ(), com.arcane.registry.ModSounds.GOLEM_SLAM.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.2f - 0.2f * tier());
    return hit;
  }

  @Override
  public void aiStep() {
    super.aiStep();
    if (!level().isClientSide && tickCount % 20 == 0 && getHealth() < getMaxHealth() * 0.6f && tickCount >= wardReadyAt
        && getTarget() != null && getTarget().isAlive()) {
      wardReadyAt = tickCount + WARD_COOLDOWN;
      addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 100, 2));
      ((net.minecraft.server.level.ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW, getX(), getY() + getBbHeight() * 0.5, getZ(), 24, 0.6, 0.8, 0.6, 0.05);
    }
  }

  @Override
  protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.arcane.registry.ModSounds.GOLEM_HUM.get(); }

  @Override
  protected float getSoundVolume() { return 0.6f; }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    this.entityData.define(TIER, 0);
  }

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
    super.onSyncedDataUpdated(key);
    if (TIER.equals(key)) refreshDimensions();
  }

  @Override
  public EntityDimensions getDimensions(Pose pose) {
    return super.getDimensions(pose).scale(visualScale());
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putInt("ArcaneTier", tier());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    this.entityData.set(TIER, SummonTier.clamp(tag.getInt("ArcaneTier")));
    refreshDimensions();
  }
}
