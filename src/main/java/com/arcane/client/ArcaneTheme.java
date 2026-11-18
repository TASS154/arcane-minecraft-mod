package com.arcane.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Tema visual "arcano luminoso": fundos escuros indigo, tracos de luz ciano e violeta, dourado so para o que esta ativo.
 * Funcoes de desenho compartilhadas pelo HUD e pela roda, para as telas falarem a mesma lingua.
 */
public final class ArcaneTheme {
  public static final int INK = 0xFF0E0A1C;
  public static final int PANEL_TOP = 0xE61A1236;
  public static final int PANEL_BOTTOM = 0xE6100A24;
  public static final int EDGE = 0xFF4A3A8C;
  public static final int EDGE_LIT = 0xFF6FE0FF;
  public static final int VIOLET = 0xFFC9A6FF;
  public static final int GOLD = 0xFFFFD36B;
  public static final int TEXT = 0xFFF2EAFF;
  public static final int TEXT_DIM = 0xFFB6AADC;
  public static final int DANGER = 0xFFFF7A66;

  private ArcaneTheme() {}

  public static int alpha(int argb, float a) {
    int base = (argb >>> 24) & 255;
    return ((int) Mth.clamp(base * a, 0, 255) << 24) | (argb & 0xFFFFFF);
  }

  public static int mix(int a, int b, float t) {
    int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
    int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
    return 0xFF000000 | ((int) Mth.lerp(t, ar, br) << 16) | ((int) Mth.lerp(t, ag, bg) << 8) | (int) Mth.lerp(t, ab, bb);
  }

  /** Painel de vidro escuro com filete e cantos acesos; {@code glow} (0 a 1) acende a moldura. */
  public static void panel(GuiGraphics g, int x, int y, int w, int h, int edge, float glow) {
    g.fillGradient(x, y, x + w, y + h, PANEL_TOP, PANEL_BOTTOM);
    if (glow > 0.01f) {
      for (int i = 3; i >= 1; i--) g.renderOutline(x - i, y - i, w + i * 2, h + i * 2, alpha(edge, glow * (0.16f / i)));
    }
    g.renderOutline(x, y, w, h, edge);
    int c = Math.min(4, Math.min(w, h) / 3);
    int lit = glow > 0.01f ? 0xFFFFFFFF : alpha(edge, 1f);
    g.fill(x, y, x + c, y + 1, lit); g.fill(x, y, x + 1, y + c, lit);
    g.fill(x + w - c, y, x + w, y + 1, lit); g.fill(x + w - 1, y, x + w, y + c, lit);
    g.fill(x, y + h - 1, x + c, y + h, lit); g.fill(x, y + h - c, x + 1, y + h, lit);
    g.fill(x + w - c, y + h - 1, x + w, y + h, lit); g.fill(x + w - 1, y + h - c, x + w, y + h, lit);
  }

  /** Barra de recurso: fundo escuro, preenchimento em degrade, brilho que passa e marcas de escala. */
  public static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int left, int right, long time) {
    fraction = Mth.clamp(fraction, 0f, 1f);
    g.fill(x - 1, y - 1, x + w + 1, y + h + 1, INK);
    g.fill(x, y, x + w, y + h, 0xFF1C1438);
    int fw = Math.round(w * fraction);
    if (fw > 0) {
      g.fillGradient(x, y, x + fw, y + h, mix(left, right, 0f), mix(left, right, 1f));
      // linha de luz no topo e brilho que corre pela barra
      g.fill(x, y, x + fw, y + 1, alpha(0xFFFFFFFF, 0.55f));
      int sheen = (int) ((time * 2L) % (w + 24)) - 12;
      for (int i = 0; i < 6; i++) {
        int sx = x + sheen + i;
        if (sx >= x && sx < x + fw) g.fill(sx, y + 1, sx + 1, y + h - 1, alpha(0xFFFFFFFF, 0.10f + 0.03f * (3 - Math.abs(i - 3))));
      }
    }
    for (int i = 1; i < 10; i++) g.fill(x + w * i / 10, y + h - 2, x + w * i / 10 + 1, y + h, alpha(0xFF000000, 0.45f));
    g.renderOutline(x - 1, y - 1, w + 2, h + 2, EDGE);
  }

  /** Cor de acento por elemento (indice de SpellElement.ordinal) quando o cliente so tem o numero. */
  public static int accentFor(int rgb) { return 0xFF000000 | (rgb & 0xFFFFFF); }
}
