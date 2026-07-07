package com.arcane.inscription;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Inscription(
        int version,
        List<GlyphInstance> glyphs,
        InscriptionMetadata metadata,
        UUID id
) {
    public static final int CURRENT_VERSION = 1;
    public static final Codec<Inscription> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("v", CURRENT_VERSION).forGetter(Inscription::version),
            GlyphInstance.CODEC.listOf().fieldOf("glyphs").forGetter(Inscription::glyphs),
            InscriptionMetadata.CODEC.optionalFieldOf("meta", InscriptionMetadata.DEFAULT).forGetter(Inscription::metadata),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).optionalFieldOf("uuid", UUID.randomUUID()).forGetter(Inscription::id)
    ).apply(instance, Inscription::new));

    public Inscription(List<GlyphInstance> glyphs) {
        this(CURRENT_VERSION, List.copyOf(glyphs), InscriptionMetadata.DEFAULT, UUID.randomUUID());
    }

    public Inscription(List<GlyphInstance> glyphs, InscriptionMetadata metadata) {
        this(CURRENT_VERSION, List.copyOf(glyphs), metadata, UUID.randomUUID());
    }

    public String fingerprint() {
        StringBuilder sb = new StringBuilder();
        for (GlyphInstance glyph : glyphs) {
            sb.append(glyph.glyphId()).append(':');
            glyph.parameters().entrySet().stream().sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                    .forEach(e -> sb.append(e.getKey()).append('=').append(e.getValue()).append(';'));
        }
        sb.append("|p=").append(metadata.precision());
        sb.append("|ink=").append(metadata.ink());
        return Integer.toHexString(Objects.hash(sb.toString()));
    }

    public int totalManaCost(com.arcane.glyph.GlyphRegistryAccessor registry) {
        int total = 0;
        for (GlyphInstance instance : glyphs) {
            total += registry.get(instance.glyphId()).map(g -> g.cost().mana()).orElse(0);
        }
        return total;
    }

    public Inscription withGlyphs(List<GlyphInstance> newGlyphs) {
        return new Inscription(version, List.copyOf(newGlyphs), metadata, id);
    }

    public Inscription withMetadata(InscriptionMetadata newMeta) {
        return new Inscription(version, glyphs, newMeta, id);
    }

    public static Inscription testSelfDamage() {
        List<GlyphInstance> glyphs = new ArrayList<>();
        glyphs.add(new GlyphInstance(new net.minecraft.resources.ResourceLocation("arcane", "self")));
        glyphs.add(new GlyphInstance(new net.minecraft.resources.ResourceLocation("arcane", "damage"),
                java.util.Map.of("amount", 4.0)));
        return new Inscription(glyphs);
    }

    public static Inscription testProjectileDamageFire() {
        List<GlyphInstance> glyphs = new ArrayList<>();
        glyphs.add(new GlyphInstance(new net.minecraft.resources.ResourceLocation("arcane", "projectile")));
        glyphs.add(new GlyphInstance(new net.minecraft.resources.ResourceLocation("arcane", "damage"),
                java.util.Map.of("amount", 5.0)));
        glyphs.add(new GlyphInstance(new net.minecraft.resources.ResourceLocation("arcane", "fire"),
                java.util.Map.of("duration", 60.0)));
        return new Inscription(glyphs);
    }

    @FunctionalInterface
    public interface GlyphRegistryAccessor {
        java.util.Optional<com.arcane.glyph.GlyphDefinition> get(net.minecraft.resources.ResourceLocation id);
    }
}
