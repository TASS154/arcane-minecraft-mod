package com.arcane.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> cliente: as inscricoes escritas em blocos por perto (cada uma e desenhada na face em que foi escrita). */
public record S2CInscriptionDecalsPacket(List<Decal> decals) {
  public record Decal(BlockPos pos, Direction face, List<ResourceLocation> glyphs, int color) {}

  public static void encode(S2CInscriptionDecalsPacket p, FriendlyByteBuf buf) {
    buf.writeVarInt(p.decals.size());
    for (Decal d : p.decals) {
      buf.writeBlockPos(d.pos);
      buf.writeVarInt(d.face.get3DDataValue());
      buf.writeVarInt(d.color);
      buf.writeVarInt(d.glyphs.size());
      for (ResourceLocation g : d.glyphs) buf.writeResourceLocation(g);
    }
  }

  public static S2CInscriptionDecalsPacket decode(FriendlyByteBuf buf) {
    int n = Math.min(buf.readVarInt(), 512);
    List<Decal> list = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      BlockPos pos = buf.readBlockPos();
      Direction face = Direction.from3DDataValue(buf.readVarInt());
      int color = buf.readVarInt();
      int g = Math.min(buf.readVarInt(), 24);
      List<ResourceLocation> glyphs = new ArrayList<>();
      for (int k = 0; k < g; k++) glyphs.add(buf.readResourceLocation());
      list.add(new Decal(pos, face, glyphs, color));
    }
    return new S2CInscriptionDecalsPacket(list);
  }

  public static void handle(S2CInscriptionDecalsPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CInscriptionDecalsPacket p) {
    com.arcane.client.render.InscriptionDecals.set(p.decals);
  }
}
