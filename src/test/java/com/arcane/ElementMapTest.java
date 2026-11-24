package com.arcane;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Todo glifo de efeito precisa de um elemento visual em data/arcane/elements/effects.json, e o elemento precisa existir. */
class ElementMapTest {
  @Test
  void everyEffectGlyphHasAVisualElement() throws IOException {
    JsonObject map;
    try (Reader r = Files.newBufferedReader(Path.of("src/main/resources/data/arcane/elements/effects.json"), StandardCharsets.UTF_8)) {
      map = JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("effects");
    }
    List<String> missing = new ArrayList<>();
    try (Stream<Path> files = Files.list(Path.of("src/main/resources/data/arcane/glyphs"))) {
      for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
        JsonObject g;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
          g = JsonParser.parseReader(r).getAsJsonObject();
        }
        if (!"effect".equals(g.get("category").getAsString())) continue;
        String id = file.getFileName().toString().replace(".json", "");
        if (!map.has(id)) missing.add(id);
      }
    }
    assertTrue(missing.isEmpty(), "efeitos sem elemento visual: " + missing);
    for (var e : map.entrySet()) {
      String name = e.getValue().getAsString().toUpperCase(Locale.ROOT);
      boolean known = false;
      for (var el : com.arcane.execution.SpellElement.values()) known |= el.name().equals(name);
      assertTrue(known, e.getKey() + ": elemento desconhecido " + name);
    }
  }
}
