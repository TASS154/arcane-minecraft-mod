package com.arcane.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChorusMathTest {
  @Test
  void multiplierGrowsPerExtraParticipantAndIsCapped() {
    assertEquals(1.0, ChorusMath.multiplier(1), 1e-9);
    assertEquals(1.35, ChorusMath.multiplier(2), 1e-9);
    assertEquals(1.7, ChorusMath.multiplier(3), 1e-9);
    assertEquals(2.05, ChorusMath.multiplier(4), 1e-9);
    assertEquals(2.4, ChorusMath.multiplier(5), 1e-9, "5 participantes chegam ao teto");
    assertEquals(2.4, ChorusMath.multiplier(12), 1e-9);
  }

  @Test
  void shareSplitsTheCostRoundingUpWithAFloorOfOne() {
    assertEquals(5, ChorusMath.share(10, 2));
    assertEquals(4, ChorusMath.share(10, 3));
    assertEquals(1, ChorusMath.share(1, 4), "o piso e 1");
    assertEquals(0, ChorusMath.share(0, 3), "custo zero nao cobra");
    assertTrue(ChorusMath.share(10, 2) * 2 >= 10, "juntos, os participantes cobrem o custo");
    assertTrue(ChorusMath.share(10, 3) * 3 >= 10);
  }
}
