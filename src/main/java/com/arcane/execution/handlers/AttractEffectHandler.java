package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Puxa os alvos na direcao de quem conjura. */
public class AttractEffectHandler implements EffectHandler {
  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    double strength = Math.min(5.0, context.amplify(call.parameters().getOrDefault("strength", 1.0), call.magnitudeKeys(), "strength"));
    Vec3 center = context.caster().position();
    for (Entity entity : targets.entities()) {
      if (entity == context.caster()) continue;
      Vec3 toCaster = center.subtract(entity.position());
      if (toCaster.lengthSqr() < 0.25) continue;
      Vec3 pull = toCaster.normalize().scale(0.8 * strength).add(0, 0.15, 0);
      entity.setDeltaMovement(entity.getDeltaMovement().add(pull));
      entity.hurtMarked = true;
    }
  }
}
