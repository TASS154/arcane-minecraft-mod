package com.arcane.execution;

/** Contas do Coro (logica pura, testavel sem o jogo). */
public final class ChorusMath {
  /** Ganho de forca por participante alem do primeiro. */
  public static final double PER_EXTRA = 0.35;
  /** Teto do multiplicador de forca. */
  public static final double CAP = 2.4;

  private ChorusMath() {}

  /** Multiplicador de forca com {@code n} participantes: 1 + 0,35 x (n - 1), no maximo 2,4. */
  public static double multiplier(int n) {
    return Math.min(CAP, 1.0 + PER_EXTRA * Math.max(0, n - 1));
  }

  /** Cota de mana de cada participante: o custo dividido entre eles, arredondado para cima, no minimo 1 (custo 0 nao cobra). */
  public static int share(int totalCost, int n) {
    if (totalCost <= 0) return 0;
    return Math.max(1, (int) Math.ceil(totalCost / (double) Math.max(1, n)));
  }
}
