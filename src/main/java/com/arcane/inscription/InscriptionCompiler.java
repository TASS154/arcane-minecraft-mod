package com.arcane.inscription;

import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.glyph.ModifierScope;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transforma uma inscricao (lista ordenada de glifos) em um plano de execucao.
 *
 * Regras:
 *  - Forma: entrega do feitico (parametros do glifo, limitados ao intervalo da definicao).
 *  - Efeito: entra no segmento aberto. Modificadores "previous_effect" valem para o efeito imediatamente
 *    anterior; se ainda nao houve efeito, valem para o proximo (as ordens Area,Dano e Dano,Area funcionam).
 *  - Filtro: vale para o segmento aberto e para os seguintes.
 *  - Condicao: abre um segmento novo que so roda se a condicao passar; "Apos Segundos" tambem o atrasa.
 *  - Atraso (modificador next_segment): abre um segmento novo que roda alguns segundos depois.
 *  - Modificador "delivery": altera a forma; "all": efeito anterior e forma.
 */
public final class InscriptionCompiler {
    private InscriptionCompiler() {}

    private static final class SegmentBuilder {
        Optional<CompiledExecutionPlan.ConditionGate> condition = Optional.empty();
        final List<CompiledExecutionPlan.EffectCallCompiled> effects = new ArrayList<>();
        final List<CompiledExecutionPlan.ChainRule> chainRules = new ArrayList<>();
        List<CompiledExecutionPlan.FilterSpec> filters = List.of();
        int delayTicks;

        boolean isEmpty() {
            return effects.isEmpty() && condition.isEmpty() && chainRules.isEmpty();
        }
    }

    public static CompiledExecutionPlan compile(Inscription inscription) {
        List<SegmentBuilder> built = new ArrayList<>();
        SegmentBuilder current = new SegmentBuilder();
        List<CompiledExecutionPlan.FilterSpec> filters = new ArrayList<>();
        List<GlyphInstance> queuedEffectModifiers = new ArrayList<>();
        List<GlyphInstance> deliveryModifiers = new ArrayList<>();
        ModifierApplicator.DeliverySpecCompiled delivery = null;
        double mana = 0;

        for (GlyphInstance instance : inscription.glyphs()) {
            GlyphDefinition def = GlyphRegistry.getOrThrow(instance.glyphId());
            mana += def.manaCost(instance.parameters());

            switch (def.category()) {
                case FORM -> {
                    Map<String, Double> params = def.resolveParams(instance.parameters());
                    delivery = new ModifierApplicator.DeliverySpecCompiled(instance.glyphId(), def.behavior().handler(),
                            params, params.getOrDefault("range", 16.0));
                }
                case EFFECT -> {
                    CompiledExecutionPlan.EffectCallCompiled effect = new CompiledExecutionPlan.EffectCallCompiled(
                            instance.glyphId(), def.behavior().handler(), Map.copyOf(def.resolveParams(instance.parameters())),
                            def.behavior().magnitudeKeys());
                    for (GlyphInstance queued : queuedEffectModifiers) {
                        effect = applyToEffect(effect, queued);
                    }
                    queuedEffectModifiers.clear();
                    current.effects.add(effect);
                }
                case FILTER -> {
                    CompiledExecutionPlan.FilterSpec spec = new CompiledExecutionPlan.FilterSpec(instance.glyphId(), def.behavior().handler());
                    if (!filters.contains(spec)) filters.add(spec);
                }
                case MODIFIER -> {
                    ModifierScope scope = def.modifierScope().orElse(null);
                    if (scope == null) break;
                    switch (scope.target()) {
                        case "previous", "previous_effect" -> applyToLastEffect(built, current, queuedEffectModifiers, instance);
                        case "delivery" -> deliveryModifiers.add(instance);
                        case "all" -> {
                            applyToLastEffect(built, current, queuedEffectModifiers, instance);
                            deliveryModifiers.add(instance);
                        }
                        case "next_segment" -> {
                            double seconds = def.resolveParam(scope.delayParam().orElse("seconds"), instance.parameters());
                            flush(built, current, filters);
                            current = new SegmentBuilder();
                            current.delayTicks = (int) Math.round(seconds * 20);
                        }
                        default -> {
                        }
                    }
                }
                case CONDITION -> {
                    flush(built, current, filters);
                    current = new SegmentBuilder();
                    current.condition = Optional.of(new CompiledExecutionPlan.ConditionGate(
                            instance.glyphId(), def.behavior().handler(), def.resolveParams(instance.parameters())));
                    if (def.parameters().containsKey("seconds")) {
                        current.delayTicks = (int) Math.round(def.resolveParam("seconds", instance.parameters()) * 20);
                    }
                }
                case ACTIVATOR -> current.chainRules.add(new CompiledExecutionPlan.ChainRule("activate", built.size()));
            }
        }
        flush(built, current, filters);

        if (delivery == null) {
            delivery = new ModifierApplicator.DeliverySpecCompiled(
                    new ResourceLocation("arcane", "self"), new ResourceLocation("arcane", "self"), Map.of(), 3.0);
        }
        delivery = ModifierApplicator.applyDeliveryModifiers(delivery, deliveryModifiers);

        List<CompiledExecutionPlan.ExecutionSegment> segments = new ArrayList<>();
        for (SegmentBuilder b : built) {
            segments.add(new CompiledExecutionPlan.ExecutionSegment(b.condition, b.filters, List.copyOf(b.effects),
                    List.copyOf(b.chainRules), b.delayTicks));
        }
        CompiledExecutionPlan.DeliverySpec spec = new CompiledExecutionPlan.DeliverySpec(
                delivery.formId(), delivery.handler(), delivery.parameters(), delivery.range());
        return new CompiledExecutionPlan(spec, List.copyOf(segments), (int) Math.ceil(mana), inscription.fingerprint());
    }

    private static void flush(List<SegmentBuilder> built, SegmentBuilder segment,
                              List<CompiledExecutionPlan.FilterSpec> filters) {
        if (segment.isEmpty()) return;
        segment.filters = List.copyOf(filters);
        built.add(segment);
    }

    private static CompiledExecutionPlan.EffectCallCompiled applyToEffect(
            CompiledExecutionPlan.EffectCallCompiled effect, GlyphInstance modifier) {
        return GlyphRegistry.get(modifier.glyphId()).flatMap(GlyphDefinition::modifierScope)
                .map(scope -> ModifierApplicator.mutateEffect(effect, scope, modifier))
                .orElse(effect);
    }

    /** Aplica ao efeito imediatamente anterior (no segmento aberto ou nos ja fechados); sem efeito ainda, enfileira. */
    private static void applyToLastEffect(List<SegmentBuilder> built, SegmentBuilder current,
                                          List<GlyphInstance> queue, GlyphInstance modifier) {
        if (!current.effects.isEmpty()) {
            int last = current.effects.size() - 1;
            current.effects.set(last, applyToEffect(current.effects.get(last), modifier));
            return;
        }
        for (int i = built.size() - 1; i >= 0; i--) {
            List<CompiledExecutionPlan.EffectCallCompiled> effects = built.get(i).effects;
            if (!effects.isEmpty()) {
                int last = effects.size() - 1;
                effects.set(last, applyToEffect(effects.get(last), modifier));
                return;
            }
        }
        queue.add(modifier);
    }
}
