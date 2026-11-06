package com.arcane.client;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SealGeometryTest {
  @Test
  void eachCategoryHasItsOwnShape() {
    assertEquals(3, SealGeometry.sidesFor("form"));
    assertEquals(SealGeometry.CIRCLE_SIDES, SealGeometry.sidesFor("effect"));
    assertEquals(4, SealGeometry.sidesFor("filter"));
    assertEquals(4, SealGeometry.sidesFor("modifier"));
    assertEquals(5, SealGeometry.sidesFor("condition"));
    assertEquals(6, SealGeometry.sidesFor("activator"));
    assertEquals(45.0, SealGeometry.baseRotationFor("modifier"), "losango = quadrado girado");
  }

  @Test
  void ringsGrowOutwardsSoEachGlyphWrapsThePreviousOne() {
    double previous = 0;
    for (int i = 0; i < 6; i++) {
      double r = SealGeometry.radiusOf(i, 6, 30);
      assertTrue(r > previous, "o anel " + i + " deve ser maior que o anterior");
      previous = r;
    }
    assertEquals(30.0, SealGeometry.radiusOf(5, 6, 30), 1e-9, "o ultimo ocupa o raio maximo");
    assertTrue(SealGeometry.radiusOf(0, 6, 30) < 10, "o primeiro fica no centro");
  }

  @Test
  void polygonVerticesLieOnTheCircle() {
    for (int[] v : SealGeometry.polygon(5, 50, 50, 20, 0)) {
      double d = Math.hypot(v[0] - 50, v[1] - 50);
      assertEquals(20.0, d, 1.0);
    }
    assertEquals(6, SealGeometry.polygon(6, 0, 0, 10, 0).size());
  }

  @Test
  void lineIsContinuousAndEndsWhereItShould() {
    List<int[]> cells = SealGeometry.line(0, 0, 9, 4);
    assertArrayEquals(new int[] {0, 0}, cells.get(0));
    assertArrayEquals(new int[] {9, 4}, cells.get(cells.size() - 1));
    for (int i = 1; i < cells.size(); i++) {
      assertTrue(Math.abs(cells.get(i)[0] - cells.get(i - 1)[0]) <= 1 && Math.abs(cells.get(i)[1] - cells.get(i - 1)[1]) <= 1, "sem saltos");
    }
  }

  @Test
  void sealRotationIsStableAndSignaturesDiffer() {
    assertEquals(SealGeometry.rotationFor("arcane:fire"), SealGeometry.rotationFor("arcane:fire"));
    Set<Double> rotations = new HashSet<>();
    for (String id : List.of("arcane:fire", "arcane:damage", "arcane:heal", "arcane:slow", "arcane:push", "arcane:ray"))
      rotations.add(SealGeometry.rotationFor(id));
    assertTrue(rotations.size() >= 4, "glifos diferentes giram de formas diferentes");
  }

  @Test
  void differentSpellsDrawDifferentSeals() {
    var a = SealGeometry.ring("form", "arcane:projectile", 50, 50, 20);
    var b = SealGeometry.ring("effect", "arcane:damage", 50, 50, 20);
    assertNotEquals(a.size(), b.size());
    assertFalse(a.isEmpty());
  }
}
