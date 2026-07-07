package com.arcane.execution;

public record DeliveryResult(TargetList targets, boolean continuePipeline) {
    public static DeliveryResult of(TargetList targets) {
        return new DeliveryResult(targets, true);
    }

    public static DeliveryResult stop(TargetList targets) {
        return new DeliveryResult(targets, false);
    }
}
