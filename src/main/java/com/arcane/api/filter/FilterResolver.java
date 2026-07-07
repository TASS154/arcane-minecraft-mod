package com.arcane.api.filter;

import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public interface FilterResolver {
    boolean matchesEntity(ExecutionContext context, Entity entity);
    boolean matchesBlock(ExecutionContext context, BlockPos pos, BlockState state);
    TargetList filter(ExecutionContext context, TargetList input);
}
