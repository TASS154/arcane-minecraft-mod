package com.arcane.affinity;

import com.arcane.execution.SpellElement;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Tabela de afinidades: leitura tolerante, sobrescrita por id (datapack) e teto com as reacoes. */
class AffinitiesTest {
  private static final ResourceLocation FIRE = new ResourceLocation("arcane", "fire_creatures");

  @AfterEach
  void reset() { Affinities.load(Map.of()); }

  static JsonElement json(String s) { return JsonParser.parseString(s); }

  @Test
  void parsesMultipliersAndImmunity() {
    var problems = new ArrayList<String>();
    var p = Affinities.parse(FIRE, json("{\"entities\":[\"minecraft:blaze\"],\"immune\":[\"fire\"],\"multipliers\":{\"frost\":1.5}}"), problems);
    assertTrue(problems.isEmpty(), problems.toString());
    assertEquals(0.0, p.multiplier(SpellElement.FIRE));
    assertEquals(1.5, p.multiplier(SpellElement.FROST));
    assertEquals(1.0, p.multiplier(SpellElement.STORM), "elemento sem entrada: neutro");
  }

  @Test
  void invalidEntriesAreLoggedAndIgnoredWithoutCrashing() {
    List<String> problems = Affinities.load(Map.of(
        new ResourceLocation("arcane", "a"), json("[1, 2]"),
        new ResourceLocation("arcane", "b"), json("{\"entities\":[\"minecraft:pig\"],\"multipliers\":{\"plasma\":2, \"fire\":-1, \"frost\":2}}"),
        new ResourceLocation("arcane", "c"), json("{\"entities\": 5}")));
    assertTrue(problems.size() >= 3, problems.toString());
    var b = Affinities.get(new ResourceLocation("arcane", "b"));
    assertNotNull(b, "o perfil com uma entrada ruim continua valendo com as boas");
    assertEquals(2.0, b.multiplier(SpellElement.FROST));
    assertEquals(1.0, b.multiplier(SpellElement.FIRE), "multiplicador negativo ignorado");
    assertNull(Affinities.get(new ResourceLocation("arcane", "a")));
  }

  @Test
  void aFileWithTheSameIdOverridesTheProfile() {
    Map<ResourceLocation, JsonElement> base = new HashMap<>();
    base.put(FIRE, json("{\"entities\":[\"minecraft:blaze\"],\"immune\":[\"fire\"]}"));
    Affinities.load(base);
    assertEquals(0.0, Affinities.get(FIRE).multiplier(SpellElement.FIRE));
    // o datapack de cima entrega o mesmo id com outro conteudo: o Minecraft passa so o de cima
    base.put(FIRE, json("{\"entities\":[\"minecraft:blaze\"],\"multipliers\":{\"fire\":0.75}}"));
    Affinities.load(base);
    assertEquals(0.75, Affinities.get(FIRE).multiplier(SpellElement.FIRE));
  }

  @Test
  void capLimitsOnlyTheBonus() {
    assertEquals(1.5, Affinities.capped(1.5, 1.0, 2.0), 1e-9, "abaixo do teto nao muda");
    assertEquals(2.0 / 1.5, Affinities.capped(1.5, 1.5, 2.0), 1e-9, "reacao 1,5 x afinidade 1,5 = 2,25: encolhe ate 2");
    assertEquals(1.0, Affinities.capped(1.5, 3.0, 2.0), 1e-9, "a reacao sozinha ja passa do teto: afinidade neutra, reacao intocada");
    assertEquals(0.5, Affinities.capped(0.5, 3.0, 2.0), 1e-9, "resistencia nunca e limitada");
  }

  @Test
  void dummyCycleStartsNeutral() {
    Affinities.load(Map.of(FIRE, json("{\"entities\":[\"minecraft:blaze\"]}")));
    assertEquals(List.of("", "arcane:fire_creatures"), Affinities.dummyCycle());
  }
}
