package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.block.ArcaneBarrierBlock;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.ShapeGeometry;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import com.arcane.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
    int shape = ShapeGeometry.shapeOf(spec.parameters().getOrDefault("shape", 0.0));
    double baseRadius = spec.parameters().getOrDefault("radius", 3.0) * context.areaScale();
    int life = (int) Math.max(1, Math.min(ArcaneBarrierBlock.MAX_LIFE, Math.round(spec.parameters().getOrDefault("duration", 200.0) / 40.0)));
    BlockState barrier = ModBlocks.ARCANE_BARRIER.get().defaultBlockState().setValue(ArcaneBarrierBlock.LIFE, life);
    Vec3 center = Targeting.center(context, spec);
    List<TargetList.BlockTarget> placed = new ArrayList<>();
    List<Entity> inside;

    if (shape != ShapeGeometry.DEFAULT) {
      double radius = ShapeGeometry.clampRadius(shape, baseRadius);
      BlockPos base = BlockPos.containing(center);
      for (ShapeGeometry.Off off : ShapeGeometry.shell(shape, radius)) {
        place(context, base.offset(off.dx(), off.dy(), off.dz()), barrier, placed);
      }
      double cx = base.getX() + 0.5, cy = base.getY(), cz = base.getZ() + 0.5;
      inside = new ArrayList<>(context.level().getEntitiesOfClass(LivingEntity.class,
          new AABB(cx - radius - 2, cy - radius - 2, cz - radius - 2, cx + radius + 2, cy + radius + 2, cz + radius + 2),
          e -> e.isAlive() && !e.isSpectator()
              && ShapeGeometry.contains(shape, e.getX() - cx, e.getY() - cy, e.getZ() - cz, radius + 1.0)));
    } else {
      Vec3 look = context.look();
      boolean alongX = Math.abs(look.x) > Math.abs(look.z);
      int stepX = alongX ? (look.x > 0 ? 1 : -1) : 0;
      int stepZ = alongX ? 0 : (look.z > 0 ? 1 : -1);
      boolean aim = spec.parameters().getOrDefault("origin", 0.0) >= 0.5;
      BlockPos feet = context.caster().blockPosition();
      BlockPos wallCenter = aim ? BlockPos.containing(center) : feet.offset(stepX * 3, 0, stepZ * 3);
      // Parede: com raio 3 sao 3 de largura por 3 de altura; o raio (Area, ritual) amplia as duas medidas.
      int half = (int) Math.max(1, Math.min(8, Math.round(baseRadius * 2.0 / 3.0 - 1)));
      int height = (int) Math.max(3, Math.min(12, Math.round(baseRadius)));
      for (int w = -half; w <= half; w++) {
        for (int h = 0; h < height; h++) {
          place(context, wallCenter.offset(alongX ? 0 : w, h, alongX ? w : 0), barrier, placed);
        }
      }
      AABB box = new AABB(wallCenter.getX() - (alongX ? 0 : half), wallCenter.getY(), wallCenter.getZ() - (alongX ? half : 0),
          wallCenter.getX() + 1 + (alongX ? 0 : half), wallCenter.getY() + height, wallCenter.getZ() + 1 + (alongX ? half : 0)).inflate(1.5);
      inside = new ArrayList<>(context.level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator()));
    }
    return DeliveryResult.of(new TargetList(inside, placed));
  }

  private static void place(ExecutionContext context, BlockPos pos, BlockState barrier, List<TargetList.BlockTarget> placed) {
    BlockState existing = context.level().getBlockState(pos);
    if (existing.isAir() || existing.canBeReplaced()) {
      context.level().setBlockAndUpdate(pos, barrier);
      placed.add(new TargetList.BlockTarget(pos, barrier));
    }
  }
}
