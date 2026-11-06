package com.arcane.ritual;

import com.arcane.TestGlyphs;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.inscription.Inscription;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.arcane.TestGlyphs.g;
import static org.junit.jupiter.api.Assertions.*;

class RitualTest {
  @BeforeAll
  static void loadGlyphs() {
    TestGlyphs.load();
  }

  @AfterEach
  void resetSettings() {
    RitualSettings.reset();
  }

  // ---------------------------------------------------------------- molde do circulo
  @Test
  void templateIsSymmetricUnderQuarterTurnsAndExcludesTheAltar() {
    for (int radius : RitualTemplate.RADII) {
      Set<RitualTemplate.Cell> cells = RitualTemplate.cells(radius);
      assertFalse(cells.contains(new RitualTemplate.Cell(0, 0)), "o centro e o altar");
      for (RitualTemplate.Cell c : cells) {
        assertTrue(cells.contains(new RitualTemplate.Cell(-c.dz(), c.dx())), "girar 90 graus deve cair em outra celula: " + c + " r=" + radius);
        assertTrue(cells.contains(new RitualTemplate.Cell(c.dx(), -c.dz())), "espelho vertical: " + c);
      }
    }
  }

  @Test
  void templateHasTheOuterRingAndTheCardinalSpokes() {
    for (int radius : RitualTemplate.RADII) {
      Set<RitualTemplate.Cell> cells = RitualTemplate.cells(radius);
      assertTrue(cells.contains(new RitualTemplate.Cell(radius, 0)));
      assertTrue(cells.contains(new RitualTemplate.Cell(0, -radius)));
      assertTrue(cells.contains(new RitualTemplate.Cell(1, 0)), "o raio cardeal comeca colado no altar");
      for (RitualTemplate.Cell c : cells) {
        assertTrue(Math.hypot(c.dx(), c.dz()) <= radius + 0.5, "nada fora do anel externo: " + c);
      }
    }
  }

  @Test
  void biggerCirclesNeedMoreTracesAndMoreInk() {
    int small = RitualTemplate.cells(4).size();
    int medium = RitualTemplate.cells(8).size();
    int large = RitualTemplate.cells(16).size();
    assertTrue(small < medium && medium < large, small + " < " + medium + " < " + large);
    assertTrue(large < 400, "um circulo maior ainda cabe em poucas dezenas de tinta: " + large);
  }

  // ---------------------------------------------------------------- niveis
  @Test
  void levelComesFromTheCircleRadius() {
    assertEquals(RitualLevel.NONE, RitualLevel.forRadius(0));
    assertEquals(RitualLevel.NONE, RitualLevel.forRadius(3));
    assertEquals(RitualLevel.MINOR, RitualLevel.forRadius(4));
    assertEquals(RitualLevel.MEDIUM, RitualLevel.forRadius(8));
    assertEquals(RitualLevel.MAJOR, RitualLevel.forRadius(16));
    assertEquals(RitualLevel.LEGENDARY, RitualLevel.of(4));
  }

  @Test
  void powerGrowsWithTheLevelAndFollowsTheServerConfig() {
    assertEquals(1.5, RitualLevel.MINOR.power(), 1e-9);
    assertEquals(3.0, RitualLevel.MEDIUM.power(), 1e-9);
    assertEquals(10.0, RitualLevel.MAJOR.power(), 1e-9);
    assertTrue(RitualLevel.MAJOR.areaScale() > RitualLevel.MEDIUM.areaScale() && RitualLevel.MEDIUM.areaScale() > RitualLevel.MINOR.areaScale());
    RitualSettings.set(2.0, 4.0, 20.0, 0.9);
    assertEquals(20.0, RitualLevel.MAJOR.power(), 1e-9);
    assertEquals(0.9, RitualSettings.minCompleteness(), 1e-9);
  }

  // ---------------------------------------------------------------- o que a magia exige
  @Test
  void ordinarySpellsNeedNoRitual() {
    RitualRequirements req = RitualRequirements.of(new Inscription(List.of(g("projectile"), g("damage"), g("fire"))));
    assertFalse(req.needsRitual());
    assertEquals(0, req.level());
    assertTrue(req.components().isEmpty());
  }

  @Test
  void ritualGlyphsDeclareTheirLevelAndComponentsInData() {
    assertEquals(1, GlyphRegistry.getOrThrow(new ResourceLocation("arcane", "summon_wolves")).ritual());
    assertEquals(2, GlyphRegistry.getOrThrow(new ResourceLocation("arcane", "summon_golem")).ritual());
    assertEquals(3, GlyphRegistry.getOrThrow(new ResourceLocation("arcane", "warp")).ritual());
    assertEquals(4, GlyphRegistry.getOrThrow(new ResourceLocation("arcane", "weather")).ritual());
    var bones = GlyphRegistry.getOrThrow(new ResourceLocation("arcane", "summon_wolves")).components();
    assertEquals(1, bones.size());
    assertEquals(new ResourceLocation("minecraft", "bone"), bones.get(0).item());
    assertEquals(6, bones.get(0).count());
  }

  @Test
  void requirementsTakeTheHighestLevelAndSumTheComponents() {
    RitualRequirements req = RitualRequirements.of(new Inscription(List.of(g("summon_wolves"), g("damage"))));
    assertEquals(1, req.level());
    assertTrue(req.needsRitual());

    RitualRequirements both = RitualRequirements.of(new Inscription(List.of(g("summon_golem"), g("warp"))));
    assertEquals(3, both.level(), "vale o maior nivel");
    assertEquals(1, both.components().get(new ResourceLocation("minecraft", "crying_obsidian")));
    assertEquals(4, both.components().get(new ResourceLocation("minecraft", "ender_pearl")));
    assertEquals(2, both.components().get(new ResourceLocation("minecraft", "iron_block")));
  }

  @Test
  void everyRitualGlyphIsRareAndHasAFamily() {
    for (String id : List.of("summon_wolves", "summon_golem", "warp", "weather")) {
      var def = GlyphRegistry.getOrThrow(new ResourceLocation("arcane", id));
      assertTrue(def.tier() >= 2, id + " deve ser raro");
      assertFalse(def.families().isEmpty());
    }
  }
}
