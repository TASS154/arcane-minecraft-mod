package com.arcane.inscription;

import com.arcane.glyph.GlyphCategory;
import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InscriptionCompiler {
    private InscriptionCompiler() {}

    public static CompiledExecutionPlan compile(Inscription inscription) {
        ModifierApplicator.DeliverySpecCompiled deliveryCompiled = null;
        List<CompiledExecutionPlan.ExecutionSegment> segments = new ArrayList<>();
        List<CompiledExecutionPlan.FilterSpec> activeFilters = new ArrayList<>();
        List<CompiledExecutionPlan.EffectCallCompiled> pendingEffects = new ArrayList<>();
        List<GlyphInstance> pendingModifiers = new ArrayList<>();
        List<CompiledExecutionPlan.ChainRule> chainRules = new ArrayList<>();
        Optional<CompiledExecutionPlan.ConditionGate> pendingCondition = Optional.empty();
        int baseMana = 0;
        int delayTicks = 0;

        for (GlyphInstance instance : inscription.glyphs()) {
            GlyphDefinition def = GlyphRegistry.getOrThrow(instance.glyphId());
            baseMana += def.cost().mana();

            switch (def.category()) {
                case FORM -> {
                    flushSegment(segments, pendingCondition, activeFilters, pendingEffects, chainRules, delayTicks);
                    pendingModifiers.clear();
                    deliveryCompiled = new ModifierApplicator.DeliverySpecCompiled(
                            instance.glyphId(),
                            def.behavior().handler(),
                            new HashMap<>(instance.parameters()),
                            instance.param("range", 16.0)
                    );
                }
                case EFFECT -> {
                    pendingEffects.add(new CompiledExecutionPlan.EffectCallCompiled(
                            instance.glyphId(),
                            def.behavior().handler(),
                            new HashMap<>(instance.parameters()),
                            def.behavior().magnitudeKeys()
                    ));
                    ModifierApplicator.applyCompileTimeModifiers(pendingEffects, pendingModifiers);
                    pendingModifiers.clear();
                }
                case FILTER -> {
                    activeFilters.add(new CompiledExecutionPlan.FilterSpec(instance.glyphId(), def.behavior().handler()));
                }
                case MODIFIER -> pendingModifiers.add(instance);
                case CONDITION -> {
                    flushSegment(segments, pendingCondition, activeFilters, pendingEffects, chainRules, delayTicks);
                    pendingCondition = Optional.of(new CompiledExecutionPlan.ConditionGate(
                            instance.glyphId(), def.behavior().handler(), new HashMap<>(instance.parameters())));
                    def.modifierScope().ifPresent(scope -> {
                        if (scope.wrap() && scope.delayParam().isPresent()) {
                            // delay handled at segment level via param
                        }
                    });
                    delayTicks = (int) (instance.param("seconds", 0) * 20);
                }
                case ACTIVATOR -> chainRules.add(new CompiledExecutionPlan.ChainRule("activate", segments.size()));
                default -> {
                }
            }
        }

        flushSegment(segments, pendingCondition, activeFilters, pendingEffects, chainRules, delayTicks);

        if (deliveryCompiled == null) {
            deliveryCompiled = new ModifierApplicator.DeliverySpecCompiled(
                    new net.minecraft.resources.ResourceLocation("arcane", "self"),
                    new net.minecraft.resources.ResourceLocation("arcane", "self"),
                    Map.of(),
                    3.0
            );
        }

        deliveryCompiled = ModifierApplicator.applyDeliveryModifiers(deliveryCompiled, pendingModifiers);

        CompiledExecutionPlan.DeliverySpec delivery = new CompiledExecutionPlan.DeliverySpec(
                deliveryCompiled.formId(),
                deliveryCompiled.handler(),
                deliveryCompiled.parameters(),
                deliveryCompiled.range()
        );

        return new CompiledExecutionPlan(delivery, List.copyOf(segments), baseMana, inscription.fingerprint());
    }

    private static void flushSegment(List<CompiledExecutionPlan.ExecutionSegment> segments,
                                     Optional<CompiledExecutionPlan.ConditionGate> condition,
                                     List<CompiledExecutionPlan.FilterSpec> filters,
                                     List<CompiledExecutionPlan.EffectCallCompiled> effects,
                                     List<CompiledExecutionPlan.ChainRule> chainRules,
                                     int delayTicks) {
        if (effects.isEmpty() && condition.isEmpty()) {
            return;
        }
        segments.add(new CompiledExecutionPlan.ExecutionSegment(
                condition,
                List.copyOf(filters),
                List.copyOf(effects),
                List.copyOf(chainRules),
                delayTicks
        ));
        effects.clear();
        chainRules.clear();
    }
}
