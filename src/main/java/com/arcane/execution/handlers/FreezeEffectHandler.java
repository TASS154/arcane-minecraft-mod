package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.List;

/**
 * Congelar: criaturas ficam lentas e com frio; em volta de cada alvo (raio ajustavel pelo modificador Area),
 * agua vira gelo, lava vira obsidiana ou pedregulho e fogo se apaga.
 */
public class FreezeEffectHandler implements EffectHandler {
  private static final int MAX_BLOCKS = 80;

  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    double duration = context.amplify(call.parameters().getOrDefault("duration", 100.0), call.magnitudeKeys(), "duration");
    int ticks = (int) Math.max(20, Math.min(600, duration));
    List<BlockPos> centers = new ArrayList<>();
    for (Entity entity : targets.entities()) {
      if (entity instanceof LivingEntity living) {
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1), context.caster());
        living.setTicksFrozen(Math.max(living.getTicksFrozen(), 140 + ticks / 2));
      }
      centers.add(entity.blockPosition());
    }
    for (TargetList.BlockTarget block : targets.blocks()) centers.add(block.pos());
    if (!Targeting.mayAlterWorld(context)) return;

    int radius = (int) Math.max(1, Math.min(10, Math.round(call.parameters().getOrDefault("radius", 1.0) * context.areaScale())));
    ServerLevel level = context.level();
    int changed = 0;
    for (BlockPos center : centers) {
      for (int x = -radius; x <= radius; x++) {
        for (int z = -radius; z <= radius; z++) {
          if (x * x + z * z > radius * radius + 1) continue;
          for (int y = -1; y <= 1; y++) {
            if (changed >= MAX_BLOCKS) return;
            BlockPos pos = center.offset(x, y, z);
            FluidState fluid = level.getFluidState(pos);
            if (fluid.is(FluidTags.WATER) && fluid.isSource()) {
              level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
              changed++;
            } else if (fluid.is(FluidTags.LAVA)) {
              level.setBlockAndUpdate(pos, fluid.isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
              changed++;
            } else if (level.getBlockState(pos).is(Blocks.FIRE)) {
              level.removeBlock(pos, false);
              changed++;
            }
          }
        }
      }
    }
  }
}
