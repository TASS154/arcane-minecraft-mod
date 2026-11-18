package com.arcane.execution;

import com.arcane.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
          level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        prev = point;
      }
    }
    for (Vec3 end : new Vec3[] {from, to}) {
      level.sendParticles(ModParticles.ARCANE_BOLT.get(), end.x, end.y, end.z, 6, 0.3, 0.4, 0.3, 0.08);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 10, 0.35, 0.5, 0.35, 0.2);
    }
    level.playSound(null, to.x, to.y, to.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.18f, 1.9f);
  }
}
