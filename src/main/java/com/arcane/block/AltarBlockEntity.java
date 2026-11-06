package com.arcane.block;

import com.arcane.inscription.Inscription;
import com.arcane.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/** O que o Altar Arcano guarda: a magia armazenada (se houver), quem a guardou e o descanso apos um ritual. */
public class AltarBlockEntity extends BlockEntity {
  @Nullable private Inscription stored;
  @Nullable private UUID owner;
  private boolean storedLegendary;
  private long cooldownUntil;

  public AltarBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.ARCANE_ALTAR.get(), pos, state);
  }

  @Nullable public Inscription stored() { return stored; }
  @Nullable public UUID owner() { return owner; }
  public boolean storedLegendary() { return storedLegendary; }
  public long cooldownUntil() { return cooldownUntil; }
  public boolean hasStored() { return stored != null; }

  public void store(Inscription spell, UUID ownerId, boolean legendary) {
    this.stored = spell;
    this.owner = ownerId;
    this.storedLegendary = legendary;
    setChanged();
  }

  public void clearStored() {
    this.stored = null;
    this.storedLegendary = false;
    setChanged();
  }

  public void setCooldownUntil(long tick) {
    this.cooldownUntil = tick;
    setChanged();
  }

  @Override
  protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    if (stored != null) {
      Inscription.CODEC.encodeStart(NbtOps.INSTANCE, stored).result().ifPresent(t -> tag.put("stored", t));
    }
    if (owner != null) tag.putUUID("owner", owner);
    tag.putBoolean("legendary", storedLegendary);
    tag.putLong("cooldown", cooldownUntil);
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);
    stored = tag.contains("stored") ? Inscription.CODEC.parse(NbtOps.INSTANCE, tag.get("stored")).result().orElse(null) : null;
    owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    storedLegendary = tag.getBoolean("legendary");
    cooldownUntil = tag.getLong("cooldown");
  }
}
