package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Efeito de pocao parametrizado: duracao (ticks, amplificada pelo ambiente) e nivel. */
public class PotionEffectHandler implements EffectHandler {
  private final MobEffect effect;
  private final int maxTicks;

  public PotionEffectHandler(MobEffect effect, int maxTicks) {
    this.effect = effect;
    this.maxTicks = maxTicks;
  }

  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    double duration = context.amplify(call.parameters().getOrDefault("duration", 100.0), call.magnitudeKeys(), "duration");
    int ticks = (int) Math.max(1, Math.min(maxTicks, duration));
    int level = (int) Math.max(0, Math.min(5, Math.round(call.parameters().getOrDefault("level", 0.0))));
    for (Entity entity : targets.entities()) {
      if (entity instanceof LivingEntity living) {
        living.addEffect(new MobEffectInstance(effect, ticks, level), context.caster());
      }
    }
  }
}
