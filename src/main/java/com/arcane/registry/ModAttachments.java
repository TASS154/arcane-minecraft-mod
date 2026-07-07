package com.arcane.registry;

import com.arcane.ArcaneMod;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ModAttachments {
    private ModAttachments() {}

    public static void register(IEventBus modBus) {
        ModCapabilities.register(modBus);
    }
}
