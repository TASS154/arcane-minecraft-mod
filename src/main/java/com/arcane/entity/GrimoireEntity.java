package com.arcane.entity;

import com.arcane.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class GrimoireEntity extends Entity {
  private UUID ownerId;
  private static final EntityDataAccessor<Integer> STAGE =
      SynchedEntityData.defineId(GrimoireEntity.class, EntityDataSerializers.INT);

  /** Dono sincronizado: o cliente calcula sozinho a posicao suavizada do livro em volta dele. */
  private static final EntityDataAccessor<java.util.Optional<UUID>> OWNER =
      SynchedEntityData.defineId(GrimoireEntity.class, EntityDataSerializers.OPTIONAL_UUID);

  public GrimoireEntity(EntityType<? extends GrimoireEntity> type, Level level) {
    super(type, level);
    this.noPhysics = true;
    this.noCulling = true;
  }

  public java.util.Optional<UUID> ownerUuid() { return this.entityData.get(OWNER); }

  public void bind(Player player, int stage) {
    this.ownerId = player.getUUID();
    this.entityData.set(OWNER, java.util.Optional.of(ownerId));
    this.entityData.set(STAGE, Math.max(1, stage));
  }

  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide) {
      int stage = visualStage();
      if (stage >= 3 && tickCount % (stage >= 6 ? 2 : 6) == 0) {
        double a = tickCount * 0.2;
        level().addParticle(com.arcane.registry.ModParticles.ARCANE_SPARK.get(),
            getX() + Math.cos(a) * 0.35, getY() + 0.3 + random.nextDouble() * 0.3, getZ() + Math.sin(a) * 0.35, 0.0, 0.01, 0.0);
      }
    }
    if (!level().isClientSide && ownerId == null && tickCount > 40) {
      discard(); // grimorio sem dono (sobra de teste ou dado antigo)
      return;
    }
    if (!level().isClientSide && ownerId != null) {
      Player owner = level().getPlayerByUUID(ownerId);
      if (owner == null || !owner.isAlive()) {
        discard();
        return;
      }
      setPos(owner.getX() + 1.2, owner.getY() + 1.0, owner.getZ() + 0.6);
    }
  }

  public int visualStage() { return this.entityData.get(STAGE); }

  @Override
  protected void defineSynchedData() {
    this.entityData.define(STAGE, 1);
    this.entityData.define(OWNER, java.util.Optional.empty());
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {
    if (tag.hasUUID("owner")) { ownerId = tag.getUUID("owner"); this.entityData.set(OWNER, java.util.Optional.of(ownerId)); }
    this.entityData.set(STAGE, Math.max(1, tag.getInt("stage")));
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    if (ownerId != null) tag.putUUID("owner", ownerId);
    tag.putInt("stage", visualStage());
  }

  @Override
  public Packet<ClientGamePacketListener> getAddEntityPacket() {
    return NetworkHooks.getEntitySpawningPacket(this);
  }
}
