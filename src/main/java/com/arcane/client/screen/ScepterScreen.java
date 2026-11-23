package com.arcane.client.screen;

import com.arcane.client.ArcaneTheme;
import com.arcane.inscription.Inscription;
import com.arcane.item.ScepterItem;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.C2SScepterPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Tela do cetro (agachar + usar): a esquerda o cajado girando em tamanho grande; a direita as 4 partes da aparencia
 * (ponta, haste, cor da gema, metal) com setas, e embaixo os 6 espacos de magia (clique escolhe, x apaga) e o botao
 * de gravar a magia ativa do grimorio. Cada mudanca vai ao servidor; a previa muda na hora.
 */
public class ScepterScreen extends Screen {
  private static final int W = 330, H = 214;
  private static final String[] PARTS = {"head", "shaft", "gem", "metal"};
  private int lastSignature = Integer.MIN_VALUE;

  public ScepterScreen() {
    super(Component.translatable("gui.arcane.scepter"));
  }

  @Override
  public boolean isPauseScreen() { return false; }

  private ItemStack scepter() {
    var p = minecraft == null ? null : minecraft.player;
    if (p == null || !(p.getMainHandItem().getItem() instanceof ScepterItem)) return ItemStack.EMPTY;
    return p.getMainHandItem();
  }

  private int left() { return (width - W) / 2; }

  private int top() { return (height - H) / 2; }

  /** Muda o que importa para os botoes de magia: quantidade, escolhida e nomes. */
  private int signature(ItemStack s) {
    if (s.isEmpty()) return 0;
    int h = ScepterItem.selected(s) * 31;
    for (Inscription i : ScepterItem.spells(s)) h = h * 17 + i.id().hashCode();
    return h;
  }

  @Override
  protected void init() {
    ItemStack s = scepter();
    lastSignature = signature(s);
    int x0 = left() + 132, y0 = top() + 26;
    for (int part = 0; part < 4; part++) {
      final int pt = part;
      int y = y0 + part * 21;
      addRenderableWidget(Button.builder(Component.literal("<"), b -> change(pt, -1)).bounds(x0 + 54, y, 18, 18).build());
      addRenderableWidget(Button.builder(Component.literal(">"), b -> change(pt, 1)).bounds(x0 + 178, y, 18, 18).build());
    }
    List<Inscription> spells = s.isEmpty() ? List.of() : ScepterItem.spells(s);
    int sel = s.isEmpty() ? -1 : ScepterItem.selected(s);
    int sy = top() + 134;
    for (int i = 0; i < ScepterItem.SLOTS; i++) {
      final int idx = i;
      int col = i % 2, row = i / 2;
      int bx = left() + 10 + col * 160, by = sy + row * 22;
      if (i < spells.size()) {
        String n = spells.get(i).metadata().name();
        String label = (i == sel ? "▶ " : "") + (i + 1) + ". " + (n == null || n.isEmpty() ? Component.translatable("gui.arcane.unnamed").getString() : n);
        addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(label, 124)), b -> send(C2SScepterPacket.SELECT, idx, 0))
            .bounds(bx, by, 130, 20).build());
        addRenderableWidget(Button.builder(Component.literal("×"), b -> send(C2SScepterPacket.REMOVE, idx, 0)).bounds(bx + 132, by, 18, 20).build());
      } else {
        Button empty = Button.builder(Component.literal((i + 1) + ". —"), b -> { }).bounds(bx, by, 150, 20).build();
        empty.active = false;
        addRenderableWidget(empty);
      }
    }
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.scepter.store"), b -> send(C2SScepterPacket.STORE_ACTIVE, 0, 0))
        .bounds(left() + 132, top() + 110, 188, 20).build());
  }

  private void send(int action, int a, int b) {
    ModNetwork.CHANNEL.sendToServer(new C2SScepterPacket(action, a, b));
  }

  private void change(int part, int delta) {
    ItemStack s = scepter();
    if (s.isEmpty()) return;
    int v = Math.floorMod(ScepterItem.style(s, part) + delta, ScepterItem.count(part));
    ScepterItem.setStyle(s, part, v); // previa imediata; o servidor grava o mesmo valor
    send(C2SScepterPacket.STYLE, part, v);
  }

  @Override
  public void tick() {
    ItemStack s = scepter();
    if (s.isEmpty()) { onClose(); return; }
    if (signature(s) != lastSignature) rebuildWidgets();
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
    renderBackground(g);
    int x = left(), y = top();
    ArcaneTheme.panel(g, x, y, W, H, ArcaneTheme.EDGE, 0.4f);
    g.drawString(font, title, x + 10, y + 9, ArcaneTheme.GOLD, false);
    ItemStack s = scepter();
    if (!s.isEmpty()) {
      // previa: o cajado grande girando devagar
      ArcaneTheme.panel(g, x + 8, y + 24, 116, 88, ArcaneTheme.EDGE, 0f);
      var pose = g.pose();
      pose.pushPose();
      // previa grande: o item desenhado 5x (o modelo do cetro ja vem inclinado na pose de inventario)
      pose.translate(x + 66 - 40, y + 68 - 40, 0);
      pose.scale(5f, 5f, 5f);
      g.renderItem(s, 0, 0);
      pose.popPose();
      for (int part = 0; part < 4; part++) {
        int ry = y + 26 + part * 21;
        g.drawString(font, Component.translatable("gui.arcane.scepter.part." + PARTS[part]), x + 132, ry + 5, ArcaneTheme.TEXT_DIM, false);
        int v = ScepterItem.style(s, part);
        Component name = Component.translatable(ScepterItem.styleName(part, v));
        int cx = x + 132 + 54 + 18 + (178 - 54 - 18) / 2;
        if (part == ScepterItem.PART_GEM || part == ScepterItem.PART_METAL) {
          int c = part == ScepterItem.PART_GEM ? ScepterItem.GEM_COLORS[v] : ScepterItem.METAL_COLORS[v];
          g.fill(cx - font.width(name) / 2 - 12, ry + 4, cx - font.width(name) / 2 - 3, ry + 13, 0xFF000000 | c);
        }
        g.drawCenteredString(font, name, cx, ry + 5, ArcaneTheme.TEXT);
      }
      g.drawString(font, Component.translatable("gui.arcane.scepter.spells", (int) Math.round(ScepterItem.POWER * 100)), x + 10, y + 120, ArcaneTheme.VIOLET, false);
    }
    super.render(g, mouseX, mouseY, partial);
  }
}
