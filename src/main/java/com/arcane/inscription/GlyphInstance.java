package com.arcane.inscription;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record GlyphInstance(ResourceLocation glyphId, Map<String, Double> parameters) {
    private static final Codec<GlyphInstance> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(GlyphInstance::glyphId),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("p", Map.of()).forGetter(GlyphInstance::parameters)
    ).apply(instance, GlyphInstance::new));

    /** Ao ler, ids antigos viram os atuais ({@link com.arcane.glyph.GlyphAliases}); ao gravar, sai o id atual. */
    public static final Codec<GlyphInstance> CODEC = RAW_CODEC.xmap(com.arcane.glyph.GlyphAliases::migrate, g -> g);

    public GlyphInstance(ResourceLocation glyphId) {
        this(glyphId, Map.of());
    }

    public double param(String key, double defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }

    public GlyphInstance withParameter(String key, double value) {
        Map<String, Double> copy = new HashMap<>(parameters);
        copy.put(key, value);
        return new GlyphInstance(glyphId, Map.copyOf(copy));
    }
}
