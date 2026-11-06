package com.arcane.inscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VowFormulaTest {
  @Test
  void bonusFollowsTheFormula() {
    assertEquals(0.28, VowFormula.bonus(VowFormula.STILLNESS, 1), 1e-9);
    assertEquals(0.28 * Math.pow(2, 0.85), VowFormula.bonus(VowFormula.STILLNESS, 2), 1e-9);
    assertEquals(50, VowFormula.percent(VowFormula.STILLNESS, 2));
    assertEquals(54, VowFormula.percent(VowFormula.BLOOD, 2));
    assertEquals(45, VowFormula.percent(VowFormula.DAYLIGHT, 2));
  }

  @Test
  void higherPledgeAlwaysGivesMoreButWithDiminishingSteps() {
    double prevGain = Double.MAX_VALUE;
    double prev = 0;
    for (int n = 1; n <= 5; n++) {
      double b = VowFormula.bonus(VowFormula.BLOOD, n);
      assertTrue(b > prev, "deve crescer");
      double gain = b - prev;
      if (n > 1) assertTrue(gain < prevGain, "cada degrau vale menos que o anterior");
      prevGain = gain;
      prev = b;
    }
  }

  @Test
  void pledgeIsClampedAndMultiplierUsesIt() {
    var crazy = new GlyphInstance(VowFormula.BLOOD).withParameter("pledge", 9999);
    assertEquals(5, VowFormula.pledge(crazy));
    assertEquals(1, VowFormula.pledge(new GlyphInstance(VowFormula.BLOOD).withParameter("pledge", -3)));
    assertEquals(VowFormula.DEFAULT_PLEDGE, VowFormula.pledge(new GlyphInstance(VowFormula.BLOOD)));
    assertEquals(1.0 + VowFormula.bonus(VowFormula.BLOOD, 5), VowFormula.multiplier(crazy), 1e-9);
  }

  @Test
  void ceilingWithTwoVowsStaysBounded() {
    double top = (1 + VowFormula.bonus(VowFormula.BLOOD, 5)) * (1 + VowFormula.bonus(VowFormula.STILLNESS, 5));
    assertTrue(top < 5.0, "teto com 2 juramentos: " + top);
  }
}
