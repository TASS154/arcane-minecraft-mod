package com.arcane.client.screen;

import com.arcane.client.ArcaneTheme;
import com.arcane.client.ClientArcaneData;
import com.arcane.client.ClientArcaneData.KnownGlyph;
import com.arcane.client.ClientArcaneData.ParamDef;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.C2SRitualPacket;
import com.arcane.network.packet.C2SSaveInscriptionPacket;
import com.arcane.ritual.OfferingValue;
import com.arcane.ritual.RitualService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tela do Altar Arcano: o ritual e desenhado AQUI (nao e a magia ativa do grimorio nem a bancada).
 * Duas abas: RITUAL (escolher glifos e numeros) e OFERENDAS (ate 3 itens do inventario, qualquer um; cada item vale uma
 * porcentagem que soma ao valor do ritual pelo tamanho do circulo; passando de 300% so nas oferendas o ritual e
 * lendario e o valor total dobra). A coluna da direita e o relatorio do servidor (circulo, mana, componentes, valor).
 * Realizar conjura agora; Guardar deixa a magia no altar para o glifo Ativar disparar. O servidor revalida tudo.
 */
public class RitualAltarScreen extends Screen {
  private static final int W = 352, H = 262, COLS = 8, ROWS = 3, CELL = 24;
  private static final int LEFT_W = 214, RIGHT_X = 228, RIGHT_W = 114;
  private static final String[] CATS = {"ritual", "form", "effect", "filter", "modifier", "condition", "activator"};
  private static RitualService.Report lastReport;

  private final BlockPos altar;
  private int left, top;
  private boolean offeringsTab;
  private int cat, page, selected = -1;
  private int dirty = 2;
  private RitualService.Report report;
  private final List<Slot> composition;
  private final List<Integer> offerSlots;

  /** Rascunho por altar: fechar a tela nao desfaz a magia que estava sendo montada (vale ate sair do mundo). */
  private static final class Draft {
    final List<Slot> composition = new ArrayList<>();
    final List<Integer> offerSlots = new ArrayList<>();
    boolean offeringsTab;
    int cat, page, selected = -1;
  }
  private static final Map<BlockPos, Draft> DRAFTS = new HashMap<>();
  private final Draft draft;
  private Button performButton, storeButton, ritualTabButton, offerTabButton;

  private static final class Slot {
    final ResourceLocation id;
    final Map<String, Double> params = new HashMap<>();
    Slot(ResourceLocation id) { this.id = id; }
  }

  public RitualAltarScreen(BlockPos altar) {
    super(Component.translatable("gui.arcane.ritual.title"));
    this.altar = altar.immutable();
    this.draft = DRAFTS.computeIfAbsent(this.altar, k -> new Draft());
    this.composition = draft.composition;
    this.offerSlots = draft.offerSlots;
    this.offeringsTab = draft.offeringsTab;
    this.cat = draft.cat;
    this.page = draft.page;
    this.selected = draft.selected;
  }

  @Override
  public void removed() {
    draft.offeringsTab = offeringsTab;
    draft.cat = cat;
    draft.page = page;
    draft.selected = selected;
    super.removed();
  }

  /** Esquece os rascunhos (ao sair do mundo). */
  public static void clearDrafts() { DRAFTS.clear(); lastReport = null; }

  /** Chamado pelo pacote de relatorio: atualiza a tela aberta. */
  public static void onReport(RitualService.Report r) {
    lastReport = r;
    if (Minecraft.getInstance().screen instanceof RitualAltarScreen s) s.report = r;
  }

