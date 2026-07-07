package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class FireEffectHandler implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
        int duration = (int) context.amplify(call.parameters().getOrDefault("duration", 60.0), call.magnitudeKeys(), "duration");
        for (Entity entity : targets.entities()) {
            entity.setSecondsOnFire(Math.max(1, duration / 20));
        }
    }
}
