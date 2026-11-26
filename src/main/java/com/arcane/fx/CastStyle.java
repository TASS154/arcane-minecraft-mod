package com.arcane.fx;

import com.arcane.execution.SpellElement;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * Maestria e assinatura (Fase M), SO aparencia: nada aqui muda dano, custo, alcance ou qualquer regra.
 *
 * <ul>
 *   <li><b>Maestria</b> (0 a 1): media dos estados de conhecimento dos glifos da inscricao (Desconhecido 0 ... Dominado
 *       1), pesada pelo custo de cada glifo. Baixa: tremida, cor um pouco lavada, rastro com falhas, voz um pouco
 *       desafinada. Alta: nitida, saturada, uma camada de acabamento e um acorde limpo no fim da voz.</li>
 *   <li><b>Assinatura</b>: derivada do estilo do jogador (os dois elementos que ele mais conjura, contados so para isto;
 *       ver docs/ESTILO_UNICO_DESENHO.md) e do UUID: um tom de destaque com o matiz levemente deslocado e um motivo
 *       (anel, espiral ou pontos). No maximo ~20% das particulas; o elemento continua dominante. O modificador Cor
 *       desliga a assinatura daquela magia. Paleta de daltonismo propria. Desligavel (a sua e a dos outros).</li>
 * </ul>
 */
public final class CastStyle {
  private CastStyle() {}

  public static final int MOTIF_RING = 0, MOTIF_SPIRAL = 1, MOTIF_DOTS = 2;
  /** Fracao maxima das particulas da conjuracao que a assinatura pode ter. */
  public static final double SIGNATURE_SHARE = 0.20;
  /** Deslocamento maximo de matiz do destaque (fracao do circulo de cor). */
  public static final double MAX_HUE_SHIFT = 0.08;

  // ------------------------------------------------------------------ maestria
  /**
   * Maestria da inscricao: media dos niveis (0..4, dividido por 4) pesada pelo custo de cada glifo (minimo 1). Glifo
   * fora do mapa conta como Desconhecido. Sem glifos: 1 (nada a errar).
   */
  public static double mastery(List<String> glyphIds, Map<String, Integer> levels, ToIntFunction<String> weightOf) {
    if (glyphIds.isEmpty()) return 1.0;
    double sum = 0, weight = 0;
    for (String id : glyphIds) {
      int w = Math.max(1, weightOf.applyAsInt(id));
      int level = Math.max(0, Math.min(4, levels.getOrDefault(id, 0)));
      sum += w * (level / 4.0);
      weight += w;
    }
    return weight <= 0 ? 1.0 : sum / weight;
  }

  /** Tremida das particulas, em blocos (0,18 com maestria 0; nenhuma com maestria 1, como era antes). */
  public static double jitter(double mastery) { return 0.18 * (1 - clamp01(mastery)); }

  /** Fracao das particulas do rastro que falha (buracos): ate 35% com maestria 0, nada a partir de 0,8. */
  public static double gapFraction(double mastery) { return Math.max(0, 0.35 * (0.8 - clamp01(mastery)) / 0.8); }

  /** Saturacao relativa da cor: 0,7 com maestria 0, 1 com maestria 1. */
  public static double saturation(double mastery) { return 0.7 + 0.3 * clamp01(mastery); }

  /** Desafinacao da voz (fracao do tom): ate 3% com maestria 0. */
  public static double detune(double mastery) { return 0.03 * (1 - clamp01(mastery)); }

  /** Maestria alta ganha a camada de acabamento (visual) e o acorde limpo (voz). */
  public static boolean polished(double mastery) { return mastery >= 0.85; }

  /** A cor com a saturacao da maestria (mistura com o cinza de mesma luminancia). */
  public static int desaturate(int rgb, double saturation) {
    int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
    double l = 0.299 * r + 0.587 * g + 0.114 * b;
    double s = clamp01(saturation);
    int nr = (int) Math.round(l + (r - l) * s), ng = (int) Math.round(l + (g - l) * s), nb = (int) Math.round(l + (b - l) * s);
    return (clamp255(nr) << 16) | (clamp255(ng) << 8) | clamp255(nb);
  }

  // ------------------------------------------------------------------ assinatura
  /**
   * Semente da assinatura, num int para o pacote de efeito: bit 30 = presente, motivo (2 bits), elemento do estilo
   * (8 bits) e deslocamento de matiz (-127..127 = -8%..+8%). O cliente tira a cor com a paleta dele (daltonismo).
   * 0 = sem assinatura (Cor na magia, conjurador sem estilo).
   */
  public static int seed(UUID player, SpellElement first, SpellElement second) {
    long h = player.getMostSignificantBits() * 31 + player.getLeastSignificantBits();
    h = h * 1_000_003L + (second == null ? 7 : second.ordinal() + 11);
    int motif = (int) Math.floorMod(h, 3L);
    int shift = (int) Math.floorMod(h >> 8, 255L) - 127;
    return (1 << 30) | (motif << 24) | ((first.ordinal() & 0xFF) << 16) | ((shift + 127) & 0xFF);
  }

  public static boolean present(int seed) { return (seed & (1 << 30)) != 0; }

  public static int motif(int seed) { return (seed >> 24) & 3; }

  public static SpellElement element(int seed) { return SpellElement.byOrdinal((seed >> 16) & 0xFF); }

  public static double hueShift(int seed) { return ((seed & 0xFF) - 127) / 127.0 * MAX_HUE_SHIFT; }

  /** Cor de destaque da assinatura: a do elemento (ou da paleta de daltonismo) com o matiz deslocado. */
  public static int accent(int seed, boolean colorblind) {
    SpellElement e = element(seed);
    int base = colorblind ? colorblindBase(e) : e.color();
    return shiftHue(base, hueShift(seed));
  }

  /** Paleta de daltonismo (azul, laranja, amarelo, branco azulado), escolhida pelo elemento. */
  static int colorblindBase(SpellElement e) {
    int[] pal = {0x3D7BFF, 0xFF8A1E, 0xFFD43B, 0xCFE3FF};
    return pal[e.ordinal() % pal.length];
  }

  public static int shiftHue(int rgb, double shift) {
    float[] hsb = java.awt.Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
    float hue = (float) ((hsb[0] + shift) % 1.0 + 1.0) % 1.0f;
    return java.awt.Color.HSBtoRGB(hue, Math.max(0.35f, hsb[1]), Math.max(0.55f, hsb[2])) & 0xFFFFFF;
  }

  /** Quantas particulas da assinatura cabem numa conjuracao de {@code total} particulas (nunca mais de 20%). */
  public static int signatureCount(int total) { return (int) Math.floor(Math.max(0, total) * SIGNATURE_SHARE); }

  private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }

  private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }
}
