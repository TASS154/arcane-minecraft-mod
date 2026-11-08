package com.arcane.execution;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometria dos formatos (funcao pura, testada). Os glifos de formato (Domo, Esfera, Anel) so gravam o numero do formato
 * no parametro "shape" da forma; qualquer forma que cria blocos ou escolhe criaturas por area usa esta classe, entao o
 * mesmo formato vale para Barreira, Aura e o que vier depois. 0 = padrao da forma (a parede da Barreira, a esfera da Aura).
 */
public final class ShapeGeometry {
  public static final int DEFAULT = 0, DOME = 1, SPHERE = 2, RING = 3;
  /** Limite de blocos de uma casca (protege o servidor); o excedente simplesmente nao e criado. */
  public static final int MAX_BLOCKS = 2500;
  public static final double MIN_RADIUS = 2.0;

  public record Off(int dx, int dy, int dz) {}

  private ShapeGeometry() {}

  public static int shapeOf(double raw) {
    int s = (int) Math.round(raw);
    return s < DEFAULT || s > RING ? DEFAULT : s;
  }

  /** Raio maximo por formato, para a casca caber em {@link #MAX_BLOCKS}. */
  public static double maxRadius(int shape) {
    return switch (shape) {
      case DOME -> 16.0;
      case SPHERE -> 12.0;
      case RING -> 24.0;
      default -> 12.0;
    };
  }

  public static double clampRadius(int shape, double radius) {
    return Math.max(MIN_RADIUS, Math.min(maxRadius(shape), radius));
  }

  /** Desloca os blocos da casca (espessura 1) relativos ao centro. */
  public static List<Off> shell(int shape, double radius) {
    List<Off> out = new ArrayList<>();
    if (shape != DOME && shape != SPHERE && shape != RING) return out;
    double r = clampRadius(shape, radius);
    int n = (int) Math.ceil(r) + 1;
    for (int dy = (shape == SPHERE ? -n : 0); dy <= (shape == RING ? 1 : n); dy++) {
      for (int dx = -n; dx <= n; dx++) {
        for (int dz = -n; dz <= n; dz++) {
          double d = shape == RING ? Math.sqrt(dx * (double) dx + dz * (double) dz)
              : Math.sqrt(dx * (double) dx + dy * (double) dy + dz * (double) dz);
          if (d >= r - 0.5 && d < r + 0.5) {
            if (out.size() >= MAX_BLOCKS) return out;
            out.add(new Off(dx, dy, dz));
          }
        }
      }
    }
    return out;
  }

  /** O ponto (relativo ao centro) esta dentro do volume do formato? Usado para escolher criaturas. */
  public static boolean contains(int shape, double dx, double dy, double dz, double radius) {
    double d3 = Math.sqrt(dx * dx + dy * dy + dz * dz);
    double d2 = Math.sqrt(dx * dx + dz * dz);
    return switch (shape) {
      case DOME -> dy >= -0.6 && d3 <= radius;
      case RING -> dy >= -0.6 && dy <= 2.6 && Math.abs(d2 - radius) <= 1.5;
      default -> d3 <= radius;
    };
  }
}
