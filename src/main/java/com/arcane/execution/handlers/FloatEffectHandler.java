package com.arcane.execution.handlers;

import com.arcane.execution.EffectCall;
import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Flutuar: concede o efeito de voo suave. A velocidade (blocos por tick) vai na amplificacao como velocidade x10. */
public class FloatEffectHandler implements EffectHandler {
  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    double duration = context.amplify(call.parameters().getOrDefault("duration", 200.0), call.magnitudeKeys(), "duration");
    int ticks = (int) Math.max(20, Math.min(1200, duration));
    double speed = Math.max(0.3, Math.min(3.0, call.parameters().getOrDefault("speed", 0.6)));
    int amplifier = (int) Math.round(speed * 10.0);
    for (Entity entity : targets.entities()) {
      if (entity instanceof LivingEntity living) {
        living.addEffect(new MobEffectInstance(ModEffects.FLOAT_FLIGHT.get(), ticks, amplifier, false, false, true), context.caster());
      }
    }
  }
}
