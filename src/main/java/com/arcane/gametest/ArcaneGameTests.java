package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.ambient.AmbientFieldResolver;
import com.arcane.execution.EnvironmentalSnapshot;
import com.arcane.inscribing.InscribingService;
import com.arcane.inscribing.InscriptionAnchor;
import com.arcane.inscribing.WorldInscriptionIndex;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Testes dentro de um mundo real (./gradlew runGameTestServer).
 * Estrutura: data/arcane/structures/empty.nbt (tools/gen_test_structure.py).
 */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class ArcaneGameTests {

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
    Inscription trap = new Inscription(List.of(
        new GlyphInstance(new ResourceLocation("arcane", "self")),
        new GlyphInstance(new ResourceLocation("arcane", "damage"), Map.of("amount", 4.0))));
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
}
