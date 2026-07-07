package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class PushEffectHandler implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
        double strength = context.amplify(call.parameters().getOrDefault("strength", 1.0), call.magnitudeKeys(), "strength");
        Vec3 push = context.look().scale(strength);
        for (Entity entity : targets.entities()) {
            entity.push(push);
        }
    }
}
