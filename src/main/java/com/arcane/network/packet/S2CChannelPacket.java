package com.arcane.network.packet;

import com.arcane.client.render.ChannelFx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Estado da canalizacao do proprio jogador (so visual): OFF, CHARGING (runas crescendo com a mana gasta) ou HOLDING
 * (runas fixas enquanto a magia se mantem). {@code shape} diz onde as runas aparecem (SELF a BEAM).
 */
public record S2CChannelPacket(int state, float progress, int shape, int rgb) {
  public static final int OFF = 0, CHARGING = 1, HOLDING = 2;
  public static final int SELF = 0, AURA = 1, WAVE = 2, PILLAR = 3, BEAM = 4;

  public static void encode(S2CChannelPacket p, FriendlyByteBuf buf) {
    buf.writeVarInt(p.state);
    buf.writeFloat(p.progress);
    buf.writeVarInt(p.shape);
    buf.writeInt(p.rgb);
  }

  public static S2CChannelPacket decode(FriendlyByteBuf buf) {
    return new S2CChannelPacket(buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readInt());
  }

  public static void handle(S2CChannelPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CChannelPacket p) {
    ChannelFx.set(p.state, p.progress, p.shape, p.rgb);
  }
}
