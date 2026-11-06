package com.arcane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** O Minecraft recusa receita modelada com linhas de larguras diferentes e as que usam simbolo sem ingrediente. */
class RecipeIntegrityTest {
  @Test
  void shapedRecipesHaveRectangularPatternsWithAllKeysDefined() throws IOException {
    Path dir = Path.of("src/main/resources/data/arcane/recipes");
    try (Stream<Path> files = Files.list(dir)) {
      for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
        JsonObject json;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
          json = JsonParser.parseReader(r).getAsJsonObject();
        }
        if (!json.has("pattern")) continue;
        var rows = json.getAsJsonArray("pattern");
        int width = rows.get(0).getAsString().length();
        var keys = json.getAsJsonObject("key");
        for (var row : rows) {
          assertEquals(width, row.getAsString().length(), file.getFileName() + ": linhas com larguras diferentes");
          for (char c : row.getAsString().toCharArray()) {
            if (c != ' ') assertTrue(keys.has(String.valueOf(c)), file.getFileName() + ": simbolo sem ingrediente: " + c);
          }
        }
      }
    }
  }
}
