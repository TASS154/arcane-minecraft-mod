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
