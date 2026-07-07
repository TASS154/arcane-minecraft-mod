package com.arcane.execution.handlers;

import com.arcane.api.filter.FilterResolver;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class HostileFilterResolver implements FilterResolver {
    @Override
    public boolean matchesEntity(ExecutionContext context, Entity entity) {
        if (entity instanceof Mob mob) {
            return mob.getTarget() != null || entity instanceof net.minecraft.world.entity.monster.Enemy;
        }
        return false;
    }

    @Override
    public boolean matchesBlock(ExecutionContext context, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public TargetList filter(ExecutionContext context, TargetList input) {
        List<Entity> filtered = new ArrayList<>();
        for (Entity entity : input.entities()) {
            if (matchesEntity(context, entity)) {
                filtered.add(entity);
            }
        }
        return new TargetList(filtered, List.of());
    }
}
