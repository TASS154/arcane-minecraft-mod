package com.arcane.inscription;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParameterCostCurveTest {
  @Test
  void costGrowsExponentiallyButGently() {
    assertEquals(1.0, SpellLimits.parameterFactor(1.0), 1e-9, "no padrao o custo e o base");
    assertEquals(0.5, SpellLimits.parameterFactor(0.2), 1e-9, "abaixo do padrao nunca passa de -50%");
    double two = SpellLimits.parameterFactor(2.0), four = SpellLimits.parameterFactor(4.0), eight = SpellLimits.parameterFactor(8.0);
    assertTrue(two > 2.0 && four > 4.0 && eight > 8.0, "acima do linear (exponencial)");
    assertTrue(four / two > two / SpellLimits.parameterFactor(1.0), "a razao entre degraus cresce");
    assertTrue(eight < 8.0 * 8.0, "mas bem abaixo de uma curva quadratica");
    assertTrue(SpellLimits.parameterFactor(1000.0) <= 40.0, "com teto");
    double prev = 0;
    for (double r = 0.5; r <= 12; r += 0.5) {
      double f = SpellLimits.parameterFactor(r);
      assertTrue(f >= prev, "monotona em " + r);
      prev = f;
    }
  }
}
