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
  /** Po solvente na mao principal: apaga todos os desenhos do bloco (a tinta permanente pura nao sai). */
  @SubscribeEvent(priority = EventPriority.HIGH)
  public void onSolventUse(PlayerInteractEvent.RightClickBlock event) {
    Player player = event.getEntity();
    if (event.getHand() != InteractionHand.MAIN_HAND || !player.getMainHandItem().is(ModItems.SOLVENT_POWDER.get())) return;
    event.setCanceled(true);
    event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    if (event.getLevel().isClientSide || !(event.getLevel() instanceof ServerLevel level)) return;
    var result = InscribingService.dissolve(level, event.getPos());
    String key = switch (result) {
      case NOTHING -> "message.arcane.solvent.nothing";
      case ERASED -> "message.arcane.solvent.erased";
      case PERMANENT_REMAINS -> "message.arcane.solvent.permanent";
    };
    player.displayClientMessage(Component.translatable(key).withStyle(result == InscribingService.Dissolve.ERASED ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY), true);
    if (result == InscribingService.Dissolve.ERASED && !player.getAbilities().instabuild) player.getMainHandItem().shrink(1);
  }

  /** Pena na mao principal + tinta na secundaria: escreve (ou apaga, agachado) em vez de usar o bloco. */
  @SubscribeEvent(priority = EventPriority.HIGH)
  public void onPenUse(PlayerInteractEvent.RightClickBlock event) {
    Player player = event.getEntity();
    if (event.getHand() != InteractionHand.MAIN_HAND) return;
    ItemStack mainItem = player.getMainHandItem();
    boolean blockPen = mainItem.getItem() instanceof com.arcane.item.BlockPenItem;
    boolean objectPen = mainItem.getItem() instanceof com.arcane.item.ObjectPenItem;
    if (!blockPen && !objectPen && !mainItem.is(ModItems.ARCANE_PEN.get())) return;
    // Caneta num Altar Arcano (sem agachar): desenha o circulo do ritual a mao, sem Compasso.
    if (!blockPen && !objectPen && !player.isShiftKeyDown() && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof com.arcane.block.ArcaneAltarBlock) {
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
      if (!event.getLevel().isClientSide && player instanceof ServerPlayer sp) drawByHand(sp, (ServerLevel) event.getLevel(), event.getPos());
      return;
    }
    ItemStack held = player.getOffhandItem();
    boolean erasing = player.isShiftKeyDown();
    boolean itemMode = !blockPen && !erasing && !held.isEmpty() && !(held.getItem() instanceof InkItem);
    event.setCanceled(true);
    event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    if (event.getLevel().isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;

    ServerLevel level = (ServerLevel) event.getLevel();
    BlockPos pos = event.getPos();
    if (!erasing && !itemMode) {
      // Escrever em bloco: so com a caneta do material dele.
      if (objectPen) {
        player.displayClientMessage(Component.translatable("message.arcane.pen.need_item").withStyle(ChatFormatting.RED), true);
        return;
      }
      if (!blockPen) {
        player.displayClientMessage(Component.translatable("message.arcane.pen.need_block_pen").withStyle(ChatFormatting.RED), true);
        return;
      }
      if (!PenRegistry.canWriteOnBlock(player, pos, level)) {
        player.displayClientMessage(Component.translatable("message.arcane.pen.wrong_material",
            Component.translatable("item.arcane." + ((com.arcane.item.BlockPenItem) mainItem.getItem()).group() + "_pen")).withStyle(ChatFormatting.RED), true);
        return;
      }
    }
    if (erasing) {
      // Apagar so com po solvente: a pena nao apaga mais.
      player.displayClientMessage(Component.translatable("message.arcane.pen.use_solvent").withStyle(ChatFormatting.GRAY), true);
      return;
    }
    // Item na mao secundaria: escreve nele. Senao, escreve no bloco. A tinta e escolhida na propria interface.
    openPenMenu(serverPlayer, pos, itemMode ? com.arcane.menu.ArcaneWorkbenchMenu.ITEM : com.arcane.menu.ArcaneWorkbenchMenu.BLOCK, event.getFace());
  }

  /** Caneta no ar com um item na mao secundaria: abre o menu de escrever no item. */
  @SubscribeEvent
  public void onPenUseInAir(PlayerInteractEvent.RightClickItem event) {
    Player player = event.getEntity();
    ItemStack mainPen = player.getMainHandItem();
    if (event.getHand() != InteractionHand.MAIN_HAND || !(mainPen.is(ModItems.ARCANE_PEN.get()) || mainPen.getItem() instanceof com.arcane.item.ObjectPenItem)) return;
    ItemStack held = player.getOffhandItem();
    if (held.isEmpty() || held.getItem() instanceof InkItem || player.isShiftKeyDown()) return;
    event.setCanceled(true);
    if (!event.getLevel().isClientSide && player instanceof ServerPlayer sp) {
      openPenMenu(sp, sp.blockPosition(), com.arcane.menu.ArcaneWorkbenchMenu.ITEM, net.minecraft.core.Direction.UP);
    }
  }

  private static void openPenMenu(ServerPlayer player, BlockPos target, int mode, net.minecraft.core.Direction clicked) {
    final net.minecraft.core.Direction face = clicked == null ? net.minecraft.core.Direction.UP : clicked;
    net.minecraftforge.network.NetworkHooks.openScreen(player,
        new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new com.arcane.menu.ArcaneWorkbenchMenu(id, inv, target, mode).withFace(face),
            Component.translatable(mode == com.arcane.menu.ArcaneWorkbenchMenu.ITEM ? "gui.arcane.pen_item_title" : "gui.arcane.pen_title")),
        buf -> buf.writeBlockPos(target).writeVarInt(mode).writeVarInt(face.get3DDataValue()));
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
      if (!com.arcane.execution.Traces.has(inscription, "persistence")) index.remove(pos, anchor.get().face());
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
