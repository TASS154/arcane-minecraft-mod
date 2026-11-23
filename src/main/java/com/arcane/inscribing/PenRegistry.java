package com.arcane.inscribing;

import com.arcane.item.AirPenItem;
import com.arcane.item.BlockPenItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PenRegistry {
  private PenRegistry() {}

  /**
   * Caneta do material escreve em blocos do seu grupo; a Caneta de Fenix escreve no ar (num lugar vazio).
   * A Pena Arcana comum escreve em itens, papel e no circulo do altar, nao em blocos.
   */
  public static boolean canWriteOnBlock(Player player, BlockPos pos, ServerLevel level) {
    ItemStack held = player.getMainHandItem();
    if (held.getItem() instanceof AirPenItem) return level.getBlockState(pos).isAir();
    return held.getItem() instanceof BlockPenItem pen && pen.canWriteOn(level.getBlockState(pos));
  }

  public static boolean isAirDrawing(InscriptionAnchor anchor) {
    return "phoenix_pen".equals(anchor.inscription().metadata().pen().getPath());
  }
}
