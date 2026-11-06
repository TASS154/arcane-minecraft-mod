package com.arcane.inscription;

import com.arcane.TestGlyphs;
import com.arcane.glyph.GlyphRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.arcane.TestGlyphs.g;
import static org.junit.jupiter.api.Assertions.*;

class InscriptionCompilerOrderTest {
  @BeforeAll
  static void loadGlyphs() {
    TestGlyphs.load();
  }

  @Test
  void allGlyphDefinitionsParse() {
    assertEquals(73, GlyphRegistry.size(), "todos os JSONs de glifo devem ser validos");
  }

  @Test
  void projectileDamageFireDiffersFromProjectileFireDamage() {
    Inscription a = new Inscription(List.of(g("projectile"), g("damage", "amount", 5.0), g("fire", "duration", 60.0)));
    Inscription b = new Inscription(List.of(g("projectile"), g("fire", "duration", 60.0), g("damage", "amount", 5.0)));

    CompiledExecutionPlan planA = InscriptionCompiler.compile(a);
    CompiledExecutionPlan planB = InscriptionCompiler.compile(b);

    assertNotEquals(planA.allEffectsInOrder(), planB.allEffectsInOrder());
    assertEquals(new ResourceLocation("arcane", "damage"), planA.allEffectsInOrder().get(0).glyphId());
    assertEquals(new ResourceLocation("arcane", "fire"), planA.allEffectsInOrder().get(1).glyphId());
    assertEquals(new ResourceLocation("arcane", "fire"), planB.allEffectsInOrder().get(0).glyphId());
    assertEquals(new ResourceLocation("arcane", "damage"), planB.allEffectsInOrder().get(1).glyphId());
  }

  @Test
  void validatorRejectsFormOutOfPlace() {
    Inscription bad = new Inscription(List.of(g("damage"), g("projectile")));
    assertFalse(InscriptionValidator.validate(bad).valid());

    Inscription good = new Inscription(List.of(g("projectile"), g("damage")));
    assertTrue(InscriptionValidator.validate(good).valid(), InscriptionValidator.validate(good).message().getString());
  }

  @Test
  void validatorRejectsSecondForm() {
    Inscription twoForms = new Inscription(List.of(g("self"), g("damage"), g("projectile")));
    assertFalse(InscriptionValidator.validate(twoForms).valid());
  }
}
