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

  /**
   * Custo de um efeito conforme o parametro principal sobe acima do padrao: cresce de forma exponencial, mas com uma
   * curva suave (x1,12 por unidade extra). Abaixo do padrao continua linear (minimo 0,5). Servidor e cliente usam esta.
   */
  public static double parameterFactor(double ratio) {
    if (ratio <= 1.0) return Math.max(0.5, ratio);
    double r = Math.min(12.0, ratio);
    return Math.min(40.0, r * Math.pow(1.12, r - 1.0));
  }

  public static double multiplier(int glyphCount) {
    return multiplier(glyphCount, growth, freeGlyphs);
  }

  public static double multiplier(int glyphCount, double growthPerGlyph, int free) {
    return Math.pow(growthPerGlyph, Math.max(0, glyphCount - free));
  }

  /**
   * Custo de uma forma persistente (Zona, Dominio): cada pulso reaplica os efeitos, entao o custo dos EFEITOS cresce com o
   * numero de pulsos (segundos / intervalo): x(1 + 0,5 por pulso alem do primeiro), limitado a x60. Quanto dano a magia
   * entrega ao longo do tempo entra no preco. Sem zona, x1.
   */
  public static double zoneMultiplier(double seconds, double intervalSeconds) {
    if (seconds <= 0.0) return 1.0;
    double pulses = seconds / Math.max(0.5, intervalSeconds);
    return Math.min(60.0, 1.0 + 0.5 * Math.max(0.0, pulses - 1.0));
  }

  /** Custo da AREA: o dobro do raio padrao custa x2,4; so sobe (reduzir a area nao barateia). Limitado a x12. */
  public static double areaCostFactor(double ratio) {
    return ratio <= 1.0 ? 1.0 : Math.min(12.0, Math.pow(ratio, 1.25));
  }

  /** Custo de uma invocacao: quantidade x tamanho^1,5 x duracao^0,7 (cada um relativo ao padrao do glifo). */
  public static double summonFactor(double countRatio, double size, double durationRatio) {
    return Math.max(0.25, countRatio) * Math.pow(Math.max(1.0, size), 1.5) * Math.pow(Math.max(0.5, Math.min(6.0, durationRatio)), 0.7);
  }
}
