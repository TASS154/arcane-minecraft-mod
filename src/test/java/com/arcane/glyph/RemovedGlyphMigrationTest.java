package com.arcane.glyph;

import com.arcane.TestGlyphs;
import com.arcane.grimoire.GlyphKnowledge;
import com.arcane.grimoire.GrimoireState;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionValidator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Glifos cortados em 26 nov (Planar, Atraso, Ferido, Inverter, efeito Chuva, Carinho): uma inscricao gravada antes do
 * corte carrega sem erro e vira a magia definida em {@link GlyphAliases} (troca de id ou glifo retirado).
 */
class RemovedGlyphMigrationTest {
  @BeforeAll
  static void loadGlyphs() {
    TestGlyphs.load();
  }

  private static ResourceLocation id(String s) {
    return new ResourceLocation("arcane", s);
  }

  /** Monta o NBT como o disco guardava antes (ids crus, sem passar pelo codec atual). */
  private static Tag disk(Object... idsAndParams) {
    ListTag list = new ListTag();
    for (Object o : idsAndParams) {
      CompoundTag g = new CompoundTag();
      if (o instanceof String s) {
        g.putString("id", "arcane:" + s);
      } else {
        Object[] a = (Object[]) o;
        g.putString("id", "arcane:" + a[0]);
        CompoundTag p = new CompoundTag();
        for (int i = 1; i < a.length; i += 2) p.putDouble((String) a[i], (Double) a[i + 1]);
        g.put("p", p);
      }
      list.add(g);
    }
    CompoundTag root = new CompoundTag();
    root.putInt("v", 1);
    root.put("glyphs", list);
    return root;
  }

  private static Inscription load(Tag tag) {
    return Inscription.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new AssertionError("nao carregou"));
  }

  private static List<String> ids(Inscription i) {
    return i.glyphs().stream().map(g -> g.glyphId().getPath()).toList();
  }

  @Test
  void removedIdsAreNotRegisteredAnymore() {
    for (String gone : List.of("glide", "delay", "wounded", "invert", "rain", "pet")) {
      assertTrue(GlyphRegistry.get(id(gone)).isEmpty(), gone + " devia ter saido do registro");
      assertTrue(GlyphAliases.isAlias(id(gone)), gone + " precisa de alias");
    }
    // Inversao da Gravidade (original) nao e o Inverter
    assertTrue(GlyphRegistry.get(id("gravity_inversion")).isPresent(), "Inversao da Gravidade continua");
  }

  @Test
  void glideBecomesFeatherKeepingTheDuration() {
    Inscription i = load(disk("self", new Object[]{"glide", "duration", 300.0}));
    assertEquals(List.of("self", "feather"), ids(i));
    assertEquals(300.0, i.glyphs().get(1).param("duration", 0));
    assertTrue(InscriptionValidator.validate(i).valid());
  }

  @Test
  void delayBecomesAfterSecondsKeepingTheSeconds() {
    Inscription i = load(disk("self", "heal", new Object[]{"delay", "seconds", 3.0}, "damage"));
    assertEquals(List.of("self", "heal", "after_seconds", "damage"), ids(i));
    assertEquals(3.0, i.glyphs().get(2).param("seconds", 0));
    assertTrue(InscriptionValidator.validate(i).valid());
  }

  @Test
  void woundedAndInvertAreDroppedAndTheRestKeepsItsOrder() {
    Inscription i = load(disk(new Object[]{"aura", "radius", 5.0}, "wounded", "hostile", "damage", "invert", "push"));
    assertEquals(List.of("aura", "hostile", "damage", "push"), ids(i));
    assertEquals(5.0, i.glyphs().get(0).param("radius", 0), "os outros glifos nao mudam");
    assertTrue(InscriptionValidator.validate(i).valid());
  }

  @Test
  void rainEffectBecomesWetAndLosesTheRadius() {
    Inscription i = load(disk("self", new Object[]{"rain", "radius", 6.0, "duration", 200.0}));
    assertEquals(List.of("self", "wet"), ids(i));
    assertEquals(200.0, i.glyphs().get(1).param("duration", 0));
    assertFalse(i.glyphs().get(1).parameters().containsKey("radius"), "Molhar nao tem raio");
    assertTrue(InscriptionValidator.validate(i).valid());
  }

  @Test
  void petBecomesHealWithTheSameFourHealth() {
    Inscription i = load(disk("touch", "pet"));
    assertEquals(List.of("touch", "heal"), ids(i));
    assertEquals(4.0, i.glyphs().get(1).param("amount", 0));
  }

  @Test
  void savingWritesTheNewIdsOnly() {
    Inscription i = load(disk("self", "glide", "invert"));
    Tag again = Inscription.CODEC.encodeStart(NbtOps.INSTANCE, i).result().orElseThrow();
    String text = again.toString();
    assertFalse(text.contains("glide") || text.contains("invert"), text);
    assertEquals(ids(i), ids(load(again)));
  }

  @Test
  void knowledgeOfADroppedGlyphIsDiscardedAndOfAFusedOneCarriesOver() {
    CompoundTag disk = new CompoundTag();
    CompoundTag k = new CompoundTag();
    k.putInt("arcane:invert", GlyphKnowledge.MASTERED.ordinal());
    k.putInt("arcane:glide", GlyphKnowledge.MASTERED.ordinal());
    disk.put("knowledge", k);
    GrimoireState s = new GrimoireState();
    s.load(disk);
    assertTrue(s.knowsGlyph(id("feather"), GlyphKnowledge.MASTERED), "quem conhecia Planar conhece Pena");
    assertFalse(s.knowsGlyph(GlyphAliases.DROPPED, GlyphKnowledge.SUSPECTED), "o Inverter some do conhecimento");
  }

  @Test
  void anOldSpellMadeOnlyOfDroppedGlyphsAfterTheFormStillLoads() {
    Inscription i = load(disk("self", "invert"));
    assertEquals(List.of("self"), ids(i), "carrega sem crash; a validacao decide se a magia vazia vale");
    assertNotNull(InscriptionValidator.validate(i));
  }

  @Test
  void aliasesNeverPointAtARemovedOrMissingGlyph() {
    for (var e : GlyphAliases.all().entrySet()) {
      ResourceLocation to = GlyphAliases.resolve(e.getKey());
      if (to.equals(GlyphAliases.DROPPED)) continue;
      assertTrue(GlyphRegistry.get(to).isPresent(), e.getKey() + " aponta para " + to + ", que nao existe");
    }
  }
}
