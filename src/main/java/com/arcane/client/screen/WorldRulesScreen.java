package com.arcane.client.screen;

import com.arcane.config.ArcaneConfig;
import com.arcane.ritual.RitualSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Regras do mundo (config do servidor), abertas pela tela de configuracoes do mod. Editaveis num mundo local;
 * em servidor dedicado mexa em serverconfig/arcane-server.toml. Todo teto aqui tem a opcao "sem limite".
 * Os numeros do ritual e dos unicos valem na hora (alem de salvar no arquivo).
 */
public class WorldRulesScreen extends Screen {
  /** 0 = sem limite (sempre por ultimo). */
  private static final double[] AREA_CAPS = {1, 2, 4, 8, 16, 32, 0};
  private static final double[] CHARGE_CAPS = {3, 6, 10, 20, 50, 0};
  private static final double[] RITUAL_CAPS = {500, 1000, 1500, 3000, 10000, 0};
  private final Screen parent;

  public WorldRulesScreen(Screen parent) {
    super(Component.translatable("gui.arcane.options.world"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int left = width / 2 - 190, right = width / 2 + 10, w = 180, y = height / 2 - 70;
    flag(left, y, "gui.arcane.rule.world_altering", ArcaneConfig.WORLD_ALTERING_SPELLS, () -> {});
    flag(left, y + 24, "gui.arcane.rule.block_breaking", ArcaneConfig.BLOCK_BREAKING_SPELLS, () -> {});
    flag(left, y + 48, "gui.arcane.rule.interactions", ArcaneConfig.ELEMENT_INTERACTIONS, () -> {});
    flag(left, y + 72, "gui.arcane.rule.scars", ArcaneConfig.SCARS_ENABLED, () -> {});
    flag(left, y + 96, "gui.arcane.rule.hard_caps", ArcaneConfig.AREA_HARD_CAPS, () -> {});
    cycle(right, y, "gui.arcane.rule.crystal_cap", "x", ArcaneConfig.CRYSTAL_AREA_CAP, AREA_CAPS, () -> {});
    cycle(right, y + 24, "gui.arcane.rule.charge_cap", "x", ArcaneConfig.CHARGE_MAX_SCALE, CHARGE_CAPS, () -> {});
    cycle(right, y + 48, "gui.arcane.rule.ritual_cap", "", RITUAL_CAPS, () -> {});
    flag(right, y + 72, "gui.arcane.rule.unique_break", ArcaneConfig.UNIQUE_OFFERINGS_BREAK_CAP, WorldRulesScreen::applyRitual);
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.close"), b -> onClose())
        .bounds(width / 2 - 50, y + 130, 100, 20).build());
  }

  private static void applyRitual() {
    RitualSettings.setMaxPercent(ArcaneConfig.RITUAL_MAX_PERCENT.get());
    RitualSettings.setUnique(ArcaneConfig.UNIQUE_OFFERINGS_BREAK_CAP.get(), ArcaneConfig.UNIQUE_OFFERINGS.get());
  }

  private void flag(int x, int y, String key, ForgeConfigSpec.BooleanValue value, Runnable after) {
    addRenderableWidget(Button.builder(flagLabel(key, value.get()), b -> { value.set(!value.get()); after.run(); b.setMessage(flagLabel(key, value.get())); })
        .bounds(x, y, 180, 20).tooltip(Tooltip.create(Component.translatable(key + ".tip"))).build());
  }

  private void cycle(int x, int y, String key, String prefix, ForgeConfigSpec.DoubleValue value, double[] steps, Runnable after) {
    addRenderableWidget(Button.builder(numberLabel(key, prefix, value.get()), b -> {
      value.set(next(value.get(), steps));
      after.run();
      b.setMessage(numberLabel(key, prefix, value.get()));
    }).bounds(x, y, 180, 20).tooltip(Tooltip.create(Component.translatable(key + ".tip"))).build());
  }

  /** O teto do ritual e inteiro: mesmo botao, outro tipo de valor. */
  private void cycle(int x, int y, String key, String prefix, double[] steps, Runnable after) {
    var value = ArcaneConfig.RITUAL_MAX_PERCENT;
    addRenderableWidget(Button.builder(numberLabel(key, prefix, value.get()), b -> {
      value.set((int) next(value.get(), steps));
      applyRitual();
      after.run();
      b.setMessage(numberLabel(key, prefix, value.get()));
    }).bounds(x, y, 180, 20).tooltip(Tooltip.create(Component.translatable(key + ".tip"))).build());
  }

  /** Proximo valor da lista; 0 (sem limite) e o ultimo e depois dele volta ao primeiro. */
  static double next(double now, double[] steps) {
    if (now <= 0) return steps[0];
    for (double s : steps) if (s > now + 1.0e-9) return s;
    return steps[steps.length - 1] == 0 ? 0 : steps[0];
  }

  private static Component numberLabel(String key, String prefix, double v) {
    if (v <= 0) return Component.translatable(key, Component.translatable("gui.arcane.rule.unlimited"));
    return Component.translatable(key, prefix + (v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v)));
  }

  private static Component flagLabel(String key, boolean on) {
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
    g.drawString(font, Component.translatable("gui.arcane.options.world.general"), width / 2 - 190, height / 2 - 84, 0xB79CFF, false);
    g.drawString(font, Component.translatable("gui.arcane.options.world.limits"), width / 2 + 10, height / 2 - 84, 0xB79CFF, false);
    super.render(g, mx, my, pt);
  }
}
