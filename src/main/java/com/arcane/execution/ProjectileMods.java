package com.arcane.execution;

import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Modificadores de Projetil (Fase J): Perseguir, Ricochete, Fragmentar e Arco. So valem na forma Projetil; nas outras
 * formas nao fazem nada (a previa da bancada avisa). Esta classe e pura: leitura dos parametros da forma, orcamento
 * (degrada em vez de dar erro) e a matematica da trajetoria. Os numeros vem de {@link Limits} (config, PROVISORIOS).
 *
 * <p>Orcamento: cada acerto que roda os trechos da magia conta um disparo. Um projetil com R ricochetes e F fragmentos
 * dispara no maximo {@code 1 + R + F} vezes (fragmento nunca fragmenta de novo nem ricocheteia). Se passar do teto, os
 * fragmentos caem primeiro, depois os ricochetes. Alem disso cada disparo passa pelo {@link SpellReentry}.
 */
public final class ProjectileMods {
  private ProjectileMods() {}

  public static final String HOMING = "homing";
  public static final String RICOCHET = "ricochet";
  public static final String FRAGMENT = "fragment";
  public static final String ARC = "arc";

  /** Numeros PROVISORIOS (docs/PERGUNTAS.md). Os de producao vem da config; os testes passam os seus. */
  public record Limits(int maxTriggers, double homingDegreesPerLevel, double homingConeDegrees, double homingRange,
                       double ricochetPowerLoss, double fragmentTotalPower, double fragmentMaxEach, int fragmentsPerWorld) {
    public static final Limits DEFAULT = new Limits(8, 3.0, 35.0, 16.0, 0.25, 1.2, 0.5, 64);
  }

  /** Limites da config; sem config carregada (testes de unidade), os padroes. */
  public static Limits limits() {
    try {
      return new Limits(com.arcane.config.ArcaneConfig.PROJECTILE_MAX_TRIGGERS.get(),
          com.arcane.config.ArcaneConfig.HOMING_DEGREES_PER_LEVEL.get(), com.arcane.config.ArcaneConfig.HOMING_CONE_DEGREES.get(),
          com.arcane.config.ArcaneConfig.HOMING_RANGE.get(), com.arcane.config.ArcaneConfig.RICOCHET_POWER_LOSS.get(),
          com.arcane.config.ArcaneConfig.FRAGMENT_TOTAL_POWER.get(), com.arcane.config.ArcaneConfig.FRAGMENT_MAX_EACH.get(),
          com.arcane.config.ArcaneConfig.FRAGMENTS_PER_WORLD.get());
    } catch (IllegalStateException e) {
      return Limits.DEFAULT;
    }
  }

  /** Bits sincronizados para o cliente desenhar o rastro de cada modificador. */
  public static final int BIT_HOMING = 1, BIT_RICOCHET = 2, BIT_FRAGMENT = 4, BIT_ARC = 8, BIT_SHARD = 16;

  public record Mods(int homing, int ricochet, int fragment, double arc) {
    public static final Mods NONE = new Mods(0, 0, 0, 0);

    public boolean any() { return homing > 0 || ricochet > 0 || fragment > 0 || arc > 0; }

    /** Disparos no pior caso: o acerto final, cada ricochete e cada fragmento. */
    public int triggers() { return 1 + ricochet + fragment; }

    public int bits() {
      return (homing > 0 ? BIT_HOMING : 0) | (ricochet > 0 ? BIT_RICOCHET : 0) | (fragment > 0 ? BIT_FRAGMENT : 0) | (arc > 0 ? BIT_ARC : 0);
    }
  }

  /** Le os modificadores dos parametros da forma (valores ja presos aos limites dos glifos). */
  public static Mods of(Map<String, Double> params) {
    int homing = clampInt(params.getOrDefault(HOMING, 0.0), 0, 3);
    int ricochet = clampInt(params.getOrDefault(RICOCHET, 0.0), 0, 4);
    int fragment = clampInt(params.getOrDefault(FRAGMENT, 0.0), 0, 6);
    if (fragment == 1) fragment = 2; // o glifo vai de 2 a 6
    double arc = Math.max(0.0, Math.min(16.0, params.getOrDefault(ARC, 0.0)));
    return new Mods(homing, ricochet, fragment, arc);
  }

  /** Algum modificador de projetil presente (para o aviso fora da forma Projetil). */
  public static boolean present(Map<String, Double> params) {
    return params.containsKey(HOMING) || params.containsKey(RICOCHET) || params.containsKey(FRAGMENT) || params.containsKey(ARC);
  }

  private static int clampInt(double v, int min, int max) {
    return (int) Math.max(min, Math.min(max, Math.round(v)));
  }

