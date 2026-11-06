package com.arcane.item;

import com.arcane.block.ArcaneAltarBlock;
import com.arcane.registry.ModItems;
import com.arcane.ritual.RitualLevel;
import com.arcane.ritual.RitualService;
import com.arcane.ritual.RitualTemplate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Compasso Arcano: desenha o circulo do ritual sozinho (design: "ele constroi, nao desenha bloco por bloco").
 *  - Botao direito num Altar Arcano: desenha o circulo do tamanho escolhido, gastando tinta comum.
 *  - Shift + botao direito: troca o tamanho (raio 4, 8 ou 16 = ritual menor, medio ou maior).
 * Segurado, mostra um holograma do circulo (cliente).
 */
public class ArcaneCompassItem extends Item {
  public static final String SIZE = "size";

  public ArcaneCompassItem(Properties properties) {
    super(properties.stacksTo(1));
  }

  public static int sizeIndex(ItemStack stack) {
    return stack.hasTag() ? Math.floorMod(stack.getTag().getInt(SIZE), RitualTemplate.RADII.length) : 0;
  }

  public static int radius(ItemStack stack) {
    return RitualTemplate.RADII[sizeIndex(stack)];
  }

  private static void cycle(Player player, ItemStack stack) {
    int next = (sizeIndex(stack) + 1) % RitualTemplate.RADII.length;
    stack.getOrCreateTag().putInt(SIZE, next);
    int radius = RitualTemplate.RADII[next];
    player.displayClientMessage(Component.translatable("message.arcane.compass.size", radius,
        Component.translatable("ritual.arcane.level." + RitualLevel.forRadius(radius).number())).withStyle(ChatFormatting.LIGHT_PURPLE), true);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (player.isShiftKeyDown()) {
      if (!level.isClientSide) cycle(player, stack);
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    return InteractionResultHolder.pass(stack);
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    Level level = context.getLevel();
    Player player = context.getPlayer();
    if (player == null) return InteractionResult.PASS;
    ItemStack stack = context.getItemInHand();
    if (player.isShiftKeyDown()) {
      if (!level.isClientSide) cycle(player, stack);
      return InteractionResult.sidedSuccess(level.isClientSide);
    }
    BlockPos pos = context.getClickedPos();
    if (!(level.getBlockState(pos).getBlock() instanceof ArcaneAltarBlock)) {
      if (!level.isClientSide) {
        player.displayClientMessage(Component.translatable("message.arcane.compass.need_altar").withStyle(ChatFormatting.GRAY), true);
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
    }
    if (level.isClientSide) return InteractionResult.SUCCESS;
    if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

    int radius = radius(stack);
    int ink = RitualService.inkCost(radius);
    boolean creative = sp.getAbilities().instabuild;
    if (!creative && countInk(sp) < ink) {
      sp.displayClientMessage(Component.translatable("message.arcane.compass.need_ink", ink).withStyle(ChatFormatting.RED), true);
      return InteractionResult.CONSUME;
    }
    int placed = RitualService.drawCircle(serverLevel, pos, radius);
    if (!creative && placed > 0) takeInk(sp, Math.max(1, (int) Math.ceil(placed / 16.0)));
    serverLevel.playSound(null, pos, com.arcane.registry.ModSounds.PEN_STROKE.get(), SoundSource.BLOCKS, 1.0f, 0.8f);
    sp.displayClientMessage(Component.translatable("message.arcane.compass.drawn", placed,
        creative ? 0 : Math.max(1, (int) Math.ceil(placed / 16.0))).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    return InteractionResult.CONSUME;
  }

  private static int countInk(ServerPlayer player) {
    int total = 0;
    for (ItemStack s : player.getInventory().items) if (s.is(ModItems.COMMON_INK.get())) total += s.getCount();
    return total;
  }

  private static void takeInk(ServerPlayer player, int amount) {
    int left = amount;
    for (ItemStack s : player.getInventory().items) {
      if (left <= 0) break;
      if (s.is(ModItems.COMMON_INK.get())) {
        int n = Math.min(left, s.getCount());
        s.shrink(n);
        left -= n;
      }
    }
  }
}
