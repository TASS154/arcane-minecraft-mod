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
 *  - Grupo / Fim do Grupo: um modificador logo depois de Fim do Grupo vale para TODOS os efeitos do grupo.
 *  - Repeticao: duplica o efeito anterior (cada copia custa mana de novo).
 *  - Filtro: vale para o segmento aberto e para os seguintes.
 *  - Condicao: abre um segmento novo que so roda se a condicao passar; "Apos Segundos" tambem o atrasa.
 *  - Atraso (modificador next_segment): abre um segmento novo que roda alguns segundos depois.
 *  - Modificador "delivery": altera a forma; "all": efeito anterior e forma.
 *  - O custo total cresce exponencialmente com o numero de glifos (SpellLimits).
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

    /** Referencia estavel a um efeito ja compilado (segmento + posicao), para modificadores e grupos. */
    private record Slot(SegmentBuilder segment, int index) {
        CompiledExecutionPlan.EffectCallCompiled get() { return segment.effects.get(index); }
        void set(CompiledExecutionPlan.EffectCallCompiled effect) { segment.effects.set(index, effect); }
    }

    public static CompiledExecutionPlan compile(Inscription inscription) {
        List<SegmentBuilder> built = new ArrayList<>();
        SegmentBuilder current = new SegmentBuilder();
        List<CompiledExecutionPlan.FilterSpec> filters = new ArrayList<>();
        List<GlyphInstance> queuedEffectModifiers = new ArrayList<>();
        List<GlyphInstance> deliveryModifiers = new ArrayList<>();
        List<Slot> allEffects = new ArrayList<>();
        List<Integer> groupStarts = new ArrayList<>();
        int[] lastGroup = null; // [inicio, fim) em allEffects, valido ate o proximo efeito/forma/condicao
        ModifierApplicator.DeliverySpecCompiled delivery = null;
        double mana = 0;

        for (GlyphInstance instance : inscription.glyphs()) {
            GlyphDefinition def = GlyphRegistry.getOrThrow(instance.glyphId());
            mana += def.manaCost(instance.parameters());

            switch (def.category()) {
                case FORM -> {
                    lastGroup = null;
                    Map<String, Double> params = def.resolveParams(instance.parameters());
                    delivery = new ModifierApplicator.DeliverySpecCompiled(instance.glyphId(), def.behavior().handler(),
                            params, params.getOrDefault("range", 16.0));
                }
                case EFFECT -> {
                    lastGroup = null;
                    CompiledExecutionPlan.EffectCallCompiled effect = new CompiledExecutionPlan.EffectCallCompiled(
                            instance.glyphId(), def.behavior().handler(), Map.copyOf(def.resolveParams(instance.parameters())),
                            def.behavior().magnitudeKeys());
                    for (GlyphInstance queued : queuedEffectModifiers) {
                        effect = applyToEffect(effect, queued);
                    }
                    queuedEffectModifiers.clear();
                    current.effects.add(effect);
                    allEffects.add(new Slot(current, current.effects.size() - 1));
                }
                case FILTER -> {
                    CompiledExecutionPlan.FilterSpec spec = new CompiledExecutionPlan.FilterSpec(instance.glyphId(), def.behavior().handler());
                    if (!filters.contains(spec)) filters.add(spec);
                }
                case MODIFIER -> {
                    ModifierScope scope = def.modifierScope().orElse(null);
                    if (scope == null) break;
                    switch (scope.target()) {
                        case "group_start" -> groupStarts.add(allEffects.size());
                        case "group_end" -> {
                            if (!groupStarts.isEmpty()) {
                                int start = groupStarts.remove(groupStarts.size() - 1);
                                lastGroup = start < allEffects.size() ? new int[] {start, allEffects.size()} : null;
                            }
                        }
                        case "previous", "previous_effect" -> applyToPrevious(allEffects, lastGroup, queuedEffectModifiers, instance);
                        case "delivery" -> deliveryModifiers.add(instance);
                        case "all" -> {
                            applyToPrevious(allEffects, lastGroup, queuedEffectModifiers, instance);
                            deliveryModifiers.add(instance);
                        }
                        case "repeat" -> {
                            if (!allEffects.isEmpty()) {
                                Slot last = allEffects.get(allEffects.size() - 1);
                                int times = (int) Math.round(def.resolveParam("times", instance.parameters()));
                                GlyphDefinition effectDef = GlyphRegistry.get(last.get().glyphId()).orElse(null);
                                for (int i = 1; i < times; i++) {
                                    last.segment().effects.add(last.get());
                                    allEffects.add(new Slot(last.segment(), last.segment().effects.size() - 1));
                                    if (effectDef != null) mana += effectDef.manaCost(last.get().parameters());
                                }
                            }
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
                    lastGroup = null;
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
        mana *= SpellLimits.multiplier(inscription.glyphs().size());
        mana *= SpellLimits.zoneMultiplier(delivery.parameters().getOrDefault("zone_seconds", 0.0), delivery.parameters().getOrDefault("zone_interval", 1.0));
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

    /**
     * Aplica ao efeito imediatamente anterior, ou a todos os efeitos do ultimo grupo fechado; sem efeito
     * nenhum ainda, enfileira para o proximo.
     */
    private static void applyToPrevious(List<Slot> allEffects, int[] lastGroup, List<GlyphInstance> queue, GlyphInstance modifier) {
        if (allEffects.isEmpty()) {
            queue.add(modifier);
            return;
        }
        int from = lastGroup != null ? lastGroup[0] : allEffects.size() - 1;
        int to = lastGroup != null ? lastGroup[1] : allEffects.size();
        for (int i = from; i < to; i++) {
            Slot slot = allEffects.get(i);
            slot.set(applyToEffect(slot.get(), modifier));
        }
    }
}
