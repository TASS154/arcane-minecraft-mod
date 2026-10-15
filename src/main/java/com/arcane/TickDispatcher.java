package com.arcane;

import com.arcane.ambient.AmbientFieldResolver;
import com.arcane.ambient.ManaPoisoningService;
import com.arcane.execution.EnvironmentalSnapshot;
import com.arcane.execution.scheduler.SpellTickScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TickDispatcher {
    private static final int AMBIENT_INTERVAL_TICKS = 20;

    private TickDispatcher() {}

    public static void onServerTick(MinecraftServer server) {
        SpellTickScheduler.tick(server);
        if (server.getTickCount() % AMBIENT_INTERVAL_TICKS == 0) {
            tickAmbient(server);
        }
    }

    private static void tickAmbient(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            EnvironmentalSnapshot environment = AmbientFieldResolver.resolve(player.serverLevel(), player.blockPosition());
            ManaPoisoningService.tick(player, environment);
        }
    }
}
