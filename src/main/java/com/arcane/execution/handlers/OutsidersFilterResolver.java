package com.arcane.execution.handlers;

import com.arcane.api.filter.FilterResolver;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.GuestLists;
import com.arcane.execution.TargetList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Filtro "Forasteiros": so quem NAO e o dono, nem convidado dele, nem pertence a eles. Blocos passam. */
public class OutsidersFilterResolver implements FilterResolver {
  @Override
  public boolean matchesEntity(ExecutionContext context, Entity entity) {
    return !GuestLists.get(context.level().getServer()).isFriendly(context.caster().getUUID(), entity);
  }

  @Override
  public boolean matchesBlock(ExecutionContext context, BlockPos pos, BlockState state) {
    return true;
  }

  @Override
  public TargetList filter(ExecutionContext context, TargetList input) {
    GuestLists lists = GuestLists.get(context.level().getServer());
    List<Entity> kept = new ArrayList<>();
    for (Entity e : input.entities()) {
      if (!lists.isFriendly(context.caster().getUUID(), e)) kept.add(e);
    }
    return new TargetList(kept, input.blocks(), input.points());
  }
}
