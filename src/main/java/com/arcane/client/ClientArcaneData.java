package com.arcane.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Copia de exibicao do estado arcano no cliente (HUD, Bancada e Grimorio).
 * Sem imports de classes client-only, para poder ser referenciada pelo pacote de sync.
 */
public final class ClientArcaneData {
  public record ParamDef(String key, double def, double min, double max, boolean isInt) {
    public double clamp(double v) {
      double c = Math.max(min, Math.min(max, v));
      return isInt ? Math.round(c) : Math.round(c * 100.0) / 100.0;
    }

    /** Passo da roda do mouse: 5% do intervalo (no minimo 1 para inteiros). */
    public double step() {
      double s = (max - min) / ((max - min) >= 100.0 ? 60.0 : 20.0);
      return isInt ? Math.max(1, Math.round(s)) : Math.max(0.1, Math.round(s * 10.0) / 10.0);
    }
  }

  public record KnownGlyph(ResourceLocation id, String category, int cost, int level, boolean scales, List<ParamDef> params, int ritual) {
    /** IDENTIFIED (3) ou melhor: pode ser usado em inscricoes. */
    public boolean usable() { return level >= 3; }

    public Optional<ParamDef> primary() { return params.isEmpty() ? Optional.empty() : Optional.of(params.get(0)); }

    /** Segundo parametro (Ctrl + roda), se o glifo tiver mais de um (ex.: Juramento Personalizado: tipo e segundos). */
    public Optional<ParamDef> secondary() { return params.size() < 2 ? Optional.empty() : Optional.of(params.get(1)); }
  }

  /** Um glifo numa composicao: id e os parametros escolhidos (so os que diferem do padrao). */
  public record GlyphEntry(ResourceLocation id, Map<String, Double> params) {
    public GlyphEntry(ResourceLocation id) { this(id, Map.of()); }
  }

  public record SpellEntry(UUID id, String name, int cost, List<GlyphEntry> glyphs, int mode, boolean favorite, String folder, long lastUsed) {
    public SpellEntry(UUID id, String name, int cost, List<GlyphEntry> glyphs) { this(id, name, cost, glyphs, 0, false, "", 0L); }
  }

  private static float mana;
  private static float maxMana = 100f;
  private static double potency = 1.0;
  private static double poison = 0.0;
  private static boolean grimoirePresent = true;
  private static int totalGlyphs;
  private static int pages = 8;
  private static int stage = 1;
  private static int maxGlyphs = 10;
  private static double costGrowth = 1.3;
  private static int costFree = 3;
  private static List<KnownGlyph> glyphs = List.of();
  private static List<SpellEntry> spells = List.of();
  private static List<String> log = List.of();
  private static UUID active;
  private static List<List<UUID>> loadouts = List.of();
  private static int activeLoadout;
  private static boolean hasData;

  private ClientArcaneData() {}

