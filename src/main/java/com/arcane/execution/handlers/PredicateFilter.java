package com.arcane.execution.handlers;

import com.arcane.api.filter.FilterResolver;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.BiPredicate;

/** Filtro de criaturas definido por um predicado. Descarta blocos. */
public class PredicateFilter implements FilterResolver {
  private final BiPredicate<ExecutionContext, Entity> test;

  public PredicateFilter(BiPredicate<ExecutionContext, Entity> test) {
    this.test = test;
  }

  @Override
  public boolean matchesEntity(ExecutionContext context, Entity entity) {
    return test.test(context, entity);
  }

  @Override
  public boolean matchesBlock(ExecutionContext context, BlockPos pos, BlockState state) {
    return false;
  }

  @Override
  public TargetList filter(ExecutionContext context, TargetList input) {
    return new TargetList(input.entities().stream().filter(e -> test.test(context, e)).toList(), List.of());
  }
}
