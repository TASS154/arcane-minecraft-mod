package com.arcane.client.hud;

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
        g.fill(x, y, x + SLOT - 2, y + SLOT - 2, active ? 0xC0F0E0B0 : 0x90000000);
        g.renderOutline(x, y, SLOT - 2, SLOT - 2, active ? 0xFFC08000 : 0xFF6A5A40);
        SealRenderer.draw(g, s.glyphs().stream().map(ClientArcaneData.GlyphEntry::id).toList(), x + (SLOT - 2) / 2, y + (SLOT - 2) / 2, 8);
        g.drawString(font, String.valueOf(i + 1), x + 2, y + 1, active ? 0x2A1A08 : 0xFFFFFF, false);
        // letra do modo: nao depende so de cor (acessibilidade)
        g.drawString(font, new String[]{"T", "C", "H", "A"}[Math.floorMod(s.mode(), 4)], x + SLOT - 9, y + SLOT - 11, active ? 0x2A1A08 : 0xC8C8C8, false);
      }
      Component label = numbered ? Component.translatable("hud.arcane.keys") : Component.translatable("hud.arcane.loadout", ClientArcaneData.activeLoadout() + 1);
      g.drawCenteredString(font, label, width / 2, y - 10, 0xD0B0FF);
    }
    double charge = ClientCasting.chargeProgress();
    if (charge >= 0) {
      int w = 80, x = width / 2 - w / 2, by = height - 62 - (shown.isEmpty() ? 0 : 0);
      g.fill(x - 1, by - 1, x + w + 1, by + 5, 0xFF000000);
      int c = charge >= 1.0 ? 0xFFFFD040 : 0xFF5AB0F0;
      g.fill(x, by, x + (int) (w * charge), by + 4, c);
      g.drawCenteredString(font, Component.translatable(charge >= 1.0 ? "hud.arcane.charged" : "hud.arcane.charging"), width / 2, by - 10, 0xFFFFFF);
    }
  }
}
