package com.arcane.client.screen;

import com.arcane.client.ClientCasting;
import com.arcane.client.ClientPrefs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Opcoes do grimorio: modo de uso, conjunto ativo e acessibilidade. Salvas em config/arcane-client.toml. */
public class ArcaneOptionsScreen extends Screen {
  private final Screen parent;

  public ArcaneOptionsScreen(Screen parent) {
    super(Component.translatable("gui.arcane.options"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int x = width / 2 - 110, y = height / 2 - 62;
    addRenderableWidget(Button.builder(modeLabel(), b -> { ClientPrefs.cycleMode(); b.setMessage(modeLabel()); })
        .bounds(x, y, 220, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.arcane.options.mode.tip"))).build());
    addRenderableWidget(Button.builder(loadoutLabel(), b -> { ClientCasting.switchLoadout(1); b.setMessage(Component.translatable("gui.arcane.options.set", ((com.arcane.client.ClientArcaneData.activeLoadout() + 1) % com.arcane.grimoire.SpellPrefs.LOADOUTS) + 1)); })
        .bounds(x, y + 24, 220, 20).build());
    addRenderableWidget(Button.builder(flag("gui.arcane.options.latch", ClientPrefs.latch()), b -> { ClientPrefs.flip(ClientPrefs.LATCH_KEYS); b.setMessage(flag("gui.arcane.options.latch", ClientPrefs.latch())); })
        .bounds(x, y + 52, 220, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.arcane.options.latch.tip"))).build());
    addRenderableWidget(Button.builder(flag("gui.arcane.options.colorblind", ClientPrefs.colorblind()), b -> { ClientPrefs.flip(ClientPrefs.COLORBLIND); b.setMessage(flag("gui.arcane.options.colorblind", ClientPrefs.colorblind())); })
        .bounds(x, y + 76, 220, 20).build());
    addRenderableWidget(Button.builder(flag("gui.arcane.options.flashes", ClientPrefs.reduceFlashes()), b -> { ClientPrefs.flip(ClientPrefs.REDUCE_FLASHES); b.setMessage(flag("gui.arcane.options.flashes", ClientPrefs.reduceFlashes())); })
        .bounds(x, y + 100, 220, 20).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.close"), b -> onClose()).bounds(x + 60, y + 130, 100, 20).build());
  }

  private static Component modeLabel() {
    return Component.translatable("gui.arcane.options.mode", Component.translatable("gui.arcane.options.mode." + ClientPrefs.mode().name().toLowerCase(java.util.Locale.ROOT)));
  }

  private static Component loadoutLabel() {
    return Component.translatable("gui.arcane.options.set", com.arcane.client.ClientArcaneData.activeLoadout() + 1);
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
    g.drawCenteredString(font, title, width / 2, height / 2 - 82, 0xFFFFFF);
    super.render(g, mx, my, pt);
    g.drawCenteredString(font, Component.translatable("gui.arcane.options.keys"), width / 2, height / 2 + 96, 0xA0A0A0);
  }
}
