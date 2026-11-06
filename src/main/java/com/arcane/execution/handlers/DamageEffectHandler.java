package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class DamageEffectHandler implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
        float amount = (float) context.amplify(call.parameters().getOrDefault("amount", 4.0), call.magnitudeKeys(), "amount");
        DamageSource source = context.level().damageSources().indirectMagic(context.caster(), context.caster());
        com.arcane.execution.SpellElement element = com.arcane.execution.SpellElement.of(context.plan());
        for (Entity entity : targets.entities()) {
            double mult = com.arcane.execution.ElementInteractions.damageMultiplier(context.level(), element, entity);
            entity.hurt(source, (float) (amount * mult));
            com.arcane.execution.ElementInteractions.afterHit(context.level(), element, entity);
        }
    }
}
