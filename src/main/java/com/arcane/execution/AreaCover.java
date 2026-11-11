package com.arcane.execution;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Pontos para cobrir o circulo inteiro de um ritual com explosoes de um raio dado (centro, depois aneis). */
public final class AreaCover {
  public static final int MAX_BLASTS = 25;

  private AreaCover() {}

  public static List<Vec3> tile(ExecutionContext ctx, double blastRadius, int max) {
    List<Vec3> out = new ArrayList<>();
    Vec3 c = ctx.ritualCenter();
    double r = ctx.ritualRadius();
    if (c == null || r <= 0) return out;
    double step = Math.max(2.0, blastRadius * 1.5);
    out.add(onGround(ctx, c.x, c.z, c.y));
    for (int ring = 1; ring * step <= r + step * 0.5 && out.size() < max; ring++) {
      double rr = Math.min(ring * step, r);
      int n = Math.max(6, (int) Math.ceil(2 * Math.PI * rr / step));
      for (int i = 0; i < n && out.size() < max; i++) {
        double a = 2 * Math.PI * i / n;
        out.add(onGround(ctx, c.x + Math.cos(a) * rr, c.z + Math.sin(a) * rr, c.y));
      }
    }
    return out;
  }

  private static Vec3 onGround(ExecutionContext ctx, double x, double z, double nearY) {
    int surface = ctx.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
    double y = Math.max(nearY - 4.0, Math.min(nearY + 4.0, surface));
    return new Vec3(x, y + 0.5, z);
  }
}
