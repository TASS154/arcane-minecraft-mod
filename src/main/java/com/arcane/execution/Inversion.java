package com.arcane.execution;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * Glifo Inverter: troca o efeito anterior pelo seu oposto. Os pares sao simetricos e cada lado compartilha os
 * nomes de parametro que importam (amount, strength, duration), entao a magnitude escrita vale para o oposto.
 */
public final class Inversion {
  public static final String KEY = "invert";

  private static final Map<String, String> PAIRS = Map.ofEntries(
      Map.entry("damage", "heal"), Map.entry("heal", "damage"),
      Map.entry("push", "attract"), Map.entry("attract", "push"),
      Map.entry("slow", "speed"), Map.entry("speed", "slow"),
      Map.entry("fire", "freeze"), Map.entry("freeze", "fire"),
      Map.entry("levitate", "gravity"), Map.entry("gravity", "levitate"),
      Map.entry("lifesteal", "life_transfer"), Map.entry("life_transfer", "lifesteal"),
      Map.entry("blind", "invisible"), Map.entry("invisible", "blind"),
      Map.entry("fear", "taunt"), Map.entry("taunt", "fear"));

  private Inversion() {}

  public static boolean inverted(Map<String, Double> parameters) {
    return parameters.getOrDefault(KEY, 0.0) > 0.0;
  }

  public static boolean hasOpposite(String path) { return PAIRS.containsKey(path); }

  public static Optional<String> opposite(String path) { return Optional.ofNullable(PAIRS.get(path)); }

  /** O efeito de verdade que roda: o oposto, quando invertido e houver par; senao o proprio. */
  public static EffectCall resolve(EffectCall call) {
    if (!inverted(call.parameters())) return call;
    String other = PAIRS.get(call.glyphId().getPath());
    if (other == null) return call;
    ResourceLocation id = new ResourceLocation(call.glyphId().getNamespace(), other);
    return new EffectCall(id, id, call.parameters(), call.magnitudeKeys());
  }
}
