package com.arcane.client;

import com.arcane.client.screen.GrimoireScreen;
import net.minecraft.client.Minecraft;

/** Pontos de entrada do cliente chamados por codigo comum (via DistExecutor). */
public final class ClientHooks {
  private ClientHooks() {}

  public static void openGrimoire() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player != null) {
      mc.setScreen(new GrimoireScreen());
    }
  }
}
