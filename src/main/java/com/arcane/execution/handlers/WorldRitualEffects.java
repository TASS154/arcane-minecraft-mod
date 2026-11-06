package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import com.arcane.registry.ModParticles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Efeitos de escala de mundo (so existem como ritual). */
public final class WorldRitualEffects {
  private WorldRitualEffects() {}

  /** Clima: 0 limpo, 1 chuva, 2 tempestade (dura 5 a 10 minutos). */
  public static class Weather implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int mode = (int) Math.max(0, Math.min(2, Math.round(call.parameters().getOrDefault("mode", 1.0))));
      int duration = 6000 + context.level().random.nextInt(6000);
      switch (mode) {
        case 0 -> context.level().setWeatherParameters(duration, 0, false, false);
        case 1 -> context.level().setWeatherParameters(0, duration, true, false);
        default -> context.level().setWeatherParameters(0, duration, true, true);
      }
      Vec3 c = context.caster().position();
      for (int i = 0; i < 40; i++) {
        context.level().sendParticles(ModParticles.ARCANE_SPARK.get(), c.x, c.y + 2 + i * 0.6, c.z, 1, 0.4, 0.0, 0.4, 0.0);
      }
    }
  }

  /** Dobrar: leva todas as criaturas alvo (ate 48) ao ponto mirado. Com Aura/Pilar e um circulo grande, desloca uma regiao. */
  public static class Warp implements EffectHandler {
    private static final int MAX_ENTITIES = 48;

    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      double range = Math.max(8.0, Math.min(64.0, call.parameters().getOrDefault("range", 32.0)));
      Vec3 dest = Targeting.ray(context, range, false).end();
      int moved = 0;
      for (Entity entity : targets.entities()) {
        if (moved >= MAX_ENTITIES) break;
        if (!(entity instanceof LivingEntity living)) continue;
        Vec3 from = living.position();
        double jitterX = (context.level().random.nextDouble() - 0.5) * 3.0;
        double jitterZ = (context.level().random.nextDouble() - 0.5) * 3.0;
        if (living.randomTeleport(dest.x + jitterX, dest.y, dest.z + jitterZ, true)) {
          context.level().sendParticles(ModParticles.ARCANE_SPARK.get(), from.x, from.y + 1.0, from.z, 8, 0.3, 0.5, 0.3, 0.05);
          moved++;
        }
      }
    }
  }
}
