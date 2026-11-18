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
  private int storedOffering;
  private boolean storedUnique;
  private long cooldownUntil;

  public AltarBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.ARCANE_ALTAR.get(), pos, state);
  }

  @Nullable public Inscription stored() { return stored; }
  @Nullable public UUID owner() { return owner; }
  public boolean storedLegendary() { return storedLegendary; }
  /** Valor (%) das oferendas gastas ao guardar a magia; vale a cada disparo. */
  public int storedOffering() { return storedOffering; }
  /** Entre as oferendas havia um item unico (o teto do valor nao se aplica). */
  public boolean storedUnique() { return storedUnique; }
  public long cooldownUntil() { return cooldownUntil; }
  public boolean hasStored() { return stored != null; }

  public void store(Inscription spell, UUID ownerId, boolean legendary) {
    store(spell, ownerId, legendary, legendary ? com.arcane.ritual.OfferingValue.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR)) : 0);
  }

  public void store(Inscription spell, UUID ownerId, boolean legendary, int offeringPercent) {
    store(spell, ownerId, legendary, offeringPercent, false);
  }

  public void store(Inscription spell, UUID ownerId, boolean legendary, int offeringPercent, boolean unique) {
    this.storedUnique = unique;
    this.storedOffering = offeringPercent;
    this.stored = spell;
    this.owner = ownerId;
    this.storedLegendary = legendary;
    setChanged();
  }

  public void clearStored() {
    this.stored = null;
    this.storedLegendary = false;
    this.storedOffering = 0;
    this.storedUnique = false;
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
    tag.putInt("offering", storedOffering);
    tag.putBoolean("unique", storedUnique);
    tag.putLong("cooldown", cooldownUntil);
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);
    stored = tag.contains("stored") ? Inscription.CODEC.parse(NbtOps.INSTANCE, tag.get("stored")).result().orElse(null) : null;
    owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    storedLegendary = tag.getBoolean("legendary");
    storedOffering = tag.getInt("offering");
    storedUnique = tag.getBoolean("unique");
    cooldownUntil = tag.getLong("cooldown");
  }
}
