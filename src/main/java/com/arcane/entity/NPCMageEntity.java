package com.arcane.entity;

import com.arcane.discovery.DiscoveryService;
import com.arcane.execution.CastingService;
import com.arcane.inscription.Inscription;
import com.arcane.item.GlyphNoteItem;
import com.arcane.npc.MageElement;
import com.arcane.npc.NPCArcaneComponent;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Mago NPC. A variante (0..17) define a skin e o elemento (variante % 6), e o elemento define a magia.
 * Conjura pelo mesmo CastingService do jogador; quem assiste suspeita dos glifos usados.
 */
public class NPCMageEntity extends Monster {
  private static final EntityDataAccessor<Integer> DATA_CASTING =
      SynchedEntityData.defineId(NPCMageEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> DATA_VARIANT =
      SynchedEntityData.defineId(NPCMageEntity.class, EntityDataSerializers.INT);
  private static final double WITNESS_RANGE = 24.0;

  private final NPCArcaneComponent arcane = new NPCArcaneComponent();

  public NPCMageEntity(EntityType<? extends Monster> type, Level level) {
    super(type, level);
  }

  public static AttributeSupplier.Builder createAttributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 24.0)
        .add(Attributes.MOVEMENT_SPEED, 0.25)
        .add(Attributes.ATTACK_DAMAGE, 2.0)
        .add(Attributes.FOLLOW_RANGE, 24.0);
  }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    entityData.define(DATA_VARIANT, 0);
    entityData.define(DATA_CASTING, 0);
  }

  /** Ticks restantes da animacao de conjuracao (bracos erguidos, brilho forte). */
  public int castingTicks() { return entityData.get(DATA_CASTING); }

  public void startCastAnimation() { entityData.set(DATA_CASTING, 14); }

  public int variant() { return entityData.get(DATA_VARIANT); }

  public void setVariant(int variant) {
    int v = Math.floorMod(variant, MageElement.VARIANTS);
    entityData.set(DATA_VARIANT, v);
    arcane.setElement(MageElement.forVariant(v));
  }

  public MageElement element() { return arcane.element(); }

  @Nullable
  @Override
  public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                      @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
    setVariant(random.nextInt(MageElement.VARIANTS));
    return super.finalizeSpawn(level, difficulty, reason, data, tag);
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putInt("Variant", variant());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    setVariant(tag.getInt("Variant"));
  }

  @Override
  public Component getTypeName() {
    return Component.translatable("entity.arcane.npc_mage." + element().id());
  }

  @Override
  public void tick() {
    super.tick();
    if (!level().isClientSide && castingTicks() > 0) entityData.set(DATA_CASTING, castingTicks() - 1);
    if (level().isClientSide || tickCount % com.arcane.config.ArcaneConfig.MAGE_CAST_INTERVAL_TICKS.get() != 0) return;

    MageElement element = element();
    Player target = level().getNearestPlayer(this, Math.max(element.castRange(), 6.0));
    if (target == null || !hasLineOfSight(target) || distanceTo(target) > element.castRange()) return;

    lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
    Inscription spell = arcane.selectSpell();
    if (CastingService.tryCast(this, spell, (ServerLevel) level()).success()) {
      startCastAnimation();
      for (ServerPlayer witness : ((ServerLevel) level()).getPlayers(p -> p.distanceToSqr(this) <= WITNESS_RANGE * WITNESS_RANGE)) {
        DiscoveryService.onWitness(witness, spell);
      }
    }
  }

  /** Dropa uma nota com um dos glifos da propria magia: e assim que o jogador passa a identifica-lo. */
  @Override
  protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHit) {
    super.dropCustomDeathLoot(source, looting, recentlyHit);
    if (level().isClientSide) return;
    List<ResourceLocation> glyphs = element().glyphIds();
    ResourceLocation glyph = glyphs.get(random.nextInt(glyphs.size()));
    spawnAtLocation(GlyphNoteItem.createForGlyph(glyph));
  }

  @Override
  protected void registerGoals() {
    this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.0, false));
    this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.8));
    this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Player.class, true));
  }
}
