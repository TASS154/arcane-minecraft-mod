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
  void invertMarksOnlyThePreviousEffect() {
    CompiledExecutionPlan plan = InscriptionCompiler.compile(new Inscription(List.of(g("self"), g("damage"), g("invert"), g("push"))));
    var effects = plan.allEffectsInOrder();
    assertTrue(Inversion.inverted(effects.get(0).parameters()), "Dano foi invertido");
    assertFalse(Inversion.inverted(effects.get(1).parameters()), "Empurrao nao");
  }

  @Test
  void inversionPairsAreSymmetricAndPointToRealGlyphs() {
    for (String id : List.of("damage", "heal", "push", "attract", "slow", "speed", "fire", "freeze", "levitate", "gravity",
        "lifesteal", "life_transfer", "blind", "invisible", "fear", "taunt")) {
      String other = Inversion.opposite(id).orElseThrow(() -> new AssertionError("sem par: " + id));
      assertEquals(id, Inversion.opposite(other).orElseThrow(), "simetrico: " + id);
      assertTrue(com.arcane.glyph.GlyphRegistry.get(new ResourceLocation("arcane", other)).isPresent(), "glifo existe: " + other);
    }
    assertFalse(Inversion.hasOpposite("glide"));
  }

  @Test
  void resolveSwapsOnlyWhenInverted() {
    var plain = new EffectCall(new ResourceLocation("arcane", "damage"), new ResourceLocation("arcane", "damage"), Map.of("amount", 3.0), List.of());
    assertEquals("damage", Inversion.resolve(plain).glyphId().getPath());
    var inverted = new EffectCall(plain.glyphId(), plain.handler(), Map.of("amount", 3.0, "invert", 1.0), List.of());
    assertEquals("heal", Inversion.resolve(inverted).glyphId().getPath());
    assertEquals("heal", Inversion.resolve(inverted).handler().getPath());
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
    for (String effect : List.of("rain", "charge", "glide", "taunt", "fear", "sleep", "rumble", "darkness", "wet", "lifesteal", "life_transfer", "read_item")) {
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
