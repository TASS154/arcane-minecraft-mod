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
        strength = Math.min(strength, 6.0);
        for (Entity entity : targets.entities()) {
            // Longe de quem conjura; em si mesmo (ou colado), segue a direcao do olhar.
            Vec3 away = entity.position().subtract(context.caster().position());
            Vec3 direction = entity == context.caster() || away.lengthSqr() < 0.25 ? context.look() : away.normalize();
            Vec3 push = new Vec3(direction.x, entity == context.caster() ? direction.y : 0.25, direction.z).scale(strength);
            entity.push(push.x, push.y, push.z);
            entity.hurtMarked = true;
        }
    }
}
