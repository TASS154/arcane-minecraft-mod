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
    double radius = Math.max(1.0, Math.min(48.0, spec.parameters().getOrDefault("radius", 5.0) * context.areaScale()));
    com.arcane.execution.Region region = com.arcane.execution.Region.of(context, spec, com.arcane.execution.ShapeGeometry.SPHERE, radius);
    List<Entity> found = region.entities(context.level(), context.caster(), false);
    region.playFx(context);
    return DeliveryResult.of(new TargetList(found, List.of()));
  }
}
