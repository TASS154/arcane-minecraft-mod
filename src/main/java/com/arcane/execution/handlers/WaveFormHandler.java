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

/** Onda: cone de ~100 graus a frente do conjurador, ate o alcance. */
public class WaveFormHandler implements FormHandler {
  private static final double COS_HALF_ANGLE = Math.cos(Math.toRadians(50));

  @Override
  public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
    double range = Math.max(2.0, Math.min(48.0, spec.range() * context.areaScale()));
    LivingEntity caster = context.caster();
    Vec3 origin = caster.position().add(0, caster.getBbHeight() * 0.5, 0);
    Vec3 look = context.look();
    List<Entity> found = context.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(range),
        e -> e != caster && e.isAlive() && !e.isSpectator() && withinCone(origin, look, e, range))
        .stream().map(e -> (Entity) e).toList();

    Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
    for (int i = -4; i <= 4; i++) {
      double angle = Math.toRadians(i * 11.0);
      double cos = Math.cos(angle), sin = Math.sin(angle);
      Vec3 dir = new Vec3(flat.x * cos - flat.z * sin, 0, flat.x * sin + flat.z * cos);
      context.level().sendParticles(ModParticles.ARCANE_SPARK.get(), origin.x + dir.x * range, origin.y, origin.z + dir.z * range,
          1, 0.0, 0.2, 0.0, 0.0);
    }
    return DeliveryResult.of(new TargetList(found, List.of()));
  }

  private static boolean withinCone(Vec3 origin, Vec3 look, Entity e, double range) {
    Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(origin);
    double dist = to.length();
    if (dist > range || dist < 1.0e-6) return dist < 1.0e-6;
    return to.scale(1.0 / dist).dot(look) >= COS_HALF_ANGLE;
  }
}
