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
        GlyphKnowledge next = step(grimoire, glyph);
        grimoire.learnGlyph(glyph, next);
        grimoire.logDiscovery("experiment:" + glyph);
      }
    });
  }

  private static void learn(ServerPlayer player, ResourceLocation glyph, GlyphKnowledge level, String source) {
    GrimoireAttachments.get(player).ifPresent(grimoire -> {
      grimoire.learnGlyph(glyph, level);
      grimoire.logDiscovery(source + ":" + glyph);
      player.displayClientMessage(Component.translatable(
          "message.arcane.discovery.learn", glyph.toString(), level.name()), false);
    });
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
