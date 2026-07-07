package com.arcane.execution;

public record PrecisionSnapshot(double powerMultiplier, double manaCostMultiplier, boolean fromWorkbench) {
    public static final PrecisionSnapshot WORKBENCH = new PrecisionSnapshot(1.0, 1.0, true);
    public static final PrecisionSnapshot FIELD = new PrecisionSnapshot(0.3, 1.75, false);
}
