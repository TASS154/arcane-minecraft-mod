package com.arcane.execution;

import com.arcane.inscription.Inscription;

import java.util.UUID;

/** Modificadores de rastro: Furtivo (menos residuo e menos alarde) e Falso Rastro (assinatura falsa e um residuo isca). */
public final class Traces {
  public static final int STEALTH_LIFETIME_DIVISOR = 4;

  private Traces() {}

  public static boolean has(Inscription inscription, String glyph) {
    return inscription.glyphs().stream().anyMatch(g -> g.glyphId().getPath().equals(glyph));
  }

  public static boolean stealthy(Inscription inscription) { return has(inscription, "stealth"); }

  public static boolean falsified(Inscription inscription) { return has(inscription, "false_trace"); }

  /** Uma assinatura que nao leva a ninguem (derivada do lugar e da hora, so para parecer outra pessoa). */
  public static UUID fakeSigner(long salt, int x, int z) {
    return UUID.nameUUIDFromBytes(("falso:" + salt + ":" + x + ":" + z).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }
}
