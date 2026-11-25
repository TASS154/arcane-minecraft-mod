package com.arcane.execution;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Pequenos raios eletricos que ligam dois pontos: o rastro de um Salto (Blink) ou de um Atravessar (Phase). So visual
 * (particulas e um estalo); poucas particulas por uso, e nada por tick.
 */
public final class ElectricTrail {
  private ElectricTrail() {}

  /** Um raio em zigue-zague de {@code from} ate {@code to}, mais faiscas nas duas pontas. */
  public static void draw(ServerLevel level, Vec3 from, Vec3 to) {
    Vec3 delta = to.subtract(from);
    double length = delta.length();
    if (length < 0.5) return;
    RandomSource rnd = level.getRandom();
    int segments = (int) Math.max(3, Math.min(14, Math.round(length * 1.6)));
    var spark = com.arcane.fx.Fx.options(com.arcane.execution.SpellElement.STORM, com.arcane.fx.Fx.Role.TRAIL, 0.8f, -1);
    for (int bolt = 0; bolt < 2; bolt++) {
      Vec3 prev = from;
      for (int i = 1; i <= segments; i++) {
        double t = i / (double) segments;
        double jitter = i == segments ? 0.0 : 0.32;
        Vec3 point = from.add(delta.scale(t)).add((rnd.nextDouble() - 0.5) * jitter, (rnd.nextDouble() - 0.5) * jitter, (rnd.nextDouble() - 0.5) * jitter);
        Vec3 step = point.subtract(prev);
        int dots = (int) Math.max(1, Math.round(step.length() * 3.0));
        for (int d = 0; d < dots; d++) {
          Vec3 p = prev.add(step.scale((d + 0.5) / dots));
          level.sendParticles(spark, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        prev = point;
      }
    }
    for (Vec3 end : new Vec3[] {from, to}) {
      com.arcane.fx.Fx.burst(level, null, end, com.arcane.execution.SpellElement.STORM, com.arcane.fx.Fx.Role.IMPACT, 12, 0.35, 0.15);
    }
    com.arcane.fx.Fx.sound(level, to, new net.minecraft.resources.ResourceLocation("arcane", "element.storm.impact"), 0.22f, 1.6f);
  }
}
