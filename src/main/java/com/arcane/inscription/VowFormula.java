package com.arcane.inscription;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Formula do bonus dos juramentos (funcao pura, testada).
 *
 * <pre>
 *   bonus      = k * promessa^0.85           (promessa = 1 a 5, parametro "pledge" do glifo)
 *   multiplicador = 1 + bonus                (aplicado a dano, cura, empurrao e nivel do efeito)
 *   total (2 juramentos) = (1 + b1) * (1 + b2)
 * </pre>
 *
 * k depende do juramento (Quietude 0,28; Sangue 0,30; Luz 0,25). O expoente 0,85 faz cada degrau extra valer um pouco
 * menos que o anterior, mas a restricao fica cada vez mais dura (ver {@code Vows}). Uma magia aceita no maximo
 * {@link #MAX_VOWS} juramentos distintos, o que limita o teto a cerca de +300%.
 */
public final class VowFormula {
  public static final String PARAM = "pledge";
  public static final int MIN_PLEDGE = 1;
  public static final int MAX_PLEDGE = 5;
  public static final int DEFAULT_PLEDGE = 2;
  public static final int MAX_VOWS = 2;
  public static final double EXPONENT = 0.85;

  public static final ResourceLocation STILLNESS = new ResourceLocation("arcane", "vow_stillness");
  public static final ResourceLocation BLOOD = new ResourceLocation("arcane", "vow_blood");
  public static final ResourceLocation DAYLIGHT = new ResourceLocation("arcane", "vow_daylight");

  private static final Map<ResourceLocation, Double> K = Map.of(STILLNESS, 0.28, BLOOD, 0.30, DAYLIGHT, 0.25);

  private VowFormula() {}

  public static boolean isVow(ResourceLocation id) {
    return K.containsKey(id);
  }

  /** Promessa dentro de 1 a 5, arredondada (valores absurdos vindos do cliente nunca passam). */
  public static int pledge(GlyphInstance vow) {
    double raw = vow.param(PARAM, DEFAULT_PLEDGE);
    if (Double.isNaN(raw)) raw = DEFAULT_PLEDGE;
    return (int) Math.max(MIN_PLEDGE, Math.min(MAX_PLEDGE, Math.round(raw)));
  }

  /** Bonus fracionario (0,5 = +50%). */
  public static double bonus(ResourceLocation vowId, int pledge) {
    double k = K.getOrDefault(vowId, 0.0);
    int n = Math.max(MIN_PLEDGE, Math.min(MAX_PLEDGE, pledge));
    return k * Math.pow(n, EXPONENT);
  }

  public static double multiplier(GlyphInstance vow) {
    return 1.0 + bonus(vow.glyphId(), pledge(vow));
  }

  /** Porcentagem para mostrar ao jogador. */
  public static int percent(ResourceLocation vowId, int pledge) {
    return (int) Math.round(bonus(vowId, pledge) * 100.0);
  }
}
