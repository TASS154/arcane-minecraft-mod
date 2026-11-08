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
    // Caneta num Altar Arcano (sem agachar): desenha o circulo do ritual a mao, sem Compasso.
    if (!player.isShiftKeyDown() && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof com.arcane.block.ArcaneAltarBlock) {
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
      if (!event.getLevel().isClientSide && player instanceof ServerPlayer sp) drawByHand(sp, (ServerLevel) event.getLevel(), event.getPos());
      return;
    }
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

    // Abre a interface de desenho: voce monta a magia que quer escrever neste bloco (nao usa a magia selecionada).
    final BlockPos target = pos;
    net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer,
        new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new com.arcane.menu.ArcaneWorkbenchMenu(id, inv, target, true),
            Component.translatable("gui.arcane.pen_title")),
        buf -> buf.writeBlockPos(target).writeBoolean(true));
  }

  private static void drawByHand(ServerPlayer player, ServerLevel level, BlockPos altar) {
    int radius = com.arcane.item.ArcanePenItem.radius(player.getMainHandItem());
    int cost = com.arcane.ritual.RitualService.handInkCost(radius);
    boolean creative = player.getAbilities().instabuild;
    int have = 0;
    for (ItemStack s : player.getInventory().items) if (s.is(ModItems.COMMON_INK.get())) have += s.getCount();
    if (!creative && have < cost) {
      player.displayClientMessage(Component.translatable("message.arcane.compass.need_ink", cost).withStyle(ChatFormatting.RED), true);
      return;
    }
    int placed = com.arcane.ritual.RitualService.drawCircle(level, altar, radius);
    int used = creative ? 0 : Math.min(have, Math.max(placed > 0 ? 1 : 0, (int) Math.ceil(placed / (double) com.arcane.ritual.RitualTemplate.HAND_TRACES_PER_INK)));
    int left = used;
    for (ItemStack s : player.getInventory().items) {
      if (left <= 0) break;
      if (s.is(ModItems.COMMON_INK.get())) { int n = Math.min(left, s.getCount()); s.shrink(n); left -= n; }
    }
    level.playSound(null, altar, com.arcane.registry.ModSounds.PEN_STROKE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 0.8f);
    player.displayClientMessage(Component.translatable("message.arcane.compass.drawn", placed, used).withStyle(ChatFormatting.LIGHT_PURPLE), true);
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
      CastingService.tryCast(serverPlayer, inscription, PrecisionSnapshot.of(inscription));
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