  /** Degrada ate caber no teto de disparos: corta fragmentos (ate 2, depois 0), depois ricochetes. Nunca da erro. */
  public static Mods budgeted(Mods m, int maxTriggers) {
    int cap = Math.max(1, maxTriggers);
    int f = m.fragment(), r = m.ricochet();
    while (1 + r + f > cap && f > 2) f--;
    while (1 + r + f > cap && r > 0) r--;
    if (1 + r + f > cap) f = 0;
    return new Mods(m.homing(), r, f, m.arc());
  }

  // ------------------------------------------------------------------ poder
  /** Fator de poder depois de {@code bounces} ricochetes (perde {@code loss} a cada um). */
  public static double bouncePower(int bounces, double loss) {
    double l = Math.max(0.0, Math.min(0.95, loss));
    return Math.pow(1.0 - l, Math.max(0, bounces));
  }

  /** Poder de cada fragmento: o total fica abaixo de {@code totalCap}, e nenhum passa de {@code maxEach}. */
  public static double fragmentPower(int count, double totalCap, double maxEach) {
    if (count <= 0) return 0.0;
    return Math.min(maxEach, totalCap / count);
  }

  // ------------------------------------------------------------------ Perseguir
  /** Curva maxima por tick, em radianos, do nivel 1 a 3. */
  public static double turnPerTick(int level, double degreesPerLevel) {
    return Math.toRadians(Math.max(0, level) * degreesPerLevel);
  }

  /** O alvo esta no cone a frente ({@code halfAngleDeg}) e dentro do alcance? */
  public static boolean inCone(Vec3 from, Vec3 dir, Vec3 target, double halfAngleDeg, double range) {
    Vec3 to = target.subtract(from);
    double d = to.length();
    if (d < 1e-6 || d > range || dir.lengthSqr() < 1e-9) return false;
    double cos = to.scale(1.0 / d).dot(dir.normalize());
    return cos >= Math.cos(Math.toRadians(halfAngleDeg));
  }

  /** Vira {@code vel} na direcao de {@code toTarget} no maximo {@code maxTurn} radianos, mantendo a velocidade. */
  public static Vec3 steer(Vec3 vel, Vec3 toTarget, double maxTurn) {
    double speed = vel.length();
    if (speed < 1e-9 || toTarget.lengthSqr() < 1e-9 || maxTurn <= 0) return vel;
    Vec3 a = vel.scale(1.0 / speed);
    Vec3 b = toTarget.normalize();
    double cos = Math.max(-1.0, Math.min(1.0, a.dot(b)));
    double angle = Math.acos(cos);
    if (angle <= maxTurn) return b.scale(speed);
    // eixo de rotacao; se for oposto, qualquer perpendicular serve
    Vec3 axis = a.cross(b);
    if (axis.lengthSqr() < 1e-12) axis = Math.abs(a.y) < 0.9 ? a.cross(new Vec3(0, 1, 0)) : a.cross(new Vec3(1, 0, 0));
    axis = axis.normalize();
    // Rodrigues
    double c = Math.cos(maxTurn), s = Math.sin(maxTurn);
    Vec3 rotated = a.scale(c).add(axis.cross(a).scale(s)).add(axis.scale(axis.dot(a) * (1 - c)));
    return rotated.normalize().scale(speed);
  }

  // ------------------------------------------------------------------ Ricochete
  /** Reflete a velocidade na face atingida (normal unitaria de eixo). */
  public static Vec3 reflect(Vec3 vel, Vec3 normal) {
    return vel.subtract(normal.scale(2 * vel.dot(normal)));
  }

  // ------------------------------------------------------------------ Arco
  /** Gravidade do arco, em blocos por tick ao quadrado. */
  public static final double ARC_GRAVITY = 0.05;

  /**
   * Velocidade inicial para um arco balistico de {@code from} ate {@code to}, com o topo {@code height} blocos acima
   * do mais alto dos dois pontos (passa por cima da cobertura). Gravidade {@code g} por tick.
   */
  public static Vec3 arcLaunch(Vec3 from, Vec3 to, double height, double g) {
    double h = Math.max(0.5, height);
    double apex = Math.max(from.y, to.y) + h;
    double up = apex - from.y;
    double down = apex - to.y;
    double vy = Math.sqrt(2 * g * up);
    double tUp = vy / g;
    double tDown = Math.sqrt(2 * down / g);
    double t = tUp + tDown;
    double dx = to.x - from.x, dz = to.z - from.z;
    return new Vec3(dx / t, vy, dz / t);
  }

  /** Simula o arco (testes e previa): posicao depois de {@code ticks} ticks. */
  public static Vec3 arcAt(Vec3 from, Vec3 v0, double g, int ticks) {
    double x = from.x, y = from.y, z = from.z, vy = v0.y;
    for (int i = 0; i < ticks; i++) {
      x += v0.x;
      y += vy;
      z += v0.z;
      vy -= g;
    }
    return new Vec3(x, y, z);
  }
}
