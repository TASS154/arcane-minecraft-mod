package com.arcane.client.screen;

import com.arcane.client.ClientArcaneData;
import com.arcane.client.ClientArcaneData.GlyphEntry;
import com.arcane.client.ClientArcaneData.KnownGlyph;
import com.arcane.client.ClientArcaneData.SpellEntry;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.C2SDeleteInscriptionPacket;
import com.arcane.network.packet.C2SSelectInscriptionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Grimorio: suas magias salvas, os glifos que voce conhece (e quantos faltam descobrir) e o diario de descobertas.
 * Tudo e leitura do estado sincronizado pelo servidor; so ativar e apagar enviam pedidos (validados la).
 *
 * Layout (bate com tools/gen_textures.py -> textures/gui/grimoire.png, 256x200):
 *   abas em y=4 | paginas (12,24)-(244,172) | botoes em y=176
 */
public class GrimoireScreen extends Screen {
  private static final ResourceLocation TEXTURE = new ResourceLocation("arcane", "textures/gui/grimoire.png");
  private static final int W = 256;
  private static final int H = 200;
  private static final int ROW = 12;
  private static final int LIST_ROWS = 11;
  private static final int COLS = 10;
  private static final int CELL = 20;

  private enum Tab { SPELLS, GLYPHS, JOURNAL }

  private int left;
  private int top;
  private Tab tab = Tab.SPELLS;
  private int spellScroll;
  private int logScroll;
  private int glyphScroll;
  private UUID selectedSpell;
  private ResourceLocation selectedGlyph;
  private boolean confirmDelete;
  private Button activateButton;
  private Button deleteButton;

  public GrimoireScreen() {
    super(Component.translatable("gui.arcane.grimoire"));
  }

