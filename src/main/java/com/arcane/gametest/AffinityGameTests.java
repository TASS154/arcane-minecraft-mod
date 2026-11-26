package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.affinity.Affinities;
import com.arcane.affinity.AffinityContext;
import com.arcane.entity.ArcaneDummyEntity;
import com.arcane.execution.Pierce;
import com.arcane.execution.SpellElement;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Afinidades elementais (Fase I). Os testes que trocam a tabela global ficam num lote proprio e a restauram no fim.
 */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class AffinityGameTests extends GameTestSupport {
  static final ResourceLocation TEST = new ResourceLocation("arcane", "test_profile");

  static Map<ResourceLocation, JsonElement> withTest(String json) {
    Map<ResourceLocation, JsonElement> files = new HashMap<>(Affinities.lastFiles());
    files.put(TEST, JsonParser.parseString(json));
    return files;
  }

  static void hit(ArcaneDummyEntity d, net.minecraft.world.damagesource.DamageSource s, float amount) {
    d.invulnerableTime = 0;
    d.hurt(s, amount);
  }

  /** O perfil do Boneco multiplica o dano uma unica vez; imunidade zera. */
  @GameTest(template = "empty", batch = "affinity_1")
  public static void affinityAppliesOnceAndImmunityBlocks(GameTestHelper h) {
    floor(h);
    var d = TrainingDummyGameTests.dummy(h, 3, 3);
    d.setAffinityProfile("arcane:fire_creatures");
    var src = Pierce.spell(h.getLevel(), d);
    AffinityContext.push(SpellElement.FROST);
    try {
      hit(d, src, 10f);
    } finally {
      AffinityContext.pop();
    }
    h.assertTrue(Math.abs(d.meter().total() - 15f) < 0.01f, "gelo x1,5 contra criatura de fogo, uma vez so (" + d.meter().total() + ")");
    float before = d.getHealth();
    AffinityContext.push(SpellElement.FIRE);
    try {
      hit(d, src, 10f);
    } finally {
      AffinityContext.pop();
    }
    h.assertTrue(d.getHealth() == before && d.meter().count() == 1, "imune ao fogo: nenhum dano");
    hit(d, src, 4f);
    h.assertTrue(Math.abs(d.meter().lastHit() - 4f) < 0.01f, "fora de um efeito (sem elemento na pilha) nada muda");
    h.succeed();
  }

  /** Conjuracao de verdade: o pipeline empilha o elemento do efeito (Dano = arcano) e a afinidade vale uma vez. */
  @GameTest(template = "empty", batch = "affinity_2", timeoutTicks = 60)
  public static void realCastUsesTheEffectElement(GameTestHelper h) {
    floor(h);
    var original = Affinities.lastFiles();
    Affinities.load(withTest("{\"dummy_only\": true, \"multipliers\": {\"arcane\": 1.5}}"));
    var p = Phase5GameTests.player(h, 3.5, 0.5, "ray", "damage");
    var d = TrainingDummyGameTests.dummy(h, 3, 4);
    d.setAffinityProfile(TEST.toString());
    var r = Phase5GameTests.cast(h, p, g("ray"), g("damage", "amount", 4));
    h.runAfterDelay(2, () -> {
      Affinities.load(original);
      h.assertTrue(r.success(), "a conjuracao funcionou: " + r.message());
      h.assertTrue(Math.abs(d.meter().total() - 6f) < 0.01f, "Dano 4 x afinidade arcana 1,5 = 6, aplicada uma vez (" + d.meter().total() + ")");
      h.succeed();
    });
  }

  /** Teto da afinidade acumulada com reacao (padrao 2): 1,5 x 1,5 vira 2. */
  @GameTest(template = "empty", batch = "affinity_3")
  public static void capLimitsAffinityTimesReaction(GameTestHelper h) {
    floor(h);
    var original = Affinities.lastFiles();
    Affinities.load(withTest("{\"dummy_only\": true, \"multipliers\": {\"storm\": 1.5}}"));
    var d = TrainingDummyGameTests.dummy(h, 3, 3);
    d.setAffinityProfile(TEST.toString());
    AffinityContext.push(SpellElement.STORM);
    try {
      AffinityContext.noteReaction(d, 1.5); // a interacao (molhado) ja deu x1,5 no valor que o efeito passou
      hit(d, Pierce.spell(h.getLevel(), d), 15f);
    } finally {
      AffinityContext.pop();
      Affinities.load(original);
    }
    float expected = 15f * (2.0f / 1.5f);
    h.assertTrue(Math.abs(d.meter().total() - expected) < 0.01f, "afinidade cortada para o total bater em 2x (" + d.meter().total() + " de " + expected + ")");
    h.succeed();
  }

  /** Sem perfil (porco, Hidra): comportamento de antes. A tabela padrao carrega e ninguem tem a Hidra. */
  @GameTest(template = "empty", batch = "affinity_1")
  public static void entitiesWithoutProfileAreUnchanged(GameTestHelper h) {
    floor(h);
    var pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
    h.assertTrue(Affinities.of(pig) == null, "porco nao tem perfil");
    var hydra = net.minecraft.world.entity.EntityType.getKey(com.arcane.registry.ModEntities.HYDRA.get());
    h.assertTrue(Affinities.profiles().values().stream().noneMatch(p -> p.entities().contains(hydra)), "a Hidra nao tem afinidade (precisa de aprovacao)");
    h.assertTrue(Affinities.profiles().size() >= 6, "a tabela padrao carregou (" + Affinities.profiles().size() + ")");
    var blaze = h.spawnWithNoFreeWill(EntityType.BLAZE, new BlockPos(5, 1, 5));
    h.assertTrue(Affinities.multiplier(blaze, SpellElement.FIRE) == 0.0, "blaze imune ao fogo pela tabela");
    float before = pig.getHealth();
    AffinityContext.push(SpellElement.FIRE);
    try {
      pig.hurt(Pierce.spell(h.getLevel(), pig), 2f);
    } finally {
      AffinityContext.pop();
    }
    h.assertTrue(Math.abs(before - pig.getHealth() - 2f) < 0.01f, "dano do porco intocado");
    h.succeed();
  }
}