  public static void apply(CompoundTag tag) {
    hasData = true;
    mana = tag.getFloat("mana");
    maxMana = Math.max(1f, tag.getFloat("maxMana"));
    potency = tag.getDouble("potency");
    poison = tag.getDouble("poison");
    grimoirePresent = !tag.contains("grimoire") || tag.getBoolean("grimoire");
    if (!tag.contains("full", Tag.TAG_COMPOUND)) return;

    CompoundTag full = tag.getCompound("full");
    totalGlyphs = full.getInt("total");
    pages = full.getInt("pages");
    stage = Math.max(1, full.getInt("stage"));
    if (full.contains("maxGlyphs")) {
      maxGlyphs = full.getInt("maxGlyphs");
      costGrowth = full.getDouble("costGrowth");
      costFree = full.getInt("costFree");
    }
    List<KnownGlyph> g = new ArrayList<>();
    for (Tag t : full.getList("glyphs", Tag.TAG_COMPOUND)) {
      CompoundTag c = (CompoundTag) t;
      List<ParamDef> params = new ArrayList<>();
      for (Tag pt : c.getList("params", Tag.TAG_COMPOUND)) {
        CompoundTag p = (CompoundTag) pt;
        params.add(new ParamDef(p.getString("k"), p.getDouble("d"), p.getDouble("lo"), p.getDouble("hi"), p.getBoolean("int")));
      }
      g.add(new KnownGlyph(new ResourceLocation(c.getString("id")),
          c.contains("category") ? c.getString("category") : "unknown", c.getInt("cost"), c.getInt("level"),
          c.getBoolean("scales"), List.copyOf(params), c.getInt("ritual")));
    }
    g.sort((a, b) -> {
      int cat = categoryOrder(a.category()) - categoryOrder(b.category());
      return cat != 0 ? cat : a.id().compareTo(b.id());
    });
    glyphs = Collections.unmodifiableList(g);

    List<SpellEntry> s = new ArrayList<>();
    for (Tag t : full.getList("spells", Tag.TAG_COMPOUND)) {
      CompoundTag c = (CompoundTag) t;
      List<GlyphEntry> entries = new ArrayList<>();
      for (Tag et : c.getList("glyphs", Tag.TAG_COMPOUND)) {
        CompoundTag e = (CompoundTag) et;
        Map<String, Double> params = new HashMap<>();
        CompoundTag p = e.getCompound("p");
        for (String key : p.getAllKeys()) params.put(key, p.getDouble(key));
        entries.add(new GlyphEntry(new ResourceLocation(e.getString("id")), Map.copyOf(params)));
      }
      s.add(new SpellEntry(c.getUUID("id"), c.getString("name"), c.getInt("cost"), List.copyOf(entries),
          c.getInt("mode"), c.getBoolean("fav"), c.getString("folder"), c.getLong("used")));
    }
    spells = Collections.unmodifiableList(s);
    active = full.hasUUID("active") ? full.getUUID("active") : null;
    List<List<UUID>> sets = new ArrayList<>();
    ListTag setTags = full.getList("loadouts", Tag.TAG_COMPOUND);
    for (int i = 0; i < setTags.size(); i++) {
      List<UUID> one = new ArrayList<>();
      for (Tag st : setTags.getCompound(i).getList("slots", Tag.TAG_INT_ARRAY)) one.add(net.minecraft.nbt.NbtUtils.loadUUID(st));
      sets.add(List.copyOf(one));
    }
    loadouts = List.copyOf(sets);
    activeLoadout = full.getInt("activeLoadout");

    List<String> lines = new ArrayList<>();
    ListTag logTag = full.getList("log", Tag.TAG_STRING);
    for (int i = 0; i < logTag.size(); i++) lines.add(logTag.getString(i));
    log = Collections.unmodifiableList(lines);
  }

  public static int categoryOrder(String category) {
    return switch (category) {
      case "form" -> 0;
      case "filter" -> 1;
      case "effect" -> 2;
      case "modifier" -> 3;
      case "condition" -> 4;
      case "activator" -> 5;
      case "ritual" -> 6;
      default -> 7;
    };
  }

  public static void reset() {
    hasData = false;
    glyphs = List.of();
    spells = List.of();
    log = List.of();
    active = null;
    loadouts = List.of();
    activeLoadout = 0;
    mana = 0f;
    totalGlyphs = 0;
  }

  /**
   * Custo de mana de uma composicao, igual ao calculo do servidor: (formas e modificadores + efeitos x pulsos da zona ou
   * do Dominio) x sobretaxa por glifo x area. A area aqui e estimada pelo raio do glifo de forma e pelos glifos Area.
   */
  public static double costOf(List<GlyphEntry> entries) {
    double other = 0, effects = 0;
    double areaRatio = 1.0;
    boolean regionForm = false;
    for (GlyphEntry e : entries) {
      Optional<KnownGlyph> known = glyph(e.id());
      if (known.isEmpty()) continue;
      KnownGlyph k = known.get();
      double cost = k.cost();
      if (k.scales() && k.primary().isPresent() && k.category().equals("effect")) {
        ParamDef p = k.primary().get();
        Double value = e.params().get(p.key());
        if (value != null && p.def() > 0) {
          cost *= com.arcane.inscription.SpellLimits.parameterFactor(p.clamp(value) / p.def());
        }
      }
      if (k.category().equals("form")) {
        ParamDef size = k.params().stream().filter(x -> x.key().equals("size")).findFirst().orElse(null);
        if (size != null) { // invocacoes: quantidade x tamanho x duracao
          double count = ratio(k, e, "count"), duration = ratio(k, e, "duration");
          cost *= com.arcane.inscription.SpellLimits.summonFactor(count, size.clamp(e.params().getOrDefault("size", size.def())), duration);
        }
        String path = e.id().getPath();
        if (path.equals("aura") || path.equals("barrier") || path.equals("pillar")) { regionForm = true; areaRatio *= ratio(k, e, "radius"); }
        else if (path.equals("wave")) { regionForm = true; areaRatio *= ratio(k, e, "range"); }
      }
      if (k.category().equals("effect")) effects += cost; else other += cost;
    }
    double seconds = 0, interval = 1.0;
    for (GlyphEntry e : entries) {
      String path = e.id().getPath();
      if (path.equals("area")) areaRatio *= 2.0;
      if (path.equals("more_range")) areaRatio *= 1.0; // alcance nao conta como area (so a Onda, abaixo)
      if (path.equals("zone") || path.equals("domain")) {
        seconds = e.params().getOrDefault("seconds", 10.0);
        interval = e.params().getOrDefault("interval", 1.0);
      }
    }
    double zone = com.arcane.inscription.SpellLimits.zoneMultiplier(seconds, interval);
    double area = regionForm ? com.arcane.inscription.SpellLimits.areaCostFactor(areaRatio) : 1.0;
    return Math.ceil((other + effects * zone) * costMultiplier(entries.size()) * area);
  }

