package com.arcane.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Orcamento de particulas: no maximo linear no raio e escalado pela qualidade. */
class FxBudgetTest {
  @Test
  void areaCountIsLinearInTheRadius() {
    int r8 = Fx.areaCount(8, 2.0, 6), r64 = Fx.areaCount(64, 2.0, 6), r128 = Fx.areaCount(128, 2.0, 6);
    assertEquals(16, r8);
    assertEquals(2 * r64, r128, "dobrar o raio dobra as particulas (nao quadruplica)");
    assertEquals(16 * r8, r128);
    assertEquals(6, Fx.areaCount(0.5, 2.0, 6), "areas pequenas tem um minimo");
  }

  @Test
  void qualityScalesButNeverZeroesARequest() {
    assertEquals(25, Fx.scaled(100, 0.25));
    assertEquals(200, Fx.scaled(100, 2.0));
    assertEquals(200, Fx.scaled(100, 9.0), "qualidade limitada a 2x");
    assertEquals(1, Fx.scaled(1, 0.25), "um pedido positivo manda ao menos uma");
    assertEquals(0, Fx.scaled(0, 1.0));
  }
}
