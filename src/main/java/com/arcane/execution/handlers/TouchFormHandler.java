package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TouchFormHandler implements FormHandler {
    @Override
    public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
        double range = context.amplify(spec.range(), List.of("range"), "range");
        Vec3 end = context.origin().add(context.look().scale(range));
        AABB box = context.caster().getBoundingBox().expandTowards(context.look().scale(range)).inflate(1.0);
        List<Entity> hits = context.level().getEntities(context.caster(), box,
                e -> e.isPickable() && e != context.caster());
        if (!hits.isEmpty()) {
            return DeliveryResult.of(TargetList.ofEntities(hits.get(0)));
        }
        return DeliveryResult.of(TargetList.empty());
    }
}
