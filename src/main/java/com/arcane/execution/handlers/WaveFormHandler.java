package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.registry.ModParticles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Onda: por padrao um cone de ~100 graus a frente do conjurador, ate o alcance (qualquer formato pode substitui-lo). */
public class WaveFormHandler implements FormHandler {
  @Override
  public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
    double range = Math.max(2.0, Math.min(48.0, spec.range() * context.areaScale()));
    com.arcane.execution.Region region = com.arcane.execution.Region.of(context, spec, com.arcane.execution.ShapeGeometry.CONE, range);
    List<Entity> found = region.entities(context.level(), context.caster(), false);
    region.playFx(context);
    return DeliveryResult.of(new TargetList(found, List.of()));
  }
}
