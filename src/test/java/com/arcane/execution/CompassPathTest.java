package com.arcane.execution;

import com.arcane.ritual.CompassPath;
import com.arcane.ritual.RitualTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompassPathTest {
  @Test
  void coversEveryCellOfTheCircleExactlyOnce() {
    for (int radius : new int[] {4, 8, 16}) {
      CompassPath path = CompassPath.of(radius);
      assertEquals(RitualTemplate.cells(radius).size(), path.cells.size(), "raio " + radius);
      assertEquals(path.cells.size(), new java.util.HashSet<>(path.cells).size(), "sem repetidos, raio " + radius);
    }
  }

  @Test
  void ringsComeFirstAndThePenDoesNotTeleportBetweenAdjacentTraces() {
    CompassPath path = CompassPath.of(8);
    // O primeiro traco esta no anel externo (distancia ~ raio), nao numa haste.
    var first = path.cells.get(0);
    assertTrue(Math.abs(Math.hypot(first.dx(), first.dz()) - 8) <= 1.0, "comeca pelo anel externo");
    int jumps = 0;
    for (int i = 1; i < path.cells.size(); i++) {
      var a = path.cells.get(i - 1);
      var b = path.cells.get(i);
      double d = Math.hypot(a.dx() - b.dx(), a.dz() - b.dz());
      if (path.jump[i]) jumps++;
      else assertTrue(d <= 2.0, "traco continuo: salto escondido de " + d + " em " + i);
    }
    assertTrue(jumps <= 14, "poucos saltos (so entre aneis e hastes): " + jumps);
  }

  @Test
  void penPositionIsContinuousAlongThePath() {
    CompassPath path = CompassPath.of(8);
    double[] prev = path.at(0);
    for (double t = 0.1; t <= path.total; t += 0.1) {
      double[] cur = path.at(t);
      assertTrue(Math.hypot(cur[0] - prev[0], cur[1] - prev[1]) < 1.2, "a caneta nao pula em t=" + t);
      prev = cur;
    }
    assertEquals(path.cells.size(), path.reached(path.total));
  }
}
