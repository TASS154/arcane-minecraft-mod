package com.arcane.execution;

import com.arcane.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * "Sombra" das Barreiras em volta de uma explosao: um bloco esta protegido se, olhando do centro da explosao, ha um
 * bloco de Barreira mais perto na mesma direcao. Feito com uma grade de direcoes (1,5 grau): cada barreira marca as
 * direcoes que cobre com a distancia dela; um bloco e protegido se a direcao dele tem barreira mais perto que ele.
 * Custo: varrer o cubo da explosao uma vez procurando barreiras e marcar as que achou (poucas centenas, em geral).
 */
public final class BarrierShadow {
  private static final int YAW = 240, PITCH = 120; // 1,5 grau por celula
  private final Vec3 centre;
  private final float[] nearest = new float[YAW * PITCH];

  private BarrierShadow(Vec3 centre) {
    this.centre = centre;
    java.util.Arrays.fill(nearest, Float.MAX_VALUE);
  }

  /** Procura barreiras ate {@code radius} do centro; nulo se nao ha nenhuma (o caso comum: custo zero depois disso). */
  public static BarrierShadow scan(ServerLevel level, Vec3 centre, double radius) {
    var barrier = ModBlocks.ARCANE_BARRIER.get();
    int r = (int) Math.ceil(radius);
    BlockPos o = BlockPos.containing(centre);
    BarrierShadow shadow = null;
    BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
    for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) for (int dz = -r; dz <= r; dz++) {
      if (dx * dx + dy * dy + dz * dz > (r + 1) * (r + 1)) continue;
      p.set(o.getX() + dx, o.getY() + dy, o.getZ() + dz);
      if (!level.isLoaded(p) || !level.getBlockState(p).is(barrier)) continue;
      if (shadow == null) shadow = new BarrierShadow(centre);
      shadow.mark(p);
    }
    return shadow;
  }

  private int cell(double yaw, double pitch) {
    int i = (int) Math.floor((yaw + Math.PI) / (2 * Math.PI) * YAW);
    int j = (int) Math.floor((pitch + Math.PI / 2) / Math.PI * PITCH);
    return Math.floorMod(i, YAW) + Math.max(0, Math.min(PITCH - 1, j)) * YAW;
  }

  private void mark(BlockPos b) {
    Vec3 d = Vec3.atCenterOf(b).subtract(centre);
    double dist = d.length();
    if (dist < 0.5) return;
    double yaw = Math.atan2(d.z, d.x), pitch = Math.asin(Math.max(-1, Math.min(1, d.y / dist)));
    // o bloco cobre um angulo de ~0,75/dist radianos em volta do centro dele
    double ang = Math.min(Math.PI / 2, 0.75 / dist);
    double cosP = Math.max(0.05, Math.cos(pitch));
    int spanY = (int) Math.ceil(ang / cosP / (2 * Math.PI) * YAW), spanP = (int) Math.ceil(ang / Math.PI * PITCH);
    int ci = (int) Math.floor((yaw + Math.PI) / (2 * Math.PI) * YAW), cj = (int) Math.floor((pitch + Math.PI / 2) / Math.PI * PITCH);
    float near = (float) (dist - 0.5);
    for (int dj = -spanP; dj <= spanP; dj++) {
      int j = cj + dj;
      if (j < 0 || j >= PITCH) continue;
      for (int di = -Math.min(spanY, YAW / 2); di <= Math.min(spanY, YAW / 2); di++) {
        int k = Math.floorMod(ci + di, YAW) + j * YAW;
        if (near < nearest[k]) nearest[k] = near;
      }
    }
  }

  /** O bloco esta atras de uma barreira (visto do centro)? */
  public boolean shadowed(BlockPos b) {
    Vec3 d = Vec3.atCenterOf(b).subtract(centre);
    double dist = d.length();
    if (dist < 0.5) return false;
    return nearest[cell(Math.atan2(d.z, d.x), Math.asin(Math.max(-1, Math.min(1, d.y / dist))))] < dist - 0.5;
  }
}
