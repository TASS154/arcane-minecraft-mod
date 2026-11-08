package com.arcane.item;

import com.arcane.ritual.RitualLevel;
import com.arcane.ritual.RitualTemplate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Pena Arcana: escreve magias em blocos (abre a interface de desenho) e em papel, e desenha a mao o circulo do ritual
 * (sem Compasso, ate raio 64, gastando metade da tinta). Shift + botao direito no ar troca o raio do circulo a mao.
 */
public class ArcanePenItem extends Item {
  public static final String SIZE = "handsize";

  public ArcanePenItem(Properties properties) {
    super(properties);
  }

  public static int sizeIndex(ItemStack stack) {
    return stack.hasTag() ? Math.floorMod(stack.getTag().getInt(SIZE), RitualTemplate.HAND_RADII.length) : 0;
  }

  public static int radius(ItemStack stack) {
    return RitualTemplate.HAND_RADII[sizeIndex(stack)];
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
    if (!level.isClientSide) {
      int next = (sizeIndex(stack) + 1) % RitualTemplate.HAND_RADII.length;
      stack.getOrCreateTag().putInt(SIZE, next);
      int radius = RitualTemplate.HAND_RADII[next];
      player.displayClientMessage(Component.translatable("message.arcane.pen.size", radius, radius * 2,
          Component.translatable("ritual.arcane.level." + RitualLevel.forRadius(radius).number())).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }
}
