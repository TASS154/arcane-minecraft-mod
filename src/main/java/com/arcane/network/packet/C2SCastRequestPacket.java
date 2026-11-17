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

public record C2SCastRequestPacket(UUID inscriptionId, boolean fromWorkbench, boolean release) {
  public C2SCastRequestPacket(UUID inscriptionId, boolean fromWorkbench) { this(inscriptionId, fromWorkbench, false); }

  public static void encode(C2SCastRequestPacket packet, FriendlyByteBuf buf) {
    buf.writeUUID(packet.inscriptionId);
    buf.writeBoolean(packet.fromWorkbench);
    buf.writeBoolean(packet.release);
  }

  public static C2SCastRequestPacket decode(FriendlyByteBuf buf) {
    return new C2SCastRequestPacket(buf.readUUID(), buf.readBoolean(), buf.readBoolean());
  }

  public static void handle(C2SCastRequestPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;
      if (packet.release) {
        com.arcane.execution.CastChannels.release(player);
      } else {
        // O modo (toque, carregar, segurar, alternar) e a potencia (so a Bancada da 100%) sao decididos no servidor.
        com.arcane.execution.CastChannels.press(player, packet.inscriptionId);
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
