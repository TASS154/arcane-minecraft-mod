package com.arcane.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometria do selo de uma magia (design: "desenhos geometricos inseridos um dentro do outro": cada glifo novo
 * envolve o anterior). Sem dependencias do Minecraft, para poder ser testada.
 *
 * Forma por categoria: Forma = triangulo, Efeito = circulo, Filtro = quadrado, Modificador = losango,
 * Condicao = pentagono, Ativador = hexagono. A rotacao vem do id do glifo, entao cada composicao tem um desenho proprio.
 */
public final class SealGeometry {
  public static final int CIRCLE_SIDES = 28;

  private SealGeometry() {}

  public static int sidesFor(String category) {
    return switch (category) {
      case "form" -> 3;
      case "effect" -> CIRCLE_SIDES;
      case "filter" -> 4;
      case "modifier" -> 4;
      case "condition" -> 5;
      case "activator" -> 6;
      default -> 8;
    };
  }

  /** Losango = quadrado girado 45 graus. */
  public static double baseRotationFor(String category) {
    return "modifier".equals(category) ? 45.0 : (sidesFor(category) == 3 ? -90.0 : 0.0);
  }

  /** Rotacao extra deterministica (0..59 graus) a partir do id, para a assinatura visual da magia. */
  public static double rotationFor(String glyphId) {
    int h = 0;
    for (int i = 0; i < glyphId.length(); i++) h = h * 31 + glyphId.charAt(i);
    return Math.floorMod(h, 60);
  }

  /** Raio do anel i de n, crescendo de ~20% a 100% de maxRadius (o primeiro glifo fica no centro). */
  public static double radiusOf(int index, int count, double maxRadius) {
    if (count <= 1) return maxRadius * 0.6;
    double t = (double) index / (count - 1);
    return maxRadius * (0.2 + 0.8 * t);
  }

  public static List<int[]> polygon(int sides, double cx, double cy, double radius, double rotationDeg) {
    List<int[]> pts = new ArrayList<>();
    for (int i = 0; i < sides; i++) {
      double a = Math.toRadians(rotationDeg + 360.0 * i / sides);
      pts.add(new int[] {(int) Math.round(cx + Math.cos(a) * radius), (int) Math.round(cy + Math.sin(a) * radius)});
    }
    return pts;
  }

  /** Pixels de uma linha (Bresenham). */
  public static List<int[]> line(int x0, int y0, int x1, int y1) {
    List<int[]> cells = new ArrayList<>();
    int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
    int err = dx + dy;
    int x = x0, y = y0;
    while (true) {
      cells.add(new int[] {x, y});
      if (x == x1 && y == y1) break;
      int e2 = 2 * err;
      if (e2 >= dy) {
        err += dy;
        x += sx;
      }
      if (e2 <= dx) {
        err += dx;
        y += sy;
      }
    }
    return cells;
  }

  /** Todos os pixels do contorno do poligono do anel. */
  public static List<int[]> ring(String category, String glyphId, double cx, double cy, double radius) {
    List<int[]> pts = polygon(sidesFor(category), cx, cy, radius, baseRotationFor(category) + rotationFor(glyphId));
    List<int[]> cells = new ArrayList<>();
    for (int i = 0; i < pts.size(); i++) {
      int[] a = pts.get(i), b = pts.get((i + 1) % pts.size());
      cells.addAll(line(a[0], a[1], b[0], b[1]));
    }
    return cells;
  }
}
