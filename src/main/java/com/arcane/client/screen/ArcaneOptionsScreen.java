package com.arcane.client.screen;

import com.arcane.client.ClientPrefs;
import com.arcane.config.ArcaneConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Opcoes do Arcane (tambem abre pelo botao "Config" da aba Mods). Coluna da esquerda: preferencias do jogador (modo de uso,
 * acessibilidade; salvas em arcane-client.toml). Coluna da direita: regras do mundo (config do servidor), editaveis
 * quando voce esta num mundo local (um jogo solo, ou um mundo aberto para a rede); em servidor dedicado
 * mexa em serverconfig/arcane-server.toml.
 */
public class ArcaneOptionsScreen extends Screen {
  private static final double[] AREA_CAPS = {1, 2, 4, 8, 16, 32};
  private static final double[] CHARGE_CAPS = {3, 6, 10, 20};
  private final Screen parent;

  public ArcaneOptionsScreen(Screen parent) {
    super(Component.translatable("gui.arcane.options"));
    this.parent = parent;
  }

  /** A config do servidor so existe (e so pode ser editada daqui) dentro de um mundo local. */
  private static boolean worldRulesAvailable() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || !mc.hasSingleplayerServer()) return false;
    try {
      ArcaneConfig.WORLD_ALTERING_SPELLS.get();
      return true;
    } catch (IllegalStateException notLoaded) {
      return false;
    }
  }

  @Override
  protected void init() {
    int left = width / 2 - 190, right = width / 2 + 10, w = 180, y = height / 2 - 70;
    addRenderableWidget(Button.builder(modeLabel(), b -> { ClientPrefs.cycleMode(); b.setMessage(modeLabel()); })
        .bounds(left, y, w, 20).tooltip(Tooltip.create(Component.translatable("gui.arcane.options.mode.tip"))).build());
    addRenderableWidget(Button.builder(flag("gui.arcane.options.latch", ClientPrefs.latch()), b -> { ClientPrefs.flip(ClientPrefs.LATCH_KEYS); b.setMessage(flag("gui.arcane.options.latch", ClientPrefs.latch())); })
        .bounds(left, y + 24, w, 20).tooltip(Tooltip.create(Component.translatable("gui.arcane.options.latch.tip"))).build());
    addRenderableWidget(Button.builder(flag("gui.arcane.options.colorblind", ClientPrefs.colorblind()), b -> { ClientPrefs.flip(ClientPrefs.COLORBLIND); b.setMessage(flag("gui.arcane.options.colorblind", ClientPrefs.colorblind())); })
        .bounds(left, y + 48, w, 20).build());
    addRenderableWidget(Button.builder(flag("gui.arcane.options.flashes", ClientPrefs.reduceFlashes()), b -> { ClientPrefs.flip(ClientPrefs.REDUCE_FLASHES); b.setMessage(flag("gui.arcane.options.flashes", ClientPrefs.reduceFlashes())); })
        .bounds(left, y + 72, w, 20).tooltip(Tooltip.create(Component.translatable("gui.arcane.options.flashes.tip"))).build());

    if (worldRulesAvailable()) {
      rule(right, y, "gui.arcane.rule.world_altering", ArcaneConfig.WORLD_ALTERING_SPELLS);
      rule(right, y + 24, "gui.arcane.rule.block_breaking", ArcaneConfig.BLOCK_BREAKING_SPELLS);
      rule(right, y + 48, "gui.arcane.rule.interactions", ArcaneConfig.ELEMENT_INTERACTIONS);
      rule(right, y + 72, "gui.arcane.rule.scars", ArcaneConfig.SCARS_ENABLED);
      cycle(right, y + 96, "gui.arcane.rule.crystal_cap", ArcaneConfig.CRYSTAL_AREA_CAP, AREA_CAPS);
      cycle(right, y + 120, "gui.arcane.rule.charge_cap", ArcaneConfig.CHARGE_MAX_SCALE, CHARGE_CAPS);
    }
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.close"), b -> onClose())
        .bounds(width / 2 - 50, y + 150, 100, 20).build());
  }

  private void rule(int x, int y, String key, ForgeConfigSpec.BooleanValue value) {
    addRenderableWidget(Button.builder(flag(key, value.get()), b -> { value.set(!value.get()); b.setMessage(flag(key, value.get())); })
        .bounds(x, y, 180, 20).tooltip(Tooltip.create(Component.translatable(key + ".tip"))).build());
  }

  private void cycle(int x, int y, String key, ForgeConfigSpec.DoubleValue value, double[] steps) {
    addRenderableWidget(Button.builder(number(key, value.get()), b -> {
      double now = value.get();
      double next = steps[0];
      for (int i = 0; i < steps.length; i++) if (steps[i] > now + 1.0e-9) { next = steps[i]; break; }
      value.set(next);
      b.setMessage(number(key, value.get()));
    }).bounds(x, y, 180, 20).tooltip(Tooltip.create(Component.translatable(key + ".tip"))).build());
  }

  private static Component number(String key, double v) {
    return Component.translatable(key, v == Math.rint(v) ? String.valueOf((int) v) : String.valueOf(v));
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
    g.drawString(font, Component.translatable("gui.arcane.options.player"), width / 2 - 190, height / 2 - 84, 0xB79CFF, false);
    g.drawString(font, Component.translatable(worldRulesAvailable() ? "gui.arcane.options.world" : "gui.arcane.options.world.none"),
        width / 2 + 10, height / 2 - 84, 0xB79CFF, false);
    super.render(g, mx, my, pt);
    g.drawCenteredString(font, Component.translatable("gui.arcane.options.keys"), width / 2, height / 2 + 108, 0xA0A0A0);
  }
}
