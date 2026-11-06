package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.ambient.AmbientFieldResolver;
import com.arcane.entity.NPCMageEntity;
import com.arcane.execution.CastingService;
import com.arcane.execution.EnvironmentalSnapshot;
import com.arcane.glyph.GlyphCategory;
import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphHandlerRegistry;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.inscribing.InscribingService;
import com.arcane.inscribing.InscriptionAnchor;
import com.arcane.inscribing.WorldInscriptionIndex;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.item.GrimoireItem;
import com.arcane.npc.MageElement;
import com.arcane.registry.ModBlocks;
import com.arcane.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Testes dentro de um mundo real (./gradlew runGameTestServer).
 * Estrutura: data/arcane/structures/empty.nbt (tools/gen_test_structure.py), 7x5x7, chao de pedra em y=0.
 * Convencao: conjuradores olham para +Z; os alvos ficam a frente deles.
 */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class ArcaneGameTests {

  // ------------------------------------------------------------------ utilitarios
  private static GlyphInstance g(String id) {
    return new GlyphInstance(new ResourceLocation("arcane", id));
  }

  private static GlyphInstance g(String id, String key, double value) {
    return new GlyphInstance(new ResourceLocation("arcane", id), Map.of(key, value));
  }

  private static void floor(GameTestHelper h) {
    for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
  }

  private static Pig pig(GameTestHelper h, int x, int z) {
    Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(x, 1, z));
    pig.setYRot(0);
    pig.setXRot(0);
    return pig;
  }

  private static void cast(GameTestHelper h, LivingEntity caster, GlyphInstance... glyphs) {
    caster.setYRot(0);
    caster.setXRot(0);
    var result = CastingService.tryCast(caster, new Inscription(List.of(glyphs)), h.getLevel());
    h.assertTrue(result.success(), "a conjuracao falhou: " + result.message());
  }

  private static void assertHurt(GameTestHelper h, LivingEntity e, float before, String what) {
    h.assertTrue(e.getHealth() < before, what + " deveria ter sido ferido (vida " + e.getHealth() + ")");
  }

  private static void assertUnhurt(GameTestHelper h, LivingEntity e, float before, String what) {
    h.assertTrue(e.getHealth() >= before, what + " nao deveria ter sido ferido (vida " + e.getHealth() + ")");
  }

  // ------------------------------------------------------------------ cobertura dos glifos
  /** Todo glifo do datapack precisa de um handler no codigo (modificadores nao precisam). */
  @GameTest(template = "empty")
  public static void everyGlyphHasAHandler(GameTestHelper h) {
    for (Map.Entry<ResourceLocation, GlyphDefinition> e : GlyphRegistry.all().entrySet()) {
      GlyphDefinition def = e.getValue();
      ResourceLocation handler = def.behavior().handler();
      boolean ok = switch (def.category()) {
        case FORM -> GlyphHandlerRegistry.getForm(handler).isPresent();
        case EFFECT -> GlyphHandlerRegistry.getEffect(handler).isPresent();
        case FILTER -> GlyphHandlerRegistry.getFilter(handler).isPresent();
        case CONDITION -> GlyphHandlerRegistry.getCondition(handler).isPresent();
        case ACTIVATOR -> GlyphHandlerRegistry.getActivator(handler).isPresent();
        case MODIFIER -> def.modifierScope().isPresent();
      };
      h.assertTrue(ok, "glifo sem handler: " + e.getKey());
    }
    h.succeed();
  }

  // ------------------------------------------------------------------ efeitos em si mesmo
  @GameTest(template = "empty")
  public static void healRestoresHealth(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    p.setHealth(3.0f);
    cast(h, p, g("self"), g("heal", "amount", 4.0));
    h.assertTrue(p.getHealth() >= 7.0f, "cura deveria dar +4, vida " + p.getHealth());
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void potionEffectsApply(GameTestHelper h) {
    floor(h);
    Map<String, MobEffect> effects = Map.of("slow", MobEffects.MOVEMENT_SLOWDOWN, "poison", MobEffects.POISON,
        "speed", MobEffects.MOVEMENT_SPEED, "levitate", MobEffects.LEVITATION, "shield", MobEffects.ABSORPTION);
    int x = 0;
    for (Map.Entry<String, MobEffect> e : effects.entrySet()) {
      Pig p = pig(h, x++, 3);
      cast(h, p, g("self"), g(e.getKey()));
      h.assertTrue(p.hasEffect(e.getValue()), "glifo " + e.getKey() + " nao aplicou o efeito");
    }
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void powerAddsALevelToPotionEffects(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    cast(h, p, g("self"), g("speed"), g("power"));
    h.assertTrue(p.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() == 2, "Velocidade nivel 1 + Potencia deveria ser amplificador 2");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void blinkTeleportsForward(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 0);
    double before = p.getZ();
    cast(h, p, g("self"), g("blink", "distance", 5.0));
    h.assertTrue(p.getZ() - before >= 3.0, "o salto deveria avancar ao menos 3 blocos, avancou " + (p.getZ() - before));
    h.succeed();
  }

  // ------------------------------------------------------------------ formas
  @GameTest(template = "empty")
  public static void auraHitsOthersWithinRadiusButNotTheCaster(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Pig near = pig(h, 3, 5);
    Pig far = pig(h, 0, 0);
    float a = caster.getHealth(), b = near.getHealth(), c = far.getHealth();
    cast(h, caster, g("aura", "radius", 3.0), g("damage", "amount", 3.0));
    assertUnhurt(h, caster, a, "o conjurador");
    assertHurt(h, near, b, "o alvo a 2 blocos");
    assertUnhurt(h, far, c, "o alvo fora do raio");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void rayHitsOnlyTheFirstCreature(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig first = pig(h, 3, 3);
    Pig behind = pig(h, 3, 5);
    float f = first.getHealth(), b = behind.getHealth();
    cast(h, caster, g("ray"), g("damage", "amount", 3.0));
    assertHurt(h, first, f, "o primeiro alvo");
    assertUnhurt(h, behind, b, "o alvo de tras");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void touchHasShortRange(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    Pig close = pig(h, 3, 2);
    Pig distant = pig(h, 0, 6);
    Pig out = pig(h, 3, 6);
    float c = close.getHealth(), o = out.getHealth();
    cast(h, caster, g("touch"), g("damage"));
    assertHurt(h, close, c, "o alvo a 2 blocos");
    cast(h, distant, g("touch"), g("damage"));
    assertUnhurt(h, out, o, "o alvo a 6 blocos (alcance padrao 3)");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void projectileHitsACreature(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    Pig target = pig(h, 3, 5);
    float before = target.getHealth();
    cast(h, caster, g("projectile"), g("damage", "amount", 4.0));
    h.succeedWhen(() -> assertHurt(h, target, before, "o alvo do projetil"));
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void projectileOnImpactIgnitesABlock(GameTestHelper h) {
    floor(h);
    h.setBlock(new BlockPos(3, 1, 6), Blocks.STONE);
    Pig caster = pig(h, 3, 0);
    cast(h, caster, g("projectile"), g("on_hit_block"), g("ignite"));
    h.succeedWhen(() -> h.assertBlock(new BlockPos(3, 2, 6), b -> b == Blocks.FIRE, () -> "o bloco deveria ter fogo em cima"));
  }

  // ------------------------------------------------------------------ filtros
  @GameTest(template = "empty")
  public static void undeadAndHostileFiltersPickTheRightCreatures(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Pig animal = pig(h, 3, 4);
    Husk husk = h.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 1, 4));
    float a = animal.getHealth(), k = husk.getHealth();
    cast(h, caster, g("aura", "radius", 5.0), g("undead"), g("damage", "amount", 3.0));
    assertUnhurt(h, animal, a, "o animal (filtro Mortos-vivos)");
    assertHurt(h, husk, k, "o morto-vivo");

    husk.invulnerableTime = 0; // senao o segundo golpe cai nos quadros de invulnerabilidade
    float k2 = husk.getHealth();
    cast(h, caster, g("aura", "radius", 5.0), g("hostile"), g("damage", "amount", 3.0));
    assertUnhurt(h, animal, a, "o animal (filtro Hostis)");
    assertHurt(h, husk, k2, "o hostil");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void animalsFilterIgnoresMonsters(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Pig animal = pig(h, 3, 4);
    Husk husk = h.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 1, 4));
    float a = animal.getHealth(), k = husk.getHealth();
    cast(h, caster, g("aura", "radius", 5.0), g("animals"), g("damage", "amount", 3.0));
    assertHurt(h, animal, a, "o animal");
    assertUnhurt(h, husk, k, "o monstro");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void nearestFilterKeepsOnlyOne(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig near = pig(h, 3, 3);
    Pig far = pig(h, 3, 5);
    float n = near.getHealth(), f = far.getHealth();
    cast(h, caster, g("aura", "radius", 6.0), g("nearest"), g("damage", "amount", 3.0));
    assertHurt(h, near, n, "o mais proximo");
    assertUnhurt(h, far, f, "o mais distante");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void woundedFilterOnlyAffectsCreaturesBelowHalfHealth(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig hurt = pig(h, 3, 3);
    Pig healthy = pig(h, 3, 4);
    hurt.setHealth(3.0f);
    float hh = healthy.getHealth();
    cast(h, caster, g("aura", "radius", 6.0), g("wounded"), g("damage", "amount", 1.0));
    assertHurt(h, hurt, 3.0f, "o ferido");
    assertUnhurt(h, healthy, hh, "o saudavel");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void notSelfFilterRemovesTheCaster(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    float before = caster.getHealth();
    cast(h, caster, g("self"), g("not_self"), g("damage", "amount", 3.0));
    assertUnhurt(h, caster, before, "o conjurador (Exceto Eu)");
    h.succeed();
  }

  // ------------------------------------------------------------------ efeitos em blocos e explosoes
  @GameTest(template = "empty")
  public static void digBreaksSoftBlocksButNeverChestsOrHardOnes(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.DIRT);
    cast(h, caster, g("ray"), g("dig"));
    h.assertBlockNotPresent(Blocks.DIRT, new BlockPos(3, 1, 4));

    h.setBlock(new BlockPos(3, 1, 4), Blocks.CHEST);
    cast(h, caster, g("ray"), g("dig", "power", 5.0));
    h.assertBlockPresent(Blocks.CHEST, new BlockPos(3, 1, 4));

    h.setBlock(new BlockPos(3, 1, 4), Blocks.OBSIDIAN);
    cast(h, caster, g("ray"), g("dig", "power", 5.0));
    h.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(3, 1, 4));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void ignitePlacesFireOnTheTargetBlock(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.STONE);
    cast(h, caster, g("ray"), g("ignite"));
    h.assertBlockPresent(Blocks.FIRE, new BlockPos(3, 2, 4));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void explosionHurtsCreaturesButNotBlocks(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    Pig target = pig(h, 3, 4);
    h.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
    float before = target.getHealth();
    cast(h, caster, g("ray"), g("explode", "radius", 3.0));
    assertHurt(h, target, before, "o alvo da explosao");
    h.assertBlockPresent(Blocks.STONE, new BlockPos(4, 1, 4));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void thunderStrikesTheTarget(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    // Vaca: um porco atingido por raio vira piglin zumbificado e sumiria do teste.
    var target = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 4));
    float before = target.getHealth();
    cast(h, caster, g("ray"), g("thunder"));
    h.runAfterDelay(3, () -> {
      assertHurt(h, target, before, "o alvo do raio");
      h.succeed();
    });
  }

  // ------------------------------------------------------------------ tempo e condicoes
  @GameTest(template = "empty", timeoutTicks = 100)
  public static void delayPostponesTheNextSegment(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    float before = caster.getHealth();
    cast(h, caster, g("self"), g("heal", "amount", 1.0), g("delay", "seconds", 1.0), g("damage", "amount", 3.0));
    h.runAfterDelay(5, () -> assertUnhurt(h, caster, before, "o conjurador antes do atraso"));
    h.succeedWhen(() -> assertHurt(h, caster, before, "o conjurador depois do atraso"));
  }

  @GameTest(template = "empty")
  public static void sneakingConditionGatesTheSegment(GameTestHelper h) {
    floor(h);
    Pig sneaking = pig(h, 2, 3);
    Pig standing = pig(h, 4, 3);
    sneaking.setShiftKeyDown(true);
    cast(h, sneaking, g("self"), g("sneaking"), g("speed"));
    cast(h, standing, g("self"), g("sneaking"), g("speed"));
    h.assertTrue(sneaking.hasEffect(MobEffects.MOVEMENT_SPEED), "agachado deveria ativar");
    h.assertTrue(!standing.hasEffect(MobEffects.MOVEMENT_SPEED), "em pe nao deveria ativar");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void lowHealthConditionGatesTheSegment(GameTestHelper h) {
    floor(h);
    Pig weak = pig(h, 2, 3);
    Pig strong = pig(h, 4, 3);
    weak.setHealth(3.0f);
    cast(h, weak, g("self"), g("low_health"), g("shield"));
    cast(h, strong, g("self"), g("low_health"), g("shield"));
    h.assertTrue(weak.hasEffect(MobEffects.ABSORPTION), "com pouca vida deveria ativar");
    h.assertTrue(!strong.hasEffect(MobEffects.ABSORPTION), "com vida cheia nao deveria ativar");
    h.succeed();
  }

  // ------------------------------------------------------------------ mago e grimorio
  @GameTest(template = "empty")
  public static void everyMageElementCastsAValidSpell(GameTestHelper h) {
    for (MageElement e : MageElement.values()) {
      var validation = com.arcane.inscription.InscriptionValidator.validate(e.spell());
      h.assertTrue(validation.valid(), e + ": " + validation.message().getString());
      com.arcane.inscription.CompiledPlanCache.getOrCompile(e.spell());
    }
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void mageVariantSurvivesSaveAndLoad(GameTestHelper h) {
    NPCMageEntity mage = h.spawn(ModEntities.NPC_MAGE.get(), new BlockPos(3, 1, 3));
    mage.setVariant(13);
    CompoundTag tag = new CompoundTag();
    mage.addAdditionalSaveData(tag);
    NPCMageEntity other = h.spawn(ModEntities.NPC_MAGE.get(), new BlockPos(1, 1, 1));
    other.readAdditionalSaveData(tag);
    h.assertTrue(other.variant() == 13, "variante deveria ser 13, foi " + other.variant());
    h.assertTrue(other.element() == MageElement.FROST, "13 % 6 = 1 = gelo, foi " + other.element());
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void grimoireItemBelongsToItsOwnerOnly(GameTestHelper h) {
    Player owner = h.makeMockPlayer();
    var stack = GrimoireItem.createFor(owner);
    h.assertTrue(GrimoireItem.isOwnedBy(stack, owner.getUUID()), "o dono deveria reconhecer o grimorio");
    h.assertTrue(!GrimoireItem.isOwnedBy(stack, java.util.UUID.randomUUID()), "outro jogador nao deveria");
    h.succeed();
  }

  // ------------------------------------------------------------------ cristal e inscricoes no mundo
  /** O cristal deve ser achado em qualquer posicao relativa, nao so em multiplos de 8. */
  @GameTest(template = "empty")
  public static void crystalZoneDetectedAtAnyOffset(GameTestHelper helper) {
    BlockPos crystal = new BlockPos(1, 1, 1);
    BlockPos probe = new BlockPos(4, 2, 3); // distancia ~3,7: zona 5 (x32), com Mana Poisoning
    EnvironmentalSnapshot before = AmbientFieldResolver.resolve(helper.getLevel(), helper.absolutePos(probe));
    helper.assertTrue(before.potencyMultiplier() == 1.0, "sem cristal a potencia deveria ser 1, foi " + before.potencyMultiplier());

    helper.setBlock(crystal, ModBlocks.MANA_CRYSTAL.get());
    EnvironmentalSnapshot near = AmbientFieldResolver.resolve(helper.getLevel(), helper.absolutePos(probe));
    helper.assertTrue(near.potencyMultiplier() == 32.0, "perto do cristal a potencia deveria ser 32, foi " + near.potencyMultiplier());
    helper.assertTrue(near.poisoningExposureRate() > 0, "perto do cristal deveria haver Mana Poisoning");

    // ~20 blocos: zona 2 (x4), sem envenenamento
    BlockPos far = helper.absolutePos(crystal).offset(14, 3, 14);
    EnvironmentalSnapshot mid = AmbientFieldResolver.resolve(helper.getLevel(), far);
    helper.assertTrue(mid.potencyMultiplier() == 4.0, "a ~20 blocos a potencia deveria ser 4, foi " + mid.potencyMultiplier());
    helper.assertTrue(mid.poisoningExposureRate() == 0, "a ~20 blocos nao deveria envenenar");
    helper.succeed();
  }

  /** Bloco inscrito com Eu + Dano fere quem pisa nele. */
  @GameTest(template = "empty", timeoutTicks = 100)
  public static void touchInscriptionTriggers(GameTestHelper helper) {
    BlockPos stone = new BlockPos(3, 1, 3);
    helper.setBlock(stone, Blocks.STONE);
    BlockPos abs = helper.absolutePos(stone);
    Inscription trap = new Inscription(List.of(g("self"), g("damage", "amount", 4.0)));
    WorldInscriptionIndex.get(helper.getLevel()).put(new InscriptionAnchor(abs, trap,
        InscribingService.TRIGGER_ON_TOUCH, new ResourceLocation("arcane", "common")));

    Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, stone.above());
    float startHealth = pig.getHealth();
    helper.succeedWhen(() -> {
      helper.assertTrue(pig.getHealth() < startHealth, "o porco ainda nao foi ferido");
      InscribingService.erase(helper.getLevel(), abs);
    });
  }

  /** Quebrar o bloco apaga a inscricao. */
  @GameTest(template = "empty")
  public static void erasingRemovesInscription(GameTestHelper helper) {
    BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
    WorldInscriptionIndex index = WorldInscriptionIndex.get(helper.getLevel());
    index.put(new InscriptionAnchor(abs, Inscription.testSelfDamage(), InscribingService.TRIGGER_ON_TOUCH,
        new ResourceLocation("arcane", "common")));
    helper.assertTrue(index.get(abs).isPresent(), "inscricao nao foi gravada");
    InscribingService.erase(helper.getLevel(), abs);
    helper.assertTrue(index.get(abs).isEmpty(), "inscricao nao foi apagada");
    helper.succeed();
  }

  // ------------------------------------------------------------------ design v2
  /** Marcar + filtro Marcados / Nao Marcados: o combo "Onda -> Marcar -> Gravidade" do design. */
  @GameTest(template = "empty")
  public static void markedAndUnmarkedFiltersSplitTheTargets(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig a = pig(h, 3, 3);
    Pig b = pig(h, 3, 4);
    cast(h, caster, g("ray"), g("mark"));
    h.assertTrue(com.arcane.execution.Marks.isMarked(a), "o primeiro alvo deveria estar marcado");
    h.assertTrue(!com.arcane.execution.Marks.isMarked(b), "o segundo nao");
    float ha = a.getHealth(), hb = b.getHealth();
    cast(h, caster, g("aura", "radius", 6.0), g("marked"), g("damage", "amount", 3.0));
    assertHurt(h, a, ha, "o marcado");
    assertUnhurt(h, b, hb, "o nao marcado");
    a.invulnerableTime = 0;
    float ha2 = a.getHealth();
    cast(h, caster, g("aura", "radius", 6.0), g("unmarked"), g("damage", "amount", 3.0));
    assertHurt(h, b, hb, "o nao marcado, agora com o outro filtro");
    assertUnhurt(h, a, ha2, "o marcado");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void gravitySlamsCreaturesAndStacks(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig target = pig(h, 3, 4);
    cast(h, caster, g("ray"), g("gravity"));
    double one = target.getDeltaMovement().y;
    h.assertTrue(one < -0.5, "deveria ser jogado para baixo, vy=" + one);
    h.assertTrue(target.fallDistance > 0, "deveria acumular queda");
    target.setDeltaMovement(0, 0, 0);
    cast(h, caster, g("ray"), g("gravity"), g("gravity"), g("gravity"));
    h.assertTrue(target.getDeltaMovement().y < one * 2, "tres Gravidade empurram bem mais que um (vy=" + target.getDeltaMovement().y + ")");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void gravityDropsSand(GameTestHelper h) {
    floor(h);
    h.setBlock(new BlockPos(3, 0, 4), Blocks.AIR);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.SAND);
    Pig caster = pig(h, 3, 1);
    cast(h, caster, g("ray"), g("gravity"));
    h.assertTrue(h.getBlockState(new BlockPos(3, 1, 4)).getBlock() != Blocks.SAND, "a areia deveria ter caido");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void attractPullsTowardTheCaster(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig target = pig(h, 3, 5);
    cast(h, caster, g("ray"), g("attract"));
    h.assertTrue(target.getDeltaMovement().z < -0.3, "deveria ser puxado para o conjurador, vz=" + target.getDeltaMovement().z);
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void cutHurtsAndClearsLeaves(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig target = pig(h, 3, 3);
    float before = target.getHealth();
    cast(h, caster, g("ray"), g("cut", "amount", 3.0));
    assertHurt(h, target, before, "o alvo do corte");
    target.discard();
    h.setBlock(new BlockPos(3, 1, 4), Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
    cast(h, caster, g("ray"), g("cut"));
    h.assertBlockNotPresent(Blocks.OAK_LEAVES, new BlockPos(3, 1, 4));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void freezeTurnsWaterToIceAndLavaToObsidian(GameTestHelper h) {
    floor(h);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.WATER);
    h.setBlock(new BlockPos(4, 1, 3), Blocks.LAVA);
    Pig caster = pig(h, 3, 3);
    cast(h, caster, g("self"), g("freeze"));
    h.assertBlockPresent(Blocks.ICE, new BlockPos(3, 1, 4));
    h.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(4, 1, 3));
    h.assertTrue(caster.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "o proprio conjurador (Eu) tambem congela");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void fireLightsTntMeltsIceAndLightsCampfires(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.TNT);
    cast(h, caster, g("ray"), g("fire"));
    h.assertBlockNotPresent(Blocks.TNT, new BlockPos(3, 1, 4));
    h.assertTrue(!h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
        new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(3, 1, 4))).inflate(3)).isEmpty(), "o TNT deveria ter sido aceso");
    // o TNT aceso (entidade) ficaria no caminho do raio
    h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
        new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(3, 1, 4))).inflate(3)).forEach(e -> e.discard());
    h.setBlock(new BlockPos(3, 1, 4), Blocks.ICE);
    cast(h, caster, g("ray"), g("fire"));
    h.assertBlockNotPresent(Blocks.ICE, new BlockPos(3, 1, 4));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void waveOnlyAffectsTheConeInFront(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Pig front = pig(h, 3, 5);
    Pig side = pig(h, 6, 3);
    Pig behind = pig(h, 3, 0);
    float f = front.getHealth(), s = side.getHealth(), b = behind.getHealth();
    cast(h, caster, g("wave"), g("damage", "amount", 3.0));
    assertHurt(h, front, f, "o alvo a frente");
    assertUnhurt(h, side, s, "o alvo ao lado");
    assertUnhurt(h, behind, b, "o alvo atras");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void pillarHitsTheColumnAroundTheAimedPoint(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    Pig aimed = pig(h, 3, 4);
    Pig neighbor = pig(h, 3, 5);
    Pig faraway = pig(h, 0, 6);
    float a = aimed.getHealth(), n = neighbor.getHealth(), f = faraway.getHealth();
    cast(h, caster, g("pillar", "radius", 2.0), g("damage", "amount", 3.0));
    assertHurt(h, aimed, a, "o alvo mirado");
    assertHurt(h, neighbor, n, "o vizinho dentro da coluna");
    assertUnhurt(h, faraway, f, "o alvo fora da coluna");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 160)
  public static void barrierRaisesAWallThatFadesAway(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    cast(h, caster, g("barrier", "duration", 40.0));
    for (int x = 2; x <= 4; x++) {
      for (int y = 1; y <= 3; y++) {
        h.assertBlockPresent(com.arcane.registry.ModBlocks.ARCANE_BARRIER.get(), new BlockPos(x, y, 4));
      }
    }
    h.succeedWhen(() -> h.assertBlockNotPresent(com.arcane.registry.ModBlocks.ARCANE_BARRIER.get(), new BlockPos(3, 2, 4)));
  }

  @GameTest(template = "empty")
  public static void alliesFilterPicksVillagersAndSparesAnimals(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Pig animal = pig(h, 3, 4);
    var villager = h.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(4, 1, 4));
    float a = animal.getHealth(), v = villager.getHealth();
    cast(h, caster, g("aura", "radius", 5.0), g("allies"), g("damage", "amount", 3.0));
    assertHurt(h, villager, v, "o aldeao (aliado)");
    assertUnhurt(h, animal, a, "o animal comum");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void woodFilterOnlyDigsWood(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.STONE);
    cast(h, caster, g("ray"), g("wood"), g("dig"));
    h.assertBlockPresent(Blocks.STONE, new BlockPos(3, 1, 4));
    h.setBlock(new BlockPos(3, 1, 4), Blocks.OAK_PLANKS);
    cast(h, caster, g("ray"), g("wood"), g("dig"));
    h.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(3, 1, 4));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void standingStillConditionGatesTheSegment(GameTestHelper h) {
    floor(h);
    Pig still = pig(h, 3, 3);
    // espera o porco assentar no chao (recem-criado ainda nao esta "onGround")
    h.runAfterDelay(10, () -> {
      cast(h, still, g("self"), g("standing_still"), g("speed"));
      h.assertTrue(still.hasEffect(MobEffects.MOVEMENT_SPEED), "parado deveria ativar");
      h.succeed();
    });
  }

  /** Magia reativa: "Eu -> Ao Sofrer Dano -> Escudo" fica armada e so dispara quando o conjurador leva dano. */
  @GameTest(template = "empty")
  public static void onHurtArmsTheSpellAndFiresOnce(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    cast(h, p, g("self"), g("on_hurt"), g("shield"));
    h.assertTrue(!p.hasEffect(MobEffects.ABSORPTION), "nao deve disparar ao conjurar");
    h.assertTrue(com.arcane.execution.ArmedSpells.armedCount(p.getUUID()) == 1, "deveria haver 1 magia armada");
    p.hurt(h.getLevel().damageSources().generic(), 1.0f);
    h.assertTrue(p.hasEffect(MobEffects.ABSORPTION), "deveria disparar ao levar dano");
    h.assertTrue(com.arcane.execution.ArmedSpells.armedCount(p.getUUID()) == 0, "dispara uma unica vez");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void armedSpellsAreCappedAtThree(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    for (int i = 0; i < 5; i++) cast(h, p, g("self"), g("on_hurt"), g("shield"));
    h.assertTrue(com.arcane.execution.ArmedSpells.armedCount(p.getUUID()) == 3, "maximo de 3 armadas");
    com.arcane.execution.ArmedSpells.clear(p.getUUID());
    h.succeed();
  }

  /** O loot por estrutura foi injetado nos baus vanilla (e so nos que o mapa lista). */
  @GameTest(template = "empty")
  public static void glyphLootIsInjectedIntoVanillaStructureChests(GameTestHelper h) {
    var loot = h.getLevel().getServer().getLootData();
    for (String path : List.of("chests/desert_pyramid", "chests/ancient_city", "chests/simple_dungeon", "chests/stronghold_library")) {
      var table = loot.getLootTable(new ResourceLocation("minecraft", path));
      h.assertTrue(table.getPool("arcane_glyphs") != null, path + " deveria ter a pool arcane_glyphs");
    }
    var untouched = loot.getLootTable(new ResourceLocation("minecraft", "chests/spawn_bonus_chest"));
    h.assertTrue(untouched.getPool("arcane_glyphs") == null, "o bau de bonus nao deve receber glifos");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void arcanePageAddsPagesToTheGrimoire(GameTestHelper h) {
    var state = new com.arcane.grimoire.GrimoireState();
    state.addPages(com.arcane.item.ArcanePageItem.PAGES_PER_ITEM);
    h.assertTrue(state.pages() == 10, "2 paginas a mais, tem " + state.pages());
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void cutShearsReadySheep(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var sheep = h.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(3, 1, 4));
    h.assertTrue(!sheep.isSheared(), "a ovelha deveria comecar com la");
    cast(h, caster, g("ray"), g("cut"));
    h.assertTrue(sheep.isSheared(), "o Corte deveria tosquiar a ovelha");
    h.succeed();
  }
}
