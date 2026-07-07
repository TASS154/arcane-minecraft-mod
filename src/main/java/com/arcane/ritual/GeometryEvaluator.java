package com.arcane.ritual;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

public final class GeometryEvaluator {
  private GeometryEvaluator() {}

  public record GeometryReport(double score, double symmetry, double connectivity, double closure, double precision) {}

  public static GeometryReport evaluate(Set<BlockPos> strokes, BlockPos center, int expectedSize) {
    if (strokes.isEmpty()) return new GeometryReport(0, 0, 0, 0, 0);
    double symmetry = computeSymmetry(strokes, center);
    double connectivity = strokes.size() > 1 ? 1.0 : 0.5;
    double closure = isClosed(strokes) ? 1.0 : 0.4;
    double precision = Math.min(1.0, strokes.size() / (double) Math.max(1, expectedSize));
    double score = (symmetry * 0.3 + connectivity * 0.25 + closure * 0.25 + precision * 0.2) * 1.5;
    return new GeometryReport(Math.min(1.5, score), symmetry, connectivity, closure, precision);
  }

  private static double computeSymmetry(Set<BlockPos> strokes, BlockPos center) {
    int matches = 0;
    for (BlockPos pos : strokes) {
      BlockPos mirrored = new BlockPos(center.getX() - (pos.getX() - center.getX()), pos.getY(), pos.getZ());
      if (strokes.contains(mirrored)) matches++;
    }
    return strokes.isEmpty() ? 0 : (double) matches / strokes.size();
  }

  private static boolean isClosed(Set<BlockPos> strokes) {
    return strokes.size() >= 4;
  }
}
