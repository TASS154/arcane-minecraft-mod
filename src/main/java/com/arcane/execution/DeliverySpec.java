package com.arcane.execution;

import com.arcane.inscription.CompiledExecutionPlan;
import net.minecraft.resources.ResourceLocation;

public record DeliverySpec(ResourceLocation formId, ResourceLocation handler, java.util.Map<String, Double> parameters,
                           double range) {
    public static DeliverySpec fromPlan(CompiledExecutionPlan.DeliverySpec spec) {
        return new DeliverySpec(spec.formId(), spec.handler(), spec.parameters(), spec.range());
    }
}
