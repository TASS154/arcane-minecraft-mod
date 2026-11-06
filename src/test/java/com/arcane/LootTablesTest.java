package com.arcane;

import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Dungeons por nivel e especialidade: o loot de cada estrutura respeita a raridade e favorece sua familia. */
class LootTablesTest {
  static final Path DATA = Path.of("src/main/resources/data/arcane");

  @BeforeAll
  static void loadGlyphs() {
    TestGlyphs.load();
  }

  static JsonObject read(Path path) throws IOException {
    return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
  }

  /** glifo -> peso, a partir das notas da primeira pool da tabela da estrutura. */
  static Map<String, Integer> noteWeights(String key) throws IOException {
    String table = read(DATA.resolve("glyph_loot_targets.json")).get(key).getAsString();
    JsonObject json = read(DATA.resolve("loot_tables/" + table + ".json"));
    Map<String, Integer> weights = new HashMap<>();
    for (JsonElement e : json.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")) {
      JsonObject entry = e.getAsJsonObject();
      String nbt = entry.getAsJsonArray("functions").get(0).getAsJsonObject().get("tag").getAsString();
      String glyph = nbt.substring(nbt.indexOf("\"") + 1, nbt.lastIndexOf("\""));
      weights.put(glyph, entry.get("weight").getAsInt());
    }
    return weights;
  }

  @Test
  void everyTargetPointsToAnExistingTableWithRealGlyphs() throws IOException {
    JsonObject targets = read(DATA.resolve("glyph_loot_targets.json"));
    assertTrue(targets.size() >= 15, "estruturas suficientes para variar a descoberta");
    for (String key : targets.keySet()) {
      assertTrue(Files.exists(DATA.resolve("loot_tables/" + targets.get(key).getAsString() + ".json")), "tabela faltando: " + key);
      for (String glyph : noteWeights(key).keySet()) {
        assertTrue(GlyphRegistry.get(new ResourceLocation(glyph)).isPresent(), key + " referencia glifo inexistente " + glyph);
      }
    }
  }

  @Test
  void dungeonLevelCapsTheRarityOfTheGlyphs() throws IOException {
    Map<String, Integer> tierOf = new HashMap<>();
    for (GlyphDefinition def : GlyphRegistry.all().values()) tierOf.put(def.id().toString(), def.tier());

    for (String glyph : noteWeights("simple_dungeon").keySet()) {
      assertEquals(1, tierOf.get(glyph), "calabouco fraco so dropa glifos comuns: " + glyph);
    }
    for (String glyph : noteWeights("desert_pyramid").keySet()) {
      assertTrue(tierOf.get(glyph) <= 2, "piramide e de nivel 2: " + glyph);
    }
    assertTrue(noteWeights("ancient_city").containsKey("arcane:gravity"), "so as estruturas mais fundas dao Gravidade");
    assertFalse(noteWeights("stronghold_library").containsKey("arcane:gravity"), "nivel 3 nao da glifo de nivel 4");
    assertFalse(noteWeights("simple_dungeon").containsKey("arcane:thunder"));
  }

  @Test
  void specialtiesFavorTheirOwnFamilies() throws IOException {
    Map<String, Integer> pyramid = noteWeights("desert_pyramid"); // fogo + sangue
    assertTrue(pyramid.get("arcane:fire") > pyramid.get("arcane:speed"), "fogo (especialidade) pesa mais que velocidade (vida)");
    assertTrue(pyramid.get("arcane:poison") > pyramid.get("arcane:slow"), "sangue (veneno) pesa mais que gelo (lentidao) numa piramide");

    Map<String, Integer> temple = noteWeights("village/village_temple"); // vida + conhecimento
    assertTrue(temple.get("arcane:heal") > temple.get("arcane:fire"), "igreja favorece cura sobre fogo");

    Map<String, Integer> library = noteWeights("stronghold_library"); // conhecimento + vazio
    assertTrue(library.get("arcane:mark") > library.get("arcane:ignite"), "biblioteca favorece conhecimento");
  }

  @Test
  void starterGlyphsNeverDrop() throws IOException {
    for (String key : read(DATA.resolve("glyph_loot_targets.json")).keySet()) {
      Map<String, Integer> w = noteWeights(key);
      assertFalse(w.containsKey("arcane:self") || w.containsKey("arcane:projectile") || w.containsKey("arcane:damage"));
    }
  }
}
