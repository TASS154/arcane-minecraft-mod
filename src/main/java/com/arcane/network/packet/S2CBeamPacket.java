package com.arcane.network.packet;

import com.arcane.client.render.BeamRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Feixe 3D do Raio, desenhado no cliente (so visual; o dano continua no servidor). */
public record S2CBeamPacket(Vec3 from, Vec3 to, int rgb, int element, int ticks) {
  public static void encode(S2CBeamPacket p, FriendlyByteBuf buf) {
    buf.writeDouble(p.from.x).writeDouble(p.from.y).writeDouble(p.from.z);
    buf.writeDouble(p.to.x).writeDouble(p.to.y).writeDouble(p.to.z);
    buf.writeInt(p.rgb);
    buf.writeVarInt(p.element);
    buf.writeVarInt(p.ticks);
  }

  public static S2CBeamPacket decode(FriendlyByteBuf buf) {
    Vec3 from = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    Vec3 to = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    return new S2CBeamPacket(from, to, buf.readInt(), buf.readVarInt(), buf.readVarInt());
  }

  public static void handle(S2CBeamPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CBeamPacket p) {
    BeamRenderer.add(p.from, p.to, p.rgb, p.element, p.ticks);
  }
}
