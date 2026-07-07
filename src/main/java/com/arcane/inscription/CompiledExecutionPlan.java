package com.arcane.inscription;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record CompiledExecutionPlan(
        DeliverySpec delivery,
        List<ExecutionSegment> segments,
        int baseManaCost,
        String fingerprint
) {
    public record DeliverySpec(ResourceLocation formId, ResourceLocation handler, Map<String, Double> parameters, double range) {}

    public record ExecutionSegment(
            Optional<ConditionGate> condition,
            List<FilterSpec> filters,
            List<EffectCallCompiled> effects,
            List<ChainRule> chainRules,
            int delayTicks
    ) {}

    public record ConditionGate(ResourceLocation conditionId, ResourceLocation handler, Map<String, Double> parameters) {}

    public record FilterSpec(ResourceLocation filterId, ResourceLocation handler) {}

    public record EffectCallCompiled(ResourceLocation glyphId, ResourceLocation handler,
                                     Map<String, Double> parameters, List<String> magnitudeKeys) {}

    public record ChainRule(String event, int targetSegmentIndex) {}

    public List<EffectCallCompiled> allEffectsInOrder() {
        List<EffectCallCompiled> ordered = new ArrayList<>();
        for (ExecutionSegment segment : segments) {
            ordered.addAll(segment.effects());
        }
        return ordered;
    }
}
