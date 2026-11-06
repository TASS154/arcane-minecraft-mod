package com.arcane.glyph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record GlyphDefinition(
        ResourceLocation id,
        GlyphCategory category,
        GlyphBehavior behavior,
        GlyphComposition composition,
        GlyphCost cost,
        Map<String, ParameterSchema.ParameterDef> parameters,
        Optional<ModifierScope> modifierScope,
        int tier,
        List<String> families
) {
    public static final Codec<GlyphDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(GlyphDefinition::id),
            GlyphCategory.CODEC.fieldOf("category").forGetter(GlyphDefinition::category),
            GlyphBehavior.CODEC.fieldOf("behavior").forGetter(GlyphDefinition::behavior),
            GlyphComposition.CODEC.optionalFieldOf("composition", GlyphComposition.DEFAULT).forGetter(GlyphDefinition::composition),
            GlyphCost.CODEC.optionalFieldOf("cost", GlyphCost.DEFAULT).forGetter(GlyphDefinition::cost),
            Codec.unboundedMap(Codec.STRING, ParameterSchema.ParameterDef.CODEC).optionalFieldOf("parameters", Map.of()).forGetter(GlyphDefinition::parameters),
            ModifierScope.CODEC.optionalFieldOf("modifier_scope").forGetter(GlyphDefinition::modifierScope),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(GlyphDefinition::tier),
            Codec.STRING.listOf().optionalFieldOf("families", List.of()).forGetter(GlyphDefinition::families)
    ).apply(instance, GlyphDefinition::new));

    /** Parametro principal: o primeiro de magnitude_keys, ou o primeiro em ordem alfabetica. */
    public Optional<String> primaryParam() {
        if (parameters.isEmpty()) return Optional.empty();
        for (String key : behavior.magnitudeKeys()) {
            if (parameters.containsKey(key)) return Optional.of(key);
        }
        return parameters.keySet().stream().sorted().findFirst();
    }

    /** Valor do parametro: o informado (limitado ao intervalo do glifo) ou o padrao do glifo. */
    public double resolveParam(String key, java.util.Map<String, Double> given) {
        ParameterSchema.ParameterDef def = parameters.get(key);
        if (def == null) return given.getOrDefault(key, 0.0);
        double value = given.getOrDefault(key, def.defaultValue());
        if (Double.isNaN(value)) value = def.defaultValue();
        return Math.max(def.min(), Math.min(def.max(), value));
    }

    /** Todos os parametros do glifo com padrao e limites aplicados. */
    public java.util.Map<String, Double> resolveParams(java.util.Map<String, Double> given) {
        java.util.Map<String, Double> out = new java.util.HashMap<>();
        for (String key : parameters.keySet()) {
            out.put(key, resolveParam(key, given));
        }
        return out;
    }

    /** Custo de mana desta instancia. Efeitos escalam linearmente com o parametro principal. */
    public double manaCost(java.util.Map<String, Double> given) {
        double base = cost.mana();
        if (category != GlyphCategory.EFFECT || !"linear".equals(cost.scaling())) return base;
        java.util.Optional<String> key = primaryParam();
        if (key.isEmpty() || !given.containsKey(key.get())) return base;
        double def = parameters.get(key.get()).defaultValue();
        if (def <= 0) return base;
        double ratio = resolveParam(key.get(), given) / def;
        return base * Math.max(0.5, Math.min(6.0, ratio));
    }
}
