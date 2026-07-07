package com.arcane.execution;

public record EnvironmentalSnapshot(
        double potencyMultiplier,
        double stabilityFactor,
        double manaRegenBonus,
        double poisoningExposureRate,
        java.util.List<String> sourceTags
) {
    public static final EnvironmentalSnapshot NEUTRAL = new EnvironmentalSnapshot(1.0, 1.0, 0.0, 0.0, java.util.List.of());

    public EnvironmentalSnapshot withPotency(double potency) {
        return new EnvironmentalSnapshot(potency, stabilityFactor, manaRegenBonus, poisoningExposureRate, sourceTags);
    }
}
