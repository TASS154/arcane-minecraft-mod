package com.arcane.training;

import java.util.Locale;

/**
 * Dano mostrado ao jogador em CORACOES (1 coracao = 2 HP, meio coracao = 1 HP). Numeros exatos em HP ficam so no
 * {@code /arcane dummy report} e nos logs. Arredonda para o meio coracao mais proximo (golpes de 0,4 HP viram meio
 * coracao, nunca zero).
 */
public final class HeartText {
  public static final String FULL = "❤";   // ❤
  public static final String SMALL = "♥";  // ♥ (usado em ♥×N)
  /** Acima disto, o texto vira "♥×N" em vez de N simbolos. */
  public static final int MAX_SYMBOLS = 10;

  private HeartText() {}

  /** Meios coracoes de um dano em HP (arredonda ao meio mais proximo; qualquer dano positivo vale ao menos 1). */
  public static int halves(float hp) {
    if (!(hp > 0)) return 0;
    return Math.max(1, Math.round(hp));
  }

  /** Coracoes cheios e se sobra um meio. */
  public record Hearts(int full, boolean half) {
    public double value() { return full + (half ? 0.5 : 0.0); }
  }

  public static Hearts hearts(float hp) {
    int h = halves(hp);
    return new Hearts(h / 2, h % 2 == 1);
  }

  /** Numero de coracoes como texto ("3,5", "12", "0,5"), sempre com virgula. */
  public static String number(float hp) {
    Hearts hs = hearts(hp);
    return hs.half() ? String.format(Locale.ROOT, "%d,5", hs.full()) : Integer.toString(hs.full());
  }

  /**
   * Dano em simbolos: ate 10 coracoes, um ❤ por coracao cheio e "½" para o meio ("❤❤❤½"); acima, "♥×N". Sem dano: "-".
   * O renderizador colore os simbolos; este texto e tambem o fallback sem cor.
   */
  public static String symbols(float hp) {
    Hearts hs = hearts(hp);
    if (hs.full() == 0 && !hs.half()) return "-";
    if (hs.value() > MAX_SYMBOLS) return SMALL + "×" + number(hp);
    return FULL.repeat(hs.full()) + (hs.half() ? "½" : "");
  }

  /** Taxa em coracoes por segundo, com uma casa ("2,5 ❤/s"). */
  public static String rate(float hpPerSecond) {
    return String.format(Locale.ROOT, "%.1f", Math.max(0f, hpPerSecond) / 2f).replace('.', ',') + " " + FULL + "/s";
  }
}
