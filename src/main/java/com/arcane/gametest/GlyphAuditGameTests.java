package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.execution.Inversion;
import com.arcane.grimoire.GlyphKnowledge;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionValidator;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.arcane.gametest.Phase5GameTests.floor;
import static com.arcane.gametest.Phase5GameTests.g;
import static com.arcane.gametest.Phase5GameTests.player;

/**
 * Auditoria de glifos (docs/AUDITORIA_GLIFOS.md). Os testes marcados {@code required = false} verificam uma regra que o
 * codigo AINDA NAO cumpre: falham sem derrubar a suite e o log mostra "optional test failed" com o furo. Quando a correcao
 * for aprovada e feita, viram obrigatorios.
 */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class GlyphAuditGameTests {

  /**
   * Furo: Inverter troca o efeito na hora de executar (SpellPipeline), depois da validacao, que so confere os glifos
   * escritos. Quem conhece Dano e Inverter cura sem nunca ter aprendido Cura.
   */
  @GameTest(template = "empty", batch = "audit_invert_1", required = false)
  public static void inverterRequiresKnowingTheResultingGlyph(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 3.5, "self", "damage", "invert"); // NAO conhece Cura
    p.setHealth(10f);
    var spell = new Inscription(List.of(g("self"), g("damage", "amount", 4.0), g("invert")));
    var r = com.arcane.execution.CastingService.tryCast(p, spell, com.arcane.execution.PrecisionSnapshot.of(spell));
    float hp = p.getHealth();
    h.assertTrue(!r.success() || hp <= 10f,
        "Inverter entregou Cura sem o jogador conhecer Cura (resultado " + r.success() + ", vida 10 -> " + hp + ")");
    h.succeed();
  }

  /**
   * O mesmo furo em todos os pares: a validacao aceita "X + Inverter" sem o oposto de X ser conhecido. Lista os pares
   * em que o oposto tem tier maior (o caminho barato para um glifo raro).
   */
  @GameTest(template = "empty", batch = "audit_invert_2", required = false)
  public static void validationOfInvertedSpellsChecksTheOpposite(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 3.5, "self", "invert");
    var grimoire = com.arcane.grimoire.GrimoireAttachments.get(p).orElseThrow();
    List<String> holes = new ArrayList<>();
    for (String effect : List.of("damage", "heal", "push", "attract", "slow", "speed", "fire", "freeze", "levitate",
        "gravity", "lifesteal", "life_transfer", "blind", "invisible", "fear", "taunt")) {
      String opposite = Inversion.opposite(effect).orElseThrow();
      grimoire.learnGlyph(new ResourceLocation("arcane", effect), GlyphKnowledge.MASTERED);
      if (grimoire.knowsGlyph(new ResourceLocation("arcane", opposite), GlyphKnowledge.IDENTIFIED)) continue;
      var spell = new Inscription(List.of(g("self"), g(effect), g("invert")));
      if (InscriptionValidator.validate(spell, Optional.of(grimoire)).valid()) {
        int tierFrom = com.arcane.glyph.GlyphRegistry.get(new ResourceLocation("arcane", effect)).orElseThrow().tier();
        int tierTo = com.arcane.glyph.GlyphRegistry.get(new ResourceLocation("arcane", opposite)).orElseThrow().tier();
        holes.add(effect + "(t" + tierFrom + ")->" + opposite + "(t" + tierTo + ")" + (tierTo > tierFrom ? " SOBE TIER" : ""));
      }
    }
    h.assertTrue(holes.isEmpty(), "aceita inverter para um glifo desconhecido: " + holes);
    h.succeed();
  }
}
