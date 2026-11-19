package com.arcane.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> clientes proximos: o compasso comecou a desenhar um circulo (so animacao; os tracos vem do servidor). */
public record S2CCompassDrawPacket(BlockPos altar, int radius, int duration, int ownerId) {
  public static void encode(S2CCompassDrawPacket p, FriendlyByteBuf buf) {
    buf.writeBlockPos(p.altar);
    buf.writeVarInt(p.radius);
    buf.writeVarInt(p.duration);
    buf.writeVarInt(p.ownerId);
  }

  public static S2CCompassDrawPacket decode(FriendlyByteBuf buf) {
    return new S2CCompassDrawPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
  }

  public static void handle(S2CCompassDrawPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> show(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void show(S2CCompassDrawPacket p) {
    com.arcane.client.render.CompassDrawFx.start(p.altar, p.radius, p.duration, p.ownerId);
  }
}
