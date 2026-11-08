package com.arcane.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShapeGeometryTest {
  @Test
  void domeIsAnUpperHollowShell() {
    var shell = ShapeGeometry.shell(ShapeGeometry.DOME, 5);
    assertFalse(shell.isEmpty());
    assertTrue(shell.stream().allMatch(o -> o.dy() >= 0), "domo nao tem parte abaixo do centro");
    assertTrue(shell.stream().allMatch(o -> {
      double d = Math.sqrt(o.dx() * o.dx() + o.dy() * o.dy() + o.dz() * o.dz());
      return d >= 4.5 && d < 5.5;
    }));
    assertFalse(shell.stream().anyMatch(o -> o.dx() == 0 && o.dy() == 0 && o.dz() == 0), "oco por dentro");
    assertTrue(shell.stream().anyMatch(o -> o.dy() == 5 && o.dx() == 0 && o.dz() == 0), "topo do domo");
  }

  @Test
  void sphereIsSymmetricAndRingIsFlat() {
    var sphere = ShapeGeometry.shell(ShapeGeometry.SPHERE, 4);
    assertTrue(sphere.stream().anyMatch(o -> o.dy() == -4), "esfera tem fundo");
    assertTrue(sphere.stream().anyMatch(o -> o.dy() == 4));
    var ring = ShapeGeometry.shell(ShapeGeometry.RING, 6);
    assertTrue(ring.stream().allMatch(o -> o.dy() == 0 || o.dy() == 1), "anel tem 2 de altura");
  }

  @Test
  void shellsAreCappedAndRadiusIsClamped() {
    assertTrue(ShapeGeometry.shell(ShapeGeometry.DOME, 9999).size() <= ShapeGeometry.MAX_BLOCKS);
    assertEquals(16.0, ShapeGeometry.clampRadius(ShapeGeometry.DOME, 9999));
    assertEquals(ShapeGeometry.MIN_RADIUS, ShapeGeometry.clampRadius(ShapeGeometry.SPHERE, -5));
    assertEquals(ShapeGeometry.DEFAULT, ShapeGeometry.shapeOf(77));
    assertTrue(ShapeGeometry.shell(ShapeGeometry.DEFAULT, 5).isEmpty());
  }

  @Test
  void containsMatchesTheVolume() {
    assertTrue(ShapeGeometry.contains(ShapeGeometry.DOME, 0, 2, 0, 5));
    assertFalse(ShapeGeometry.contains(ShapeGeometry.DOME, 0, -3, 0, 5), "abaixo do chao nao e dentro do domo");
    assertTrue(ShapeGeometry.contains(ShapeGeometry.SPHERE, 0, -3, 0, 5));
    assertFalse(ShapeGeometry.contains(ShapeGeometry.SPHERE, 9, 0, 0, 5));
    assertTrue(ShapeGeometry.contains(ShapeGeometry.RING, 5, 0, 0, 5));
    assertFalse(ShapeGeometry.contains(ShapeGeometry.RING, 0, 0, 0, 5), "o centro do anel e vazio");
  }
}
