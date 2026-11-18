package com.arcane.client.hud;

import com.arcane.client.ArcaneTheme;
import com.arcane.client.ClientArcaneData;
import com.arcane.client.ClientArcaneData.SpellEntry;
import com.arcane.client.ClientCasting;
import com.arcane.client.ClientPrefs;
import com.arcane.client.SealRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/**
 * Faixa de magias do conjunto ativo (selo, numero, custo e modo) acima da hotbar, mais a barra de carga do modo Carregar
 * e a magia ativa com custo e modo. Sem conjunto, mostra so a magia ativa.
 */
public class SpellBarOverlay implements IGuiOverlay {
  private static final int SLOT = 22;

  @Override
  public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.options.hideGui || mc.player == null || mc.player.isSpectator() || !ClientArcaneData.hasData() || mc.screen != null) return;
    Font font = mc.font;
    int y = height - 56;
    List<SpellEntry> set = ClientArcaneData.activeLoadoutSpells();
    boolean numbered = ClientPrefs.mode() == ClientPrefs.UseMode.CASTER;
    List<SpellEntry> shown = numbered ? ClientCasting.pickable() : set;
    if (!shown.isEmpty()) {
      int n = Math.min(shown.size(), 9);
      int x0 = width / 2 - n * SLOT / 2;
      for (int i = 0; i < n; i++) {
        SpellEntry s = shown.get(i);
        int x = x0 + i * SLOT;
        boolean active = ClientArcaneData.active().map(a -> a.equals(s.id())).orElse(false);
        ArcaneTheme.panel(g, x, y, SLOT - 2, SLOT - 2, active ? ArcaneTheme.EDGE_LIT : ArcaneTheme.EDGE, active ? 1f : 0f);
        SealRenderer.draw(g, s.glyphs().stream().map(ClientArcaneData.GlyphEntry::id).toList(), x + (SLOT - 2) / 2, y + (SLOT - 2) / 2, 8);
        g.drawString(font, String.valueOf(i + 1), x + 2, y + 1, active ? ArcaneTheme.GOLD : ArcaneTheme.TEXT_DIM, false);
        // letra do modo: nao depende so de cor (acessibilidade)
        g.drawString(font, new String[]{"T", "C", "H", "A"}[Math.floorMod(s.mode(), 4)], x + SLOT - 9, y + SLOT - 11, active ? ArcaneTheme.EDGE_LIT : ArcaneTheme.TEXT_DIM, false);
      }
      Component label = numbered ? Component.translatable("hud.arcane.keys") : Component.translatable("hud.arcane.loadout", ClientArcaneData.activeLoadout() + 1);
      g.drawCenteredString(font, label, width / 2, y - 10, ArcaneTheme.VIOLET);
    }
    double charge = ClientCasting.chargeProgress();
    if (charge >= 0) {
      int w = 96, x = width / 2 - w / 2, by = height - 66;
      long time = mc.level != null ? mc.level.getGameTime() : 0;
      boolean full = charge >= 0.999;
      ArcaneTheme.panel(g, x - 6, by - 14, w + 12, 24, full ? ArcaneTheme.GOLD : ArcaneTheme.EDGE_LIT, (float) (0.3 + 0.7 * charge));
      ArcaneTheme.bar(g, x, by, w, 5, (float) charge, full ? 0xFFFFB347 : 0xFF6A4BFF, full ? 0xFFFFE27A : 0xFF7FE8FF, time);
      g.drawCenteredString(font, Component.translatable(full ? "hud.arcane.charged" : "hud.arcane.charging"), width / 2, by - 11, full ? ArcaneTheme.GOLD : ArcaneTheme.TEXT);
    }
  }
}
