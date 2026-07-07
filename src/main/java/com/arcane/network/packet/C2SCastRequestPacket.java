package com.arcane.network.packet;

import com.arcane.execution.CastingService;
import com.arcane.execution.PrecisionSnapshot;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.inscription.Inscription;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public record C2SCastRequestPacket(UUID inscriptionId, boolean fromWorkbench) {
  public static void encode(C2SCastRequestPacket packet, FriendlyByteBuf buf) {
    buf.writeUUID(packet.inscriptionId);
    buf.writeBoolean(packet.fromWorkbench);
  }

  public static C2SCastRequestPacket decode(FriendlyByteBuf buf) {
    return new C2SCastRequestPacket(buf.readUUID(), buf.readBoolean());
  }

  public static void handle(C2SCastRequestPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;
      GrimoireAttachments.get(player).flatMap(g -> g.getInscription(packet.inscriptionId)).ifPresent(inscription -> {
        PrecisionSnapshot precision = packet.fromWorkbench ? PrecisionSnapshot.WORKBENCH : PrecisionSnapshot.FIELD;
        CastingService.tryCast(player, inscription, precision);
      });
    });
    ctx.get().setPacketHandled(true);
  }
}
