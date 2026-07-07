package com.arcane.client.screen;

import com.arcane.inscription.GlyphInstance;
import com.arcane.menu.ArcaneWorkbenchMenu;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.C2SSaveInscriptionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class ArcaneWorkbenchScreen extends AbstractContainerScreen<ArcaneWorkbenchMenu> {
  private static final ResourceLocation TEXTURE = new ResourceLocation("arcane", "textures/gui/arcane_workbench.png");
  private EditBox nameField;

  public ArcaneWorkbenchScreen(ArcaneWorkbenchMenu menu, Inventory inv, Component title) {
    super(menu, inv, title);
    this.imageWidth = 176;
    this.imageHeight = 166;
  }

  @Override
  protected void init() {
    super.init();
    nameField = new EditBox(font, leftPos + 8, topPos + 18, 160, 16, Component.literal("name"));
    nameField.setMaxLength(32);
    addRenderableWidget(nameField);

    addRenderableWidget(Button.builder(Component.literal("+ Self"), b -> menu.addGlyph(new GlyphInstance(new ResourceLocation("arcane", "self"))))
        .bounds(leftPos + 8, topPos + 40, 50, 20).build());
    addRenderableWidget(Button.builder(Component.literal("+ Projectile"), b -> menu.addGlyph(new GlyphInstance(new ResourceLocation("arcane", "projectile"))))
        .bounds(leftPos + 62, topPos + 40, 60, 20).build());
    addRenderableWidget(Button.builder(Component.literal("+ Damage"), b -> menu.addGlyph(new GlyphInstance(new ResourceLocation("arcane", "damage"), java.util.Map.of("amount", 5.0))))
        .bounds(leftPos + 8, topPos + 64, 60, 20).build());
    addRenderableWidget(Button.builder(Component.literal("+ Fire"), b -> menu.addGlyph(new GlyphInstance(new ResourceLocation("arcane", "fire"))))
        .bounds(leftPos + 72, topPos + 64, 50, 20).build());
    addRenderableWidget(Button.builder(Component.literal("Clear"), b -> menu.clearGlyphs())
        .bounds(leftPos + 8, topPos + 88, 50, 20).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.arcane.save"), b -> {
      List<String> ids = menu.building().stream().map(g -> g.glyphId().toString()).toList();
      ModNetwork.CHANNEL.sendToServer(new C2SSaveInscriptionPacket(nameField.getValue(), ids));
    }).bounds(leftPos + 62, topPos + 88, 60, 20).build());
  }

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xCC1A0A2E);
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    renderBackground(graphics);
    super.render(graphics, mouseX, mouseY, partialTick);
    graphics.drawString(font, Component.translatable("gui.arcane.workbench"), leftPos + 8, topPos + 6, 0xFFFFFF, false);
    int y = topPos + 112;
    for (GlyphInstance glyph : menu.building()) {
      graphics.drawString(font, glyph.glyphId().toString(), leftPos + 8, y, 0xAAAAFF, false);
      y += 10;
    }
  }
}
