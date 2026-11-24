package com.arcane.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Avisa o cliente que uma criatura esta (ou nao) sob a invisibilidade arcana (a pocao vanilla nao manda isto). */
public record S2CVeilPacket(int entityId, boolean on) {
  public static void encode(S2CVeilPacket p, FriendlyByteBuf buf) {
    buf.writeVarInt(p.entityId).writeBoolean(p.on);
  }

  public static S2CVeilPacket decode(FriendlyByteBuf buf) {
    return new S2CVeilPacket(buf.readVarInt(), buf.readBoolean());
  }

  public static void handle(S2CVeilPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CVeilPacket p) {
    com.arcane.client.render.InvisibilityRender.setVeiled(p.entityId(), p.on());
  }
}
