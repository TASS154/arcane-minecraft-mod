package com.arcane.execution.handlers;

import net.minecraft.world.phys.Vec3;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseFireBlock;

/** Acende fogo em cima do bloco alvo. */
public class IgniteEffectHandler implements EffectHandler {
  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    if (!Targeting.mayAlterWorld(context)) return;
    for (TargetList.BlockTarget block : targets.blocks()) {
      BlockPos above = block.pos().above();
      if (com.arcane.execution.ElementInteractions.canIgniteAt(context.level(), above) && context.level().isEmptyBlock(above) && BaseFireBlock.canBePlacedAt(context.level(), above, Direction.UP)) {
        context.level().setBlockAndUpdate(above, BaseFireBlock.getState(context.level(), above));
      }
    }
    // Ponto vazio: acende no chao logo abaixo.
    for (var point : targets.points()) {
      com.arcane.execution.Targeting.groundCell(context.level(), point).filter(cell -> Vec3.atCenterOf(cell).distanceToSqr(context.caster().position()) > 2.25).ifPresent(cell -> {
        if (com.arcane.execution.ElementInteractions.canIgniteAt(context.level(), cell) && BaseFireBlock.canBePlacedAt(context.level(), cell, Direction.UP)) {
          context.level().setBlockAndUpdate(cell, BaseFireBlock.getState(context.level(), cell));
        }
      });
    }
  }
}
