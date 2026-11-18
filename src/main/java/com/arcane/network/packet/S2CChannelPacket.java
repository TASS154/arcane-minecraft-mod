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
public record S2CChannelPacket(int state, float progress, int shape, int rgb, float radius, net.minecraft.world.phys.Vec3 anchor) {
  public S2CChannelPacket(int state, float progress, int shape, int rgb) { this(state, progress, shape, rgb, 0f, null); }

  public static final int OFF = 0, CHARGING = 1, HOLDING = 2;
  public static final int SELF = 0, AURA = 1, WAVE = 2, PILLAR = 3, BEAM = 4;

  public static void encode(S2CChannelPacket p, FriendlyByteBuf buf) {
    buf.writeVarInt(p.state);
    buf.writeFloat(p.progress);
    buf.writeVarInt(p.shape);
    buf.writeInt(p.rgb);
    buf.writeFloat(p.radius);
    buf.writeBoolean(p.anchor != null);
    if (p.anchor != null) { buf.writeDouble(p.anchor.x); buf.writeDouble(p.anchor.y); buf.writeDouble(p.anchor.z); }
  }

  public static S2CChannelPacket decode(FriendlyByteBuf buf) {
    int state = buf.readVarInt();
    float progress = buf.readFloat();
    int shape = buf.readVarInt(), rgb = buf.readInt();
    float radius = buf.readFloat();
    net.minecraft.world.phys.Vec3 anchor = buf.readBoolean() ? new net.minecraft.world.phys.Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
    return new S2CChannelPacket(state, progress, shape, rgb, radius, anchor);
  }

  public static void handle(S2CChannelPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CChannelPacket p) {
    ChannelFx.set(p.state, p.progress, p.shape, p.rgb, p.radius, p.anchor);
  }
}
