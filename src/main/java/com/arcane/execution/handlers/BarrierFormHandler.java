package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.block.ArcaneBarrierBlock;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import com.arcane.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Barreira: parede de 3x3 blocos de energia a 3 blocos a frente, perpendicular ao olhar. Some sozinha
 * (duracao ajustavel, ate 30 s). So ocupa espaco livre. Entrega os blocos criados como alvos.
 */
public class BarrierFormHandler implements FormHandler {
  @Override
  public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
    if (!Targeting.mayAlterWorld(context)) return DeliveryResult.of(TargetList.empty());
    Vec3 look = context.look();
    boolean alongX = Math.abs(look.x) > Math.abs(look.z);
    int stepX = alongX ? (look.x > 0 ? 1 : -1) : 0;
    int stepZ = alongX ? 0 : (look.z > 0 ? 1 : -1);
    BlockPos feet = context.caster().blockPosition();
    BlockPos center = feet.offset(stepX * 3, 0, stepZ * 3);

    int life = (int) Math.max(1, Math.min(ArcaneBarrierBlock.MAX_LIFE, Math.round(spec.parameters().getOrDefault("duration", 200.0) / 40.0)));
    BlockState barrier = ModBlocks.ARCANE_BARRIER.get().defaultBlockState().setValue(ArcaneBarrierBlock.LIFE, life);
    List<TargetList.BlockTarget> placed = new ArrayList<>();
    for (int w = -1; w <= 1; w++) {
      for (int h = 0; h <= 2; h++) {
        BlockPos pos = center.offset(alongX ? 0 : w, h, alongX ? w : 0);
        BlockState existing = context.level().getBlockState(pos);
        if (existing.isAir() || existing.canBeReplaced()) {
          context.level().setBlockAndUpdate(pos, barrier);
          placed.add(new TargetList.BlockTarget(pos, barrier));
        }
      }
    }
    return DeliveryResult.of(new TargetList(List.of(), placed));
  }
}
