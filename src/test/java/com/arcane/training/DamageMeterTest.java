package com.arcane.training;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Medidor de dano do Boneco de Treino e da simulacao de balanco. */
class DamageMeterTest {
  @Test
  void damageAccumulatesAndTracksTheLastHit() {
    DamageMeter m = new DamageMeter();
    m.record(10, 4f, "spell");
    m.record(15, 2.5f, "spell");
    assertEquals(6.5f, m.total(), 1e-6);
    assertEquals(2, m.count());
    assertEquals(2.5f, m.lastHit(), 1e-6);
    assertEquals(5, m.sinceLast(20));
  }

  @Test
  void perSecondUsesTheLastFiveSeconds() {
    DamageMeter m = new DamageMeter();
    m.record(0, 10f, "a");
    m.record(90, 10f, "a");
    assertEquals(4f, m.perSecond(99), 1e-6, "20 HP em 5 s = 4 HP/s");
    assertEquals(2f, m.perSecond(150), 1e-6, "o golpe do tick 0 saiu da janela");
  }

  @Test
  void idleSessionIsKeptUntilTheNextHit() {
    DamageMeter m = new DamageMeter();
    m.record(0, 3f, "a");
    m.noteReaction("steam");
    m.noteManaCost(12);
    m.markIdle();
    assertEquals(3f, m.total(), 1e-6, "o relatorio continua mostrando a sessao fechada");
    assertEquals(12, m.lastManaCost());
    m.record(500, 1f, "b");
    assertEquals(1f, m.total(), 1e-6, "o proximo golpe abre uma sessao nova");
    assertTrue(m.reactions().isEmpty());
  }

  @Test
  void reactionsAreCounted() {
    DamageMeter m = new DamageMeter();
    m.noteReaction("steam");
    m.noteReaction("steam");
    m.noteReaction("shatter");
    assertEquals(2, m.reactions().get("steam"));
    assertEquals(1, m.reactions().get("shatter"));
  }

  @Test
  void zeroAndNegativeHitsAreIgnored() {
    DamageMeter m = new DamageMeter();
    m.record(0, 0f, "a");
    m.record(0, -1f, "a");
    assertEquals(0, m.count());
  }

  @Test
  void trackedMetersBelongToTheirEntity() {
    Object a = new Object(), b = new Object();
    DamageMeter.track(a).record(0, 5f, "x");
    assertEquals(5f, DamageMeter.of(a).total(), 1e-6);
    assertNull(DamageMeter.of(b));
    DamageMeter.untrack(a);
    assertNull(DamageMeter.of(a));
  }
}
