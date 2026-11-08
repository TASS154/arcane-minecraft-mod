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
    return Math.max(0.1, m);
  }

  /** Efeitos colaterais depois de acertar (hoje: gelo apaga o fogo do alvo). */
  public static void afterHit(ServerLevel level, SpellElement element, Entity target) {
    if (!enabled()) return;
    for (InteractionRules.Rule rule : InteractionRules.matching(level, element, target)) {
      if (rule.after().equals("extinguish") && target.isOnFire()) {
        target.clearFire();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0, target.getZ(), 6, 0.2, 0.3, 0.2, 0.02);
      }
    }
  }

  /** Fogo nao pega com chuva caindo sobre o bloco. */
  public static boolean canIgniteAt(ServerLevel level, BlockPos pos) {
    return !enabled() || !level.isRainingAt(pos);
  }
}
