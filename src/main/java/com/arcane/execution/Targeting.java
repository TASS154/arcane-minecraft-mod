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
      targets = TargetList.empty();
      hitPoint = end;
    }
    if (particles) {
      // sai da altura da mao direita, nao do olho
      Vec3 right = ctx.look().cross(new Vec3(0, 1, 0));
      right = right.lengthSqr() < 1.0e-4 ? Vec3.ZERO : right.normalize();
      Vec3 beamStart = start.add(ctx.look().scale(0.6)).add(right.scale(0.3)).add(0.0, -0.25, 0.0);
      SpellElement element = SpellElement.of(ctx.plan());
        com.arcane.network.ModNetwork.sendBeam(level, beamStart, hitPoint, element.color(), element == SpellElement.STORM, 12);
    }
    return new RayResult(targets, hitPoint);
  }

  /** Posicoes de impacto: centro das criaturas e centro dos blocos. */
  public static List<Vec3> positions(TargetList targets) {
    List<Vec3> out = new ArrayList<>();
    for (Entity e : targets.entities()) out.add(e.position().add(0, e.getBbHeight() * 0.5, 0));
    for (TargetList.BlockTarget b : targets.blocks()) out.add(Vec3.atCenterOf(b.pos()));
    return out;
  }

  /** Efeitos que alteram o mundo: jogadores sempre; criaturas so com mobGriefing ligado. */
  public static boolean mayAlterWorld(ExecutionContext ctx) {
    return ctx.caster() instanceof Player || ctx.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
  }
}
