package com.arcane.network.packet;

import com.arcane.discovery.DiscoveryService;
import com.arcane.inscription.GlyphInstance;
import com.arcane.menu.ArcaneWorkbenchMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record C2SSaveInscriptionPacket(String name, List<String> glyphIds) {
  private static final int MAX_GLYPHS = 24;

  public static void encode(C2SSaveInscriptionPacket packet, FriendlyByteBuf buf) {
    buf.writeUtf(packet.name);
    buf.writeVarInt(packet.glyphIds.size());
    packet.glyphIds.forEach(buf::writeUtf);
  }

  public static C2SSaveInscriptionPacket decode(FriendlyByteBuf buf) {
    String name = buf.readUtf();
    int count = Math.min(buf.readVarInt(), MAX_GLYPHS + 1);
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < count; i++) ids.add(buf.readUtf());
    return new C2SSaveInscriptionPacket(name, ids);
  }

  public static void handle(C2SSaveInscriptionPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null || !(player.containerMenu instanceof ArcaneWorkbenchMenu menu)) return;
      if (packet.glyphIds.size() > MAX_GLYPHS) return;
      List<ResourceLocation> used = new ArrayList<>();
      for (String raw : packet.glyphIds) {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) return;
        used.add(id);
      }
      menu.clearGlyphs();
      used.forEach(id -> menu.addGlyph(new GlyphInstance(id)));
      String name = packet.name.length() > 32 ? packet.name.substring(0, 32) : packet.name;
      menu.setSpellName(name);

      // Valida antes de salvar: composicoes invalidas contam como experimento, mas nao entram no grimorio.
      var draft = new com.arcane.inscription.Inscription(List.copyOf(menu.building()));
      var validation = com.arcane.inscription.InscriptionValidator.validate(draft,
          com.arcane.grimoire.GrimoireAttachments.get(player));
      if (validation.valid() && menu.saveToGrimoire(player)) {
        player.displayClientMessage(Component.translatable("message.arcane.workbench.saved",
            name.isEmpty() ? Component.translatable("gui.arcane.unnamed") : Component.literal(name))
            .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        player.level().playSound(null, player.blockPosition(), com.arcane.registry.ModSounds.WORKBENCH_SAVE.get(),
            net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.0f);
      } else {
        player.displayClientMessage(Component.translatable("message.arcane.workbench.invalid", validation.message())
            .withStyle(ChatFormatting.RED), true);
      }
      DiscoveryService.onExperiment(player, used);
    });
    ctx.get().setPacketHandled(true);
  }
}
