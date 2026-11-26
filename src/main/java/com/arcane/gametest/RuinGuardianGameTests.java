package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.entity.RuinGuardianEntity;
import com.arcane.entity.RuinGuardians;
import com.arcane.execution.Pierce;
import com.arcane.execution.Wetness;
import com.arcane.registry.ModEntities;
import com.arcane.world.ArcaneRuins;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Guardiao de Ruina (Fase N): casca, quebra-cabeca de 2 passos, recompensa modesta e onde aparece. NAO visto em jogo. */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class RuinGuardianGameTests extends GameTestSupport {

  static RuinGuardianEntity guardian(GameTestHelper h, int x, int z) {
    var g = h.spawnWithNoFreeWill(ModEntities.RUIN_GUARDIAN.get(), new BlockPos(x, 1, z));
    g.setup(RuinGuardians.first(), null);
    return g;
  }

  static void hit(RuinGuardianEntity g, GameTestHelper h, float amount) {
    g.invulnerableTime = 0;
    g.hurt(Pierce.spell(h.getLevel(), g), amount);
  }

  /** A casca absorve tudo: magia, fogo, pancada. Vida fixa do JSON (sem escala). */
  @GameTest(template = "empty", batch = "guardian_1")
  public static void shellAbsorbsEverything(GameTestHelper h) {
    floor(h);
    var g = guardian(h, 3, 3);
    h.assertTrue(g.getMaxHealth() == (float) RuinGuardians.first().health(), "vida do JSON (" + g.getMaxHealth() + ")");
    h.assertTrue(g.shellUp(), "comeca com a casca");
    hit(g, h, 10f);
    g.invulnerableTime = 0;
    g.hurt(h.getLevel().damageSources().onFire(), 5f);
    g.invulnerableTime = 0;
    g.hurt(h.getLevel().damageSources().generic(), 5f);
    h.assertTrue(g.getHealth() == g.getMaxHealth(), "nada passa pela casca (" + g.getHealth() + ")");
    h.succeed();
  }

  /** Passo 1: molhar. Passo 2: fogo numa conjuracao = Vapor, a casca racha e o dano entra x1,5. Fogo seco nao racha. */
  @GameTest(template = "empty", batch = "guardian_2", timeoutTicks = 80)
  public static void wetThenFireCracksTheShell(GameTestHelper h) {
    floor(h);
    var p = Phase5GameTests.player(h, 3.5, 0.5, "ray", "fire");
    var g = guardian(h, 3, 4);
    var dry = Phase5GameTests.cast(h, p, g("ray"), g("fire"));
    h.assertTrue(dry.success(), "conjurou: " + dry.message());
    boolean[] stage = {false};
    h.succeedWhen(() -> {
      if (!stage[0]) {
        h.assertTrue(g.shellUp(), "fogo sem molhar: a casca fica");
        g.clearFire();
        Wetness.soak(h.getLevel(), g, 400);
        Phase5GameTests.cast(h, p, g("ray"), g("fire"));
        stage[0] = true;
        h.fail("passo 2 lancado");
      }
      h.assertTrue(!g.shellUp() && g.windowTicks() > 0, "molhado + fogo: a casca rachou");
      float before = g.getHealth();
      hit(g, h, 10f);
      float lost = before - g.getHealth();
      h.assertTrue(Math.abs(lost - 15f) < 0.01f, "janela: dano x1,5 (" + lost + ")");
    });
  }

  /** A janela fecha e a casca volta. */
  @GameTest(template = "empty", batch = "guardian_1", timeoutTicks = 260)
  public static void shellComesBack(GameTestHelper h) {
    floor(h);
    var g = guardian(h, 3, 3);
    g.onReaction("hiss");
    h.assertTrue(g.shellUp(), "outra reacao nao racha");
    g.onReaction("steam");
    h.assertTrue(!g.shellUp(), "Vapor racha");
    h.runAfterDelay(RuinGuardians.first().windowTicks() + 5, () -> {
      h.assertTrue(g.shellUp(), "a casca voltou");
      h.succeed();
    });
  }

  /** Recompensa modesta: tinta, material e as vezes uma nota de glifo de tier ate 3, nunca original. */
  @GameTest(template = "empty", batch = "guardian_1")
  public static void rewardIsModestAndNeverOriginal(GameTestHelper h) {
    var g = guardian(h, 3, 3);
    var rand = RandomSource.create(77);
    int notes = 0;
    for (int i = 0; i < 60; i++) {
      for (var stack : g.rewards(rand)) {
        if (stack.getItem() == com.arcane.registry.ModItems.GLYPH_NOTE.get()) {
          notes++;
          var id = ResourceLocation.tryParse(stack.getOrCreateTag().getString("glyph"));
          var def = com.arcane.glyph.GlyphRegistry.get(id).orElseThrow();
          h.assertTrue(def.tier() <= 3 && !def.families().contains("original"), "nota de glifo comum: " + id + " tier " + def.tier());
        } else {
          h.assertTrue(stack.getCount() <= 4, "pouca coisa: " + stack);
        }
      }
    }
    h.assertTrue(notes > 5 && notes < 40, "nota de glifo so as vezes (" + notes + " de 60)");
    h.succeed();
  }

  /** So no Salao do Ritual das Ruinas, com a chance fixa pela posicao. */
  @GameTest(template = "empty", batch = "guardian_1")
  public static void spawnsOnlyInTheRuins(GameTestHelper h) {
    BlockPos hit = null, miss = null;
    for (int i = 0; i < 200 && (hit == null || miss == null); i++) {
      BlockPos c = h.absolutePos(new BlockPos(3, 1, 3)).offset(i * 31, 0, 0);
      if (ArcaneRuins.guardianRolls(c, RuinGuardians.first().spawnChance())) { if (hit == null) hit = c; }
      else if (miss == null) miss = c;
    }
    h.assertTrue(hit != null && miss != null, "a chance de 50% acerta e erra");
    h.assertTrue(ArcaneRuins.guardianRolls(hit, RuinGuardians.first().spawnChance()) && !ArcaneRuins.guardianRolls(miss, RuinGuardians.first().spawnChance()), "fixa");
    h.assertTrue(net.minecraft.world.entity.SpawnPlacements.getPlacementType(ModEntities.RUIN_GUARDIAN.get())
        == net.minecraft.world.entity.SpawnPlacements.Type.NO_RESTRICTIONS, "sem regra de nascer natural registrada");
    h.succeed();
  }
}
