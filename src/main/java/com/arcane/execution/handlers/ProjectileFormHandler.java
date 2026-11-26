package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.entity.ArcaneProjectileEntity;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.registry.ModEntities;

import java.util.List;

public class ProjectileFormHandler implements FormHandler {
    @Override
    public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
        ArcaneProjectileEntity projectile = new ArcaneProjectileEntity(ModEntities.ARCANE_PROJECTILE.get(), context.level());
        projectile.setOwner(context.caster());
        projectile.setPos(context.origin().x, context.origin().y, context.origin().z);
        // Mais Proximo logo depois da forma: o orbe sai na direcao da criatura mais proxima (pode errar; nao teletransporta)
        net.minecraft.world.phys.Vec3 aim = context.look();
        if (com.arcane.execution.Seek.on(spec)) {
            var target = com.arcane.execution.Seek.nearest(context, com.arcane.execution.Seek.PROJECTILE_RANGE, true);
            if (target != null) aim = com.arcane.execution.Seek.aimPoint(target).subtract(context.origin()).normalize();
        }
        projectile.shoot(aim.x, aim.y, aim.z,
                (float) Math.max(0.5, Math.min(4.0, spec.parameters().getOrDefault("speed", 1.5))),
                0.5F);
        projectile.setInscription(context.inscription());
        projectile.setPlan(com.arcane.inscription.CompiledPlanCache.getOrCompile(context.inscription()));
        projectile.setPrecision(context.precision());
        projectile.setEnvironment(context.environment());
        projectile.setRitualBonus(context.geometryBonus(), context.areaScale(), context.ritualLevel());
        // Fase J: Perseguir, Ricochete, Fragmentar e Arco, degradados para caber no orcamento de disparos
        var limits = com.arcane.execution.ProjectileMods.limits();
        var mods = com.arcane.execution.ProjectileMods.budgeted(com.arcane.execution.ProjectileMods.of(spec.parameters()), limits.maxTriggers());
        projectile.setMods(mods, limits);
        if (mods.arc() > 0) {
            var from = context.origin();
            var to = arcTarget(context);
            projectile.setDeltaMovement(com.arcane.execution.ProjectileMods.arcLaunch(from, to, mods.arc(),
                    com.arcane.execution.ProjectileMods.ARC_GRAVITY));
        }
        context.level().addFreshEntity(projectile);
        return DeliveryResult.stop(TargetList.empty());
    }

    /** Arco: o ponto mirado (primeiro bloco na mira ate 48 blocos; sem bloco, 24 blocos a frente). */
    static net.minecraft.world.phys.Vec3 arcTarget(ExecutionContext context) {
        var from = context.origin();
        var end = from.add(context.look().normalize().scale(48));
        var hit = context.level().clip(new net.minecraft.world.level.ClipContext(from, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, context.caster()));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) return hit.getLocation();
        return from.add(context.look().normalize().scale(24));
    }
}
