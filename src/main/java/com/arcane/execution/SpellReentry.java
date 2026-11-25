package com.arcane.execution;

import com.arcane.config.ArcaneConfig;
import net.minecraft.server.level.ServerLevel;

/**
 * Orcamento de reentrancia de magias (servidor). Cada trecho executado ({@link SpellPipeline#executeSegment}) entra aqui:
 * ha um limite de profundidade (magia disparada de dentro de outra: armadura, desenho em criatura, Ao Sofrer Dano, Coro,
 * zonas) e um limite de execucoes por tick no servidor inteiro. Passou, o trecho e descartado e conta em
 * {@link #dropped()} (um aviso no log por tick, no maximo).
 *
 * <p>Cada sistema ja se protege sozinho (desenho em criatura: {@code ItemDrawings.DEPTH}; armadura: 1 vez por segundo;
 * Ao Sofrer Dano: {@code ArmedSpells.FIRING}). Este orcamento e a rede de seguranca para lacos que cruzam sistemas
 * (armadura de um, desenho de outro, magia armada de um terceiro) e para picos de muitas magias no mesmo tick.
 */
public final class SpellReentry {
  private static int depth;
  private static long tick = Long.MIN_VALUE;
  private static int executedThisTick;
  private static long dropped;
  private static long warnedTick = Long.MIN_VALUE;

  private SpellReentry() {}

  /** Tenta entrar; false = descartar este trecho. Quem recebeu true chama {@link #exit()} num finally. */
  public static boolean enter(ServerLevel level) {
    long now = level.getServer().getTickCount();
    if (now != tick) {
      tick = now;
      executedThisTick = 0;
    }
    if (depth >= maxDepth() || executedThisTick >= maxPerTick()) {
      dropped++;
      if (warnedTick != now) {
        warnedTick = now;
        com.mojang.logging.LogUtils.getLogger().warn("Arcane: magia descartada pelo orcamento de reentrancia (profundidade {}, {} trechos neste tick)",
            depth, executedThisTick);
      }
      return false;
    }
    depth++;
    executedThisTick++;
    return true;
  }

  public static void exit() {
    if (depth > 0) depth--;
  }

  /** Trechos descartados desde o inicio (testes). */
  public static long dropped() { return dropped; }

  /** Profundidade atual (testes). */
  public static int depth() { return depth; }

  static int maxDepth() {
    try {
      return ArcaneConfig.SPELL_MAX_DEPTH.get();
    } catch (IllegalStateException e) {
      return 8;
    }
  }

  static int maxPerTick() {
    try {
      return ArcaneConfig.SPELL_MAX_EXECUTIONS_PER_TICK.get();
    } catch (IllegalStateException e) {
      return 512;
    }
  }
}
