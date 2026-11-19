package com.arcane.ritual;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caminho que a caneta do compasso percorre: primeiro os aneis (o externo e o interno, em volta), depois as hastes,
 * uma a uma, indo e voltando. Servidor e cliente usam a mesma classe: o servidor poe cada traco quando a caneta
 * chega nele, e o cliente move a caneta continuamente por este caminho (nos saltos entre tracos ela se ergue).
 */
public final class CompassPath {
  private static final Map<Integer, CompassPath> CACHE = new HashMap<>();

  public final List<RitualTemplate.Cell> cells;
  /** Instante (em unidades de caminho) em que a caneta chega a cada celula. */
  public final double[] arrive;
  /** jump[i]: o movimento de i-1 ate i e um salto (caneta no ar), nao um traco. */
  public final boolean[] jump;
  public final double total;

  private CompassPath(List<RitualTemplate.Cell> cells) {
    this.cells = cells;
    int n = cells.size();
    arrive = new double[n];
    jump = new boolean[n];
    double t = 0;
    for (int i = 1; i < n; i++) {
      double d = dist(cells.get(i - 1), cells.get(i));
      if (d > 2.0) {
        jump[i] = true;
        t += 3.0 + d * 0.5;
      } else {
        t += Math.max(1.0, d);
      }
      arrive[i] = t;
    }
    total = Math.max(1.0, t);
  }

  public static synchronized CompassPath of(int radius) {
    return CACHE.computeIfAbsent(radius, r -> new CompassPath(build(r)));
  }

  private static double dist(RitualTemplate.Cell a, RitualTemplate.Cell b) {
    return Math.hypot(a.dx() - b.dx(), a.dz() - b.dz());
  }

  private static double angle(RitualTemplate.Cell c) {
    return Math.atan2(c.dz(), c.dx());
  }

  private static List<RitualTemplate.Cell> build(int radius) {
    int mid = Math.max(2, radius / 2);
    List<RitualTemplate.Cell> outer = new ArrayList<>();
    List<RitualTemplate.Cell> inner = new ArrayList<>();
    List<List<RitualTemplate.Cell>> spokes = new ArrayList<>();
    for (int i = 0; i < 8; i++) spokes.add(new ArrayList<>());
    for (RitualTemplate.Cell c : RitualTemplate.cells(radius)) {
      double d = Math.hypot(c.dx(), c.dz());
      if (Math.abs(d - radius) <= 1.0) outer.add(c);
      else if (Math.abs(d - mid) <= 1.0) inner.add(c);
      else {
        int octant = Math.floorMod((int) Math.round(angle(c) / (Math.PI / 4)), 8);
        spokes.get(octant).add(c);
      }
    }
    Comparator<RitualTemplate.Cell> byAngle = Comparator.comparingDouble(CompassPath::angle)
        .thenComparingInt(c -> c.dx() * c.dx() + c.dz() * c.dz()).thenComparingInt(RitualTemplate.Cell::dx);
    outer.sort(byAngle);
    inner.sort(byAngle);
    List<RitualTemplate.Cell> out = new ArrayList<>(outer);
    out.addAll(inner);
    boolean outward = true;
    for (List<RitualTemplate.Cell> spoke : spokes) {
      if (spoke.isEmpty()) continue;
      spoke.sort(Comparator.comparingInt((RitualTemplate.Cell c) -> c.dx() * c.dx() + c.dz() * c.dz()).thenComparingInt(RitualTemplate.Cell::dx));
      if (!outward) java.util.Collections.reverse(spoke);
      out.addAll(spoke);
      outward = !outward;
    }
    return out;
  }

  /** Quantas celulas a caneta ja alcancou no instante t (unidades de caminho). */
  public int reached(double t) {
    int lo = 0, hi = arrive.length;
    while (lo < hi) {
      int mid = (lo + hi) >>> 1;
      if (arrive[mid] <= t) lo = mid + 1;
      else hi = mid;
    }
    return lo;
  }

  /** Posicao da caneta no instante t: {x, z, altura de salto (0 a 1)}, em celulas relativas ao altar. */
  public double[] at(double t) {
    t = Math.max(0.0, Math.min(total, t));
    int n = reached(t);
    int i = Math.max(0, n - 1);
    if (i >= cells.size() - 1) {
      RitualTemplate.Cell c = cells.get(cells.size() - 1);
      return new double[] {c.dx(), c.dz(), 0.0};
    }
    RitualTemplate.Cell a = cells.get(i);
    RitualTemplate.Cell b = cells.get(i + 1);
    double span = arrive[i + 1] - arrive[i];
    double f = span <= 0 ? 1.0 : (t - arrive[i]) / span;
    f = Math.max(0.0, Math.min(1.0, f));
    boolean j = jump[i + 1];
    double e = j ? f * f * (3 - 2 * f) : f;
    return new double[] {a.dx() + (b.dx() - a.dx()) * e, a.dz() + (b.dz() - a.dz()) * e, j ? Math.sin(f * Math.PI) : 0.0};
  }
}