  @Override
  protected void init() {
    left = (width - W) / 2;
    top = (height - H) / 2;
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.tab.spells"), b -> setTab(Tab.SPELLS))
        .bounds(left + 14, top + 2, 70, 18).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.tab.glyphs"), b -> setTab(Tab.GLYPHS))
        .bounds(left + 86, top + 2, 70, 18).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.tab.journal"), b -> setTab(Tab.JOURNAL))
        .bounds(left + 158, top + 2, 70, 18).build());
    activateButton = addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.activate"), b -> activate())
        .bounds(left + 134, top + 176, 52, 20).build());
    deleteButton = addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.delete"), b -> delete())
        .bounds(left + 190, top + 176, 52, 20).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.close"), b -> onClose())
        .bounds(left + 14, top + 176, 52, 20).build());
    if (selectedSpell == null) selectedSpell = ClientArcaneData.active().orElse(null);
    updateButtons();
  }

  private void setTab(Tab t) {
    tab = t;
    confirmDelete = false;
    updateButtons();
  }

  private void updateButtons() {
    boolean spells = tab == Tab.SPELLS && selectedSpellEntry().isPresent();
    activateButton.visible = spells;
    deleteButton.visible = spells;
    deleteButton.setMessage(Component.translatable(confirmDelete ? "gui.arcane.grimoire.confirm" : "gui.arcane.grimoire.delete")
        .withStyle(confirmDelete ? ChatFormatting.RED : ChatFormatting.WHITE));
    activateButton.active = selectedSpellEntry().map(s -> !ClientArcaneData.active().map(a -> a.equals(s.id())).orElse(false)).orElse(false);
  }

  private Optional<SpellEntry> selectedSpellEntry() {
    if (selectedSpell == null) return Optional.empty();
    return ClientArcaneData.spells().stream().filter(s -> s.id().equals(selectedSpell)).findFirst();
  }

  private void activate() {
    selectedSpellEntry().ifPresent(s -> ModNetwork.CHANNEL.sendToServer(new C2SSelectInscriptionPacket(s.id())));
  }

  private void delete() {
    Optional<SpellEntry> spell = selectedSpellEntry();
    if (spell.isEmpty()) return;
    if (!confirmDelete) {
      confirmDelete = true;
    } else {
      ModNetwork.CHANNEL.sendToServer(new C2SDeleteInscriptionPacket(spell.get().id()));
      selectedSpell = null;
      confirmDelete = false;
    }
    updateButtons();
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  // ------------------------------------------------------------------ entrada
  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (button == 0) {
      if (tab == Tab.SPELLS) {
        int row = (int) ((my - (top + 28)) / ROW);
        if (mx >= left + 14 && mx <= left + 124 && row >= 0 && row < LIST_ROWS) {
          List<SpellEntry> spells = ClientArcaneData.spells();
          int index = spellScroll + row;
          if (index < spells.size()) {
            selectedSpell = spells.get(index).id();
            confirmDelete = false;
            updateButtons();
            return true;
          }
        }
      } else if (tab == Tab.GLYPHS) {
        KnownGlyph g = glyphAt(mx, my);
        if (g != null) {
          selectedGlyph = g.id();
          return true;
        }
      }
    }
    return super.mouseClicked(mx, my, button);
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double delta) {
    int step = delta > 0 ? -1 : 1;
    switch (tab) {
      case SPELLS -> spellScroll = Math.max(0, Math.min(spellScroll + step, Math.max(0, ClientArcaneData.spells().size() - LIST_ROWS)));
      case JOURNAL -> logScroll = Math.max(0, Math.min(logScroll + step, Math.max(0, ClientArcaneData.log().size() - LIST_ROWS)));
      case GLYPHS -> {
        int rows = (slotCount() + COLS - 1) / COLS;
        glyphScroll = Math.max(0, Math.min(glyphScroll + step, Math.max(0, rows - 5)));
      }
    }
    return true;
  }

  /** 5 colunas em cada pagina, deixando a lombada livre. */
  private int cellX(int col) {
    return left + (col < 5 ? 18 + col * CELL : 138 + (col - 5) * CELL);
  }

  private int slotCount() {
    return Math.max(ClientArcaneData.totalGlyphs(), ClientArcaneData.glyphs().size());
  }

  private KnownGlyph glyphAt(double mx, double my) {
    List<KnownGlyph> glyphs = ClientArcaneData.glyphs();
    for (int i = 0; i < COLS * 5; i++) {
      int index = glyphScroll * COLS + i;
      if (index >= glyphs.size()) break;
      int x = cellX(i % COLS);
      int y = top + 28 + (i / COLS) * CELL;
      if (mx >= x && mx < x + 18 && my >= y && my < y + 18) return glyphs.get(index);
    }
    return null;
  }

  // ------------------------------------------------------------------ desenho
  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    renderBackground(g);
    g.blit(TEXTURE, left, top, 0, 0, W, H, 256, 256);
    super.render(g, mouseX, mouseY, partialTick);
    switch (tab) {
      case SPELLS -> renderSpells(g, mouseX, mouseY);
      case GLYPHS -> renderGlyphs(g, mouseX, mouseY);
      case JOURNAL -> renderJournal(g);
    }
  }

  private static Component spellName(SpellEntry s) {
    return s.name().isEmpty() ? Component.translatable("gui.arcane.unnamed") : Component.literal(s.name());
  }

  private void renderSpells(GuiGraphics g, int mouseX, int mouseY) {
    List<SpellEntry> spells = ClientArcaneData.spells();
    if (spells.isEmpty()) {
      g.drawWordWrap(font, Component.translatable("gui.arcane.grimoire.empty_spells"), left + 18, top + 30, 100, 0x5A4630);
      return;
    }
    Optional<UUID> active = ClientArcaneData.active();
    for (int r = 0; r < LIST_ROWS; r++) {
      int index = spellScroll + r;
      if (index >= spells.size()) break;
      SpellEntry s = spells.get(index);
      int y = top + 28 + r * ROW;
      boolean selected = s.id().equals(selectedSpell);
      if (selected) g.fill(left + 14, y - 1, left + 124, y + ROW - 1, 0x55A07830);
      boolean isActive = active.map(a -> a.equals(s.id())).orElse(false);
      if (isActive) g.drawString(font, "★", left + 16, y + 1, 0xC08000, false);
      String name = font.plainSubstrByWidth(spellName(s).getString(), 78);
      g.drawString(font, name, left + 26, y + 1, selected ? 0x2A1A08 : 0x4A3820, false);
      if (s.cost() >= 0) {
        String cost = String.valueOf(s.cost());
        g.drawString(font, cost, left + 122 - font.width(cost), y + 1, 0x1F6F8F, false);
      }
    }
    if (spells.size() > LIST_ROWS) {
      g.drawString(font, (spellScroll + 1) + "-" + Math.min(spells.size(), spellScroll + LIST_ROWS) + "/" + spells.size(),
          left + 14, top + 162, 0x7A6A50, false);
    }

    Optional<SpellEntry> sel = selectedSpellEntry();
    if (sel.isEmpty()) {
      g.drawWordWrap(font, Component.translatable("gui.arcane.grimoire.pick_spell"), left + 134, top + 30, 100, 0x5A4630);
      return;
    }
    SpellEntry spell = sel.get();
    g.drawString(font, font.plainSubstrByWidth(spellName(spell).getString(), 106), left + 134, top + 28, 0x2A1A08, false);
    if (spell.cost() >= 0) {
      g.drawString(font, Component.translatable("gui.arcane.glyph_cost", spell.cost()), left + 134, top + 40, 0x1F6F8F, false);
    }
    List<Component> tip = null;
    for (int i = 0; i < spell.glyphs().size(); i++) {
      GlyphEntry entry = spell.glyphs().get(i);
      int x = left + 134 + (i % 6) * 18;
      int y = top + 56 + (i / 6) * 18;
      g.blit(iconFor(entry.id()), x, y, 0, 0, 16, 16, 16, 16);
      if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
        tip = glyphEntryTooltip(entry);
      }
    }
    if (tip != null) g.renderComponentTooltip(font, tip, mouseX, mouseY);
  }

  private List<Component> glyphEntryTooltip(GlyphEntry entry) {
    List<Component> lines = new ArrayList<>();
    lines.add(ArcaneWorkbenchScreen.glyphName(entry.id()));
    ClientArcaneData.glyph(entry.id()).ifPresent(k -> {
      lines.add(Component.translatable("category.arcane." + k.category()).withStyle(ChatFormatting.GRAY));
      entry.params().forEach((key, v) -> lines.add(Component.literal(key + ": " + trim(v)).withStyle(ChatFormatting.AQUA)));
    });
    return lines;
  }

  private void renderGlyphs(GuiGraphics g, int mouseX, int mouseY) {
    List<KnownGlyph> glyphs = ClientArcaneData.glyphs();
    int total = slotCount();
    long identified = glyphs.stream().filter(KnownGlyph::usable).count();
    g.drawString(font, Component.translatable("gui.arcane.grimoire.discovered", identified, total), left + 18, top + 168, 0x5A4630, false);

    KnownGlyph hovered = null;
    for (int i = 0; i < COLS * 5; i++) {
      int index = glyphScroll * COLS + i;
      if (index >= total) break;
      int x = cellX(i % COLS);
      int y = top + 28 + (i / COLS) * CELL;
      if (index < glyphs.size()) {
        KnownGlyph k = glyphs.get(index);
        g.blit(iconFor(k.id()), x, y, 0, 0, 16, 16, 16, 16);
        if (!k.usable()) {
          g.fill(x, y, x + 16, y + 16, 0x99000000);
          g.drawString(font, "?", x + 5, y + 4, 0xFFD860, false);
        }
        if (k.id().equals(selectedGlyph)) {
          g.renderOutline(x - 1, y - 1, 18, 18, 0xFFC08000);
        }
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) hovered = k;
      } else {
        g.blit(iconFor(new ResourceLocation("arcane", "unknown")), x, y, 0, 0, 16, 16, 16, 16);
      }
    }

    // Detalhe do glifo selecionado, na parte de baixo das paginas
    Optional<KnownGlyph> sel = selectedGlyph == null ? Optional.empty() : ClientArcaneData.glyph(selectedGlyph);
    if (sel.isPresent()) {
      KnownGlyph k = sel.get();
      g.drawString(font, ArcaneWorkbenchScreen.glyphName(k.id()).copy().withStyle(s -> s.withColor(ArcaneWorkbenchScreen.categoryColor(k.category()) & 0x7F7F7F)),
          left + 18, top + 132, 0x2A1A08, false);
      g.drawString(font, Component.translatable("category.arcane." + k.category()).getString() + " - "
          + Component.translatable("knowledge.arcane." + knowledgeName(k.level())).getString(), left + 18, top + 142, 0x5A4630, false);
      String descKey = "glyph." + k.id().getNamespace() + "." + k.id().getPath() + ".desc";
      if (k.usable() && I18n.exists(descKey)) {
        g.drawWordWrap(font, Component.translatable(descKey), left + 18, top + 152, 220, 0x4A3820);
      } else if (!k.usable()) {
        g.drawWordWrap(font, Component.translatable("gui.arcane.glyph_unknown_hint"), left + 18, top + 152, 220, 0x8A6A20);
      }
    }
    if (hovered != null) {
      List<Component> lines = new ArrayList<>();
      lines.add(ArcaneWorkbenchScreen.glyphName(hovered.id()));
      lines.add(Component.translatable("category.arcane." + hovered.category()).withStyle(ChatFormatting.GRAY));
      lines.add(Component.translatable("gui.arcane.glyph_cost", hovered.cost()).withStyle(ChatFormatting.AQUA));
      g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }
  }

  private void renderJournal(GuiGraphics g) {
    List<String> log = ClientArcaneData.log();
    if (log.isEmpty()) {
      g.drawWordWrap(font, Component.translatable("gui.arcane.grimoire.empty_journal"), left + 18, top + 30, 210, 0x5A4630);
      return;
    }
    // Mais recentes primeiro
    for (int r = 0; r < LIST_ROWS; r++) {
      int index = log.size() - 1 - (logScroll + r);
      if (index < 0) break;
      g.drawString(font, font.plainSubstrByWidth(describe(log.get(index)).getString(), 218), left + 18, top + 28 + r * ROW, 0x4A3820, false);
    }
  }

  /** "experiment:arcane:fire" -> "Experimento com Fogo". */
  static Component describe(String entry) {
    int colon = entry.indexOf(':');
    if (colon < 0) return Component.literal(entry);
    String kind = entry.substring(0, colon);
    String rest = entry.substring(colon + 1);
    ResourceLocation glyph = ResourceLocation.tryParse(rest);
    String key = "log.arcane." + kind;
    if (!I18n.exists(key)) return Component.literal(entry);
    if (glyph == null) return Component.translatable(key);
    return Component.translatable(key, ArcaneWorkbenchScreen.glyphName(glyph));
  }

  private static ResourceLocation iconFor(ResourceLocation glyph) {
    return new ResourceLocation(glyph.getNamespace(), "textures/gui/glyphs/" + glyph.getPath() + ".png");
  }

  private static String knowledgeName(int level) {
    return switch (level) {
      case 1 -> "suspected";
      case 2 -> "partial";
      case 3 -> "identified";
      case 4 -> "mastered";
      default -> "unknown";
    };
  }

  private static String trim(double v) {
    return v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.1f", v);
  }
}
