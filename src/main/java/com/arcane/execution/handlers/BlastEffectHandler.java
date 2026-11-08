package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.Chain;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.SpellImmunity;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Explosao Destrutiva: como a Explosao, mas DESTROI blocos (como TNT, com queda de itens) quando as configs
 * permitirem (worldAlteringSpells, blockBreakingSpells e mobGriefing para criaturas). O conjurador e imune.
 * No maximo 2 detonacoes por conjuracao, raio ate 6 (12 em rituais grandes).
 */
public class BlastEffectHandler implements EffectHandler {
  private static final int MAX_EXPLOSIONS = 2;
  private static final float MAX_RADIUS = 6.0f;

  /**
   * Uma explosao dentro de um bloco solido perde quase toda a forca: o impacto acontece no ar encostado ao bloco,
   * do lado mais proximo de quem conjura (como um projetil que bate na parede).
   */
  static Vec3 impact(ExecutionContext context, net.minecraft.core.BlockPos pos) {
    Vec3 best = Vec3.atCenterOf(pos);
    double bestD = Double.MAX_VALUE;
    for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
      net.minecraft.core.BlockPos n = pos.relative(d);
      if (!context.level().getBlockState(n).isAir()) continue;
      Vec3 c = Vec3.atCenterOf(n);
      double dist = c.distanceToSqr(context.caster().position());
      if (dist < bestD) { bestD = dist; best = c; }
    }
    return best;
  }

  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    float cap = MAX_RADIUS * (float) Math.min(2.0, Math.max(1.0, context.areaScale()));
    float radius = (float) Math.max(1.0, Math.min(cap, call.parameters().getOrDefault("radius", 3.0) * context.areaScale()));
    Level.ExplosionInteraction interaction = Targeting.mayBreakBlocks(context) ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
    int count = 0;
    java.util.List<Vec3> centers = new java.util.ArrayList<>();
    for (var e : targets.entities()) centers.add(e.position().add(0, e.getBbHeight() * 0.5, 0));
    for (var b : targets.blocks()) centers.add(impact(context, b.pos()));
    centers.addAll(targets.points());
    for (Vec3 pos : centers) {
      if (count++ >= MAX_EXPLOSIONS) break;
      context.level().sendParticles(com.arcane.registry.ModParticles.ARCANE_SHOCK.get(), pos.x, pos.y + 0.3, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
      SpellImmunity.explodeAs(context.caster(), () -> context.level().explode(context.caster(), pos.x, pos.y, pos.z, radius, interaction));
      if (Chain.enabled(call)) {
        Chain.explosions(context, pos, radius, interaction, 0, new java.util.HashSet<>(java.util.List.of(context.caster().getUUID())), Chain.explosionBudget());
      }
    }
  }
}
