package com.arcane.execution;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiPredicate;

/**
 * Tabela de interacoes elemento x ambiente, lida de data/arcane/interactions/*.json (recarregavel por datapack).
 * Uma regra nova e uma linha de JSON; uma condicao nova e uma entrada em {@link #CONDITIONS}.
 */
public final class InteractionRules {
  private static final Logger LOG = LogUtils.getLogger();

  public record Rule(SpellElement element, List<String> when, double damageAdd, String after) {}

  private static volatile List<Rule> rules = List.of();
  private static volatile List<String> problems = List.of();

  private static final Map<String, BiPredicate<ServerLevel, Entity>> CONDITIONS = Map.of(
      "raining", (level, e) -> level.isRainingAt(e.blockPosition().above()),
      "wet", (level, e) -> e.isInWaterOrRain(),
      "frozen", (level, e) -> e instanceof LivingEntity living && living.getTicksFrozen() > 0,
      "on_fire", (level, e) -> e.isOnFire(),
      "hot_biome", (level, e) -> {
        var biome = level.getBiome(e.blockPosition());
        return biome.is(BiomeTags.IS_NETHER) || biome.value().getBaseTemperature() >= 1.5f;
      },
      "lush_biome", (level, e) -> {
        var biome = level.getBiome(e.blockPosition());
        return biome.is(BiomeTags.IS_FOREST) || biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
      });

  private InteractionRules() {}

  public static List<Rule> rules() { return rules; }

  /** Linhas com regras ignoradas na ultima leitura (elemento, condicao ou acao desconhecidos). */
  public static List<String> problems() { return problems; }

  public static boolean knownCondition(String name) {
    if (name.startsWith("biome_tag:")) return ResourceLocation.tryParse(name.substring("biome_tag:".length())) != null;
    return CONDITIONS.containsKey(name);
  }

  private static boolean holds(String name, ServerLevel level, Entity target) {
    if (name.startsWith("biome_tag:")) {
      ResourceLocation id = ResourceLocation.tryParse(name.substring("biome_tag:".length()));
      return id != null && level.getBiome(target.blockPosition()).is(TagKey.create(Registries.BIOME, id));
    }
    BiPredicate<ServerLevel, Entity> p = CONDITIONS.get(name);
    return p != null && p.test(level, target);
  }

  /** Regras cujo elemento e condicoes valem para o alvo. */
  public static List<Rule> matching(ServerLevel level, SpellElement element, Entity target) {
    List<Rule> out = new ArrayList<>();
    for (Rule r : rules) {
      if (r.element() != element) continue;
      boolean all = true;
      for (String c : r.when()) {
        if (!holds(c, level, target)) { all = false; break; }
      }
      if (all) out.add(r);
    }
    return out;
  }

  /** Interpreta o JSON de um arquivo de regras; entradas invalidas sao registradas e ignoradas. */
  public static List<Rule> parse(JsonObject root, List<String> problemsOut) {
    List<Rule> out = new ArrayList<>();
    JsonArray array = root.has("rules") && root.get("rules").isJsonArray() ? root.getAsJsonArray("rules") : new JsonArray();
    int index = 0;
    for (JsonElement el : array) {
      index++;
      if (!el.isJsonObject()) { problemsOut.add("regra " + index + ": nao e um objeto"); continue; }
      JsonObject o = el.getAsJsonObject();
      SpellElement element;
      try {
        element = SpellElement.valueOf(o.get("element").getAsString().toUpperCase(Locale.ROOT));
      } catch (RuntimeException e) {
        problemsOut.add("regra " + index + ": elemento invalido");
        continue;
      }
      List<String> when = new ArrayList<>();
      boolean ok = true;
      if (o.has("when")) {
        for (JsonElement c : o.getAsJsonArray("when")) {
          String name = c.getAsString();
          if (!knownCondition(name)) { problemsOut.add("regra " + index + ": condicao desconhecida '" + name + "'"); ok = false; }
          when.add(name);
        }
      }
      String after = o.has("after") ? o.get("after").getAsString() : "";
      if (!after.isEmpty() && !after.equals("extinguish")) { problemsOut.add("regra " + index + ": acao desconhecida '" + after + "'"); ok = false; }
      if (ok) out.add(new Rule(element, List.copyOf(when), o.has("damage_add") ? o.get("damage_add").getAsDouble() : 0.0, after));
    }
    return out;
  }

  public static SimpleJsonResourceReloadListener reloadListener() {
    return new SimpleJsonResourceReloadListener(new Gson(), "interactions") {
      @Override
      protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<Rule> loaded = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        for (var entry : new TreeMap<>(files).entrySet()) {
          if (!entry.getValue().isJsonObject()) continue;
          List<String> local = new ArrayList<>();
          loaded.addAll(parse(entry.getValue().getAsJsonObject(), local));
          local.forEach(p -> issues.add(entry.getKey() + ": " + p));
        }
        rules = List.copyOf(loaded);
        problems = List.copyOf(issues);
        issues.forEach(p -> LOG.warn("Regra de interacao ignorada em {}", p));
        LOG.info("Carregadas {} regras de interacao.", rules.size());
      }
    };
  }
}
