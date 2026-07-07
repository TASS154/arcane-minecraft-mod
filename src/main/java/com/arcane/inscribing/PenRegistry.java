package com.arcane.inscribing;

import com.arcane.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PenRegistry {
  private PenRegistry() {}

  public static boolean canWriteOnBlock(Player player, BlockPos pos, ServerLevel level) {
    ItemStack held = player.getMainHandItem();
    return held.is(ModItems.ARCANE_PEN.get());
  }
}
