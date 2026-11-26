package com.arcane.entity;

import com.arcane.training.DamageMeter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * Boneco de Treino Arcano (Fase H): o instrumento de medida das outras fases. Nunca morre (a vida para em 1 HP) e volta
 * ao maximo depois de {@link #RESET_TICKS} sem dano. Mede todo golpe com um {@link DamageMeter} e mostra, num letreiro
 * sobre a cabeca, a barra de coracoes, o ultimo golpe, a media de coracoes por segundo, as tags de estado e o custo de
 * mana da ultima conjuracao que o acertou.
 *
 * <p>Mao vazia: alterna a armadura (nenhuma, ferro, diamante, netherita). Agachado + mao vazia: alterna o perfil de
 * afinidade elemental. Agachado + soco de mao vazia: recolhe o boneco (devolve o item). {@code /kill} remove.
 */
public class ArcaneDummyEntity extends Mob {
  /** Vida maxima: 40 HP (20 coracoes). PROVISORIO (docs/PERGUNTAS.md, "Numeros provisorios"). */
  public static final double MAX_HEALTH = 40.0;
  /** 5 s sem dano: volta ao maximo. */
  public static final int RESET_TICKS = 100;

  /** Bits das tags de estado mostradas no letreiro (ordem = ordem na tela). */
  public static final String[] TAGS = {"wet", "burning", "frozen", "marked", "poisoned", "slowed"};

  private static final EntityDataAccessor<Float> LAST_HIT = SynchedEntityData.defineId(ArcaneDummyEntity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Float> PER_SECOND = SynchedEntityData.defineId(ArcaneDummyEntity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Integer> MANA = SynchedEntityData.defineId(ArcaneDummyEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(ArcaneDummyEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> ARMOR = SynchedEntityData.defineId(ArcaneDummyEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<String> PROFILE = SynchedEntityData.defineId(ArcaneDummyEntity.class, EntityDataSerializers.STRING);

  /** Conjuntos de armadura, na ordem do clique. */
  public static final List<List<Item>> ARMOR_SETS = List.of(
      List.of(),
      List.of(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS),
      List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS),
      List.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS));
  public static final String[] ARMOR_NAMES = {"none", "iron", "diamond", "netherite"};

  /** Perfis de afinidade na ordem do clique agachado ("" = neutro). A Fase I liga a tabela de afinidades aqui. */
  public static Supplier<List<String>> PROFILES = com.arcane.affinity.Affinities::dummyCycle;

  private DamageMeter meter;

  public ArcaneDummyEntity(EntityType<? extends ArcaneDummyEntity> type, Level level) {
    super(type, level);
    if (!level.isClientSide) meter = DamageMeter.track(this);
    setPersistenceRequired();
  }

  public static AttributeSupplier.Builder createAttributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, MAX_HEALTH)
        .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
        .add(Attributes.MOVEMENT_SPEED, 0.0);
  }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    entityData.define(LAST_HIT, 0f);
    entityData.define(PER_SECOND, 0f);
    entityData.define(MANA, -1);
    entityData.define(STATE, 0);
    entityData.define(ARMOR, 0);
    entityData.define(PROFILE, "");
  }

  public DamageMeter meter() { return meter; }

  public float syncedLastHit() { return entityData.get(LAST_HIT); }

  public float syncedPerSecond() { return entityData.get(PER_SECOND); }

  public int syncedMana() { return entityData.get(MANA); }

  public int syncedState() { return entityData.get(STATE); }

  public int armorTier() { return entityData.get(ARMOR); }

  public String affinityProfile() { return entityData.get(PROFILE); }

  public void setAffinityProfile(String id) { entityData.set(PROFILE, id == null ? "" : id); }

  // ------------------------------------------------------------------ nunca morre
  @Override
  public void setHealth(float health) {
    super.setHealth(isRemoved() ? health : Math.max(1f, health));
  }

  @Override
  public void kill() {
    discard();
  }

  @Override
  public boolean isPushable() { return false; }

  @Override
  protected void doPush(net.minecraft.world.entity.Entity entity) {}

  @Override
  public boolean removeWhenFarAway(double distance) { return false; }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    // Agachado + soco de mao vazia: recolhe o boneco
    if (source.getDirectEntity() instanceof Player p && p.isShiftKeyDown() && p.getMainHandItem().isEmpty()
        && source.getEntity() == p && !level().isClientSide) {
      spawnAtLocation(new ItemStack(com.arcane.registry.ModItems.TRAINING_DUMMY.get()));
      for (EquipmentSlot slot : armorSlots()) setItemSlot(slot, ItemStack.EMPTY);
      discard();
      return false;
    }
    return super.hurt(source, amount);
  }

  @Override
  protected void dropEquipment() {}

  // ------------------------------------------------------------------ interacoes
  @Override
  protected InteractionResult mobInteract(Player player, InteractionHand hand) {
    if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
    if (level().isClientSide) return InteractionResult.SUCCESS;
    if (player.isShiftKeyDown()) cycleProfile();
    else setArmorTier(armorTier() + 1);
    return InteractionResult.CONSUME;
  }

  public void cycleProfile() {
    List<String> all = PROFILES.get();
    if (all.isEmpty()) return;
    int i = all.indexOf(affinityProfile());
    setAffinityProfile(all.get((i + 1) % all.size()));
  }

  public void setArmorTier(int tier) {
    int t = Math.floorMod(tier, ARMOR_SETS.size());
    entityData.set(ARMOR, t);
    List<Item> set = ARMOR_SETS.get(t);
    EquipmentSlot[] slots = armorSlots();
    for (int i = 0; i < slots.length; i++) {
      setItemSlot(slots[i], set.isEmpty() ? ItemStack.EMPTY : new ItemStack(set.get(i)));
      setDropChance(slots[i], 0f);
    }
  }

  private static EquipmentSlot[] armorSlots() {
    return new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
  }

  // ------------------------------------------------------------------ medida
  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide || meter == null) return;
    long now = level().getGameTime();
    if (!meter.idle() && meter.sinceLast(now) >= RESET_TICKS) resetNow();
    entityData.set(LAST_HIT, meter.lastHit());
    entityData.set(PER_SECOND, meter.perSecond(now));
    entityData.set(MANA, meter.lastManaCost());
    entityData.set(STATE, stateBits());
  }

  /** Volta ao maximo e fecha a sessao do medidor (os numeros ficam ate o proximo golpe). */
  public void resetNow() {
    setHealth(getMaxHealth());
    clearFire();
    setTicksFrozen(0);
    if (meter != null) meter.markIdle();
  }

  public int stateBits() {
    int bits = 0;
    if (isInWaterOrRain() || com.arcane.execution.Wetness.isSoaked(this)) bits |= 1;
    if (isOnFire()) bits |= 1 << 1;
    if (getTicksFrozen() > 0) bits |= 1 << 2;
    if (com.arcane.execution.Marks.isMarked(this)) bits |= 1 << 3;
    if (hasEffect(MobEffects.POISON) || hasEffect(MobEffects.WITHER)) bits |= 1 << 4;
    if (hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) bits |= 1 << 5;
    return bits;
  }

  // ------------------------------------------------------------------ salvar
  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putInt("ArcaneArmorTier", armorTier());
    tag.putString("ArcaneAffinity", affinityProfile());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    entityData.set(ARMOR, Math.floorMod(tag.getInt("ArcaneArmorTier"), ARMOR_SETS.size()));
    setAffinityProfile(tag.getString("ArcaneAffinity"));
  }
}
