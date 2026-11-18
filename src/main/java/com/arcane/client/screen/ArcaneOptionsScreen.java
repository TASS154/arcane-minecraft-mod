package com.arcane.client.screen;

import com.arcane.client.ClientPrefs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Opcoes do Arcane (tambem abre pelo botao "Config" da aba Mods). Coluna da esquerda: preferencias do jogador (modo de uso,
 * acessibilidade; salvas em arcane-client.toml). Coluna da direita: regras do mundo (config do servidor), editaveis
 * quando voce esta num mundo local (um jogo solo, ou um mundo aberto para a rede); em servidor dedicado
 * mexa em serverconfig/arcane-server.toml.
 */
public class ArcaneOptionsScreen extends Screen {
  private final Screen parent;

  public ArcaneOptionsScreen(Screen parent) {
    super(Component.translatable("gui.arcane.options"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int left = width / 2 - 100, w = 200, y = height / 2 - 70;
    addRenderableWidget(Button.builder(modeLabel(), b -> { ClientPrefs.cycleMode(); b.setMessage(modeLabel()); })
        .bounds(left, y, w, 20).tooltip(Tooltip.create(Component.translatable("gui.arcane.options.mode.tip"))).build());
    addRenderableWidget(Button.builder(flag("gui.arcane.options.latch", ClientPrefs.latch()), b -> { ClientPrefs.flip(ClientPrefs.LATCH_KEYS); b.setMessage(flag("gui.arcane.options.latch", ClientPrefs.latch())); })
        .bounds(left, y + 24, w, 20).tooltip(Tooltip.create(Component.translatable("gui.arcane.options.latch.tip"))).build());
    addRenderableWidget(Button.builder(flag("gui.arcane.options.colorblind", ClientPrefs.colorblind()), b -> { ClientPrefs.flip(ClientPrefs.COLORBLIND); b.setMessage(flag("gui.arcane.options.colorblind", ClientPrefs.colorblind())); })
        .bounds(left, y + 48, w, 20).build());
    addRenderableWidget(Button.builder(flag("gui.arcane.options.flashes", ClientPrefs.reduceFlashes()), b -> { ClientPrefs.flip(ClientPrefs.REDUCE_FLASHES); b.setMessage(flag("gui.arcane.options.flashes", ClientPrefs.reduceFlashes())); })
        .bounds(left, y + 72, w, 20).tooltip(Tooltip.create(Component.translatable("gui.arcane.options.flashes.tip"))).build());

    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.close"), b -> onClose())
        .bounds(width / 2 - 50, y + 104, 100, 20).build());
  }

  private static Component modeLabel() {
    return Component.translatable("gui.arcane.options.mode", Component.translatable("gui.arcane.options.mode." + ClientPrefs.mode().name().toLowerCase(java.util.Locale.ROOT)));
  }

  private static Component flag(String key, boolean on) {
    return Component.translatable(key, Component.translatable(on ? "gui.arcane.on" : "gui.arcane.off"));
  }

  @Override
  public void onClose() {
    minecraft.setScreen(parent);
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float pt) {
    renderBackground(g);
    g.drawCenteredString(font, title, width / 2, height / 2 - 98, 0xFFFFFF);
    g.drawString(font, Component.translatable("gui.arcane.options.player"), width / 2 - 100, height / 2 - 84, 0xB79CFF, false);
    super.render(g, mx, my, pt);
    g.drawCenteredString(font, Component.translatable("gui.arcane.options.world.hint"), width / 2, height / 2 + 96, 0xA0A0A0);
    g.drawCenteredString(font, Component.translatable("gui.arcane.options.keys"), width / 2, height / 2 + 112, 0xA0A0A0);
  }
}
