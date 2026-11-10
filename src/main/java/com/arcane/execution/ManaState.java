package com.arcane.execution;

import net.minecraft.nbt.CompoundTag;

/**
 * Mana do jogador. O maximo e: (base editavel por comando + bonus por glifos descobertos + bonus de pocao) x multiplicador
 * (grimorio perdido = 0,5). A regeneracao pode ser multiplicada por pocao. Os bonus sao recalculados pelo servidor
 * (nao sao gravados); so a base e o valor atual sao.
 */
public class ManaState {
    public static final int DEFAULT_MAX = 100;
    private float current = DEFAULT_MAX;
    private float max = DEFAULT_MAX;
    private float regenPerTick = 0.05f;
    private float maxMultiplier = 1.0f;
    private float glyphBonus = 0f;
    private float potionBonus = 0f;
    private float regenMultiplier = 1f;

    public float current() { return current; }
    public float max() { return (max + glyphBonus + potionBonus) * maxMultiplier; }
    /** Maximo base (o que o comando /arcane mana max edita). */
    public float baseMax() { return max; }
    public float glyphBonus() { return glyphBonus; }
    public float potionBonus() { return potionBonus; }
    public float regenPerTick() { return regenPerTick * regenMultiplier; }
    public float regenMultiplier() { return regenMultiplier; }
    public float maxMultiplier() { return maxMultiplier; }

    public void setMaxMultiplier(float multiplier) {
        this.maxMultiplier = multiplier;
        clamp();
    }

    public void setBaseMax(float value) {
        this.max = Math.max(1f, value);
        clamp();
    }

    public void setBonuses(float glyphBonus, float potionBonus, float regenMultiplier) {
        this.glyphBonus = Math.max(0f, glyphBonus);
        this.potionBonus = Math.max(0f, potionBonus);
        this.regenMultiplier = Math.max(0f, regenMultiplier);
        clamp();
    }

    public void set(float value) {
        this.current = Math.max(0f, Math.min(max(), value));
    }

    private void clamp() {
        if (current > max()) current = max();
    }

    public boolean trySpend(int amount) {
        if (current < amount) {
            return false;
        }
        current -= amount;
        return true;
    }

    /** Zera a mana (Juramento: Mana selada). */
    public void drain() {
        current = 0f;
    }

    public void tick() {
        if (current < max()) {
            current = Math.min(max(), current + regenPerTick());
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
