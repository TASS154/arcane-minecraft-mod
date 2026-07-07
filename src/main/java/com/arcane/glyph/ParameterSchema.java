package com.arcane.glyph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

public record ParameterSchema(Map<String, ParameterDef> parameters) {
    public static final ParameterSchema EMPTY = new ParameterSchema(Map.of());

    public static final Codec<ParameterSchema> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, ParameterDef.CODEC).optionalFieldOf("parameters", Map.of())
                    .forGetter(ParameterSchema::parameters)
    ).apply(instance, ParameterSchema::new));

    public record ParameterDef(String type, double defaultValue, double min, double max, List<String> enumValues) {
        public static final Codec<ParameterDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(ParameterDef::type),
                Codec.DOUBLE.optionalFieldOf("default", 0.0).forGetter(ParameterDef::defaultValue),
                Codec.DOUBLE.optionalFieldOf("min", Double.NEGATIVE_INFINITY).forGetter(ParameterDef::min),
                Codec.DOUBLE.optionalFieldOf("max", Double.POSITIVE_INFINITY).forGetter(ParameterDef::max),
                Codec.STRING.listOf().optionalFieldOf("enum", List.of()).forGetter(ParameterDef::enumValues)
        ).apply(instance, ParameterDef::new));
    }
}
