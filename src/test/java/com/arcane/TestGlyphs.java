package com.arcane;

import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.inscription.GlyphInstance;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/** Carrega os glifos reais do datapack (como o jogo faz no reload) para os testes unitarios. */
public final class TestGlyphs {
  public static final Path GLYPH_DIR = Path.of("src/main/resources/data/arcane/glyphs");
  private static boolean loaded;

  private TestGlyphs() {}

  public static synchronized void load() {
    if (loaded) return;
    Map<ResourceLocation, GlyphDefinition> defs = new HashMap<>();
    try (Stream<Path> files = Files.list(GLYPH_DIR)) {
      for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
        String name = file.getFileName().toString().replace(".json", "");
        JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        ResourceLocation id = new ResourceLocation("arcane", name);
        defs.put(id, GlyphRegistry.parse(id, json));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    GlyphRegistry.replaceAll(defs);
    loaded = true;
  }

  public static GlyphInstance g(String id) {
    return new GlyphInstance(new ResourceLocation("arcane", id));
  }

  public static GlyphInstance g(String id, String key, double value) {
    return new GlyphInstance(new ResourceLocation("arcane", id), Map.of(key, value));
  }
}
