package com.arcane.network.packet;

import com.arcane.client.ClientArcaneData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> bancada: linhas da previa (avisos em dourado, erros em vermelho). */
public record S2CPreviewPacket(List<Component> lines) {
  public static void encode(S2CPreviewPacket p, FriendlyByteBuf buf) {
    buf.writeVarInt(p.lines.size());
    for (Component c : p.lines) buf.writeComponent(c);
  }

  public static S2CPreviewPacket decode(FriendlyByteBuf buf) {
    int n = Math.min(buf.readVarInt(), 64);
    List<Component> lines = new ArrayList<>();
    for (int i = 0; i < n; i++) lines.add(buf.readComponent());
    return new S2CPreviewPacket(lines);
  }

  public static void handle(S2CPreviewPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CPreviewPacket p) {
    ClientArcaneData.setPreview(p.lines);
  }
}
