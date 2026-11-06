package com.arcane.item;

import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.grimoire.GrimoireState;
import com.arcane.network.ArcaneSync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Pagina Arcana: botao direito acrescenta paginas ao grimorio (capacidade para mais magias). */
public class ArcanePageItem extends Item {
  public static final int PAGES_PER_ITEM = 2;

  public ArcanePageItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
    if (player instanceof ServerPlayer serverPlayer) {
      var grimoire = GrimoireAttachments.get(serverPlayer);
      if (grimoire.isPresent()) {
        if (grimoire.get().pages() >= GrimoireState.MAX_PAGES) {
          serverPlayer.displayClientMessage(Component.translatable("message.arcane.page.max").withStyle(ChatFormatting.GRAY), true);
          return InteractionResultHolder.fail(stack);
        }
        int before = grimoire.get().visualStage();
        grimoire.get().addPages(PAGES_PER_ITEM);
        stack.shrink(1);
        serverPlayer.displayClientMessage(Component.translatable("message.arcane.page.added", PAGES_PER_ITEM,
            grimoire.get().pages()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        if (grimoire.get().visualStage() > before) {
          serverPlayer.displayClientMessage(Component.translatable("message.arcane.grimoire.stage_up",
              Component.translatable("grimoire.arcane.stage." + grimoire.get().visualStage())).withStyle(ChatFormatting.GOLD), false);
        }
        ArcaneSync.sync(serverPlayer, true);
      }
    }
    return InteractionResultHolder.sidedSuccess(stack, false);
  }
}
