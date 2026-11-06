package com.arcane.execution;

import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.registry.ModParticles;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * "Elemento visual" de uma magia: define a cor do projetil e as particulas das formas, do impacto e da conjuracao.
 * Vem do primeiro efeito com identidade propria (fogo, gelo, raio...). Nao muda a mecanica, so a apresentacao
 * (design: "um projetil de corte chega diferente de um de fogo").
 */
public enum SpellElement {
  ARCANE(() -> ModParticles.ARCANE_SPARK, 0xE6FAFF),
  FIRE(() -> ModParticles.ARCANE_EMBER, 0xFF8C28),
  FROST(() -> ModParticles.ARCANE_FROST, 0x96D7FF),
  STORM(() -> ModParticles.ARCANE_BOLT, 0xFFEB5A),
  VENOM(() -> ModParticles.ARCANE_VENOM, 0x78EB46),
  FORCE(() -> ModParticles.ARCANE_FORCE, 0xB478FF),
  VOID(() -> ModParticles.ARCANE_VOID, 0x9B5AE6),
  LIFE(() -> ModParticles.ARCANE_LIFE, 0xFF82BE);

  private final Supplier<RegistryObject<SimpleParticleType>> particle;
  private final int color;

  SpellElement(Supplier<RegistryObject<SimpleParticleType>> particle, int color) {
    this.particle = particle;
    this.color = color;
  }

  public SimpleParticleType particle() { return particle.get().get(); }

  /** Cor RGB (0xRRGGBB) para tingir o projetil. */
  public int color() { return color; }

  public static SpellElement byOrdinal(int ordinal) {
    SpellElement[] all = values();
    return all[Math.floorMod(ordinal, all.length)];
  }

  public static SpellElement ofGlyph(String glyphPath) {
    return switch (glyphPath) {
      case "fire", "ignite", "explode" -> FIRE;
      case "slow", "freeze" -> FROST;
      case "thunder", "weather" -> STORM;
      case "poison", "wither" -> VENOM;
      case "gravity", "push", "attract", "levitate" -> FORCE;
      case "blink", "warp", "blind", "invisible" -> VOID;
      case "heal", "shield", "speed" -> LIFE;
      default -> ARCANE;
    };
  }

  /** O primeiro efeito, em ordem, que tem elemento proprio; sem nenhum, arcano. */
  public static SpellElement of(CompiledExecutionPlan plan) {
    for (CompiledExecutionPlan.ExecutionSegment segment : plan.segments()) {
      for (CompiledExecutionPlan.EffectCallCompiled effect : segment.effects()) {
        SpellElement e = ofGlyph(effect.glyphId().getPath());
        if (e != ARCANE) return e;
      }
    }
    return ARCANE;
  }
}
