package com.arcane.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Material da Mascara Arcana e do Manto Isolante. Protecao fisica baixa de proposito:
 * o valor destas pecas esta na protecao contra Mana Poisoning (ver ManaPoisoningService).
 * Textura: assets/arcane/textures/models/armor/arcane_weave_layer_1.png
 */
public enum ArcaneArmorMaterial implements ArmorMaterial {
  ARCANE_WEAVE;

  @Override
  public int getDurabilityForType(ArmorItem.Type type) {
    return switch (type) {
      case HELMET -> 110;
      case CHESTPLATE -> 160;
      case LEGGINGS -> 150;
      case BOOTS -> 130;
    };
  }

  @Override
  public int getDefenseForType(ArmorItem.Type type) {
    return switch (type) {
      case HELMET -> 1;
      case CHESTPLATE -> 2;
      case LEGGINGS -> 2;
      case BOOTS -> 1;
    };
  }

  @Override
  public int getEnchantmentValue() { return 20; }

  @Override
  public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }

  @Override
  public Ingredient getRepairIngredient() { return Ingredient.of(Items.AMETHYST_SHARD); }

  @Override
  public String getName() { return "arcane:arcane_weave"; }

  @Override
  public float getToughness() { return 0f; }

  @Override
  public float getKnockbackResistance() { return 0f; }
}
