package com.arcane.client.hud;

import com.arcane.client.ClientArcaneData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Barra de mana, magia ativa e avisos de zona (amplificacao do cristal e Mana Poisoning).
 * Fica a direita da hotbar; em telas estreitas vai para o canto superior esquerdo.
 */
public class ManaHudOverlay implements IGuiOverlay {
  private static final ResourceLocation TEXTURE = new ResourceLocation("arcane", "textures/gui/mana_bar.png");
  private static final int BAR_W = 102;

  @Override
  public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.options.hideGui || mc.player == null || mc.player.isSpectator() || !ClientArcaneData.hasData()) return;
    Font font = mc.font;

    int x = width / 2 + 91 + 18;
    int y = height - 14;
    if (x + BAR_W + 2 > width) {
      x = 16;
      y = 6;
    }

    float max = ClientArcaneData.maxMana();
    float mana = Math.min(ClientArcaneData.mana(), max);
    boolean poisoned = ClientArcaneData.poison() > 0;

    g.blit(TEXTURE, x - 12, y, 104, 0, 9, 9, 128, 32);
    g.blit(TEXTURE, x, y, 0, 0, BAR_W, 10, 128, 32);
    int fill = Math.round(100 * (mana / max));
    if (fill > 0) {
      g.blit(TEXTURE, x + 1, y + 1, 0, poisoned ? 20 : 10, fill, 8, 128, 32);
    }
    String value = (int) mana + "/" + (int) max;
    g.drawString(font, value, x + BAR_W / 2 - font.width(value) / 2, y + 1, 0xFFFFFF, true);

    // Magia ativa (tecla V)
    Component spell = ClientArcaneData.activeSpell()
        .map(s -> s.name().isEmpty() ? (Component) Component.translatable("gui.arcane.unnamed") : Component.literal(s.name()))
        .orElse(Component.translatable("hud.arcane.no_spell"));
    g.drawString(font, spell, x, y - 10, 0xD0B0FF, true);

    int infoY = y - 20;
    if (!ClientArcaneData.grimoirePresent()) {
      g.drawString(font, Component.translatable("hud.arcane.no_grimoire"), x, infoY, 0xFF6060, true);
      infoY -= 10;
    }
    double potency = ClientArcaneData.potency();
    if (potency > 1.01) {
      g.drawString(font, Component.translatable("hud.arcane.zone", String.format("%.0f", potency)), x, infoY, 0x5ADCF0, true);
      infoY -= 10;
    }
    if (poisoned) {
      long t = mc.level != null ? mc.level.getGameTime() : 0;
      int color = (t / 10) % 2 == 0 ? 0x3C6EFF : 0x8FB0FF;
      g.drawString(font, Component.translatable("hud.arcane.poisoning"), x, infoY, color, true);
    }
  }
}
