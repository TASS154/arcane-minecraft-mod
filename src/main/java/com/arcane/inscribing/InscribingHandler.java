package com.arcane.inscribing;

import com.arcane.execution.CastingService;
import com.arcane.execution.PrecisionSnapshot;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionValidator;
import com.arcane.item.InkItem;
import com.arcane.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class InscribingHandler {
  /** Pena na mao principal + tinta na secundaria: escreve (ou apaga, agachado) em vez de usar o bloco. */
  @SubscribeEvent(priority = EventPriority.HIGH)
  public void onPenUse(PlayerInteractEvent.RightClickBlock event) {
    Player player = event.getEntity();
    if (event.getHand() != InteractionHand.MAIN_HAND || !player.getMainHandItem().is(ModItems.ARCANE_PEN.get())) return;
    ItemStack ink = player.getOffhandItem();
    boolean erasing = player.isShiftKeyDown();
    if (!erasing && !(ink.getItem() instanceof InkItem)) {
      if (!event.getLevel().isClientSide) {
        player.displayClientMessage(Component.translatable("message.arcane.pen.need_ink").withStyle(ChatFormatting.GRAY), true);
      }
      return;
    }
    event.setCanceled(true);
    event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    if (event.getLevel().isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;

    ServerLevel level = (ServerLevel) event.getLevel();
    BlockPos pos = event.getPos();
    if (erasing) {
      boolean removed = InscribingService.erase(level, pos);
      player.displayClientMessage(Component.translatable(removed ? "message.arcane.pen.erased" : "message.arcane.pen.nothing")
          .withStyle(ChatFormatting.GRAY), true);
      return;
    }

    Optional<Inscription> active = GrimoireAttachments.get(serverPlayer).flatMap(g -> g.activeInscriptionId().flatMap(g::getInscription));
    if (active.isEmpty()) {
      player.displayClientMessage(Component.translatable("hud.arcane.no_spell").withStyle(ChatFormatting.RED), true);
      return;
    }
    InscriptionValidator.ValidationResult validation = InscriptionValidator.validate(active.get(), GrimoireAttachments.get(serverPlayer));
    if (!validation.valid()) {
      player.displayClientMessage(Component.translatable("message.arcane.workbench.invalid", validation.message())
          .withStyle(ChatFormatting.RED), true);
      return;
    }

    BlockEntity be = level.getBlockEntity(pos);
    String trigger = be instanceof ChestBlockEntity ? InscribingService.TRIGGER_ON_OPEN : InscribingService.TRIGGER_ON_TOUCH;
    ResourceLocation inkId = new ResourceLocation("arcane", ((InkItem) ink.getItem()).inkId());
    if (InscribingService.inscribe(level, player, pos, active.get().glyphs(), inkId, trigger, active.get().metadata().name())) {
      if (!player.getAbilities().instabuild) ink.shrink(1);
      player.displayClientMessage(Component.translatable("message.arcane.pen.written." + trigger)
          .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }
  }

  /** Bau inscrito: abrir dispara a inscricao (custo e precisao de quem abre, comportamento original). */
  @SubscribeEvent
  public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    if (event.getLevel().isClientSide || event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND) return;
    Player player = event.getEntity();
    BlockPos pos = event.getPos();
    WorldInscriptionIndex index = WorldInscriptionIndex.get((ServerLevel) event.getLevel());
    Optional<InscriptionAnchor> anchor = index.get(pos);
    if (anchor.isEmpty()) return;

    Inscription inscription = anchor.get().inscription();
    if (InscribingService.TRIGGER_ON_OPEN.equals(anchor.get().triggerType())
        && event.getLevel().getBlockEntity(pos) instanceof ChestBlockEntity
        && player instanceof ServerPlayer serverPlayer) {
      CastingService.tryCast(serverPlayer, inscription, PrecisionSnapshot.WORKBENCH);
    }
  }

  /** Quebrar o bloco apaga a inscricao. */
  @SubscribeEvent
  public void onBreak(BlockEvent.BreakEvent event) {
    if (event.getLevel() instanceof ServerLevel level) {
      InscribingService.erase(level, event.getPos());
    }
  }
}
