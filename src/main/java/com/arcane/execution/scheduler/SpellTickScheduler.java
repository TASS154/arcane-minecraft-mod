package com.arcane.execution.scheduler;

import com.arcane.execution.ExecutionContext;
import com.arcane.execution.SpellPipeline;
import com.arcane.inscription.CompiledExecutionPlan;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayDeque;
import java.util.Queue;

public final class SpellTickScheduler {
    private static final Queue<ScheduledSegment> PENDING = new ArrayDeque<>();

    private SpellTickScheduler() {}

    public static void schedule(ExecutionContext context, CompiledExecutionPlan.ExecutionSegment segment, int delayTicks) {
        PENDING.add(new ScheduledSegment(context, segment, context.level().getServer().getTickCount() + delayTicks));
    }

    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        while (!PENDING.isEmpty() && PENDING.peek().executeAt <= now) {
            ScheduledSegment scheduled = PENDING.poll();
            if (scheduled.context.level().getServer() == server) {
                new SpellPipeline().executeSegment(scheduled.context, scheduled.segment);
            }
        }
    }

    private record ScheduledSegment(ExecutionContext context, CompiledExecutionPlan.ExecutionSegment segment, long executeAt) {}
}
