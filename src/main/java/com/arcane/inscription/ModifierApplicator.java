package com.arcane.inscription;

import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.glyph.ModifierScope;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Aplica modificadores sobre parametros. So muda parametros que o alvo realmente possui. */
public final class ModifierApplicator {
    private ModifierApplicator() {}

    public static Map<String, Double> mutate(Map<String, Double> params, ModifierScope scope, GlyphInstance modifier) {
        Map<String, Double> result = new HashMap<>(params);
        for (Map.Entry<String, ModifierScope.ModifierMutation> entry : scope.mutate().entrySet()) {
            String key = entry.getKey();
            if (!result.containsKey(key)) continue;
            ModifierScope.ModifierMutation mutation = entry.getValue();
            double current = result.get(key);
            double value = mutation.param().map(p -> modifier.param(p, mutation.value())).orElse(mutation.value());
            result.put(key, switch (mutation.op()) {
                case "multiply" -> current * value;
                case "add" -> current + value;
                case "set" -> value;
                default -> current;
            });
        }
        return result;
    }

    public static CompiledExecutionPlan.EffectCallCompiled mutateEffect(CompiledExecutionPlan.EffectCallCompiled effect,
                                                                         ModifierScope scope, GlyphInstance modifier) {
        return new CompiledExecutionPlan.EffectCallCompiled(effect.glyphId(), effect.handler(),
                Map.copyOf(mutate(effect.parameters(), scope, modifier)), effect.magnitudeKeys());
    }

    public static DeliverySpecCompiled applyDeliveryModifiers(DeliverySpecCompiled delivery,
                                                              java.util.List<GlyphInstance> modifiers) {
        Map<String, Double> params = new HashMap<>(delivery.parameters());
        for (GlyphInstance modifier : modifiers) {
            Optional<ModifierScope> scope = GlyphRegistry.get(modifier.glyphId()).flatMap(GlyphDefinition::modifierScope);
            if (scope.isPresent()) {
                params = new HashMap<>(mutate(params, scope.get(), modifier));
            }
        }
        double range = params.getOrDefault("range", delivery.range());
        return new DeliverySpecCompiled(delivery.formId(), delivery.handler(), Map.copyOf(params), range);
    }

    public record DeliverySpecCompiled(ResourceLocation formId, ResourceLocation handler,
                                       Map<String, Double> parameters, double range) {}
}
