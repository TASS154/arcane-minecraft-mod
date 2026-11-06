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

  @Test
  void oathBonusFollowsTheFormulaAndIsCapped() {
    assertEquals(0.30 * Math.log(1 + 30 / 5.0), VowFormula.oathBonus(1, 30), 1e-9);
    assertEquals(58, (int) Math.round(VowFormula.oathBonus(1, 30) * 100));
    assertTrue(VowFormula.oathBonus(3, 600) <= VowFormula.OATH_CAP + 1e-9);
    assertTrue(VowFormula.oathBonus(2, 300) > VowFormula.oathBonus(2, 30), "mais tempo, mais bonus");
    assertTrue(VowFormula.oathBonus(3, 30) > VowFormula.oathBonus(5, 30), "mana selada rende mais que cegueira");
    var wild = new GlyphInstance(VowFormula.OATH).withParameter("kind", 77).withParameter("seconds", 1e9);
    assertEquals(5, VowFormula.oathKind(wild));
    assertEquals(600.0, VowFormula.oathSeconds(wild));
    assertEquals(1.0 + VowFormula.oathBonus(5, 600), VowFormula.multiplier(wild), 1e-9);
  }
}
