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
}
