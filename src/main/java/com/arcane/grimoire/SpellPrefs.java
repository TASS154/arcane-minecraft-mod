package com.arcane.grimoire;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Organizacao do grimorio por jogador: modo de conjuracao, favoritos, pasta e uso recente de cada magia, alem dos
 * conjuntos (loadouts) de magias para troca rapida. Tudo e escolha do jogador e validado aqui (limites e ids).
 */
public final class SpellPrefs {
  /** Como a tecla de conjurar age: toque (uma vez), carregar (solta para lancar), segurar (repete) ou alternar (liga/desliga). */
  public enum CastMode {
    TAP, CHARGE, HOLD, TOGGLE;

    public static CastMode of(int ordinal) {
      return ordinal < 0 || ordinal >= values().length ? TAP : values()[ordinal];
    }
  }

  public static final class Pref {
    public CastMode mode = CastMode.TAP;
    public boolean favorite;
    public String folder = "";
    public long lastUsed;
  }

  public static final int LOADOUTS = 4, SLOTS = 6, MAX_FOLDER = 12;

  private final Map<UUID, Pref> prefs = new HashMap<>();
  private final List<List<UUID>> loadouts = new ArrayList<>();
  private int activeLoadout;

  public SpellPrefs() {
    for (int i = 0; i < LOADOUTS; i++) loadouts.add(new ArrayList<>());
  }

  public Pref get(UUID id) { return prefs.getOrDefault(id, DEFAULT); }

  private static final Pref DEFAULT = new Pref();

  public CastMode mode(UUID id) { return get(id).mode; }

  private Pref edit(UUID id) { return prefs.computeIfAbsent(id, k -> new Pref()); }

  public void setMode(UUID id, CastMode mode) { edit(id).mode = mode; }

  public void setFavorite(UUID id, boolean favorite) { edit(id).favorite = favorite; }

  public void setFolder(UUID id, String folder) {
    String clean = folder == null ? "" : folder.strip();
    edit(id).folder = clean.length() > MAX_FOLDER ? clean.substring(0, MAX_FOLDER) : clean;
  }

  public void touch(UUID id, long gameTime) { edit(id).lastUsed = gameTime; }

  public int activeLoadout() { return activeLoadout; }

  public void setActiveLoadout(int index) { activeLoadout = Math.floorMod(index, LOADOUTS); }

  public List<UUID> loadout(int index) { return java.util.Collections.unmodifiableList(loadouts.get(Math.floorMod(index, LOADOUTS))); }

  /** Poe a magia no conjunto (se couber) ou tira (se ja estava). Retorna true se ela ficou no conjunto. */
  public boolean toggleInLoadout(int index, UUID id) {
    List<UUID> set = loadouts.get(Math.floorMod(index, LOADOUTS));
    if (set.remove(id)) return false;
    if (set.size() >= SLOTS) return false;
    set.add(id);
    return true;
  }

  public void forget(UUID id) {
    prefs.remove(id);
    loadouts.forEach(l -> l.remove(id));
  }

  public void copyFrom(SpellPrefs other, java.util.Set<UUID> valid) {
    other.prefs.forEach((id, p) -> { if (valid.contains(id) && !prefs.containsKey(id)) prefs.put(id, p); });
  }

  public void save(CompoundTag tag) {
    ListTag list = new ListTag();
    prefs.forEach((id, p) -> {
      CompoundTag c = new CompoundTag();
      c.putUUID("id", id);
      c.putInt("mode", p.mode.ordinal());
      c.putBoolean("fav", p.favorite);
      c.putString("folder", p.folder);
      c.putLong("used", p.lastUsed);
      list.add(c);
    });
    tag.put("prefs", list);
    ListTag sets = new ListTag();
    for (List<UUID> set : loadouts) {
      ListTag one = new ListTag();
      set.forEach(id -> one.add(net.minecraft.nbt.NbtUtils.createUUID(id)));
      CompoundTag holder = new CompoundTag();
      holder.put("slots", one);
      sets.add(holder);
    }
    tag.put("loadouts", sets);
    tag.putInt("activeLoadout", activeLoadout);
  }

  public void load(CompoundTag tag) {
    prefs.clear();
    loadouts.forEach(List::clear);
    for (Tag t : tag.getList("prefs", Tag.TAG_COMPOUND)) {
      CompoundTag c = (CompoundTag) t;
      if (!c.hasUUID("id")) continue;
      Pref p = new Pref();
      p.mode = CastMode.of(c.getInt("mode"));
      p.favorite = c.getBoolean("fav");
      p.folder = c.getString("folder");
      p.lastUsed = c.getLong("used");
      prefs.put(c.getUUID("id"), p);
    }
    ListTag sets = tag.getList("loadouts", Tag.TAG_COMPOUND);
    for (int i = 0; i < Math.min(LOADOUTS, sets.size()); i++) {
      for (Tag s : sets.getCompound(i).getList("slots", Tag.TAG_INT_ARRAY)) {
        if (loadouts.get(i).size() < SLOTS) loadouts.get(i).add(net.minecraft.nbt.NbtUtils.loadUUID(s));
      }
    }
    activeLoadout = Math.floorMod(tag.getInt("activeLoadout"), LOADOUTS);
  }
}
