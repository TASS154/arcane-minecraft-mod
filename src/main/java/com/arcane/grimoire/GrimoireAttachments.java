package com.arcane.grimoire;

import com.arcane.execution.ManaState;
import com.arcane.registry.ModCapabilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class GrimoireAttachments {
    private GrimoireAttachments() {}

    public static Optional<GrimoireState> get(Player player) {
        return player.getCapability(ModCapabilities.GRIMOIRE).resolve();
    }

    public static GrimoireState getOrCreate(Player player) {
        return player.getCapability(ModCapabilities.GRIMOIRE).orElse(new GrimoireState());
    }

    public static Optional<ManaState> getMana(Player player) {
        return player.getCapability(ModCapabilities.MANA).resolve();
    }

    public static ManaState getManaOrCreate(Player player) {
        return player.getCapability(ModCapabilities.MANA).orElse(new ManaState());
    }

    public static void updateManaMultiplier(ServerPlayer player) {
        getMana(player).ifPresent(mana -> {
            boolean hasGrimoire = get(player).map(GrimoireState::grimoirePresent).orElse(false);
            mana.setMaxMultiplier(hasGrimoire ? 1.0f : 0.5f);
        });
    }
}
