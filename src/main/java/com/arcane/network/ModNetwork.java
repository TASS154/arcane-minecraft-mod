package com.arcane.network;

import com.arcane.ArcaneMod;
import com.arcane.network.packet.C2SCastRequestPacket;
import com.arcane.network.packet.C2SCopyToScrollPacket;
import com.arcane.network.packet.C2SJournalNotePacket;
import com.arcane.network.packet.C2SRenameInscriptionPacket;
import com.arcane.network.packet.C2SOpenWritePacket;
import com.arcane.network.packet.C2SDeleteInscriptionPacket;
import com.arcane.network.packet.C2SSaveInscriptionPacket;
import com.arcane.network.packet.C2SSelectInscriptionPacket;
import com.arcane.network.packet.C2SAreaDialPacket;
import com.arcane.network.packet.C2SRitualPacket;
import com.arcane.network.packet.S2COpenRitualPacket;
import com.arcane.network.packet.S2CRitualReportPacket;
import com.arcane.network.packet.S2CBeamPacket;
import com.arcane.network.packet.S2CGrimoireSyncPacket;
import com.arcane.network.packet.S2CShapeFxPacket;
import com.arcane.network.packet.S2CSpellFxPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
  private static final String PROTOCOL = "8";
  public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      new ResourceLocation(ArcaneMod.MODID, "main"),
      () -> PROTOCOL,
      PROTOCOL::equals,
      PROTOCOL::equals
  );
  private static int id = 0;

  private ModNetwork() {}

  public static void register() {
    CHANNEL.messageBuilder(com.arcane.network.packet.C2SGrimoireOrgPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(com.arcane.network.packet.C2SGrimoireOrgPacket::encode).decoder(com.arcane.network.packet.C2SGrimoireOrgPacket::decode)
        .consumerMainThread(com.arcane.network.packet.C2SGrimoireOrgPacket::handle).add();
    CHANNEL.messageBuilder(C2SCastRequestPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(C2SCastRequestPacket::encode).decoder(C2SCastRequestPacket::decode)
        .consumerMainThread(C2SCastRequestPacket::handle).add();
    CHANNEL.messageBuilder(com.arcane.network.packet.S2CInscriptionDecalsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(com.arcane.network.packet.S2CInscriptionDecalsPacket::encode).decoder(com.arcane.network.packet.S2CInscriptionDecalsPacket::decode)
        .consumerMainThread(com.arcane.network.packet.S2CInscriptionDecalsPacket::handle).add();
    CHANNEL.messageBuilder(S2CSpellFxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(S2CSpellFxPacket::encode).decoder(S2CSpellFxPacket::decode)
        .consumerMainThread(S2CSpellFxPacket::handle).add();
    CHANNEL.messageBuilder(com.arcane.network.packet.S2CPlayerAnimPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(com.arcane.network.packet.S2CPlayerAnimPacket::encode).decoder(com.arcane.network.packet.S2CPlayerAnimPacket::decode)
        .consumerMainThread(com.arcane.network.packet.S2CPlayerAnimPacket::handle).add();
    CHANNEL.messageBuilder(com.arcane.network.packet.C2SPreviewPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(com.arcane.network.packet.C2SPreviewPacket::encode).decoder(com.arcane.network.packet.C2SPreviewPacket::decode)
        .consumerMainThread(com.arcane.network.packet.C2SPreviewPacket::handle).add();
    CHANNEL.messageBuilder(com.arcane.network.packet.S2CPreviewPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(com.arcane.network.packet.S2CPreviewPacket::encode).decoder(com.arcane.network.packet.S2CPreviewPacket::decode)
        .consumerMainThread(com.arcane.network.packet.S2CPreviewPacket::handle).add();
    CHANNEL.messageBuilder(S2CBeamPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(S2CBeamPacket::encode).decoder(S2CBeamPacket::decode)
        .consumerMainThread(S2CBeamPacket::handle).add();
    CHANNEL.messageBuilder(S2CShapeFxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(S2CShapeFxPacket::encode).decoder(S2CShapeFxPacket::decode)
        .consumerMainThread(S2CShapeFxPacket::handle).add();
    CHANNEL.messageBuilder(S2CGrimoireSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(S2CGrimoireSyncPacket::encode).decoder(S2CGrimoireSyncPacket::decode)
        .consumerMainThread(S2CGrimoireSyncPacket::handle).add();
    CHANNEL.messageBuilder(C2SSaveInscriptionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(C2SSaveInscriptionPacket::encode).decoder(C2SSaveInscriptionPacket::decode)
        .consumerMainThread(C2SSaveInscriptionPacket::handle).add();
    CHANNEL.messageBuilder(C2SSelectInscriptionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(C2SSelectInscriptionPacket::encode).decoder(C2SSelectInscriptionPacket::decode)
        .consumerMainThread(C2SSelectInscriptionPacket::handle).add();
    CHANNEL.messageBuilder(C2SRitualPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(C2SRitualPacket::encode).decoder(C2SRitualPacket::decode)
        .consumerMainThread(C2SRitualPacket::handle).add();
    CHANNEL.messageBuilder(S2COpenRitualPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(S2COpenRitualPacket::encode).decoder(S2COpenRitualPacket::decode)
        .consumerMainThread(S2COpenRitualPacket::handle).add();
    CHANNEL.messageBuilder(S2CRitualReportPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(S2CRitualReportPacket::encode).decoder(S2CRitualReportPacket::decode)
        .consumerMainThread(S2CRitualReportPacket::handle).add();
    CHANNEL.messageBuilder(com.arcane.network.packet.S2CWorldRulesPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(com.arcane.network.packet.S2CWorldRulesPacket::encode).decoder(com.arcane.network.packet.S2CWorldRulesPacket::decode)
        .consumerMainThread(com.arcane.network.packet.S2CWorldRulesPacket::handle).add();
    CHANNEL.messageBuilder(com.arcane.network.packet.S2CCompassDrawPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(com.arcane.network.packet.S2CCompassDrawPacket::encode).decoder(com.arcane.network.packet.S2CCompassDrawPacket::decode)
        .consumerMainThread(com.arcane.network.packet.S2CCompassDrawPacket::handle).add();
    CHANNEL.messageBuilder(com.arcane.network.packet.C2SWorldRulePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(com.arcane.network.packet.C2SWorldRulePacket::encode).decoder(com.arcane.network.packet.C2SWorldRulePacket::decode)
        .consumerMainThread(com.arcane.network.packet.C2SWorldRulePacket::handle).add();
    CHANNEL.messageBuilder(com.arcane.network.packet.C2SScepterPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(com.arcane.network.packet.C2SScepterPacket::encode).decoder(com.arcane.network.packet.C2SScepterPacket::decode)
        .consumerMainThread(com.arcane.network.packet.C2SScepterPacket::handle).add();
    CHANNEL.messageBuilder(com.arcane.network.packet.C2SFlightBoostPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(com.arcane.network.packet.C2SFlightBoostPacket::encode).decoder(com.arcane.network.packet.C2SFlightBoostPacket::decode)
        .consumerMainThread(com.arcane.network.packet.C2SFlightBoostPacket::handle).add();
    CHANNEL.messageBuilder(C2SAreaDialPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(C2SAreaDialPacket::encode).decoder(C2SAreaDialPacket::decode)
        .consumerMainThread(C2SAreaDialPacket::handle).add();
    CHANNEL.messageBuilder(C2SDeleteInscriptionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(C2SDeleteInscriptionPacket::encode).decoder(C2SDeleteInscriptionPacket::decode)
        .consumerMainThread(C2SDeleteInscriptionPacket::handle).add();
    CHANNEL.messageBuilder(C2SCopyToScrollPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(C2SCopyToScrollPacket::encode).decoder(C2SCopyToScrollPacket::decode)
        .consumerMainThread(C2SCopyToScrollPacket::handle).add();
    CHANNEL.messageBuilder(C2SRenameInscriptionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(C2SRenameInscriptionPacket::encode).decoder(C2SRenameInscriptionPacket::decode)
        .consumerMainThread(C2SRenameInscriptionPacket::handle).add();
    CHANNEL.messageBuilder(C2SOpenWritePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(C2SOpenWritePacket::encode).decoder(C2SOpenWritePacket::decode)
        .consumerMainThread(C2SOpenWritePacket::handle).add();
    CHANNEL.messageBuilder(com.arcane.network.packet.S2CChannelPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(com.arcane.network.packet.S2CChannelPacket::encode).decoder(com.arcane.network.packet.S2CChannelPacket::decode)
        .consumerMainThread(com.arcane.network.packet.S2CChannelPacket::handle).add();
    CHANNEL.messageBuilder(C2SJournalNotePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(C2SJournalNotePacket::encode).decoder(C2SJournalNotePacket::decode)
        .consumerMainThread(C2SJournalNotePacket::handle).add();
  }

  /** Estado de canalizacao (runas de carga/manter) so para o proprio jogador. */
  public static void sendChannel(ServerPlayer player, int state, float progress, int shape, int rgb, float radius, net.minecraft.world.phys.Vec3 anchor) {
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new com.arcane.network.packet.S2CChannelPacket(state, progress, shape, rgb, radius, anchor));
  }

  public static void sendChannel(ServerPlayer player, int state, float progress, int shape, int rgb) {
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new com.arcane.network.packet.S2CChannelPacket(state, progress, shape, rgb));
  }

  /** Feixe 3D visivel para quem estiver a ate 96 blocos. */
  public static void sendBeam(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 from,
                              net.minecraft.world.phys.Vec3 to, int rgb, int element, int ticks) {
    CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(from.x, from.y, from.z, 96.0, level.dimension())),
        new S2CBeamPacket(from, to, rgb, element, ticks));
  }

  /** Efeito de forma/impacto/ritual visivel num raio de 96 blocos. */
  public static void sendShape(net.minecraft.server.level.ServerLevel level, int type, net.minecraft.world.phys.Vec3 pos,
                               net.minecraft.world.phys.Vec3 dir, float size, int rgb, int element, int ticks) {
    CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, 96.0, level.dimension())),
        new S2CShapeFxPacket(type, pos, dir, size, rgb, element, ticks));
  }

  /** Animacao do corpo do jogador, para ele e para quem o esta vendo. */
  public static void sendPlayerAnim(ServerPlayer player, int clip) {
    CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
        new com.arcane.network.packet.S2CPlayerAnimPacket(player.getUUID(), clip));
  }

  public static void sendToPlayer(ServerPlayer player, Object packet) {
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
  }
}
