package com.arcane.execution;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Marcas arcanas: guardadas na propria entidade (some ao expirar), lidas pelos filtros Marcados e Nao Marcados. */
public final class Marks {
  private static final String KEY = "arcane_marked_until";

  private Marks() {}

  public static void mark(Entity entity, long untilGameTime, int ticks) {
    entity.getPersistentData().putLong(KEY, untilGameTime);
    if (entity instanceof LivingEntity living) {
      living.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
    }
  }

  public static boolean isMarked(Entity entity) {
    return entity.getPersistentData().getLong(KEY) > entity.level().getGameTime();
  }
}
