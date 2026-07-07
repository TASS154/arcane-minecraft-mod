package com.arcane.inscription;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record GlyphInstance(ResourceLocation glyphId, Map<String, Double> parameters) {
    public static final Codec<GlyphInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(GlyphInstance::glyphId),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("p", Map.of()).forGetter(GlyphInstance::parameters)
    ).apply(instance, GlyphInstance::new));

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
