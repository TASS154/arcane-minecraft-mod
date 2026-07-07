package com.arcane.inscription;

import com.arcane.glyph.GlyphCategory;
import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.glyph.ModifierScope;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModifierApplicator {
    private ModifierApplicator() {}

    public static void applyCompileTimeModifiers(List<CompiledExecutionPlan.EffectCallCompiled> effects,
                                                 List<GlyphInstance> modifiers) {
        for (GlyphInstance modifierInstance : modifiers) {
            Optional<GlyphDefinition> defOpt = GlyphRegistry.get(modifierInstance.glyphId());
            if (defOpt.isEmpty() || defOpt.get().modifierScope().isEmpty()) {
                continue;
            }
            ModifierScope scope = defOpt.get().modifierScope().get();
            if ("previous".equals(scope.target()) || "previous_effect".equals(scope.target())) {
                if (!effects.isEmpty()) {
                    CompiledExecutionPlan.EffectCallCompiled last = effects.get(effects.size() - 1);
                    effects.set(effects.size() - 1, mutateEffect(last, scope, modifierInstance));
                }
            }
        }
    }

    private static CompiledExecutionPlan.EffectCallCompiled mutateEffect(
            CompiledExecutionPlan.EffectCallCompiled effect,
            ModifierScope scope,
            GlyphInstance modifierInstance) {
        Map<String, Double> params = new HashMap<>(effect.parameters());
        for (Map.Entry<String, ModifierScope.ModifierMutation> entry : scope.mutate().entrySet()) {
            String key = entry.getKey();
            ModifierScope.ModifierMutation mutation = entry.getValue();
            double current = params.getOrDefault(key, 0.0);
            double modValue = mutation.param()
                    .map(p -> modifierInstance.param(p, mutation.value()))
                    .orElse(mutation.value());
            params.put(key, switch (mutation.op()) {
                case "multiply" -> current * modValue;
                case "add" -> current + modValue;
                case "set" -> modValue;
                default -> current;
            });
        }
        return new CompiledExecutionPlan.EffectCallCompiled(effect.glyphId(), effect.handler(), Map.copyOf(params),
                effect.magnitudeKeys());
    }

    public static DeliverySpecCompiled applyDeliveryModifiers(DeliverySpecCompiled delivery, List<GlyphInstance> modifiers) {
        Map<String, Double> params = new HashMap<>(delivery.parameters());
        for (GlyphInstance modifierInstance : modifiers) {
            GlyphRegistry.get(modifierInstance.glyphId()).flatMap(GlyphDefinition::modifierScope).ifPresent(scope -> {
                if ("delivery".equals(scope.target())) {
                    scope.mutate().forEach((key, mutation) -> {
                        double current = params.getOrDefault(key, delivery.range());
                        double modValue = mutation.param().map(p -> modifierInstance.param(p, mutation.value())).orElse(mutation.value());
                        params.put(key, "multiply".equals(mutation.op()) ? current * modValue : current + modValue);
                    });
                }
            });
        }
        return new DeliverySpecCompiled(delivery.formId(), delivery.handler(), Map.copyOf(params), delivery.range());
    }

    public record DeliverySpecCompiled(ResourceLocation formId, ResourceLocation handler,
                                       Map<String, Double> parameters, double range) {}
}
