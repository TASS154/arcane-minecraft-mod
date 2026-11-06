package com.arcane.grimoire;

import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Passagem de conhecimento entre grimorios (design: "outro jogador pega o grimorio e passa a ter suas magias",
 * "magos podem compartilhar magias"). Estudar nunca da conhecimento pronto: copia as FORMULAS e faz o estudioso
 * SUSPEITAR dos glifos que ainda nao conhece; entender de verdade exige experimentar na bancada.
 */
public final class GrimoireTransfer {
  private GrimoireTransfer() {}

  public record StudyResult(int added, int skippedNoPages, int skippedKnown, int suspected) {}

  /** O dono pega o proprio grimorio de volta: tudo volta, sem perder o que fez nesse meio tempo. */
  public static void restore(GrimoireState target, CompoundTag contents) {
    GrimoireState old = new GrimoireState();
    old.load(contents);
    target.mergeFrom(old);
  }

  /** Estudar o grimorio de outra pessoa (ou o conteudo de um pergaminho). */
  public static StudyResult study(GrimoireState target, GrimoireState source) {
    int added = 0, noPages = 0, known = 0, suspected = 0;
    for (Inscription spell : source.spellbook().values()) {
      StudyResult one = studySpell(target, spell);
      added += one.added();
      noPages += one.skippedNoPages();
      known += one.skippedKnown();
      suspected += one.suspected();
    }
    return new StudyResult(added, noPages, known, suspected);
  }

  public static StudyResult studySpell(GrimoireState target, Inscription spell) {
    if (target.hasSameSpell(spell)) return new StudyResult(0, 0, 1, 0);
    // Mesmo sem pagina livre, os glifos da formula ainda dao pistas.
    int suspected = 0;
    for (GlyphInstance glyph : spell.glyphs()) {
      ResourceLocation id = glyph.glyphId();
      if (com.arcane.glyph.GlyphRegistry.get(id).isEmpty() || target.knowledge().containsKey(id)) continue;
      target.learnGlyph(id, GlyphKnowledge.SUSPECTED);
      target.logDiscovery("study:" + id);
      suspected++;
    }
    if (target.freePages() <= 0) return new StudyResult(0, 1, 0, suspected);
    target.addInscription(new Inscription(spell.glyphs(), spell.metadata()));
    return new StudyResult(1, 0, 0, suspected);
  }
}
