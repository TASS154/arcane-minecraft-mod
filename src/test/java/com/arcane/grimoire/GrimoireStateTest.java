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

  // ---------------------------------------------------------------- design v2: paginas, recuperar, estudar
  private static GrimoireState veteran() {
    GrimoireState v = new GrimoireState();
    for (String id : List.of("self", "projectile", "damage", "fire", "thunder")) {
      v.learnGlyph(new ResourceLocation("arcane", id), GlyphKnowledge.MASTERED);
    }
    v.addInscription(named("Descarga", g("projectile"), g("thunder")));
    v.addInscription(named("Bola de Fogo", g("projectile"), g("damage"), g("fire")));
    v.addPages(20);
    v.logDiscovery("loot:arcane:thunder");
    return v;
  }

  @Test
  void pagesSetTheSpellCapacityAndTheVisualStage() {
    GrimoireState state = new GrimoireState();
    assertEquals(8, state.pages());
    assertEquals(1, state.visualStage());
    assertEquals(8, state.freePages());
    state.addInscription(named("a", g("self"), g("heal")));
    assertEquals(7, state.freePages());
    state.addPages(40);
    assertEquals(48, state.pages());
    assertEquals(5, state.visualStage(), "48 paginas = paginas em orbita");
    assertEquals(7, GrimoireState.stageFor(100));
    state.addPages(10_000);
    assertEquals(GrimoireState.MAX_PAGES, state.pages(), "ha um teto");
  }

  @Test
  void pagesSurviveSaveAndLoad() {
    GrimoireState a = new GrimoireState();
    a.addPages(30);
    CompoundTag tag = new CompoundTag();
    a.save(tag);
    GrimoireState b = new GrimoireState();
    b.load(tag);
    assertEquals(38, b.pages());
    assertEquals(GrimoireState.stageFor(38), b.visualStage());
  }

  @Test
  void ownerRecoveringTheGrimoireKeepsEverythingAndDoesNotDuplicateTheStarterSpell() {
    GrimoireState original = veteran();
    original.addInscription(GrimoireState.createDefault().spellbook().values().iterator().next()); // "Faisca"
    CompoundTag snapshot = new CompoundTag();
    original.save(snapshot);

    GrimoireState provisional = GrimoireState.createDefault(); // o que o jogador tem depois de morrer
    provisional.learnGlyph(new ResourceLocation("arcane", "heal"), GlyphKnowledge.PARTIAL); // algo que aprendeu nesse meio tempo
    int before = provisional.spellbook().size();

    GrimoireTransfer.restore(provisional, snapshot);

    assertEquals(GlyphKnowledge.MASTERED, provisional.knowledge().get(new ResourceLocation("arcane", "thunder")));
    assertEquals(GlyphKnowledge.PARTIAL, provisional.knowledge().get(new ResourceLocation("arcane", "heal")), "o que aprendeu depois fica");
    assertTrue(provisional.findByName("Descarga").isPresent());
    assertTrue(provisional.findByName("Bola de Fogo").isPresent());
    assertEquals(before + 2, provisional.spellbook().size(), "so as 2 magias novas; a Faisca nao duplica");
    assertEquals(28, provisional.pages(), "as paginas do original");
    assertTrue(provisional.discoveryLog().contains("loot:arcane:thunder"));
  }

  @Test
  void studyingSomeoneElsesGrimoireCopiesFormulasButOnlyMakesYouSuspectTheGlyphs() {
    GrimoireState thief = new GrimoireState();
    thief.learnGlyph(new ResourceLocation("arcane", "projectile"), GlyphKnowledge.MASTERED);
    thief.learnGlyph(new ResourceLocation("arcane", "damage"), GlyphKnowledge.MASTERED);

    GrimoireTransfer.StudyResult result = GrimoireTransfer.study(thief, veteran());

    assertEquals(2, result.added());
    assertTrue(thief.findByName("Descarga").isPresent(), "a formula e copiada");
    assertEquals(GlyphKnowledge.SUSPECTED, thief.knowledge().get(new ResourceLocation("arcane", "thunder")), "mas Trovao so e suspeito");
    assertEquals(GlyphKnowledge.SUSPECTED, thief.knowledge().get(new ResourceLocation("arcane", "fire")));
    assertEquals(GlyphKnowledge.MASTERED, thief.knowledge().get(new ResourceLocation("arcane", "damage")), "o que ja sabia nao regride");
    assertEquals(2, result.suspected());

    var descarga = thief.findByName("Descarga").orElseThrow();
    assertFalse(InscriptionValidator.validate(descarga, Optional.of(thief)).valid(),
        "copiar a formula nao permite lancar: falta identificar os glifos");
  }

  @Test
  void studyingTheSameSpellTwiceDoesNothing() {
    GrimoireState thief = new GrimoireState();
    GrimoireTransfer.study(thief, veteran());
    int size = thief.spellbook().size();
    GrimoireTransfer.StudyResult again = GrimoireTransfer.study(thief, veteran());
    assertEquals(0, again.added());
    assertEquals(2, again.skippedKnown());
    assertEquals(size, thief.spellbook().size());
  }

  @Test
  void withoutFreePagesTheFormulaIsNotCopiedButTheGlyphsStillGiveClues() {
    GrimoireState full = new GrimoireState();
    for (int i = 0; i < GrimoireState.DEFAULT_PAGES; i++) full.addInscription(named("m" + i, g("self"), g("heal", "amount", 1.0 + i)));
    assertEquals(0, full.freePages());

    GrimoireTransfer.StudyResult result = GrimoireTransfer.studySpell(full, veteran().findByName("Descarga").orElseThrow());

    assertEquals(0, result.added());
    assertEquals(1, result.skippedNoPages());
    assertEquals(GlyphKnowledge.SUSPECTED, full.knowledge().get(new ResourceLocation("arcane", "thunder")));
  }
}
