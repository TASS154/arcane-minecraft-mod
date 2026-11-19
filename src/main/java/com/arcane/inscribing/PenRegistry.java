package com.arcane.inscribing;

import com.arcane.item.BlockPenItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PenRegistry {
  private PenRegistry() {}

  /** So a caneta do material do bloco escreve nele (a Pena Arcana comum escreve em itens, papel e no ar, nao em blocos). */
  public static boolean canWriteOnBlock(Player player, BlockPos pos, ServerLevel level) {
    ItemStack held = player.getMainHandItem();
    return held.getItem() instanceof BlockPenItem pen && pen.canWriteOn(level.getBlockState(pos));
  }
}
