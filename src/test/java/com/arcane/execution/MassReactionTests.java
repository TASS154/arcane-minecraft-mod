package com.arcane.execution;

import com.arcane.client.DiscoveryView;
import com.arcane.client.hud.VaporOverlay;
import com.arcane.network.packet.S2CReactionJournalPacket;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Fase K: esquema das reacoes, reacoes em massa, Diario de descobertas e limites da Nuvem de Vapor. */
class MassReactionTests {
  static final Path DEFAULT = Path.of("src/main/resources/data/arcane/interactions/default.json");

  @AfterEach
  void reset() { MassReactions.set(List.of()); }

  static JsonElement defaults() throws Exception { return JsonParser.parseString(Files.readString(DEFAULT)); }

  @Test
  void oldRulesWithoutIdStillParseAndGetAGeneratedId() {
    var problems = new ArrayList<String>();
    var rules = InteractionRules.parse(JsonParser.parseString(
        "{\"rules\":[{\"element\":\"fire\",\"when\":[\"wet\"],\"damage_add\":-0.4,\"after\":\"steam\"}]}").getAsJsonObject(), problems);
    assertTrue(problems.isEmpty(), problems.toString());
    var r = rules.get(0);
    assertEquals("fire_wet_steam", r.id());
    assertEquals("reaction.arcane.fire_wet_steam", r.nameKey());
    assertEquals("reaction.arcane.fire_wet_steam.hint", r.hintKey());
  }

  @Test
  void newFieldsAreReadAndBadIdsRejected() {
    var problems = new ArrayList<String>();
    var rules = InteractionRules.parse(JsonParser.parseString("{\"rules\":["
        + "{\"id\":\"steam\",\"name_key\":\"x.name\",\"hint_key\":\"x.hint\",\"element\":\"fire\",\"when\":[\"wet\"]},"
        + "{\"id\":\"Bad Id!\",\"element\":\"fire\"}]}").getAsJsonObject(), problems);
    assertEquals(1, rules.size());
    assertEquals("x.name", rules.get(0).nameKey());
    assertEquals(1, problems.size(), problems.toString());
  }

  @Test
  void defaultFileHasIdsAndTheVaporCloud() throws Exception {
    var problems = new ArrayList<String>();
    var rules = InteractionRules.parse(defaults().getAsJsonObject(), problems);
    assertTrue(problems.isEmpty(), problems.toString());
    assertEquals(8, rules.size(), "as 8 regras de antes continuam");
    assertTrue(rules.stream().anyMatch(r -> r.id().equals("steam") && r.after().equals("steam")));
    MassReactions.load(Map.of(new ResourceLocation("arcane", "default"), defaults()), problems);
    assertTrue(problems.isEmpty(), problems.toString());
    var vapor = MassReactions.get("vapor_cloud");
    assertNotNull(vapor);
    assertEquals(3, vapor.triggers().size());
    assertTrue(vapor.seconds() >= 10 && vapor.seconds() <= 20, "some em 10 a 20 s");
  }

  @Test
  void eachTriggerFiresOnlyOnItsThreshold() throws Exception {
    MassReactions.load(Map.of(new ResourceLocation("arcane", "default"), defaults()), new ArrayList<>());
    assertNull(MassReactions.firstFiring(SpellElement.FIRE, "aura", Map.of("steam", 2), (b, r) -> 0), "2 vapores: abaixo de 3");
    assertNotNull(MassReactions.firstFiring(SpellElement.FIRE, "aura", Map.of("steam", 3), (b, r) -> 0), "3 alvos molhados queimados");
    assertNull(MassReactions.firstFiring(SpellElement.FIRE, "projectile", Map.of(), (b, r) -> 5), "5 blocos de agua: abaixo de 6");
    assertNotNull(MassReactions.firstFiring(SpellElement.FIRE, "projectile", Map.of(), (b, r) -> 6), "6 blocos de agua no raio");
    assertNull(MassReactions.firstFiring(SpellElement.FROST, "projectile", Map.of(), (b, r) -> 99), "agua so conta com fogo");
    assertNotNull(MassReactions.firstFiring(SpellElement.FIRE, "rain", Map.of(), (b, r) -> 0), "Chuva + Fogo");
    assertNull(MassReactions.firstFiring(SpellElement.STORM, "rain", Map.of(), (b, r) -> 0));
  }

