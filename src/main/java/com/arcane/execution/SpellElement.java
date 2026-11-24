package com.arcane.execution;

import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.registry.ModParticles;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * "Elemento visual" de uma magia: cor primaria e secundaria, particula e sons da conjuracao e do impacto. Vem do
 * primeiro efeito com elemento proprio; o mapa efeito -> elemento esta em data/arcane/elements/effects.json (todo
 * efeito precisa constar; o teste ElementMapTest confere). Nao muda a mecanica: as interacoes elementais de dano usam
 * {@link #mechanic}, o mapa antigo de 23 efeitos.
 *
 * <p>Os 8 elementos novos ainda reaproveitam as particulas dos antigos (com a cor propria no circulo, no projetil e
 * no impacto); particulas com movimento proprio, sons em sounds.json e decais de impacto ficam para depois.
 */
public enum SpellElement {
  ARCANE(() -> ModParticles.ARCANE_SPARK, 0xE6FAFF, 0x9FD8FF),
  FIRE(() -> ModParticles.ARCANE_EMBER, 0xFF8C28, 0xFFD24A),
  FROST(() -> ModParticles.ARCANE_FROST, 0x96D7FF, 0xFFFFFF),
  STORM(() -> ModParticles.ARCANE_BOLT, 0xFFEB5A, 0x7FC8FF),
  VENOM(() -> ModParticles.ARCANE_VENOM, 0x78EB46, 0x2E7D32),
  FORCE(() -> ModParticles.ARCANE_FORCE, 0xB478FF, 0xFFFFFF),
  VOID(() -> ModParticles.ARCANE_VOID, 0x9B5AE6, 0x1A0A2E),
  LIFE(() -> ModParticles.ARCANE_LIFE, 0xFF82BE, 0xFFF0F6),
  EARTH(() -> ModParticles.ARCANE_FORCE, 0xA0784A, 0x5C4632),
  WATER(() -> ModParticles.ARCANE_FROST, 0x3C8CFF, 0xB8E4FF),
  LIGHT(() -> ModParticles.ARCANE_SPARK, 0xFFF6C8, 0xFFD24A),
  SHADOW(() -> ModParticles.ARCANE_VOID, 0x3A2A4A, 0x8A5AC8),
  BLOOD(() -> ModParticles.ARCANE_EMBER, 0xB0102A, 0x4A0010),
  TIME(() -> ModParticles.ARCANE_SPARK, 0xE8C060, 0x60A0FF),
  NATURE(() -> ModParticles.ARCANE_LIFE, 0x5CC850, 0xC8E878),
  SOUND(() -> ModParticles.ARCANE_BOLT, 0x9EE8D8, 0xFFFFFF);

  private final Supplier<RegistryObject<SimpleParticleType>> particle;
  private final int color, secondary;

  SpellElement(Supplier<RegistryObject<SimpleParticleType>> particle, int color, int secondary) {
    this.particle = particle;
    this.color = color;
    this.secondary = secondary;
  }

  public SimpleParticleType particle() { return particle.get().get(); }

  /** Cor primaria (0xRRGGBB): projetil, circulo, impacto. */
  public int color() { return color; }

  /** Cor secundaria do proprio elemento (bordas), usada quando a magia nao tem um segundo elemento. */
  public int secondary() { return secondary; }

  public static SpellElement byOrdinal(int ordinal) {
    SpellElement[] all = values();
    return all[Math.floorMod(ordinal, all.length)];
  }

  // ------------------------------------------------------------------ mapa visual (dados)
  private static java.util.Map<String, SpellElement> map;

  /** Efeito -> elemento visual, lido de data/arcane/elements/effects.json (uma vez). */
  public static synchronized java.util.Map<String, SpellElement> map() {
    if (map != null) return map;
    java.util.Map<String, SpellElement> m = new java.util.HashMap<>();
    try (var in = SpellElement.class.getResourceAsStream("/data/arcane/elements/effects.json")) {
      if (in != null) {
        var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        json.getAsJsonObject("effects").entrySet().forEach(e -> m.put(e.getKey(), valueOf(e.getValue().getAsString().toUpperCase(java.util.Locale.ROOT))));
      }
    } catch (Exception e) {
      com.mojang.logging.LogUtils.getLogger().error("Nao foi possivel ler data/arcane/elements/effects.json", e);
    }
    map = java.util.Map.copyOf(m);
    return map;
  }

  /** Elemento visual de um efeito (sem entrada no mapa: arcano). */
  public static SpellElement ofGlyph(String glyphPath) {
    return map().getOrDefault(glyphPath, ARCANE);
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

  /**
   * Cor secundaria da magia: se ela tem um SEGUNDO elemento diferente do primeiro, a cor dele; senao, a secundaria
   * do proprio elemento (bordas do projetil e das runas).
   */
  public static int secondaryColor(CompiledExecutionPlan plan) {
    SpellElement first = null;
    for (CompiledExecutionPlan.ExecutionSegment segment : plan.segments()) {
      for (CompiledExecutionPlan.EffectCallCompiled effect : segment.effects()) {
        SpellElement e = ofGlyph(effect.glyphId().getPath());
        if (e == ARCANE) continue;
        if (first == null) first = e;
        else if (e != first) return e.color();
      }
    }
    return (first == null ? ARCANE : first).secondary();
  }

  // ------------------------------------------------------------------ mecanica (inalterada)
  /** Elemento das INTERACOES de dano (o mapa antigo, para nao mudar a mecanica). */
  public static SpellElement mechanicOfGlyph(String glyphPath) {
    return switch (glyphPath) {
      case "fire", "ignite", "explode", "blast" -> FIRE;
      case "slow", "freeze" -> FROST;
      case "thunder", "weather" -> STORM;
      case "poison", "wither" -> VENOM;
      case "gravity", "push", "attract", "levitate", "float", "telekinesis" -> FORCE;
      case "blink", "warp", "blind", "invisible" -> VOID;
      case "heal", "shield", "speed" -> LIFE;
      default -> ARCANE;
    };
  }

  public static SpellElement mechanic(CompiledExecutionPlan plan) {
    for (CompiledExecutionPlan.ExecutionSegment segment : plan.segments()) {
      for (CompiledExecutionPlan.EffectCallCompiled effect : segment.effects()) {
        SpellElement e = mechanicOfGlyph(effect.glyphId().getPath());
        if (e != ARCANE) return e;
      }
    }
    return ARCANE;
  }
}
