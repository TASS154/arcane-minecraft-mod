package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import net.minecraft.world.level.block.state.BlockState;

/** Quebra blocos moles. Nunca quebra blocos com inventario (baus, fornalhas...) nem inquebraveis. */
public class DigEffectHandler implements EffectHandler {
  private static final int MAX_BLOCKS = 4;

  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    if (!Targeting.mayBreakBlocks(context)) return;
    double power = Math.max(0.5, Math.min(5.0, call.parameters().getOrDefault("power", 2.0)));
    int broken = 0;
    for (TargetList.BlockTarget block : targets.blocks()) {
      if (broken >= MAX_BLOCKS) break;
      BlockState state = context.level().getBlockState(block.pos());
      float hardness = state.getDestroySpeed(context.level(), block.pos());
      if (state.isAir() || hardness < 0 || hardness > power || state.hasBlockEntity()) continue;
      if (context.level().destroyBlock(block.pos(), true, context.caster())) broken++;
    }
  }
}
