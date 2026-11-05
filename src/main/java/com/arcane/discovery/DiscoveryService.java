package com.arcane.discovery;

import com.arcane.grimoire.GlyphKnowledge;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.grimoire.GrimoireState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class DiscoveryService {
  private DiscoveryService() {}

  public static void learnMastered(ServerPlayer player, ResourceLocation glyph) {
    learn(player, glyph, GlyphKnowledge.MASTERED, "mastered");
  }

  public static void learnFromLoot(Player player, ResourceLocation glyph) {
    if (player instanceof ServerPlayer serverPlayer) {
      learn(serverPlayer, glyph, GlyphKnowledge.IDENTIFIED, "loot");
    }
  }

  public static void onExperiment(ServerPlayer player, List<ResourceLocation> glyphs) {
    GrimoireAttachments.get(player).ifPresent(grimoire -> {
      for (ResourceLocation glyph : glyphs) {
        // Ignora ids inexistentes enviados pelo cliente, para nao poluir o grimorio.
        if (com.arcane.glyph.GlyphRegistry.get(glyph).isEmpty()) continue;
        GlyphKnowledge before = grimoire.knowledge().getOrDefault(glyph, GlyphKnowledge.UNKNOWN);
        GlyphKnowledge next = step(grimoire, glyph);
        grimoire.learnGlyph(glyph, next);
        grimoire.logDiscovery("experiment:" + glyph);
        if (next != before) {
          player.displayClientMessage(Component.translatable("message.arcane.discovery.progress",
              Component.translatable(glyphKey(glyph)), Component.translatable("knowledge.arcane." + next.name().toLowerCase())), false);
          if (next == GlyphKnowledge.IDENTIFIED) {
            playLearned(player);
          }
        }
      }
    });
    com.arcane.network.ArcaneSync.sync(player, true);
  }

  private static void learn(ServerPlayer player, ResourceLocation glyph, GlyphKnowledge level, String source) {
    GrimoireAttachments.get(player).ifPresent(grimoire -> {
      grimoire.learnGlyph(glyph, level);
      grimoire.logDiscovery(source + ":" + glyph);
      player.displayClientMessage(Component.translatable(
          "message.arcane.discovery.learn", Component.translatable(glyphKey(glyph)),
          Component.translatable("knowledge.arcane." + level.name().toLowerCase())), false);
      playLearned(player);
    });
    com.arcane.network.ArcaneSync.sync(player, true);
  }

  public static String glyphKey(ResourceLocation glyph) {
    return "glyph." + glyph.getNamespace() + "." + glyph.getPath();
  }

  private static void playLearned(ServerPlayer player) {
    player.level().playSound(null, player.blockPosition(), com.arcane.registry.ModSounds.GLYPH_LEARNED.get(),
        net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 1.0f);
  }

  private static GlyphKnowledge step(GrimoireState grimoire, ResourceLocation glyph) {
    GlyphKnowledge current = grimoire.knowledge().getOrDefault(glyph, GlyphKnowledge.UNKNOWN);
    return switch (current) {
      case UNKNOWN -> GlyphKnowledge.SUSPECTED;
      case SUSPECTED -> GlyphKnowledge.PARTIAL;
      case PARTIAL -> GlyphKnowledge.IDENTIFIED;
      case IDENTIFIED, MASTERED -> current;
    };
  }
}
