package com.arcane.glyph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record GlyphBehavior(
        ResourceLocation handler,
        String affects,
        List<String> magnitudeKeys
) {
    public static final Codec<GlyphBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("handler").forGetter(GlyphBehavior::handler),
            Codec.STRING.optionalFieldOf("affects", "resolved_targets").forGetter(GlyphBehavior::affects),
            Codec.STRING.listOf().optionalFieldOf("magnitude_keys", List.of()).forGetter(GlyphBehavior::magnitudeKeys)
    ).apply(instance, GlyphBehavior::new));
}
