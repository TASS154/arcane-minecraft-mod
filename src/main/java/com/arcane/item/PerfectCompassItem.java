package com.arcane.item;

import com.arcane.block.ArcaneAltarBlock;
import com.arcane.ritual.RitualLevel;
import com.arcane.ritual.RitualService;
import com.arcane.ritual.RitualTemplate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Compasso Perfeito (so no criativo): desenha sozinho um circulo de ritual com simetria perfeita, sem gastar tinta,
 * de qualquer tamanho da mao (ate raio 64), mesmo onde o chao nao ajuda. Shift + botao direito troca o tamanho.
 */
public class PerfectCompassItem extends Item {
  public PerfectCompassItem(Properties properties) {
    super(properties.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
  }

  public static int radius(ItemStack stack) {
    return RitualTemplate.HAND_RADII[stack.hasTag() ? Math.floorMod(stack.getTag().getInt("size"), RitualTemplate.HAND_RADII.length) : 0];
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
    if (!level.isClientSide) cycle(player, stack);
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }

  private static void cycle(Player player, ItemStack stack) {
    int next = (stack.hasTag() ? stack.getTag().getInt("size") + 1 : 1) % RitualTemplate.HAND_RADII.length;
    stack.getOrCreateTag().putInt("size", next);
    int radius = RitualTemplate.HAND_RADII[next];
    player.displayClientMessage(Component.translatable("message.arcane.pen.size", radius, radius * 2,
        Component.translatable("ritual.arcane.level." + RitualLevel.forRadius(radius).number())).withStyle(ChatFormatting.LIGHT_PURPLE), true);
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    Level level = context.getLevel();
    Player player = context.getPlayer();
    if (player == null) return InteractionResult.PASS;
    if (player.isShiftKeyDown()) {
      if (!level.isClientSide) cycle(player, context.getItemInHand());
      return InteractionResult.sidedSuccess(level.isClientSide);
    }
    BlockPos pos = context.getClickedPos();
    if (!(level.getBlockState(pos).getBlock() instanceof ArcaneAltarBlock)) {
      if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.arcane.compass.need_altar").withStyle(ChatFormatting.GRAY), true);
      return InteractionResult.sidedSuccess(level.isClientSide);
    }
    if (level.isClientSide) return InteractionResult.SUCCESS;
    int radius = radius(context.getItemInHand());
    int placed = RitualService.drawPerfectCircle((ServerLevel) level, pos, radius);
    level.playSound(null, pos, com.arcane.registry.ModSounds.PEN_STROKE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f);
    player.displayClientMessage(Component.translatable("message.arcane.compass.drawn", placed, 0).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    return InteractionResult.CONSUME;
  }
}
