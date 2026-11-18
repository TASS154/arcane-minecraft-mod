package com.arcane.ritual;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.List;
import java.util.Map;

/**
 * Valor de uma oferenda do altar, em PORCENTAGEM do poder do ritual. Qualquer item vale algo: itens preciosos tem valor
 * proprio, o resto vale pela raridade, e cada encantamento soma um pouco. O valor das oferendas soma ao valor do ritual
 * pelo tamanho do circulo (menor 150%, medio 300%, maior 1000%); passando de {@link #LEGENDARY_THRESHOLD} so nas oferendas
 * o ritual e lendario e o total dobra (limite em {@link RitualSettings#maxPercent()}).
 */
public final class OfferingValue {
  /** Acima disto (so nas oferendas) o ritual e lendario. */
  public static final int LEGENDARY_THRESHOLD = 300;
  public static final int MAX_ITEMS = 3;

  private static final Map<Item, Integer> PRECIOUS = Map.ofEntries(
      Map.entry(Items.NETHER_STAR, 310), // sozinha ja e lendaria, como era a oferenda original
      Map.entry(Items.DRAGON_EGG, 400),
      Map.entry(Items.ENCHANTED_GOLDEN_APPLE, 200),
      Map.entry(Items.TOTEM_OF_UNDYING, 150),
      Map.entry(Items.HEART_OF_THE_SEA, 120),
      Map.entry(Items.NETHERITE_INGOT, 110),
      Map.entry(Items.NETHERITE_SCRAP, 40),
      Map.entry(Items.BEACON, 160),
      Map.entry(Items.ELYTRA, 140),
      Map.entry(Items.DIAMOND_BLOCK, 160),
      Map.entry(Items.DIAMOND, 30),
      Map.entry(Items.EMERALD_BLOCK, 100),
      Map.entry(Items.EMERALD, 12),
      Map.entry(Items.GOLD_BLOCK, 60),
      Map.entry(Items.GOLD_INGOT, 7),
      Map.entry(Items.IRON_BLOCK, 30),
      Map.entry(Items.IRON_INGOT, 4),
      Map.entry(Items.AMETHYST_SHARD, 6),
      Map.entry(Items.ECHO_SHARD, 60),
      Map.entry(Items.ENDER_EYE, 25),
      Map.entry(Items.ENDER_PEARL, 12),
      Map.entry(Items.BLAZE_ROD, 10),
      Map.entry(Items.GHAST_TEAR, 22),
      Map.entry(Items.WITHER_SKELETON_SKULL, 120),
      Map.entry(Items.SHULKER_SHELL, 35),
      Map.entry(Items.LAPIS_LAZULI, 3),
      Map.entry(Items.REDSTONE, 2),
      Map.entry(Items.BONE, 2),
      Map.entry(Items.COAL, 2));

  private OfferingValue() {}

  /** Valor, em %, de UM item desta pilha (cada oferenda e uma unidade). */
  public static int of(ItemStack stack) {
    if (stack.isEmpty()) return 0;
    int base = PRECIOUS.getOrDefault(stack.getItem(), switch (stack.getRarity()) {
      case COMMON -> 1;
      case UNCOMMON -> 10;
      case RARE -> 30;
      case EPIC -> 80;
    });
    int enchants = 0;
    for (int level : EnchantmentHelper.getEnchantments(stack).values()) enchants += 4 + level * 3;
    return base + enchants;
  }

  public static int sum(List<ItemStack> stacks) {
    int total = 0;
    for (ItemStack s : stacks) total += of(s);
    return total;
  }

  public static boolean legendary(int offeringPercent) { return offeringPercent > LEGENDARY_THRESHOLD; }

  /**
   * Valor total do ritual em %: tamanho do circulo + oferendas, dobrado se as oferendas o tornam lendario, limitado ao teto
   * da config. {@code sizePercent} e o poder do nivel do circulo (150/300/1000).
   */
  public static int total(int sizePercent, int offeringPercent) {
    long total = (long) sizePercent + offeringPercent;
    if (legendary(offeringPercent)) total *= 2;
    return (int) Math.min(total, RitualSettings.maxPercent());
  }
}
