package com.arcane.grimoire;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public enum GlyphKnowledge {
    UNKNOWN(0),
    SUSPECTED(1),
    PARTIAL(2),
    IDENTIFIED(3),
    MASTERED(4);

    private final int level;

    GlyphKnowledge(int level) {
        this.level = level;
    }

    public boolean atLeast(GlyphKnowledge other) {
        return this.level >= other.level;
    }

    public static GlyphKnowledge fromLevel(int level) {
        for (GlyphKnowledge k : values()) {
            if (k.level == level) return k;
        }
        return UNKNOWN;
    }
}
