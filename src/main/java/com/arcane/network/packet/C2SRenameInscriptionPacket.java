package com.arcane.network.packet;

import com.arcane.grimoire.GrimoireAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Grimorio -> servidor: renomeia uma magia salva (de qualquer lugar; nao precisa da Bancada). */
public record C2SRenameInscriptionPacket(UUID inscriptionId, String name) {
  public static void encode(C2SRenameInscriptionPacket p, FriendlyByteBuf buf) {
    buf.writeUUID(p.inscriptionId);
    buf.writeUtf(p.name, 64);
  }

  public static C2SRenameInscriptionPacket decode(FriendlyByteBuf buf) {
    return new C2SRenameInscriptionPacket(buf.readUUID(), buf.readUtf(64));
  }

  public static void handle(C2SRenameInscriptionPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;
      String name = p.name.replaceAll("\\p{Cntrl}", " ").trim();
      if (name.length() > 32) name = name.substring(0, 32);
      final String finalName = name;
      GrimoireAttachments.get(player).ifPresent(g -> g.getInscription(p.inscriptionId).ifPresent(old -> {
        var m = old.metadata();
        g.spellbook().put(old.id(), old.withMetadata(new com.arcane.inscription.InscriptionMetadata(
            finalName, m.ink(), m.pen(), m.precision(), m.author(), m.created(), m.sourceKind())));
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.arcane.grimoire.renamed",
            finalName.isEmpty() ? net.minecraft.network.chat.Component.translatable("gui.arcane.unnamed") : net.minecraft.network.chat.Component.literal(finalName))
            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
        com.arcane.network.ArcaneSync.sync(player, true);
      }));
    });
    ctx.get().setPacketHandled(true);
  }
}
