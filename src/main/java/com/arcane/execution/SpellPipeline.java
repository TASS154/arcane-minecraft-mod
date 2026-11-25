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

        Zones.begin(context);
        runSegments(context);
        return ExecutionResult.ok(0);
    }

    /** Roda os segmentos do plano; os que tem atraso vao para o agendador. */
    public void runSegments(ExecutionContext context) {
        for (ExecutionSegment segment : context.plan().segments()) {
            // "Quando a zona acabar": espera o fim da zona desta conjuracao (sem zona, nunca roda).
            if (Zones.isEndSegment(segment)) {
                Zones.attachEnd(context, segment);
                continue;
            }
            // "Ao quebrar a barreira": espera alguem quebrar um bloco da barreira desta conjuracao.
            if (BarrierBreaks.isBreakSegment(segment)) {
                if ("cast".equals(context.trigger().type())) BarrierBreaks.attach(context, segment);
                continue;
            }
            // Trechos com gatilho (Ao Sofrer Dano) ficam armados no conjurador em vez de rodar agora.
            if ("cast".equals(context.trigger().type()) && ArmedSpells.isTriggerSegment(segment)) {
                ArmedSpells.arm(context, segment);
                continue;
            }
            if (segment.delayTicks() > 0) {
                SpellTickScheduler.schedule(context, segment, segment.delayTicks());
                continue;
            }
            executeSegment(context, segment);
        }
    }

    public void executeSegment(ExecutionContext context, ExecutionSegment segment) {
        if (!SpellReentry.enter(context.level())) return; // orcamento de reentrancia (laços de realimentacao)
        try {
            executeSegmentInner(context, segment);
        } finally {
            SpellReentry.exit();
        }
    }

    private void executeSegmentInner(ExecutionContext context, ExecutionSegment segment) {
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
        // Alvos podem ter morrido ou sumido durante o atraso.
        TargetList current = new TargetList(
                targets.entities().stream().filter(e -> e.isAlive() && !e.isRemoved()).toList(), targets.blocks(), targets.points());
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
            // Inverter troca o efeito pelo oposto (Dano <-> Cura, Empurrao <-> Atrair...).
            EffectCall call = Inversion.resolve(EffectCall.fromCompiled(effectCompiled));
            // Aleatorio: a forca do efeito sorteia de 50% a 200% a cada conjuracao.
            if (call.parameters().getOrDefault("random", 0.0) > 0.0) call = randomize(context, call);
            EffectHandler handler = GlyphHandlerRegistry.getEffect(call.handler()).orElse(null);
            if (handler != null) {
                TargetList effective = targets;
                // O conjurador nao sofre os efeitos nocivos das proprias formas de area (so a forma Eu o atinge).
                if (EffectReach.harmful(call.glyphId().getPath()) && !"self".equals(context.plan().delivery().formId().getPath())
                        && targets.entities().contains(context.caster())) {
                    effective = new TargetList(targets.entities().stream().filter(e -> e != context.caster()).toList(), targets.blocks(), targets.points());
                }
                // Controle (Petrificar, Sono...) respeita o nivel de cada criatura contra a forca da magia.
                if (CreatureLevels.isControl(call.glyphId().getPath())) CreatureLevels.apply(context, call, effective, handler);
                else handler.apply(context, call, effective);
                // Sobrecarga: o excesso de forca cobra seu preco (nunca mata: so ate sobrar 1,5 coracao).
                if (call.parameters().getOrDefault("overcharge", 0.0) > 0.0 && context.caster().getHealth() > 3.0f) {
                    context.caster().hurt(context.level().damageSources().magic(), 2.0f);
                }
            }
        }
    }

    public static final java.util.Set<String> RANDOM_KEYS = java.util.Set.of("amount", "strength", "duration", "radius", "power", "level");

    public static EffectCall randomize(ExecutionContext context, EffectCall call) {
        double f = Math.pow(2.0, context.level().random.nextDouble() * 2.0 - 1.0); // 0,5x a 2x, com a mesma chance de subir ou cair
        java.util.Map<String, Double> params = new java.util.HashMap<>(call.parameters());
        params.replaceAll((k, v) -> RANDOM_KEYS.contains(k) ? (k.equals("level") ? Math.max(0, Math.round(v * f)) : v * f) : v);
        return new EffectCall(call.glyphId(), call.handler(), params, call.magnitudeKeys());
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
