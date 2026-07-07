package com.arcane.item;

import com.arcane.discovery.DiscoveryService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GlyphNoteItem extends Item {
  public GlyphNoteItem(Properties properties) {
    super(properties);
  }

  public static ItemStack createForGlyph(ResourceLocation glyphId) {
    ItemStack stack = new ItemStack(com.arcane.registry.ModItems.GLYPH_NOTE.get());
    stack.getOrCreateTag().putString("glyph", glyphId.toString());
    return stack;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!level.isClientSide && stack.hasTag() && stack.getTag().contains("glyph")) {
      ResourceLocation glyph = new ResourceLocation(stack.getTag().getString("glyph"));
      DiscoveryService.learnFromLoot(player, glyph);
      stack.shrink(1);
      return InteractionResultHolder.success(stack);
    }
    return InteractionResultHolder.pass(stack);
  }
}
