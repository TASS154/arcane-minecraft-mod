package com.arcane.client.hud;

import com.arcane.client.ArcaneTheme;
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

    long time = mc.level != null ? mc.level.getGameTime() : 0;
    float fraction = max <= 0 ? 0f : mana / max;
    boolean low = fraction < 0.2f;
    // vidro escuro atras da barra e do rotulo, e a barra em degrade azul para ciano (veneno: indigo palido)
    ArcaneTheme.panel(g, x - 15, y - 3, BAR_W + 17, 16, low && (time / 6) % 2 == 0 ? ArcaneTheme.DANGER : ArcaneTheme.EDGE, 0f);
    g.drawString(font, "\u2726", x - 11, y + 1, poisoned ? 0xFF8FA0FF : ArcaneTheme.EDGE_LIT, true);
    ArcaneTheme.bar(g, x + 1, y + 1, 100, 8, fraction, poisoned ? 0xFF2E3C99 : 0xFF3F5BFF, poisoned ? 0xFF8FA0FF : 0xFF7FE8FF, time);
    String value = (int) mana + "/" + (int) max;
    g.drawString(font, value, x + BAR_W / 2 - font.width(value) / 2, y + 1, 0xFFFFFF, true);

    // Magia ativa (tecla V)
    Component spell = ClientArcaneData.activeSpell()
        .map(s -> s.name().isEmpty() ? (Component) Component.translatable("gui.arcane.unnamed") : Component.literal(s.name()))
        .orElse(Component.translatable("hud.arcane.no_spell"));
    var activeEntry = ClientArcaneData.activeSpell();
    Component line = activeEntry.map(a -> (Component) spell.copy().append(Component.literal(" · " + Math.max(0, a.cost()) + " · ")
        .append(Component.translatable("gui.arcane.mode." + a.mode())))).orElse(spell);
    g.drawString(font, line, x, y - 10, activeEntry.isPresent() && activeEntry.get().cost() > mana ? 0xFF7070 : 0xD0B0FF, true);

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
