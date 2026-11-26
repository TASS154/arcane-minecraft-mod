package com.arcane.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Nuvem de Vapor (Fase K): o jogador esta dentro por mais {@code ticks} ticks (o cliente desenha o veu). */
public record S2CVaporPacket(int ticks) {
  public static void encode(S2CVaporPacket p, FriendlyByteBuf buf) { buf.writeVarInt(p.ticks); }

  public static S2CVaporPacket decode(FriendlyByteBuf buf) { return new S2CVaporPacket(Math.min(200, buf.readVarInt())); }

  public static void handle(S2CVaporPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CVaporPacket p) {
    com.arcane.client.hud.VaporOverlay.inside(p.ticks());
  }
}
