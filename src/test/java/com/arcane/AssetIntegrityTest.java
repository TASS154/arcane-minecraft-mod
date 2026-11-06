package com.arcane;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Confere que nenhum asset referenciado esta faltando (evita a textura roxa e preta no jogo):
 * modelos -> texturas, blockstates -> modelos, sounds.json -> .ogg, glifos -> icone e traducao.
 */
class AssetIntegrityTest {
  static final Path ASSETS = Path.of("src/main/resources/assets/arcane");
  static final Path DATA = Path.of("src/main/resources/data/arcane");
  static final List<String> ITEMS = List.of("arcane_workbench", "mana_crystal", "arcane_pen", "common_ink", "blood_ink",
      "invisible_ink", "glyph_note", "arcane_mask", "insulating_cloak", "glyph_projector", "symmetry_mirror", "npc_mage_spawn_egg", "grimoire", "arcane_page", "arcane_altar", "arcane_chalk", "arcane_compass");

  static JsonObject read(Path path) throws IOException {
    return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
  }

  /** "arcane:block/x" -> assets/arcane/textures/block/x.png; texturas vanilla sao ignoradas. */
  static void assertTextureExists(String ref, String context) {
    if (ref.startsWith("#") || !ref.startsWith("arcane:")) return;
    Path png = ASSETS.resolve("textures").resolve(ref.substring("arcane:".length()) + ".png");
    assertTrue(Files.exists(png), context + " referencia textura inexistente: " + png);
  }

  static void assertModelExists(String ref, String context) {
    if (!ref.startsWith("arcane:")) return;
    Path json = ASSETS.resolve("models").resolve(ref.substring("arcane:".length()) + ".json");
    assertTrue(Files.exists(json), context + " referencia modelo inexistente: " + json);
  }

  @Test
  void everyItemHasModelAndEveryModelTextureExists() throws IOException {
    for (String item : ITEMS) {
      Path model = ASSETS.resolve("models/item/" + item + ".json");
      assertTrue(Files.exists(model), "item sem modelo: " + item);
    }
    try (Stream<Path> models = Files.walk(ASSETS.resolve("models"))) {
      for (Path model : models.filter(p -> p.toString().endsWith(".json")).toList()) {
        JsonObject json = read(model);
        if (json.has("parent")) assertModelExists(json.get("parent").getAsString(), model.toString());
        if (json.has("textures")) {
          for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("textures").entrySet()) {
            assertTextureExists(e.getValue().getAsString(), model.toString());
          }
        }
      }
    }
  }

  @Test
  void blockstatesPointToModels() throws IOException {
    try (Stream<Path> states = Files.list(ASSETS.resolve("blockstates"))) {
      for (Path state : states.toList()) {
        for (Map.Entry<String, JsonElement> v : read(state).getAsJsonObject("variants").entrySet()) {
          assertModelExists(v.getValue().getAsJsonObject().get("model").getAsString(), state.toString());
        }
      }
    }
  }

  @Test
  void soundsExist() throws IOException {
    for (Map.Entry<String, JsonElement> e : read(ASSETS.resolve("sounds.json")).entrySet()) {
      for (JsonElement s : e.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
        String name = s.getAsString().substring("arcane:".length());
        assertTrue(Files.exists(ASSETS.resolve("sounds/" + name + ".ogg")), "som faltando: " + name);
      }
    }
  }

  @Test
  void particleTexturesExist() throws IOException {
    try (Stream<Path> files = Files.list(ASSETS.resolve("particles"))) {
      List<Path> defs = files.filter(f -> f.toString().endsWith(".json")).toList();
      assertTrue(defs.size() >= 8, "particulas por elemento faltando");
      for (Path def : defs) {
        for (JsonElement t : read(def).getAsJsonArray("textures")) {
          String name = t.getAsString().substring("arcane:".length());
          assertTrue(Files.exists(ASSETS.resolve("textures/particle/" + name + ".png")), "particula sem textura: " + name);
        }
      }
    }
  }

  @Test
  void glyphsHaveIconsAndTranslations() throws IOException {
    JsonObject pt = read(ASSETS.resolve("lang/pt_br.json"));
    JsonObject en = read(ASSETS.resolve("lang/en_us.json"));
    List<String> missing = new ArrayList<>();
    try (Stream<Path> glyphs = Files.list(DATA.resolve("glyphs"))) {
      for (Path glyph : glyphs.toList()) {
        String name = glyph.getFileName().toString().replace(".json", "");
        if (!Files.exists(ASSETS.resolve("textures/gui/glyphs/" + name + ".png"))) missing.add("icone " + name);
        for (String key : List.of("glyph.arcane." + name, "glyph.arcane." + name + ".desc")) {
          if (!pt.has(key)) missing.add("pt_br " + key);
          if (!en.has(key)) missing.add("en_us " + key);
        }
      }
    }
    assertTrue(missing.isEmpty(), "faltando: " + missing);
  }

  @Test
  void languagesHaveTheSameKeys() throws IOException {
    JsonObject pt = read(ASSETS.resolve("lang/pt_br.json"));
    JsonObject en = read(ASSETS.resolve("lang/en_us.json"));
    assertEquals(pt.keySet(), en.keySet());
    for (String item : ITEMS) {
      String key = List.of("arcane_workbench", "mana_crystal", "arcane_altar").contains(item) ? "block.arcane." + item : "item.arcane." + item;
      assertTrue(pt.has(key), "item sem nome traduzido: " + key);
    }
  }

  @Test
  void guiTexturesExist() {
    for (String gui : List.of("gui/arcane_workbench.png", "gui/mana_bar.png", "gui/grimoire.png", "entity/grimoire.png",
        "entity/arcane_projectile.png", "models/armor/arcane_weave_layer_1.png", "gui/glyphs/unknown.png")) {
      assertTrue(Files.exists(ASSETS.resolve("textures/" + gui)), "faltando: " + gui);
    }
  }

  @Test
  void everyMageVariantHasASkinAndEveryElementAName() throws IOException {
    for (int i = 0; i < 18; i++) {
      assertTrue(Files.exists(ASSETS.resolve(String.format("textures/entity/npc_mage/mage_%02d.png", i))), "skin faltando: " + i);
    }
    JsonObject pt = read(ASSETS.resolve("lang/pt_br.json"));
    for (String e : List.of("fire", "frost", "storm", "venom", "blast", "gale")) {
      assertTrue(pt.has("entity.arcane.npc_mage." + e), "nome do mago: " + e);
    }
  }

  @Test
  void everyTranslationKeyUsedInGuiCodeExists() throws IOException {
    JsonObject pt = read(ASSETS.resolve("lang/pt_br.json"));
    List<String> missing = new ArrayList<>();
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("translatable\\(\"([a-z_.]+)\"");
    try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
      for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
        var m = pattern.matcher(Files.readString(file, StandardCharsets.UTF_8));
        while (m.find()) {
          String key = m.group(1);
          if (key.endsWith(".")) continue;
          if (!pt.has(key)) missing.add(key + "  (" + file.getFileName() + ")");
        }
      }
    }
    assertTrue(missing.isEmpty(), "chaves de traducao faltando no pt_br: " + missing);
  }
}
