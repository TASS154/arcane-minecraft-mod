package com.arcane.network.packet;

import com.arcane.grimoire.GrimoireState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CGrimoireSyncPacket(CompoundTag data) {
  public static void encode(S2CGrimoireSyncPacket packet, FriendlyByteBuf buf) {
    buf.writeNbt(packet.data);
  }

  public static S2CGrimoireSyncPacket decode(FriendlyByteBuf buf) {
    return new S2CGrimoireSyncPacket(buf.readNbt());
  }

  public static void handle(S2CGrimoireSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(packet));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CGrimoireSyncPacket packet) {
    // Client-side cache for GUI; server remains authoritative.
    Minecraft.getInstance().setScreen(null);
  }
}
