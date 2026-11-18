package com.arcane.client.screen;

import com.arcane.client.ArcaneTheme;
import com.arcane.client.ClientArcaneData;
import com.arcane.client.ClientArcaneData.KnownGlyph;
import com.arcane.client.ClientArcaneData.ParamDef;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.C2SRitualPacket;
import com.arcane.network.packet.C2SSaveInscriptionPacket;
import com.arcane.ritual.RitualService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tela do Altar Arcano: o ritual e desenhado AQUI (nao e a magia ativa do grimorio nem a bancada). Escolha glifos na
 * paleta, ajuste os numeros (quantidade, tamanho...), veja o circulo medido em volta do altar, as oferendas que o ritual
 * pede (item, quanto precisa e quanto voce tem) e a mana. Rituais nao precisam de forma: o circulo e a area.
 * Realizar conjura agora; Guardar deixa a magia no altar para o glifo Ativar disparar. O servidor revalida tudo.
 */
public class RitualAltarScreen extends Screen {
  private static final String[] TABS = {"ritual", "form", "effect", "filter", "modifier", "condition", "activator"};
  private static final int W = 316, H = 222, COLS = 10, ROWS = 3, CELL = 24;
  private static RitualService.Report lastReport;

  private final BlockPos altar;
  private int left, top;
  private int tab, page, selected = -1;
  private boolean legendary;
  private int dirty = 2;
  private RitualService.Report report;
  private final List<Slot> composition = new ArrayList<>();
  private Button performButton, storeButton;

  private static final class Slot {
    final ResourceLocation id;
    final Map<String, Double> params = new HashMap<>();
    Slot(ResourceLocation id) { this.id = id; }
  }

  public RitualAltarScreen(BlockPos altar) {
    super(Component.translatable("gui.arcane.ritual.title"));
    this.altar = altar;
  }

  /** Chamado pelo pacote de relatorio: atualiza a tela aberta. */
  public static void onReport(RitualService.Report r) {
    lastReport = r;
    if (net.minecraft.client.Minecraft.getInstance().screen instanceof RitualAltarScreen s) s.report = r;
  }

  @Override
  protected void init() {
    left = (width - W) / 2;
    top = (height - H) / 2;
    report = lastReport;
    performButton = addRenderableWidget(Button.builder(Component.translatable("gui.arcane.ritual.perform"), b -> send(C2SRitualPacket.PERFORM))
        .bounds(left + W - 214, top + H - 26, 66, 20).build());
    storeButton = addRenderableWidget(Button.builder(Component.translatable("gui.arcane.ritual.store"), b -> send(C2SRitualPacket.STORE))
        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.arcane.ritual.store.tip")))
        .bounds(left + W - 144, top + H - 26, 66, 20).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.close"), b -> onClose())
        .bounds(left + W - 74, top + H - 26, 64, 20).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.ritual.clear"), b -> { composition.clear(); selected = -1; dirty = 1; })
        .bounds(left + 10, top + H - 26, 50, 20).build());
    dirty = 1;
  }

  @Override
  public boolean isPauseScreen() { return false; }

  // ------------------------------------------------------------------ dados
  private List<KnownGlyph> palette() {
    List<KnownGlyph> out = new ArrayList<>();
    for (KnownGlyph g : ClientArcaneData.glyphs()) if (g.usable() && g.category().equals(TABS[tab])) out.add(g);
    return out;
  }

  private List<C2SSaveInscriptionPacket.Entry> entries() {
    List<C2SSaveInscriptionPacket.Entry> out = new ArrayList<>();
    for (Slot s : composition) out.add(new C2SSaveInscriptionPacket.Entry(s.id.toString(), Map.copyOf(s.params)));
    return out;
  }

  private void send(int action) {
    ModNetwork.CHANNEL.sendToServer(new C2SRitualPacket(altar, action, legendary, entries()));
  }

  private KnownGlyph known(ResourceLocation id) {
    for (KnownGlyph g : ClientArcaneData.glyphs()) if (g.id().equals(id)) return g;
    return null;
  }

