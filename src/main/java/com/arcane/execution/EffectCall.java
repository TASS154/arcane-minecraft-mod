package com.arcane.execution;

import com.arcane.inscription.CompiledExecutionPlan;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record EffectCall(ResourceLocation glyphId, ResourceLocation handler, Map<String, Double> parameters,
                         java.util.List<String> magnitudeKeys) {
    public static EffectCall fromCompiled(CompiledExecutionPlan.EffectCallCompiled compiled) {
        return new EffectCall(compiled.glyphId(), compiled.handler(), compiled.parameters(), compiled.magnitudeKeys());
    }
}
