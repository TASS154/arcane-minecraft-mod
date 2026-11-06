package com.arcane.inscription;

/**
 * Limite de tamanho e custo exponencial das magias (design: "ate 10 glifos, consumo de mana cresce
 * exponencialmente"). Valores provisorios vindos da config do servidor (ArcaneConfig); os padroes abaixo
 * valem em testes e ate a config carregar. O cliente recebe os valores do servidor (ArcaneSync).
 *
 * multiplicador(n) = crescimento ^ max(0, n - gratis)
 */
public final class SpellLimits {
  public static final int DEFAULT_MAX_GLYPHS = 10;
  public static final double DEFAULT_GROWTH = 1.3;
  public static final int DEFAULT_FREE_GLYPHS = 3;

  private static volatile int maxGlyphs = DEFAULT_MAX_GLYPHS;
  private static volatile double growth = DEFAULT_GROWTH;
  private static volatile int freeGlyphs = DEFAULT_FREE_GLYPHS;

  private SpellLimits() {}

  public static void set(int max, double growthPerGlyph, int free) {
    maxGlyphs = Math.max(1, max);
    growth = Math.max(1.0, growthPerGlyph);
    freeGlyphs = Math.max(0, free);
  }

  public static void reset() {
    set(DEFAULT_MAX_GLYPHS, DEFAULT_GROWTH, DEFAULT_FREE_GLYPHS);
  }

  public static int maxGlyphs() { return maxGlyphs; }
  public static double growth() { return growth; }
  public static int freeGlyphs() { return freeGlyphs; }

  public static double multiplier(int glyphCount) {
    return multiplier(glyphCount, growth, freeGlyphs);
  }

  public static double multiplier(int glyphCount, double growthPerGlyph, int free) {
    return Math.pow(growthPerGlyph, Math.max(0, glyphCount - free));
  }
}
