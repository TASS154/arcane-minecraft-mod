package com.arcane.network.packet;

import com.arcane.execution.SpellPreview;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Bancada -> servidor: pede a previa (o que a composicao faz e avisos) sem salvar nada. */
public record C2SPreviewPacket(List<C2SSaveInscriptionPacket.Entry> glyphs) {
  public static void encode(C2SPreviewPacket p, FriendlyByteBuf buf) {
    buf.writeVarInt(p.glyphs.size());
    for (var entry : p.glyphs) {
      buf.writeUtf(entry.id(), 128);
      buf.writeVarInt(entry.params().size());
      entry.params().forEach((key, value) -> {
        buf.writeUtf(key, 32);
        buf.writeDouble(value);
      });
    }
  }

  public static C2SPreviewPacket decode(FriendlyByteBuf buf) {
    int count = Math.min(buf.readVarInt(), C2SSaveInscriptionPacket.MAX_GLYPHS + 1);
    List<C2SSaveInscriptionPacket.Entry> entries = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String id = buf.readUtf(128);
      int n = Math.min(buf.readVarInt(), 8);
      var params = new java.util.HashMap<String, Double>();
      for (int k = 0; k < n; k++) params.put(buf.readUtf(32), buf.readDouble());
      entries.add(new C2SSaveInscriptionPacket.Entry(id, params));
    }
    return new C2SPreviewPacket(entries);
  }

  public static void handle(C2SPreviewPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null || p.glyphs.size() > C2SSaveInscriptionPacket.MAX_GLYPHS) return;
      List<GlyphInstance> instances = new ArrayList<>();
      for (var entry : p.glyphs) {
        ResourceLocation id = ResourceLocation.tryParse(entry.id());
        if (id == null) return;
        instances.add(new GlyphInstance(id, C2SSaveInscriptionPacket.sanitize(id, entry.params())));
      }
      List<Component> lines = SpellPreview.describe(new Inscription(instances));
      ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CPreviewPacket(lines));
    });
    ctx.get().setPacketHandled(true);
  }
}
