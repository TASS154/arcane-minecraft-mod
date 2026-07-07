package com.arcane.execution.handlers;

import com.arcane.api.filter.FilterResolver;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SelfFilterResolver implements FilterResolver {
    @Override
    public boolean matchesEntity(ExecutionContext context, Entity entity) {
        return entity == context.caster();
    }

    @Override
    public boolean matchesBlock(ExecutionContext context, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public TargetList filter(ExecutionContext context, TargetList input) {
        return TargetList.ofEntities(context.caster());
    }
}
