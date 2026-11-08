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
        if (com.arcane.execution.Chain.enabled(call)) {
            java.util.Set<java.util.UUID> seen = com.arcane.execution.Chain.seenOf(targets.entities());
            seen.add(context.caster().getUUID());
            for (Entity entity : targets.entities()) {
                if (entity instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive()) {
                    com.arcane.execution.Chain.arc(context, living, amount, source, seen);
                }
            }
        }
        // Blocos muito frageis (vidro, folhas, gelo fino...) racham sob o dano; nada mais.
        if (!com.arcane.execution.Targeting.mayBreakBlocks(context)) return;
        int broken = 0;
        for (var block : targets.blocks()) {
            if (broken >= 2) break;
            var state = context.level().getBlockState(block.pos());
            float hardness = state.getDestroySpeed(context.level(), block.pos());
            if (!state.isAir() && hardness >= 0.0f && hardness <= 0.3f) {
                context.level().destroyBlock(block.pos(), false, context.caster());
                broken++;
            }
        }
    }
}
