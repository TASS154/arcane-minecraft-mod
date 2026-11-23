package com.arcane.item;

import net.minecraft.world.item.Item;

/** Caneta de Criatura (so por loot): desenha numa criatura, que passa a disparar a magia sempre que atacar. */
public class CreaturePenItem extends Item {
  public CreaturePenItem(Properties properties) {
    super(properties.stacksTo(1));
  }
}
