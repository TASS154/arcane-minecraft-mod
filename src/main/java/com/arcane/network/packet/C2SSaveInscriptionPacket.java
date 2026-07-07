package com.arcane.network.packet;

import com.arcane.discovery.DiscoveryService;
import com.arcane.inscription.GlyphInstance;
import com.arcane.menu.ArcaneWorkbenchMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record C2SSaveInscriptionPacket(String name, List<String> glyphIds) {
  public static void encode(C2SSaveInscriptionPacket packet, FriendlyByteBuf buf) {
    buf.writeUtf(packet.name);
    buf.writeVarInt(packet.glyphIds.size());
    packet.glyphIds.forEach(buf::writeUtf);
  }

  public static C2SSaveInscriptionPacket decode(FriendlyByteBuf buf) {
    String name = buf.readUtf();
    int count = buf.readVarInt();
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < count; i++) ids.add(buf.readUtf());
    return new C2SSaveInscriptionPacket(name, ids);
  }

  public static void handle(C2SSaveInscriptionPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null || !(player.containerMenu instanceof ArcaneWorkbenchMenu menu)) return;
      menu.clearGlyphs();
      packet.glyphIds.forEach(id -> menu.addGlyph(new GlyphInstance(new ResourceLocation(id))));
      menu.setSpellName(packet.name);
      menu.saveToGrimoire(player);
      List<ResourceLocation> used = packet.glyphIds.stream().map(ResourceLocation::new).toList();
      DiscoveryService.onExperiment(player, used);
    });
    ctx.get().setPacketHandled(true);
  }
}
