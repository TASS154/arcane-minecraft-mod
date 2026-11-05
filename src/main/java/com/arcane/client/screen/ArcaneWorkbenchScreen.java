package com.arcane.client.screen;

import com.arcane.client.ClientArcaneData;
import com.arcane.client.ClientArcaneData.KnownGlyph;
import com.arcane.client.ClientSetup;
import com.arcane.menu.ArcaneWorkbenchMenu;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.C2SSaveInscriptionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bancada Arcana: lista apenas os glifos que o jogador conhece, monta a inscricao em ordem,
 * mostra custo e uma previa de validade. Salvar e conjurar sao sempre decididos pelo servidor.
 *
 * Layout (bate com tools/gen_textures.py -> textures/gui/arcane_workbench.png, 240x222):
 *   glifos conhecidos (8,30)-(113,163) | composicao (120,30)-(231,135) | custo (120,140)-(231,163)
 *   botoes de edicao em y=168, seletor de magia ativa em y=196.
 */
public class ArcaneWorkbenchScreen extends AbstractContainerScreen<ArcaneWorkbenchMenu> {
  private static final ResourceLocation TEXTURE = new ResourceLocation("arcane", "textures/gui/arcane_workbench.png");
  private static final int COLS = 5;
  private static final int ROWS = 6;
  private static final int SLOT = 20;
  private static final int GRID_X = 11;
  private static final int GRID_Y = 33;
  private static final int LIST_X = 123;
  private static final int LIST_Y = 33;
  private static final int LIST_ROW = 12;
  private static final int LIST_ROWS = 8;
  private static final int MAX_GLYPHS = 24;

  private final List<ResourceLocation> composition = new ArrayList<>();
  private EditBox nameField;
  private int gridScroll;
  private int listScroll;
  private int selected = -1;

  public ArcaneWorkbenchScreen(ArcaneWorkbenchMenu menu, Inventory inv, Component title) {
    super(menu, inv, title);
    this.imageWidth = 240;
    this.imageHeight = 222;
  }

