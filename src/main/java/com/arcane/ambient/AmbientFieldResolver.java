package com.arcane.ambient;

import com.arcane.block.ManaCrystalBlock;
import com.arcane.execution.EnvironmentalSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class AmbientFieldResolver {
  private static final int[] ZONE_RADII = {32, 24, 16, 10, 5};
  private static final double[] ZONE_MULTIPLIERS = {2, 4, 8, 16, 32};

  private AmbientFieldResolver() {}

  public static EnvironmentalSnapshot resolve(ServerLevel level, BlockPos pos) {
    double bestMultiplier = 1.0;
    int zone = 0;
    BlockPos crystalPos = findNearestCrystal(level, pos, 64);
    if (crystalPos != null) {
      double dist = Math.sqrt(crystalPos.distSqr(pos));
      for (int i = 0; i < ZONE_RADII.length; i++) {
        if (dist <= ZONE_RADII[i]) {
          zone = i + 1;
          bestMultiplier = ZONE_MULTIPLIERS[i];
        }
      }
    }
    double stability = zone >= 3 ? Math.max(0.5, 1.0 - zone * 0.1) : 1.0;
    double poison = zone >= 3 ? (zone - 2) * 0.15 : 0.0;
    return new EnvironmentalSnapshot(bestMultiplier, stability, zone * 0.02, poison,
        java.util.List.of("zone:" + zone));
  }

  private static BlockPos findNearestCrystal(ServerLevel level, BlockPos origin, int radius) {
    BlockPos.MutableBlockPos nearest = null;
    double nearestDist = Double.MAX_VALUE;
    for (int x = -radius; x <= radius; x += 8) {
      for (int z = -radius; z <= radius; z += 8) {
        for (int y = -radius; y <= radius; y += 8) {
          BlockPos check = origin.offset(x, y, z);
          BlockState state = level.getBlockState(check);
          if (ManaCrystalBlock.isCrystal(state)) {
            double dist = check.distSqr(origin);
            if (dist < nearestDist) {
              nearestDist = dist;
              nearest = check.mutable();
            }
          }
        }
      }
    }
    return nearest == null ? null : nearest.immutable();
  }
}
