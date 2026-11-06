package com.arcane.ritual;

import java.util.HashSet;
import java.util.Set;

/**
 * Molde do circulo ritual (geometria pura, sem Minecraft). Em torno do altar (0,0):
 *   - anel externo no raio R;
 *   - anel medio no raio R/2;
 *   - quatro raios cardeais do altar ate o anel externo;
 *   - quatro raios diagonais do anel medio ao externo.
 * A simetria de 4 lados faz o circulo ficar bonito e facil de conferir.
 */
public final class RitualTemplate {
  /** Raios dos circulos: menor, medio e maior. */
  public static final int[] RADII = {4, 8, 16};

  private RitualTemplate() {}

  public record Cell(int dx, int dz) {}

  public static Set<Cell> cells(int radius) {
    Set<Cell> cells = new HashSet<>();
    ring(cells, radius);
    ring(cells, Math.max(2, radius / 2));
    for (int i = 1; i <= radius; i++) {
      cells.add(new Cell(i, 0));
      cells.add(new Cell(-i, 0));
      cells.add(new Cell(0, i));
      cells.add(new Cell(0, -i));
    }
    int mid = Math.max(2, radius / 2);
    for (int i = (int) Math.round(mid / Math.sqrt(2)); i <= Math.round(radius / Math.sqrt(2)); i++) {
      cells.add(new Cell(i, i));
      cells.add(new Cell(-i, i));
      cells.add(new Cell(i, -i));
      cells.add(new Cell(-i, -i));
    }
    cells.remove(new Cell(0, 0));
    return cells;
  }

  private static void ring(Set<Cell> out, int radius) {
    for (int x = -radius; x <= radius; x++) {
      for (int z = -radius; z <= radius; z++) {
        if (Math.round(Math.sqrt((double) x * x + (double) z * z)) == radius) out.add(new Cell(x, z));
      }
    }
  }
}
