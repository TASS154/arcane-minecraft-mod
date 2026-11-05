package com.arcane.ambient;

import com.arcane.block.ManaCrystalBlock;
import com.arcane.execution.EnvironmentalSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class AmbientFieldResolver {
  private static final int[] ZONE_RADII = {32, 24, 16, 10, 5};
  private static final double[] ZONE_MULTIPLIERS = {2, 4, 8, 16, 32};
  /** Alem da maior zona o cristal nao tem efeito, entao nao ha por que procurar mais longe. */
  private static final int SEARCH_RADIUS = ZONE_RADII[0];

  private AmbientFieldResolver() {}

  public static EnvironmentalSnapshot resolve(ServerLevel level, BlockPos pos) {
    double bestMultiplier = 1.0;
    int zone = 0;
    BlockPos crystalPos = findNearestCrystal(level, pos, SEARCH_RADIUS);
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

  /**
   * Cristal mais proximo dentro do raio. Percorre secao a secao (16x16x16) e pula, pela paleta,
   * as secoes que nao podem conter cristal; assim acha qualquer cristal sem varrer bloco a bloco.
   * So olha chunks ja carregados, para nunca forcar carregamento.
   */
  static BlockPos findNearestCrystal(ServerLevel level, BlockPos origin, int radius) {
    BlockPos nearest = null;
    double nearestDist = (double) radius * radius;
    int minCx = SectionPos.blockToSectionCoord(origin.getX() - radius);
    int maxCx = SectionPos.blockToSectionCoord(origin.getX() + radius);
    int minCz = SectionPos.blockToSectionCoord(origin.getZ() - radius);
    int maxCz = SectionPos.blockToSectionCoord(origin.getZ() + radius);
    int minSy = Math.max(level.getMinSection(), SectionPos.blockToSectionCoord(origin.getY() - radius));
    int maxSy = Math.min(level.getMaxSection() - 1, SectionPos.blockToSectionCoord(origin.getY() + radius));
    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    for (int cx = minCx; cx <= maxCx; cx++) {
      for (int cz = minCz; cz <= maxCz; cz++) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) continue;
        LevelChunkSection[] sections = chunk.getSections();
        for (int sy = minSy; sy <= maxSy; sy++) {
          LevelChunkSection section = sections[level.getSectionIndexFromSectionY(sy)];
          if (section.hasOnlyAir() || !section.maybeHas(ManaCrystalBlock::isCrystal)) continue;
          int baseX = SectionPos.sectionToBlockCoord(cx);
          int baseY = SectionPos.sectionToBlockCoord(sy);
          int baseZ = SectionPos.sectionToBlockCoord(cz);
          for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
              for (int x = 0; x < 16; x++) {
                BlockState state = section.getBlockState(x, y, z);
                if (!ManaCrystalBlock.isCrystal(state)) continue;
                cursor.set(baseX + x, baseY + y, baseZ + z);
                double dist = cursor.distSqr(origin);
                if (dist <= nearestDist) {
                  nearestDist = dist;
                  nearest = cursor.immutable();
                }
              }
            }
          }
        }
      }
    }
    return nearest;
  }
}
