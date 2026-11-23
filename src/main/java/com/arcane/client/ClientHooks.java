package com.arcane.client;

import com.arcane.client.screen.GrimoireScreen;
import net.minecraft.client.Minecraft;

/** Pontos de entrada do cliente chamados por codigo comum (via DistExecutor). */
public final class ClientHooks {
  private ClientHooks() {}

  /** Altar Arcano: abre a tela do ritual (desenhar a magia e fazer as oferendas), nao o grimorio nem a bancada. */
  public static void openRitual(net.minecraft.core.BlockPos altar) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player != null) mc.setScreen(new com.arcane.client.screen.RitualAltarScreen(altar));
  }

  public static void openScepter() {
    var mc = net.minecraft.client.Minecraft.getInstance();
    if (mc.player != null) mc.setScreen(new com.arcane.client.screen.ScepterScreen());
  }

  public static void openGrimoire() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player != null) {
      mc.setScreen(new GrimoireScreen());
    }
  }
}
