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
        // Em bloco ou ponto vazio: uma onda de choque empurra quem esta perto (menos o conjurador).
        java.util.List<Vec3> centers = new java.util.ArrayList<>(targets.points());
        for (var block : targets.blocks()) centers.add(Vec3.atCenterOf(block.pos()));
        for (Vec3 c : centers) {
            for (Entity near : context.level().getEntities(context.caster(), new net.minecraft.world.phys.AABB(c, c).inflate(2.5), e -> e.isAlive() && e != context.caster())) {
                Vec3 away = near.position().subtract(c);
                Vec3 dir = away.lengthSqr() < 0.04 ? new Vec3(0, 1, 0) : away.normalize();
                near.push(dir.x * strength * 0.6, 0.3 * strength * 0.6 + 0.1, dir.z * strength * 0.6);
                near.hurtMarked = true;
            }
        }
    }
}
