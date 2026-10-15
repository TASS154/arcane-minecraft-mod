package com.arcane.execution.scheduler;

import com.arcane.execution.ExecutionContext;
import com.arcane.execution.SpellPipeline;
import com.arcane.inscription.CompiledExecutionPlan;
import net.minecraft.server.MinecraftServer;

import java.util.Comparator;
import java.util.PriorityQueue;

public final class SpellTickScheduler {
    private static final int MAX_SEGMENTS_PER_TICK = 32;
    private static final SpellPipeline PIPELINE = SpellPipeline.INSTANCE;
    private static final PriorityQueue<ScheduledSegment> PENDING = new PriorityQueue<>(
            Comparator.comparingLong(ScheduledSegment::executeAt)
                    .thenComparingLong(ScheduledSegment::sequence));

    private static long nextSequence;

    private SpellTickScheduler() {}

    public static void schedule(ExecutionContext context, CompiledExecutionPlan.ExecutionSegment segment, int delayTicks) {
        long executeAt = context.level().getServer().getTickCount() + delayTicks;
        PENDING.add(new ScheduledSegment(context, segment, executeAt, nextSequence++));
    }

    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        int processed = 0;
        while (processed < MAX_SEGMENTS_PER_TICK && !PENDING.isEmpty()) {
            ScheduledSegment scheduled = PENDING.peek();
            if (scheduled.executeAt > now) {
                break;
            }
            PENDING.poll();
            if (scheduled.context.level().getServer() == server) {
                PIPELINE.executeSegment(scheduled.context, scheduled.segment);
                processed++;
            }
        }
    }

    private record ScheduledSegment(
            ExecutionContext context,
            CompiledExecutionPlan.ExecutionSegment segment,
            long executeAt,
            long sequence
    ) {}
}
