package com.arcane.ritual;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** De onde saem os componentes fisicos do ritual: inventario do jogador, um mapa (testes) ou ilimitado (criativo). */
public interface ComponentSource {
  int count(Item item);

  void take(Item item, int amount);

  static ComponentSource of(Player player) {
    if (player.getAbilities().instabuild) return unlimited();
    return new ComponentSource() {
      @Override
      public int count(Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) if (stack.is(item)) total += stack.getCount();
        return total;
      }

      @Override
      public void take(Item item, int amount) {
        int left = amount;
        for (ItemStack stack : player.getInventory().items) {
          if (left <= 0) break;
          if (stack.is(item)) {
            int n = Math.min(left, stack.getCount());
            stack.shrink(n);
            left -= n;
          }
        }
      }
    };
  }

  static ComponentSource of(Map<Item, Integer> items) {
    Map<Item, Integer> stock = new HashMap<>(items);
    return new ComponentSource() {
      @Override
      public int count(Item item) {
        return stock.getOrDefault(item, 0);
      }

      @Override
      public void take(Item item, int amount) {
        stock.merge(item, -amount, Integer::sum);
      }
    };
  }

  static ComponentSource unlimited() {
    return new ComponentSource() {
      @Override
      public int count(Item item) {
        return Integer.MAX_VALUE / 2;
      }

      @Override
      public void take(Item item, int amount) {}
    };
  }
}
