package com.arcane.fx;

import com.arcane.config.ArcaneConfig;

/** Qualidade e teto das particulas de elemento (config do servidor; sem config carregada, os padroes). */
public final class FxSettings {
  private FxSettings() {}

  public static double quality() {
    try {
      return ArcaneConfig.FX_QUALITY.get();
    } catch (IllegalStateException e) {
      return 1.0;
    }
  }

  public static int maxPerCast() {
    try {
      return ArcaneConfig.FX_MAX_PARTICLES_PER_CAST.get();
    } catch (IllegalStateException e) {
      return 400;
    }
  }
}
