package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.block.ArcaneBarrierBlock;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.Region;
import com.arcane.execution.ShapeGeometry;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import com.arcane.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Barreira. Sem formato: parede perpendicular ao olhar, a 3 blocos a frente (largura e altura crescem com o raio e o
 * Area). Com um glifo de formato (Domo, Esfera, Anel): uma casca de blocos em volta do centro. O centro e o conjurador,
 * ou o ponto mirado com o glifo Mira. Some sozinha (Duracao). So ocupa espaco livre.
 * Alvos entregues aos glifos seguintes: os blocos criados E as criaturas dentro ou encostadas na barreira
 * (Barreira + Cura cura quem esta no domo; Barreira + Dano fere quem encosta; Filtros restringem).
 */
public class BarrierFormHandler implements FormHandler {
  @Override
  public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
    if (!Targeting.mayAlterWorld(context)) return DeliveryResult.of(TargetList.empty());
    double radius = spec.parameters().getOrDefault("radius", 3.0) * context.areaScale();
    int life = (int) Math.max(1, Math.min(ArcaneBarrierBlock.MAX_LIFE, Math.round(spec.parameters().getOrDefault("duration", 200.0) / 40.0)));
    BlockState barrier = ModBlocks.ARCANE_BARRIER.get().defaultBlockState().setValue(ArcaneBarrierBlock.LIFE, life);
    Region region = Region.of(context, spec, ShapeGeometry.WALL, radius);
    List<TargetList.BlockTarget> placed = new ArrayList<>();
    for (BlockPos pos : region.shell()) {
      BlockState existing = context.level().getBlockState(pos);
      if (existing.isAir() || existing.canBeReplaced()) {
        context.level().setBlockAndUpdate(pos, barrier);
        placed.add(new TargetList.BlockTarget(pos, barrier));
      }
    }
    // Quem esta dentro ou encostado (margem de 1 bloco) tambem e alvo dos glifos seguintes.
    Region touching = new Region(region.shape(), region.center(), region.radius() + 1.0, region.height(), region.facing());
    List<Entity> inside = touching.entities(context.level(), context.caster(), true);
    return DeliveryResult.of(new TargetList(inside, placed));
  }
}
