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
    private static final int MAX_LOG = 60;
    private final List<String> discoveryLog = new ArrayList<>();
    public static final int DEFAULT_PAGES = 8;
    public static final int MAX_PAGES = 120;
    private int pages = DEFAULT_PAGES;
    private int visualStage = 1;
    private boolean grimoirePresent = true;
    private long grimoireDestroyedUntilTick = 0L;
    private UUID activeInscriptionId = null;

    public Map<ResourceLocation, GlyphKnowledge> knowledge() { return knowledge; }
    public Map<UUID, Inscription> spellbook() { return spellbook; }
    public List<String> discoveryLog() { return discoveryLog; }
    public int visualStage() { return visualStage; }

    /** Capacidade: quantas magias cabem. Cresce com Paginas Arcanas (e, no futuro, rituais). */
    public int pages() { return pages; }
    public int freePages() { return pages - spellbook.size(); }

    public void addPages(int amount) {
        pages = Math.max(1, Math.min(MAX_PAGES, pages + amount));
        visualStage = stageFor(pages);
    }

    /** Estagios visuais do design: caderno, livro, livro grosso, flutuante, paginas em orbita, so paginas, infinito. */
    public static int stageFor(int pages) {
        if (pages < 12) return 1;
        if (pages < 20) return 2;
        if (pages < 32) return 3;
        if (pages < 48) return 4;
        if (pages < 70) return 5;
        if (pages < 100) return 6;
        return 7;
    }

    public boolean hasSameSpell(Inscription spell) {
        return spellbook.values().stream().anyMatch(i -> i.glyphs().equals(spell.glyphs()));
    }

    /**
     * Recupera um grimorio (o proprio, ao pega-lo de volta): conhecimento mais alto de cada glifo, todas as magias
     * que ainda nao existem aqui (ignorando a capacidade), paginas e diario.
     */
    public void mergeFrom(GrimoireState other) {
        other.knowledge.forEach(this::learnGlyph);
        other.spellbook.forEach((id, spell) -> {
            if (!spellbook.containsKey(id) && !hasSameSpell(spell)) spellbook.put(id, spell);
        });
        pages = Math.max(pages, other.pages);
        visualStage = stageFor(pages);
        other.discoveryLog.forEach(this::logDiscovery);
        if (activeInscriptionId == null && other.activeInscriptionId != null && spellbook.containsKey(other.activeInscriptionId)) {
            activeInscriptionId = other.activeInscriptionId;
        }
    }
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

    /** Apaga uma magia do grimorio. Se era a ativa, passa para outra (ou nenhuma). */
    public boolean removeInscription(UUID id) {
        if (spellbook.remove(id) == null) return false;
        if (id.equals(activeInscriptionId)) {
            activeInscriptionId = spellbook.keySet().stream().findFirst().orElse(null);
        }
        return true;
    }

    /** Magia salva com este nome (sem distinguir maiusculas), para o salvar-por-cima da bancada. */
    public Optional<Inscription> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return spellbook.values().stream().filter(i -> i.metadata().name().equalsIgnoreCase(name.trim())).findFirst();
    }

    public Optional<Inscription> getInscription(UUID id) {
        return Optional.ofNullable(spellbook.get(id));
    }

    public void logDiscovery(String entry) {
        discoveryLog.add(entry);
        while (discoveryLog.size() > MAX_LOG) {
            discoveryLog.remove(0);
        }
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
        ListTag log = new ListTag();
        discoveryLog.forEach(entry -> log.add(net.minecraft.nbt.StringTag.valueOf(entry)));
        tag.put("discoveryLog", log);
        tag.putInt("pages", pages);
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
        discoveryLog.clear();
        if (tag.contains("discoveryLog", Tag.TAG_LIST)) {
            ListTag log = tag.getList("discoveryLog", Tag.TAG_STRING);
            for (int i = 0; i < log.size(); i++) discoveryLog.add(log.getString(i));
        }
        pages = tag.contains("pages") ? Math.max(1, Math.min(MAX_PAGES, tag.getInt("pages"))) : DEFAULT_PAGES;
        visualStage = stageFor(pages);
        grimoirePresent = !tag.contains("grimoirePresent") || tag.getBoolean("grimoirePresent");
        grimoireDestroyedUntilTick = tag.getLong("destroyedUntil");
        activeInscriptionId = tag.hasUUID("activeInscription") ? tag.getUUID("activeInscription") : null;
    }

    public static GrimoireState createDefault() {
        GrimoireState state = new GrimoireState();
        state.learnGlyph(new ResourceLocation("arcane", "self"), GlyphKnowledge.MASTERED);
        state.learnGlyph(new ResourceLocation("arcane", "projectile"), GlyphKnowledge.MASTERED);
        state.learnGlyph(new ResourceLocation("arcane", "damage"), GlyphKnowledge.MASTERED);
        Inscription starter = new Inscription(List.of(
                new com.arcane.inscription.GlyphInstance(new ResourceLocation("arcane", "projectile")),
                new com.arcane.inscription.GlyphInstance(new ResourceLocation("arcane", "damage"))),
                new com.arcane.inscription.InscriptionMetadata("Faísca", new ResourceLocation("arcane", "common"),
                        new ResourceLocation("arcane", "arcane_pen"), 1.0, Optional.empty(), 0L,
                        com.arcane.inscription.InscriptionSourceKind.GRIMOIRE));
        state.addInscription(starter);
        state.setActiveInscription(starter.id());
        return state;
    }
}
