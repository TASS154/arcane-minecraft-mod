package com.arcane.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arcane.inscription.SpellLimits;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InteractionRulesTest {
  @Test
  void parsesValidRulesAndReportsInvalidOnes() {
    var json = JsonParser.parseString("""
        {"rules": [
          {"element": "storm", "when": ["raining", "wet"], "damage_add": 0.5},
          {"element": "frost", "when": ["on_fire"], "after": "extinguish"},
          {"element": "fire", "when": ["biome_tag:minecraft:is_nether"], "damage_add": 0.2},
          {"element": "plasma", "damage_add": 1.0},
          {"element": "fire", "when": ["flying"], "damage_add": 1.0},
          {"element": "fire", "after": "explode"}
        ]}""").getAsJsonObject();
    List<String> problems = new ArrayList<>();
    var rules = InteractionRules.parse(json, problems);
    assertEquals(3, rules.size());
    assertEquals(3, problems.size(), problems.toString());
    assertEquals(SpellElement.STORM, rules.get(0).element());
    assertEquals(List.of("raining", "wet"), rules.get(0).when());
    assertEquals("extinguish", rules.get(1).after());
  }

  @Test
  void bundledTableParsesWithoutProblems() throws Exception {
    try (var in = getClass().getResourceAsStream("/data/arcane/interactions/default.json")) {
      var json = JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
      List<String> problems = new ArrayList<>();
      var rules = InteractionRules.parse(json, problems);
      assertTrue(problems.isEmpty(), problems.toString());
      assertEquals(7, rules.size());
    }
  }

  @Test
  void zoneCostGrowsWithPulsesAndIsCapped() {
    assertEquals(1.0, SpellLimits.zoneMultiplier(0, 1.0), 1e-9);
    assertEquals(1.0, SpellLimits.zoneMultiplier(1, 1.0), 1e-9);
    assertEquals(2.35, SpellLimits.zoneMultiplier(10, 1.0), 1e-9);
    assertTrue(SpellLimits.zoneMultiplier(20, 1.0) > SpellLimits.zoneMultiplier(10, 1.0));
    assertEquals(6.0, SpellLimits.zoneMultiplier(60, 0.5), 1e-9);
    assertEquals(SpellLimits.zoneMultiplier(10, 0.5), SpellLimits.zoneMultiplier(10, 0.1), 1e-9, "intervalo minimo 0,5 s");
  }
}
