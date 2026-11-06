package com.arcane.network.packet;

import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.network.ArcaneSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Grimorio -> servidor: apaga uma magia salva. So afeta o grimorio de quem enviou. */
public record C2SDeleteInscriptionPacket(UUID inscriptionId) {
  public static void encode(C2SDeleteInscriptionPacket packet, FriendlyByteBuf buf) {
    buf.writeUUID(packet.inscriptionId);
  }

  public static C2SDeleteInscriptionPacket decode(FriendlyByteBuf buf) {
    return new C2SDeleteInscriptionPacket(buf.readUUID());
  }

  public static void handle(C2SDeleteInscriptionPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;
      GrimoireAttachments.get(player).ifPresent(grimoire -> {
        if (grimoire.removeInscription(packet.inscriptionId)) {
          ArcaneSync.sync(player, true);
        }
      });
    });
    ctx.get().setPacketHandled(true);
  }
}
