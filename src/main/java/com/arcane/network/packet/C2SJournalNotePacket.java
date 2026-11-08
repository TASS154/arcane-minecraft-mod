package com.arcane.network.packet;

import com.arcane.grimoire.GrimoireAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Diario do grimorio -> servidor: o jogador escreve uma anotacao propria (ate 80 caracteres). */
public record C2SJournalNotePacket(String text) {
  public static final int MAX = 80;

  public static void encode(C2SJournalNotePacket p, FriendlyByteBuf buf) {
    buf.writeUtf(p.text, MAX);
  }

  public static C2SJournalNotePacket decode(FriendlyByteBuf buf) {
    return new C2SJournalNotePacket(buf.readUtf(MAX));
  }

  public static void handle(C2SJournalNotePacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;
      String text = p.text.replaceAll("\\p{Cntrl}", " ").trim();
      if (text.isEmpty()) return;
      GrimoireAttachments.get(player).ifPresent(g -> g.logDiscovery("note:" + text));
      com.arcane.network.ArcaneSync.sync(player, true);
    });
    ctx.get().setPacketHandled(true);
  }
}
