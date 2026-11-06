package com.arcane.execution;

import com.arcane.inscription.Inscription;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Juramentos: glifos que trocam uma restricao por poder. O bonus de forca vem do compilador (modificador "all");
 * aqui fica a parte que so o servidor decide: a restricao ser cumprida e o preco ser pago.
 */
public final class Vows {
  public static final ResourceLocation STILLNESS = new ResourceLocation("arcane", "vow_stillness");
  public static final ResourceLocation BLOOD = new ResourceLocation("arcane", "vow_blood");
  public static final ResourceLocation DAYLIGHT = new ResourceLocation("arcane", "vow_daylight");
  /** Vida minima (alem do preco) para o Juramento de Sangue. */
  public static final float BLOOD_COST = 4.0f;

  private Vows() {}

  private static boolean has(Inscription inscription, ResourceLocation id) {
    return inscription.glyphs().stream().anyMatch(g -> id.equals(g.glyphId()));
  }

  /** Chave de mensagem da restricao quebrada, ou null se todos os juramentos da magia foram cumpridos. */
  public static String violation(LivingEntity caster, Inscription inscription) {
    if (has(inscription, STILLNESS)) {
      double dx = caster.getX() - caster.xo, dz = caster.getZ() - caster.zo;
      if (!caster.onGround() || dx * dx + dz * dz >= 1.0e-4) return "message.arcane.vow.stillness";
    }
    if (has(inscription, DAYLIGHT)) {
      if (!caster.level().isDay() || !caster.level().canSeeSky(caster.blockPosition())) return "message.arcane.vow.daylight";
    }
    if (has(inscription, BLOOD) && caster.getHealth() <= BLOOD_COST + 2.0f) return "message.arcane.vow.blood";
    return null;
  }

  /** Cobra o preco dos juramentos depois de a magia ter sido conjurada com sucesso. */
  public static void pay(LivingEntity caster, Inscription inscription) {
    if (has(inscription, BLOOD)) caster.hurt(caster.damageSources().magic(), BLOOD_COST);
  }
}