  @Override
  protected void init() {
    left = (width - W) / 2;
    top = (height - H) / 2;
    report = lastReport;
    ritualTabButton = addRenderableWidget(Button.builder(Component.translatable("gui.arcane.ritual.tab.ritual"), b -> { offeringsTab = false; })
        .bounds(left + 10, top + 20, 80, 14).build());
    offerTabButton = addRenderableWidget(Button.builder(Component.translatable("gui.arcane.ritual.tab.offerings"), b -> { offeringsTab = true; })
        .bounds(left + 94, top + 20, 90, 14).build());
    int by = top + H - 24;
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.ritual.clear"), b -> { composition.clear(); offerSlots.clear(); selected = -1; dirty = 1; })
        .bounds(left + 10, by, 50, 18).build());
    performButton = addRenderableWidget(Button.builder(Component.translatable("gui.arcane.ritual.perform"), b -> send(C2SRitualPacket.PERFORM))
        .bounds(left + W - 214, by, 66, 18).build());
    storeButton = addRenderableWidget(Button.builder(Component.translatable("gui.arcane.ritual.store"), b -> send(C2SRitualPacket.STORE))
        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.arcane.ritual.store.tip")))
        .bounds(left + W - 144, by, 66, 18).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.close"), b -> onClose())
        .bounds(left + W - 74, by, 64, 18).build());
    dirty = 1;
  }

  @Override
  public boolean isPauseScreen() { return false; }

  // ------------------------------------------------------------------ dados
  private List<KnownGlyph> palette() {
    List<KnownGlyph> out = new ArrayList<>();
    for (KnownGlyph g : ClientArcaneData.glyphs()) if (g.usable() && g.category().equals(CATS[cat])) out.add(g);
    return out;
  }

  private List<C2SSaveInscriptionPacket.Entry> entries() {
    List<C2SSaveInscriptionPacket.Entry> out = new ArrayList<>();
    for (Slot s : composition) out.add(new C2SSaveInscriptionPacket.Entry(s.id.toString(), Map.copyOf(s.params)));
    return out;
  }

  private void send(int action) {
    ModNetwork.CHANNEL.sendToServer(new C2SRitualPacket(altar, action, List.copyOf(offerSlots), entries()));
  }

  private KnownGlyph known(ResourceLocation id) {
    for (KnownGlyph g : ClientArcaneData.glyphs()) if (g.id().equals(id)) return g;
    return null;
  }

  private ItemStack inventoryStack(int slot) {
    var player = Minecraft.getInstance().player;
    return player == null || slot < 0 || slot >= 36 ? ItemStack.EMPTY : player.getInventory().getItem(slot);
  }

  @Override
  public void tick() {
    // oferenda cujo item sumiu do inventario cai da lista
    if (offerSlots.removeIf(s -> inventoryStack(s).isEmpty())) dirty = 1;
    if (dirty > 0 && --dirty == 0) send(C2SRitualPacket.PREVIEW);
    boolean ok = report != null && report.valid() && !composition.isEmpty();
    performButton.active = ok;
    storeButton.active = ok;
    ritualTabButton.active = offeringsTab;
    offerTabButton.active = !offeringsTab;
  }

  // ------------------------------------------------------------------ geometria
  private int contentX() { return left + 10; }
  private int paletteY() { return top + 56; }
  private int slotsY() { return paletteY() + ROWS * CELL + 28; }
  private int paramsY() { return slotsY() + 2 * 21 + 14; }
  private int offerSlotX(int i) { return contentX() + 12 + i * 62; }
  private int offerSlotY() { return top + 54; }
  private int invX() { return contentX() + 12; }
  private int invY() { return top + 118; }

  private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

  // ------------------------------------------------------------------ entrada
  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (super.mouseClicked(mx, my, button)) return true;
    return offeringsTab ? clickOfferings(mx, my) : clickRitual(mx, my, button);
  }

  private boolean clickOfferings(double mx, double my) {
    for (int i = 0; i < OfferingValue.MAX_ITEMS; i++) {
      if (in(mx, my, offerSlotX(i), offerSlotY(), 30, 30) && i < offerSlots.size()) { offerSlots.remove(i); dirty = 2; return true; }
    }
    for (int s = 0; s < 36; s++) {
      int[] p = invCell(s);
      if (in(mx, my, p[0], p[1], 18, 18) && !inventoryStack(s).isEmpty()) {
        if (offerSlots.contains(s)) offerSlots.remove((Integer) s);
        else if (offerSlots.size() < OfferingValue.MAX_ITEMS) offerSlots.add(s);
        dirty = 2;
        return true;
      }
    }
    return false;
  }

  /** Inventario: linhas 1-3 (slots 9-35) e, separada, a barra de atalhos (slots 0-8). */
  private int[] invCell(int slot) {
    int col = slot % 9;
    int row = slot < 9 ? 3 : slot / 9 - 1;
    return new int[] {invX() + col * 19, invY() + row * 19 + (slot < 9 ? 5 : 0)};
  }

  private boolean clickRitual(double mx, double my, int button) {
    // categoria: [<] nome [>]
    if (in(mx, my, contentX(), top + 38, 14, 14)) { cat = Math.floorMod(cat - 1, CATS.length); page = 0; return true; }
    if (in(mx, my, contentX() + LEFT_W - 14, top + 38, 14, 14)) { cat = Math.floorMod(cat + 1, CATS.length); page = 0; return true; }
    // paleta
    List<KnownGlyph> list = palette();
    int per = COLS * ROWS;
    for (int i = 0; i < per; i++) {
      int index = page * per + i;
      if (index >= list.size()) break;
      int x = contentX() + (i % COLS) * CELL, y = paletteY() + (i / COLS) * CELL;
      if (in(mx, my, x, y, CELL - 2, CELL - 2)) {
        if (composition.size() < ClientArcaneData.maxGlyphs()) { composition.add(new Slot(list.get(index).id())); selected = composition.size() - 1; dirty = 4; }
        return true;
      }
    }
    // paginas
    int pageY = paletteY() + ROWS * CELL + 1;
    if (in(mx, my, contentX() + LEFT_W - 56, pageY, 12, 10) && page > 0) { page--; return true; }
    if (in(mx, my, contentX() + LEFT_W - 14, pageY, 12, 10) && (page + 1) * per < list.size()) { page++; return true; }
    // composicao: clique seleciona; clique direito (ou Shift) remove
    for (int i = 0; i < composition.size(); i++) {
      int x = contentX() + (i % 10) * 21, y = slotsY() + (i / 10) * 21;
      if (in(mx, my, x, y, 19, 19)) {
        if (button == 1 || hasShiftDown()) { composition.remove(i); selected = Math.min(selected, composition.size() - 1); dirty = 4; }
        else selected = i;
        return true;
      }
    }
    // parametros do glifo selecionado: [-] valor [+]
    if (selected >= 0 && selected < composition.size()) {
      KnownGlyph k = known(composition.get(selected).id);
      if (k != null) {
        for (int i = 0; i < Math.min(2, k.params().size()); i++) {
          int y = paramsY() + i * 12;
          if (my >= y && my < y + 11) {
            if (mx >= contentX() + 110 && mx < contentX() + 124) { bump(k.params().get(i), -1); return true; }
            if (mx >= contentX() + 160 && mx < contentX() + 174) { bump(k.params().get(i), 1); return true; }
          }
        }
      }
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
    if (offeringsTab) return true;
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
    g.drawCenteredString(font, title, left + W / 2, top + 6, ArcaneTheme.GOLD);
    // divisoria entre o conteudo e o relatorio
    g.fill(left + RIGHT_X - 6, top + 38, left + RIGHT_X - 5, top + H - 30, ArcaneTheme.alpha(ArcaneTheme.EDGE, 0.5f));

    KnownGlyph hover = null;
    ItemStack hoverStack = ItemStack.EMPTY;
    if (offeringsTab) hoverStack = renderOfferings(g, mx, my);
    else hover = renderRitual(g, mx, my);
    renderReport(g);
    super.render(g, mx, my, pt);

    if (!hoverStack.isEmpty()) {
      List<Component> tip = new ArrayList<>(hoverStack.getTooltipLines(Minecraft.getInstance().player, net.minecraft.world.item.TooltipFlag.Default.NORMAL));
      tip.add(Component.translatable("gui.arcane.ritual.item_value", OfferingValue.of(hoverStack)).withStyle(net.minecraft.ChatFormatting.GOLD));
      g.renderComponentTooltip(font, tip, mx, my);
    } else if (hover != null) {
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

  private KnownGlyph renderRitual(GuiGraphics g, int mx, int my) {
    KnownGlyph hover = null;
    int cx = contentX();
    // categoria
    g.fill(cx, top + 38, cx + LEFT_W, top + 52, 0x60000000);
    g.drawString(font, "<", cx + 4, top + 41, ArcaneTheme.EDGE_LIT, false);
    g.drawString(font, ">", cx + LEFT_W - 10, top + 41, ArcaneTheme.EDGE_LIT, false);
    g.drawCenteredString(font, Component.translatable("category.arcane." + CATS[cat]), cx + LEFT_W / 2, top + 41, ArcaneTheme.TEXT);

    // paleta
    List<KnownGlyph> list = palette();
    int per = COLS * ROWS;
    for (int i = 0; i < per; i++) {
      int index = page * per + i;
      if (index >= list.size()) break;
      int x = cx + (i % COLS) * CELL, y = paletteY() + (i / COLS) * CELL;
      boolean over = in(mx, my, x, y, CELL - 2, CELL - 2);
      g.fill(x, y, x + CELL - 2, y + CELL - 2, over ? 0xA03A2C70 : 0x70000000);
      g.renderOutline(x, y, CELL - 2, CELL - 2, ArcaneTheme.alpha(ArcaneTheme.EDGE, over ? 1f : 0.6f));
      g.blit(iconFor(list.get(index).id()), x + 3, y + 3, 0, 0, 16, 16, 16, 16);
      if (over) hover = list.get(index);
    }
    if (list.isEmpty()) g.drawString(font, Component.translatable("gui.arcane.ritual.none_in_tab"), cx + 2, paletteY() + 8, ArcaneTheme.TEXT_DIM, false);
    int pages = Math.max(1, (list.size() + per - 1) / per);
    int pageY = paletteY() + ROWS * CELL + 1;
    g.drawString(font, "<", cx + LEFT_W - 54, pageY + 1, ArcaneTheme.TEXT_DIM, false);
    g.drawCenteredString(font, (page + 1) + "/" + pages, cx + LEFT_W - 32, pageY + 1, ArcaneTheme.TEXT_DIM);
    g.drawString(font, ">", cx + LEFT_W - 12, pageY + 1, ArcaneTheme.TEXT_DIM, false);

    // composicao
    g.drawString(font, Component.translatable("gui.arcane.ritual.composition", composition.size(), ClientArcaneData.maxGlyphs()), cx, slotsY() - 11, ArcaneTheme.VIOLET, false);
    for (int i = 0; i < composition.size(); i++) {
      int x = cx + (i % 10) * 21, y = slotsY() + (i / 10) * 21;
      boolean on = i == selected;
      g.fill(x, y, x + 19, y + 19, on ? 0xA0504090 : 0x70000000);
      g.renderOutline(x, y, 19, 19, on ? ArcaneTheme.GOLD : ArcaneTheme.EDGE);
      g.blit(iconFor(composition.get(i).id), x + 2, y + 2, 0, 0, 15, 15, 15, 15);
      if (in(mx, my, x, y, 19, 19)) hover = known(composition.get(i).id);
    }
    if (composition.isEmpty()) g.drawString(font, Component.translatable("gui.arcane.ritual.empty"), cx + 2, slotsY() + 6, ArcaneTheme.TEXT_DIM, false);

    // parametros do selecionado
    if (selected >= 0 && selected < composition.size()) {
      Slot s = composition.get(selected);
      KnownGlyph k = known(s.id);
      g.drawString(font, ArcaneWorkbenchScreen.glyphName(s.id), cx, paramsY() - 10, ArcaneTheme.TEXT, false);
      if (k != null) {
        for (int i = 0; i < Math.min(2, k.params().size()); i++) {
          ParamDef p = k.params().get(i);
          int y = paramsY() + i * 12;
          g.drawString(font, font.plainSubstrByWidth(p.key(), 100), cx, y + 2, ArcaneTheme.TEXT_DIM, false);
          g.drawString(font, "[-]", cx + 110, y + 2, ArcaneTheme.EDGE_LIT, false);
          g.drawCenteredString(font, num(s.params.getOrDefault(p.key(), p.def())), cx + 143, y + 2, ArcaneTheme.TEXT);
          g.drawString(font, "[+]", cx + 160, y + 2, ArcaneTheme.EDGE_LIT, false);
        }
      }
    }
    return hover;
  }

  /** Aba Oferendas: 3 espacos grandes e o inventario para escolher (clique = pega/solta). */
  private ItemStack renderOfferings(GuiGraphics g, int mx, int my) {
    ItemStack hover = ItemStack.EMPTY;
    int cx = contentX();
    g.drawString(font, Component.translatable("gui.arcane.ritual.offer_hint"), cx, top + 40, ArcaneTheme.TEXT_DIM, false);
    for (int i = 0; i < OfferingValue.MAX_ITEMS; i++) {
      int x = offerSlotX(i), y = offerSlotY();
      boolean filled = i < offerSlots.size();
      g.fill(x, y, x + 30, y + 30, filled ? 0xA0504090 : 0x70000000);
      g.renderOutline(x, y, 30, 30, filled ? ArcaneTheme.GOLD : ArcaneTheme.EDGE);
      if (filled) {
        ItemStack st = inventoryStack(offerSlots.get(i));
        g.renderItem(st, x + 7, y + 7);
        g.drawCenteredString(font, "+" + OfferingValue.of(st) + "%", x + 15, y + 33, ArcaneTheme.GOLD);
        if (in(mx, my, x, y, 30, 30)) hover = st;
      }
    }
    g.drawString(font, Component.translatable("gui.arcane.ritual.inventory"), cx, invY() - 11, ArcaneTheme.VIOLET, false);
    for (int s = 0; s < 36; s++) {
      int[] p = invCell(s);
      ItemStack st = inventoryStack(s);
      boolean chosen = offerSlots.contains(s);
      g.fill(p[0], p[1], p[0] + 18, p[1] + 18, chosen ? 0xA0504090 : 0x60000000);
      g.renderOutline(p[0], p[1], 18, 18, chosen ? ArcaneTheme.GOLD : ArcaneTheme.alpha(ArcaneTheme.EDGE, 0.5f));
      if (!st.isEmpty()) {
        g.renderItem(st, p[0] + 1, p[1] + 1);
        if (in(mx, my, p[0], p[1], 18, 18)) hover = st;
      }
    }
    return hover;
  }

  /** Coluna da direita: relatorio do servidor, com quebra de linha para nada sair da moldura. */
  private void renderReport(GuiGraphics g) {
    int x = left + RIGHT_X, w = RIGHT_W, y = top + 40;
    g.drawString(font, Component.translatable("gui.arcane.ritual.report"), x, y, ArcaneTheme.VIOLET, false);
    y += 12;
    if (report == null) {
      g.drawString(font, "...", x, y, ArcaneTheme.TEXT_DIM, false);
      return;
    }
    Component circle = report.circleLevel() > 0
        ? Component.translatable("gui.arcane.ritual.circle", Component.translatable("ritual.arcane.level." + report.circleLevel()), report.completenessPct())
        : Component.translatable("gui.arcane.ritual.circle_none", report.completenessPct());
    y = wrap(g, circle, x, y, w, report.circleLevel() > 0 ? 0xFF9BE08A : ArcaneTheme.DANGER);
    if (report.needLevel() > 0) {
      boolean enough = report.circleLevel() >= report.needLevel() || report.legendary();
      y = wrap(g, Component.translatable("gui.arcane.ritual.needs_level", Component.translatable("ritual.arcane.level." + report.needLevel())), x, y, w,
          enough ? 0xFF9BE08A : ArcaneTheme.DANGER);
    }
    boolean manaOk = report.mana() >= report.cost();
    y = wrap(g, Component.translatable("gui.arcane.ritual.mana", report.cost(), report.mana()), x, y, w, manaOk ? ArcaneTheme.EDGE_LIT : ArcaneTheme.DANGER);
    y += 3;
    // componentes que os glifos exigem
    g.drawString(font, Component.translatable("gui.arcane.ritual.offerings"), x, y, ArcaneTheme.GOLD, false);
    y += 10;
    if (report.needs().isEmpty()) {
      y = wrap(g, Component.translatable("gui.arcane.ritual.no_offerings"), x, y, w, ArcaneTheme.TEXT_DIM);
    }
    for (RitualService.Need n : report.needs()) {
      var item = ForgeRegistries.ITEMS.getValue(n.item());
      if (item != null) g.renderItem(new ItemStack(item), x, y - 4, 0);
      boolean ok = n.have() >= n.need();
      String have = n.have() >= 9999 ? "∞" : String.valueOf(n.have());
      g.drawString(font, have + "/" + n.need(), x + 20, y, ok ? 0xFF9BE08A : ArcaneTheme.DANGER, false);
      y += 15;
    }
    y += 3;
    // valor do ritual: tamanho + oferendas = total
    g.drawString(font, Component.translatable("gui.arcane.ritual.value"), x, y, ArcaneTheme.GOLD, false);
    y += 10;
    g.drawString(font, Component.translatable("gui.arcane.ritual.value_size", report.sizePct()), x, y, ArcaneTheme.TEXT_DIM, false);
    y += 10;
    g.drawString(font, Component.translatable("gui.arcane.ritual.value_offer", report.offeringPct(), OfferingValue.LEGENDARY_THRESHOLD), x, y,
        report.legendary() ? ArcaneTheme.GOLD : ArcaneTheme.TEXT_DIM, false);
    y += 10;
    if (report.legendary()) {
      y = wrap(g, Component.translatable("gui.arcane.ritual.legendary_on"), x, y, w, ArcaneTheme.GOLD);
    }
    g.drawString(font, Component.translatable("gui.arcane.ritual.value_total", report.totalPct()), x, y, ArcaneTheme.TEXT, false);
    y += 12;
    if (!report.valid() && !composition.isEmpty()) {
      wrap(g, report.message(), x, y, w, ArcaneTheme.DANGER);
    }
  }

  /** Escreve o texto quebrado na largura dada e devolve o proximo y. */
  private int wrap(GuiGraphics g, Component text, int x, int y, int w, int color) {
    for (var line : font.split(text, w)) {
      g.drawString(font, line, x, y, color, false);
      y += 9;
    }
    return y + 1;
  }
}
