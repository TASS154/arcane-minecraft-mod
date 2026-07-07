package com.arcane.execution;

import net.minecraft.nbt.CompoundTag;

public class ManaState {
    public static final int DEFAULT_MAX = 100;
    private float current = DEFAULT_MAX;
    private float max = DEFAULT_MAX;
    private float regenPerTick = 0.05f;
    private float maxMultiplier = 1.0f;

    public float current() { return current; }
    public float max() { return max * maxMultiplier; }
    public float regenPerTick() { return regenPerTick; }
    public float maxMultiplier() { return maxMultiplier; }

    public void setMaxMultiplier(float multiplier) {
        this.maxMultiplier = multiplier;
        if (current > max()) {
            current = max();
        }
    }

    public boolean trySpend(int amount) {
        if (current < amount) {
            return false;
        }
        current -= amount;
        return true;
    }

    public void tick() {
        if (current < max()) {
            current = Math.min(max(), current + regenPerTick);
        }
    }

    public void save(CompoundTag tag) {
        tag.putFloat("current", current);
        tag.putFloat("max", max);
        tag.putFloat("regen", regenPerTick);
        tag.putFloat("maxMultiplier", maxMultiplier);
    }

    public void load(CompoundTag tag) {
        current = tag.getFloat("current");
        max = tag.contains("max") ? tag.getFloat("max") : DEFAULT_MAX;
        regenPerTick = tag.contains("regen") ? tag.getFloat("regen") : 0.05f;
        maxMultiplier = tag.contains("maxMultiplier") ? tag.getFloat("maxMultiplier") : 1.0f;
    }
}
