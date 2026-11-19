package com.arcane.inscribing;

import com.arcane.config.ArcaneConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Tintas. As comuns (comum, sangue, invisivel) se desgastam com os dias do jogo. A tinta permanente pura nunca se
 * desgasta e NAO sai com po solvente. Misturada a uma comum (ids terminados em "_perm"), nunca se desgasta mas AINDA
 * sai com o po solvente.
 */
public final class InkRegistry {
  public static final long DAY_TICKS = 24000L;
  public static final String PERMANENT = "permanent";
  private static final String MIXED_SUFFIX = "_perm";

  private static final Map<String, Double> MANA_MULTIPLIERS = Map.of(
      "common", 1.0,
      "blood", 0.85,
      "invisible", 1.1,
      "prismatic", 0.75,
      "core", 0.5,
      "permanent", 1.0
  );

  private InkRegistry() {}

  /** A tinta comum por tras de uma mistura ("blood_perm" vira "blood"); as demais voltam iguais. */
  public static String base(ResourceLocation inkId) {
    String path = inkId.getPath();
    return path.endsWith(MIXED_SUFFIX) ? path.substring(0, path.length() - MIXED_SUFFIX.length()) : path;
  }

  public static double getManaMultiplier(ResourceLocation inkId) {
    return MANA_MULTIPLIERS.getOrDefault(base(inkId), 1.0);
  }

  public static boolean isBloodInk(ResourceLocation inkId) {
    return "blood".equals(base(inkId));
  }

  public static boolean isInvisible(ResourceLocation inkId) {
    return "invisible".equals(base(inkId));
  }

  public static boolean isMixed(ResourceLocation inkId) {
    return inkId.getPath().endsWith(MIXED_SUFFIX);
  }

  /** So a permanente pura nao sai com po solvente. */
  public static boolean isErasable(ResourceLocation inkId) {
    return !PERMANENT.equals(inkId.getPath());
  }

  /** Dias do jogo que a tinta dura; 0 = nunca se desgasta. */
  public static int lifetimeDays(ResourceLocation inkId) {
    if (PERMANENT.equals(inkId.getPath()) || isMixed(inkId)) return 0;
    return switch (inkId.getPath()) {
      case "common" -> ArcaneConfig.INK_DAYS_COMMON.get();
      case "blood" -> ArcaneConfig.INK_DAYS_BLOOD.get();
      case "invisible" -> ArcaneConfig.INK_DAYS_INVISIBLE.get();
      default -> 0;
    };
  }

  /** O desenho feito em {@code created} com esta tinta ja se desgastou em {@code now}? */
  public static boolean expired(ResourceLocation inkId, long created, long now) {
    int days = lifetimeDays(inkId);
    return days > 0 && now - created >= days * DAY_TICKS;
  }
}
