package com.arcane.entity;

import com.arcane.execution.SpellElement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Guardiao de Ruina (Fase N): mini-chefe que so aparece nas Ruinas Arcanas (Salao do Ritual), sem escala por jogador,
 * vida modesta. Quebra-cabeca de 2 passos, configurado em {@code data/arcane/guardians/*.json}: uma casca de gelo
 * absorve TUDO; ela so racha com a reacao do JSON (padrao: Vapor = Fogo num alvo Molhado). Rachada, abre uma janela
 * de vulnerabilidade (dano x1,5 por 8 s) e depois volta. A afinidade dele (gelo imune, fogo x1,25) esta em
 * {@code affinities/ruin_guardian.json}. Recompensa modesta: tinta, material e chance de uma nota de glifo NAO original.
 * Usa o {@link PhaseGate} extraido da Hidra. NAO visto em jogo.
 */
public class RuinGuardianEntity extends Monster {
  private static final EntityDataAccessor<Integer> WINDOW = SynchedEntityData.defineId(RuinGuardianEntity.class, EntityDataSerializers.INT);

  private ResourceLocation defId = RuinGuardians.DEFAULT.id();
  private BlockPos home;

  /** Casca de pe = janela fechada. */
  static final PhaseGate<RuinGuardianEntity> GATE = new PhaseGate<>(g -> g.windowTicks() <= 0,
      s -> s.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY), g -> g.windowTicks() > 0, 1f);

  public RuinGuardianEntity(EntityType<? extends RuinGuardianEntity> type, Level level) {
    super(type, level);
    xpReward = 20;
    setPersistenceRequired();
  }

  public static AttributeSupplier.Builder createAttributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, RuinGuardians.DEFAULT.health())
        .add(Attributes.ATTACK_DAMAGE, RuinGuardians.DEFAULT.attack())
        .add(Attributes.MOVEMENT_SPEED, 0.22)
        .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
        .add(Attributes.FOLLOW_RANGE, 20);
  }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    entityData.define(WINDOW, 0);
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
    goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
    goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
    goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    targetSelector.addGoal(1, new HurtByTargetGoal(this));
    targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
  }

  /** Aplica a definicao (vida e ataque fixos, sem escala) e guarda o Salao como casa. */
  public void setup(RuinGuardians.Def def, BlockPos home) {
    this.defId = def.id();
    this.home = home == null ? null : home.immutable();
    var hp = getAttribute(Attributes.MAX_HEALTH);
    if (hp != null) hp.setBaseValue(def.health());
    var atk = getAttribute(Attributes.ATTACK_DAMAGE);
    if (atk != null) atk.setBaseValue(def.attack());
    setHealth(getMaxHealth());
    if (home != null) restrictTo(home, 10);
  }

  public RuinGuardians.Def def() { return RuinGuardians.get(defId); }

  public int windowTicks() { return entityData.get(WINDOW); }

  public boolean shellUp() { return GATE.shellUp(this); }

  /** Reacao causada nele por uma conjuracao (CastScope): a do JSON racha a casca. */
  public void onReaction(String id) {
    if (level().isClientSide || !shellUp() || !def().breakReactions().contains(id)) return;
    entityData.set(WINDOW, def().windowTicks());
    if (level() instanceof ServerLevel sl) {
      com.arcane.fx.Fx.burst(sl, this, position().add(0, getBbHeight() * 0.6, 0), SpellElement.FROST, com.arcane.fx.Fx.Role.IMPACT, 30, 0.5, 0.3, 1.3f, 0xDFF6FF);
      com.arcane.fx.Fx.sound(sl, position(), new ResourceLocation("arcane", "reaction.shatter"), 1.2f, 0.8f);
    }
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    if (GATE.blocks(this, source)) {
      // a casca absorve: so um brilho gelado (uma vez a cada 4 ticks)
      if (level() instanceof ServerLevel sl && tickCount % 4 == 0) {
        com.arcane.fx.Fx.burst(sl, this, position().add(0, getBbHeight() * 0.5, 0), SpellElement.FROST, com.arcane.fx.Fx.Role.BODY, 6, 0.4, 0.05);
        com.arcane.fx.Fx.sound(sl, position(), new ResourceLocation("minecraft", "block.glass.hit"), 0.8f, 1.3f);
      }
      return false;
    }
    float scaled = shellUp() ? amount : amount * def().windowMultiplier();
    return super.hurt(source, scaled);
  }

  @Override
  public void aiStep() {
    super.aiStep();
    if (level().isClientSide) return;
    int w = windowTicks();
    if (w > 0) {
      entityData.set(WINDOW, w - 1);
      if (w == 1 && level() instanceof ServerLevel sl) {
        // a casca volta
        com.arcane.fx.Fx.burst(sl, this, position().add(0, getBbHeight() * 0.5, 0), SpellElement.FROST, com.arcane.fx.Fx.Role.BODY, 20, 0.5, 0.05);
      }
    } else if (tickCount % 20 == 0 && level() instanceof ServerLevel sl) {
      com.arcane.fx.Fx.burst(sl, this, position().add(0, getBbHeight() * 0.5, 0), SpellElement.FROST, com.arcane.fx.Fx.Role.BODY, 4, 0.45, 0.01);
    }
  }

  // ------------------------------------------------------------------ recompensa
  /** Tinta, material e, com chance, uma nota de glifo de tier ate {@code glyph_max_tier} (nunca original). */
  public List<ItemStack> rewards(net.minecraft.util.RandomSource rand) {
    var d = def();
    List<ItemStack> out = new ArrayList<>();
    for (var drop : List.of(d.ink(), d.material())) {
      var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(drop.item());
      int n = drop.min() + (drop.max() > drop.min() ? rand.nextInt(drop.max() - drop.min() + 1) : 0);
      if (item != null && n > 0) out.add(new ItemStack(item, n));
    }
    if (rand.nextDouble() < d.glyphChance()) {
      List<ResourceLocation> pool = new ArrayList<>();
      for (var g : com.arcane.glyph.GlyphRegistry.all().values()) {
        if (g.tier() <= d.glyphMaxTier() && g.tier() < 5 && !g.families().contains("original")) pool.add(g.id());
      }
      if (!pool.isEmpty()) {
        pool.sort(java.util.Comparator.comparing(ResourceLocation::toString));
        var note = new ItemStack(com.arcane.registry.ModItems.GLYPH_NOTE.get());
        note.getOrCreateTag().putString("glyph", pool.get(rand.nextInt(pool.size())).toString());
        out.add(note);
      }
    }
    return out;
  }

  @Override
  protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
    super.dropCustomDeathLoot(source, looting, hit);
    for (ItemStack s : rewards(random)) spawnAtLocation(s);
  }

  @Override
  public boolean removeWhenFarAway(double d) { return false; }

  // ------------------------------------------------------------------ salvar
  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putString("GuardianDef", defId.toString());
    if (home != null) tag.putLong("GuardianHome", home.asLong());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    var id = ResourceLocation.tryParse(tag.getString("GuardianDef"));
    if (id != null) defId = id;
    if (tag.contains("GuardianHome")) {
      home = BlockPos.of(tag.getLong("GuardianHome"));
      restrictTo(home, 10);
    }
  }
}
