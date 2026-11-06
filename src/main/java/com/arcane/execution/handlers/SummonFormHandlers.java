package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.Summons;
import com.arcane.execution.TargetList;
import com.arcane.registry.ModParticles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Formas de invocacao (so existem como ritual). As criaturas invocadas sao os alvos do que vem depois. */
public final class SummonFormHandlers {
  private SummonFormHandlers() {}

  private static Vec3 spot(ExecutionContext context, int index, int total) {
    double angle = Math.PI * 2 * index / Math.max(1, total) + 0.6;
    return context.caster().position().add(Math.cos(angle) * 2.0, 0.1, Math.sin(angle) * 2.0);
  }

  /** Lobos domesticados (nao agridem quem os invocou). */
  public static class Wolves implements FormHandler {
    @Override
    public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
      int count = (int) Math.max(1, Math.min(8, Math.round(spec.parameters().getOrDefault("count", 3.0))));
      long until = context.level().getGameTime() + (long) Math.max(100, Math.min(12000, spec.parameters().getOrDefault("duration", 1200.0)));
      LivingEntity caster = context.caster();
      List<Entity> summoned = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        Wolf wolf = EntityType.WOLF.create(context.level());
        if (wolf == null) continue;
        Vec3 p = spot(context, i, count);
        wolf.moveTo(p.x, p.y, p.z, caster.getYRot(), 0f);
        if (caster instanceof Player player) wolf.tame(player);
        wolf.setPersistenceRequired();
        Summons.mark(wolf, until);
        context.level().addFreshEntity(wolf);
        context.level().sendParticles(com.arcane.execution.SpellElement.of(context.plan()).particle(), p.x, p.y + 0.5, p.z, 10, 0.3, 0.4, 0.3, 0.05);
        summoned.add(wolf);
      }
      return DeliveryResult.of(new TargetList(summoned, List.of()));
    }
  }

  /** Golem de ferro protetor. */
  public static class Golem implements FormHandler {
    @Override
    public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
      long until = context.level().getGameTime() + (long) Math.max(200, Math.min(24000, spec.parameters().getOrDefault("duration", 2400.0)));
      IronGolem golem = EntityType.IRON_GOLEM.create(context.level());
      if (golem == null) return DeliveryResult.of(TargetList.empty());
      Vec3 p = context.caster().position().add(context.look().x * 2.5, 0.1, context.look().z * 2.5);
      golem.moveTo(p.x, p.y, p.z, context.caster().getYRot(), 0f);
      golem.setPlayerCreated(context.caster() instanceof Player);
      golem.setPersistenceRequired();
      Summons.mark(golem, until);
      context.level().addFreshEntity(golem);
      context.level().sendParticles(com.arcane.execution.SpellElement.of(context.plan()).particle(), p.x, p.y + 1.0, p.z, 24, 0.5, 0.8, 0.5, 0.06);
      return DeliveryResult.of(TargetList.ofEntities(golem));
    }
  }
}
