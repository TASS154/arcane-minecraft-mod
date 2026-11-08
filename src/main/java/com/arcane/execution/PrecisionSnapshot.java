package com.arcane.execution;

public record PrecisionSnapshot(double powerMultiplier, double manaCostMultiplier, boolean fromWorkbench) {
    /** Potencia de uma magia escrita no campo (caneta em papel ou em bloco). */
    public static final double FIELD_POWER = 0.3;
    public static final PrecisionSnapshot WORKBENCH = new PrecisionSnapshot(1.0, 1.0, true);
    /** Custo de mana igual ao exibido: so a potencia cai. */
    public static final PrecisionSnapshot FIELD = new PrecisionSnapshot(FIELD_POWER, 1.0, false);

    /** A potencia vem de ONDE A MAGIA FOI ESCRITA (metadata.precision), nao de onde e lancada. */
    public static PrecisionSnapshot of(com.arcane.inscription.Inscription inscription) {
        double p = inscription.metadata().precision();
        if (p >= 1.0) return WORKBENCH;
        if (p == FIELD_POWER) return FIELD;
        return new PrecisionSnapshot(p, 1.0, false);
    }
}
