package com.arcane.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Copia de exibicao do estado arcano no cliente (HUD e Bancada).
 * Sem imports de classes client-only, para poder ser referenciada pelo pacote de sync.
 */
public final class ClientArcaneData {
  public record KnownGlyph(ResourceLocation id, String category, int cost, int level) {
    /** IDENTIFIED (3) ou melhor: pode ser usado em inscricoes. */
    public boolean usable() { return level >= 3; }
  }

  public record SpellEntry(UUID id, String name, List<ResourceLocation> glyphs) {}

  private static float mana;
  private static float maxMana = 100f;
  private static double potency = 1.0;
  private static double poison = 0.0;
  private static boolean grimoirePresent = true;
  private static List<KnownGlyph> glyphs = List.of();
  private static List<SpellEntry> spells = List.of();
  private static UUID active;
  private static boolean hasData;

  private ClientArcaneData() {}

  public static void apply(CompoundTag tag) {
    hasData = true;
    mana = tag.getFloat("mana");
    maxMana = Math.max(1f, tag.getFloat("maxMana"));
    potency = tag.getDouble("potency");
    poison = tag.getDouble("poison");
    grimoirePresent = !tag.contains("grimoire") || tag.getBoolean("grimoire");
    if (tag.contains("full", Tag.TAG_COMPOUND)) {
      CompoundTag full = tag.getCompound("full");
      List<KnownGlyph> g = new ArrayList<>();
      for (Tag t : full.getList("glyphs", Tag.TAG_COMPOUND)) {
        CompoundTag c = (CompoundTag) t;
        g.add(new KnownGlyph(new ResourceLocation(c.getString("id")),
            c.contains("category") ? c.getString("category") : "unknown", c.getInt("cost"), c.getInt("level")));
      }
      g.sort((a, b) -> {
        int cat = categoryOrder(a.category()) - categoryOrder(b.category());
        return cat != 0 ? cat : a.id().compareTo(b.id());
      });
      glyphs = Collections.unmodifiableList(g);

      List<SpellEntry> s = new ArrayList<>();
      for (Tag t : full.getList("spells", Tag.TAG_COMPOUND)) {
        CompoundTag c = (CompoundTag) t;
        List<ResourceLocation> ids = new ArrayList<>();
        ListTag list = c.getList("glyphs", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) ids.add(new ResourceLocation(list.getString(i)));
        s.add(new SpellEntry(c.getUUID("id"), c.getString("name"), List.copyOf(ids)));
      }
      spells = Collections.unmodifiableList(s);
      active = full.hasUUID("active") ? full.getUUID("active") : null;
    }
  }

  public static int categoryOrder(String category) {
    return switch (category) {
      case "form" -> 0;
      case "filter" -> 1;
      case "effect" -> 2;
      case "modifier" -> 3;
      case "condition" -> 4;
      case "activator" -> 5;
      default -> 6;
    };
  }

  public static void reset() {
    hasData = false;
    glyphs = List.of();
    spells = List.of();
    active = null;
    mana = 0f;
  }

  public static boolean hasData() { return hasData; }
  public static float mana() { return mana; }
  public static float maxMana() { return maxMana; }
  public static double potency() { return potency; }
  public static double poison() { return poison; }
  public static boolean grimoirePresent() { return grimoirePresent; }
  public static List<KnownGlyph> glyphs() { return glyphs; }
  public static List<SpellEntry> spells() { return spells; }
  public static Optional<UUID> active() { return Optional.ofNullable(active); }

  public static Optional<KnownGlyph> glyph(ResourceLocation id) {
    return glyphs.stream().filter(g -> g.id().equals(id)).findFirst();
  }

  public static Optional<SpellEntry> activeSpell() {
    if (active == null) return Optional.empty();
    return spells.stream().filter(s -> s.id().equals(active)).findFirst();
  }
}
