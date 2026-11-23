package com.arcane.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Criatura Selada: guarda uma criatura banida pelo glifo Selamento. Usar num bloco a solta ali (o item se gasta). */
public class SealedCreatureItem extends Item {
  public SealedCreatureItem(Properties properties) { super(properties.stacksTo(1)); }

  @Override
  public boolean isFoil(ItemStack stack) { return stack.hasTag() && stack.getTag().contains("entity"); }

  @Override
  public InteractionResult useOn(UseOnContext ctx) {
    ItemStack stack = ctx.getItemInHand();
    if (!stack.hasTag() || !stack.getTag().contains("entity")) return InteractionResult.PASS;
    if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
    BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
    var data = stack.getTag().getCompound("entity").copy();
    data.remove("UUID");
    var entity = EntityType.loadEntityRecursive(data, level, e -> {
      e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, e.getYRot(), e.getXRot());
      return e;
    });
    if (entity == null || !level.addFreshEntity(entity)) return InteractionResult.FAIL;
    level.playSound(null, at, SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.2f, 0.7f);
    level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 1, 0, 0, 0, 0);
    if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) stack.shrink(1);
    return InteractionResult.CONSUME;
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
    lines.add(Component.translatable("item.arcane.sealed_creature.tip").withStyle(ChatFormatting.GRAY));
  }
}
