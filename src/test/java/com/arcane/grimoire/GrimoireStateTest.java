package com.arcane.grimoire;

import com.arcane.TestGlyphs;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionMetadata;
import com.arcane.inscription.InscriptionSourceKind;
import com.arcane.inscription.InscriptionValidator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.arcane.TestGlyphs.g;
import static org.junit.jupiter.api.Assertions.*;

class GrimoireStateTest {
  @BeforeAll
  static void loadGlyphs() {
    TestGlyphs.load();
  }

  private static Inscription named(String name, GlyphInstance... glyphs) {
    return new Inscription(List.of(glyphs), new InscriptionMetadata(name, new ResourceLocation("arcane", "common"),
        new ResourceLocation("arcane", "arcane_pen"), 1.0, Optional.empty(), 0L, InscriptionSourceKind.WORKBENCH));
  }

  @Test
  void starterSpellWorksOutOfTheBox() {
    GrimoireState state = GrimoireState.createDefault();
    Inscription starter = state.getInscription(state.activeInscriptionId().orElseThrow()).orElseThrow();
    var result = InscriptionValidator.validate(starter, Optional.of(state));
    assertTrue(result.valid(), "a magia inicial deve ser conjuravel sem conhecer Fogo: " + result.message().getString());
  }

  @Test
  void removingTheActiveSpellPromotesAnotherOne() {
    GrimoireState state = new GrimoireState();
    Inscription a = named("A", g("self"), g("heal"));
    Inscription b = named("B", g("self"), g("heal"));
    state.addInscription(a);
    state.addInscription(b);
    state.setActiveInscription(a.id());

    assertTrue(state.removeInscription(a.id()));
    assertEquals(Optional.of(b.id()), state.activeInscriptionId());
    assertTrue(state.removeInscription(b.id()));
    assertTrue(state.activeInscriptionId().isEmpty());
    assertFalse(state.removeInscription(b.id()), "apagar de novo nao faz nada");
  }

  @Test
  void findByNameIgnoresCaseAndBlank() {
    GrimoireState state = new GrimoireState();
    Inscription a = named("Bola de Fogo", g("self"), g("heal"));
    state.addInscription(a);
    assertEquals(Optional.of(a), state.findByName("bola de fogo"));
    assertEquals(Optional.of(a), state.findByName("  Bola de Fogo "));
    assertTrue(state.findByName("").isEmpty());
    assertTrue(state.findByName("outra").isEmpty());
  }

  @Test
  void spellsParametersAndJournalSurviveSaveAndLoad() {
    GrimoireState state = new GrimoireState();
    state.learnGlyph(new ResourceLocation("arcane", "fire"), GlyphKnowledge.PARTIAL);
    Inscription spell = named("Forte", g("projectile"), g("damage", "amount", 9.0));
    state.addInscription(spell);
    state.setActiveInscription(spell.id());
    state.logDiscovery("experiment:arcane:fire");

    CompoundTag tag = new CompoundTag();
    state.save(tag);
    GrimoireState loaded = new GrimoireState();
    loaded.load(tag);

    assertEquals(GlyphKnowledge.PARTIAL, loaded.knowledge().get(new ResourceLocation("arcane", "fire")));
    Inscription back = loaded.getInscription(spell.id()).orElseThrow();
    assertEquals("Forte", back.metadata().name());
    assertEquals(9.0, back.glyphs().get(1).parameters().get("amount"), 1e-9);
    assertEquals(Optional.of(spell.id()), loaded.activeInscriptionId());
    assertEquals(List.of("experiment:arcane:fire"), loaded.discoveryLog());
  }

  @Test
  void journalIsCapped() {
    GrimoireState state = new GrimoireState();
    for (int i = 0; i < 200; i++) state.logDiscovery("loot:arcane:fire#" + i);
    assertEquals(60, state.discoveryLog().size());
    assertEquals("loot:arcane:fire#199", state.discoveryLog().get(59), "ficam os mais recentes");
  }

  @Test
  void knownGlyphNeedsToBeIdentifiedToCompose() {
    GrimoireState state = new GrimoireState();
    state.learnGlyph(new ResourceLocation("arcane", "self"), GlyphKnowledge.MASTERED);
    state.learnGlyph(new ResourceLocation("arcane", "heal"), GlyphKnowledge.PARTIAL);
    Inscription spell = named("x", g("self"), g("heal"));
    assertFalse(InscriptionValidator.validate(spell, Optional.of(state)).valid());
    state.learnGlyph(new ResourceLocation("arcane", "heal"), GlyphKnowledge.IDENTIFIED);
    assertTrue(InscriptionValidator.validate(spell, Optional.of(state)).valid());
    state.learnGlyph(new ResourceLocation("arcane", "heal"), GlyphKnowledge.SUSPECTED);
    assertEquals(GlyphKnowledge.IDENTIFIED, state.knowledge().get(new ResourceLocation("arcane", "heal")),
        "o conhecimento nunca regride");
  }
}
