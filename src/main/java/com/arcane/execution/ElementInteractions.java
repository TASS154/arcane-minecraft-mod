package com.arcane.execution;

import com.arcane.config.ArcaneConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.tags.BiomeTags;

/**
 * Interacoes entre elementos e ambiente: o jogador vence por entender o terreno, nao por numeros maiores.
 * Raio rende mais na chuva e em alvos molhados; fogo perde forca na chuva; gelo derrete em biomas quentes e
 * apaga quem esta pegando fogo; veneno rende mais no pantano. Tudo desligavel pela config.
 */
public final class ElementInteractions {
  private ElementInteractions() {}

  public static boolean enabled() {
    return ArcaneConfig.ELEMENT_INTERACTIONS.get();
  }

  /** Multiplicador (1.0 = sem efeito) do dano de um elemento contra um alvo. */
  public static double damageMultiplier(ServerLevel level, SpellElement element, Entity target) {
    if (!enabled()) return 1.0;
    BlockPos pos = target.blockPosition();
    boolean wet = target.isInWaterOrRain();
    double strength = ArcaneConfig.ELEMENT_INTERACTION_STRENGTH.get();
    double m = 1.0;
    switch (element) {
      case STORM -> {
        if (level.isRainingAt(pos.above())) m += 0.5 * strength;
        if (wet) m += 0.3 * strength;
      }
      case FIRE -> {
        if (wet) m -= 0.4 * strength;
        if (target instanceof LivingEntity living && living.getTicksFrozen() > 0) m += 0.25 * strength;
      }
      case FROST -> {
        var biome = level.getBiome(pos);
        if (biome.is(BiomeTags.IS_NETHER) || biome.value().getBaseTemperature() >= 1.5f) m -= 0.3 * strength;
        if (target.isOnFire()) m += 0.0; // o bonus de apagar o fogo vem em afterHit
      }
      case VENOM -> {
        if (level.getBiome(pos).is(BiomeTags.IS_FOREST) || level.getBiome(pos).is(net.minecraft.world.level.biome.Biomes.SWAMP)
            || level.getBiome(pos).is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP)) m += 0.3 * strength;
      }
      default -> { }
    }
    return Math.max(0.1, m);
  }

  /** Efeitos colaterais depois de acertar: gelo apaga o fogo do alvo. */
  public static void afterHit(ServerLevel level, SpellElement element, Entity target) {
    if (!enabled()) return;
    if (element == SpellElement.FROST && target.isOnFire()) {
      target.clearFire();
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0, target.getZ(), 6, 0.2, 0.3, 0.2, 0.02);
    }
  }

  /** Fogo nao pega com chuva caindo sobre o bloco. */
  public static boolean canIgniteAt(ServerLevel level, BlockPos pos) {
    return !enabled() || !level.isRainingAt(pos);
  }
}
