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
        java.util.List<net.minecraft.core.BlockPos> spots = new java.util.ArrayList<>();
        for (var block : targets.blocks()) spots.add(block.pos());
        for (var point : targets.points()) {
            var cell = com.arcane.execution.Targeting.groundCell(context.level(), point);
            if (cell.isPresent()) { spots.add(cell.get()); spots.add(cell.get().below()); }
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
