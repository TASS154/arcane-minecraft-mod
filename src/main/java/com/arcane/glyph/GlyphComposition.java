package com.arcane.glyph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public record GlyphComposition(
        int maxPerInscription,
        String position,
        List<String> requires,
        List<String> allowedAfter,
        List<String> disallowedAfter
) {
    public static final GlyphComposition DEFAULT = new GlyphComposition(99, "any", List.of(), List.of(), List.of());

    public static final Codec<GlyphComposition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("max_per_inscription", 99).forGetter(GlyphComposition::maxPerInscription),
            Codec.STRING.optionalFieldOf("position", "any").forGetter(GlyphComposition::position),
            Codec.STRING.listOf().optionalFieldOf("requires", List.of()).forGetter(GlyphComposition::requires),
            Codec.STRING.listOf().optionalFieldOf("allowed_after", List.of()).forGetter(GlyphComposition::allowedAfter),
            Codec.STRING.listOf().optionalFieldOf("disallowed_after", List.of()).forGetter(GlyphComposition::disallowedAfter)
    ).apply(instance, GlyphComposition::new));
}
