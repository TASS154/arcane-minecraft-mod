package com.arcane.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Nuvem de Vapor (Fase K), so para quem esta dentro: um veu cinza-claro que fecha a visao pelas bordas e cobre a tela de
 * leve. Entra e sai devagar (nada de clarao). Com "reduzir claroes", o veu e mais fraco e so nas bordas. NAO visto em jogo.
 */
public class VaporOverlay implements IGuiOverlay {
  private static final long FADE_MS = 600L;
  private static long untilMs = 0L;
  private static float level = 0f;
  private static long lastMs = 0L;

  /** O servidor avisou: dentro da nuvem por mais {@code ticks} ticks. */
  public static void inside(int ticks) {
    untilMs = Math.max(untilMs, System.currentTimeMillis() + ticks * 50L);
  }

  /** Intensidade do veu (0 a 1), suavizada no tempo (testes de unidade). */
  public static float approach(float current, boolean in, long dtMs) {
    float step = Math.min(1f, dtMs / (float) FADE_MS);
    return in ? Math.min(1f, current + step) : Math.max(0f, current - step);
  }

  @Override
  public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
    long now = System.currentTimeMillis();
    long dt = lastMs == 0 ? 0 : Math.min(200, now - lastMs);
    lastMs = now;
    level = approach(level, now < untilMs, dt);
    if (level <= 0f || Minecraft.getInstance().options.hideGui) return;
    boolean calm = com.arcane.client.ClientPrefs.reduceFlashes();
    int fill = (int) ((calm ? 40 : 95) * level);
    int edgeA = (int) ((calm ? 110 : 190) * level);
    int rgb = 0xD8DEE4;
    if (!calm) g.fill(0, 0, width, height, (fill << 24) | rgb);
    int edge = (edgeA << 24) | rgb;
    int clear = rgb;
    int band = Math.max(30, height / 4);
    g.fillGradient(0, 0, width, band, edge, clear);
    g.fillGradient(0, height - band, width, height, clear, edge);
    int side = Math.max(30, width / 5);
    g.fillGradient(0, 0, side, height, edge, clear);
    g.fillGradient(width - side, 0, width, height, clear, edge);
  }
}
