package com.arcane.npc;

import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionMetadata;
import com.arcane.inscription.InscriptionSourceKind;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Os seis elementos dos magos NPC. Cada skin (0..17) pertence a um elemento (variante % 6)
 * e o elemento define a magia que o mago conjura, usando o mesmo motor do jogador.
 */
public enum MageElement {
  FIRE("fire", 12.0, "Bola de Fogo",
      g("projectile"), g("damage", "amount", 3.0), g("fire", "duration", 60.0)),
  FROST("frost", 12.0, "Lança de Gelo",
      g("projectile"), g("damage", "amount", 2.0), g("slow", "duration", 80.0)),
  STORM("storm", 16.0, "Descarga",
      g("ray", "range", 16.0), g("damage", "amount", 5.0), g("push", "strength", 0.8)),
  VENOM("venom", 12.0, "Dardo Venenoso",
      g("projectile"), g("damage", "amount", 2.0), g("poison", "duration", 100.0)),
  BLAST("blast", 14.0, "Estouro Arcano",
      g("projectile"), g("explode", "radius", 2.0)),
  GALE("gale", 5.0, "Rajada",
      g("aura", "radius", 5.0), g("push", "strength", 1.4), g("damage", "amount", 2.0));

  public static final int VARIANTS = 18;

  private final String id;
  private final double castRange;
  private final Inscription spell;

  MageElement(String id, double castRange, String spellName, GlyphInstance... glyphs) {
    this.id = id;
    this.castRange = castRange;
    this.spell = new Inscription(List.of(glyphs), new InscriptionMetadata(spellName,
        new ResourceLocation("arcane", "common"), new ResourceLocation("arcane", "arcane_pen"), 1.0,
        Optional.empty(), 0L, InscriptionSourceKind.GRIMOIRE));
  }

  public String id() { return id; }
  public double castRange() { return castRange; }
  public Inscription spell() { return spell; }

  public List<ResourceLocation> glyphIds() {
    List<ResourceLocation> ids = new ArrayList<>();
    for (GlyphInstance g : spell.glyphs()) ids.add(g.glyphId());
    return ids;
  }

  public static MageElement forVariant(int variant) {
    return values()[Math.floorMod(variant, values().length)];
  }

  private static GlyphInstance g(String glyph) {
    return new GlyphInstance(new ResourceLocation("arcane", glyph));
  }

  private static GlyphInstance g(String glyph, String key, double value) {
    return new GlyphInstance(new ResourceLocation("arcane", glyph), Map.of(key, value));
  }
}
