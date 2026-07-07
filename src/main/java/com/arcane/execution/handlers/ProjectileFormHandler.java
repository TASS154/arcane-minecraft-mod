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
        projectile.shoot(context.look().x, context.look().y, context.look().z,
                (float) context.amplify(1.5, List.of("speed"), "speed"),
                1.0F);
        projectile.setInscription(context.inscription());
        projectile.setPlan(com.arcane.inscription.CompiledPlanCache.getOrCompile(context.inscription()));
        projectile.setPrecision(context.precision());
        projectile.setEnvironment(context.environment());
        context.level().addFreshEntity(projectile);
        return DeliveryResult.stop(TargetList.empty());
    }
}
