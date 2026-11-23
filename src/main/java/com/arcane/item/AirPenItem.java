package com.arcane.item;

import net.minecraft.world.item.Item;

/** Caneta de Fenix: desenha no ar um circulo flutuante que dispara a magia quando uma criatura chega perto. */
public class AirPenItem extends Item {
  public AirPenItem(Properties properties) {
    super(properties.stacksTo(1));
  }
}
