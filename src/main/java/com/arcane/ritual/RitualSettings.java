package com.arcane.ritual;

/**
 * Valores de balanceamento dos rituais (PROVISORIOS), vindos da config do servidor (ArcaneConfig).
 * Os padroes valem em testes e ate a config carregar.
 */
public final class RitualSettings {
  public static final double DEFAULT_MINOR = 1.5;
  public static final double DEFAULT_MEDIUM = 3.0;
  public static final double DEFAULT_MAJOR = 10.0;
  public static final double DEFAULT_MIN_COMPLETENESS = 0.85;
  public static final int COOLDOWN_TICKS = 100;
  public static final int TRIGGER_RANGE = 48;

  private static volatile double minor = DEFAULT_MINOR;
  private static volatile double medium = DEFAULT_MEDIUM;
  private static volatile double major = DEFAULT_MAJOR;
  private static volatile double minCompleteness = DEFAULT_MIN_COMPLETENESS;

  private RitualSettings() {}

  public static void set(double minorPower, double mediumPower, double majorPower, double completeness) {
    minor = Math.max(1.0, minorPower);
    medium = Math.max(1.0, mediumPower);
    major = Math.max(1.0, majorPower);
    minCompleteness = Math.max(0.3, Math.min(1.0, completeness));
  }

  public static void reset() {
    set(DEFAULT_MINOR, DEFAULT_MEDIUM, DEFAULT_MAJOR, DEFAULT_MIN_COMPLETENESS);
  }

  public static double minCompleteness() { return minCompleteness; }

  public static double power(RitualLevel level) {
    return switch (level) {
      case NONE -> 1.0;
      case MINOR -> minor;
      case MEDIUM -> medium;
      case MAJOR, LEGENDARY -> major;
    };
  }
}
