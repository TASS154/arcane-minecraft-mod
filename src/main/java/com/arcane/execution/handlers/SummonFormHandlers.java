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
import com.arcane.entity.ArcaneGolemEntity;
import com.arcane.entity.SpectralWolfEntity;
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
      int tier = (int) Math.max(0, Math.min(2, Math.round(spec.parameters().getOrDefault("size", 1.0)) - 1));
      LivingEntity caster = context.caster();
      List<Entity> summoned = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        SpectralWolfEntity wolf = com.arcane.registry.ModEntities.SPECTRAL_WOLF.get().create(context.level());
        if (wolf == null) continue;
        wolf.setTier(tier);
        Vec3 p = spot(context, i, count);
        wolf.moveTo(p.x, p.y, p.z, caster.getYRot(), 0f);
        if (caster instanceof Player player) wolf.tame(player);
        wolf.setHealth(wolf.getMaxHealth());
        wolf.setPersistenceRequired();
        Summons.mark(wolf, until);
        context.level().addFreshEntity(wolf);
        context.level().sendParticles(com.arcane.execution.SpellElement.of(context.plan()).particle(), p.x, p.y + 0.5, p.z, 10, 0.3, 0.4, 0.3, 0.05);
        summoned.add(wolf);
      }
      return DeliveryResult.of(new TargetList(summoned, List.of()));
    }
  }

  /** Golem Runico protetor: o mago escolhe quantos (1 a 3) e o tamanho/nivel (1 a 3, cada um com modelo proprio). */
  public static class Golem implements FormHandler {
    @Override
    public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
      long until = context.level().getGameTime() + (long) Math.max(200, Math.min(24000, spec.parameters().getOrDefault("duration", 2400.0)));
      int count = (int) Math.max(1, Math.min(3, Math.round(spec.parameters().getOrDefault("count", 1.0))));
      int tier = (int) Math.max(0, Math.min(2, Math.round(spec.parameters().getOrDefault("size", 1.0)) - 1));
      List<Entity> summoned = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        ArcaneGolemEntity golem = com.arcane.registry.ModEntities.ARCANE_GOLEM.get().create(context.level());
        if (golem == null) continue;
        golem.setTier(tier);
        // varios golens ficam lado a lado, a frente de quem conjurou
        double side = (i - (count - 1) / 2.0) * (2.6 + tier);
        Vec3 look = context.look();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        Vec3 p = context.caster().position().add(look.x * (2.5 + tier), 0.1, look.z * (2.5 + tier)).add(right.scale(side));
        golem.moveTo(p.x, p.y, p.z, context.caster().getYRot(), 0f);
        golem.setPlayerCreated(context.caster() instanceof Player);
        golem.setPersistenceRequired();
        Summons.mark(golem, until);
        context.level().addFreshEntity(golem);
        context.level().sendParticles(com.arcane.execution.SpellElement.of(context.plan()).particle(), p.x, p.y + 1.0, p.z, 24, 0.5, 0.8, 0.5, 0.06);
        summoned.add(golem);
      }
      return DeliveryResult.of(new TargetList(summoned, List.of()));
    }
  }
}
