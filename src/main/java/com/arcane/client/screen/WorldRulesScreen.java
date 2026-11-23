package com.arcane.client.screen;

import com.arcane.network.ModNetwork;
import com.arcane.network.packet.C2SWorldRulePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Regras do mundo (config do servidor). Abre com /arcane rules, so para administradores: o servidor revalida a cada
 * clique, aplica e salva na hora em serverconfig/arcane-server.toml (nao ha botao "Salvar"). Todo teto tem "sem limite".
 */
public class WorldRulesScreen extends Screen {
  private static final String[] FLAGS = {"world_altering", "block_breaking", "interactions", "scars", "hard_caps"};
  private static final String[] NUMBERS = {"crystal_cap", "charge_cap", "ritual_cap"};
  private static final String UNIQUE = "unique_break";

  private Map<String, Double> values;
  private boolean canEdit;
  private final java.util.Map<String, Button> buttons = new java.util.HashMap<>();

  private WorldRulesScreen(Map<String, Double> values, boolean canEdit) {
    super(Component.translatable("gui.arcane.options.world"));
    this.values = values;
    this.canEdit = canEdit;
  }

  /** Chamado pelo pacote do servidor: abre a tela ou atualiza a que ja esta aberta. */
  public static void show(Map<String, Double> values, boolean canEdit) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.screen instanceof WorldRulesScreen s) s.update(values, canEdit);
    else mc.setScreen(new WorldRulesScreen(values, canEdit));
  }

  private void update(Map<String, Double> values, boolean canEdit) {
    this.values = values;
    this.canEdit = canEdit;
    buttons.forEach((k, b) -> { b.setMessage(label(k)); b.active = canEdit; });
  }

  @Override
  protected void init() {
    buttons.clear();
    int left = width / 2 - 190, right = width / 2 + 10, y = height / 2 - 70;
    for (int i = 0; i < FLAGS.length; i++) add(left, y + i * 24, FLAGS[i]);
    for (int i = 0; i < NUMBERS.length; i++) add(right, y + i * 24, NUMBERS[i]);
    add(right, y + NUMBERS.length * 24, UNIQUE);
    add(left, y + FLAGS.length * 24, "hydra_devastation");
    add(width / 2 - 90, y - 28, "profile");
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.close"), b -> onClose())
        .bounds(width / 2 - 50, y + 150, 100, 20).build());
  }

  private void add(int x, int y, String key) {
    Button b = addRenderableWidget(Button.builder(label(key), btn -> ModNetwork.CHANNEL.sendToServer(new C2SWorldRulePacket(key)))
        .bounds(x, y, 180, 20).tooltip(Tooltip.create(Component.translatable("gui.arcane.rule." + key + ".tip"))).build());
    b.active = canEdit;
    buttons.put(key, b);
  }

  private Component label(String key) {
    double v = values.getOrDefault(key, 0.0);
    String name = "gui.arcane.rule." + key;
    if (key.equals("profile")) return Component.translatable(name, Component.translatable("gui.arcane.rule.profile." + (int) v));
    if (key.equals("hydra_devastation")) return Component.translatable(name, Component.translatable(v > 0.5 ? "gui.arcane.on" : "gui.arcane.off"));
    for (String f : FLAGS) if (f.equals(key)) return Component.translatable(name, Component.translatable(v > 0.5 ? "gui.arcane.on" : "gui.arcane.off"));
    if (key.equals(UNIQUE)) return Component.translatable(name, Component.translatable(v > 0.5 ? "gui.arcane.on" : "gui.arcane.off"));
    if (v <= 0) return Component.translatable(name, Component.translatable("gui.arcane.rule.unlimited"));
    String num = v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    return Component.translatable(name, key.equals("ritual_cap") ? num : "x" + num);
  }

  @Override
  public void onClose() {
    minecraft.setScreen(null);
  }

  @Override
  public boolean isPauseScreen() { return false; }

  @Override
  public void render(GuiGraphics g, int mx, int my, float pt) {
    renderBackground(g);
    g.drawCenteredString(font, title, width / 2, height / 2 - 98, 0xFFFFFF);
    g.drawString(font, Component.translatable("gui.arcane.options.world.general"), width / 2 - 190, height / 2 - 84, 0xB79CFF, false);
    g.drawString(font, Component.translatable("gui.arcane.options.world.limits"), width / 2 + 10, height / 2 - 84, 0xB79CFF, false);
    super.render(g, mx, my, pt);
    g.drawCenteredString(font, Component.translatable(canEdit ? "gui.arcane.options.world.autosave" : "gui.arcane.options.world.readonly"),
        width / 2, height / 2 + 84, canEdit ? 0xA0A0A0 : 0xFF8080);
  }
}
