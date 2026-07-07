package com.arcane.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record S2CSpellFxPacket(UUID inscriptionId, String fingerprint) {
  public static void encode(S2CSpellFxPacket packet, FriendlyByteBuf buf) {
    buf.writeUUID(packet.inscriptionId);
    buf.writeUTF(packet.fingerprint);
  }

  public static S2CSpellFxPacket decode(FriendlyByteBuf buf) {
    return new S2CSpellFxPacket(buf.readUUID(), buf.readUTF());
  }

  public static void handle(S2CSpellFxPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(packet));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CSpellFxPacket packet) {
    var player = Minecraft.getInstance().player;
    if (player != null) {
      player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }
  }
}
