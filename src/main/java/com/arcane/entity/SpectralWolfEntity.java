package com.arcane.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;

/** Lobo Espectral: lobo domesticavel com modelo proprio em 3 niveis (Espectral, Alfa, Fenrir) que cresce com o nivel. */
public class SpectralWolfEntity extends Wolf {
  private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(SpectralWolfEntity.class, EntityDataSerializers.INT);

  public SpectralWolfEntity(EntityType<? extends SpectralWolfEntity> type, Level level) {
    super(type, level);
  }

  public int tier() { return this.entityData.get(TIER); }

  public float visualScale() { return SummonTier.wolfScale(tier()); }

  public void setTier(int tier) {
    int t = SummonTier.clamp(tier);
    this.entityData.set(TIER, t);
    if (!level().isClientSide) SummonTier.apply(this, t, SummonTier.wolfScale(t));
    refreshDimensions();
  }

  /**
   * Lobo Espectral: NAO luta como o lobo comum. Ele <b>salta para as costas</b> do alvo distante (6 a 20 blocos, recarga de 5 s),
   * a mordida <b>entorpece</b> (lentidao por 3 s) e, sendo espectral, nao sofre dano de queda.
   */
  private static final int POUNCE_COOLDOWN = 100;
  private long pounceReadyAt;

  @Override
  public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
    boolean hit = super.doHurtTarget(target);
    if (hit && target instanceof net.minecraft.world.entity.LivingEntity living) {
      living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
    }
    return hit;
  }

  @Override
  public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) { return false; }

  @Override
  public void aiStep() {
    super.aiStep();
    if (level().isClientSide || tickCount % 10 != 0 || tickCount < pounceReadyAt) return;
    var target = getTarget();
    if (target == null || !target.isAlive()) return;
    double d = distanceTo(target);
    if (d < 6.0 || d > 20.0) return;
    var back = target.getLookAngle().multiply(1, 0, 1);
    if (back.lengthSqr() < 1.0e-4) back = new net.minecraft.world.phys.Vec3(0, 0, 1);
    back = back.normalize().scale(-1.6 * visualScale());
    double x = target.getX() + back.x, y = target.getY(), z = target.getZ() + back.z;
    var box = getBoundingBox().move(x - getX(), y - getY(), z - getZ());
    if (!level().noCollision(this, box)) return; // sem espaco atras do alvo: tenta no proximo ciclo
    pounceReadyAt = tickCount + POUNCE_COOLDOWN;
    var sl = (net.minecraft.server.level.ServerLevel) level();
    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.5, getZ(), 16, 0.3, 0.4, 0.3, 0.1);
    teleportTo(x, y, z);
    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, x, y + 0.5, z, 16, 0.3, 0.4, 0.3, 0.1);
    sl.playSound(null, x, y, z, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1.7f);
    getNavigation().stop();
  }

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
