package com.arcane.client;

import com.arcane.client.ClientArcaneData.SpellEntry;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.C2SCastRequestPacket;
import com.arcane.network.packet.C2SGrimoireOrgPacket;
import com.arcane.network.packet.C2SSelectInscriptionPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Entrada do conjurador: tecla de conjurar (pressionar e soltar, para os modos carregar e segurar), roda radial,
 * troca de conjunto e teclas 1 a 9. Quem decide o que acontece e o servidor; aqui so se avisa "pressionou" e "soltou".
 */
public final class ClientCasting {
  private static boolean castDown;
  private static boolean latched;
  private static long castStart;
  private static boolean radialDown;

  private ClientCasting() {}

  /** Progresso de carga (0 a 1) para o HUD, ou -1 se nao esta carregando. */
  public static double chargeProgress() {
    if (!castDown && !latched) return -1;
    Minecraft mc = Minecraft.getInstance();
    var active = ClientArcaneData.activeSpell();
    if (active.isEmpty() || active.get().mode() != 1 || mc.level == null) return -1;
    return Math.min(1.0, (mc.level.getGameTime() - castStart) / 60.0);
  }

  /** Magias que a roda e as teclas numericas oferecem: conjunto ativo, senao favoritas, senao todas. */
  public static List<SpellEntry> pickable() {
    List<SpellEntry> set = ClientArcaneData.activeLoadoutSpells();
    if (!set.isEmpty()) return set;
    List<SpellEntry> all = ClientArcaneData.spells();
    List<SpellEntry> fav = all.stream().filter(SpellEntry::favorite).toList();
    List<SpellEntry> base = fav.isEmpty() ? all : fav;
    return new ArrayList<>(base.stream().sorted(Comparator.comparingLong(SpellEntry::lastUsed).reversed()).limit(12).toList());
  }

  public static void select(SpellEntry entry) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.getConnection() == null) return;
    ModNetwork.CHANNEL.sendToServer(new C2SSelectInscriptionPacket(entry.id()));
    Component name = entry.name().isEmpty() ? Component.translatable("gui.arcane.unnamed") : Component.literal(entry.name());
    mc.player.displayClientMessage(Component.translatable("hud.arcane.selected", name).withStyle(ChatFormatting.LIGHT_PURPLE), true);
  }

  public static void switchLoadout(int direction) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.getConnection() == null) return;
    int next = Math.floorMod(ClientArcaneData.activeLoadout() + direction, com.arcane.grimoire.SpellPrefs.LOADOUTS);
    ModNetwork.CHANNEL.sendToServer(new C2SGrimoireOrgPacket(C2SGrimoireOrgPacket.LOADOUT_SWITCH, new UUID(0, 0), next, ""));
    mc.player.displayClientMessage(Component.translatable("hud.arcane.loadout", next + 1).withStyle(ChatFormatting.LIGHT_PURPLE), true);
  }

  private static void sendCast(boolean release) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.getConnection() == null) return;
    var active = ClientArcaneData.active();
    if (active.isEmpty()) {
      if (!release) mc.player.displayClientMessage(Component.translatable("hud.arcane.no_spell").withStyle(ChatFormatting.GRAY), true);
      return;
    }
    ModNetwork.CHANNEL.sendToServer(new C2SCastRequestPacket(active.get(), false, release));
  }

  private static boolean channeled() {
    return ClientArcaneData.activeSpell().map(s -> s.mode() == 1 || s.mode() == 2).orElse(false);
  }

  /** Chamado todo tick de cliente (inicio do tick, antes de o jogo tratar as teclas da hotbar). */
  public static void tickStart() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.screen != null) return;
    if (ClientPrefs.mode() == ClientPrefs.UseMode.CASTER) {
      for (int i = 0; i < 9; i++) {
        while (mc.options.keyHotbarSlots[i].consumeClick()) {
          List<SpellEntry> list = pickable();
          if (i < list.size()) select(list.get(i));
          else mc.player.displayClientMessage(Component.translatable("hud.arcane.slot_empty", i + 1).withStyle(ChatFormatting.GRAY), true);
        }
      }
    }
  }

  public static void tick() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null) return;
    boolean latch = ClientPrefs.latch();

    // ---- conjurar
    boolean down = ClientSetup.CAST_KEY != null && ClientSetup.CAST_KEY.isDown() && mc.screen == null;
    if (ClientSetup.CAST_KEY != null) while (ClientSetup.CAST_KEY.consumeClick()) { /* estado vem de isDown */ }
    if (latch && channeled()) {
      if (down && !castDown) {
        if (latched) { latched = false; sendCast(true); }
        else { latched = true; castStart = mc.level != null ? mc.level.getGameTime() : 0; sendCast(false); }
      }
    } else {
      if (latched) { latched = false; sendCast(true); }
      if (down && !castDown) { castStart = mc.level != null ? mc.level.getGameTime() : 0; sendCast(false); }
      if (!down && castDown) sendCast(true);
    }
    castDown = down;

    // ---- roda radial / conjunto
    if (ClientSetup.RADIAL_KEY != null) {
      boolean r = ClientSetup.RADIAL_KEY.isDown() && mc.screen == null;
      if (r && !radialDown && ClientPrefs.mode() == ClientPrefs.UseMode.RADIAL && !pickable().isEmpty()) {
        mc.setScreen(new com.arcane.client.screen.RadialScreen());
      }
      radialDown = r;
      while (ClientSetup.RADIAL_KEY.consumeClick()) {
        if (ClientPrefs.mode() == ClientPrefs.UseMode.LOADOUT && mc.screen == null) switchLoadout(1);
      }
    }
  }
}