  @Test
  void badMassReactionsAreLoggedNotCrashing() {
    var problems = new ArrayList<String>();
    MassReactions.load(Map.of(new ResourceLocation("arcane", "x"), JsonParser.parseString(
        "{\"mass_reactions\":[{\"id\":\"a\",\"zone\":\"lava_lake\",\"triggers\":[{\"type\":\"form_element\",\"form\":\"rain\",\"element\":\"fire\"}]},"
            + "{\"id\":\"b\",\"zone\":\"vapor_cloud\",\"triggers\":[{\"type\":\"nope\"}]},"
            + "{\"id\":\"c\",\"zone\":\"vapor_cloud\",\"seconds\":99,\"triggers\":[{\"type\":\"form_element\",\"form\":\"rain\",\"element\":\"fire\"}]}]}")), problems);
    assertEquals(1, MassReactions.defs().size(), problems.toString());
    assertEquals(20, MassReactions.defs().get(0).seconds(), "duracao presa em 20 s");
    assertTrue(problems.size() >= 2);
  }

  // ------------------------------------------------------------------ Diario
  @Test
  void journalRecordsOnceAndSurvivesSaving() {
    var j = new ReactionJournal();
    UUID a = UUID.randomUUID(), b = UUID.randomUUID();
    assertTrue(j.record(a, new ReactionJournal.Entry("steam", 1000L, 3, "aura,fire")));
    assertFalse(j.record(a, new ReactionJournal.Entry("steam", 2000L, 9, "projectile,fire")), "so a primeira vez");
    assertEquals(1000L, j.of(a).get("steam").epochMillis(), "a data da primeira fica");
    assertTrue(j.record(a, new ReactionJournal.Entry("vapor_cloud", 1500L, 3, "aura,fire")));
    assertFalse(j.has(b, "steam"), "a descoberta de um jogador nao vale para outro");
    var loaded = ReactionJournal.load(j.save(new CompoundTag()));
    assertEquals(j.of(a), loaded.of(a), "salvar e ler devolve o mesmo diario");
    assertEquals(List.of("steam", "vapor_cloud"), List.copyOf(loaded.of(a).keySet()), "na ordem da descoberta");
    assertTrue(loaded.of(b).isEmpty());
  }

  @Test
  void discoveryViewHidesUnknownAndCounts() {
    var catalog = List.of(new S2CReactionJournalPacket.Def("steam", "n.steam", "h.steam"),
        new S2CReactionJournalPacket.Def("hiss", "n.hiss", "h.hiss"),
        new S2CReactionJournalPacket.Def("pack_reaction", "n.p", "h.p"));
    var found = List.of(new ReactionJournal.Entry("steam", 1L, 4, "aura,fire"), new ReactionJournal.Entry("removed_by_pack", 1L, 1, ""));
    var rows = DiscoveryView.rows(catalog, found);
    assertEquals(3, rows.size(), "reacao de datapack aparece sozinha; a que saiu do catalogo nao");
    assertEquals(1, DiscoveryView.known(rows));
    assertTrue(rows.get(0).known());
    assertEquals(List.of("aura", "fire"), rows.get(0).excerpt());
    assertFalse(rows.get(1).known());
    assertEquals("", rows.get(1).nameKey(), "sem spoiler: nem nome nem dica");
    assertEquals("", rows.get(1).hintKey());
  }

  // ------------------------------------------------------------------ nuvem
  @Test
  void cloudMergeAndDensityLimits() {
    assertEquals(Math.sqrt(32), ReactionZones.mergedRadius(4, 4), 1e-9, "a area se soma");
    assertEquals(ReactionZones.MAX_RADIUS, ReactionZones.mergedRadius(7, 7), 1e-9, "teto do raio");
    Vec3 c = ReactionZones.mergedCenter(Vec3.ZERO, 2, new Vec3(10, 0, 0), 4);
    assertEquals(8.0, c.x, 1e-9, "puxa para a maior");
    assertTrue(ReactionZones.shouldMerge(Vec3.ZERO, 4, new Vec3(4, 0, 0), 4));
    assertFalse(ReactionZones.shouldMerge(Vec3.ZERO, 4, new Vec3(6, 0, 0), 4));
    int d4 = ReactionZones.density(4), d8 = ReactionZones.density(8);
    assertTrue(d8 < d4 * 4, "dobrar o raio (4x a area) nao quadruplica as particulas: " + d4 + " -> " + d8);
  }

  @Test
  void veilFadesInAndOut() {
    assertEquals(0.5f, VaporOverlay.approach(0f, true, 300), 1e-6);
    assertEquals(1f, VaporOverlay.approach(0.9f, true, 300), 1e-6);
    assertEquals(0f, VaporOverlay.approach(0.2f, false, 600), 1e-6);
  }
}
