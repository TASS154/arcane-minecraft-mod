package com.arcane.network.packet;

import com.arcane.config.WorldRules;
import com.arcane.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: avanca uma regra do mundo. So administradores; o servidor responde com o estado atual. */
public record C2SWorldRulePacket(String key) {
  public static void encode(C2SWorldRulePacket p, FriendlyByteBuf buf) { buf.writeUtf(p.key, 64); }

  public static C2SWorldRulePacket decode(FriendlyByteBuf buf) { return new C2SWorldRulePacket(buf.readUtf(64)); }

  public static void handle(C2SWorldRulePacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;
      if (WorldRules.canEdit(player)) WorldRules.advance(p.key);
      ModNetwork.sendToPlayer(player, new S2CWorldRulesPacket(WorldRules.snapshot(), WorldRules.canEdit(player)));
    });
    ctx.get().setPacketHandled(true);
  }
}
