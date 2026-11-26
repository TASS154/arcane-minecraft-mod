package com.arcane.execution;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reacoes em massa (Fase K): uma estrutura generica de "limiar para zona". Cada definicao, lida de
 * {@code mass_reactions} nos arquivos de {@code data/arcane/interactions/}, tem gatilhos (qualquer um basta) e a zona
 * que cria. Hoje so existe a Nuvem de Vapor ({@code vapor_cloud}); um tipo de zona novo e um caso em
 * {@link ReactionZones}.
 *
 * <p>Gatilhos, avaliados no fim de cada efeito de uma conjuracao ({@link CastScope}):
 * <ul>
 *   <li>{@code reaction_count}: a reacao {@code reaction} aconteceu em {@code count} alvos nesse efeito
 *       (ex.: 3 alvos molhados queimados = 3 vapores);</li>
 *   <li>{@code blocks_in_radius}: efeito do elemento {@code element} com {@code count} blocos {@code block} a ate
 *       {@code radius} do ponto atingido;</li>
 *   <li>{@code form_element}: a forma {@code form} com um efeito do elemento {@code element} (Chuva + Fogo).</li>
 * </ul>
 */
public final class MassReactions {
  private MassReactions() {}

  public record Trigger(String type, String reaction, int count, SpellElement element, ResourceLocation block, double radius, String form) {}

  public record Def(String id, String nameKey, String hintKey, String zone, List<Trigger> triggers, double radius,
                    int seconds, double heartsPerSecond, int maxPerChunk, int maxPerWorld) {}

  private static volatile List<Def> defs = List.of();

  public static List<Def> defs() { return defs; }

  public static Def get(String id) {
    for (Def d : defs) if (d.id().equals(id)) return d;
    return null;
  }

  /** Troca a tabela (testes). */
  public static void set(List<Def> list) { defs = List.copyOf(list); }

  /** Le {@code mass_reactions} de todos os arquivos de interacao; entradas ruins vao para {@code problems}. */
  public static void load(Map<ResourceLocation, JsonElement> files, List<String> problems) {
    java.util.LinkedHashMap<String, Def> out = new java.util.LinkedHashMap<>();
    for (var entry : new TreeMap<>(files).entrySet()) {
      if (!entry.getValue().isJsonObject()) continue;
      JsonObject root = entry.getValue().getAsJsonObject();
      if (!root.has("mass_reactions") || !root.get("mass_reactions").isJsonArray()) continue;
      int index = 0;
      for (JsonElement el : root.getAsJsonArray("mass_reactions")) {
        index++;
        Def d = parse(el, problems, entry.getKey() + ": reacao em massa " + index);
        if (d != null) out.put(d.id(), d); // mesmo id num arquivo posterior substitui
      }
    }
    defs = List.copyOf(out.values());
  }

