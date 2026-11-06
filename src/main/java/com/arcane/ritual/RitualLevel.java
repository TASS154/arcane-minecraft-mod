package com.arcane.ritual;

/**
 * Niveis de ritual (design: menor / medio / maior / lendario). O nivel vem do TAMANHO do circulo desenhado.
 * Os multiplicadores sao provisorios (config do servidor); o lendario nao multiplica mais: quebra regras
 * (ver RitualService).
 */
public enum RitualLevel {
  NONE(0, 0, 1.0, 1.0),
  MINOR(1, RitualTemplate.RADII[0], 1.5, 1.5),
  MEDIUM(2, RitualTemplate.RADII[1], 3.0, 2.5),
  MAJOR(3, RitualTemplate.RADII[2], 10.0, 4.0),
  LEGENDARY(4, RitualTemplate.RADII[2], 10.0, 4.0);

  private final int number;
  private final int radius;
  private final double defaultPower;
  private final double areaScale;

  RitualLevel(int number, int radius, double defaultPower, double areaScale) {
    this.number = number;
    this.radius = radius;
    this.defaultPower = defaultPower;
    this.areaScale = areaScale;
  }

  public int number() { return number; }
  public int radius() { return radius; }
  public double defaultPower() { return defaultPower; }
  /** Multiplicador de poder em vigor (config do servidor). */
  public double power() { return RitualSettings.power(this); }
  /** Quanto as formas (raio, alcance, tamanho da barreira) crescem. */
  public double areaScale() { return areaScale; }

  public static RitualLevel of(int number) {
    for (RitualLevel l : values()) if (l.number == number) return l;
    return NONE;
  }

  /** Maior nivel de circulo cujo raio cabe (4, 8 ou 16); o lendario depende da oferenda, nao do tamanho. */
  public static RitualLevel forRadius(int radius) {
    if (radius >= MAJOR.radius) return MAJOR;
    if (radius >= MEDIUM.radius) return MEDIUM;
    if (radius >= MINOR.radius) return MINOR;
    return NONE;
  }
}
