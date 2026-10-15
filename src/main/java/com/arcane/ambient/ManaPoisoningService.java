package com.arcane.ambient;

import com.arcane.execution.EnvironmentalSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class ManaPoisoningService {
  private ManaPoisoningService() {}

  public static void tick(ServerPlayer player, EnvironmentalSnapshot environment) {
    if (environment.poisoningExposureRate() <= 0) return;
    float protection = getProtection(player);
    float exposure = (float) (environment.poisoningExposureRate() * (1.0 - protection));
    if (exposure <= 0) return;
    // Invoked on a 20-tick cadence; keep confusion ~every 40 ticks and damage every call.
    if ((player.tickCount / 20) % 2 == 0) {
      player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
    }
    if (exposure > 0.2f) {
      player.hurt(player.damageSources().magic(), exposure * 2f);
    }
  }

  private static float getProtection(ServerPlayer player) {
    float protection = 0f;
    if (player.getInventory().getArmor(3).is(com.arcane.registry.ModItems.ARCANE_MASK.get())) protection += 0.3f;
    if (player.getInventory().getArmor(2).is(com.arcane.registry.ModItems.INSULATING_CLOAK.get())) protection += 0.4f;
    return Math.min(0.9f, protection);
  }
}
