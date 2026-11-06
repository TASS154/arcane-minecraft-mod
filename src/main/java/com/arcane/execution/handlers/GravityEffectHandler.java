package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Gravidade: o alvo e jogado para baixo e acumula distancia de queda (pouso violento).
 * Em blocos que caem (areia, cascalho, bigorna) com ar embaixo, derruba o bloco.
 * Varios glifos Gravidade somam: cada um aplica seu impulso (a "mini singularidade" do design).
 */
public class GravityEffectHandler implements EffectHandler {
  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    double strength = Math.min(8.0, context.amplify(call.parameters().getOrDefault("strength", 1.0), call.magnitudeKeys(), "strength"));
    for (Entity entity : targets.entities()) {
      Vec3 v = entity.getDeltaMovement();
      entity.setDeltaMovement(v.x * 0.5, Math.min(v.y, 0.0) - 0.9 * strength, v.z * 0.5);
      entity.fallDistance += (float) (4.0 * strength);
      entity.hurtMarked = true;
      if (entity instanceof LivingEntity living) {
        living.removeEffect(MobEffects.LEVITATION);
        living.removeEffect(MobEffects.SLOW_FALLING);
      }
    }
    if (!Targeting.mayAlterWorld(context)) return;
    int dropped = 0;
    for (TargetList.BlockTarget block : targets.blocks()) {
      if (dropped >= 6) break;
      BlockState state = context.level().getBlockState(block.pos());
      if (state.getBlock() instanceof FallingBlock && FallingBlock.isFree(context.level().getBlockState(block.pos().below()))) {
        FallingBlockEntity.fall(context.level(), block.pos(), state);
        dropped++;
      }
    }
  }
}
