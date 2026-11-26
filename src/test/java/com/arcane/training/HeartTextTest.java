package com.arcane.training;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Dano em coracoes: 1 coracao = 2 HP, meio = 1 HP; acima de 10, "♥×N". */
class HeartTextTest {
  @Test
  void halvesRoundToTheNearestHalfHeart() {
    assertEquals(0, HeartText.halves(0f));
    assertEquals(1, HeartText.halves(0.3f), "dano positivo nunca vira zero");
    assertEquals(1, HeartText.halves(1f));
    assertEquals(7, HeartText.halves(7.2f));
    assertEquals(8, HeartText.halves(7.6f));
  }

  @Test
  void symbolsUseFullAndHalfHearts() {
    assertEquals("-", HeartText.symbols(0f));
    assertEquals("½", HeartText.symbols(1f));
    assertEquals("❤❤❤½", HeartText.symbols(7f));
    assertEquals("❤".repeat(10), HeartText.symbols(20f));
  }

  @Test
  void aboveTenHeartsUsesTheCompactForm() {
    assertEquals("♥×10,5", HeartText.symbols(21f));
    assertEquals("♥×50", HeartText.symbols(100f));
  }

  @Test
  void rateIsInHeartsPerSecond() {
    assertEquals("2,5 ❤/s", HeartText.rate(5f));
    assertEquals("0,0 ❤/s", HeartText.rate(-1f));
  }
}
