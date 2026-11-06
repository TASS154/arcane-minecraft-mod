package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.registry.ModParticles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Todas as criaturas vivas num raio ao redor do conjurador (o conjurador fica de fora). */
public class AuraFormHandler implements FormHandler {
  @Override
  public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
    double radius = Math.max(1.0, Math.min(24.0, spec.parameters().getOrDefault("radius", 5.0)));
    LivingEntity caster = context.caster();
    List<Entity> found = context.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius),
        e -> e != caster && e.isAlive() && !e.isSpectator() && e.distanceToSqr(caster) <= radius * radius)
        .stream().map(e -> (Entity) e).toList();
    int points = (int) Math.min(48, radius * 6);
    for (int i = 0; i < points; i++) {
      double angle = Math.PI * 2 * i / points;
      context.level().sendParticles(ModParticles.ARCANE_SPARK.get(),
          caster.getX() + Math.cos(angle) * radius, caster.getY() + 0.2, caster.getZ() + Math.sin(angle) * radius,
          1, 0.0, 0.05, 0.0, 0.0);
    }
    return DeliveryResult.of(new TargetList(found, List.of()));
  }
}
