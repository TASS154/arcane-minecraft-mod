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
 * As seis escolas dos magos NPC. Cada skin (0..17) pertence a uma escola (variante % 6). Cada escola tem um grimorio
 * com magias de papeis diferentes (ataque, area, defesa, fuga, apoio, truque), conjuradas pelo mesmo motor do jogador;
 * o mago escolhe qual usar conforme a situacao e a sua personalidade (ver {@link MageBrain}).
 */
public enum MageElement {
  FIRE("fire", 12.0,
      spell(Role.ATTACK, "Bola de Fogo", 12, 40, g("projectile"), g("damage", "amount", 3.0), g("fire", "duration", 60.0)),
      spell(Role.AREA, "Anel de Brasas", 5, 120, g("aura", "radius", 4.0), g("fire", "duration", 80.0)),
      spell(Role.DEFEND, "Escudo Rubro", 0, 200, g("self"), g("shield")),
      spell(Role.ESCAPE, "Salto de Fumaca", 0, 160, g("self"), g("blink"))),
  FROST("frost", 12.0,
      spell(Role.ATTACK, "Lanca de Gelo", 12, 40, g("projectile"), g("damage", "amount", 2.0), g("slow", "duration", 80.0)),
      spell(Role.AREA, "Inverno", 6, 140, g("aura", "radius", 5.0), g("freeze", "duration", 60.0)),
      spell(Role.DEFEND, "Pele de Gelo", 0, 200, g("self"), g("shield")),
      spell(Role.SUPPORT, "Orvalho", 10, 180, g("projectile"), g("heal", "amount", 4.0))),
  STORM("storm", 16.0,
      spell(Role.ATTACK, "Descarga", 16, 50, g("ray", "range", 16.0), g("damage", "amount", 4.0), g("push", "strength", 0.6)),
      spell(Role.AREA, "Trovao", 14, 220, g("projectile"), g("thunder")),
      spell(Role.ESCAPE, "Passo do Relampago", 0, 140, g("self"), g("blink")),
      spell(Role.DEFEND, "Velocidade", 0, 200, g("self"), g("speed"))),
  VENOM("venom", 12.0,
      spell(Role.ATTACK, "Dardo Venenoso", 12, 40, g("projectile"), g("damage", "amount", 2.0), g("poison", "duration", 100.0)),
      spell(Role.AREA, "Nevoa Podre", 5, 160, g("aura", "radius", 4.0), g("poison", "duration", 80.0), g("blind", "duration", 40.0)),
      spell(Role.TRICK, "Veu", 0, 260, g("self"), g("invisible", "duration", 100.0)),
      spell(Role.ESCAPE, "Fuga", 0, 160, g("self"), g("blink"))),
  BLAST("blast", 14.0,
      spell(Role.ATTACK, "Estouro Arcano", 14, 60, g("projectile"), g("explode", "radius", 2.0)),
      spell(Role.AREA, "Onda", 8, 140, g("wave"), g("push", "strength", 1.2), g("damage", "amount", 2.0)),
      spell(Role.DEFEND, "Barreira", 0, 240, g("barrier"), g("shield")),
      spell(Role.TRICK, "Chamariz", 10, 260, g("projectile"), g("decoy"))),
  GALE("gale", 8.0,
      spell(Role.ATTACK, "Rajada", 6, 40, g("aura", "radius", 5.0), g("push", "strength", 1.4), g("damage", "amount", 2.0)),
      spell(Role.AREA, "Levitar", 10, 160, g("projectile"), g("levitate", "duration", 40.0)),
      spell(Role.ESCAPE, "Vento de Fuga", 0, 120, g("self"), g("speed")),
      spell(Role.SUPPORT, "Brisa Curativa", 10, 180, g("projectile"), g("heal", "amount", 3.0)),
      spell(Role.TRICK, "Atracao", 10, 200, g("projectile"), g("attract", "strength", 1.0)));

  /** Papel de uma magia no combate. */
  public enum Role { ATTACK, AREA, DEFEND, ESCAPE, SUPPORT, TRICK }

  /** Uma magia do grimorio do mago: papel, alcance (0 = em si mesmo) e espera em ticks. */
  public record MageSpell(Role role, double range, int cooldown, Inscription inscription) {}

  public static final int VARIANTS = 18;

  private final String id;
  private final double castRange;
  private final List<MageSpell> spells;

  MageElement(String id, double castRange, MageSpell... spells) {
    this.id = id;
    this.castRange = castRange;
    this.spells = List.of(spells);
  }

  public String id() { return id; }
  public double castRange() { return castRange; }
  public List<MageSpell> spells() { return spells; }

  /** A magia de ataque principal (compatibilidade e drop). */
  public Inscription spell() { return spells.get(0).inscription(); }

  /** Glifos de todas as magias da escola (o drop de nota sorteia um deles). */
  public List<ResourceLocation> glyphIds() {
    List<ResourceLocation> ids = new ArrayList<>();
    for (MageSpell s : spells) for (GlyphInstance g : s.inscription().glyphs()) if (!ids.contains(g.glyphId())) ids.add(g.glyphId());
    return ids;
  }

  public static MageElement forVariant(int variant) {
    return values()[Math.floorMod(variant, values().length)];
  }

  private static MageSpell spell(Role role, String name, double range, int cooldown, GlyphInstance... glyphs) {
    return new MageSpell(role, range, cooldown, new Inscription(List.of(glyphs), new InscriptionMetadata(name,
        new ResourceLocation("arcane", "common"), new ResourceLocation("arcane", "arcane_pen"), 1.0,
        Optional.empty(), 0L, InscriptionSourceKind.GRIMOIRE)));
  }

  private static GlyphInstance g(String glyph) {
    return new GlyphInstance(new ResourceLocation("arcane", glyph));
  }

  private static GlyphInstance g(String glyph, String key, double value) {
    return new GlyphInstance(new ResourceLocation("arcane", glyph), Map.of(key, value));
  }
}
