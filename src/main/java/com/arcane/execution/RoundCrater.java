package com.arcane.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Cratera esferica das explosoes destrutivas de magia. O vanilla sorteia raios a partir de uma grade cubica e, em raios
 * grandes, a cratera sai quadrada e cheia de espinhos; aqui e uma esfera de verdade, com a borda levemente irregular e
 * mais curta em blocos resistentes (a pedra aguenta mais que a terra; obsidiana e rocha-mae ficam).
 */
public final class RoundCrater {
  /** Raio maximo varrido (esfera de 24 = ~58 mil blocos, ainda barato de uma vez). */
  public static final float MAX_RADIUS = 24f;
  /** Blocos com resistencia a explosao acima disso nunca quebram. */
  static final float UNBREAKABLE = 40f;

  private RoundCrater() {}

  public static List<BlockPos> blocks(ServerLevel level, Vec3 center, float radius) {
    float r = Math.min(MAX_RADIUS, radius);
    List<BlockPos> out = new ArrayList<>();
    int reach = (int) Math.ceil(r);
    BlockPos origin = BlockPos.containing(center);
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    for (int dx = -reach; dx <= reach; dx++) {
      for (int dy = -reach; dy <= reach; dy++) {
        for (int dz = -reach; dz <= reach; dz++) {
          pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
          if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) continue;
          double dist = Math.sqrt(sq(pos.getX() + 0.5 - center.x) + sq(pos.getY() + 0.5 - center.y) + sq(pos.getZ() + 0.5 - center.z));
          if (dist > r) continue;
          BlockState state = level.getBlockState(pos);
          if (state.isAir()) continue;
          float resistance = state.getBlock().getExplosionResistance();
          if (resistance >= UNBREAKABLE || state.getDestroySpeed(level, pos) < 0f) continue;
          // borda: a casca externa (acima de 85% do raio) pode sobrar um pouco, e blocos duros cedem menos
          double edge = r * (0.88 + 0.12 * hash01(pos)) - resistance * 0.12;
          if (dist <= edge) out.add(pos.immutable());
        }
      }
    }
    return out;
  }

  private static double sq(double v) { return v * v; }

  private static double hash01(BlockPos p) {
    long h = p.asLong() * 0x9E3779B97F4A7C15L;
    h ^= h >>> 29;
    return ((h & 0xFFFF) / 65535.0);
  }
}
