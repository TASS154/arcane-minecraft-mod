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
    // Chao ou ponto vazio: o efeito fica numa nuvem que paira ali por alguns segundos (no maximo 2 nuvens).
    java.util.List<net.minecraft.world.phys.Vec3> spots = new java.util.ArrayList<>();
    for (var block : targets.blocks()) spots.add(net.minecraft.world.phys.Vec3.atBottomCenterOf(block.pos().above()));
    for (var point : targets.points()) spots.add(point);
    int clouds = 0;
    for (var spot : spots) {
      if (clouds++ >= 2) break;
      var cloud = new net.minecraft.world.entity.AreaEffectCloud(context.level(), spot.x, spot.y, spot.z);
      cloud.setOwner(context.caster());
      cloud.getPersistentData().putBoolean(com.arcane.execution.SpellImmunity.CLOUD_TAG, true);
      cloud.setRadius(2.0f);
      cloud.setDuration(100);
      cloud.setWaitTime(0);
      cloud.setFixedColor(effect.getColor());
      cloud.addEffect(new MobEffectInstance(effect, Math.min(ticks, 200), level));
      context.level().addFreshEntity(cloud);
    }
  }
}
