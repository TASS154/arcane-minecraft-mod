package com.arcane.execution.handlers;

import com.arcane.execution.EffectCall;
import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Flutuar: concede o efeito de voo suave. A amplificacao leva a velocidade (decimos de bloco por tick) e a planagem (ver FlightService). */
public class FloatEffectHandler implements EffectHandler {
  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    double duration = context.amplify(call.parameters().getOrDefault("duration", 200.0), call.magnitudeKeys(), "duration");
    int ticks = (int) Math.max(20, Math.min(1200, duration));
    double speed = Math.max(0.3, Math.min(3.0, call.parameters().getOrDefault("speed", 0.6)));
    double glide = Math.max(0.0, Math.min(1.0, call.parameters().getOrDefault("glide", 0.0)));
    int amplifier = com.arcane.execution.FlightService.encode(speed, glide);
    for (Entity entity : targets.entities()) {
      if (entity instanceof LivingEntity living) {
        living.addEffect(new MobEffectInstance(ModEffects.FLOAT_FLIGHT.get(), ticks, amplifier, false, false, true), context.caster());
      }
    }
  }
}
