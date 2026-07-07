package com.arcane.item;

import net.minecraft.world.item.Item;

public class InkItem extends Item {
  private final String inkId;

  public InkItem(Properties properties, String inkId) {
    super(properties);
    this.inkId = inkId;
  }

  public String inkId() {
    return inkId;
  }
}
