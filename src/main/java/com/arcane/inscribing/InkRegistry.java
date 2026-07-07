package com.arcane.inscribing;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public final class InkRegistry {
  private static final Map<String, Double> MANA_MULTIPLIERS = Map.of(
      "common", 1.0,
      "blood", 0.85,
      "invisible", 1.1,
      "prismatic", 0.75,
      "core", 0.5
  );

  private InkRegistry() {}

  public static double getManaMultiplier(ResourceLocation inkId) {
    return MANA_MULTIPLIERS.getOrDefault(inkId.getPath(), 1.0);
  }

  public static boolean isBloodInk(ResourceLocation inkId) {
    return "blood".equals(inkId.getPath());
  }
}
