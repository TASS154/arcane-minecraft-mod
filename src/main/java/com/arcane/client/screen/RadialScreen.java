package com.arcane.client.screen;

import com.arcane.client.ArcaneTheme;
import com.arcane.client.ClientArcaneData;
import com.arcane.client.ClientArcaneData.SpellEntry;
import com.arcane.client.ClientCasting;
import com.arcane.client.ClientPrefs;
import com.arcane.client.ClientSetup;
import com.arcane.client.SealRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Roda radial: segurar a tecla abre o circulo com as magias (selo geometrico de cada uma); soltar escolhe a que o mouse
 * aponta. Com "alternar em vez de segurar" a roda fica aberta ate um clique. Esc cancela.
 */
public class RadialScreen extends Screen {
  private static final int RADIUS = 72;
  private final List<SpellEntry> spells = ClientCasting.pickable();
  private int hovered = -1;

  public RadialScreen() {
    super(Component.translatable("gui.arcane.radial"));
  }

  @Override
  public boolean isPauseScreen() { return false; }

  private int count() { return Math.max(1, spells.size()); }

  private double angleOf(int i) { return -Math.PI / 2 + i * (2 * Math.PI / count()); }

  private void updateHover(double mx, double my) {
    double dx = mx - width / 2.0, dy = my - height / 2.0;
    if (Math.sqrt(dx * dx + dy * dy) < 18 || spells.isEmpty()) { hovered = -1; return; }
    double a = Math.atan2(dy, dx) + Math.PI / 2 + Math.PI / count();
    a = ((a % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
    hovered = Math.min(spells.size() - 1, (int) (a / (2 * Math.PI / count())));
  }

  private void choose() {
    if (hovered >= 0 && hovered < spells.size()) ClientCasting.select(spells.get(hovered));
    onClose();
  }

  @Override
  public void tick() {
    if (ClientPrefs.latch() || ClientSetup.RADIAL_KEY == null) return;
    var key = ClientSetup.RADIAL_KEY.getKey();
    if (key.getType() == InputConstants.Type.KEYSYM) {
      long window = Minecraft.getInstance().getWindow().getWindow();
      if (!InputConstants.isKeyDown(window, key.getValue())) choose();
    }
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    updateHover(mx, my);
    choose();
    return true;
  }

  /** Anel de pontos de luz em volta do centro. */
  private static void ring(GuiGraphics g, int cx, int cy, int radius, int segments, int color) {
    for (int k = 0; k < segments; k++) {
      double a = Math.PI * 2 * k / segments;
      int px = cx + (int) Math.round(Math.cos(a) * radius), py = cy + (int) Math.round(Math.sin(a) * radius);
      g.fill(px, py, px + 1, py + 1, color);
    }
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    updateHover(mouseX, mouseY);
    int cx = width / 2, cy = height / 2;
    // fundo escurecido e um disco de vidro com aneis; o cursor aponta para a magia escolhida
    g.fillGradient(0, 0, width, height, 0xB0070412, 0xB0120A28);
    long time = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
    ring(g, cx, cy, RADIUS + 34, 120, ArcaneTheme.alpha(ArcaneTheme.EDGE, 0.6f));
    ring(g, cx, cy, RADIUS - 34, 72, ArcaneTheme.alpha(ArcaneTheme.EDGE_LIT, 0.5f));
    ring(g, cx, cy, 22, 40, ArcaneTheme.alpha(ArcaneTheme.VIOLET, 0.8f));
    for (int k = 0; k < 12; k++) {
      double a = Math.PI * 2 * k / 12 + time * 0.01;
      int px = cx + (int) Math.round(Math.cos(a) * (RADIUS + 34)), py = cy + (int) Math.round(Math.sin(a) * (RADIUS + 34));
      g.fill(px - 1, py - 1, px + 2, py + 2, ArcaneTheme.alpha(ArcaneTheme.EDGE_LIT, 0.85f));
    }
    if (hovered >= 0 && hovered < spells.size()) {
      int hx = cx + (int) Math.round(Math.cos(angleOf(hovered)) * (RADIUS - 24)), hy = cy + (int) Math.round(Math.sin(angleOf(hovered)) * (RADIUS - 24));
      for (int k = 0; k <= 8; k++) g.fill(cx + (hx - cx) * k / 8, cy + (hy - cy) * k / 8, cx + (hx - cx) * k / 8 + 1, cy + (hy - cy) * k / 8 + 1, ArcaneTheme.alpha(ArcaneTheme.EDGE_LIT, 0.9f));
    }
    Component centre = Component.translatable("gui.arcane.radial.set", ClientArcaneData.activeLoadout() + 1);
    for (int i = 0; i < spells.size(); i++) {
      SpellEntry s = spells.get(i);
      int x = cx + (int) Math.round(Math.cos(angleOf(i)) * RADIUS);
      int y = cy + (int) Math.round(Math.sin(angleOf(i)) * RADIUS);
      boolean on = i == hovered;
      boolean active = ClientArcaneData.active().map(a -> a.equals(s.id())).orElse(false);
      int r = on ? 24 : 20;
      ArcaneTheme.panel(g, x - r, y - r, r * 2, r * 2, active ? ArcaneTheme.GOLD : on ? ArcaneTheme.EDGE_LIT : ArcaneTheme.EDGE, on || active ? 1f : 0f);
      SealRenderer.draw(g, s.glyphs().stream().map(ClientArcaneData.GlyphEntry::id).toList(), x, y - 3, r - 6);
      String name = font.plainSubstrByWidth(s.name().isEmpty() ? Component.translatable("gui.arcane.unnamed").getString() : s.name(), r * 2 - 2);
      g.drawString(font, name, x - font.width(name) / 2, y + r - 10, on ? ArcaneTheme.TEXT : ArcaneTheme.TEXT_DIM, false);
      if (s.favorite()) g.drawString(font, "\u2605", x - r + 2, y - r + 2, ArcaneTheme.GOLD, false);
      g.drawString(font, String.valueOf(i + 1), x + r - 8, y - r + 2, ArcaneTheme.TEXT_DIM, false);
    }
    if (hovered >= 0 && hovered < spells.size()) {
      SpellEntry s = spells.get(hovered);
      Component title = s.name().isEmpty() ? Component.translatable("gui.arcane.unnamed") : Component.literal(s.name());
      g.drawCenteredString(font, title, cx, cy - 8, ArcaneTheme.TEXT);
      g.drawCenteredString(font, Component.translatable("gui.arcane.mode." + s.mode()).append(" \u00b7 " + Math.max(0, s.cost())), cx, cy + 4, ArcaneTheme.VIOLET);
    } else {
      g.drawCenteredString(font, spells.isEmpty() ? Component.translatable("gui.arcane.radial.empty") : centre, cx, cy - 4, ArcaneTheme.VIOLET);
    }
  }
}
