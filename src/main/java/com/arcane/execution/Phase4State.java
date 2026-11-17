package com.arcane.execution;

import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Estados temporarios da Fase 4 guardados na propria entidade (chave = hora de jogo em que acabam). Sem custo por tick:
 * so os eventos de {@link Phase4Service} os consultam.
 */
public final class Phase4State {
  public static final String CURSE = "arcane_curse_until", SILENCE = "arcane_silence_until", REFLECT = "arcane_reflect_until",
      PETRIFY = "arcane_petrify_until", CHARGED = "arcane_charged_until", LINK = "arcane_link_until", LINK_OF = "arcane_link_of",
      RECALL = "arcane_recall", OWNER = "arcane_minion_owner", MINION_UNTIL = "arcane_minion_until";

  /** Estados nocivos que Dissipar em si mesmo remove. */
  public static final List<String> NEGATIVE = List.of(CURSE, SILENCE, PETRIFY, MindStates.FEAR_KEY, MindStates.SLEEP_KEY);
  /** Estados de magia que Dissipar remove de outras criaturas. */
  public static final List<String> MAGICAL = List.of(REFLECT, CHARGED, LINK, Wetness.KEY, Marks.KEY);

  private Phase4State() {}

  public static void set(Entity e, String key, int ticks) {
    long until = e.level().getGameTime() + Math.max(1, ticks);
    if (e.getPersistentData().getLong(key) < until) e.getPersistentData().putLong(key, until);
  }

  public static boolean active(Entity e, String key) {
    return e.getPersistentData().getLong(key) > e.level().getGameTime();
  }

  public static void clear(Entity e, String key) {
    e.getPersistentData().remove(key);
  }
}
