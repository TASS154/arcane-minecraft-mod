package com.arcane.execution;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lista de convidados de cada jogador (salva no mundo). O filtro "Forasteiros" deixa de fora o dono da magia, os
 * convidados dele e os animais domesticados (ou invocados) de qualquer um dos dois.
 */
public class GuestLists extends SavedData {
  private static final String DATA_NAME = "arcane_guests";
  private final Map<UUID, Set<UUID>> guests = new HashMap<>();

  public static GuestLists get(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(GuestLists::load, GuestLists::new, DATA_NAME);
  }

  public static GuestLists load(CompoundTag tag) {
    GuestLists lists = new GuestLists();
    for (Tag t : tag.getList("owners", Tag.TAG_COMPOUND)) {
      CompoundTag c = (CompoundTag) t;
      if (!c.hasUUID("owner")) continue;
      Set<UUID> set = new HashSet<>();
      for (Tag g : c.getList("guests", Tag.TAG_INT_ARRAY)) set.add(NbtUtils.loadUUID(g));
      lists.guests.put(c.getUUID("owner"), set);
    }
    return lists;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag owners = new ListTag();
    guests.forEach((owner, set) -> {
      if (set.isEmpty()) return;
      CompoundTag c = new CompoundTag();
      c.putUUID("owner", owner);
      ListTag list = new ListTag();
      set.forEach(g -> list.add(NbtUtils.createUUID(g)));
      c.put("guests", list);
      owners.add(c);
    });
    tag.put("owners", owners);
    return tag;
  }

  public Set<UUID> of(UUID owner) { return java.util.Collections.unmodifiableSet(guests.getOrDefault(owner, Set.of())); }

  public boolean add(UUID owner, UUID guest) {
    if (owner.equals(guest)) return false;
    boolean changed = guests.computeIfAbsent(owner, k -> new HashSet<>()).add(guest);
    if (changed) setDirty();
    return changed;
  }

  public boolean remove(UUID owner, UUID guest) {
    Set<UUID> set = guests.get(owner);
    boolean changed = set != null && set.remove(guest);
    if (changed) setDirty();
    return changed;
  }

  /** O alvo e o dono, um convidado ou pertence (domesticado/invocado) a um deles? */
  public boolean isFriendly(UUID owner, Entity entity) {
    Set<UUID> set = guests.getOrDefault(owner, Set.of());
    if (entity.getUUID().equals(owner) || set.contains(entity.getUUID())) return true;
    if (entity instanceof OwnableEntity pet) {
      UUID master = pet.getOwnerUUID();
      return master != null && (master.equals(owner) || set.contains(master));
    }
    return false;
  }
}
