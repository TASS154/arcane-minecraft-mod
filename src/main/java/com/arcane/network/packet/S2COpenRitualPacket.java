package com.arcane.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Altar -> cliente: abre a tela do ritual (desenhar a magia e fazer as oferendas) para este altar. */
public record S2COpenRitualPacket(BlockPos altar) {
  public static void encode(S2COpenRitualPacket p, FriendlyByteBuf buf) { buf.writeBlockPos(p.altar); }

  public static S2COpenRitualPacket decode(FriendlyByteBuf buf) { return new S2COpenRitualPacket(buf.readBlockPos()); }

  public static void handle(S2COpenRitualPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> open(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void open(S2COpenRitualPacket p) {
    com.arcane.client.ClientHooks.openRitual(p.altar);
  }
}