  @Override
  public void tick() {
    if (dirty > 0 && --dirty == 0) send(C2SRitualPacket.PREVIEW);
    boolean ok = report != null && report.valid() && !composition.isEmpty();
    performButton.active = ok;
    storeButton.active = ok;
  }

  // ------------------------------------------------------------------ entrada
  private int paletteX() { return left + 10; }
  private int paletteY() { return top + 44; }
  private int slotsY() { return top + 44 + ROWS * CELL + 18; }
  private int paramsY() { return slotsY() + 30; }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (super.mouseClicked(mx, my, button)) return true;
    // abas
    for (int i = 0; i < TABS.length; i++) {
      int x = left + 10 + i * 43;
      if (mx >= x && mx < x + 42 && my >= top + 22 && my < top + 38) { tab = i; page = 0; return true; }
    }
    // paleta
    List<KnownGlyph> list = palette();
    int per = COLS * ROWS;
    for (int i = 0; i < per; i++) {
      int index = page * per + i;
      if (index >= list.size()) break;
      int x = paletteX() + (i % COLS) * CELL, y = paletteY() + (i / COLS) * CELL;
      if (mx >= x && mx < x + CELL - 2 && my >= y && my < y + CELL - 2) {
        if (composition.size() < ClientArcaneData.maxGlyphs()) { composition.add(new Slot(list.get(index).id())); selected = composition.size() - 1; dirty = 4; }
        return true;
      }
    }
    // paginas
    if (my >= top + 44 + ROWS * CELL && my < top + 44 + ROWS * CELL + 12) {
      if (mx >= left + W - 48 && mx < left + W - 28 && page > 0) { page--; return true; }
      if (mx >= left + W - 26 && mx < left + W - 10 && (page + 1) * per < list.size()) { page++; return true; }
    }
    // composicao: clique seleciona; clique direito (ou Shift) remove
    for (int i = 0; i < composition.size(); i++) {
      int x = left + 10 + i * CELL, y = slotsY();
      if (mx >= x && mx < x + CELL - 2 && my >= y && my < y + CELL - 2) {
        if (button == 1 || hasShiftDown()) { composition.remove(i); selected = Math.min(selected, composition.size() - 1); dirty = 4; }
        else selected = i;
        return true;
      }
    }
    // parametros do glifo selecionado: [-] valor [+]
    if (selected >= 0 && selected < composition.size()) {
      KnownGlyph k = known(composition.get(selected).id);
      if (k != null) {
        for (int i = 0; i < Math.min(3, k.params().size()); i++) {
          int y = paramsY() + i * 13;
          if (my >= y && my < y + 12) {
            if (mx >= left + 132 && mx < left + 144) { bump(k.params().get(i), -1); return true; }
            if (mx >= left + 190 && mx < left + 202) { bump(k.params().get(i), 1); return true; }
          }
        }
      }
    }
    // oferenda lendaria (Estrela do Nether)
    if (mx >= left + W - 150 && mx < left + W - 10 && my >= top + 60 + 15 * 7 && my < top + 60 + 15 * 7 + 12) {
      legendary = !legendary;
      dirty = 1;
      return true;
    }
    return false;
  }

  private void bump(ParamDef p, int dir) {
    Slot s = composition.get(selected);
    double now = s.params.getOrDefault(p.key(), p.def());
    s.params.put(p.key(), p.clamp(now + dir * p.step()));
    dirty = 4;
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double delta) {
    int per = COLS * ROWS;
    if (delta < 0 && (page + 1) * per < palette().size()) page++;
    else if (delta > 0 && page > 0) page--;
    return true;
  }

  // ------------------------------------------------------------------ desenho
  private static ResourceLocation iconFor(ResourceLocation glyph) {
    return new ResourceLocation(glyph.getNamespace(), "textures/gui/glyphs/" + glyph.getPath() + ".png");
  }

  private static String num(double v) { return v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.1f", v); }

  @Override
  public void render(GuiGraphics g, int mx, int my, float pt) {
    renderBackground(g);
    ArcaneTheme.panel(g, left, top, W, H, ArcaneTheme.EDGE, 0.5f);
    g.drawCenteredString(font, title, left + W / 2, top + 7, ArcaneTheme.GOLD);

    // abas de categoria
    for (int i = 0; i < TABS.length; i++) {
      int x = left + 10 + i * 43;
      boolean on = i == tab;
      g.fill(x, top + 22, x + 42, top + 38, on ? ArcaneTheme.alpha(ArcaneTheme.EDGE_LIT, 0.35f) : 0x60000000);
      g.renderOutline(x, top + 22, 42, 16, on ? ArcaneTheme.EDGE_LIT : ArcaneTheme.EDGE);
      String name = font.plainSubstrByWidth(Component.translatable("category.arcane." + TABS[i]).getString(), 40);
      g.drawString(font, name, x + 21 - font.width(name) / 2, top + 26, on ? ArcaneTheme.TEXT : ArcaneTheme.TEXT_DIM, false);
    }

    // paleta
    List<KnownGlyph> list = palette();
    int per = COLS * ROWS;
    KnownGlyph hover = null;
    for (int i = 0; i < per; i++) {
      int index = page * per + i;
      if (index >= list.size()) break;
      int x = paletteX() + (i % COLS) * CELL, y = paletteY() + (i / COLS) * CELL;
      boolean over = mx >= x && mx < x + CELL - 2 && my >= y && my < y + CELL - 2;
      g.fill(x, y, x + CELL - 2, y + CELL - 2, over ? 0xA03A2C70 : 0x70000000);
      g.renderOutline(x, y, CELL - 2, CELL - 2, ArcaneTheme.alpha(ArcaneTheme.EDGE, over ? 1f : 0.6f));
      g.blit(iconFor(list.get(index).id()), x + 3, y + 3, 0, 0, 16, 16, 16, 16);
      if (over) hover = list.get(index);
    }
    if (list.isEmpty()) g.drawString(font, Component.translatable("gui.arcane.ritual.none_in_tab"), left + 12, top + 70, ArcaneTheme.TEXT_DIM, false);
    int pages = Math.max(1, (list.size() + per - 1) / per);
    g.drawString(font, "<  " + (page + 1) + "/" + pages + "  >", left + W - 48, top + 44 + ROWS * CELL + 2, ArcaneTheme.TEXT_DIM, false);

    // composicao
    g.drawString(font, Component.translatable("gui.arcane.ritual.composition", composition.size(), ClientArcaneData.maxGlyphs()), left + 10, slotsY() - 11, ArcaneTheme.VIOLET, false);
    for (int i = 0; i < composition.size(); i++) {
      int x = left + 10 + i * CELL, y = slotsY();
      boolean on = i == selected;
      g.fill(x, y, x + CELL - 2, y + CELL - 2, on ? 0xA0504090 : 0x70000000);
      g.renderOutline(x, y, CELL - 2, CELL - 2, on ? ArcaneTheme.GOLD : ArcaneTheme.EDGE);
      g.blit(iconFor(composition.get(i).id), x + 3, y + 3, 0, 0, 16, 16, 16, 16);
      if (mx >= x && mx < x + CELL - 2 && my >= y && my < y + CELL - 2) hover = known(composition.get(i).id);
    }
    if (composition.isEmpty()) g.drawString(font, Component.translatable("gui.arcane.ritual.empty"), left + 12, slotsY() + 8, ArcaneTheme.TEXT_DIM, false);

    // parametros do selecionado
    if (selected >= 0 && selected < composition.size()) {
      Slot s = composition.get(selected);
      KnownGlyph k = known(s.id);
      g.drawString(font, ArcaneWorkbenchScreen.glyphName(s.id), left + 10, paramsY() - 10, ArcaneTheme.TEXT, false);
      if (k != null) {
        for (int i = 0; i < Math.min(3, k.params().size()); i++) {
          ParamDef p = k.params().get(i);
          int y = paramsY() + i * 13;
          g.drawString(font, p.key(), left + 10, y + 2, ArcaneTheme.TEXT_DIM, false);
          g.drawString(font, "[-]", left + 132, y + 2, ArcaneTheme.EDGE_LIT, false);
          g.drawString(font, num(s.params.getOrDefault(p.key(), p.def())), left + 150, y + 2, ArcaneTheme.TEXT, false);
          g.drawString(font, "[+]", left + 190, y + 2, ArcaneTheme.EDGE_LIT, false);
        }
      }
    }

    renderReport(g);
    super.render(g, mx, my, pt);
    if (hover != null) {
      List<Component> tip = new ArrayList<>();
      tip.add(ArcaneWorkbenchScreen.glyphName(hover.id()).copy().withStyle(net.minecraft.ChatFormatting.WHITE));
      String desc = "glyph." + hover.id().getNamespace() + "." + hover.id().getPath() + ".desc";
      if (net.minecraft.client.resources.language.I18n.exists(desc)) {
        tip.addAll(font.getSplitter().splitLines(Component.translatable(desc), 220, net.minecraft.network.chat.Style.EMPTY).stream()
            .map(fs -> (Component) Component.literal(fs.getString()).withStyle(net.minecraft.ChatFormatting.GRAY)).toList());
      }
      g.renderComponentTooltip(font, tip, mx, my);
    }
  }

  private void renderReport(GuiGraphics g) {
    int x = left + W - 150, y = top + 60;
    g.fill(x - 6, y - 8, left + W - 6, top + H - 34, 0x40000000);
    g.drawString(font, Component.translatable("gui.arcane.ritual.report"), x, y - 5, ArcaneTheme.VIOLET, false);
    if (report == null) {
      g.drawString(font, "...", x, y + 8, ArcaneTheme.TEXT_DIM, false);
      return;
    }
    y += 6;
    // circulo
    Component circle = report.circleLevel() > 0
        ? Component.translatable("gui.arcane.ritual.circle", Component.translatable("ritual.arcane.level." + report.circleLevel()), report.completenessPct())
        : Component.translatable("gui.arcane.ritual.circle_none", report.completenessPct());
    g.drawString(font, circle, x, y, report.circleLevel() > 0 ? 0xFF9BE08A : ArcaneTheme.DANGER, false);
    y += 11;
    if (report.needLevel() > 0) {
      boolean enough = report.circleLevel() >= report.needLevel() || report.legendary();
      g.drawString(font, Component.translatable("gui.arcane.ritual.needs_level", Component.translatable("ritual.arcane.level." + report.needLevel())), x, y,
          enough ? 0xFF9BE08A : ArcaneTheme.DANGER, false);
      y += 11;
    }
    // mana
    boolean manaOk = report.mana() >= report.cost();
    g.drawString(font, Component.translatable("gui.arcane.ritual.mana", report.cost(), report.mana()), x, y, manaOk ? ArcaneTheme.EDGE_LIT : ArcaneTheme.DANGER, false);
    y += 12;
    // oferendas
    g.drawString(font, Component.translatable("gui.arcane.ritual.offerings"), x, y, ArcaneTheme.GOLD, false);
    y += 10;
    if (report.needs().isEmpty()) {
      g.drawString(font, Component.translatable("gui.arcane.ritual.no_offerings"), x, y, ArcaneTheme.TEXT_DIM, false);
      y += 11;
    }
    for (RitualService.Need n : report.needs()) {
      var item = ForgeRegistries.ITEMS.getValue(n.item());
      if (item != null) g.renderItem(new ItemStack(item), x, y - 4, 0);
      boolean ok = n.have() >= n.need();
      String have = n.have() >= 9999 ? "∞" : String.valueOf(n.have());
      g.drawString(font, have + "/" + n.need(), x + 20, y, ok ? 0xFF9BE08A : ArcaneTheme.DANGER, false);
      y += 15;
    }
    // Estrela do Nether: oferenda opcional que abre o ritual lendario
    int sy = top + 60 + 15 * 7;
    g.renderItem(new ItemStack(Items.NETHER_STAR), x, sy - 4, 0);
    g.drawString(font, Component.translatable(legendary ? "gui.arcane.ritual.legendary_on" : "gui.arcane.ritual.legendary_off"), x + 20, sy, legendary ? ArcaneTheme.GOLD : ArcaneTheme.TEXT_DIM, false);
    if (!report.valid() && !composition.isEmpty()) {
      g.drawString(font, font.plainSubstrByWidth(report.message().getString(), 140), x, top + H - 44, ArcaneTheme.DANGER, false);
    }
  }
}
