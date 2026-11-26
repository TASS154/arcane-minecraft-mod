package com.arcane.execution;

import com.arcane.config.ArcaneConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Interacoes entre elementos e ambiente: o jogador vence por entender o terreno, nao por numeros maiores.
 * As regras vem de data/arcane/interactions/*.json ({@link InteractionRules}); aqui so se aplica a forca da config.
 * Tudo desligavel pela config.
 */
public final class ElementInteractions {
  private ElementInteractions() {}

  public static boolean enabled() {
    return ArcaneConfig.ELEMENT_INTERACTIONS.get();
  }

  /** Multiplicador (1.0 = sem efeito) do dano de um elemento contra um alvo. */
  public static double damageMultiplier(ServerLevel level, SpellElement element, Entity target) {
    if (!enabled()) return 1.0;
    double strength = ArcaneConfig.ELEMENT_INTERACTION_STRENGTH.get();
    double m = 1.0;
    for (InteractionRules.Rule rule : InteractionRules.matching(level, element, target)) m += rule.damageAdd() * strength;
    m = Math.max(0.1, m);
    com.arcane.affinity.AffinityContext.noteReaction(target, m); // teto da afinidade acumulada (Fase I)
    return m;
  }

  /** Efeitos colaterais depois de acertar (hoje: gelo apaga o fogo do alvo). */
  public static void afterHit(ServerLevel level, SpellElement element, Entity target) {
    if (!enabled()) return;
    for (InteractionRules.Rule rule : InteractionRules.matching(level, element, target)) {
      if (rule.after().equals("steam")) {
        // Fogo em alvo molhado: vira vapor, apaga o fogo e seca a criatura.
        target.clearFire();
        Wetness.dry(target);
        com.arcane.fx.Fx.reaction(level, null, target.position().add(0, 1.0, 0), com.arcane.fx.Fx.Reaction.STEAM);
        com.arcane.training.DamageMeterEvents.reaction(target, "steam");
      } else if (rule.after().equals("extinguish") && target.isOnFire()) {
        target.clearFire();
        com.arcane.fx.Fx.reaction(level, null, target.position().add(0, 1.0, 0), com.arcane.fx.Fx.Reaction.HISS);
        com.arcane.training.DamageMeterEvents.reaction(target, "hiss");
      } else if (rule.when().contains("frozen") && element == SpellElement.FIRE) {
        // Fogo em alvo congelado (a regra so soma dano): o gelo estilhaca. So visual.
        com.arcane.fx.Fx.reaction(level, null, target.position().add(0, 1.0, 0), com.arcane.fx.Fx.Reaction.SHATTER);
        com.arcane.training.DamageMeterEvents.reaction(target, "shatter");
      }
    }
  }

  /** Ticks de gelo extras que as regras dao a um alvo (ex.: molhado congela mais rapido). */
  public static int freezeBonus(ServerLevel level, SpellElement element, Entity target) {
    if (!enabled()) return 0;
    double strength = ArcaneConfig.ELEMENT_INTERACTION_STRENGTH.get();
    int bonus = 0;
    for (InteractionRules.Rule rule : InteractionRules.matching(level, element, target)) bonus += (int) Math.round(rule.freezeAdd() * strength);
    return bonus;
  }

  /** Fogo nao pega com chuva caindo sobre o bloco. */
  public static boolean canIgniteAt(ServerLevel level, BlockPos pos) {
    return !enabled() || !level.isRainingAt(pos);
  }
}
