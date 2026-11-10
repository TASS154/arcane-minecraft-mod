package com.arcane.execution;

import com.arcane.config.ArcaneConfig;
import com.arcane.grimoire.GlyphKnowledge;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.registry.ModEffects;
import net.minecraft.world.entity.player.Player;

/** Recalcula os bonus de mana do jogador: glifos descobertos (parcial ou mais) e efeitos de pocao. */
public final class ManaBonuses {
    private ManaBonuses() {}

    public static int discoveredGlyphs(Player player) {
        return GrimoireAttachments.get(player)
            .map(g -> (int) g.knowledge().values().stream().filter(k -> k.atLeast(GlyphKnowledge.PARTIAL)).count()).orElse(0);
    }

    public static void refresh(Player player) {
        GrimoireAttachments.getMana(player).ifPresent(mana -> {
            float glyphs = (float) (discoveredGlyphs(player) * ArcaneConfig.MANA_PER_GLYPH.get());
            float potion = 0f;
            var vessel = player.getEffect(ModEffects.MANA_VESSEL.get());
            if (vessel != null) potion = (float) (mana.baseMax() * ArcaneConfig.MANA_VESSEL_BONUS.get() * (vessel.getAmplifier() + 1));
            float regen = 1f;
            var surge = player.getEffect(ModEffects.MANA_SURGE.get());
            if (surge != null) regen = (float) (1.0 + (ArcaneConfig.MANA_SURGE_MULTIPLIER.get() - 1.0) * (surge.getAmplifier() + 1));
            mana.setBonuses(glyphs, potion, regen);
        });
    }
}