  @Override
  protected void init() {
    super.init();
    nameField = new EditBox(font, leftPos + 120, topPos + 9, 111, 16, Component.translatable("gui.arcane.name"));
    nameField.setMaxLength(32);
    nameField.setHint(Component.translatable("gui.arcane.name").withStyle(ChatFormatting.DARK_GRAY));
    addRenderableWidget(nameField);

    addRenderableWidget(Button.builder(Component.literal("↑"), b -> move(-1))
        .bounds(leftPos + 8, topPos + 168, 18, 20).tooltip(tip("gui.arcane.move_up")).build());
    addRenderableWidget(Button.builder(Component.literal("↓"), b -> move(1))
        .bounds(leftPos + 28, topPos + 168, 18, 20).tooltip(tip("gui.arcane.move_down")).build());
    addRenderableWidget(Button.builder(Component.literal("✕"), b -> removeSelected())
        .bounds(leftPos + 48, topPos + 168, 18, 20).tooltip(tip("gui.arcane.remove")).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.clear"), b -> {
      composition.clear();
      selected = -1;
      listScroll = 0;
    }).bounds(leftPos + 68, topPos + 168, 45, 20).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.save"), b -> save())
        .bounds(leftPos + 120, topPos + 168, 54, 20).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.cast"), b -> ClientSetup.tryCast(true))
        .bounds(leftPos + 177, topPos + 168, 54, 20).tooltip(tip("gui.arcane.cast.tooltip")).build());

    addRenderableWidget(Button.builder(Component.literal("◀"), b -> ClientSetup.cycleSpell(-1))
        .bounds(leftPos + 8, topPos + 196, 18, 20).build());
    addRenderableWidget(Button.builder(Component.literal("▶"), b -> ClientSetup.cycleSpell(1))
        .bounds(leftPos + 214, topPos + 196, 18, 20).build());
  }

  private static net.minecraft.client.gui.components.Tooltip tip(String key) {
    return net.minecraft.client.gui.components.Tooltip.create(Component.translatable(key));
  }

  // ------------------------------------------------------------------ acoes
  private void save() {
    List<String> ids = composition.stream().map(ResourceLocation::toString).toList();
    ModNetwork.CHANNEL.sendToServer(new C2SSaveInscriptionPacket(nameField.getValue(), ids));
  }

  private void move(int dir) {
    if (selected < 0 || selected >= composition.size()) return;
    int target = selected + dir;
    if (target < 0 || target >= composition.size()) return;
    ResourceLocation g = composition.remove(selected);
    composition.add(target, g);
    selected = target;
    ensureVisible();
  }

  private void removeSelected() {
    if (selected < 0 || selected >= composition.size()) return;
    composition.remove(selected);
    selected = Math.min(selected, composition.size() - 1);
    listScroll = Math.max(0, Math.min(listScroll, composition.size() - LIST_ROWS));
  }

  private void ensureVisible() {
    if (selected < listScroll) listScroll = selected;
    if (selected >= listScroll + LIST_ROWS) listScroll = selected - LIST_ROWS + 1;
  }

  // ------------------------------------------------------------------ entrada
  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button == 0) {
      KnownGlyph glyph = glyphAt(mouseX, mouseY);
      if (glyph != null && composition.size() < MAX_GLYPHS) {
        composition.add(glyph.id());
        selected = composition.size() - 1;
        ensureVisible();
        return true;
      }
      int row = listRowAt(mouseX, mouseY);
      if (row >= 0) {
        selected = row;
        return true;
      }
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    int step = delta > 0 ? -1 : 1;
    if (inside(mouseX, mouseY, 8, 30, 113, 163)) {
      int rows = (ClientArcaneData.glyphs().size() + COLS - 1) / COLS;
      gridScroll = Math.max(0, Math.min(gridScroll + step, Math.max(0, rows - ROWS)));
      return true;
    }
    if (inside(mouseX, mouseY, 120, 30, 231, 135)) {
      listScroll = Math.max(0, Math.min(listScroll + step, Math.max(0, composition.size() - LIST_ROWS)));
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) {
      onClose();
      return true;
    }
    if (nameField.isFocused()) {
      return nameField.keyPressed(keyCode, scanCode, modifiers) || nameField.canConsumeInput() || super.keyPressed(keyCode, scanCode, modifiers);
    }
    if (keyCode == 261 || keyCode == 259) { // DELETE / BACKSPACE
      removeSelected();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  private boolean inside(double mx, double my, int x0, int y0, int x1, int y1) {
    return mx >= leftPos + x0 && mx <= leftPos + x1 && my >= topPos + y0 && my <= topPos + y1;
  }

  private KnownGlyph glyphAt(double mx, double my) {
    List<KnownGlyph> glyphs = ClientArcaneData.glyphs();
    for (int i = 0; i < COLS * ROWS; i++) {
      int index = gridScroll * COLS + i;
      if (index >= glyphs.size()) break;
      int x = leftPos + GRID_X + (i % COLS) * SLOT;
      int y = topPos + GRID_Y + (i / COLS) * SLOT;
      if (mx >= x && mx < x + 18 && my >= y && my < y + 18) return glyphs.get(index);
    }
    return null;
  }

  private int listRowAt(double mx, double my) {
    if (!inside(mx, my, 121, 31, 230, 134)) return -1;
    int row = (int) ((my - topPos - LIST_Y) / LIST_ROW) + listScroll;
    return row >= 0 && row < composition.size() && row < listScroll + LIST_ROWS ? row : -1;
  }

  // ------------------------------------------------------------------ desenho
  @Override
  protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
    g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
  }

  @Override
  protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    g.drawString(font, title, 10, 13, 0xE8D8FF, false);
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    renderBackground(g);
    super.render(g, mouseX, mouseY, partialTick);
    renderGlyphGrid(g, mouseX, mouseY);
    renderComposition(g, mouseX, mouseY);
    renderSummary(g);
    renderActiveSpell(g);
    renderGlyphTooltip(g, mouseX, mouseY);
  }

  private void renderGlyphGrid(GuiGraphics g, int mouseX, int mouseY) {
    List<KnownGlyph> glyphs = ClientArcaneData.glyphs();
    if (glyphs.isEmpty()) {
      g.drawWordWrap(font, Component.translatable("gui.arcane.no_glyphs"), leftPos + 12, topPos + 36, 96, 0x8070A0);
      return;
    }
    for (int i = 0; i < COLS * ROWS; i++) {
      int index = gridScroll * COLS + i;
      if (index >= glyphs.size()) break;
      KnownGlyph glyph = glyphs.get(index);
      int x = leftPos + GRID_X + (i % COLS) * SLOT;
      int y = topPos + GRID_Y + (i / COLS) * SLOT;
      boolean hover = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
      g.blit(TEXTURE, x, y, hover ? 20 : 0, 230, 18, 18, 256, 256);
      g.blit(iconFor(glyph.id()), x + 1, y + 1, 0, 0, 16, 16, 16, 16);
      if (!glyph.usable()) {
        g.fill(x + 1, y + 1, x + 17, y + 17, 0x88000000);
        g.drawString(font, "?", x + 7, y + 5, 0xFFD860, false);
      }
    }
    int rows = (glyphs.size() + COLS - 1) / COLS;
    if (rows > ROWS) {
      String page = (gridScroll + 1) + "/" + (rows - ROWS + 1);
      g.drawString(font, page, leftPos + 110 - font.width(page), topPos + 154, 0x6A5A8A, false);
    }
  }

  private void renderComposition(GuiGraphics g, int mouseX, int mouseY) {
    if (composition.isEmpty()) {
      g.drawWordWrap(font, Component.translatable("gui.arcane.compose_hint"), leftPos + 124, topPos + 36, 104, 0x8070A0);
      return;
    }
    for (int r = 0; r < LIST_ROWS; r++) {
      int index = listScroll + r;
      if (index >= composition.size()) break;
      ResourceLocation id = composition.get(index);
      int y = topPos + LIST_Y + r * LIST_ROW;
      if (index == selected) {
        g.fill(leftPos + 121, y - 1, leftPos + 230, y + LIST_ROW - 1, 0x665ADCF0);
      } else if (listRowAt(mouseX, mouseY) == index) {
        g.fill(leftPos + 121, y - 1, leftPos + 230, y + LIST_ROW - 1, 0x33FFFFFF);
      }
      g.drawString(font, (index + 1) + ".", leftPos + LIST_X, y + 1, 0x8F80B0, false);
      g.blit(iconFor(id), leftPos + LIST_X + 14, y, 10, 10, 0, 0, 16, 16, 16, 16);
      Optional<KnownGlyph> known = ClientArcaneData.glyph(id);
      int color = known.map(k -> k.usable() ? categoryColor(k.category()) : 0xFFD860).orElse(0xFF6060);
      String name = font.plainSubstrByWidth(glyphName(id).getString(), 80);
      g.drawString(font, name, leftPos + LIST_X + 27, y + 1, color, false);
    }
    if (composition.size() > LIST_ROWS) {
      String more = (listScroll + 1) + "-" + Math.min(composition.size(), listScroll + LIST_ROWS) + "/" + composition.size();
      g.drawString(font, more, leftPos + 228 - font.width(more), topPos + 126, 0x6A5A8A, false);
    }
  }

  private void renderSummary(GuiGraphics g) {
    int cost = 0;
    boolean experimental = false;
    for (ResourceLocation id : composition) {
      Optional<KnownGlyph> k = ClientArcaneData.glyph(id);
      cost += k.map(KnownGlyph::cost).orElse(0);
      if (k.isEmpty() || !k.get().usable()) experimental = true;
    }
    g.drawString(font, Component.translatable("gui.arcane.cost", cost, (int) ClientArcaneData.mana()),
        leftPos + 124, topPos + 143, 0x5ADCF0, false);

    Component status;
    int color;
    if (composition.isEmpty()) {
      status = Component.translatable("message.arcane.inscription.empty");
      color = 0x8070A0;
    } else if (!ClientArcaneData.glyph(composition.get(0)).map(k -> "form".equals(k.category())).orElse(false)) {
      status = Component.translatable("message.arcane.inscription.form_not_first");
      color = 0xFF6060;
    } else if (experimental) {
      status = Component.translatable("gui.arcane.experiment");
      color = 0xFFD860;
    } else {
      status = Component.translatable("gui.arcane.ready");
      color = 0x80E080;
    }
    g.drawString(font, font.plainSubstrByWidth(status.getString(), 106), leftPos + 124, topPos + 153, color, false);
  }

  private void renderActiveSpell(GuiGraphics g) {
    Component label = ClientArcaneData.activeSpell()
        .map(s -> s.name().isEmpty() ? (Component) Component.translatable("gui.arcane.unnamed") : Component.literal(s.name()))
        .orElse(Component.translatable("hud.arcane.no_spell"));
    Component line = Component.translatable("gui.arcane.active", label);
    String text = font.plainSubstrByWidth(line.getString(), 180);
    g.drawString(font, text, leftPos + 120 - font.width(text) / 2, topPos + 202, 0xD0B0FF, false);
  }

  private void renderGlyphTooltip(GuiGraphics g, int mouseX, int mouseY) {
    KnownGlyph glyph = glyphAt(mouseX, mouseY);
    if (glyph == null) return;
    List<Component> lines = new ArrayList<>();
    lines.add(glyphName(glyph.id()).copy().withStyle(s -> s.withColor(categoryColor(glyph.category()))));
    lines.add(Component.translatable("category.arcane." + glyph.category()).withStyle(ChatFormatting.GRAY));
    lines.add(Component.translatable("gui.arcane.glyph_cost", glyph.cost()).withStyle(ChatFormatting.AQUA));
    lines.add(Component.translatable("knowledge.arcane." + knowledgeName(glyph.level())).withStyle(ChatFormatting.DARK_PURPLE));
    String descKey = "glyph." + glyph.id().getNamespace() + "." + glyph.id().getPath() + ".desc";
    if (glyph.usable() && I18n.exists(descKey)) {
      lines.add(Component.translatable(descKey).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    } else if (!glyph.usable()) {
      lines.add(Component.translatable("gui.arcane.glyph_unknown_hint").withStyle(ChatFormatting.YELLOW));
    }
    g.renderComponentTooltip(font, lines, mouseX, mouseY);
  }

  // ------------------------------------------------------------------ util
  private static ResourceLocation iconFor(ResourceLocation glyph) {
    return new ResourceLocation(glyph.getNamespace(), "textures/gui/glyphs/" + glyph.getPath() + ".png");
  }

  static Component glyphName(ResourceLocation id) {
    String key = "glyph." + id.getNamespace() + "." + id.getPath();
    return I18n.exists(key) ? Component.translatable(key) : Component.literal(id.getPath());
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

  static int categoryColor(String category) {
    return switch (category) {
      case "form" -> 0x5ADCF0;
      case "effect" -> 0xFA8C46;
      case "filter" -> 0x8CDC6E;
      case "modifier" -> 0xF0C850;
      case "condition" -> 0xCDA5F5;
      case "activator" -> 0xF0F5FF;
      default -> 0xFFFFFF;
    };
  }
}
