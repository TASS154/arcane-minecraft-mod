package com.arcane.glyph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ModifierScope(
        String target,
        String kind,
        Map<String, ModifierMutation> mutate,
        boolean wrap,
        Optional<String> delayParam
) {
    public static final Codec<ModifierScope> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("target").forGetter(ModifierScope::target),
            Codec.STRING.optionalFieldOf("kind", "effect").forGetter(ModifierScope::kind),
            Codec.unboundedMap(Codec.STRING, ModifierMutation.CODEC).optionalFieldOf("mutate", Map.of()).forGetter(ModifierScope::mutate),
            Codec.BOOL.optionalFieldOf("wrap", false).forGetter(ModifierScope::wrap),
            Codec.STRING.optionalFieldOf("delay_param").forGetter(ModifierScope::delayParam)
    ).apply(instance, ModifierScope::new));

    public record ModifierMutation(String op, double value, Optional<String> param) {
        public static final Codec<ModifierMutation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("op").forGetter(ModifierMutation::op),
                Codec.DOUBLE.optionalFieldOf("value", 1.0).forGetter(ModifierMutation::value),
                Codec.STRING.optionalFieldOf("param").forGetter(ModifierMutation::param)
        ).apply(instance, ModifierMutation::new));
    }
}
