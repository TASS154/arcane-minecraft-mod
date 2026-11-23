package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.world.entity.LivingEntity;

public class HealEffectHandler implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
        float amount = (float) context.amplify(call.parameters().getOrDefault("amount", 4.0), call.magnitudeKeys(), "amount");
        for (var entity : targets.entities()) {
            if (entity instanceof LivingEntity living) {
                living.heal(amount);
            }
        }
        // Plantas crescem onde a cura toca (como farinha de osso); no ar, nada acontece.
        if (!com.arcane.execution.Targeting.mayAlterWorld(context)) return;
        int grown = 0;
        // A plantacao fica sobre terra arada (que nao e "solida" em cima), entao em vez de procurar o chao olha o
        // proprio ponto, um acima e dois abaixo: acha a planta mirada direto, a muda no chao ou a planta sob os pes.
        java.util.Set<net.minecraft.core.BlockPos> spots = new java.util.LinkedHashSet<>();
        for (var block : targets.blocks()) { spots.add(block.pos()); spots.add(block.pos().above()); }
        // Curar uma criatura que esta no meio da plantacao tambem cura as plantas aos pes dela.
        for (var e : targets.entities()) spots.add(e.blockPosition());
        for (var point : targets.points()) {
            var c = net.minecraft.core.BlockPos.containing(point);
            spots.add(c); spots.add(c.below()); spots.add(c.above()); spots.add(c.below(2));
        }
        for (var pos : spots) {
            if (grown >= 4) break;
            if (net.minecraft.world.item.BoneMealItem.growCrop(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE_MEAL), context.level(), pos)) {
                grown++;
                context.level().sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }
}
