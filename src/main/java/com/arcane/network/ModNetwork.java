package com.arcane.network;

import com.arcane.ArcaneMod;
import com.arcane.network.packet.C2SCastRequestPacket;
import com.arcane.network.packet.C2SSaveInscriptionPacket;
import com.arcane.network.packet.S2CGrimoireSyncPacket;
import com.arcane.network.packet.S2CSpellFxPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
  private static final String PROTOCOL = "1";
  public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      new ResourceLocation(ArcaneMod.MODID, "main"),
      () -> PROTOCOL,
      PROTOCOL::equals,
      PROTOCOL::equals
  );
  private static int id = 0;

  private ModNetwork() {}

  public static void register() {
    CHANNEL.messageBuilder(C2SCastRequestPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(C2SCastRequestPacket::encode).decoder(C2SCastRequestPacket::decode)
        .consumerMainThread(C2SCastRequestPacket::handle).add();
    CHANNEL.messageBuilder(S2CSpellFxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(S2CSpellFxPacket::encode).decoder(S2CSpellFxPacket::decode)
        .consumerMainThread(S2CSpellFxPacket::handle).add();
    CHANNEL.messageBuilder(S2CGrimoireSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(S2CGrimoireSyncPacket::encode).decoder(S2CGrimoireSyncPacket::decode)
        .consumerMainThread(S2CGrimoireSyncPacket::handle).add();
    CHANNEL.messageBuilder(C2SSaveInscriptionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(C2SSaveInscriptionPacket::encode).decoder(C2SSaveInscriptionPacket::decode)
        .consumerMainThread(C2SSaveInscriptionPacket::handle).add();
  }

  public static void sendToPlayer(ServerPlayer player, Object packet) {
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
  }
}
