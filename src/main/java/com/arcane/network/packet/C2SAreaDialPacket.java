package com.arcane.network.packet;

import com.arcane.execution.AreaDial;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Roda do mouse: sobe (+1) ou desce (-1) o regulador de area x dano. O servidor limita e confirma na barra de acao. */
public record C2SAreaDialPacket(int delta) {
  public static void encode(C2SAreaDialPacket packet, FriendlyByteBuf buf) { buf.writeVarInt(packet.delta); }

  public static C2SAreaDialPacket decode(FriendlyByteBuf buf) { return new C2SAreaDialPacket(buf.readVarInt()); }

  public static void handle(C2SAreaDialPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player != null) apply(player, packet.delta);
    });
    ctx.get().setPacketHandled(true);
  }

  public static void apply(ServerPlayer player, int delta) {
    int step = AreaDial.add(player.getUUID(), Integer.signum(delta));
    double f = AreaDial.factorOf(step);
    player.displayClientMessage(Component.translatable("hud.arcane.area_dial", String.format("%.2f", f), String.format("%.2f", 1.0 / f))
        .withStyle(step == 0 ? ChatFormatting.GRAY : ChatFormatting.LIGHT_PURPLE), true);
  }
}
