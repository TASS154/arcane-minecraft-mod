package com.arcane.network;

import com.arcane.TestGlyphs;
import com.arcane.client.ClientArcaneData;
import com.arcane.client.ClientArcaneData.GlyphEntry;
import com.arcane.grimoire.GlyphKnowledge;
import com.arcane.grimoire.GrimoireState;
import com.arcane.inscription.CompiledPlanCache;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionMetadata;
import com.arcane.inscription.InscriptionSourceKind;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.arcane.TestGlyphs.g;
import static org.junit.jupiter.api.Assertions.*;

/** O que o servidor envia e o que o cliente reconstroi: precisam concordar, sobretudo no custo. */
class SyncTest {
  @BeforeAll
  static void loadGlyphs() {
    TestGlyphs.load();
  }

  private static void applyKnowledge(GrimoireState state) {
    CompoundTag tag = new CompoundTag();
    tag.putFloat("mana", 50f);
    tag.putFloat("maxMana", 100f);
    tag.put("full", ArcaneSync.buildKnowledge(state));
    ClientArcaneData.reset();
    ClientArcaneData.apply(tag);
  }

  @Test
  void clientCostPreviewMatchesTheServerCompiler() {
    GrimoireState state = new GrimoireState();
    for (String id : List.of("projectile", "damage", "fire", "slow", "area", "explode", "heal")) {
      state.learnGlyph(new ResourceLocation("arcane", id), GlyphKnowledge.IDENTIFIED);
    }
    List<com.arcane.inscription.GlyphInstance> glyphs = List.of(
        g("projectile"), g("damage", "amount", 12.0), g("fire", "duration", 120.0), g("slow", "level", 2.0), g("explode", "radius", 4.0));
    Inscription spell = new Inscription(glyphs, new InscriptionMetadata("teste", new ResourceLocation("arcane", "common"),
        new ResourceLocation("arcane", "arcane_pen"), 1.0, Optional.empty(), 0L, InscriptionSourceKind.WORKBENCH));
    state.addInscription(spell);
    state.setActiveInscription(spell.id());

    applyKnowledge(state);
    var synced = ClientArcaneData.spells().get(0);
    int serverCost = CompiledPlanCache.getOrCompile(spell).baseManaCost();
    assertEquals(serverCost, synced.cost());
    assertEquals(serverCost, ClientArcaneData.costOf(synced.glyphs()), 1e-9,
        "a previa da bancada deve dar o mesmo custo que o servidor cobra");
  }

  @Test
  void parametersTravelToTheClient() {
    GrimoireState state = new GrimoireState();
    state.learnGlyph(new ResourceLocation("arcane", "damage"), GlyphKnowledge.IDENTIFIED);
    state.learnGlyph(new ResourceLocation("arcane", "slow"), GlyphKnowledge.PARTIAL);
    state.learnGlyph(new ResourceLocation("arcane", "fire"), GlyphKnowledge.UNKNOWN);
    Inscription spell = new Inscription(List.of(g("self"), g("damage", "amount", 7.0)));
    state.addInscription(spell);
    applyKnowledge(state);

    var damage = ClientArcaneData.glyph(new ResourceLocation("arcane", "damage")).orElseThrow();
    assertEquals("amount", damage.primary().orElseThrow().key());
    assertEquals(1.0, damage.primary().get().min());
    assertEquals(20.0, damage.primary().get().max());
    assertTrue(damage.usable());
    assertFalse(ClientArcaneData.glyph(new ResourceLocation("arcane", "slow")).orElseThrow().usable());
    assertEquals("duration", ClientArcaneData.glyph(new ResourceLocation("arcane", "slow")).orElseThrow().primary().orElseThrow().key(),
        "duracao e o parametro principal do Lentidao");
    assertEquals(7.0, ClientArcaneData.spells().get(0).glyphs().get(1).params().get("amount"), 1e-9);
    assertEquals(73, ClientArcaneData.totalGlyphs());
  }

  @Test
  void adjustingStepsAreSensible() {
    var duration = new ClientArcaneData.ParamDef("duration", 100, 20, 600, true);
    assertEquals(29.0, duration.step(), 1e-9);
    assertEquals(600.0, duration.clamp(99999), 1e-9);
    var amount = new ClientArcaneData.ParamDef("amount", 4.0, 1.0, 20.0, false);
    assertEquals(1.0, amount.step(), 1e-9);
    assertEquals(4.5, amount.clamp(4.4999), 1e-9, "arredonda a duas casas");
  }
}
