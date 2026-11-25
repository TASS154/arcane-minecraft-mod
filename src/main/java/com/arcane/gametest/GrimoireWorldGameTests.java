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

/** Mago NPC, grimorio, cristal e inscricoes no mundo. (dividido de ArcaneGameTests em 25 nov; os testes nao mudaram.) */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class GrimoireWorldGameTests extends GameTestSupport {

  @GameTest(template = "empty")
  public static void everyMageElementCastsAValidSpell(GameTestHelper h) {
    for (MageElement e : MageElement.values()) {
      h.assertTrue(e.spells().size() >= 4, e + ": grimorio com 4 magias ou mais");
      for (var sp : e.spells()) {
        var validation = com.arcane.inscription.InscriptionValidator.validate(sp.inscription());
        h.assertTrue(validation.valid(), e + " / " + sp.inscription().metadata().name() + ": " + validation.message().getString());
        com.arcane.inscription.CompiledPlanCache.getOrCompile(sp.inscription());
      }
    }
    // a "inteligencia": vida baixa defende; o pacifico nunca ataca e cura quem esta ferido; perto demais usa area
    var rnd = net.minecraft.util.RandomSource.create(3);
    int[] ready = new int[8];
    var low = new com.arcane.npc.MageBrain.Situation(0.2f, 8, 1, false, true);
    h.assertTrue(com.arcane.npc.MageBrain.choose(MageElement.FIRE, com.arcane.npc.MagePersonality.HOSTILE, low, ready, rnd).role() == MageElement.Role.DEFEND,
        "com pouca vida o mago se defende");
    var fight = new com.arcane.npc.MageBrain.Situation(1f, 8, 1, false, true);
    for (int i = 0; i < 20; i++) {
      var pick = com.arcane.npc.MageBrain.choose(MageElement.FIRE, com.arcane.npc.MagePersonality.HARMLESS, fight, ready, rnd);
      h.assertTrue(pick == null || pick.role() == MageElement.Role.ESCAPE, "o pacifico nunca ataca");
    }
    var hurtAlly = new com.arcane.npc.MageBrain.Situation(1f, 99, 0, true, false);
    h.assertTrue(com.arcane.npc.MageBrain.choose(MageElement.FROST, com.arcane.npc.MagePersonality.HARMLESS, hurtAlly, ready, rnd).role() == MageElement.Role.SUPPORT,
        "o pacifico de gelo cura o jogador ferido");
    var close = new com.arcane.npc.MageBrain.Situation(1f, 2.5, 1, false, true);
    h.assertTrue(com.arcane.npc.MageBrain.choose(MageElement.FROST, com.arcane.npc.MagePersonality.HOSTILE, close, ready, rnd).role() == MageElement.Role.AREA,
        "inimigo colado: magia de area");
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
    mage.setPersonality(com.arcane.npc.MagePersonality.TRICKSTER);
    CompoundTag tag2 = new CompoundTag();
    mage.addAdditionalSaveData(tag2);
    other.readAdditionalSaveData(tag2);
    h.assertTrue(other.personality() == com.arcane.npc.MagePersonality.TRICKSTER, "a personalidade e salva");
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

  /** O cristal deve ser achado em qualquer posicao relativa, nao so em multiplos de 8. */
  @GameTest(template = "empty", batch = "crystal")
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
    helper.setBlock(crystal, Blocks.AIR);
    checkCrystalWidensReach(helper);
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
}