  /** Valor escolhido de um parametro dividido pelo padrao do glifo (1 se o glifo nao tem o parametro). */
  private static double ratio(KnownGlyph k, GlyphEntry e, String key) {
    for (ParamDef p : k.params()) {
      if (p.key().equals(key) && p.def() > 0) return p.clamp(e.params().getOrDefault(key, p.def())) / p.def();
    }
    return 1.0;
  }

  public static int pages() { return pages; }
  public static int stage() { return stage; }
  public static int freePages() { return pages - spells.size(); }
  public static int maxGlyphs() { return maxGlyphs; }

  /** Sobretaxa exponencial por tamanho (igual ao servidor, com os valores que ele enviou). */
  public static double costMultiplier(int glyphCount) {
    return Math.pow(costGrowth, Math.max(0, glyphCount - costFree));
  }

  public static boolean hasData() { return hasData; }
  public static float mana() { return mana; }
  public static float maxMana() { return maxMana; }
  public static double potency() { return potency; }
  public static double poison() { return poison; }
  public static List<List<UUID>> loadouts() { return loadouts; }
  public static int activeLoadout() { return activeLoadout; }
  /** Previsao local da troca de conjunto (o servidor confirma na proxima sincronizacao). */
  public static void setActiveLoadoutLocal(int index) { activeLoadout = index; }

  /** Magias do conjunto ativo que ainda existem, na ordem dos slots. */
  public static List<SpellEntry> activeLoadoutSpells() {
    if (loadouts.isEmpty()) return List.of();
    List<SpellEntry> out = new ArrayList<>();
    for (UUID id : loadouts.get(Math.floorMod(activeLoadout, loadouts.size()))) {
      spells.stream().filter(s -> s.id().equals(id)).findFirst().ifPresent(out::add);
    }
    return out;
  }
  public static boolean grimoirePresent() { return grimoirePresent; }
  public static int totalGlyphs() { return totalGlyphs; }
  public static List<KnownGlyph> glyphs() { return glyphs; }
  public static List<SpellEntry> spells() { return spells; }
  public static List<String> log() { return log; }
  public static Optional<UUID> active() { return Optional.ofNullable(active); }

  public static Optional<KnownGlyph> glyph(ResourceLocation id) {
    return glyphs.stream().filter(g -> g.id().equals(id)).findFirst();
  }

  public static Optional<SpellEntry> activeSpell() {
    if (active == null) return Optional.empty();
    return spells.stream().filter(s -> s.id().equals(active)).findFirst();
  }

  // ---- Diario de descobertas (Fase K)
  private static List<com.arcane.network.packet.S2CReactionJournalPacket.Def> reactionCatalog = List.of();
  private static List<com.arcane.execution.ReactionJournal.Entry> reactionsFound = List.of();

  public static void setReactions(List<com.arcane.network.packet.S2CReactionJournalPacket.Def> catalog,
                                  List<com.arcane.execution.ReactionJournal.Entry> found) {
    reactionCatalog = List.copyOf(catalog);
    reactionsFound = List.copyOf(found);
  }

  public static List<com.arcane.network.packet.S2CReactionJournalPacket.Def> reactionCatalog() { return reactionCatalog; }

  public static List<com.arcane.execution.ReactionJournal.Entry> reactionsFound() { return reactionsFound; }

  // ---- previa da bancada (calculada pelo servidor)
  private static List<net.minecraft.network.chat.Component> preview = List.of();

  public static void setPreview(List<net.minecraft.network.chat.Component> lines) { preview = List.copyOf(lines); }
  public static List<net.minecraft.network.chat.Component> preview() { return preview; }

  /** Quantos avisos (linhas douradas) a previa tem. */
  public static int previewWarnings() {
    int n = 0;
    for (var c : preview) if (c.getStyle().getColor() != null && c.getStyle().getColor().getValue() == net.minecraft.ChatFormatting.GOLD.getColor()) n++;
    return n;
  }
}
