package com.arcane.glyph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

public enum GlyphCategory implements StringRepresentable {
    FORM("form"),
    EFFECT("effect"),
    FILTER("filter"),
    MODIFIER("modifier"),
    CONDITION("condition"),
    ACTIVATOR("activator");

    public static final Codec<GlyphCategory> CODEC = StringRepresentable.fromEnum(GlyphCategory::values);

    private final String name;

    GlyphCategory(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
