package com.arcane.client.screen;

import com.arcane.client.ClientArcaneData;
import com.arcane.client.ClientArcaneData.GlyphEntry;
import com.arcane.client.ClientArcaneData.KnownGlyph;
import com.arcane.client.ClientArcaneData.SpellEntry;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.C2SGrimoireOrgPacket;
import com.arcane.network.packet.C2SOpenWritePacket;
import com.arcane.network.packet.C2SRenameInscriptionPacket;
import com.arcane.network.packet.C2SJournalNotePacket;
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
  private static final int SPELL_ROWS = 8;
  private static final int COLS = 5;
  private static final int GRID_ROWS = 7;
  private static final int CELL = 20;

  private enum Tab { SPELLS, GLYPHS, JOURNAL, DISCOVERIES }
  private int discoveryScroll;

  private int left;
  private int top;
  private Tab tab = Tab.SPELLS;
  private int spellPage;
  private int filterMode;
  private net.minecraft.client.gui.components.EditBox searchBox, folderBox;
  private Button prevPage, nextPage, filterButton, modeButton, gearButton;
  private final Button[] loadoutButtons = new Button[com.arcane.grimoire.SpellPrefs.LOADOUTS];
  private int logScroll;
  private int glyphScroll;
  private UUID selectedSpell;
  private ResourceLocation selectedGlyph;
  private boolean confirmDelete;
  private Button deleteButton;
  private Button copyButton;

  public GrimoireScreen() {
    super(Component.translatable("gui.arcane.grimoire"));
  }

  @Override
  protected void init() {
    left = (width - W) / 2;
    top = (height - H) / 2;
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.tab.spells"), b -> setTab(Tab.SPELLS))
        .bounds(left + 14, top + 2, 52, 18).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.tab.glyphs"), b -> setTab(Tab.GLYPHS))
        .bounds(left + 68, top + 2, 52, 18).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.tab.journal"), b -> setTab(Tab.JOURNAL))
        .bounds(left + 122, top + 2, 52, 18).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.tab.discoveries"), b -> setTab(Tab.DISCOVERIES))
        .bounds(left + 176, top + 2, 54, 18).build());
    deleteButton = addRenderableWidget(Button.builder(Component.literal("✖"), b -> delete())
        .bounds(left + 222, top + 176, 20, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("gui.arcane.grimoire.delete"))).build());
    copyButton = addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.write"), b -> openWrite())
        .bounds(left + 14, top + 176, 60, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("gui.arcane.grimoire.write.tooltip"))).build());
    renameBox = new net.minecraft.client.gui.components.EditBox(font, left + 134, top + 25, 106, 14, Component.translatable("gui.arcane.name"));
    renameBox.setMaxLength(32);
    renameBox.setHint(Component.translatable("gui.arcane.name"));
    addRenderableWidget(renameBox);
    renameBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.arcane.grimoire.rename.tooltip")));
    noteBox = new net.minecraft.client.gui.components.EditBox(font, left + 18, top + 176, 150, 18, Component.translatable("gui.arcane.grimoire.note"));
    noteBox.setMaxLength(C2SJournalNotePacket.MAX);
    noteBox.setHint(Component.translatable("gui.arcane.grimoire.note.hint"));
    addRenderableWidget(noteBox);
    noteButton = addRenderableWidget(Button.builder(Component.translatable("gui.arcane.grimoire.note.add"), b -> addNote())
        .bounds(left + 172, top + 175, 60, 20).build());
    searchBox = new net.minecraft.client.gui.components.EditBox(font, left + 14, top + 24, 110, 12, Component.translatable("gui.arcane.search"));
    searchBox.setMaxLength(24);
    searchBox.setHint(Component.translatable("gui.arcane.search.hint"));
    searchBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.arcane.search.tip")));
    searchBox.setResponder(v -> { spellPage = 0; });
    addRenderableWidget(searchBox);
    prevPage = addRenderableWidget(Button.builder(Component.literal("◀"), b -> turnPage(-1)).bounds(left + 14, top + 136, 18, 14).build());
    nextPage = addRenderableWidget(Button.builder(Component.literal("▶"), b -> turnPage(1)).bounds(left + 106, top + 136, 18, 14).build());
    filterButton = addRenderableWidget(Button.builder(filterLabel(), b -> { filterMode = (filterMode + 1) % (4 + folders().size()); spellPage = 0; updateButtons(); })
        .bounds(left + 14, top + 152, 110, 14).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.arcane.filter.tip"))).build());
    modeButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> selectedSpellEntry().ifPresent(sp ->
        ModNetwork.CHANNEL.sendToServer(new C2SGrimoireOrgPacket(C2SGrimoireOrgPacket.MODE, sp.id(), nextMode(sp), ""))))
        .bounds(left + 134, top + 94, 22, 14).build());
    folderBox = new net.minecraft.client.gui.components.EditBox(font, left + 134, top + 112, 106, 12, Component.translatable("gui.arcane.folder"));
    folderBox.setMaxLength(com.arcane.grimoire.SpellPrefs.MAX_FOLDER);
    folderBox.setHint(Component.translatable("gui.arcane.folder"));
    folderBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.arcane.folder.tip")));
    addRenderableWidget(folderBox);
    for (int i = 0; i < loadoutButtons.length; i++) {
      final int set = i;
      loadoutButtons[i] = addRenderableWidget(Button.builder(Component.literal("" + (i + 1)), b -> selectedSpellEntry().ifPresent(sp ->
          ModNetwork.CHANNEL.sendToServer(new C2SGrimoireOrgPacket(C2SGrimoireOrgPacket.LOADOUT_TOGGLE, sp.id(), set, ""))))
          .bounds(left + 160 + i * 20, top + 94, 18, 14).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.arcane.loadout.tip"))).build());
    }
    gearButton = addRenderableWidget(Button.builder(Component.literal("⚙"), b -> minecraft.setScreen(new ArcaneOptionsScreen(this)))
        .bounds(left + 232, top + 2, 18, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.arcane.options"))).build());
    if (selectedSpell == null) selectedSpell = ClientArcaneData.active().orElse(null);
    updateButtons();
  }

  // ------------------------------------------------------------------ lista por pagina, busca e filtros
  private List<String> folders() {
    return ClientArcaneData.spells().stream().map(SpellEntry::folder).filter(f -> !f.isEmpty()).distinct().sorted().toList();
  }

  private Component filterLabel() {
    int folderCount = folders().size();
    if (filterMode >= 4 + folderCount) filterMode = 0;
    return switch (filterMode) {
      case 0 -> Component.translatable("gui.arcane.filter.all");
      case 1 -> Component.translatable("gui.arcane.filter.favorites");
      case 2 -> Component.translatable("gui.arcane.filter.recent");
      case 3 -> Component.translatable("gui.arcane.filter.set", ClientArcaneData.activeLoadout() + 1);
      default -> Component.translatable("gui.arcane.filter.folder", folders().get(filterMode - 4));
    };
  }

  private static int nextMode(SpellEntry sp) {
    boolean toggle = sp.glyphs().stream().anyMatch(g -> com.arcane.execution.CastChannels.TOGGLE_GLYPHS.contains(g.id().getPath()));
    int n = sp.mode();
    do { n = (n + 1) % 4; } while (n == 3 && !toggle);
    return n;
  }

  /** Magias que passam pela busca e pelo filtro, favoritas primeiro. Busca: texto no nome ou num glifo (id ou nome), ou ">5" / "<5" para o custo. */
  private List<SpellEntry> filtered() {
    String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(java.util.Locale.ROOT);
    List<SpellEntry> all = ClientArcaneData.spells();
    List<UUID> set = ClientArcaneData.loadouts().isEmpty() ? List.of() : ClientArcaneData.loadouts().get(Math.floorMod(ClientArcaneData.activeLoadout(), ClientArcaneData.loadouts().size()));
    List<String> folders = folders();
    List<SpellEntry> out = new ArrayList<>();
    for (SpellEntry sp : all) {
      boolean ok = switch (filterMode) {
        case 0 -> true;
        case 1 -> sp.favorite();
        case 2 -> sp.lastUsed() > 0;
        case 3 -> set.contains(sp.id());
        default -> filterMode - 4 < folders.size() && sp.folder().equals(folders.get(filterMode - 4));
      };
      if (ok && !q.isEmpty() && !matches(sp, q)) ok = false;
      if (ok) out.add(sp);
    }
    if (filterMode == 2) out.sort((a, b) -> Long.compare(b.lastUsed(), a.lastUsed()));
    else out.sort((a, b) -> Boolean.compare(b.favorite(), a.favorite()));
    return out;
  }

  private static boolean matches(SpellEntry sp, String q) {
    if ((q.startsWith(">") || q.startsWith("<")) && q.length() > 1) {
      try {
        int n = Integer.parseInt(q.substring(1).trim());
        return q.startsWith(">") ? sp.cost() > n : sp.cost() >= 0 && sp.cost() < n;
      } catch (NumberFormatException e) {
        return false;
      }
    }
    if (sp.name().toLowerCase(java.util.Locale.ROOT).contains(q) || sp.folder().toLowerCase(java.util.Locale.ROOT).contains(q)) return true;
    for (GlyphEntry g : sp.glyphs()) {
      if (g.id().getPath().contains(q) || ArcaneWorkbenchScreen.glyphName(g.id()).getString().toLowerCase(java.util.Locale.ROOT).contains(q)) return true;
    }
    return false;
  }

  private int pageCount() { return Math.max(1, (filtered().size() + SPELL_ROWS - 1) / SPELL_ROWS); }

  private void turnPage(int d) {
    spellPage = Math.max(0, Math.min(pageCount() - 1, spellPage + d));
    updateButtons();
  }

  private void setTab(Tab t) {
    tab = t;
    confirmDelete = false;
    updateButtons();
  }

  private net.minecraft.client.gui.components.EditBox renameBox;
  private UUID renameShownFor;
  private int shownMode = -1;
  private UUID folderShownFor;
  private net.minecraft.client.gui.components.EditBox noteBox;
  private Button noteButton;

  private void addNote() {
    String text = noteBox.getValue().trim();
    if (text.isEmpty()) return;
    ModNetwork.CHANNEL.sendToServer(new C2SJournalNotePacket(text));
    noteBox.setValue("");
    logScroll = 0;
  }

  private void rename() {
    selectedSpellEntry().ifPresent(sp -> ModNetwork.CHANNEL.sendToServer(new C2SRenameInscriptionPacket(sp.id(), renameBox.getValue())));
  }

  private void updateButtons() {
    boolean renaming = tab == Tab.SPELLS && selectedSpellEntry().isPresent();
    if (renameBox != null) {
      renameBox.visible = renaming;
      UUID now = renaming ? selectedSpell : null;
      if (!java.util.Objects.equals(now, renameShownFor)) {
        renameShownFor = now;
        renameBox.setValue(renaming ? selectedSpellEntry().get().name() : "");
      }
    }
    if (noteBox != null) {
      noteBox.visible = tab == Tab.JOURNAL;
      noteBox.setEditable(tab == Tab.JOURNAL);
      noteButton.visible = tab == Tab.JOURNAL;
    }
    boolean onSpells = tab == Tab.SPELLS;
    if (searchBox != null) {
      spellPage = Math.max(0, Math.min(pageCount() - 1, spellPage));
      searchBox.visible = onSpells;
      prevPage.visible = onSpells; nextPage.visible = onSpells; filterButton.visible = onSpells;
      prevPage.active = spellPage > 0; nextPage.active = spellPage < pageCount() - 1;
      filterButton.setMessage(filterLabel());
      var sel = selectedSpellEntry();
      boolean has = onSpells && sel.isPresent();
      modeButton.visible = has; folderBox.visible = has;
      for (Button lb : loadoutButtons) lb.visible = has;
      if (has) {
        SpellEntry sp = sel.get();
        Component modeName = Component.translatable("gui.arcane.mode." + sp.mode());
        modeButton.setMessage(Component.literal(modeName.getString().substring(0, 1).toUpperCase(java.util.Locale.ROOT)));
        if (sp.mode() != shownMode) {
          shownMode = sp.mode();
          modeButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(modeName.copy().append("\n").append(Component.translatable("gui.arcane.mode.tip"))));
        }
        if (!java.util.Objects.equals(sp.id(), folderShownFor)) { folderShownFor = sp.id(); folderBox.setValue(sp.folder()); }
        for (int i = 0; i < loadoutButtons.length; i++) {
          boolean in = i < ClientArcaneData.loadouts().size() && ClientArcaneData.loadouts().get(i).contains(sp.id());
          loadoutButtons[i].setMessage(Component.literal((in ? "§a" : "§8") + (i + 1)));
        }
      }
    }
    boolean spells = tab == Tab.SPELLS && selectedSpellEntry().isPresent();
    deleteButton.visible = spells;
    copyButton.visible = tab == Tab.SPELLS;
    deleteButton.setMessage(Component.literal(confirmDelete ? "?" : "✖").withStyle(confirmDelete ? ChatFormatting.RED : ChatFormatting.WHITE));
  }

  private Optional<SpellEntry> selectedSpellEntry() {
    if (selectedSpell == null) return Optional.empty();
    return ClientArcaneData.spells().stream().filter(s -> s.id().equals(selectedSpell)).findFirst();
  }

  private void activate() {
    selectedSpellEntry().ifPresent(s -> ModNetwork.CHANNEL.sendToServer(new C2SSelectInscriptionPacket(s.id())));
  }

  private void openWrite() {
    ModNetwork.CHANNEL.sendToServer(new C2SOpenWritePacket());
  }

  private void delete() {
    Optional<SpellEntry> spell = selectedSpellEntry();
    if (spell.isEmpty()) return;
    if (!confirmDelete) {
      confirmDelete = true;
    } else {
      ModNetwork.CHANNEL.sendToServer(new C2SDeleteInscriptionPacket(spell.get().id()));
      // Seleciona sozinho a que vinha depois (ou a anterior, se era a ultima).
      List<SpellEntry> all = ClientArcaneData.spells();
      int at = -1;
      for (int i = 0; i < all.size(); i++) if (all.get(i).id().equals(spell.get().id())) at = i;
      selectedSpell = all.size() <= 1 || at < 0 ? null : all.get(at + 1 < all.size() ? at + 1 : at - 1).id();
      confirmDelete = false;
    }
    updateButtons();
  }

  @Override
  public boolean keyPressed(int key, int scan, int mods) {
    if (key == 257 || key == 335) { // Enter
      if (renameBox != null && renameBox.isFocused() && renameBox.visible) { rename(); return true; }
      if (folderBox != null && folderBox.isFocused() && folderBox.visible) {
        selectedSpellEntry().ifPresent(sp -> ModNetwork.CHANNEL.sendToServer(
            new C2SGrimoireOrgPacket(C2SGrimoireOrgPacket.FOLDER, sp.id(), 0, folderBox.getValue())));
        return true;
      }
      if (noteBox != null && noteBox.isFocused() && noteBox.visible) { addNote(); return true; }
    }
    return super.keyPressed(key, scan, mods);
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
        int row = (int) ((my - (top + 38)) / ROW);
        if (mx >= left + 14 && mx <= left + 124 && row >= 0 && row < SPELL_ROWS) {
          List<SpellEntry> spells = filtered();
          int index = spellPage * SPELL_ROWS + row;
          if (index < spells.size()) {
            SpellEntry hit = spells.get(index);
            if (mx >= left + 20 && mx < left + 32) {
              ModNetwork.CHANNEL.sendToServer(new C2SGrimoireOrgPacket(C2SGrimoireOrgPacket.FAVORITE, hit.id(), hit.favorite() ? 0 : 1, ""));
              return true;
            }
            selectedSpell = hit.id();
            if (!ClientArcaneData.active().map(a -> a.equals(hit.id())).orElse(false)) activate();
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
      case SPELLS -> turnPage(step);
      case JOURNAL -> logScroll = Math.max(0, Math.min(logScroll + step * LIST_ROWS, Math.max(0, ClientArcaneData.log().size() - 1)));
      case DISCOVERIES -> discoveryScroll = Math.max(0, Math.min(discoveryScroll + step, Math.max(0, ClientArcaneData.reactionCatalog().size() - 1)));
      case GLYPHS -> {
        int rows = (slotCount() + COLS - 1) / COLS;
        glyphScroll = Math.max(0, Math.min(glyphScroll + step, Math.max(0, rows - GRID_ROWS)));
      }
    }
    return true;
  }

  private int slotCount() {
    return Math.max(ClientArcaneData.totalGlyphs(), ClientArcaneData.glyphs().size());
  }

  private KnownGlyph glyphAt(double mx, double my) {
    List<KnownGlyph> glyphs = ClientArcaneData.glyphs();
    for (int i = 0; i < COLS * GRID_ROWS; i++) {
      int index = glyphScroll * COLS + i;
      if (index >= glyphs.size()) break;
      int x = left + 18 + (i % COLS) * CELL;
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
      case DISCOVERIES -> renderDiscoveries(g);
    }
    com.arcane.client.ClientStatus.render(g, font, left + W / 2, top + H + 6);
  }

  private static Component spellName(SpellEntry s) {
    return s.name().isEmpty() ? Component.translatable("gui.arcane.unnamed") : Component.literal(s.name());
  }

  private void renderSpells(GuiGraphics g, int mouseX, int mouseY) {
    updateButtons();
    List<SpellEntry> spells = filtered();
    if (ClientArcaneData.spells().isEmpty()) {
      g.drawWordWrap(font, Component.translatable("gui.arcane.grimoire.empty_spells"), left + 18, top + 40, 100, 0xB6AADC);
      return;
    }
    if (spells.isEmpty()) {
      g.drawWordWrap(font, Component.translatable("gui.arcane.search.none"), left + 18, top + 42, 100, 0xB6AADC);
    }
    Optional<UUID> active = ClientArcaneData.active();
    for (int r = 0; r < SPELL_ROWS; r++) {
      int index = spellPage * SPELL_ROWS + r;
      if (index >= spells.size()) break;
      SpellEntry s = spells.get(index);
      int y = top + 38 + r * ROW;
      boolean selected = s.id().equals(selectedSpell);
      if (selected) g.fill(left + 14, y - 1, left + 124, y + ROW - 1, 0x663C2A94);
      boolean isActive = active.map(a -> a.equals(s.id())).orElse(false);
      if (isActive) g.drawString(font, "▶", left + 15, y + 1, 0xC9A6FF, false);
      g.drawString(font, s.favorite() ? "★" : "☆", left + 23, y + 1, s.favorite() ? 0xFFD36B : 0x6A5F96, false);
      String name = font.plainSubstrByWidth(spellName(s).getString(), 70);
      g.drawString(font, name, left + 33, y + 1, selected ? 0xF2EAFF : 0xD2C8F0, false);
      if (s.cost() >= 0) {
        String cost = String.valueOf(s.cost());
        g.drawString(font, cost, left + 122 - font.width(cost), y + 1, 0x6FE0FF, false);
      }
    }
    String pageText = (spellPage + 1) + "/" + pageCount();
    g.drawString(font, pageText, left + 69 - font.width(pageText) / 2, top + 139, 0xB6AADC, false);

    Optional<SpellEntry> sel = selectedSpellEntry();
    if (sel.isEmpty()) {
      g.drawWordWrap(font, Component.translatable("gui.arcane.grimoire.pick_spell"), left + 134, top + 30, 100, 0xB6AADC);
      return;
    }
    SpellEntry spell = sel.get();
    Component stageName = Component.translatable("grimoire.arcane.stage." + ClientArcaneData.stage());
    g.drawString(font, Component.translatable("gui.arcane.grimoire.pages", spells.size(), ClientArcaneData.pages()),
        left + 134, top + 150, spells.size() >= ClientArcaneData.pages() ? 0xFF7A66 : 0xB6AADC, false);
    g.drawString(font, font.plainSubstrByWidth(Component.translatable("gui.arcane.grimoire.stage", stageName).getString(), 106),
        left + 134, top + 160, 0xC9A6FF, false);
    if (spell.cost() >= 0) {
      g.drawString(font, Component.translatable("gui.arcane.glyph_cost", spell.cost()), left + 134, top + 43, 0x6FE0FF, false);
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
    // Selo geometrico: cada glifo envolve o anterior (design). Unico por composicao.
    com.arcane.client.SealRenderer.draw(g, spell.glyphs().stream().map(GlyphEntry::id).toList(), left + 228, top + 151, 9);
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

    // Pagina esquerda: grade de glifos (conhecidos primeiro; o resto aparece como "?")
    KnownGlyph hovered = null;
    for (int i = 0; i < COLS * GRID_ROWS; i++) {
      int index = glyphScroll * COLS + i;
      if (index >= total) break;
      int x = left + 18 + (i % COLS) * CELL;
      int y = top + 28 + (i / COLS) * CELL;
      if (index < glyphs.size()) {
        KnownGlyph k = glyphs.get(index);
        g.blit(iconFor(k.id()), x, y, 0, 0, 16, 16, 16, 16);
        if (!k.usable()) {
          g.fill(x, y, x + 16, y + 16, 0x99000000);
          g.drawString(font, "?", x + 5, y + 4, 0xFFD860, false);
        }
        if (k.id().equals(selectedGlyph)) {
          g.renderOutline(x - 1, y - 1, 18, 18, 0xFF6FE0FF);
        }
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) hovered = k;
      } else {
        g.blit(iconFor(new ResourceLocation("arcane", "unknown")), x, y, 0, 0, 16, 16, 16, 16);
      }
    }
    int rows = (total + COLS - 1) / COLS;
    if (rows > GRID_ROWS) {
      g.drawString(font, (glyphScroll + 1) + "/" + (rows - GRID_ROWS + 1), left + 118 - font.width((glyphScroll + 1) + "/" + (rows - GRID_ROWS + 1)), top + 162, 0x9488BE, false);
    }

    // Pagina direita: detalhes do glifo escolhido (ou o que esta sob o mouse)
    KnownGlyph shown = hovered != null ? hovered : (selectedGlyph == null ? null : ClientArcaneData.glyph(selectedGlyph).orElse(null));
    int x = left + 138;
    if (shown == null) {
      g.drawWordWrap(font, Component.translatable("gui.arcane.grimoire.pick_glyph"), x, top + 30, 100, 0xB6AADC);
    } else {
      int y = top + 28;
      g.drawString(font, ArcaneWorkbenchScreen.glyphName(shown.id()).copy().withStyle(st -> st.withColor(ArcaneWorkbenchScreen.categoryColor(shown.category()))), x, y, 0xF2EAFF, false);
      y += 12;
      g.drawString(font, Component.translatable("category.arcane." + shown.category()), x, y, 0xB6AADC, false);
      y += 10;
      g.drawString(font, Component.translatable("knowledge.arcane." + knowledgeName(shown.level())), x, y, 0xC9A6FF, false);
      y += 10;
      g.drawString(font, Component.translatable("gui.arcane.glyph_cost", shown.cost()), x, y, 0x6FE0FF, false);
      y += 14;
      for (ClientArcaneData.ParamDef p : shown.params()) {
        g.drawString(font, font.plainSubstrByWidth(p.key() + ": " + trim(p.def()) + " (" + trim(p.min()) + "-" + trim(p.max()) + ")", 100), x, y, 0x6FE0FF, false);
        y += 10;
      }
      if (!shown.params().isEmpty()) y += 4;
      String descKey = "glyph." + shown.id().getNamespace() + "." + shown.id().getPath() + ".desc";
      Component desc = shown.usable() && I18n.exists(descKey) ? Component.translatable(descKey)
          : Component.translatable("gui.arcane.glyph_unknown_hint");
      drawLines(g, desc, x, y, 100, shown.usable() ? 0xD2C8F0 : 0xE8B84C, Math.max(1, (top + 158 - y) / 10));
    }
    Component discovered = Component.translatable("gui.arcane.grimoire.discovered", identified, total);
    g.drawString(font, discovered, left + 238 - font.width(discovered), top + 162, 0xB6AADC, false);

    if (hovered != null && selectedGlyph != null && !hovered.id().equals(selectedGlyph)) {
      // detalhe ja mostra o que esta sob o mouse
    }
  }

  private String ellipsize(String text, int width) {
    if (font.width(text) <= width) return text;
    return font.plainSubstrByWidth(text, width - font.width("...")) + "...";
  }

  /** Quebra o texto na largura dada e desenha no maximo maxLines linhas (para caber na pagina). */
  private void drawLines(GuiGraphics g, Component text, int x, int y, int width, int color, int maxLines) {
    List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, width);
    for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
      g.drawString(font, lines.get(i), x, y + i * 10, color, false);
    }
  }

  private void renderJournal(GuiGraphics g) {
    List<String> log = ClientArcaneData.log();
    if (log.isEmpty()) {
      g.drawWordWrap(font, Component.translatable("gui.arcane.grimoire.empty_journal"), left + 18, top + 30, 210, 0xB6AADC);
      return;
    }
    // Mais recentes primeiro; uma pagina do livro por coluna, para o texto nunca cruzar a lombada.
    for (int r = 0; r < LIST_ROWS * 2; r++) {
      int index = log.size() - 1 - (logScroll + r);
      if (index < 0) break;
      int x = left + (r < LIST_ROWS ? 18 : 138);
      int y = top + 28 + (r % LIST_ROWS) * ROW;
      g.drawString(font, ellipsize(describe(log.get(index)).getString(), 102), x, y, 0xD2C8F0, false);
    }
    if (log.size() > LIST_ROWS * 2) {
      g.drawString(font, Component.translatable("gui.arcane.grimoire.scroll_hint"), left + 18, top + 162, 0x9488BE, false);
    }
  }

  /** Aba Descobertas (Fase K): contador "X de Y", descobertas com nome, dica, dia e trecho; o resto como "???". */
  private void renderDiscoveries(GuiGraphics g) {
    var rows = com.arcane.client.DiscoveryView.rows(ClientArcaneData.reactionCatalog(), ClientArcaneData.reactionsFound());
    int known = com.arcane.client.DiscoveryView.known(rows);
    g.drawString(font, Component.translatable("gui.arcane.discoveries.count", known, rows.size()), left + 18, top + 26, 0xE6D9A8, false);
    if (known == 0) {
      g.drawWordWrap(font, Component.translatable("gui.arcane.discoveries.empty"), left + 18, top + 40, 102, 0xB6AADC);
    }
    // duas colunas (uma por pagina do livro), 4 linhas cada; a roda rola de uma em uma
    int perPage = 4;
    for (int r = 0; r < perPage * 2; r++) {
      int index = discoveryScroll + r;
      if (index >= rows.size()) break;
      var row = rows.get(index);
      int x = left + (r < perPage ? 18 : 138);
      int y = top + (known == 0 && r < perPage ? 92 : 40) + (r % perPage) * 32;
      if (known == 0 && r < perPage && y > top + 150) continue;
      if (!row.known()) {
        g.fill(x, y, x + 100, y + 26, 0x40221A33);
        g.drawString(font, Component.translatable("gui.arcane.discoveries.unknown"), x + 4, y + 9, 0x6E6488, false);
        continue;
      }
      g.drawString(font, ellipsize(Component.translatable(row.nameKey()).getString(), 100), x, y, 0xF0E2B0, false);
      g.drawString(font, ellipsize(Component.translatable(row.hintKey()).getString(), 100), x, y + 10, 0xB6AADC, false);
      StringBuilder glyphs = new StringBuilder();
      for (String id : row.excerpt()) {
        if (glyphs.length() > 0) glyphs.append(" · ");
        glyphs.append(ArcaneWorkbenchScreen.glyphName(new ResourceLocation("arcane", id)).getString());
      }
      String line = Component.translatable("gui.arcane.discoveries.day", row.day()).getString() + (glyphs.length() > 0 ? " · " + glyphs : "");
      g.drawString(font, ellipsize(line, 100), x, y + 20, 0x8A7FB0, false);
    }
    if (rows.size() > perPage * 2) {
      g.drawString(font, Component.translatable("gui.arcane.grimoire.scroll_hint"), left + 18, top + 172, 0x9488BE, false);
    }
  }

  /** "experiment:arcane:fire" -> "Experimento com Fogo". */
  static Component describe(String entry) {
    int colon = entry.indexOf(':');
    if (colon < 0) return Component.literal(entry);
    String kind = entry.substring(0, colon);
    String rest = entry.substring(colon + 1);
    if (kind.equals("note")) return Component.literal("✎ " + rest);
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
