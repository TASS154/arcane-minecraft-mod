package com.arcane.glyph;

import com.arcane.TestGlyphs;
import com.arcane.grimoire.GlyphKnowledge;
import com.arcane.grimoire.GrimoireState;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionValidator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Migracao de ids antigos: grimorios e inscricoes salvos antes de um corte ou fusao continuam validos. */
class GlyphAliasesTest {
  private static final ResourceLocation OLD = new ResourceLocation("arcane", "legacy_mend");
  private static final ResourceLocation OLDER = new ResourceLocation("arcane", "legacy_mend_v0");
  private static final ResourceLocation HEAL = new ResourceLocation("arcane", "heal");
  private static final ResourceLocation SELF = new ResourceLocation("arcane", "self");

  @BeforeAll
  static void loadGlyphs() {
    TestGlyphs.load();
  }

  @AfterEach
  void cleanup() {
    GlyphAliases.unregister(OLD);
    GlyphAliases.unregister(OLDER);
  }

  /** Grava a inscricao como estava no disco antes do corte (o codec grava o id como esta). */
  private static Tag saved(GlyphInstance... glyphs) {
    return Inscription.CODEC.encodeStart(NbtOps.INSTANCE, new Inscription(List.of(glyphs))).result().orElseThrow();
  }

  private static Inscription load(Tag tag) {
    return Inscription.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow();
  }

  @Test
  void savedInscriptionWithOldIdLoadsAsTheNewGlyphAndStaysValid() {
    Tag disk = saved(new GlyphInstance(SELF), new GlyphInstance(OLD, Map.of("amount", 6.0)));
    GlyphAliases.register(OLD, HEAL, Map.of());

    Inscription loaded = load(disk);
    assertEquals(HEAL, loaded.glyphs().get(1).glyphId());
    assertEquals(6.0, loaded.glyphs().get(1).param("amount", 0), "o parametro escrito continua");
    assertTrue(InscriptionValidator.validate(loaded).valid(), "a magia antiga continua valida");
  }

  @Test
  void withoutTheAliasTheOldSpellIsRejected() {
    Inscription loaded = load(saved(new GlyphInstance(SELF), new GlyphInstance(OLD)));
    assertFalse(InscriptionValidator.validate(loaded).valid(), "corte sem alias quebra as magias salvas");
  }

  @Test
  void fusionFixesTheKindButNeverOverwritesWhatWasWritten() {
    GlyphAliases.register(OLD, HEAL, Map.of("kind", 1.0, "amount", 2.0));
    Inscription loaded = load(saved(new GlyphInstance(SELF), new GlyphInstance(OLD, Map.of("amount", 9.0))));
    GlyphInstance g = loaded.glyphs().get(1);
    assertEquals(1.0, g.param("kind", 0), "a fusao fixa o kind do glifo antigo");
    assertEquals(9.0, g.param("amount", 0), "o valor escrito pelo jogador vence o fixado");
  }

  @Test
  void chainsAreFollowedAndSavingWritesTheCurrentId() {
    GlyphAliases.register(OLDER, OLD, Map.of());
    GlyphAliases.register(OLD, HEAL, Map.of());
    assertEquals(HEAL, GlyphAliases.resolve(OLDER));
    Inscription loaded = load(saved(new GlyphInstance(SELF), new GlyphInstance(OLDER)));
    assertEquals(HEAL, loaded.glyphs().get(1).glyphId());
    CompoundTag resaved = (CompoundTag) saved(loaded.glyphs().toArray(GlyphInstance[]::new));
    assertFalse(resaved.toString().contains("legacy_mend"), "regravar ja usa o id atual: " + resaved);
  }

  @Test
  void knowledgeOfTheOldIdCountsForTheNewOneKeepingTheHighestLevel() {
    GrimoireState before = new GrimoireState();
    before.learnGlyph(OLD, GlyphKnowledge.MASTERED);
    before.learnGlyph(HEAL, GlyphKnowledge.SUSPECTED);
    CompoundTag disk = new CompoundTag();
    before.save(disk);

    GlyphAliases.register(OLD, HEAL, Map.of());
    GrimoireState after = new GrimoireState();
    after.load(disk);
    assertTrue(after.knowsGlyph(HEAL, GlyphKnowledge.MASTERED), "o nivel maior (do id antigo) vale para o novo");
    Inscription spell = new Inscription(List.of(new GlyphInstance(SELF), new GlyphInstance(HEAL)));
    after.learnGlyph(SELF, GlyphKnowledge.MASTERED);
    assertTrue(InscriptionValidator.validate(spell, Optional.of(after)).valid());
  }
}
