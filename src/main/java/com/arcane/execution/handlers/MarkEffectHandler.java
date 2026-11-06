package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.Marks;
import com.arcane.execution.TargetList;
import net.minecraft.world.entity.Entity;

/** Marca as criaturas alvo (nao fere). Os filtros Marcados/Nao Marcados as reconhecem depois. */
public class MarkEffectHandler implements EffectHandler {
  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    double duration = context.amplify(call.parameters().getOrDefault("duration", 200.0), call.magnitudeKeys(), "duration");
    int ticks = (int) Math.max(20, Math.min(2400, duration));
    long until = context.level().getGameTime() + ticks;
    for (Entity entity : targets.entities()) {
      Marks.mark(entity, until, ticks);
    }
  }
}
