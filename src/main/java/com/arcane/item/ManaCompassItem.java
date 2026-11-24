package com.arcane.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Bussola Arcana: o ponteiro aponta para a maior fonte de magia por perto, na ordem de importancia: um ritual gigante
 * (circulo de raio 16 ou mais, que vaza mana), um covil de Hidra, Ruinas Arcanas e, por fim, o cristal de mana mais
 * proximo. Alcance de 512 blocos (cristais: 128, so nos chunks ja carregados). Botao direito diz o que e e a que
 * distancia. Sem nada por perto, o ponteiro gira devagar.
 */
public class ManaCompassItem extends Item {
  public static final String TARGET = "arcane_target", KIND = "arcane_target_kind", DIM = "arcane_target_dim";
  public static final double RANGE = 512, CRYSTAL_RANGE = 128;

  public ManaCompassItem(Properties properties) { super(properties.stacksTo(1)); }

  /** Alvo atual: posicao e tipo ("ritual", "lair", "ruins", "crystal"), ou nulo. */
  public record Target(BlockPos pos, String kind) {}

  public static Target find(ServerLevel level, BlockPos from) {
    Target best = null;
    double bestD = RANGE * RANGE;
    for (BlockPos altar : com.arcane.ritual.RitualLeaks.leakingAltars(level)) {
      double d = altar.distSqr(from);
      if (d < bestD) { bestD = d; best = new Target(altar, "ritual"); }
    }
    if (best == null && level.dimension() == Level.OVERWORLD) {
      for (var lair : com.arcane.world.HydraLairs.get(level).lairs()) {
        if (lair.defeated) continue;
        double d = lair.centre.distSqr(from);
        if (d < bestD) { bestD = d; best = new Target(lair.centre, "lair"); }
      }
    }
    if (best == null) {
      for (BlockPos ruins : com.arcane.execution.WorldMarks.sites(level, "ruins")) {
        double d = ruins.distSqr(from);
        if (d < bestD) { bestD = d; best = new Target(ruins, "ruins"); }
      }
    }
    if (best == null) {
      BlockPos crystal = com.arcane.ambient.AmbientFieldResolver.findNearestCrystal(level, from, (int) CRYSTAL_RANGE);
      if (crystal != null) best = new Target(crystal, "crystal");
    }
    return best;
  }

  @Override
  public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
    if (!(level instanceof ServerLevel sl) || !(holder instanceof Player p) || p.tickCount % 20 != (slot % 20)) return;
    if (!selected && p.getOffhandItem() != stack) return;
    Target t = find(sl, p.blockPosition());
    CompoundTag tag = stack.getOrCreateTag();
    if (t == null) {
      if (tag.contains(TARGET)) { tag.remove(TARGET); tag.remove(KIND); tag.remove(DIM); }
      return;
    }
    if (tag.getLong(TARGET) != t.pos().asLong() || !t.kind().equals(tag.getString(KIND))) {
      tag.putLong(TARGET, t.pos().asLong());
      tag.putString(KIND, t.kind());
      tag.putString(DIM, level.dimension().location().toString());
    }
  }

  /** Alvo gravado no item (o cliente le daqui para girar o ponteiro). */
  @Nullable
  public static BlockPos target(ItemStack stack, Level level) {
    if (!stack.hasTag() || !stack.getTag().contains(TARGET)) return null;
    if (!level.dimension().location().toString().equals(stack.getTag().getString(DIM))) return null;
    return BlockPos.of(stack.getTag().getLong(TARGET));
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (level instanceof ServerLevel sl) {
      Target t = find(sl, player.blockPosition());
      if (t == null) {
        player.displayClientMessage(Component.translatable("message.arcane.mana_compass.none").withStyle(ChatFormatting.GRAY), true);
      } else {
        int dist = (int) Math.sqrt(t.pos().distSqr(player.blockPosition()));
        player.displayClientMessage(Component.translatable("message.arcane.mana_compass.found",
            Component.translatable("message.arcane.mana_compass.kind." + t.kind()), dist,
            Component.translatable("direction.arcane." + com.arcane.ritual.RitualLeaks.direction(player.blockPosition(), t.pos())))
            .withStyle(ChatFormatting.AQUA), true);
      }
    }
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
    lines.add(Component.translatable("item.arcane.mana_compass.tip").withStyle(ChatFormatting.GRAY));
    if (stack.hasTag() && stack.getTag().contains(KIND)) {
      lines.add(Component.translatable("message.arcane.mana_compass.kind." + stack.getTag().getString(KIND)).withStyle(ChatFormatting.AQUA));
    }
  }
}
