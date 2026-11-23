package com.arcane.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ilusao: um clone do jogador (com a skin, a armadura e o nome dele) que nao interage com o mundo. Ele imita a
 * INTENCAO do dono, nao os movimentos exatos: anda quando ele anda (na mesma direcao, ao lado), corre quando ele corre,
 * ataca com outra arma do inventario quando ele ataca e finge conjurar quando ele conjura. Nao fere, nao quebra, nao
 * abre nada; um golpe qualquer o desfaz em fumaca. Criaturas que miravam o dono podem trocar de alvo para um clone.
 */
public class IllusionEntity extends PathfinderMob {
  private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(IllusionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
  private int slot, lifetime = 300;
  private final List<ItemStack> weapons = new ArrayList<>();
  private int lastOwnerSwing;

  public IllusionEntity(EntityType<? extends IllusionEntity> type, Level level) {
    super(type, level);
    setPersistenceRequired();
  }

  public static AttributeSupplier.Builder createAttributes() {
    return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32.0);
  }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    entityData.define(OWNER, Optional.empty());
  }

  public Optional<UUID> owner() { return entityData.get(OWNER); }

  /** Prepara o clone: dono, posicao ao lado dele (slot), duracao e as armas que ele pode "puxar". */
  public void setup(Player owner, int slot, int lifetime) {
    entityData.set(OWNER, Optional.of(owner.getUUID()));
    this.slot = slot;
    this.lifetime = lifetime;
    for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
      setItemSlot(s, owner.getItemBySlot(s).copy());
      setDropChance(s, 0f);
    }
    for (ItemStack st : owner.getInventory().items) {
      if (!st.isEmpty() && (st.getItem() instanceof net.minecraft.world.item.TieredItem || st.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem
          || st.getItem() instanceof net.minecraft.world.item.TridentItem || st.getItem() instanceof com.arcane.item.ScepterItem)) weapons.add(st.copyWithCount(1));
    }
    setItemSlot(EquipmentSlot.MAINHAND, owner.getMainHandItem().copy());
    setDropChance(EquipmentSlot.MAINHAND, 0f);
    setCustomName(owner.getName());
    setCustomNameVisible(false);
  }

  private Player ownerEntity() {
    return owner().map(id -> level().getPlayerByUUID(id)).orElse(null);
  }

  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide) return;
    Player owner = ownerEntity();
    if (owner == null || !owner.isAlive() || --lifetime <= 0 || owner.level() != level()) { vanish(); return; }
    // Anda ao lado do dono, na mesma direcao: a posicao alvo gira com a visao dele.
    double yaw = Math.toRadians(owner.getYRot() + 90 + (slot - 1) * 70.0);
    Vec3 want = owner.position().add(Math.cos(yaw) * 2.6, 0, Math.sin(yaw) * 2.6);
    double far = want.distanceTo(position());
    if (far > 16) teleportTo(want.x, want.y, want.z);
    else if (far > 0.8) getNavigation().moveTo(want.x, want.y, want.z, owner.isSprinting() ? 1.6 : 1.1);
    else getNavigation().stop();
    setSprinting(owner.isSprinting());
    setShiftKeyDown(owner.isShiftKeyDown());
    if (far < 3) getLookControl().setLookAt(position().add(owner.getLookAngle().scale(8)).add(0, getEyeHeight(), 0));
    // Ataque: quando o dono golpeia, o clone golpeia com outra arma.
    if (owner.swingTime > 0 && lastOwnerSwing == 0) {
      if (!weapons.isEmpty()) setItemSlot(EquipmentSlot.MAINHAND, weapons.get(random.nextInt(weapons.size())).copy());
      swing(InteractionHand.MAIN_HAND, true);
    }
    lastOwnerSwing = owner.swingTime;
  }

  /** O dono conjurou: o clone finge outra magia (gesto e faiscas de cor sorteada). */
  public void mimicCast() {
    if (!(level() instanceof ServerLevel sl)) return;
    swing(InteractionHand.MAIN_HAND, true);
    var p = switch (random.nextInt(4)) {
      case 0 -> ParticleTypes.FLAME;
      case 1 -> ParticleTypes.SNOWFLAKE;
      case 2 -> ParticleTypes.ELECTRIC_SPARK;
      default -> ParticleTypes.WITCH;
    };
    Vec3 hand = position().add(getLookAngle().scale(0.8)).add(0, getEyeHeight() - 0.3, 0);
    sl.sendParticles(p, hand.x, hand.y, hand.z, 14, 0.2, 0.2, 0.2, 0.05);
  }

  private void vanish() {
    if (level() instanceof ServerLevel sl) {
      sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 16, 0.3, 0.6, 0.3, 0.02);
      sl.playSound(null, blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.6f, 1.4f);
    }
    discard();
  }

  /** Qualquer golpe desfaz a ilusao (sem dano de verdade, sem drop). */
  @Override
  public boolean hurt(DamageSource source, float amount) {
    if (level().isClientSide || isInvulnerableTo(source)) return false;
    vanish();
    return true;
  }

  /** Nao fere ninguem nem interage com nada. */
  @Override public boolean doHurtTarget(Entity target) { return false; }
  @Override public boolean isPushable() { return false; }
  @Override protected void doPush(Entity entity) { }
  @Override public boolean canPickUpLoot() { return false; }
  @Override public boolean shouldDropExperience() { return false; }
  @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) { }
  @Override public boolean removeWhenFarAway(double distance) { return false; }
  @Override public boolean isAlliedTo(Entity other) { return other instanceof Player p && owner().map(p.getUUID()::equals).orElse(false); }
  @Override public boolean canBeSeenAsEnemy() { return true; }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    owner().ifPresent(id -> tag.putUUID("IllusionOwner", id));
    tag.putInt("IllusionLife", lifetime);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    if (tag.hasUUID("IllusionOwner")) entityData.set(OWNER, Optional.of(tag.getUUID("IllusionOwner")));
    lifetime = tag.getInt("IllusionLife");
  }

  /** Para os criaturas que miram o dono: escolhe um clone (ou o dono) ao acaso. */
  public static LivingEntity confuse(LivingEntity owner, net.minecraft.util.RandomSource rnd) {
    List<IllusionEntity> mine = owner.level().getEntitiesOfClass(IllusionEntity.class, owner.getBoundingBox().inflate(16),
        c -> c.owner().map(owner.getUUID()::equals).orElse(false));
    if (mine.isEmpty()) return owner;
    int pick = rnd.nextInt(mine.size() + 1);
    return pick == mine.size() ? owner : mine.get(pick);
  }
}
