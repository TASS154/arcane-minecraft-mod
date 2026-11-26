package com.arcane.entity;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Fase N: definicao do Guardiao de Ruina em JSON e o portao de fase generico. */
class RuinGuardiansTest {
  static final ResourceLocation ID = new ResourceLocation("arcane", "frost_warden");

  @Test
  void shippedJsonParsesAndMatchesTheBuiltInDefault() throws Exception {
    var problems = new ArrayList<String>();
    var d = RuinGuardians.parse(ID, JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/arcane/guardians/frost_warden.json"))), problems);
    assertTrue(problems.isEmpty(), problems.toString());
    assertEquals(RuinGuardians.DEFAULT, d, "o padrao embutido e o mesmo do JSON");
    assertEquals(160, d.windowTicks());
    assertTrue(d.breakReactions().contains("steam"));
  }

  @Test
  void valuesAreClampedAndOriginalsNeverDrop() {
    var problems = new ArrayList<String>();
    var d = RuinGuardians.parse(ID, JsonParser.parseString(
        "{\"health\": 9999, \"reward\": {\"glyph_max_tier\": 5, \"glyph_chance\": 3}, \"shell\": {\"window_multiplier\": 10}}"), problems);
    assertEquals(200, d.health(), "vida modesta: teto de 200");
    assertEquals(4, d.glyphMaxTier(), "nunca o tier dos originais");
    assertEquals(1.0, d.glyphChance());
    assertEquals(3f, d.windowMultiplier());
    assertNull(RuinGuardians.parse(ID, JsonParser.parseString("[1]"), problems));
  }

  @Test
  void phaseGateBlocksUntilTheShellFallsThenScales() {
    boolean[] shell = {true};
    var gate = new PhaseGate<Object>(o -> shell[0], s -> false, o -> !shell[0], 1.5f);
    assertTrue(gate.blocks(new Object(), null), "casca de pe: bloqueia");
    assertEquals(10f, gate.scale(new Object(), 10f));
    shell[0] = false;
    assertFalse(gate.blocks(new Object(), null));
    assertEquals(15f, gate.scale(new Object(), 10f), 1e-6, "janela aberta: x1,5");
    var bypass = new PhaseGate<Object>(o -> true, s -> true, o -> false, 2f);
    assertFalse(bypass.blocks(new Object(), null), "o que atravessa (o /kill na Hidra) passa");
  }
}
