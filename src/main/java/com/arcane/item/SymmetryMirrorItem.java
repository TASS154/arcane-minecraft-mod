package com.arcane.item;

import com.arcane.block.ArcaneAltarBlock;
import com.arcane.ritual.RitualService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Espelho de Simetria: botao direito num Altar Arcano completa o circulo copiando os tracos simetricos. */
public class SymmetryMirrorItem extends Item {
  public SymmetryMirrorItem(Properties properties) {
    super(properties.stacksTo(1));
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    var level = context.getLevel();
    if (!(level.getBlockState(context.getClickedPos()).getBlock() instanceof ArcaneAltarBlock)) return InteractionResult.PASS;
    if (!(level instanceof ServerLevel server)) return InteractionResult.sidedSuccess(true);
    int placed = RitualService.repairBySymmetry(server, context.getClickedPos());
    if (context.getPlayer() != null) {
      context.getPlayer().displayClientMessage(Component.translatable(placed > 0 ? "message.arcane.mirror.repaired" : "message.arcane.mirror.nothing", placed)
          .withStyle(placed > 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY), true);
    }
    if (placed > 0) {
      server.playSound(null, context.getClickedPos(), com.arcane.registry.ModSounds.PEN_STROKE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f);
    }
    return InteractionResult.sidedSuccess(false);
  }
}
