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
    CastingService.tryCast(event.getEntity(), inscription, PrecisionSnapshot.FIELD);
    event.setCanceled(true);
  }

  @SubscribeEvent
  public static void onDrop(PlayerInteractEvent event) {
    // Scroll on drop handled via LivingEntity tick pickup if needed
  }
}
