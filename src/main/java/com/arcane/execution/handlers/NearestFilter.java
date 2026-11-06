package com.arcane.execution.handlers;

import com.arcane.api.filter.FilterResolver;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.List;

/** Mantem apenas a criatura mais proxima do conjurador. */
public class NearestFilter implements FilterResolver {
  @Override
  public boolean matchesEntity(ExecutionContext context, Entity entity) {
    return true;
  }

  @Override
  public boolean matchesBlock(ExecutionContext context, BlockPos pos, BlockState state) {
    return false;
  }

  @Override
  public TargetList filter(ExecutionContext context, TargetList input) {
    List<Entity> nearest = input.entities().stream()
        .min(Comparator.comparingDouble(e -> e.distanceToSqr(context.caster()))).map(List::of).orElse(List.of());
    return new TargetList(nearest, List.of());
  }
}
