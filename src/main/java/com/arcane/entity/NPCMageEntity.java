package com.arcane.entity;

import com.arcane.discovery.DiscoveryService;
import com.arcane.execution.CastingService;
import com.arcane.item.GlyphNoteItem;
import com.arcane.npc.MageBrain;
import com.arcane.npc.MageElement;
import com.arcane.npc.MagePersonality;
import com.arcane.npc.NPCArcaneComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Mago NPC. A variante (0..17) define a skin e a escola (variante % 6); a escola traz um grimorio com magias de
 * ataque, area, defesa, fuga, apoio e truque. A personalidade (hostil, defensivo, inofensivo, estudioso, trapaceiro)
 * decide quem ele ataca e como luta; a escolha de cada magia e do {@link MageBrain}. Conjura pelo mesmo
 * CastingService do jogador, gasta mana propria (que regenera) e cada magia tem a sua espera. Quem assiste suspeita
 * dos glifos usados.
 */
public class NPCMageEntity extends Monster {
  private static final EntityDataAccessor<Integer> DATA_CASTING =
      SynchedEntityData.defineId(NPCMageEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> DATA_VARIANT =
      SynchedEntityData.defineId(NPCMageEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> DATA_PERSONALITY =
      SynchedEntityData.defineId(NPCMageEntity.class, EntityDataSerializers.INT);
  private static final double WITNESS_RANGE = 24.0, GUARD_RADIUS = 6.0, WARN_RADIUS = 11.0;
  public static final float MAX_MANA = 100f;

  private final NPCArcaneComponent arcane = new NPCArcaneComponent();
  private int[] cooldowns = new int[8];
  private float mana = MAX_MANA;
  private BlockPos home;
  /** Ticks fugindo de quem o feriu (inofensivo, estudioso). */
  private int fleeTicks;
  @Nullable private LivingEntity fleeFrom;
  private long lastWarn;

  public NPCMageEntity(EntityType<? extends Monster> type, Level level) {
    super(type, level);
  }

  public static AttributeSupplier.Builder createAttributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 24.0)
        .add(Attributes.MOVEMENT_SPEED, 0.27)
        .add(Attributes.ATTACK_DAMAGE, 2.0)
        .add(Attributes.FOLLOW_RANGE, 24.0);
  }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    entityData.define(DATA_VARIANT, 0);
    entityData.define(DATA_CASTING, 0);
    entityData.define(DATA_PERSONALITY, 0);
  }

  /** Ticks restantes da animacao de conjuracao (bracos erguidos, brilho forte). */
  public int castingTicks() { return entityData.get(DATA_CASTING); }

  public void startCastAnimation() { entityData.set(DATA_CASTING, 14); }

  public int variant() { return entityData.get(DATA_VARIANT); }

  public void setVariant(int variant) {
    int v = Math.floorMod(variant, MageElement.VARIANTS);
    entityData.set(DATA_VARIANT, v);
    arcane.setElement(MageElement.forVariant(v));
    cooldowns = new int[element().spells().size()];
  }

  public MageElement element() { return arcane.element(); }

  public MagePersonality personality() { return MagePersonality.byOrdinal(entityData.get(DATA_PERSONALITY)); }

  public void setPersonality(MagePersonality p) {
    entityData.set(DATA_PERSONALITY, p.ordinal());
    var hp = getAttribute(Attributes.MAX_HEALTH);
    if (hp != null) {
      hp.setBaseValue(24.0 * p.healthFactor());
      setHealth(getMaxHealth());
    }
  }

  public float mana() { return mana; }

  @Nullable
  public BlockPos home() { return home; }

  @Nullable
  @Override
  public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                      @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
    setVariant(random.nextInt(MageElement.VARIANTS));
    setPersonality(MagePersonality.random(random));
    home = blockPosition();
    return super.finalizeSpawn(level, difficulty, reason, data, tag);
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putInt("Variant", variant());
    tag.putInt("Personality", personality().ordinal());
    tag.putFloat("Mana", mana);
    if (home != null) tag.putLong("Home", home.asLong());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    setVariant(tag.getInt("Variant"));
    entityData.set(DATA_PERSONALITY, tag.getInt("Personality"));
    mana = tag.contains("Mana") ? tag.getFloat("Mana") : MAX_MANA;
    home = tag.contains("Home") ? BlockPos.of(tag.getLong("Home")) : null;
  }

  @Override
  public Component getTypeName() {
    return Component.translatable("entity.arcane.npc_mage.titled",
        Component.translatable("entity.arcane.npc_mage." + element().id()),
        Component.translatable("mage.arcane.personality." + personality().id()));
  }

  /** So os hostis somem ao ficar longe (os outros moram onde estao). */
  @Override
  public boolean removeWhenFarAway(double distance) {
    return personality() == MagePersonality.HOSTILE && super.removeWhenFarAway(distance);
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    boolean hurt = super.hurt(source, amount);
    if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
      MagePersonality p = personality();
      if (p == MagePersonality.HARMLESS || (p == MagePersonality.SCHOLAR && getHealth() < getMaxHealth() * 0.6f)) {
        fleeTicks = 100;
        fleeFrom = attacker;
        setTarget(null);
        say(attacker, "flee");
      } else {
        setTarget(attacker);
      }
    }
    return hurt;
  }

  private void say(LivingEntity to, String line) {
    if (to instanceof ServerPlayer sp) {
      sp.displayClientMessage(Component.translatable("mage.arcane.say." + personality().id() + "." + line, getDisplayName())
          .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY), true);
    }
  }

  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide) return;
    if (castingTicks() > 0) entityData.set(DATA_CASTING, castingTicks() - 1);
    if (home == null) home = blockPosition();
    mana = Math.min(MAX_MANA, mana + 0.35f);
    for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i]--;
    if (fleeTicks > 0) fleeTicks--;
    if (tickCount % 10 != 0) return;
    ServerLevel sl = (ServerLevel) level();
    MagePersonality who = personality();
    // defensivo: avisa quem se aproxima e ataca quem entra no circulo da guarda
    if (who == MagePersonality.DEFENSIVE && getTarget() == null) {
      Player near = sl.getNearestPlayer(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, WARN_RADIUS,
          p -> !((Player) p).isCreative() && !p.isSpectator());
      if (near != null) {
        if (near.position().distanceTo(Vec3.atCenterOf(home)) <= GUARD_RADIUS) setTarget(near);
        else if (sl.getGameTime() - lastWarn > 200) { lastWarn = sl.getGameTime(); say(near, "warn"); }
      }
    }
    if (tickCount % Math.max(10, com.arcane.config.ArcaneConfig.MAGE_CAST_INTERVAL_TICKS.get() / 2) != 0) return;
    LivingEntity target = getTarget() != null && getTarget().isAlive() ? getTarget() : null;
    // aliado ferido: o jogador mais ferido por perto (so para apoio)
    Player hurtAlly = null;
    if (who == MagePersonality.HARMLESS || who == MagePersonality.DEFENSIVE || who == MagePersonality.SCHOLAR) {
      for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(10))) {
        if (p == target || p.getHealth() >= p.getMaxHealth() * 0.7f) continue;
        if (hurtAlly == null || p.getHealth() < hurtAlly.getHealth()) hurtAlly = p;
      }
    }
    int enemies = target == null ? 0 : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(5), p -> !p.isSpectator() && !p.isCreative()).size();
    var situation = new MageBrain.Situation(getHealth() / getMaxHealth(), target == null ? 99 : distanceTo(target), enemies, hurtAlly != null, target != null);
    var spell = MageBrain.choose(element(), who, situation, cooldowns, random);
    if (spell == null || mana < 15) return;
    LivingEntity aim = spell.role() == MageElement.Role.SUPPORT ? hurtAlly : target;
    if (!MageBrain.selfCast(spell)) {
      if (aim == null || !hasLineOfSight(aim)) return;
      lookAt(EntityAnchorArgument.Anchor.EYES, aim.getEyePosition());
    }
    if (CastingService.tryCast(this, spell.inscription(), sl).success()) {
      startCastAnimation();
      mana -= 15 + spell.cooldown() / 10f;
      cooldowns[element().spells().indexOf(spell)] = spell.cooldown();
      if (spell.role() == MageElement.Role.SUPPORT && aim != null) say(aim, "heal");
      for (ServerPlayer witness : sl.getPlayers(p -> p.distanceToSqr(this) <= WITNESS_RANGE * WITNESS_RANGE)) {
        DiscoveryService.onWitness(witness, spell.inscription());
      }
    }
  }

  /** Dropa uma nota com um dos glifos do seu grimorio: e assim que o jogador passa a identifica-lo. */
  @Override
  protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
    super.dropCustomDeathLoot(source, looting, recentlyHit);
    if (level().isClientSide) return;
    List<ResourceLocation> glyphs = element().glyphIds();
    spawnAtLocation(GlyphNoteItem.createForGlyph(glyphs.get(random.nextInt(glyphs.size()))));
    if (personality() == MagePersonality.SCHOLAR) spawnAtLocation(GlyphNoteItem.createForGlyph(glyphs.get(random.nextInt(glyphs.size()))));
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
    goalSelector.addGoal(2, new MageMoveGoal(this));
    goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.7));
    goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 10f));
    goalSelector.addGoal(9, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
    targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
        p -> personality() == MagePersonality.HOSTILE || (personality() == MagePersonality.TRICKSTER && p.distanceTo(this) < 14)));
  }

  /**
   * Movimento de mago: mantem a distancia preferida do alvo (recua se ele chega perto, avanca se esta longe e
   * circula de lado no meio); foge de quem o feriu quando a personalidade manda; o defensivo nao se afasta da casa.
   */
  static final class MageMoveGoal extends Goal {
    private final NPCMageEntity mage;
    private int repath;
    private int strafe = 1;

    MageMoveGoal(NPCMageEntity mage) {
      this.mage = mage;
      setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
      return mage.fleeTicks > 0 && mage.fleeFrom != null || mage.getTarget() != null && mage.getTarget().isAlive();
    }

    @Override
    public void tick() {
      if (--repath > 0) return;
      repath = 10;
      if (mage.fleeTicks > 0 && mage.fleeFrom != null) {
        Vec3 away = mage.position().subtract(mage.fleeFrom.position()).normalize().scale(10).add(mage.position());
        mage.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
        return;
      }
      LivingEntity t = mage.getTarget();
      if (t == null) return;
      mage.getLookControl().setLookAt(t, 30f, 30f);
      double d = mage.distanceTo(t);
      double want = MageBrain.preferredDistance(mage.element(), mage.personality());
      Vec3 to = t.position().subtract(mage.position()).normalize();
      Vec3 goal;
      if (d < want - 1.5) goal = mage.position().subtract(to.scale(5));
      else if (d > mage.element().castRange() - 1) goal = t.position();
      else {
        if (mage.random.nextInt(4) == 0) strafe = -strafe;
        goal = mage.position().add(new Vec3(-to.z, 0, to.x).scale(3 * strafe));
      }
      if (mage.personality() == MagePersonality.DEFENSIVE && mage.home != null && goal.distanceTo(Vec3.atCenterOf(mage.home)) > 10) {
        goal = Vec3.atCenterOf(mage.home);
      }
      mage.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.05);
    }
  }
}
