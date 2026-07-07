package com.arcane.execution;

import com.arcane.api.effect.EffectHandler;
import com.arcane.api.filter.FilterResolver;
import com.arcane.api.form.FormHandler;
import com.arcane.glyph.GlyphHandlerRegistry;
import com.arcane.inscription.CompiledExecutionPlan;

public final class SpellPipeline {
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

        for (int i = 0; i < plan.segments().size(); i++) {
            CompiledExecutionPlan.ExecutionSegment segment = plan.segments().get(i);
            if (segment.delayTicks() > 0) {
                com.arcane.execution.scheduler.SpellTickScheduler.schedule(context, segment, segment.delayTicks());
                continue;
            }
            executeSegment(context, segment);
        }

        return ExecutionResult.ok(0);
    }

    public void executeSegment(ExecutionContext context, CompiledExecutionPlan.ExecutionSegment segment) {
        if (segment.condition().isPresent()) {
            CompiledExecutionPlan.ConditionGate gate = segment.condition().get();
            boolean pass = GlyphHandlerRegistry.getCondition(gate.handler())
                    .map(h -> h.test(context, new com.arcane.inscription.GlyphInstance(gate.conditionId(), gate.parameters())))
                    .orElse(true);
            if (!pass) {
                return;
            }
        }

        TargetList targets = context.resolvedTargets();
        for (CompiledExecutionPlan.FilterSpec filter : segment.filters()) {
            FilterResolver resolver = GlyphHandlerRegistry.getFilter(filter.handler()).orElse(null);
            if (resolver != null) {
                targets = resolver.filter(context, targets);
            }
        }

        for (CompiledExecutionPlan.EffectCallCompiled effectCompiled : segment.effects()) {
            EffectHandler handler = GlyphHandlerRegistry.getEffect(effectCompiled.handler()).orElse(null);
            if (handler != null) {
                handler.apply(context, EffectCall.fromCompiled(effectCompiled), targets);
            }
        }

        for (CompiledExecutionPlan.ChainRule rule : segment.chainRules()) {
            if ("activate".equals(rule.event())) {
                GlyphHandlerRegistry.getActivator(new net.minecraft.resources.ResourceLocation("arcane", "activate"))
                        .ifPresent(h -> h.activate(context, new com.arcane.inscription.GlyphInstance(
                                new net.minecraft.resources.ResourceLocation("arcane", "activate"), java.util.Map.of())));
            }
        }
    }
}
