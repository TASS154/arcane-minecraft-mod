package com.arcane.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

/**
 * Circulo magico com runas, desenhado num plano qualquer (u, v): dois aneis, uma faixa de runas (tracos curtos que
 * variam de runa para runa) e uma estrela de seis pontas por dentro. Usado na base do Pilar e na origem do Raio.
 */
final class RuneCircle {
  private static final float TAU = 6.2831855f;
  private static final int RUNES = 12;

  private RuneCircle() {}

  static void draw(VertexConsumer c, PoseStack.Pose pose, Vec3 center, Vec3 u, Vec3 v, float radius, float spin,
                   int r, int g, int b, int alpha) {
    if (radius <= 0.05f || alpha <= 0) return;
    float w = Math.max(0.012f, radius * 0.018f);
    Vec3 n = u.cross(v).normalize();
    // aneis externo e interno da faixa de runas
    ring(c, pose, center, u, v, radius, w * 1.4f, 0f, 48, r, g, b, alpha);
    ring(c, pose, center, u, v, radius * 0.78f, w, 0f, 40, r, g, b, alpha);
    ring(c, pose, center, u, v, radius * 0.30f, w * 0.8f, spin * 1.5f, 24, 255, 255, 255, (int) (alpha * 0.8f));
    // runas: cada uma e um conjunto de tracos entre os dois aneis (padrao decidido pelo indice)
    for (int i = 0; i < RUNES; i++) {
      float a = spin + i * TAU / RUNES;
      float lo = radius * 0.82f, hi = radius * 0.96f, mid = (lo + hi) * 0.5f;
      int pattern = (i * 5 + 3) % 7;
      // haste radial
      stroke(c, pose, center, u, v, a, lo, a, hi, w, n, 255, 255, 255, alpha);
      float da = 0.11f;
      if ((pattern & 1) != 0) stroke(c, pose, center, u, v, a, mid, a + da, hi, w, n, r, g, b, alpha);
      if ((pattern & 2) != 0) stroke(c, pose, center, u, v, a, mid, a - da, hi, w, n, r, g, b, alpha);
      if ((pattern & 4) != 0) stroke(c, pose, center, u, v, a - da, lo, a + da, lo, w, n, r, g, b, alpha);
      if (pattern == 0 || pattern == 6) stroke(c, pose, center, u, v, a - da * 0.8f, mid, a + da * 0.8f, mid, w, n, 255, 255, 255, alpha);
    }
    // hexagrama (dois triangulos) girando ao contrario
    float in = radius * 0.72f;
    for (int t = 0; t < 2; t++) {
      for (int k = 0; k < 3; k++) {
        float a0 = -spin * 0.7f + t * TAU / 6f + k * TAU / 3f, a1 = a0 + TAU / 3f;
        stroke(c, pose, center, u, v, a0, in, a1, in, w, n, r, g, b, alpha);
      }
    }
  }

  /** Traco entre dois pontos dados em (angulo, raio). */
  private static void stroke(VertexConsumer c, PoseStack.Pose pose, Vec3 center, Vec3 u, Vec3 v, float a0, float r0, float a1, float r1,
                             float w, Vec3 n, int r, int g, int b, int alpha) {
    Vec3 p0 = pt(center, u, v, a0, r0), p1 = pt(center, u, v, a1, r1);
    Vec3 d = p1.subtract(p0);
    if (d.lengthSqr() < 1e-8) return;
    Vec3 side = d.cross(n).normalize();
    BeamRenderer.face(c, pose, p0.add(side.scale(w)), p1.add(side.scale(w)), p1.subtract(side.scale(w)), p0.subtract(side.scale(w)), r, g, b, alpha);
  }

  private static void ring(VertexConsumer c, PoseStack.Pose pose, Vec3 center, Vec3 u, Vec3 v, float radius, float half, float spin,
                           int seg, int r, int g, int b, int a) {
    for (int i = 0; i < seg; i++) {
      float s0 = spin + i * TAU / seg, s1 = spin + (i + 1) * TAU / seg;
      BeamRenderer.face(c, pose, pt(center, u, v, s0, radius + half), pt(center, u, v, s1, radius + half),
          pt(center, u, v, s1, radius - half), pt(center, u, v, s0, radius - half), r, g, b, a);
    }
  }

  private static Vec3 pt(Vec3 center, Vec3 u, Vec3 v, float angle, float radius) {
    return center.add(u.scale(Math.cos(angle) * radius)).add(v.scale(Math.sin(angle) * radius));
  }
}
