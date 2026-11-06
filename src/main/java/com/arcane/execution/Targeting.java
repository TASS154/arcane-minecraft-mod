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
      trail(level, start.add(ctx.look().scale(0.6)), hitPoint);
    }
    return new RayResult(targets, hitPoint);
  }

  private static void trail(ServerLevel level, Vec3 from, Vec3 to) {
    Vec3 delta = to.subtract(from);
    double length = delta.length();
    if (length < 0.1) return;
    Vec3 step = delta.normalize().scale(0.6);
    int count = Math.min(90, (int) (length / 0.6));
    Vec3 p = from;
    for (int i = 0; i < count; i++) {
      level.sendParticles(ModParticles.ARCANE_SPARK.get(), p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
      p = p.add(step);
    }
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
