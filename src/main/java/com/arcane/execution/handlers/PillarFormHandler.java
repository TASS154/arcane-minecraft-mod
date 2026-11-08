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

/** Pilar: por padrao uma coluna vertical no ponto mirado. Pega as criaturas da regiao e o bloco atingido pela mira. */
public class PillarFormHandler implements FormHandler {
  @Override
  public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
    double radius = Math.max(1.0, Math.min(24.0, spec.parameters().getOrDefault("radius", 2.0) * context.areaScale()));
    com.arcane.execution.Region region = com.arcane.execution.Region.of(context, spec, com.arcane.execution.ShapeGeometry.COLUMN, radius);
    List<Entity> entities = new ArrayList<>(region.entities(context.level(), context.caster(), false));
    region.playFx(context);
    double range = Math.max(4.0, Math.min(64.0, spec.range()));
    List<TargetList.BlockTarget> blocks = Targeting.ray(context, range, false).targets().blocks();
    // O centro da coluna e sempre um ponto de impacto (efeitos de ponto agem ali, mesmo sem bloco na mira).
    List<net.minecraft.world.phys.Vec3> points = List.of(region.center());
    return DeliveryResult.of(new TargetList(entities, blocks, points));
  }
}
