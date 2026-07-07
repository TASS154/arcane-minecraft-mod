package com.arcane.inscription;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

public record InscriptionMetadata(
        String name,
        ResourceLocation ink,
        ResourceLocation pen,
        double precision,
        Optional<UUID> author,
        long created,
        InscriptionSourceKind sourceKind
) {
    public static final InscriptionMetadata DEFAULT = new InscriptionMetadata(
            "", new ResourceLocation("arcane", "common"), new ResourceLocation("arcane", "arcane_pen"),
            1.0, Optional.empty(), 0L, InscriptionSourceKind.GRIMOIRE);

    public static final Codec<InscriptionMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(InscriptionMetadata::name),
            ResourceLocation.CODEC.optionalFieldOf("ink", new ResourceLocation("arcane", "common")).forGetter(InscriptionMetadata::ink),
            ResourceLocation.CODEC.optionalFieldOf("pen", new ResourceLocation("arcane", "arcane_pen")).forGetter(InscriptionMetadata::pen),
            Codec.DOUBLE.optionalFieldOf("precision", 1.0).forGetter(InscriptionMetadata::precision),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).optionalFieldOf("author").forGetter(InscriptionMetadata::author),
            Codec.LONG.optionalFieldOf("created", 0L).forGetter(InscriptionMetadata::created),
            InscriptionSourceKind.CODEC.optionalFieldOf("source", InscriptionSourceKind.GRIMOIRE).forGetter(InscriptionMetadata::sourceKind)
    ).apply(instance, InscriptionMetadata::new));
}
