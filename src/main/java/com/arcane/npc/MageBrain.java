package com.arcane.npc;

import com.arcane.npc.MageElement.MageSpell;
import com.arcane.npc.MageElement.Role;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * A "inteligencia" do mago: dada a situacao (vida, distancia do alvo, inimigos por perto, aliado ferido), escolhe a
 * magia do grimorio da escola que faz mais sentido, respeitando a espera de cada uma e a personalidade.
 * Regras, em ordem:
 * <ol>
 *   <li>vida abaixo de 35%: defesa (escudo, barreira, velocidade); se ja usou, fuga (teleporte);</li>
 *   <li>inimigo colado (menos de 4 blocos) e magia de area pronta: area (afasta ou prende);</li>
 *   <li>inofensivo: so apoio (cura em jogador ferido) e fuga; nunca ataca;</li>
 *   <li>trapaceiro: truque primeiro, depois area de controle, raramente dano;</li>
 *   <li>estudioso: revida com area ou ataque e depois foge;</li>
 *   <li>o resto: ataque a distancia se o alvo estiver ao alcance; com 2 ou mais inimigos perto, area.</li>
 * </ol>
 */
public final class MageBrain {
  private MageBrain() {}

  /** Situacao vista pelo mago neste momento. */
  public record Situation(float healthFrac, double targetDist, int enemiesNear, boolean allyHurt, boolean hasTarget) {}

  public static MageSpell choose(MageElement school, MagePersonality who, Situation s, int[] cooldowns, RandomSource rnd) {
    List<MageSpell> ready = new ArrayList<>();
    List<MageSpell> all = school.spells();
    for (int i = 0; i < all.size(); i++) if (cooldowns[i] <= 0) ready.add(all.get(i));
    if (ready.isEmpty()) return null;
    if (s.healthFrac() < 0.35f) {
      MageSpell d = pick(ready, Role.DEFEND);
      if (d != null) return d;
      MageSpell e = pick(ready, Role.ESCAPE);
      if (e != null) return e;
    }
    switch (who) {
      case HARMLESS -> {
        if (s.allyHurt()) return pick(ready, Role.SUPPORT);
        return s.hasTarget() ? pick(ready, Role.ESCAPE) : null;
      }
      case TRICKSTER -> {
        if (!s.hasTarget()) return null;
        MageSpell t = pick(ready, Role.TRICK);
        if (t != null && rnd.nextFloat() < 0.6f) return t;
        MageSpell a = pick(ready, Role.AREA);
        if (a != null && s.targetDist() <= a.range() + 1) return a;
        return rnd.nextFloat() < 0.25f ? inRange(pick(ready, Role.ATTACK), s) : null;
      }
      case SCHOLAR -> {
        if (!s.hasTarget()) return null;
        MageSpell e = pick(ready, Role.ESCAPE);
        if (e != null && rnd.nextFloat() < 0.5f) return e;
        MageSpell a = pick(ready, Role.AREA);
        if (a != null && s.targetDist() <= a.range() + 1) return a;
        return inRange(pick(ready, Role.ATTACK), s);
      }
      default -> {
        if (!s.hasTarget()) {
          return s.allyHurt() ? pick(ready, Role.SUPPORT) : null;
        }
        MageSpell area = pick(ready, Role.AREA);
        if (area != null && (s.targetDist() < 4 || s.enemiesNear() >= 2) && s.targetDist() <= area.range() + 1) return area;
        if (s.targetDist() < 3) {
          MageSpell e = pick(ready, Role.ESCAPE);
          if (e != null && rnd.nextFloat() < 0.4f) return e;
        }
        MageSpell t = pick(ready, Role.TRICK);
        if (t != null && rnd.nextFloat() < 0.15f) return t;
        return inRange(pick(ready, Role.ATTACK), s);
      }
    }
  }

  private static MageSpell inRange(MageSpell spell, Situation s) {
    return spell != null && s.targetDist() <= spell.range() + 0.5 ? spell : null;
  }

  private static MageSpell pick(List<MageSpell> ready, Role role) {
    for (MageSpell sp : ready) if (sp.role() == role) return sp;
    return null;
  }

  /** O mago aponta para quem: a si mesmo nas magias de alcance 0, ao aliado no apoio, ao alvo no resto. */
  public static boolean selfCast(MageSpell spell) { return spell.range() <= 0; }

  /** Quanto o mago quer ficar longe do alvo (o meio do alcance do ataque). */
  public static double preferredDistance(MageElement school, MagePersonality who) {
    double attack = school.spells().get(0).range();
    return who == MagePersonality.TRICKSTER ? 6 : Math.max(3.5, attack * 0.6);
  }

  public static boolean attacks(MagePersonality who, LivingEntity e) { return who != MagePersonality.HARMLESS; }
}
