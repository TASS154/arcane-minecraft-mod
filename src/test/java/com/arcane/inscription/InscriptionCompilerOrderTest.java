package com.arcane.inscription;

import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class InscriptionCompilerOrderTest {
  static final Path GLYPH_DIR = Path.of("src/main/resources/data/arcane/glyphs");

  /** Carrega os glifos reais do datapack, como o jogo faria no reload. */
  @BeforeAll
  static void loadGlyphs() throws IOException {
    Map<ResourceLocation, GlyphDefinition> defs = new HashMap<>();
    try (Stream<Path> files = Files.list(GLYPH_DIR)) {
      for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
        String name = file.getFileName().toString().replace(".json", "");
        JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        ResourceLocation id = new ResourceLocation("arcane", name);
        defs.put(id, GlyphRegistry.parse(id, json));
      }
    }
    GlyphRegistry.replaceAll(defs);
  }

  @Test
  void allGlyphDefinitionsParse() {
    assertEquals(16, GlyphRegistry.size(), "todos os JSONs de glifo devem ser validos");
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

  @Test
  void validatorRejectsFormOutOfPlace() {
    Inscription bad = new Inscription(List.of(
        new GlyphInstance(new ResourceLocation("arcane", "damage")),
        new GlyphInstance(new ResourceLocation("arcane", "projectile"))
    ));
    assertFalse(InscriptionValidator.validate(bad).valid());

    Inscription good = new Inscription(List.of(
        new GlyphInstance(new ResourceLocation("arcane", "projectile")),
        new GlyphInstance(new ResourceLocation("arcane", "damage"))
    ));
    assertTrue(InscriptionValidator.validate(good).valid(), InscriptionValidator.validate(good).message().getString());
  }
}
