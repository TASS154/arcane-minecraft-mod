package com.arcane.execution;

import com.arcane.TestGlyphs;
import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionCompiler;
import com.arcane.inscription.InscriptionValidator;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.arcane.TestGlyphs.g;
import static org.junit.jupiter.api.Assertions.*;

class Phase3GrammarTest {
  @BeforeAll
  static void loadGlyphs() {
    TestGlyphs.load();
  }

  @Test
  void newTriggersOpenAConditionSegmentWithTheirDefaults() {
    for (String trigger : List.of("on_kill", "on_fall", "on_jump")) {
      CompiledExecutionPlan plan = InscriptionCompiler.compile(new Inscription(List.of(g("self"), g(trigger), g("heal"))));
      var gate = plan.segments().get(0).condition();
      assertTrue(gate.isPresent() && gate.get().conditionId().getPath().equals(trigger), trigger);
      assertTrue(ArmedSpells.isTriggerSegment(plan.segments().get(0)), trigger + " arma a magia");
    }
    CompiledExecutionPlan health = InscriptionCompiler.compile(new Inscription(List.of(g("self"), g("target_health"), g("heal"))));
    assertEquals(30.0, health.segments().get(0).condition().orElseThrow().parameters().get("percent"));
    assertFalse(ArmedSpells.isTriggerSegment(health.segments().get(0)), "condicao de estado nao arma");
  }

  @Test
  void stateConditionParametersAreClamped() {
    CompiledExecutionPlan plan = InscriptionCompiler.compile(new Inscription(List.of(g("self"), g("target_health", "percent", 500.0), g("heal"))));
    assertEquals(95.0, plan.segments().get(0).condition().orElseThrow().parameters().get("percent"));
    CompiledExecutionPlan kind = InscriptionCompiler.compile(new Inscription(List.of(g("self"), g("holding", "kind", 99.0), g("heal"))));
    assertEquals(7.0, kind.segments().get(0).condition().orElseThrow().parameters().get("kind"));
  }

  @Test
  void newEffectsValidateAfterAForm() {
    for (String effect : List.of("charge", "taunt", "fear", "sleep", "rumble", "darkness", "wet", "lifesteal", "life_transfer", "read_item")) {
      var result = InscriptionValidator.validate(new Inscription(List.of(g("self"), g(effect))));
      assertTrue(result.valid(), effect + ": " + result.message().getString());
    }
  }

  @Test
  void regionAndHoldingVariantsAreNamed() {
    assertEquals(9, com.arcane.execution.handlers.StateConditions.REGIONS.length);
    assertEquals(8, com.arcane.execution.handlers.StateConditions.ITEMS.length);
  }
}
