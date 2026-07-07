package com.arcane.glyph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GlyphCost(int mana, String scaling) {
    public static final GlyphCost DEFAULT = new GlyphCost(0, "linear");

    public static final Codec<GlyphCost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("mana", 0).forGetter(GlyphCost::mana),
            Codec.STRING.optionalFieldOf("scaling", "linear").forGetter(GlyphCost::scaling)
    ).apply(instance, GlyphCost::new));
}
