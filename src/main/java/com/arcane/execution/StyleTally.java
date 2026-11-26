package com.arcane.execution;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Conta, so para a assinatura visual (Fase M), quantas vezes o jogador conjurou cada elemento. Nao da bonus nenhum: o
 * sistema de estilo com bonus de docs/ESTILO_UNICO_DESENHO.md continua esperando aprovacao. Fica nos dados que
 * sobrevivem a morte ({@code PlayerPersisted}).
 */
public final class StyleTally {
  private StyleTally() {}

  public static final String KEY = "arcane_style_tally";

  private static CompoundTag tag(Player p) {
    CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    CompoundTag t = persisted.getCompound(KEY);
    persisted.put(KEY, t);
    p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    return t;
  }

  public static void count(Player p, SpellElement e) {
    CompoundTag t = tag(p);
    t.putInt(e.name(), Math.min(1_000_000, t.getInt(e.name()) + 1));
  }

  /** Os dois elementos mais conjurados (empate: o de menor ordem); null se nunca conjurou. */
  public static SpellElement[] top(Player p) {
    CompoundTag t = tag(p);
    SpellElement first = null, second = null;
    int a = 0, b = 0;
    for (SpellElement e : SpellElement.values()) {
      int n = t.getInt(e.name());
      if (n > a) { second = first; b = a; first = e; a = n; }
      else if (n > b) { second = e; b = n; }
    }
    return first == null ? null : new SpellElement[] {first, second};
  }
}
