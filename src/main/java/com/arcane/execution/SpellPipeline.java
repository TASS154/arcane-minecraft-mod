package com.arcane.execution;

import com.arcane.api.effect.EffectHandler;
import com.arcane.api.filter.FilterResolver;
import com.arcane.api.form.FormHandler;
import com.arcane.execution.scheduler.SpellTickScheduler;
import com.arcane.glyph.ArcaneIds;
import com.arcane.glyph.GlyphHandlerRegistry;
import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.inscription.CompiledExecutionPlan.ChainRule;
import com.arcane.inscription.CompiledExecutionPlan.ConditionGate;
import com.arcane.inscription.CompiledExecutionPlan.EffectCallCompiled;
import com.arcane.inscription.CompiledExecutionPlan.ExecutionSegment;
import com.arcane.inscription.CompiledExecutionPlan.FilterSpec;
import com.arcane.inscription.GlyphInstance;

import java.util.Map;

public final class SpellPipeline {
    public static final SpellPipeline INSTANCE = new SpellPipeline();

    public ExecutionResult execute(ExecutionContext context) {
        CompiledExecutionPlan plan = context.plan();
        DeliverySpec deliverySpec = DeliverySpec.fromPlan(plan.delivery());

        FormHandler formHandler = GlyphHandlerRegistry.getForm(deliverySpec.handler())
                .orElse(null);
        if (formHandler == null) {
            return ExecutionResult.fail("missing_form_handler");
        }

        DeliveryResult delivery = formHandler.deliver(context, deliverySpec);
        context.setResolvedTargets(delivery.targets());

        if (!delivery.continuePipeline()) {
            return ExecutionResult.ok(0);
        }

        for (ExecutionSegment segment : plan.segments()) {
            if (segment.delayTicks() > 0) {
                SpellTickScheduler.schedule(context, segment, segment.delayTicks());
                continue;
            }
            executeSegment(context, segment);
        }

        return ExecutionResult.ok(0);
    }

    public void executeSegment(ExecutionContext context, ExecutionSegment segment) {
        if (!passesCondition(context, segment)) {
            return;
        }
        TargetList targets = applyFilters(context, segment, context.resolvedTargets());
        applyEffects(context, segment, targets);
        applyChainRules(context, segment);
    }

    private boolean passesCondition(ExecutionContext context, ExecutionSegment segment) {
        if (segment.condition().isEmpty()) {
            return true;
        }
        ConditionGate gate = segment.condition().get();
        GlyphInstance instance = new GlyphInstance(gate.conditionId(), gate.parameters());
        return GlyphHandlerRegistry.getCondition(gate.handler())
                .map(handler -> handler.test(context, instance))
                .orElse(true);
    }

    private TargetList applyFilters(ExecutionContext context, ExecutionSegment segment, TargetList targets) {
        TargetList current = targets;
        for (FilterSpec filter : segment.filters()) {
            FilterResolver resolver = GlyphHandlerRegistry.getFilter(filter.handler()).orElse(null);
            if (resolver != null) {
                current = resolver.filter(context, current);
            }
        }
        return current;
    }

    private void applyEffects(ExecutionContext context, ExecutionSegment segment, TargetList targets) {
        for (EffectCallCompiled effectCompiled : segment.effects()) {
            EffectHandler handler = GlyphHandlerRegistry.getEffect(effectCompiled.handler()).orElse(null);
            if (handler != null) {
                handler.apply(context, EffectCall.fromCompiled(effectCompiled), targets);
            }
        }
    }

    private void applyChainRules(ExecutionContext context, ExecutionSegment segment) {
        for (ChainRule rule : segment.chainRules()) {
            if (!"activate".equals(rule.event())) {
                continue;
            }
            GlyphInstance instance = new GlyphInstance(ArcaneIds.ACTIVATE, Map.of());
            GlyphHandlerRegistry.getActivator(ArcaneIds.ACTIVATE)
                    .ifPresent(handler -> handler.activate(context, instance));
        }
    }
}
