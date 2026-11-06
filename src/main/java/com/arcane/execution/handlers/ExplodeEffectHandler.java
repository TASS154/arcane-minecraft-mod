package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Explosao que fere criaturas mas nunca destroi blocos. */
public class ExplodeEffectHandler implements EffectHandler {
  private static final int MAX_EXPLOSIONS = 3;
  private static final float MAX_RADIUS = 8.0f;

  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    float cap = MAX_RADIUS * (float) Math.min(2.0, Math.max(1.0, context.areaScale()));
    float radius = (float) Math.max(1.0, Math.min(cap, call.parameters().getOrDefault("radius", 2.0) * context.areaScale()));
    int count = 0;
    for (Vec3 pos : Targeting.positions(targets)) {
      if (count++ >= MAX_EXPLOSIONS) break;
      context.level().sendParticles(com.arcane.registry.ModParticles.ARCANE_SHOCK.get(), pos.x, pos.y + 0.3, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
      context.level().explode(context.caster(), pos.x, pos.y, pos.z, radius, Level.ExplosionInteraction.NONE);
    }
  }
}
