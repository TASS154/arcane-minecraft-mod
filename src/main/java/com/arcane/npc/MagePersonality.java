package com.arcane.npc;

/**
 * Personalidade de um mago NPC: decide QUEM ele ataca e COMO luta (o elemento decide com que magias).
 * <ul>
 *   <li>HOSTIL: caca jogadores e luta ate o fim, preferindo dano;</li>
 *   <li>DEFENSIVO: guarda a sua area; avisa quem chega perto e so ataca quem invade (a 6 blocos) ou o fere;</li>
 *   <li>INOFENSIVO: nunca ataca; foge de quem o fere e cura jogadores feridos por perto;</li>
 *   <li>ESTUDIOSO: ignora todos; se ferido, revida uma vez e escapa (teleporte, invisibilidade);</li>
 *   <li>TRAPACEIRO: provoca jogadores com truques (ilusao, empurrao, lentidao, levitar), quase sem dano.</li>
 * </ul>
 */
public enum MagePersonality {
  HOSTILE("hostile", 30, 1.0, 1.0),
  DEFENSIVE("defensive", 25, 1.2, 0.8),
  HARMLESS("harmless", 15, 0.6, 0.0),
  SCHOLAR("scholar", 15, 0.9, 0.6),
  TRICKSTER("trickster", 15, 0.8, 0.3);

  private final String id;
  private final int weight;
  /** Multiplicadores de vida e de dano/agressividade. */
  private final double health, aggression;

  MagePersonality(String id, int weight, double health, double aggression) {
    this.id = id;
    this.weight = weight;
    this.health = health;
    this.aggression = aggression;
  }

  public String id() { return id; }
  public double healthFactor() { return health; }
  public double aggression() { return aggression; }

  /** Sorteio ponderado (hostis e defensivos sao os mais comuns). */
  public static MagePersonality random(net.minecraft.util.RandomSource rnd) {
    int total = 0;
    for (MagePersonality p : values()) total += p.weight;
    int roll = rnd.nextInt(total);
    for (MagePersonality p : values()) {
      roll -= p.weight;
      if (roll < 0) return p;
    }
    return HOSTILE;
  }

  public static MagePersonality byOrdinal(int i) {
    MagePersonality[] all = values();
    return all[Math.floorMod(i, all.length)];
  }
}
