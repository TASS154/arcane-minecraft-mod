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

  // ---------------------------------------------------------------- design v2
  @Test
  void aModifierAfterEndGroupAffectsEveryEffectInTheGroup() {
    CompiledExecutionPlan p = plan(g("self"), g("group"), g("damage", "amount", 4.0), g("heal", "amount", 4.0),
        g("end_group"), g("power"), g("fire"));
    assertEquals(6.0, param(p, 0, "amount"), 1e-9, "Dano dentro do grupo");
    assertEquals(6.0, param(p, 1, "amount"), 1e-9, "Cura dentro do grupo");
    assertEquals(60.0, param(p, 2, "duration"), 1e-9, "Fogo, fora do grupo, nao muda");
  }

  @Test
  void withoutAGroupAModifierOnlyAffectsTheImmediatelyPreviousEffect() {
    CompiledExecutionPlan p = plan(g("self"), g("damage", "amount", 4.0), g("heal", "amount", 4.0), g("power"));
    assertEquals(4.0, param(p, 0, "amount"), 1e-9);
    assertEquals(6.0, param(p, 1, "amount"), 1e-9);
  }

  @Test
  void modifiersInsideAnOpenGroupStillApplyToTheirOwnPreviousEffect() {
    CompiledExecutionPlan p = plan(g("self"), g("group"), g("damage", "amount", 4.0), g("power"), g("heal", "amount", 4.0), g("end_group"));
    assertEquals(6.0, param(p, 0, "amount"), 1e-9);
    assertEquals(4.0, param(p, 1, "amount"), 1e-9);
  }

  @Test
  void weakenHalvesAndCheapensNothingElse() {
    assertEquals(2.0, param(plan(g("self"), g("damage", "amount", 4.0), g("weaken")), 0, "amount"), 1e-9);
    assertEquals(1.0, param(plan(g("self"), g("speed"), g("weaken")), 0, "level"), 1e-9, "Enfraquecer nao mexe em nivel");
  }

  @Test
  void repeatDuplicatesThePreviousEffectAndChargesForEachCopy() {
    CompiledExecutionPlan once = plan(g("self"), g("gravity"));
    CompiledExecutionPlan thrice = plan(g("self"), g("gravity"), g("repeat", "times", 3.0));
    assertEquals(1, once.allEffectsInOrder().size());
    assertEquals(3, thrice.allEffectsInOrder().size());
    assertTrue(thrice.baseManaCost() > once.baseManaCost() * 2, "cada copia custa de novo");
  }

  @Test
  void costGrowsExponentiallyWithGlyphCount() {
    com.arcane.inscription.SpellLimits.reset();
    assertEquals(1.0, com.arcane.inscription.SpellLimits.multiplier(1), 1e-9);
    assertEquals(1.0, com.arcane.inscription.SpellLimits.multiplier(3), 1e-9, "os 3 primeiros nao pagam sobretaxa");
    assertEquals(1.3, com.arcane.inscription.SpellLimits.multiplier(4), 1e-9);
    assertEquals(Math.pow(1.3, 7), com.arcane.inscription.SpellLimits.multiplier(10), 1e-9);
    double ratio = com.arcane.inscription.SpellLimits.multiplier(10) / com.arcane.inscription.SpellLimits.multiplier(9);
    assertEquals(1.3, ratio, 1e-9, "cada glifo a mais multiplica o custo (exponencial, nao linear)");
  }

  @Test
  void aTenGlyphSpellCostsMuchMoreThanTheSumOfItsParts() {
    com.arcane.inscription.SpellLimits.reset();
    CompiledExecutionPlan small = plan(g("self"), g("heal"), g("shield"));
    CompiledExecutionPlan big = plan(g("self"), g("heal"), g("shield"), g("speed"), g("heal"), g("shield"), g("speed"), g("heal"), g("shield"), g("speed"));
    int sum = 1 + 3 + 3 + 2 + 3 + 3 + 2 + 3 + 3 + 2;
    assertTrue(big.baseManaCost() > sum * 3, "custo " + big.baseManaCost() + " deveria passar muito de " + sum + " (soma simples)");
    assertTrue(small.baseManaCost() <= 8);
  }

  @Test
  void validatorRejectsSpellsOverTheLimit() {
    com.arcane.inscription.SpellLimits.reset();
    GlyphInstance[] ok = new GlyphInstance[10];
    ok[0] = g("self");
    for (int i = 1; i < 10; i++) ok[i] = g("heal");
    assertTrue(InscriptionValidator.validate(new Inscription(List.of(ok))).valid());
    GlyphInstance[] tooMany = new GlyphInstance[11];
    tooMany[0] = g("self");
    for (int i = 1; i < 11; i++) tooMany[i] = g("heal");
    var result = InscriptionValidator.validate(new Inscription(List.of(tooMany)));
    assertFalse(result.valid());
  }

  @Test
  void everyGlyphHasATierAndAFamily() {
    for (var def : com.arcane.glyph.GlyphRegistry.all().values()) {
      assertTrue(def.tier() >= 1 && def.tier() <= 4, def.id() + " tier");
      assertFalse(def.families().isEmpty(), def.id() + " familias");
    }
  }
}
