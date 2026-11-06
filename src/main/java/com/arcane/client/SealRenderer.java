package com.arcane.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Desenha o selo geometrico de uma magia (ver SealGeometry). As cores sao escuras, para ler sobre o pergaminho. */
public final class SealRenderer {
  private SealRenderer() {}

  private static int colorFor(String category) {
    return switch (category) {
      case "form" -> 0xFF1E82AA;
      case "effect" -> 0xFFBE4628;
      case "filter" -> 0xFF468C3C;
      case "modifier" -> 0xFFAA7814;
      case "condition" -> 0xFF6E3CAA;
      case "activator" -> 0xFF5A6473;
      default -> 0xFF7A6A50;
    };
  }

  public static void draw(GuiGraphics g, List<ResourceLocation> glyphs, int cx, int cy, int maxRadius) {
    int n = glyphs.size();
    if (n == 0) return;
    // O primeiro glifo e o anel mais interno; cada novo envolve o anterior. Os maiores sao desenhados primeiro.
    for (int i = n - 1; i >= 0; i--) {
      ResourceLocation id = glyphs.get(i);
      String category = ClientArcaneData.glyph(id).map(ClientArcaneData.KnownGlyph::category).orElse("unknown");
      double radius = SealGeometry.radiusOf(i, n, maxRadius);
      int color = colorFor(category);
      for (int[] p : SealGeometry.ring(category, id.toString(), cx, cy, radius)) {
        g.fill(p[0], p[1], p[0] + 1, p[1] + 1, color);
      }
    }
    g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFF2A1A08);
  }
}
