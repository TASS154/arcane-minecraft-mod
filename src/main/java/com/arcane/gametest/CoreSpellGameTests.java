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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Nucleo das magias: cobertura dos glifos, efeitos em si mesmo, formas, filtros, blocos e explosoes, tempo e condicoes. (dividido de ArcaneGameTests em 25 nov; os testes nao mudaram.) */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class CoreSpellGameTests extends GameTestSupport {

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

  // Lote proprio: efeitos de area de testes vizinhos (empurrar, atrair) podiam arremessar a vaca para longe.
  @GameTest(template = "empty", batch = "isolated_thunder")
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
}
