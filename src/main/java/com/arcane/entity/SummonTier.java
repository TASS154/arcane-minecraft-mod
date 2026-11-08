package com.arcane.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Tamanho/nivel das criaturas invocadas (1 a 3 = tier 0 a 2): cada nivel tem um modelo diferente, escala maior e
 * atributos maiores. Os modificadores sao gravados com a criatura, entao sobrevivem a salvar e carregar.
 */
public final class SummonTier {
  public static final int MAX = 2;
  private static final UUID HEALTH = UUID.fromString("5b1f7e22-4c0a-4f55-9d8e-3a6f2b9c1d01");
  private static final UUID DAMAGE = UUID.fromString("5b1f7e22-4c0a-4f55-9d8e-3a6f2b9c1d02");
  private static final UUID SPEED = UUID.fromString("5b1f7e22-4c0a-4f55-9d8e-3a6f2b9c1d03");

  private SummonTier() {}

  public static int clamp(int tier) { return Math.max(0, Math.min(MAX, tier)); }

  /** Escala visual e da caixa de colisao. */
  public static float wolfScale(int tier) { return switch (clamp(tier)) { case 0 -> 1.0f; case 1 -> 1.55f; default -> 2.3f; }; }

  public static float golemScale(int tier) { return switch (clamp(tier)) { case 0 -> 1.0f; case 1 -> 1.4f; default -> 1.9f; }; }

  /** Aplica vida, dano e (um pouco de) velocidade proporcionais ao nivel. */
  public static void apply(LivingEntity entity, int tier, float scale) {
    set(entity.getAttribute(Attributes.MAX_HEALTH), HEALTH, "arcane_tier_health", scale * scale - 1.0);
    set(entity.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE, "arcane_tier_damage", scale * 0.9 - 0.9);
    set(entity.getAttribute(Attributes.MOVEMENT_SPEED), SPEED, "arcane_tier_speed", tier > 0 ? 0.08 * tier : 0.0);
    entity.setHealth(entity.getMaxHealth());
  }

  private static void set(AttributeInstance attr, UUID id, String name, double amount) {
    if (attr == null) return;
    attr.removeModifier(id);
    if (Math.abs(amount) > 1e-6) attr.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_BASE));
  }
}
