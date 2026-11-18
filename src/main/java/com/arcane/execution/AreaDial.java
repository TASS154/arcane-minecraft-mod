package com.arcane.execution;

import com.arcane.inscription.CompiledExecutionPlan;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Regulador de area do conjurador (roda do mouse): cada passo multiplica a area por {@link #STEP} e divide o dano
 * (e os demais numeros de efeito) pelo mesmo fator: area maior = menos dano, area menor = mais dano, na mesma proporcao.
 * So vale para magias que tem area (formas de regiao ou efeitos com raio). Fica so no servidor e some ao sair.
 */
public final class AreaDial {
  public static final double STEP = 1.25;
  public static final int MIN = -4, MAX = 8;
  private static final Map<UUID, Integer> STEPS = new ConcurrentHashMap<>();

  private AreaDial() {}

  public static int step(UUID player) { return STEPS.getOrDefault(player, 0); }

  public static int add(UUID player, int delta) {
    int next = Math.max(MIN, Math.min(MAX, step(player) + delta));
    if (next == 0) STEPS.remove(player); else STEPS.put(player, next);
    return next;
  }

  public static void reset(UUID player) { STEPS.remove(player); }

  public static void clear() { STEPS.clear(); }

  public static double factorOf(int step) { return Math.pow(STEP, step); }

  /** A magia tem area para o regulador agir? */
  public static boolean appliesTo(CompiledExecutionPlan plan) {
    String form = plan.delivery().handler().getPath();
    if (form.equals("aura") || form.equals("wave") || form.equals("pillar") || form.equals("barrier")) return true;
    for (var segment : plan.segments()) {
      for (var effect : segment.effects()) if (effect.parameters().containsKey("radius")) return true;
    }
    return false;
  }

  /** Fator de area que vale para este jogador e esta magia (1 se nao tem area). */
  public static double factorFor(UUID player, CompiledExecutionPlan plan) {
    int step = step(player);
    return step == 0 || !appliesTo(plan) ? 1.0 : factorOf(step);
  }
}
