package com.arcane.glyph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** Componente fisico que um glifo de ritual consome (ex.: 4 perolas do End). */
public record ComponentRequirement(ResourceLocation item, int count) {
  public static final Codec<ComponentRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      ResourceLocation.CODEC.fieldOf("item").forGetter(ComponentRequirement::item),
      Codec.INT.optionalFieldOf("count", 1).forGetter(ComponentRequirement::count)
  ).apply(instance, ComponentRequirement::new));
}
