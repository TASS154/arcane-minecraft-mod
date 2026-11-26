package com.arcane.fx;

import com.arcane.execution.SpellElement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Voz da magia (Fase L). Regras da melodia; o som em si NAO foi ouvido. */
class SpellVoiceTest {
  static final Map<String, SpellVoice.Kind> KINDS = Map.of("projectile", SpellVoice.Kind.FORM, "aura", SpellVoice.Kind.FORM,
      "power", SpellVoice.Kind.MODIFIER, "swift", SpellVoice.Kind.MODIFIER);
  static final Map<String, Integer> COST = Map.of("explode", 9, "thunder", 8, "damage", 2);

  static List<SpellVoice.Note> compose(List<String> ids, boolean big) {
    return SpellVoice.compose(ids, id -> KINDS.getOrDefault(id, SpellVoice.Kind.EFFECT), id -> COST.getOrDefault(id, 1),
        SpellElement.FIRE, 1.0, big);
  }

  @Test
  void sameInscriptionSameMelody() {
    var ids = List.of("projectile", "damage", "fire", "power");
    assertEquals(compose(ids, false), compose(ids, false));
    assertEquals(SpellVoice.hash("fire"), SpellVoice.hash("fire"));
    assertEquals(0xE40C292C, SpellVoice.hash("a"), "FNV-1a de 32 bits (valor de referencia), igual ao preview_voice.py");
  }

  @Test
  void orderChangesTheMelody() {
    var a = compose(List.of("projectile", "damage", "fire"), false);
    var b = compose(List.of("projectile", "fire", "damage"), false);
    assertNotEquals(a, b, "a ordem dos glifos muda a melodia");
    assertEquals(a.get(0), b.get(0), "a primeira nota (a forma) e a mesma");
  }

  @Test
  void categoryPicksTheOctave() {
    var n = compose(List.of("projectile", "damage", "power"), false);
    assertEquals(0, n.get(0).octave(), "forma grave");
    assertEquals(1, n.get(1).octave(), "efeito no meio");
    assertEquals(2, n.get(2).octave(), "modificador agudo");
    assertEquals(0, n.get(0).delay());
    assertEquals(SpellVoice.STEP_TICKS, n.get(1).delay());
  }

  @Test
  void atMostEightNotesKeepingFirstLastAndHeaviest() {
    List<String> ids = new ArrayList<>(List.of("projectile", "a", "b", "explode", "c", "d", "thunder", "e", "f", "power"));
    var n = compose(ids, false);
    assertEquals(SpellVoice.MAX_NOTES, n.size());
    var kept = SpellVoice.keptIndices(ids, id -> COST.getOrDefault(id, 1));
    assertEquals(0, kept.get(0), "a primeira fica");
    assertEquals(ids.size() - 1, kept.get(kept.size() - 1), "a ultima fica");
    assertTrue(kept.contains(ids.indexOf("explode")) && kept.contains(ids.indexOf("thunder")), "as mais pesadas ficam: " + kept);
    for (int i = 1; i < kept.size(); i++) assertTrue(kept.get(i) > kept.get(i - 1), "ordem original");
    assertEquals(SpellVoice.MAX_WITH_FLOURISH, compose(ids, true).size(), "magia grande: quinta e oitava a mais");
  }

  @Test
  void pitchAlwaysInsideMinecraftRange() {
    for (int d = 0; d < 5; d++) for (int o = 0; o < 3; o++) {
      float p = new SpellVoice.Note(d, o, 0, 1f, false).pitch();
      assertTrue(p >= 0.5f && p <= 2.0f, "grau " + d + " oitava " + o + ": " + p);
    }
    for (String id : List.of("fire", "frost", "explode", "a", "zzz", "projectile", "satellites")) {
      for (var n : compose(List.of("projectile", id, "power"), true)) assertTrue(n.pitch() >= 0.5f && n.pitch() <= 2.0f);
    }
    assertTrue(new SpellVoice.Note(0, 0, 0, 1f, false).pitch() < new SpellVoice.Note(0, 1, 0, 1f, false).pitch(), "forma mais grave que efeito");
  }

  @Test
  void bigSpellsAddAFifthAnOctaveAndATail() {
    var plain = compose(List.of("aura", "explode"), false);
    var big = compose(List.of("aura", "explode"), true);
    assertEquals(plain.size() + 2, big.size());
    assertTrue(big.get(big.size() - 1).longTail(), "cauda longa no fim");
    assertEquals(Math.min(2, plain.get(0).octave() + 1), big.get(big.size() - 1).octave(), "a oitava da primeira nota");
  }

  @Test
  void packRoundTrip() {
    var notes = compose(List.of("projectile", "damage", "fire", "power", "explode", "thunder"), true);
    var back = SpellVoice.unpack(SpellVoice.pack(notes));
    assertEquals(notes.size(), back.size());
    for (int i = 0; i < notes.size(); i++) {
      assertEquals(notes.get(i).degree(), back.get(i).degree());
      assertEquals(notes.get(i).octave(), back.get(i).octave());
      assertEquals(notes.get(i).delay(), back.get(i).delay());
      assertEquals(notes.get(i).longTail(), back.get(i).longTail());
      assertEquals(notes.get(i).volume(), back.get(i).volume(), 1f / 15);
    }
    assertTrue(SpellVoice.unpack(new long[3]).isEmpty(), "sem voz: nenhuma nota");
  }

  @Test
  void everyElementHasAFamilySample() {
    for (SpellElement e : SpellElement.values()) assertTrue(SpellVoice.FAMILIES.contains(SpellVoice.family(e)), e.name());
  }
}
