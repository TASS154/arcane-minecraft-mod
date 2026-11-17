package com.arcane.execution;

import com.arcane.TestGlyphs;
import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionCompiler;
import com.arcane.inscription.InscriptionValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.arcane.TestGlyphs.g;
import static org.junit.jupiter.api.Assertions.*;

class Phase4GrammarTest {
  @BeforeAll
  static void loadGlyphs() {
    TestGlyphs.load();
  }

  @Test
  void bindAndOverchargeMarkOnlyThePreviousEffect() {
    CompiledExecutionPlan plan = InscriptionCompiler.compile(new Inscription(List.of(g("self"), g("slow"), g("bind"), g("damage", "amount", 2.0), g("overcharge"))));
    var e = plan.allEffectsInOrder();
    assertEquals(1.0, e.get(0).parameters().get("bind"));
    assertNull(e.get(1).parameters().get("bind"));
    assertEquals(1.0, e.get(1).parameters().get("overcharge"));
    assertEquals(4.0, e.get(1).parameters().get("amount"), 1e-9, "dobra o dano");
  }

  @Test
  void afterHitsArmsTheSpellAndKeepsItsCounterParameter() {
    CompiledExecutionPlan plan = InscriptionCompiler.compile(new Inscription(List.of(g("self"), g("after_hits", "n", 4.0), g("heal"))));
    var seg = plan.segments().get(0);
    assertTrue(ArmedSpells.isTriggerSegment(seg));
    assertEquals(4.0, seg.condition().orElseThrow().parameters().get("n"));
    var clamped = InscriptionCompiler.compile(new Inscription(List.of(g("self"), g("after_hits", "n", 99.0), g("heal"))));
    assertEquals(10.0, clamped.segments().get(0).condition().orElseThrow().parameters().get("n"));
  }

  @Test
  void onSignalIsAPlainPassThroughCondition() {
    CompiledExecutionPlan plan = InscriptionCompiler.compile(new Inscription(List.of(g("self"), g("on_signal"), g("heal"))));
    assertFalse(ArmedSpells.isTriggerSegment(plan.segments().get(0)), "nao arma: so troca o gatilho da inscricao");
  }

  @Test
  void stealthAndFalseTraceAreDetectedFromTheInscription() {
    Inscription quiet = new Inscription(List.of(g("self"), g("heal"), g("stealth")));
    Inscription liar = new Inscription(List.of(g("self"), g("heal"), g("false_trace")));
    assertTrue(Traces.stealthy(quiet) && !Traces.falsified(quiet));
    assertTrue(Traces.falsified(liar) && !Traces.stealthy(liar));
    assertNotEquals(Traces.fakeSigner(1, 2, 3), Traces.fakeSigner(2, 2, 3));
    assertEquals(Traces.fakeSigner(1, 2, 3), Traces.fakeSigner(1, 2, 3), "estavel");
  }

  @Test
  void everyNewEffectValidatesAfterAForm() {
    for (String effect : List.of("satellites", "link", "trail", "hook", "climb", "phase", "recall", "imbue", "petrify", "acid", "electrify",
        "contagion", "reanimate", "sense", "arcane_sight", "emit_signal", "dispel", "reflect", "silence", "drain_mana", "swap", "reveal", "decoy", "curse")) {
      var result = InscriptionValidator.validate(new Inscription(List.of(g("self"), g(effect))));
      assertTrue(result.valid(), effect + ": " + result.message().getString());
    }
    for (String modifier : List.of("stealth", "false_trace")) {
      var result = InscriptionValidator.validate(new Inscription(List.of(g("self"), g("heal"), g(modifier))));
      assertTrue(result.valid(), modifier + ": " + result.message().getString());
    }
  }
}
