package com.arcane.inscribing;

import com.arcane.execution.CastingService;
import com.arcane.execution.PrecisionSnapshot;
import com.arcane.inscription.Inscription;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.arcane.ArcaneMod;

@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public class ScrollHandler {
  @SubscribeEvent
  public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
    ItemStack stack = event.getItemStack();
    if (!ItemInscriptionHelper.isScroll(stack)) return;
    Inscription inscription = ItemInscriptionHelper.read(stack);
    if (inscription == null || event.getLevel().isClientSide) return;
    if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
      if (serverPlayer.isShiftKeyDown()) {
        study(serverPlayer, stack, inscription);
      } else {
        CastingService.tryCast(serverPlayer, inscription, PrecisionSnapshot.of(inscription));
      }
    }
    event.setCanceled(true);
  }

  /** Shift + botao direito: copia a formula para o grimorio (se houver pagina) e passa a suspeitar dos glifos. */
  private static void study(net.minecraft.server.level.ServerPlayer player, ItemStack stack, Inscription inscription) {
    com.arcane.grimoire.GrimoireAttachments.get(player).ifPresent(grimoire -> {
      var result = com.arcane.grimoire.GrimoireTransfer.studySpell(grimoire, inscription);
      if (result.skippedKnown() > 0) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.arcane.scroll.already_known"), true);
        return;
      }
      stack.shrink(1);
      player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.arcane.grimoire.studied",
          result.added(), result.suspected(), result.skippedNoPages()), false);
      com.arcane.network.ArcaneSync.sync(player, true);
    });
  }

  @SubscribeEvent
  public static void onDrop(PlayerInteractEvent event) {
    // Scroll on drop handled via LivingEntity tick pickup if needed
  }
}
