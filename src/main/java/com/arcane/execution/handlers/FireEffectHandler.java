package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class FireEffectHandler implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
        int duration = (int) context.amplify(call.parameters().getOrDefault("duration", 60.0), call.magnitudeKeys(), "duration");
        for (Entity entity : targets.entities()) {
            entity.setSecondsOnFire(Math.max(1, duration / 20));
        }
        if (!com.arcane.execution.Targeting.mayAlterWorld(context)) return;
        for (com.arcane.execution.TargetList.BlockTarget block : targets.blocks()) {
            igniteBlock(context, block.pos());
        }
    }

    /** O fogo interage com o mundo: acende TNT, fogueiras e velas; derrete gelo e neve. */
    private static void igniteBlock(ExecutionContext context, net.minecraft.core.BlockPos pos) {
        var level = context.level();
        var state = level.getBlockState(pos);
        var block = state.getBlock();
        if (block instanceof net.minecraft.world.level.block.TntBlock) {
            level.removeBlock(pos, false);
            state.onCaughtFire(level, pos, null, context.caster());
        } else if (block instanceof net.minecraft.world.level.block.CampfireBlock
                || block instanceof net.minecraft.world.level.block.CandleBlock) {
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
                level.setBlockAndUpdate(pos, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, true));
            }
        } else if (state.is(net.minecraft.world.level.block.Blocks.ICE)) {
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
        } else if (state.is(net.minecraft.world.level.block.Blocks.SNOW) || state.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)) {
            level.removeBlock(pos, false);
        }
    }
}
