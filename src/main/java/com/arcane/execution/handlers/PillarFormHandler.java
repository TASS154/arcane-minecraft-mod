package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import com.arcane.registry.ModParticles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Pilar: coluna vertical no ponto mirado. Pega as criaturas na coluna e o bloco atingido (se a mira acertou um). */
public class PillarFormHandler implements FormHandler {
  @Override
  public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
    double range = Math.max(4.0, Math.min(64.0, spec.range()));
    double radius = Math.max(1.0, Math.min(8.0, spec.parameters().getOrDefault("radius", 2.0)));
    Targeting.RayResult ray = Targeting.ray(context, range, false);
    Vec3 base = ray.end();

    LivingEntity caster = context.caster();
    AABB column = new AABB(base.x - radius, base.y - 2, base.z - radius, base.x + radius, base.y + 10, base.z + radius);
    List<Entity> entities = new ArrayList<>(context.level().getEntitiesOfClass(LivingEntity.class, column,
        e -> e != caster && e.isAlive() && !e.isSpectator()));
    for (int i = 0; i < 20; i++) {
      context.level().sendParticles(ModParticles.ARCANE_SPARK.get(), base.x, base.y + i * 0.5, base.z, 1, radius * 0.3, 0.0, radius * 0.3, 0.0);
    }
    return DeliveryResult.of(new TargetList(entities, ray.targets().blocks()));
  }
}
