package com.arcane.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Servidor -> cliente: estado das regras do mundo; abre a tela (ou a atualiza se ja estiver aberta). */
public record S2CWorldRulesPacket(Map<String, Double> values, boolean canEdit) {
  public static void encode(S2CWorldRulesPacket p, FriendlyByteBuf buf) {
    buf.writeBoolean(p.canEdit);
    buf.writeVarInt(p.values.size());
    p.values.forEach((k, v) -> { buf.writeUtf(k, 64); buf.writeDouble(v); });
  }

  public static S2CWorldRulesPacket decode(FriendlyByteBuf buf) {
    boolean edit = buf.readBoolean();
    int n = Math.min(buf.readVarInt(), 64);
    Map<String, Double> map = new LinkedHashMap<>();
    for (int i = 0; i < n; i++) map.put(buf.readUtf(64), buf.readDouble());
    return new S2CWorldRulesPacket(map, edit);
  }

  public static void handle(S2CWorldRulesPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> show(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void show(S2CWorldRulesPacket p) {
    com.arcane.client.screen.WorldRulesScreen.show(p.values, p.canEdit);
  }
}
