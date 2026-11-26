package com.arcane.entity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Definicoes do Guardiao de Ruina (Fase N), lidas de {@code data/arcane/guardians/*.json}. Um arquivo = um guardiao.
 * Numeros PROVISORIOS (docs/PERGUNTAS.md). Sem escala por jogador: a vida e fixa.
 */
public final class RuinGuardians {
  private RuinGuardians() {}

  public record Drop(ResourceLocation item, int min, int max) {}

  public record Def(ResourceLocation id, double health, double attack, String requiredState, List<String> breakReactions,
                    int windowTicks, float windowMultiplier, double spawnChance, Drop ink, Drop material,
                    double glyphChance, int glyphMaxTier) {}

  /** Padrao embutido (se o JSON faltar ou quebrar): o mesmo do frost_warden.json. */
  public static final Def DEFAULT = new Def(new ResourceLocation("arcane", "frost_warden"), 60, 5, "wet", List.of("steam"),
      160, 1.5f, 0.5, new Drop(new ResourceLocation("arcane", "common_ink"), 2, 4),
      new Drop(new ResourceLocation("minecraft", "blue_ice"), 1, 3), 0.3, 3);

  private static volatile Map<ResourceLocation, Def> defs = Map.of();

  public static Def get(ResourceLocation id) { return defs.getOrDefault(id, DEFAULT); }

  public static Def first() { return defs.isEmpty() ? DEFAULT : defs.values().iterator().next(); }

  public static Def parse(ResourceLocation id, JsonElement el, List<String> problems) {
    if (el == null || !el.isJsonObject()) { problems.add(id + ": nao e um objeto"); return null; }
    JsonObject o = el.getAsJsonObject();
    try {
      double health = clamp(num(o, "health", DEFAULT.health()), 10, 200);
      double attack = clamp(num(o, "attack", DEFAULT.attack()), 0, 20);
      JsonObject shell = o.has("shell") ? o.getAsJsonObject("shell") : new JsonObject();
      String state = shell.has("requires_state") ? shell.get("requires_state").getAsString() : DEFAULT.requiredState();
      List<String> reactions = new ArrayList<>();
      if (shell.has("breaks_on")) for (JsonElement r : shell.getAsJsonArray("breaks_on")) reactions.add(r.getAsString());
      if (reactions.isEmpty()) reactions.addAll(DEFAULT.breakReactions());
      int window = (int) Math.round(clamp(num(shell, "window_seconds", DEFAULT.windowTicks() / 20.0), 2, 30) * 20);
      float mult = (float) clamp(num(shell, "window_multiplier", DEFAULT.windowMultiplier()), 1, 3);
      double spawn = clamp(num(o, "spawn_chance", DEFAULT.spawnChance()), 0, 1);
      JsonObject reward = o.has("reward") ? o.getAsJsonObject("reward") : new JsonObject();
      Drop ink = drop(reward, "ink", DEFAULT.ink());
      Drop material = drop(reward, "material", DEFAULT.material());
      double glyphChance = clamp(num(reward, "glyph_chance", DEFAULT.glyphChance()), 0, 1);
      int tier = (int) clamp(num(reward, "glyph_max_tier", DEFAULT.glyphMaxTier()), 1, 4); // nunca original (tier 5)
      return new Def(id, health, attack, state, List.copyOf(reactions), window, mult, spawn, ink, material, glyphChance, tier);
    } catch (RuntimeException e) {
      problems.add(id + ": " + e.getMessage());
      return null;
    }
  }

  private static Drop drop(JsonObject reward, String key, Drop fallback) {
    if (!reward.has(key)) return fallback;
    JsonObject d = reward.getAsJsonObject(key);
    ResourceLocation item = ResourceLocation.tryParse(d.get("item").getAsString());
    if (item == null) return fallback;
    int min = (int) clamp(num(d, "min", 1), 0, 16), max = (int) clamp(num(d, "max", min), min, 16);
    return new Drop(item, min, max);
  }

  private static double num(JsonObject o, String key, double fallback) { return o.has(key) ? o.get(key).getAsDouble() : fallback; }

  private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }

  public static List<String> load(Map<ResourceLocation, JsonElement> files) {
    List<String> problems = new ArrayList<>();
    java.util.LinkedHashMap<ResourceLocation, Def> out = new java.util.LinkedHashMap<>();
    for (var e : new TreeMap<>(files).entrySet()) {
      Def d = parse(e.getKey(), e.getValue(), problems);
      if (d != null) out.put(e.getKey(), d);
    }
    defs = java.util.Collections.unmodifiableMap(out);
    return problems;
  }

  public static SimpleJsonResourceReloadListener reloadListener() {
    return new SimpleJsonResourceReloadListener(new Gson(), "guardians") {
      @Override
      protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        load(files).forEach(p -> LogUtils.getLogger().warn("Guardiao de Ruina: {}", p));
      }
    };
  }
}
