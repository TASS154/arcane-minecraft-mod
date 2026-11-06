package com.arcane.execution.handlers;

import com.arcane.api.filter.FilterResolver;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

/** Filtro de blocos definido por um predicado. Descarta criaturas. */
public class BlockPredicateFilter implements FilterResolver {
  private final Predicate<BlockState> test;

  public BlockPredicateFilter(Predicate<BlockState> test) {
    this.test = test;
  }

  @Override
  public boolean matchesEntity(ExecutionContext context, Entity entity) {
    return false;
  }

  @Override
  public boolean matchesBlock(ExecutionContext context, BlockPos pos, BlockState state) {
    return test.test(state);
  }

  @Override
  public TargetList filter(ExecutionContext context, TargetList input) {
    return new TargetList(List.of(), input.blocks().stream().filter(b -> test.test(b.state())).toList());
  }
}
