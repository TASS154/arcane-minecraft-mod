package com.arcane.network.packet;

import com.arcane.discovery.DiscoveryService;
import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.inscription.GlyphInstance;
import com.arcane.menu.ArcaneWorkbenchMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Bancada -> servidor: salva a composicao no grimorio. O servidor valida e limita todos os valores. */
public record C2SSaveInscriptionPacket(String name, List<Entry> glyphs) {
  public static final int MAX_GLYPHS = 24;
  private static final int MAX_PARAMS = 8;

  public record Entry(String id, Map<String, Double> params) {}

  public static void encode(C2SSaveInscriptionPacket packet, FriendlyByteBuf buf) {
    buf.writeUtf(packet.name, 64);
    buf.writeVarInt(packet.glyphs.size());
    for (Entry entry : packet.glyphs) {
      buf.writeUtf(entry.id, 128);
      buf.writeVarInt(entry.params.size());
      entry.params.forEach((key, value) -> {
        buf.writeUtf(key, 32);
        buf.writeDouble(value);
      });
    }
  }

  public static C2SSaveInscriptionPacket decode(FriendlyByteBuf buf) {
    String name = buf.readUtf(64);
    int count = Math.min(buf.readVarInt(), MAX_GLYPHS + 1);
    List<Entry> entries = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String id = buf.readUtf(128);
      int paramCount = Math.min(buf.readVarInt(), MAX_PARAMS);
      Map<String, Double> params = new HashMap<>();
      for (int p = 0; p < paramCount; p++) {
        params.put(buf.readUtf(32), buf.readDouble());
      }
      entries.add(new Entry(id, params));
    }
    return new C2SSaveInscriptionPacket(name, entries);
  }

  public static void handle(C2SSaveInscriptionPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null || !(player.containerMenu instanceof ArcaneWorkbenchMenu menu)) return;
      if (packet.glyphs.size() > MAX_GLYPHS) return;

      List<ResourceLocation> used = new ArrayList<>();
      List<GlyphInstance> instances = new ArrayList<>();
      for (Entry entry : packet.glyphs) {
        ResourceLocation id = ResourceLocation.tryParse(entry.id);
        if (id == null) return;
        used.add(id);
        instances.add(new GlyphInstance(id, sanitize(id, entry.params)));
      }
      menu.clearGlyphs();
      instances.forEach(menu::addGlyph);
      String name = packet.name.length() > 32 ? packet.name.substring(0, 32) : packet.name;
      menu.setSpellName(name.trim());

      // Valida antes de salvar: composicoes invalidas contam como experimento, mas nao entram no grimorio.
      var draft = new com.arcane.inscription.Inscription(List.copyOf(menu.building()));
      var validation = com.arcane.inscription.InscriptionValidator.validate(draft,
          com.arcane.grimoire.GrimoireAttachments.get(player));
      var grimoire = com.arcane.grimoire.GrimoireAttachments.get(player);
      boolean replaces = grimoire.map(g -> g.findByName(menu.spellName()).isPresent()).orElse(false);
      boolean noPages = validation.valid() && !replaces && grimoire.map(g -> g.freePages() <= 0).orElse(false);
      if (noPages) {
        player.displayClientMessage(Component.translatable("message.arcane.workbench.no_pages").withStyle(ChatFormatting.RED), true);
      } else if (validation.valid() && menu.saveToGrimoire(player)) {
        player.displayClientMessage(Component.translatable("message.arcane.workbench.saved",
            name.isBlank() ? Component.translatable("gui.arcane.unnamed") : Component.literal(name.trim()))
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

  /** So parametros que o glifo declara, limitados ao intervalo dele; valores iguais ao padrao nem sao guardados. */
  static Map<String, Double> sanitize(ResourceLocation id, Map<String, Double> given) {
    GlyphDefinition def = GlyphRegistry.get(id).orElse(null);
    if (def == null) return Map.of();
    Map<String, Double> clean = new HashMap<>();
    for (Map.Entry<String, Double> e : given.entrySet()) {
      if (!def.parameters().containsKey(e.getKey())) continue;
      double value = def.resolveParam(e.getKey(), Map.of(e.getKey(), e.getValue()));
      if (Math.abs(value - def.parameters().get(e.getKey()).defaultValue()) > 1e-9) {
        clean.put(e.getKey(), value);
      }
    }
    return Map.copyOf(clean);
  }
}
