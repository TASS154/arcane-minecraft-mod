package com.arcane.execution;

import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Modificador Cadeia: o efeito se propaga de criatura em criatura. Explosoes detonam de novo, menores, em quem estava
 * perto (apos 5 ticks); o Dano salta em arco para a criatura mais proxima, com 60% do dano a cada salto.
 * Limites: ate 3 saltos e 8 detonacoes extras por conjuracao; cada criatura so e atingida uma vez.
 */
public final class Chain {
  public static final int MAX_HOPS = 3, MAX_EXTRA_EXPLOSIONS = 8;
  public static final double ARC_RANGE = 5.0, FALLOFF = 0.6;

  private Chain() {}

  public static boolean enabled(EffectCall call) {
    return call.parameters().getOrDefault("chain", 0.0) > 0.0;
  }

  /** Detonacao em cadeia: nas criaturas perto de {@code center} (menos o conjurador), com raio reduzido. */
  public static void explosions(ExecutionContext ctx, Vec3 center, float radius, Level.ExplosionInteraction interaction,
                                int depth, Set<UUID> seen, int[] budget) {
    if (depth >= MAX_HOPS || radius < 1.0f) return;
    ServerLevel level = ctx.level();
    AABB box = new AABB(center, center).inflate(radius * 1.6);
    List<LivingEntity> near = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, box,
        e -> e.isAlive() && e != ctx.caster() && !seen.contains(e.getUUID())));
    near.sort((a, b) -> Double.compare(a.distanceToSqr(center), b.distanceToSqr(center)));
    float next = Math.max(1.0f, radius * 0.7f);
    for (LivingEntity target : near) {
      if (budget[0] <= 0) return;
      budget[0]--;
      seen.add(target.getUUID());
      Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
      var server = level.getServer();
      server.tell(new TickTask(server.getTickCount() + 5, () -> {
        level.sendParticles(com.arcane.registry.ModParticles.ARCANE_SHOCK.get(), at.x, at.y + 0.3, at.z, 1, 0, 0, 0, 0);
        SpellImmunity.explodeAs(ctx.caster(), () -> level.explode(ctx.caster(), at.x, at.y, at.z, next, interaction));
        explosions(ctx, at, next, interaction, depth + 1, seen, budget);
      }));
    }
  }

  public static int[] explosionBudget() { return new int[]{MAX_EXTRA_EXPLOSIONS}; }

  /** Arco de dano: a partir de {@code first}, salta para a criatura mais proxima ainda nao atingida. */
  public static void arc(ExecutionContext ctx, LivingEntity first, float amount, DamageSource source, Set<UUID> seen) {
    ServerLevel level = ctx.level();
    LivingEntity from = first;
    float dmg = amount;
    for (int hop = 0; hop < MAX_HOPS; hop++) {
      dmg *= (float) FALLOFF;
      final LivingEntity origin = from;
      LivingEntity best = null;
      double bestD = Double.MAX_VALUE;
      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, origin.getBoundingBox().inflate(ARC_RANGE),
          x -> x.isAlive() && x != ctx.caster() && !seen.contains(x.getUUID()))) {
        double d = e.distanceToSqr(origin);
        if (d < bestD) { bestD = d; best = e; }
      }
      if (best == null) return;
      seen.add(best.getUUID());
      Vec3 a = origin.position().add(0, origin.getBbHeight() * 0.5, 0);
      Vec3 b = best.position().add(0, best.getBbHeight() * 0.5, 0);
      com.arcane.network.ModNetwork.sendBeam(level, a, b, SpellElement.of(ctx.plan()).color(), SpellElement.of(ctx.plan()).ordinal(), 8);
      best.hurt(source, dmg);
      from = best;
    }
  }

  public static Set<UUID> seenOf(Iterable<? extends net.minecraft.world.entity.Entity> hit) {
    Set<UUID> seen = new HashSet<>();
    for (var e : hit) seen.add(e.getUUID());
    return seen;
  }
}
