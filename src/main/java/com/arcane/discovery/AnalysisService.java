package com.arcane.discovery;

import com.arcane.grimoire.GlyphKnowledge;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class AnalysisService {
  private AnalysisService() {}

  public static void analyzeInscription(ServerPlayer player, Inscription inscription) {
    GrimoireAttachments.get(player).ifPresent(grimoire -> {
      int revealed = 0;
      for (GlyphInstance glyph : inscription.glyphs()) {
        GlyphKnowledge current = grimoire.knowledge().getOrDefault(glyph.glyphId(), GlyphKnowledge.UNKNOWN);
        if (current.atLeast(GlyphKnowledge.MASTERED)) {
          revealed++;
          continue;
        }
        GlyphKnowledge next = switch (current) {
          case UNKNOWN -> GlyphKnowledge.SUSPECTED;
          case SUSPECTED -> GlyphKnowledge.PARTIAL;
          case PARTIAL -> GlyphKnowledge.IDENTIFIED;
          case IDENTIFIED -> GlyphKnowledge.MASTERED;
          default -> current;
        };
        grimoire.learnGlyph(glyph.glyphId(), next);
        player.displayClientMessage(Component.translatable("message.arcane.analysis.reveal",
            glyph.glyphId().toString(), next.name()), false);
        revealed++;
      }
      grimoire.logDiscovery("analysis:" + inscription.id());
    });
  }
}
