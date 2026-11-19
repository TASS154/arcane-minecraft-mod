package com.arcane.item;

import com.arcane.inscribing.ItemDrawings;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Caneta de objeto: escreve so em objetos da sua classe (comida, arma ou pocao). O item fica na mao secundaria. */
public class ObjectPenItem extends Item {
  private final ItemDrawings.Kind kind;

  public ObjectPenItem(Properties properties, ItemDrawings.Kind kind) {
    super(properties.stacksTo(1));
    this.kind = kind;
  }

  public ItemDrawings.Kind kind() {
    return kind;
  }

  public boolean canWriteOn(ItemStack stack) {
    return !stack.isEmpty() && ItemDrawings.kindOf(stack) == kind;
  }
}
