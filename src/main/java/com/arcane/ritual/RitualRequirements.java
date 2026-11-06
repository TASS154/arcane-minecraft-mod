package com.arcane.ritual;

import com.arcane.glyph.ComponentRequirement;
import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * O que uma magia exige para ser conjurada: nivel de ritual (0 = nenhum, 1 menor, 2 medio, 3 maior, 4 lendario)
 * e os componentes fisicos de todos os glifos dela somados. Tudo vem das definicoes (datapack), sem codigo por glifo.
 */
public record RitualRequirements(int level, Map<ResourceLocation, Integer> components) {
  public static final RitualRequirements NONE = new RitualRequirements(0, Map.of());

  public static RitualRequirements of(Inscription inscription) {
    int level = 0;
    Map<ResourceLocation, Integer> components = new LinkedHashMap<>();
    for (GlyphInstance instance : inscription.glyphs()) {
      GlyphDefinition def = GlyphRegistry.get(instance.glyphId()).orElse(null);
      if (def == null) continue;
      level = Math.max(level, def.ritual());
      for (ComponentRequirement c : def.components()) {
        components.merge(c.item(), c.count(), Integer::sum);
      }
    }
    return level == 0 && components.isEmpty() ? NONE : new RitualRequirements(level, Map.copyOf(components));
  }

  public boolean needsRitual() {
    return level > 0 || !components.isEmpty();
  }
}
