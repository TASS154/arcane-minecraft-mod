package com.arcane.fx;

import com.arcane.execution.ElementVisuals;
import com.arcane.execution.SpellElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** data/arcane/elements/<id>.json: todo elemento tem arquivo valido, e campo ausente vira o padrao de antes. */
class ElementVisualsTest {
  @Test
  void everyElementHasAValidJsonAndKeepsItsColors() throws IOException {
    var all = ElementVisuals.load();
    for (SpellElement e : SpellElement.values()) {
      Path file = Path.of("src/main/resources/data/arcane/elements/" + e.name().toLowerCase(Locale.ROOT) + ".json");
      assertTrue(Files.exists(file), "falta " + file);
      var v = all.get(e);
      assertNotNull(v);
      assertTrue(FxFamilies.SPRITES.contains(v.sprite()), e + ": familia desconhecida " + v.sprite());
      assertTrue(FxFamilies.DECALS.contains(v.decal()), e + ": decal desconhecido " + v.decal());
      assertTrue(v.motion().minLife() >= 1 && v.motion().maxLife() >= v.motion().minLife(), e + ": vida invalida");
      assertTrue(v.light() >= 0 && v.light() <= 15);
    }
    // as cores nao mudaram com a mudanca para json
    assertEquals(0xFF8C28, SpellElement.FIRE.color());
    assertEquals(0xFFD24A, SpellElement.FIRE.secondary());
    assertEquals(0x9EE8D8, SpellElement.SOUND.color());
  }

  @Test
  void missingFieldsFallBackToTheOldBehaviour() {
    var parse = assertDoesNotThrow(() -> ElementVisuals.class.getDeclaredMethod("parse", JsonObject.class, ElementVisuals.Visual.class));
    parse.setAccessible(true);
    var defaults = assertDoesNotThrow(() -> ElementVisuals.class.getDeclaredMethod("defaults", SpellElement.class));
    defaults.setAccessible(true);
    var base = (ElementVisuals.Visual) assertDoesNotThrow(() -> defaults.invoke(null, SpellElement.FROST));
    var empty = (ElementVisuals.Visual) assertDoesNotThrow(() -> parse.invoke(null, new JsonObject(), base));
    assertEquals(base, empty, "json vazio = comportamento antigo");
    JsonObject only = new JsonObject();
    only.addProperty("color", "#010203");
    var v = (ElementVisuals.Visual) assertDoesNotThrow(() -> parse.invoke(null, only, base));
    assertEquals(0x010203, v.color());
    assertEquals(base.motion(), v.motion());
  }
}
