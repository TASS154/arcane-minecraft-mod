package com.arcane.execution;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Perfurar: a fracao {@code pierce} do dano vira dano perfurante (tipo arcane:pierce), que ignora armadura, encantamentos
 * de protecao, o efeito Resistencia, a reducao do Petrificar e a dureza dos chefes (Hidra). Cada camada de Perfurar
 * soma 20%; 5 camadas = 100%. O resto do dano continua sendo dano magico comum.
 */
public final class Pierce {
  public static final ResourceKey<DamageType> TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("arcane", "pierce"));

  private Pierce() {}

  public static boolean is(DamageSource source) { return source.is(TYPE); }

  public static DamageSource source(ExecutionContext context) {
    var holder = context.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TYPE);
    return new DamageSource(holder, context.caster(), context.caster());
  }

  /** Fere em duas partes no mesmo instante: a comum e a perfurante (sem que o tempo de invencibilidade engula a segunda). */
  public static void hurt(ExecutionContext context, Entity entity, DamageSource normal, float amount, double pierce) {
    float piercing = (float) (amount * pierce), rest = amount - piercing;
    if (rest > 0.01f) entity.hurt(normal, rest);
    if (piercing > 0.01f) {
      if (entity instanceof LivingEntity living) living.invulnerableTime = 0;
      entity.hurt(source(context), piercing);
      context.level().sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5,
          entity.getZ(), 6, 0.2, 0.3, 0.2, 0.3);
    }
  }
}
