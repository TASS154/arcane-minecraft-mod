package com.arcane.network.packet;

import com.arcane.client.anim.PlayerAnimClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Manda tocar uma animacao (clipe) no modelo de um jogador; vai para quem o acompanha e para ele mesmo. */
public record S2CPlayerAnimPacket(UUID player, int clip) {
  public static void encode(S2CPlayerAnimPacket p, FriendlyByteBuf buf) {
    buf.writeUUID(p.player);
    buf.writeVarInt(p.clip);
  }

  public static S2CPlayerAnimPacket decode(FriendlyByteBuf buf) {
    return new S2CPlayerAnimPacket(buf.readUUID(), buf.readVarInt());
  }

  public static void handle(S2CPlayerAnimPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CPlayerAnimPacket p) {
    PlayerAnimClient.play(p.player, p.clip);
  }
}
