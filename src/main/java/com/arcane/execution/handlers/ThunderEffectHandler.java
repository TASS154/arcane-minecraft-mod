package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

public class ThunderEffectHandler implements EffectHandler {
  private static final int MAX_STRIKES = 3;

  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    int strikes = 0;
    for (Vec3 pos : Targeting.positions(targets)) {
      if (strikes++ >= MAX_STRIKES) break;
      LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(context.level());
      if (bolt == null) continue;
      // As posicoes miram o centro do corpo; o raio cai no chao, abaixo dele.
      bolt.moveTo(pos.x, Math.floor(pos.y), pos.z);
      if (context.caster() instanceof ServerPlayer player) bolt.setCause(player);
      context.level().addFreshEntity(bolt);
      context.level().sendParticles(com.arcane.registry.ModParticles.ARCANE_BOLT_STRIKE.get(), pos.x, Math.floor(pos.y) + 2.0, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
    }
  }
}
