package com.arcane.network.packet;

import com.arcane.client.ClientArcaneData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Estado de exibicao do grimorio e da mana (servidor -> cliente). Ver ArcaneSync. */
public record S2CGrimoireSyncPacket(CompoundTag data) {
  public static void encode(S2CGrimoireSyncPacket packet, FriendlyByteBuf buf) {
    buf.writeNbt(packet.data);
  }

  public static S2CGrimoireSyncPacket decode(FriendlyByteBuf buf) {
    CompoundTag tag = buf.readNbt();
    return new S2CGrimoireSyncPacket(tag == null ? new CompoundTag() : tag);
  }

  public static void handle(S2CGrimoireSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> ClientArcaneData.apply(packet.data));
    ctx.get().setPacketHandled(true);
  }
}
