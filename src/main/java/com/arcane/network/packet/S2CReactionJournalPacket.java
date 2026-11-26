package com.arcane.network.packet;

import com.arcane.execution.ReactionJournal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Diario de descobertas (Fase K): o catalogo de reacoes carregadas e as que o jogador ja causou. */
public record S2CReactionJournalPacket(List<Def> catalog, List<ReactionJournal.Entry> found) {
  public record Def(String id, String nameKey, String hintKey) {}

  public static final int MAX = 256;

  public static void encode(S2CReactionJournalPacket p, FriendlyByteBuf buf) {
    int n = Math.min(MAX, p.catalog.size());
    buf.writeVarInt(n);
    for (int i = 0; i < n; i++) {
      var d = p.catalog.get(i);
      buf.writeUtf(d.id(), 64).writeUtf(d.nameKey(), 128).writeUtf(d.hintKey(), 128);
    }
    int m = Math.min(MAX, p.found.size());
    buf.writeVarInt(m);
    for (int i = 0; i < m; i++) {
      var e = p.found.get(i);
      buf.writeUtf(e.id(), 64);
      buf.writeLong(e.epochMillis());
      buf.writeVarLong(e.gameDay());
      buf.writeUtf(e.excerpt(), 256);
    }
  }

  public static S2CReactionJournalPacket decode(FriendlyByteBuf buf) {
    int n = Math.min(MAX, buf.readVarInt());
    List<Def> catalog = new ArrayList<>(n);
    for (int i = 0; i < n; i++) catalog.add(new Def(buf.readUtf(64), buf.readUtf(128), buf.readUtf(128)));
    int m = Math.min(MAX, buf.readVarInt());
    List<ReactionJournal.Entry> found = new ArrayList<>(m);
    for (int i = 0; i < m; i++) found.add(new ReactionJournal.Entry(buf.readUtf(64), buf.readLong(), buf.readVarLong(), buf.readUtf(256)));
    return new S2CReactionJournalPacket(catalog, found);
  }

  public static void handle(S2CReactionJournalPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CReactionJournalPacket p) {
    com.arcane.client.ClientArcaneData.setReactions(p.catalog(), p.found());
  }
}
