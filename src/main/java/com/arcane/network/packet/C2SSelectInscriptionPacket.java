package com.arcane.network.packet;

import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.network.ArcaneSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Define a inscricao ativa (tecla V). O servidor so aceita ids que existem no grimorio do jogador. */
public record C2SSelectInscriptionPacket(UUID inscriptionId) {
  public static void encode(C2SSelectInscriptionPacket packet, FriendlyByteBuf buf) {
    buf.writeUUID(packet.inscriptionId);
  }

  public static C2SSelectInscriptionPacket decode(FriendlyByteBuf buf) {
    return new C2SSelectInscriptionPacket(buf.readUUID());
  }

  public static void handle(C2SSelectInscriptionPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;
      GrimoireAttachments.get(player).ifPresent(grimoire -> {
        if (grimoire.getInscription(packet.inscriptionId).isPresent()) {
          grimoire.setActiveInscription(packet.inscriptionId);
          ArcaneSync.sync(player, true);
        }
      });
    });
    ctx.get().setPacketHandled(true);
  }
}
