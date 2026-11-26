package com.arcane.fx;

import com.arcane.execution.SpellElement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Maestria e assinatura (Fase M): so aparencia. O visual NAO foi visto em jogo. */
class CastStyleTest {
  @Test
  void masteryIsTheCostWeightedAverageOfKnowledge() {
    var ids = List.of("projectile", "damage", "explode");
    Map<String, Integer> cost = Map.of("projectile", 3, "damage", 1, "explode", 6);
    assertEquals(1.0, CastStyle.mastery(ids, Map.of("projectile", 4, "damage", 4, "explode", 4), cost::get), 1e-9);
    assertEquals(0.0, CastStyle.mastery(ids, Map.of(), cost::get), 1e-9, "nada conhecido: 0");
    double m = CastStyle.mastery(ids, Map.of("projectile", 4, "damage", 4, "explode", 2), cost::get);
    assertEquals((3 * 1.0 + 1 * 1.0 + 6 * 0.5) / 10, m, 1e-9, "o glifo caro pesa mais");
    assertEquals(1.0, CastStyle.mastery(List.of(), Map.of(), id -> 1), 1e-9);
  }

  @Test
  void lowMasteryIsRougherAndOneIsExactlyTheOldLook() {
    assertEquals(0.0, CastStyle.jitter(1.0), 1e-12, "maestria 1: sem tremida (como antes)");
    assertEquals(0.0, CastStyle.gapFraction(1.0), 1e-12);
    assertEquals(1.0, CastStyle.saturation(1.0), 1e-12);
    assertEquals(0.0, CastStyle.detune(1.0), 1e-12);
    assertTrue(CastStyle.jitter(0) > CastStyle.jitter(0.5) && CastStyle.gapFraction(0) > CastStyle.gapFraction(0.5));
    assertTrue(CastStyle.polished(0.9) && !CastStyle.polished(0.5));
    assertEquals(0xFF8C28, CastStyle.desaturate(0xFF8C28, 1.0), "saturacao 1: mesma cor");
    int washed = CastStyle.desaturate(0xFF0000, 0.0);
    assertEquals((washed >> 16) & 0xFF, washed & 0xFF, "saturacao 0: cinza");
  }

  @Test
  void signatureIsDeterministicAndPersonal() {
    UUID a = new UUID(1, 2), b = new UUID(3, 4);
    int sa = CastStyle.seed(a, SpellElement.FIRE, SpellElement.VOID);
    assertEquals(sa, CastStyle.seed(a, SpellElement.FIRE, SpellElement.VOID), "mesma pessoa, mesmo estilo: mesma assinatura");
    assertTrue(CastStyle.present(sa));
    assertFalse(CastStyle.present(0));
    assertEquals(SpellElement.FIRE, CastStyle.element(sa));
    assertTrue(CastStyle.motif(sa) >= 0 && CastStyle.motif(sa) <= 2);
    assertTrue(Math.abs(CastStyle.hueShift(sa)) <= CastStyle.MAX_HUE_SHIFT + 1e-9, "matiz so levemente deslocado");
    boolean differs = false;
    for (long i = 0; i < 40 && !differs; i++) differs = CastStyle.seed(new UUID(i, i * 7 + 1), SpellElement.FIRE, SpellElement.VOID) != sa;
    assertTrue(differs, "pessoas diferentes tem assinaturas diferentes");
    assertNotEquals(CastStyle.accent(sa, false), CastStyle.accent(sa, true), "paleta de daltonismo propria");
    int bSeed = CastStyle.seed(b, SpellElement.FROST, null);
    assertEquals(SpellElement.FROST, CastStyle.element(bSeed));
  }

  @Test
  void signatureStaysUnderTwentyPercent() {
    for (int total = 0; total <= 64; total++) assertTrue(CastStyle.signatureCount(total) <= total * 0.2 + 1e-9);
    assertEquals(2, CastStyle.signatureCount(14));
  }

  @Test
  void highMasteryAddsACleanChordOnly() {
    var notes = SpellVoice.compose(List.of("projectile", "damage"), id -> id.equals("projectile") ? SpellVoice.Kind.FORM : SpellVoice.Kind.EFFECT,
        id -> 1, SpellElement.FIRE, 1.0, false);
    assertEquals(notes, SpellVoice.withMastery(notes, 0.5), "maestria media: a mesma melodia");
    var rich = SpellVoice.withMastery(notes, 1.0);
    assertEquals(notes.size() + 1, rich.size());
    assertEquals(notes.get(notes.size() - 1).delay(), rich.get(rich.size() - 1).delay(), "acorde: junto da ultima nota");
  }
}
