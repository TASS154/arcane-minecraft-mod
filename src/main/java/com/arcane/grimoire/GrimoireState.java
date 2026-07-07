package com.arcane.grimoire;

import com.arcane.inscription.Inscription;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class GrimoireState {
    private final Map<ResourceLocation, GlyphKnowledge> knowledge = new HashMap<>();
    private final Map<UUID, Inscription> spellbook = new LinkedHashMap<>();
    private final List<String> discoveryLog = new ArrayList<>();
    private int visualStage = 1;
    private boolean grimoirePresent = true;
    private long grimoireDestroyedUntilTick = 0L;
    private UUID activeInscriptionId = null;

    public Map<ResourceLocation, GlyphKnowledge> knowledge() { return knowledge; }
    public Map<UUID, Inscription> spellbook() { return spellbook; }
    public List<String> discoveryLog() { return discoveryLog; }
    public int visualStage() { return visualStage; }
    public boolean grimoirePresent() { return grimoirePresent; }
    public long grimoireDestroyedUntilTick() { return grimoireDestroyedUntilTick; }
    public Optional<UUID> activeInscriptionId() { return Optional.ofNullable(activeInscriptionId); }

    public void setActiveInscription(UUID id) { this.activeInscriptionId = id; }
    public void setGrimoirePresent(boolean present) { this.grimoirePresent = present; }
    public void setGrimoireDestroyedUntilTick(long tick) { this.grimoireDestroyedUntilTick = tick; }
    public void setVisualStage(int stage) { this.visualStage = stage; }

    public boolean knowsGlyph(ResourceLocation glyphId, GlyphKnowledge minimum) {
        return knowledge.getOrDefault(glyphId, GlyphKnowledge.UNKNOWN).atLeast(minimum);
    }

    public void learnGlyph(ResourceLocation glyphId, GlyphKnowledge level) {
        GlyphKnowledge current = knowledge.getOrDefault(glyphId, GlyphKnowledge.UNKNOWN);
        if (level.atLeast(current)) {
            knowledge.put(glyphId, level);
        }
    }

    public void addInscription(Inscription inscription) {
        spellbook.put(inscription.id(), inscription);
    }

    public Optional<Inscription> getInscription(UUID id) {
        return Optional.ofNullable(spellbook.get(id));
    }

    public void logDiscovery(String entry) {
        discoveryLog.add(entry);
    }

    public void save(CompoundTag tag) {
        CompoundTag knowledgeTag = new CompoundTag();
        knowledge.forEach((id, k) -> knowledgeTag.putInt(id.toString(), k.ordinal()));
        tag.put("knowledge", knowledgeTag);

        ListTag spells = new ListTag();
        spellbook.forEach((id, inscription) -> {
            CompoundTag spellTag = new CompoundTag();
            spellTag.putUUID("id", id);
            Inscription.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, inscription)
                    .result().ifPresent(encoded -> spellTag.put("data", encoded));
            spells.add(spellTag);
        });
        tag.put("spellbook", spells);
        tag.putInt("visualStage", visualStage);
        tag.putBoolean("grimoirePresent", grimoirePresent);
        tag.putLong("destroyedUntil", grimoireDestroyedUntilTick);
        if (activeInscriptionId != null) {
            tag.putUUID("activeInscription", activeInscriptionId);
        }
    }

    public void load(CompoundTag tag) {
        knowledge.clear();
        if (tag.contains("knowledge", Tag.TAG_COMPOUND)) {
            CompoundTag knowledgeTag = tag.getCompound("knowledge");
            for (String key : knowledgeTag.getAllKeys()) {
                knowledge.put(new ResourceLocation(key), GlyphKnowledge.fromLevel(knowledgeTag.getInt(key)));
            }
        }
        spellbook.clear();
        if (tag.contains("spellbook", Tag.TAG_LIST)) {
            ListTag spells = tag.getList("spellbook", Tag.TAG_COMPOUND);
            for (Tag element : spells) {
                CompoundTag spellTag = (CompoundTag) element;
                UUID id = spellTag.getUUID("id");
                Inscription.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, spellTag.get("data"))
                        .result().ifPresent(inscription -> spellbook.put(id, inscription));
            }
        }
        visualStage = tag.getInt("visualStage");
        if (visualStage == 0) visualStage = 1;
        grimoirePresent = !tag.contains("grimoirePresent") || tag.getBoolean("grimoirePresent");
        grimoireDestroyedUntilTick = tag.getLong("destroyedUntil");
        activeInscriptionId = tag.hasUUID("activeInscription") ? tag.getUUID("activeInscription") : null;
    }

    public static GrimoireState createDefault() {
        GrimoireState state = new GrimoireState();
        state.learnGlyph(new ResourceLocation("arcane", "self"), GlyphKnowledge.MASTERED);
        state.learnGlyph(new ResourceLocation("arcane", "projectile"), GlyphKnowledge.MASTERED);
        state.learnGlyph(new ResourceLocation("arcane", "damage"), GlyphKnowledge.MASTERED);
        state.addInscription(Inscription.testProjectileDamageFire());
        state.setActiveInscription(state.spellbook.keySet().iterator().next());
        return state;
    }
}
