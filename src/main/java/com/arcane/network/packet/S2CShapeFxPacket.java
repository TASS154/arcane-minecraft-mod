package com.arcane.network.packet;

import com.arcane.client.render.ShapeFxRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Efeito visual 3D de uma forma (aura, onda, pilar), impacto ou ritual. So visual; o dano e do servidor. */
public record S2CShapeFxPacket(int type, Vec3 pos, Vec3 dir, float size, int rgb, int element, int ticks) {
  public static final int AURA = 0, WAVE = 1, PILLAR = 2, IMPACT = 3, RITUAL = 4, HOLOGRAM = 5;

  public static void encode(S2CShapeFxPacket p, FriendlyByteBuf buf) {
    buf.writeVarInt(p.type);
    buf.writeDouble(p.pos.x).writeDouble(p.pos.y).writeDouble(p.pos.z);
    buf.writeFloat((float) p.dir.x).writeFloat((float) p.dir.y).writeFloat((float) p.dir.z);
    buf.writeFloat(p.size);
    buf.writeInt(p.rgb);
    buf.writeVarInt(p.element);
    buf.writeVarInt(p.ticks);
  }

  public static S2CShapeFxPacket decode(FriendlyByteBuf buf) {
    int type = buf.readVarInt();
    Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    Vec3 dir = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
    return new S2CShapeFxPacket(type, pos, dir, buf.readFloat(), buf.readInt(), buf.readVarInt(), buf.readVarInt());
  }

  public static void handle(S2CShapeFxPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CShapeFxPacket p) {
    ShapeFxRenderer.add(p.type, p.pos, p.dir, p.size, p.rgb, p.element, p.ticks);
  }
}
