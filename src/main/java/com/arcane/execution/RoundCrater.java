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
  /** Raio maximo com o teto fixo de area ligado (esfera de 24 = ~58 mil blocos, barato de uma vez). */
  public static final float MAX_RADIUS = 24f;

  /** Raio maximo da cratera: 24 com o teto fixo ligado; desligado, craterMaxRadius da config (0 = sem limite). */
  public static float maxRadius() {
    if (com.arcane.config.ArcaneConfig.AREA_HARD_CAPS.get()) return MAX_RADIUS;
    int cfg = com.arcane.config.ArcaneConfig.CRATER_MAX_RADIUS.get();
    return cfg <= 0 ? 4096f : (float) cfg;
  }
  /** Blocos com resistencia a explosao acima disso nunca quebram. */
  static final float UNBREAKABLE = 40f;

  private RoundCrater() {}

  public static List<BlockPos> blocks(ServerLevel level, Vec3 center, float radius) {
    List<BlockPos> out = new ArrayList<>();
    var longs = blockLongs(level, center, radius);
    for (int i = 0; i < longs.size(); i++) out.add(BlockPos.of(longs.getLong(i)));
    return out;
  }

  /** Igual a {@link #blocks}, mas guarda cada posicao como um long (8 bytes): uma cratera enorme nao enche a memoria de objetos. */
  public static it.unimi.dsi.fastutil.longs.LongArrayList blockLongs(ServerLevel level, Vec3 center, float radius) {
    float r = Math.min(maxRadius(), radius);
    it.unimi.dsi.fastutil.longs.LongArrayList out = new it.unimi.dsi.fastutil.longs.LongArrayList();
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
          if (dist <= edge) out.add(pos.asLong());
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
