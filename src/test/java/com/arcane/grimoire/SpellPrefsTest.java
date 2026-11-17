package com.arcane.grimoire;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpellPrefsTest {
  @Test
  void roundTripKeepsModesFavoritesFoldersAndSets() {
    SpellPrefs a = new SpellPrefs();
    UUID x = UUID.randomUUID(), y = UUID.randomUUID();
    a.setMode(x, SpellPrefs.CastMode.CHARGE);
    a.setFavorite(x, true);
    a.setFolder(x, "fogo");
    a.touch(y, 1234L);
    a.toggleInLoadout(1, x);
    a.toggleInLoadout(1, y);
    a.setActiveLoadout(1);
    CompoundTag tag = new CompoundTag();
    a.save(tag);
    SpellPrefs b = new SpellPrefs();
    b.load(tag);
    assertEquals(SpellPrefs.CastMode.CHARGE, b.mode(x));
    assertTrue(b.get(x).favorite);
    assertEquals("fogo", b.get(x).folder);
    assertEquals(1234L, b.get(y).lastUsed);
    assertEquals(java.util.List.of(x, y), b.loadout(1));
    assertEquals(1, b.activeLoadout());
  }

  @Test
  void setsHoldAtMostSixAndToggleRemoves() {
    SpellPrefs p = new SpellPrefs();
    UUID[] ids = new UUID[7];
    for (int i = 0; i < 7; i++) { ids[i] = UUID.randomUUID(); p.toggleInLoadout(0, ids[i]); }
    assertEquals(SpellPrefs.SLOTS, p.loadout(0).size());
    assertFalse(p.loadout(0).contains(ids[6]), "a setima nao cabe");
    assertFalse(p.toggleInLoadout(0, ids[0]), "tirar devolve false");
    assertEquals(5, p.loadout(0).size());
  }

  @Test
  void folderIsTrimmedAndLimited() {
    SpellPrefs p = new SpellPrefs();
    UUID id = UUID.randomUUID();
    p.setFolder(id, "   uma pasta muito comprida mesmo   ");
    assertTrue(p.get(id).folder.length() <= SpellPrefs.MAX_FOLDER);
    assertFalse(p.get(id).folder.startsWith(" "));
  }

  @Test
  void forgetRemovesFromEverywhere() {
    SpellPrefs p = new SpellPrefs();
    UUID id = UUID.randomUUID();
    p.setFavorite(id, true);
    p.toggleInLoadout(3, id);
    p.forget(id);
    assertFalse(p.get(id).favorite);
    assertTrue(p.loadout(3).isEmpty());
  }

  @Test
  void invalidModeFallsBackToTap() {
    assertEquals(SpellPrefs.CastMode.TAP, SpellPrefs.CastMode.of(99));
    assertEquals(SpellPrefs.CastMode.TOGGLE, SpellPrefs.CastMode.of(3));
  }
}
