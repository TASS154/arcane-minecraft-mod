package com.arcane.execution;

import com.arcane.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Utilitarios de mira e de posicao dos alvos, compartilhados pelas formas e efeitos. */
public final class Targeting {
  private Targeting() {}

  public record RayResult(TargetList targets, Vec3 end) {}

  /** Alcance maximo da mira para o glifo Mira (origem). */
  public static final double AIM_RANGE = 32.0;

  /** Centro de uma forma: o conjurador, ou o ponto mirado se o glifo Mira gravou origin=1. */
  public static Vec3 center(ExecutionContext ctx, DeliverySpec spec) {
    if (spec.parameters().getOrDefault("origin", 0.0) >= 0.5) return ray(ctx, AIM_RANGE, false).end();
    return ctx.position();
  }

  /** Raio do olhar do conjurador: primeira criatura ou bloco no caminho, dentro do alcance. */
  public static RayResult ray(ExecutionContext ctx, double range, boolean particles) {
    ServerLevel level = ctx.level();
    LivingEntity caster = ctx.caster();
    Vec3 start = ctx.origin();
    Vec3 end = start.add(ctx.look().scale(range));
    BlockHitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
    Vec3 blockEnd = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
    EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, caster, start, blockEnd,
        new AABB(start, blockEnd).inflate(1.0), e -> e.isPickable() && !e.isSpectator() && e.isAlive() && e != caster);

    TargetList targets;
    Vec3 hitPoint;
    if (entity != null) {
      targets = TargetList.ofEntities(entity.getEntity());
      hitPoint = entity.getLocation();
    } else if (block.getType() != HitResult.Type.MISS) {
      BlockPos pos = block.getBlockPos();
      targets = new TargetList(List.of(), List.of(new TargetList.BlockTarget(pos, level.getBlockState(pos))));
      hitPoint = block.getLocation();
    } else {
      targets = TargetList.ofPoint(end);
      hitPoint = end;
    }
    if (particles) {
      // O feixe nasce de um ponto diferente a cada conjuracao, num circulo em volta do conjurador,
      // mas sempre termina onde o jogador mira (hitPoint).
      var rnd = caster.getRandom();
      Vec3 look = ctx.look();
      Vec3 right = look.cross(new Vec3(0, 1, 0));
      right = right.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : right.normalize();
      Vec3 upAxis = right.cross(look).normalize();
      double ang = rnd.nextDouble() * Math.PI * 2;
      double rad = 0.8 + rnd.nextDouble() * 0.9;
      Vec3 beamStart = start.add(look.scale(0.9 + rnd.nextDouble() * 0.6))
          .add(right.scale(Math.cos(ang) * rad))
          .add(upAxis.scale(Math.max(-0.3, Math.sin(ang) * rad)));
      SpellElement element = SpellElement.of(ctx.plan());
      com.arcane.network.ModNetwork.sendShape(level, com.arcane.network.packet.S2CShapeFxPacket.IMPACT, hitPoint,
          look.scale(-1), 0.9f, element.color(), element.ordinal(), 12);
      ElementSounds.playImpact(level, hitPoint, element);
      WorldScars.leave(ctx, hitPoint);
      com.arcane.network.ModNetwork.sendBeam(level, beamStart, hitPoint, element.color(), element.ordinal(), 14);
    }
    return new RayResult(targets, hitPoint);
  }

  /** Posicoes de impacto: centro das criaturas e centro dos blocos. */
  public static List<Vec3> positions(TargetList targets) {
    List<Vec3> out = new ArrayList<>();
    for (Entity e : targets.entities()) out.add(e.position().add(0, e.getBbHeight() * 0.5, 0));
    for (TargetList.BlockTarget b : targets.blocks()) out.add(Vec3.atCenterOf(b.pos()));
    out.addAll(targets.points());
    return out;
  }

  /**
   * Celula de ar logo acima do chao perto de um ponto (ate 4 blocos abaixo): onde fogo, nuvens e plantas "pousam".
   * Vazio se nao ha chao por perto (o ponto esta no ceu).
   */
  public static java.util.Optional<BlockPos> groundCell(net.minecraft.world.level.Level level, Vec3 point) {
    BlockPos cell = BlockPos.containing(point.add(0, 0.5, 0));
    for (int i = 0; i <= 5; i++) {
      BlockPos below = cell.below();
      var st = level.getBlockState(below);
      if (!st.isAir() && st.isFaceSturdy(level, below, net.minecraft.core.Direction.UP) && level.getBlockState(cell).canBeReplaced()) {
        return java.util.Optional.of(cell);
      }
      cell = below;
    }
    return java.util.Optional.empty();
  }

  /** Efeitos que alteram o mundo: jogadores sempre; criaturas so com mobGriefing ligado. */
  public static boolean mayAlterWorld(ExecutionContext ctx) {
    return ctx.caster() instanceof Player || ctx.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
  }
}
