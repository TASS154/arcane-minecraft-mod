package com.arcane.inscription;

import com.arcane.glyph.GlyphRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InscriptionCompilerOrderTest {
  @BeforeAll
  static void loadGlyphs() {
    // Glyphs loaded at runtime via datapack; compiler tests use IDs directly.
  }

  @Test
  void projectileDamageFireDiffersFromProjectileFireDamage() {
    Inscription a = new Inscription(List.of(
        new GlyphInstance(new ResourceLocation("arcane", "projectile")),
        new GlyphInstance(new ResourceLocation("arcane", "damage"), Map.of("amount", 5.0)),
        new GlyphInstance(new ResourceLocation("arcane", "fire"), Map.of("duration", 60.0))
    ));
    Inscription b = new Inscription(List.of(
        new GlyphInstance(new ResourceLocation("arcane", "projectile")),
        new GlyphInstance(new ResourceLocation("arcane", "fire"), Map.of("duration", 60.0)),
        new GlyphInstance(new ResourceLocation("arcane", "damage"), Map.of("amount", 5.0))
    ));

  CompiledExecutionPlan planA = InscriptionCompiler.compile(a);
  CompiledExecutionPlan planB = InscriptionCompiler.compile(b);

    assertNotEquals(planA.allEffectsInOrder(), planB.allEffectsInOrder());
    assertEquals(new ResourceLocation("arcane", "damage"), planA.allEffectsInOrder().get(0).glyphId());
    assertEquals(new ResourceLocation("arcane", "fire"), planA.allEffectsInOrder().get(1).glyphId());
    assertEquals(new ResourceLocation("arcane", "fire"), planB.allEffectsInOrder().get(0).glyphId());
    assertEquals(new ResourceLocation("arcane", "damage"), planB.allEffectsInOrder().get(1).glyphId());
  }
}
