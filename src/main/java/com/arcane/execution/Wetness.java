package com.arcane.execution;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Status "molhado" (glifo Molhar e Chuva): guardado na propria entidade com a hora em que seca. Alimenta a condicao
 * "wet" das regras de interacao (data/arcane/interactions/*.json): raio fere mais, gelo congela mais rapido e fogo apaga.
 */
public final class Wetness {
  private static final String KEY = "arcane_wet_until";
  public static final int MAX_TICKS = 1200;

  private Wetness() {}

  public static void soak(ServerLevel level, Entity entity, int ticks) {
    long until = level.getGameTime() + Math.max(1, Math.min(MAX_TICKS, ticks));
    if (entity.getPersistentData().getLong(KEY) < until) entity.getPersistentData().putLong(KEY, until);
    if (entity.isOnFire()) entity.clearFire();
    level.sendParticles(ParticleTypes.FALLING_WATER, entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ(), 10, 0.3, 0.1, 0.3, 0.0);
    level.sendParticles(ParticleTypes.SPLASH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
  }

  /** Molhado pelo glifo (a agua e a chuva de verdade ja contam na condicao das regras). */
  public static boolean isSoaked(Entity entity) {
    return entity.getPersistentData().getLong(KEY) > entity.level().getGameTime();
  }

  public static void dry(Entity entity) {
    entity.getPersistentData().remove(KEY);
  }
}
