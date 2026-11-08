package com.arcane.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class RegionTest {
  private static final Vec3 ORIGIN = new Vec3(0, 0, 0);
  private static final Vec3 NORTH = new Vec3(0, 0, -1);
  private static final Vec3 EAST = new Vec3(1, 0, 0);

  private static Region r(int shape, double radius, Vec3 facing) {
    return new Region(shape, ORIGIN, radius, shape == ShapeGeometry.WALL ? Region.wallHeight(radius) : Region.COLUMN_HEIGHT, facing);
  }

  @Test
  void coneOnlyCoversTheFrontWithinRange() {
    var cone = r(ShapeGeometry.CONE, 8, NORTH);
    assertTrue(cone.contains(new Vec3(0, 0, -5)), "na frente");
    assertTrue(cone.contains(new Vec3(3, 0, -5)), "dentro dos 50 graus");
    assertFalse(cone.contains(new Vec3(0, 0, 5)), "atras");
    assertFalse(cone.contains(new Vec3(6, 0, -1)), "fora do angulo");
    assertFalse(cone.contains(new Vec3(0, 0, -9)), "alem do alcance");
  }

  @Test
  void columnIsTallAndNarrow() {
    var col = r(ShapeGeometry.COLUMN, 2, NORTH);
    assertTrue(col.contains(new Vec3(1, 8, 1)));
    assertFalse(col.contains(new Vec3(3, 1, 0)), "fora do raio");
    assertFalse(col.contains(new Vec3(0, 12, 0)), "acima da altura");
  }

  @Test
  void wallFollowsTheFacingAxis() {
    var wall = r(ShapeGeometry.WALL, 3, NORTH); // along z: lateral = x
    assertTrue(wall.contains(new Vec3(1, 1, 0)));
    assertFalse(wall.contains(new Vec3(0, 1, 4)), "longe da parede");
    var eastWall = r(ShapeGeometry.WALL, 3, EAST);
    assertTrue(eastWall.contains(new Vec3(0, 1, 1)));
    assertFalse(eastWall.contains(new Vec3(4, 1, 0)));
  }

  @Test
  void everyShapeHasAShellWithinTheCapAndWallMatchesTheOldSize() {
    for (int shape = ShapeGeometry.DOME; shape <= ShapeGeometry.WALL; shape++) {
      var shell = r(shape, 5, NORTH).shell();
      assertFalse(shell.isEmpty(), "formato " + shape + " sem casca");
      assertTrue(shell.size() <= ShapeGeometry.MAX_BLOCKS);
    }
    assertEquals(9, r(ShapeGeometry.WALL, 3, NORTH).shell().size(), "parede 3x3 com raio 3");
    assertEquals(3, Region.wallHalf(3) * 2 + 1);
    assertEquals(15, Region.wallHalf(12) * 2 + 1, "ritual: 15 de largura");
    assertEquals(12, Region.wallHeight(12));
  }
}
