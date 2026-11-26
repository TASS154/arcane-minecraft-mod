package com.arcane.item;

import com.arcane.entity.ArcaneDummyEntity;
import com.arcane.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** Boneco de Treino Arcano: usar num bloco coloca o boneco em cima, virado para quem colocou. */
public class TrainingDummyItem extends Item {
  public TrainingDummyItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult useOn(UseOnContext ctx) {
    if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
    var pos = ctx.getClickedPos().relative(ctx.getClickedFace());
    Vec3 at = Vec3.atBottomCenterOf(pos);
    ArcaneDummyEntity dummy = ModEntities.TRAINING_DUMMY.get().create(level);
    if (dummy == null) return InteractionResult.FAIL;
    float yaw = ctx.getPlayer() == null ? 0f : ctx.getPlayer().getYRot() + 180f;
    dummy.moveTo(at.x, at.y, at.z, yaw, 0f);
    dummy.setYHeadRot(yaw);
    dummy.setYBodyRot(yaw);
    if (!level.noCollision(dummy)) return InteractionResult.FAIL;
    level.addFreshEntity(dummy);
    level.playSound(null, at.x, at.y, at.z, SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75f, 0.8f);
    if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
    return InteractionResult.CONSUME;
  }
}
