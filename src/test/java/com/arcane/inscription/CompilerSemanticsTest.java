package com.arcane.inscription;

import com.arcane.TestGlyphs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.arcane.TestGlyphs.g;
import static org.junit.jupiter.api.Assertions.*;

/** Regras do compilador que antes nao funcionavam: modificadores, atraso, limites e custo. */
class CompilerSemanticsTest {
  @BeforeAll
  static void loadGlyphs() {
    TestGlyphs.load();
  }

  private static CompiledExecutionPlan plan(GlyphInstance... glyphs) {
    return InscriptionCompiler.compile(new Inscription(List.of(glyphs)));
  }

  private static double param(CompiledExecutionPlan plan, int effect, String key) {
    return plan.allEffectsInOrder().get(effect).parameters().get(key);
  }

  @Test
  void rangeModifierChangesDeliveryRange() {
    assertEquals(24.0, plan(g("ray")).delivery().range(), 1e-9);
    assertEquals(36.0, plan(g("ray"), g("damage"), g("more_range")).delivery().range(), 1e-9);
    assertEquals(36.0, plan(g("ray"), g("more_range"), g("damage")).delivery().range(), 1e-9, "a ordem nao importa");
  }

  @Test
  void touchRangeIsShortNotSixteen() {
    assertEquals(3.0, plan(g("touch"), g("damage")).delivery().range(), 1e-9);
  }

  @Test
  void areaWorksBeforeAndAfterTheEffect() {
    assertEquals(4.0, param(plan(g("projectile"), g("area"), g("explode")), 0, "radius"), 1e-9);
    assertEquals(4.0, param(plan(g("projectile"), g("explode"), g("area")), 0, "radius"), 1e-9);
    assertEquals(10.0, plan(g("aura"), g("damage"), g("area")).delivery().parameters().get("radius"), 1e-9,
        "Area tambem dobra o raio da forma");
  }

  @Test
  void powerAndLengthenTargetOnlyThePreviousEffect() {
    CompiledExecutionPlan p = plan(g("self"), g("damage", "amount", 4.0), g("power"), g("fire"));
    assertEquals(6.0, param(p, 0, "amount"), 1e-9);
    assertEquals(60.0, param(p, 1, "duration"), 1e-9, "Fogo nao e afetado");
    assertEquals(120.0, param(plan(g("self"), g("fire"), g("lengthen")), 0, "duration"), 1e-9);
    assertEquals(2.0, param(plan(g("self"), g("slow"), g("power")), 0, "level"), 1e-9, "Potencia soma um nivel");
  }

  @Test
  void swiftSpeedsUpTheProjectile() {
    assertEquals(2.25, plan(g("projectile"), g("damage"), g("swift")).delivery().parameters().get("speed"), 1e-9);
  }

  @Test
  void delayOpensANewDelayedSegment() {
    CompiledExecutionPlan p = plan(g("self"), g("damage"), g("delay", "seconds", 3.0), g("fire"));
    assertEquals(2, p.segments().size());
    assertEquals(0, p.segments().get(0).delayTicks());
    assertEquals(60, p.segments().get(1).delayTicks());
    assertEquals("arcane:fire", p.segments().get(1).effects().get(0).glyphId().toString());
  }

  @Test
  void afterSecondsUsesItsDefaultAndDoesNotLeakIntoOtherSegments() {
    CompiledExecutionPlan p = plan(g("self"), g("after_seconds"), g("damage"), g("sneaking"), g("heal"));
    assertEquals(40, p.segments().get(0).delayTicks(), "padrao de 2 s");
    assertEquals(0, p.segments().get(1).delayTicks(), "o atraso nao vaza para a condicao seguinte");
    assertTrue(p.segments().get(1).condition().isPresent());
  }

  @Test
  void conditionGatesOnlyItsOwnSegment() {
    CompiledExecutionPlan p = plan(g("projectile"), g("damage"), g("on_hit_entity"), g("fire"));
    assertTrue(p.segments().get(0).condition().isEmpty());
    assertEquals("arcane:on_hit_entity", p.segments().get(1).condition().orElseThrow().conditionId().toString());
  }

  @Test
  void filtersApplyToTheSegmentsAfterThem() {
    CompiledExecutionPlan p = plan(g("aura"), g("undead"), g("damage"));
    assertEquals(1, p.segments().get(0).filters().size());
    assertEquals("arcane:undead", p.segments().get(0).filters().get(0).filterId().toString());
  }

  @Test
  void parametersAreClampedToTheGlyphRange() {
    assertEquals(20.0, param(plan(g("self"), g("damage", "amount", 9999.0)), 0, "amount"), 1e-9);
    assertEquals(1.0, param(plan(g("self"), g("damage", "amount", -5.0)), 0, "amount"), 1e-9);
    assertEquals(4.0, param(plan(g("self"), g("damage", "inventado", 7.0)), 0, "amount"), 1e-9, "parametro desconhecido e ignorado");
  }

  @Test
  void effectCostScalesWithItsMainParameter() {
    int base = plan(g("self"), g("damage")).baseManaCost();           // 1 + 2
    int doubled = plan(g("self"), g("damage", "amount", 8.0)).baseManaCost(); // 1 + 4
    int half = plan(g("self"), g("damage", "amount", 2.0)).baseManaCost();    // 1 + 1
    assertEquals(3, base);
    assertEquals(5, doubled);
    assertEquals(2, half);
    assertEquals(plan(g("self"), g("slow")).baseManaCost(), plan(g("self"), g("slow", "level", 3.0)).baseManaCost(),
        "so o parametro principal (duracao) escala o custo");
  }

  @Test
  void activatorAloneStillProducesASegment() {
    assertEquals(1, plan(g("activate")).segments().size());
  }
}
