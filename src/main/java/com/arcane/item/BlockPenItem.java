package com.arcane.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Caneta de bloco: escreve so em blocos do seu material (a lista de blocos de cada grupo e uma tag de dados,
 * data/arcane/tags/blocks/pen_<grupo>.json, editavel por datapack). Terra, pedra, metal, neve e madeira.
 */
public class BlockPenItem extends Item {
  private final String group;
  private final TagKey<Block> tag;

  public BlockPenItem(Properties properties, String group) {
    super(properties.stacksTo(1));
    this.group = group;
    this.tag = TagKey.create(Registries.BLOCK, new ResourceLocation("arcane", "pen_" + group));
  }

  public String group() {
    return group;
  }

  public boolean canWriteOn(BlockState state) {
    return state.is(tag);
  }
}
