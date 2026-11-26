package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.execution.CastingService;
import com.arcane.execution.StyleTally;
import com.arcane.execution.SpellElement;
import com.arcane.fx.CastStyle;
import com.arcane.inscription.Inscription;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Maestria e assinatura (Fase M): so aparencia. Regressao: maestria 0 e 1 dao exatamente o mesmo jogo. */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class MasteryGameTests extends GameTestSupport {

  /** O mesmo Raio + Dano no Boneco com maestria 0 e 1: mesmo dano, mesma mana, mesmo resultado. */
  @GameTest(template = "empty", batch = "mastery_1", timeoutTicks = 120)
  public static void masteryNeverChangesGameplay(GameTestHelper h) {
    floor(h);
    var p = Phase5GameTests.player(h, 3.5, 0.5, "ray", "damage");
    var d = TrainingDummyGameTests.dummy(h, 3, 4);
    float[] dealt = new float[2];
    int[] mana = new int[2];
    CastingService.devMastery = 0.0;
    var r0 = Phase5GameTests.cast(h, p, g("ray"), g("damage", "amount", 4));
    CastingService.devMastery = null;
    h.runAfterDelay(30, () -> {
      dealt[0] = d.meter().total();
      mana[0] = r0.manaSpent();
      d.resetNow();
      d.meter().clear();
      d.invulnerableTime = 0; // o primeiro golpe deixou os ticks de invulnerabilidade
      CastingService.devMastery = 1.0;
      var r1 = Phase5GameTests.cast(h, p, g("ray"), g("damage", "amount", 4));
      CastingService.devMastery = null;
      mana[1] = r1.manaSpent();
      h.runAfterDelay(3, () -> {
        dealt[1] = d.meter().total();
        h.assertTrue(r0.success() && r1.success(), "as duas conjuraram");
        h.assertTrue(dealt[0] > 0 && dealt[0] == dealt[1], "mesmo dano com maestria 0 e 1 (" + dealt[0] + " e " + dealt[1] + ")");
        h.assertTrue(mana[0] == mana[1], "mesma mana (" + mana[0] + " e " + mana[1] + ")");
        h.succeed();
      });
    });
  }

  /** Maestria do jogador sai do grimorio; a assinatura so existe depois de conjurar (estilo) e o Cor a desliga. */
  @GameTest(template = "empty", batch = "mastery_1")
  public static void masteryAndSignatureComeFromThePlayer(GameTestHelper h) {
    var p = Phase5GameTests.player(h, 3.5, 3.5, "projectile", "damage");
    var known = new Inscription(List.of(g("projectile"), g("damage")));
    h.assertTrue(CastingService.masteryOf(p, known) == 1.0, "glifos dominados: maestria 1");
    var half = new Inscription(List.of(g("projectile"), g("explode")));
    double m = CastingService.masteryOf(p, half);
    h.assertTrue(m > 0 && m < 1, "Explosao desconhecida baixa a maestria (" + m + ")");
    h.assertTrue(CastingService.signatureOf(p, false) == 0, "sem estilo ainda: sem assinatura");
    StyleTally.count(p, SpellElement.FIRE);
    StyleTally.count(p, SpellElement.FIRE);
    StyleTally.count(p, SpellElement.VOID);
    int seed = CastingService.signatureOf(p, false);
    h.assertTrue(CastStyle.present(seed) && CastStyle.element(seed) == SpellElement.FIRE, "estilo Fogo + Vazio");
    h.assertTrue(seed == CastStyle.seed(p.getUUID(), SpellElement.FIRE, SpellElement.VOID), "deterministica");
    h.assertTrue(CastingService.signatureOf(p, true) == 0, "o modificador Cor desliga a assinatura");
    var pig = h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, new net.minecraft.core.BlockPos(1, 1, 1));
    h.assertTrue(CastingService.masteryOf(pig, half) == 1.0 && CastingService.signatureOf(pig, false) == 0, "criatura: visual de sempre");
    h.succeed();
  }
}
