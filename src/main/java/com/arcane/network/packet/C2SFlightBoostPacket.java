package com.arcane.network.packet;

import com.arcane.execution.FlightService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Nivel de alta velocidade do voo (0 = normal, 1 a 3 = cada vez mais rapido e caro). O servidor limita e cobra. */
public record C2SFlightBoostPacket(int level) {
  public static void encode(C2SFlightBoostPacket p, FriendlyByteBuf buf) { buf.writeVarInt(p.level); }

  public static C2SFlightBoostPacket decode(FriendlyByteBuf buf) { return new C2SFlightBoostPacket(buf.readVarInt()); }

  public static void handle(C2SFlightBoostPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player != null) FlightService.setBoost(player, p.level);
    });
    ctx.get().setPacketHandled(true);
  }
}
