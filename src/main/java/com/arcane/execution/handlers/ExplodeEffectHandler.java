package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Explosao que fere criaturas mas nunca destroi blocos. */
public class ExplodeEffectHandler implements EffectHandler {
  private static final int MAX_EXPLOSIONS = 3;
  private static final float MAX_RADIUS = 8.0f;

  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    float cap = Math.min(40f, MAX_RADIUS * (float) Math.max(1.0, context.areaFactor()));
    float radius = (float) Math.max(1.0, Math.min(cap, call.parameters().getOrDefault("radius", 2.0) * context.areaFactor()));
    int count = 0;
    boolean cover = context.ritualRadius() > 0;
    List<Vec3> spots = cover ? com.arcane.execution.AreaCover.tile(context, radius, com.arcane.execution.AreaCover.MAX_BLASTS) : Targeting.positions(targets);
    int limit = cover ? com.arcane.execution.AreaCover.MAX_BLASTS : MAX_EXPLOSIONS;
    for (Vec3 pos : spots) {
      if (count++ >= limit) break;
      context.level().sendParticles(com.arcane.registry.ModParticles.ARCANE_SHOCK.get(), pos.x, pos.y + 0.3, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
      final float r = radius;
      com.arcane.execution.SpellImmunity.explodeAs(context.caster(), () -> context.level().explode(context.caster(), pos.x, pos.y, pos.z, r, Level.ExplosionInteraction.NONE));
      if (com.arcane.execution.Chain.enabled(call)) {
        com.arcane.execution.Chain.explosions(context, pos, radius, Level.ExplosionInteraction.NONE, 0,
            new java.util.HashSet<>(java.util.List.of(context.caster().getUUID())), com.arcane.execution.Chain.explosionBudget());
      }
    }
  }
}
