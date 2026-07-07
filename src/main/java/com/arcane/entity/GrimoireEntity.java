package com.arcane.entity;

import com.arcane.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
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
  private int visualStage = 1;

  public GrimoireEntity(EntityType<? extends GrimoireEntity> type, Level level) {
    super(type, level);
    this.noPhysics = true;
  }

  public void bind(Player player, int stage) {
    this.ownerId = player.getUUID();
    this.visualStage = stage;
  }

  @Override
  public void tick() {
    super.tick();
    if (!level().isClientSide && ownerId != null) {
      Player owner = level().getPlayerByUUID(ownerId);
      if (owner == null || !owner.isAlive()) {
        discard();
        return;
      }
      setPos(owner.getX() + 1.2, owner.getY() + 1.0, owner.getZ() + 0.6);
    }
  }

  public int visualStage() { return visualStage; }

  @Override
  protected void defineSynchedData() {}

  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {
    if (tag.hasUUID("owner")) ownerId = tag.getUUID("owner");
    visualStage = tag.getInt("stage");
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    if (ownerId != null) tag.putUUID("owner", ownerId);
    tag.putInt("stage", visualStage);
  }

  @Override
  public Packet<ClientGamePacketListener> getAddEntityPacket() {
    return NetworkHooks.getEntitySpawningPacket(this);
  }
}