  public static Def parse(JsonElement el, List<String> problems, String where) {
    if (el == null || !el.isJsonObject()) { problems.add(where + ": nao e um objeto"); return null; }
    JsonObject o = el.getAsJsonObject();
    try {
      String id = o.get("id").getAsString();
      if (!id.matches("[a-z0-9_]{1,48}")) { problems.add(where + ": id invalido '" + id + "'"); return null; }
      String zone = o.has("zone") ? o.get("zone").getAsString() : "";
      if (!ReactionZones.KINDS.contains(zone)) { problems.add(where + ": zona desconhecida '" + zone + "'"); return null; }
      List<Trigger> triggers = new ArrayList<>();
      if (o.has("triggers")) for (JsonElement t : o.getAsJsonArray("triggers")) {
        Trigger tr = trigger(t.getAsJsonObject(), problems, where);
        if (tr != null) triggers.add(tr);
      }
      if (triggers.isEmpty()) { problems.add(where + ": sem gatilhos validos"); return null; }
      double radius = clamp(o.has("radius") ? o.get("radius").getAsDouble() : 4.0, 1.0, ReactionZones.MAX_RADIUS);
      int seconds = (int) clamp(o.has("seconds") ? o.get("seconds").getAsInt() : 15, 10, 20);
      double hps = clamp(o.has("damage_hearts_per_second") ? o.get("damage_hearts_per_second").getAsDouble() : 0.25, 0.0, 2.0);
      int perChunk = (int) clamp(o.has("max_per_chunk") ? o.get("max_per_chunk").getAsInt() : 2, 1, 16);
      int perWorld = (int) clamp(o.has("max_per_world") ? o.get("max_per_world").getAsInt() : 16, 1, 128);
      String nameKey = o.has("name_key") ? o.get("name_key").getAsString() : "reaction.arcane." + id;
      String hintKey = o.has("hint_key") ? o.get("hint_key").getAsString() : "reaction.arcane." + id + ".hint";
      return new Def(id, nameKey, hintKey, zone, List.copyOf(triggers), radius, seconds, hps, perChunk, perWorld);
    } catch (RuntimeException e) {
      problems.add(where + ": " + e.getMessage());
      return null;
    }
  }

  private static Trigger trigger(JsonObject t, List<String> problems, String where) {
    String type = t.has("type") ? t.get("type").getAsString() : "";
    SpellElement element = null;
    if (t.has("element")) {
      try {
        element = SpellElement.valueOf(t.get("element").getAsString().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        problems.add(where + ": elemento desconhecido '" + t.get("element").getAsString() + "'");
        return null;
      }
    }
    int count = t.has("count") ? Math.max(1, t.get("count").getAsInt()) : 1;
    switch (type) {
      case "reaction_count" -> {
        if (!t.has("reaction")) { problems.add(where + ": reaction_count sem 'reaction'"); return null; }
        return new Trigger(type, t.get("reaction").getAsString(), count, element, null, 0, "");
      }
      case "blocks_in_radius" -> {
        ResourceLocation block = t.has("block") ? ResourceLocation.tryParse(t.get("block").getAsString()) : null;
        if (block == null || element == null) { problems.add(where + ": blocks_in_radius precisa de 'element' e 'block'"); return null; }
        return new Trigger(type, "", count, element, block, clamp(t.has("radius") ? t.get("radius").getAsDouble() : 3.0, 1.0, 6.0), "");
      }
      case "form_element" -> {
        if (!t.has("form") || element == null) { problems.add(where + ": form_element precisa de 'form' e 'element'"); return null; }
        return new Trigger(type, "", 1, element, null, 0, t.get("form").getAsString());
      }
      default -> {
        problems.add(where + ": gatilho desconhecido '" + type + "'");
        return null;
      }
    }
  }

  private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }

  // ------------------------------------------------------------------ avaliacao (pura)
  /**
   * O gatilho vale para o que aconteceu num efeito? {@code blocks} conta os blocos do tipo pedido no raio (so e
   * chamado se o elemento bate, porque varre o mundo).
   */
  public static boolean fires(Trigger t, SpellElement element, String form, Map<String, Integer> reactions,
                              java.util.function.BiFunction<ResourceLocation, Double, Integer> blocks) {
    return switch (t.type()) {
      case "reaction_count" -> reactions.getOrDefault(t.reaction(), 0) >= t.count();
      case "blocks_in_radius" -> element == t.element() && blocks != null && blocks.apply(t.block(), t.radius()) >= t.count();
      case "form_element" -> element == t.element() && t.form().equals(form);
      default -> false;
    };
  }

  /** Primeira definicao com algum gatilho valendo (ou null). */
  public static Def firstFiring(SpellElement element, String form, Map<String, Integer> reactions,
                                java.util.function.BiFunction<ResourceLocation, Double, Integer> blocks) {
    for (Def d : defs) {
      for (Trigger t : d.triggers()) if (fires(t, element, form, reactions, blocks)) return d;
    }
    return null;
  }
}
