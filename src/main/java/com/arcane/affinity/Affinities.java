package com.arcane.affinity;

import com.arcane.execution.SpellElement;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Afinidades elementais de criaturas (Fase I), lidas de {@code data/arcane/affinities/*.json} (um arquivo = um perfil).
 * Cada perfil vale para tipos de entidade ({@code entities}) e/ou tags ({@code tags}) e da um multiplicador por elemento
 * ({@code multipliers}) e imunidades ({@code immune}). Um datapack sobrescreve um perfil com um arquivo de mesmo id.
 * Entrada invalida: registrada no log e ignorada (nunca derruba o jogo). Criatura sem perfil (a Hidra, por exemplo):
 * multiplicador 1, comportamento de antes.
 *
 * <p>O elemento e o de GAMEPLAY do efeito que causa o dano ({@code SpellElement.ofGlyph}, de effects.json), nao o visual
 * da magia (composicao, modificador Cor). Ver {@link AffinityContext}.
 */
public final class Affinities {
  private static final Logger LOG = LogUtils.getLogger();

  public record Profile(ResourceLocation id, Set<ResourceLocation> entities, List<ResourceLocation> tags,
                        Map<SpellElement, Double> multipliers, Set<SpellElement> immune) {
    public double multiplier(SpellElement e) {
      if (immune.contains(e)) return 0.0;
      return multipliers.getOrDefault(e, 1.0);
    }
  }

  private static volatile Map<ResourceLocation, Profile> profiles = Map.of();
  private static volatile Map<ResourceLocation, JsonElement> lastFiles = Map.of();

  /** Arquivos da ultima leitura (os testes trocam a tabela e depois a restauram). */
  public static Map<ResourceLocation, JsonElement> lastFiles() { return lastFiles; }

  private Affinities() {}

  public static Map<ResourceLocation, Profile> profiles() { return profiles; }

  public static Profile get(ResourceLocation id) { return profiles.get(id); }

  /** Perfil de uma entidade: o Boneco usa o perfil escolhido; os outros, o primeiro por tipo, depois por tag. */
  public static Profile of(Entity e) {
    if (e instanceof com.arcane.entity.ArcaneDummyEntity dummy) {
      ResourceLocation id = ResourceLocation.tryParse(dummy.affinityProfile());
      return dummy.affinityProfile().isEmpty() || id == null ? null : profiles.get(id);
    }
    ResourceLocation type = EntityType.getKey(e.getType());
    for (Profile p : profiles.values()) if (p.entities().contains(type)) return p;
    for (Profile p : profiles.values()) {
      for (ResourceLocation tag : p.tags()) {
        if (e.getType().is(TagKey.create(Registries.ENTITY_TYPE, tag))) return p;
      }
    }
    return null;
  }

  /** Multiplicador de afinidade de {@code e} contra {@code element} (1 = neutro, 0 = imune). */
  public static double multiplier(Entity e, SpellElement element) {
    Profile p = of(e);
    return p == null ? 1.0 : p.multiplier(element);
  }

  /**
   * Teto da afinidade acumulada com as reacoes: se reacao x afinidade passa de {@code cap}, a afinidade encolhe ate o
   * total bater no teto, nunca abaixo de 1 (o teto nao mexe no numero das reacoes, que ja existia). So limita bonus.
   */
  public static double capped(double affinity, double reaction, double cap) {
    if (affinity <= 1.0 || cap <= 0) return affinity;
    double r = reaction <= 0 ? 1.0 : reaction;
    if (r * affinity <= cap) return affinity;
    return Math.max(1.0, cap / r);
  }

  /** Ids dos perfis em ordem (o Boneco alterna por eles; "" = neutro primeiro). */
  public static List<String> dummyCycle() {
    List<String> out = new ArrayList<>();
    out.add("");
    profiles.keySet().stream().map(ResourceLocation::toString).sorted().forEach(out::add);
    return out;
  }

  // ------------------------------------------------------------------ leitura
  /** Le um perfil; problemas vao para {@code problems} e o perfil sai null se nao der para usar. */
  public static Profile parse(ResourceLocation id, JsonElement json, List<String> problems) {
    if (json == null || !json.isJsonObject()) {
      problems.add(id + ": nao e um objeto");
      return null;
    }
    JsonObject o = json.getAsJsonObject();
    try {
      Set<ResourceLocation> entities = new java.util.LinkedHashSet<>();
      List<ResourceLocation> tags = new ArrayList<>();
      if (o.has("entities")) for (JsonElement el : o.getAsJsonArray("entities")) {
        ResourceLocation rl = ResourceLocation.tryParse(el.getAsString());
        if (rl == null) problems.add(id + ": entidade invalida '" + el.getAsString() + "'");
        else entities.add(rl);
      }
      if (o.has("tags")) for (JsonElement el : o.getAsJsonArray("tags")) {
        String s = el.getAsString();
        ResourceLocation rl = ResourceLocation.tryParse(s.startsWith("#") ? s.substring(1) : s);
        if (rl == null) problems.add(id + ": tag invalida '" + s + "'");
        else tags.add(rl);
      }
      Map<SpellElement, Double> mult = new EnumMap<>(SpellElement.class);
      if (o.has("multipliers")) for (var e : o.getAsJsonObject("multipliers").entrySet()) {
        SpellElement el = element(e.getKey());
        double v = e.getValue().getAsDouble();
        if (el == null) problems.add(id + ": elemento desconhecido '" + e.getKey() + "'");
        else if (!(v >= 0) || v > 10) problems.add(id + ": multiplicador fora de 0..10 em '" + e.getKey() + "'");
        else mult.put(el, v);
      }
      Set<SpellElement> immune = EnumSet.noneOf(SpellElement.class);
      if (o.has("immune")) for (JsonElement el : o.getAsJsonArray("immune")) {
        SpellElement e = element(el.getAsString());
        if (e == null) problems.add(id + ": elemento desconhecido em immune '" + el.getAsString() + "'");
        else immune.add(e);
      }
      if (entities.isEmpty() && tags.isEmpty() && !o.has("dummy_only")) problems.add(id + ": sem entities nem tags (so o Boneco usa)");
      return new Profile(id, Set.copyOf(entities), List.copyOf(tags), Map.copyOf(mult), Set.copyOf(immune));
    } catch (RuntimeException e) {
      problems.add(id + ": " + e.getMessage());
      return null;
    }
  }

  private static SpellElement element(String name) {
    try {
      return SpellElement.valueOf(name.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /** Troca a tabela inteira (o que um /reload faz). Devolve os problemas encontrados. */
  public static List<String> load(Map<ResourceLocation, JsonElement> files) {
    lastFiles = Map.copyOf(files);
    Map<ResourceLocation, Profile> out = new LinkedHashMap<>();
    List<String> problems = new ArrayList<>();
    for (var entry : new TreeMap<>(files).entrySet()) {
      Profile p = parse(entry.getKey(), entry.getValue(), problems);
      if (p != null) out.put(entry.getKey(), p);
    }
    profiles = java.util.Collections.unmodifiableMap(out);
    return problems;
  }

  public static SimpleJsonResourceReloadListener reloadListener() {
    return new SimpleJsonResourceReloadListener(new Gson(), "affinities") {
      @Override
      protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<String> problems = load(files);
        problems.forEach(p -> LOG.warn("Afinidade: {}", p));
        LOG.info("Carregados {} perfis de afinidade.", profiles.size());
      }
    };
  }
}
