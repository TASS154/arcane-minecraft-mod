package com.arcane.client;

import com.arcane.ArcaneMod;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ultima mensagem do servidor (a da barra de acao, como "Magia salva"). Com uma tela aberta a barra de acao
 * fica escondida, entao as telas do mod mostram esta linha por alguns segundos.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public final class ClientStatus {
  private static final long LIFETIME_MS = 5000L;
  private static Component message;
  private static long since;

  private ClientStatus() {}

  @SubscribeEvent
  public static void onSystem(ClientChatReceivedEvent.System event) {
    if (event.isOverlay()) set(event.getMessage());
  }

  public static void set(Component text) {
    message = text;
    since = System.currentTimeMillis();
  }

  public static boolean active() {
    return message != null && System.currentTimeMillis() - since < LIFETIME_MS;
  }

  /** Desenha a mensagem centralizada em torno de centerX, na altura y (esmaece no fim). */
  public static void render(GuiGraphics g, Font font, int centerX, int y) {
    if (!active()) return;
    long age = System.currentTimeMillis() - since;
    int alpha = age > LIFETIME_MS - 800 ? (int) (255 * (LIFETIME_MS - age) / 800.0) : 255;
    if (alpha < 12) return;
    int w = font.width(message);
    int x = centerX - w / 2;
    g.fill(x - 4, y - 3, x + w + 4, y + 11, ((alpha * 170 / 255) << 24));
    g.drawString(font, message, x, y, (alpha << 24) | 0xFFFFFF, false);
  }
}
