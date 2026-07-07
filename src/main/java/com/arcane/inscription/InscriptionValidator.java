package com.arcane.inscription;

import com.arcane.glyph.GlyphCategory;
import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.grimoire.GlyphKnowledge;
import com.arcane.grimoire.GrimoireState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InscriptionValidator {
    private InscriptionValidator() {}

    public record ValidationResult(boolean valid, Component message) {
        public static ValidationResult ok() {
            return new ValidationResult(true, Component.empty());
        }

        public static ValidationResult fail(String key) {
            return new ValidationResult(false, Component.translatable(key));
        }
    }

    public static ValidationResult validate(Inscription inscription) {
        return validate(inscription, Optional.empty());
    }

    public static ValidationResult validate(Inscription inscription, Optional<GrimoireState> grimoire) {
        if (inscription.glyphs().isEmpty()) {
            return ValidationResult.fail("message.arcane.inscription.empty");
        }

        boolean hasForm = false;
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        GlyphCategory previousCategory = null;

        for (int i = 0; i < inscription.glyphs().size(); i++) {
            GlyphInstance instance = inscription.glyphs().get(i);
            Optional<GlyphDefinition> defOpt = GlyphRegistry.get(instance.glyphId());
            if (defOpt.isEmpty()) {
                return ValidationResult.fail("message.arcane.inscription.unknown_glyph");
            }

            GlyphDefinition def = defOpt.get();
            counts.merge(instance.glyphId(), 1, Integer::sum);
            if (counts.get(instance.glyphId()) > def.composition().maxPerInscription()) {
                return ValidationResult.fail("message.arcane.inscription.too_many_glyph");
            }

            if (def.category() == GlyphCategory.FORM) {
                if (i != 0) {
                    return ValidationResult.fail("message.arcane.inscription.form_not_first");
                }
                hasForm = true;
            } else if (!hasForm && def.category() != GlyphCategory.CONDITION && def.category() != GlyphCategory.ACTIVATOR) {
                return ValidationResult.fail("message.arcane.inscription.missing_form");
            }

            if (previousCategory != null && !def.composition().allowedAfter().isEmpty()) {
                if (!def.composition().allowedAfter().contains(previousCategory.getSerializedName())) {
                    return ValidationResult.fail("message.arcane.inscription.invalid_order");
                }
            }

            if (previousCategory != null && def.composition().disallowedAfter().contains(previousCategory.getSerializedName())) {
                return ValidationResult.fail("message.arcane.inscription.invalid_order");
            }

            if (grimoire.isPresent() && !grimoire.get().knowsGlyph(instance.glyphId(), GlyphKnowledge.IDENTIFIED)) {
                return ValidationResult.fail("message.arcane.inscription.unknown_knowledge");
            }

            previousCategory = def.category();
        }

        if (!hasForm && inscription.glyphs().stream().noneMatch(g ->
                GlyphRegistry.get(g.glyphId()).map(d -> d.category() == GlyphCategory.ACTIVATOR).orElse(false))) {
            return ValidationResult.fail("message.arcane.inscription.missing_form");
        }

        return ValidationResult.ok();
    }
}
