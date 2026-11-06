package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.registry.ModParticles;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Teleporta o conjurador ate onde ele mira (parando antes de paredes). Nao usa a lista de alvos. */
public class BlinkEffectHandler implements EffectHandler {
  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    LivingEntity caster = context.caster();
    double distance = Math.max(1.0, Math.min(32.0, call.parameters().getOrDefault("distance", 8.0)));
    Vec3 start = context.origin();
    Vec3 look = context.look();
    Vec3 end = start.add(look.scale(distance));
    BlockHitResult hit = context.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
    Vec3 target = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(0.7));

    Vec3 from = caster.position();
    double eye = caster.getEyeHeight();
    boolean moved = false;
    // Recua passo a passo ate achar um lugar livre para o corpo.
    for (int i = 0; i < 6 && !moved; i++) {
      Vec3 candidate = target.subtract(look.scale(0.5 * i));
      moved = caster.randomTeleport(candidate.x, candidate.y - eye, candidate.z, true);
    }
    if (moved) {
      context.level().sendParticles(com.arcane.execution.SpellElement.of(context.plan()).particle(), from.x, from.y + 1.0, from.z, 14, 0.3, 0.5, 0.3, 0.05);
      context.level().sendParticles(com.arcane.execution.SpellElement.of(context.plan()).particle(), caster.getX(), caster.getY() + 1.0, caster.getZ(), 14, 0.3, 0.5, 0.3, 0.05);
    }
  }
}
