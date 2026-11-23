package com.arcane.item;

import com.arcane.config.ArcaneConfig;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Pote de tinta. Cada pote tem uma quantidade (inkPotUnits) e cada glifo desenhado gasta algumas unidades; um pote
 * usado pela metade guarda o que sobrou (nbt "ink_units"). Pote sem nbt esta cheio.
 */
public class InkItem extends Item {
  private static final String UNITS = "ink_units";
  private final String inkId;

  public InkItem(Properties properties, String inkId) {
    super(properties);
    this.inkId = inkId;
  }

  public String inkId() {
    return inkId;
  }

  public static int potUnits() {
    return ArcaneConfig.INK_POT_UNITS.get();
  }

  public static int units(ItemStack stack) {
    return stack.hasTag() && stack.getTag().contains(UNITS) ? Math.max(0, stack.getTag().getInt(UNITS)) : potUnits();
  }

  private static void setUnits(ItemStack stack, int units) {
    stack.getOrCreateTag().putInt(UNITS, units);
  }

  /** Unidades de tinta que um desenho com {@code glyphs} glifos custa. */
  public static int cost(int glyphs) {
    return Math.max(1, glyphs) * ArcaneConfig.INK_UNITS_PER_GLYPH.get();
  }

  private static boolean isInk(ItemStack s, String inkId) {
    return !s.isEmpty() && s.getItem() instanceof InkItem k && k.inkId().equals(inkId);
  }

  /** Total de unidades desta tinta no inventario do jogador. */
  public static int total(Player player, String inkId) {
    int sum = 0;
    Inventory inv = player.getInventory();
    for (int i = 0; i < inv.getContainerSize(); i++) {
      ItemStack s = inv.getItem(i);
      if (isInk(s, inkId)) sum += units(s) * s.getCount();
    }
    return sum;
  }

  /** Gasta {@code amount} unidades: usa antes os potes pela metade e so depois abre um pote cheio. Falso (sem gastar) se nao ha tanto. */
  public static boolean consume(Player player, String inkId, int amount) {
    if (total(player, inkId) < amount) return false;
    Inventory inv = player.getInventory();
    int left = amount;
    for (int pass = 0; pass < 2 && left > 0; pass++) {
      for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
        ItemStack s = inv.getItem(i);
        if (!isInk(s, inkId)) continue;
        boolean partial = s.hasTag() && s.getTag().contains(UNITS);
        if ((pass == 0) != partial) continue;
        while (left > 0 && !s.isEmpty()) {
          int u = units(s);
          if (u > left) {
            if (s.getCount() == 1) {
              setUnits(s, u - left);
            } else {
              s.shrink(1);
              ItemStack rest = s.copyWithCount(1);
              setUnits(rest, u - left);
              if (!inv.add(rest)) player.drop(rest, false);
            }
            left = 0;
          } else {
            left -= u;
            s.shrink(1);
          }
        }
      }
    }
    return true;
  }
}
