package com.arcane.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Clarao curto nas bordas da tela, na cor do elemento, ao conjurar (feedback imediato). */
public class CastFlashOverlay implements IGuiOverlay {
  private static final long DURATION_MS = 380L;
  private static long startMs = 0L;
  private static int rgb = 0xFFFFFF;

  public static void trigger(int color) {
    rgb = color;
    startMs = System.currentTimeMillis();
  }

  @Override
  public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
    long elapsed = System.currentTimeMillis() - startMs;
    if (elapsed >= DURATION_MS || Minecraft.getInstance().options.hideGui) return;
    float t = 1f - elapsed / (float) DURATION_MS;
    int alpha = (int) (110 * t * t);
    int edge = (alpha << 24) | (rgb & 0xFFFFFF);
    int clear = rgb & 0xFFFFFF;
    int band = Math.max(24, height / 6);
    g.fillGradient(0, 0, width, band, edge, clear);
    g.fillGradient(0, height - band, width, height, clear, edge);
    int side = Math.max(24, width / 10);
    g.fillGradient(0, 0, side, height, edge, clear);
    g.fillGradient(width - side, 0, width, height, clear, edge);
  }
}
