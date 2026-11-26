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

  /**
   * Uma reacao elemento x ambiente. {@code id}, {@code nameKey} e {@code hintKey} (Fase K) sao opcionais no JSON: sem
   * eles, o id sai do elemento e das condicoes ({@code fire_wet_steam}) e as chaves de texto seguem o id.
   */
  public record Rule(String id, String nameKey, String hintKey, SpellElement element, List<String> when, double damageAdd,
                     String after, int freezeAdd) {
    public Rule(SpellElement element, List<String> when, double damageAdd, String after, int freezeAdd) {
      this(defaultId(element, when, after), "", "", element, when, damageAdd, after, freezeAdd);
    }

    public Rule {
      if (id == null || id.isBlank()) id = defaultId(element, when, after);
      if (nameKey == null || nameKey.isBlank()) nameKey = "reaction.arcane." + id;
      if (hintKey == null || hintKey.isBlank()) hintKey = "reaction.arcane." + id + ".hint";
    }
  }

  /** Id gerado quando o JSON nao tem: elemento, condicoes e acao ({@code fire_wet_steam}). */
  public static String defaultId(SpellElement element, List<String> when, String after) {
    StringBuilder b = new StringBuilder(element.name().toLowerCase(Locale.ROOT));
    for (String c : when) b.append('_').append(c.replace(':', '_').replace('/', '_'));
    if (after != null && !after.isEmpty()) b.append('_').append(after);
    return b.toString();
  }

  /** Reacoes distintas por id, na ordem em que foram lidas (o Diario de descobertas lista estas). */
  public static List<Rule> distinct() {
    java.util.LinkedHashMap<String, Rule> out = new java.util.LinkedHashMap<>();
    for (Rule r : rules) out.putIfAbsent(r.id(), r);
    return List.copyOf(out.values());
  }

  private static volatile List<Rule> rules = List.of();
  private static volatile List<String> problems = List.of();

  private static final Map<String, BiPredicate<ServerLevel, Entity>> CONDITIONS = Map.of(
      "raining", (level, e) -> level.isRainingAt(e.blockPosition().above()),
      "wet", (level, e) -> e.isInWaterOrRain() || Wetness.isSoaked(e),
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
      if (!after.isEmpty() && !after.equals("extinguish") && !after.equals("steam")) { problemsOut.add("regra " + index + ": acao desconhecida '" + after + "'"); ok = false; }
      int freezeAdd = o.has("freeze_add") ? Math.max(0, Math.min(400, o.get("freeze_add").getAsInt())) : 0;
      String id = o.has("id") ? o.get("id").getAsString() : "";
      if (!id.isEmpty() && !id.matches("[a-z0-9_]{1,48}")) { problemsOut.add("regra " + index + ": id invalido '" + id + "'"); ok = false; }
      String nameKey = o.has("name_key") ? o.get("name_key").getAsString() : "";
      String hintKey = o.has("hint_key") ? o.get("hint_key").getAsString() : "";
      if (ok) out.add(new Rule(id, nameKey, hintKey, element, List.copyOf(when),
          o.has("damage_add") ? o.get("damage_add").getAsDouble() : 0.0, after, freezeAdd));
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
        List<String> massIssues = new ArrayList<>();
        MassReactions.load(files, massIssues);
        issues.addAll(massIssues);
        problems = List.copyOf(issues);
        issues.forEach(p -> LOG.warn("Regra de interacao ignorada em {}", p));
        LOG.info("Carregadas {} regras de interacao.", rules.size());
      }
    };
  }
}
