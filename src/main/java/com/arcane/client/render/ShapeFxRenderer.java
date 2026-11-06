package com.arcane.client.render;

import com.arcane.ArcaneMod;
import com.arcane.execution.SpellElement;
import com.arcane.network.packet.S2CShapeFxPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometria 3D das formas e dos impactos: aura (anel e parede que se expandem), onda (arco), pilar (coluna com aneis
 * subindo), impacto (octaedro e estilhacos por elemento) e ritual (coluna de luz com aneis em fases).
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public class ShapeFxRenderer {
  private static final float TAU = 6.2831855f;
  private static final List<Fx> FX = new ArrayList<>();

  private static final class Fx {
    final int type;
    final Vec3 pos;
    final Vec3 dir;
    final float size;
    final int rgb;
    final SpellElement element;
    final int total;
    int age;

    Fx(int type, Vec3 pos, Vec3 dir, float size, int rgb, SpellElement element, int total) {
      this.type = type;
      this.pos = pos;
      this.dir = dir;
      this.size = size;
      this.rgb = rgb;
      this.element = element;
      this.total = total;
    }

    float life(float partial) { return Math.min(1f, (age + partial) / total); }
  }

  public static void add(int type, Vec3 pos, Vec3 dir, float size, int rgb, int element, int ticks) {
    synchronized (FX) {
      if (FX.size() < 48) FX.add(new Fx(type, pos, dir, size, rgb, SpellElement.byOrdinal(element), Math.max(4, ticks)));
    }
  }

  private static float ease(float t) { return 1f - (1f - t) * (1f - t); }

  // ---------------------------------------------------------------- particulas (tick)
  @SubscribeEvent
  public static void tick(TickEvent.ClientTickEvent e) {
    if (e.phase != TickEvent.Phase.END) return;
    Minecraft mc = Minecraft.getInstance();
    synchronized (FX) {
      if (mc.level != null) for (Fx f : FX) emit(mc, f);
      FX.removeIf(f -> ++f.age >= f.total);
    }
  }

  private static void emit(Minecraft mc, Fx f) {
    var level = mc.level;
    var rnd = level.random;
    var particle = f.element.particle();
    float life = f.life(0f);
    switch (f.type) {
      case S2CShapeFxPacket.AURA -> {
        double r = f.size * ease(life);
        for (int i = 0; i < 8; i++) {
          double a = rnd.nextDouble() * TAU;
          level.addParticle(particle, f.pos.x + Math.cos(a) * r, f.pos.y + 0.15, f.pos.z + Math.sin(a) * r, 0, 0.05 + rnd.nextDouble() * 0.05, 0);
        }
      }
      case S2CShapeFxPacket.WAVE -> {
        double r = f.size * ease(life);
        double yaw = Math.atan2(f.dir.z, f.dir.x);
        for (int i = 0; i < 8; i++) {
          double a = yaw + Math.toRadians((rnd.nextDouble() - 0.5) * 100);
          level.addParticle(particle, f.pos.x + Math.cos(a) * r, f.pos.y + 0.6 + rnd.nextDouble() * 0.6, f.pos.z + Math.sin(a) * r,
              Math.cos(a) * 0.1, 0.02, Math.sin(a) * 0.1);
        }
      }
      case S2CShapeFxPacket.PILLAR -> {
        for (int i = 0; i < 6; i++) {
          double a = rnd.nextDouble() * TAU, r = f.size * 0.5 * rnd.nextDouble();
          level.addParticle(particle, f.pos.x + Math.cos(a) * r, f.pos.y + rnd.nextDouble() * 9, f.pos.z + Math.sin(a) * r, 0, 0.08, 0);
        }
      }
      case S2CShapeFxPacket.IMPACT -> {
        if (f.age == 0) {
          for (int i = 0; i < 18; i++) {
            Vec3 d = new Vec3(rnd.nextGaussian(), rnd.nextGaussian() * 0.7 + 0.2, rnd.nextGaussian()).normalize().scale(0.12 + rnd.nextDouble() * 0.12);
            level.addParticle(particle, f.pos.x, f.pos.y, f.pos.z, d.x, d.y, d.z);
          }
        }
      }
      case S2CShapeFxPacket.RITUAL -> {
        double r = f.size * ease(Math.min(1f, life * 1.6f));
        for (int i = 0; i < 12; i++) {
          double a = rnd.nextDouble() * TAU;
          level.addParticle(particle, f.pos.x + Math.cos(a) * r, f.pos.y + 0.1, f.pos.z + Math.sin(a) * r, 0, 0.1, 0);
        }
        for (int i = 0; i < 4; i++) {
          level.addParticle(particle, f.pos.x + (rnd.nextDouble() - 0.5) * 0.5, f.pos.y + rnd.nextDouble() * 16, f.pos.z + (rnd.nextDouble() - 0.5) * 0.5, 0, 0.1, 0);
        }
      }
      default -> { }
    }
  }

  // ---------------------------------------------------------------- render
  @SubscribeEvent
  public static void render(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
    synchronized (FX) {
      if (FX.isEmpty()) return;
      Minecraft mc = Minecraft.getInstance();
      PoseStack pose = event.getPoseStack();
      Vec3 cam = event.getCamera().getPosition();
      MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
      pose.pushPose();
      pose.translate(-cam.x, -cam.y, -cam.z);
      VertexConsumer c = buffers.getBuffer(RenderType.lightning());
      PoseStack.Pose last = pose.last();
      for (Fx f : FX) draw(c, last, f, event.getPartialTick());
      pose.popPose();
      buffers.endBatch(RenderType.lightning());
    }
  }

  private static void draw(VertexConsumer c, PoseStack.Pose pose, Fx f, float partial) {
    float life = f.life(partial);
    float fade = 1f - life * life;
    int r = (f.rgb >> 16) & 255, g = (f.rgb >> 8) & 255, b = f.rgb & 255;
    switch (f.type) {
      case S2CShapeFxPacket.AURA -> {
        float rad = f.size * ease(life);
        wall(c, pose, f.pos, rad, 0.9f, 0f, TAU, 36, r, g, b, (int) (170 * fade), 0);
        ring(c, pose, f.pos.add(0, 0.06, 0), rad, 0.10f, 0f, TAU, 40, r, g, b, (int) (230 * fade));
        ring(c, pose, f.pos.add(0, 0.06, 0), rad * 0.7f, 0.05f, 0f, TAU, 32, 255, 255, 255, (int) (200 * fade));
      }
      case S2CShapeFxPacket.WAVE -> {
        float rad = f.size * ease(life);
        float yaw = (float) Math.atan2(f.dir.z, f.dir.x);
        float half = (float) Math.toRadians(50);
        Vec3 o = f.pos;
        wall(c, pose, o.add(0, 0.1, 0), rad, 1.2f, yaw - half, yaw + half, 20, r, g, b, (int) (190 * fade), 0);
        ring(c, pose, o.add(0, 0.1, 0), rad, 0.10f, yaw - half, yaw + half, 20, 255, 255, 255, (int) (230 * fade));
        ring(c, pose, o.add(0, 0.1, 0), rad * 0.6f, 0.06f, yaw - half, yaw + half, 14, r, g, b, (int) (160 * fade));
      }
      case S2CShapeFxPacket.PILLAR -> {
        float open = Math.min(1f, life * 6f);
        float w = f.size * 0.5f * open * fade;
        Vec3 top = f.pos.add(0, 10, 0);
        BeamRenderer.box(c, pose, f.pos, top, new Vec3(1, 0, 0), new Vec3(0, 0, 1), Math.max(0.01f, w), r, g, b, 90);
        BeamRenderer.box(c, pose, f.pos, top, new Vec3(1, 0, 0), new Vec3(0, 0, 1), Math.max(0.01f, w * 0.35f), 255, 255, 255, 255);
        for (int i = 0; i < 3; i++) {
          float h = ((life * 2.2f + i / 3f) % 1f) * 10f;
          ring(c, pose, f.pos.add(0, h, 0), f.size * (0.6f + 0.4f * open) * fade + 0.1f, 0.08f, (life + i) * 3f, (life + i) * 3f + TAU, 28, 255, 255, 255, (int) (220 * fade));
        }
        ring(c, pose, f.pos.add(0, 0.05, 0), f.size * ease(Math.min(1f, life * 3f)), 0.1f, 0f, TAU, 32, r, g, b, (int) (230 * fade));
      }
      case S2CShapeFxPacket.IMPACT -> {
        float rr = (0.25f + 0.9f * ease(life)) * f.size;
        octahedron(c, pose, f.pos, rr, r, g, b, (int) (200 * fade));
        octahedron(c, pose, f.pos, rr * 0.5f, 255, 255, 255, (int) (230 * fade));
        // estilhacos radiais
        int shards = f.element == SpellElement.FIRE || f.element == SpellElement.LIFE ? 6 : 10;
        for (int i = 0; i < shards; i++) {
          float y = 1f - (i + 0.5f) * 2f / shards;
          float rad = (float) Math.sqrt(Math.max(0f, 1f - y * y));
          float ang = i * 2.3999631f; // angulo aureo
          Vec3 d = new Vec3(Math.cos(ang) * rad, y, Math.sin(ang) * rad);
          float start = rr * 0.6f, end = rr * (1.4f + 0.8f * ease(life));
          Vec3 u = Math.abs(d.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
          Vec3 pu = d.cross(u).normalize(), pv = d.cross(pu).normalize();
          float w = (f.element == SpellElement.STORM ? 0.03f : 0.05f) * fade + 0.005f;
          BeamRenderer.box(c, pose, f.pos.add(d.scale(start)), f.pos.add(d.scale(end)), pu, pv, w, 255, 255, 255, (int) (220 * fade));
        }
        ring(c, pose, f.pos, rr * 1.2f, 0.05f * fade + 0.01f, 0f, TAU, 24, r, g, b, (int) (200 * fade));
      }
      case S2CShapeFxPacket.RITUAL -> drawRitual(c, pose, f, life, fade, r, g, b);
      case S2CShapeFxPacket.HOLOGRAM -> drawHologram(c, pose, f, life, r, g, b);
      default -> { }
    }
  }

  /** Um anel poligonal por glifo, empilhados e girando; surge, fica e some suavemente. */
  private static void drawHologram(VertexConsumer c, PoseStack.Pose pose, Fx f, float life, int r, int g, int b) {
    float vis = Math.min(1f, life * 8f) * Math.min(1f, (1f - life) * 6f);
    if (vis <= 0.02f) return;
    int n = Math.max(1, Math.round(f.size));
    float spin = life * 14f;
    for (int i = 0; i < n; i++) {
      float y = 0.25f * i - 0.125f * (n - 1);
      float rad = 0.55f + 0.05f * i;
      int sides = 3 + (i % 6);
      float rot = spin * (i % 2 == 0 ? 1 : -1) + i;
      // poligono: segmentos retos entre os vertices
      for (int k = 0; k < sides; k++) {
        float a0 = rot + k * TAU / sides, a1 = rot + (k + 1) * TAU / sides;
        Vec3 p0 = f.pos.add(Math.cos(a0) * rad, y, Math.sin(a0) * rad), p1 = f.pos.add(Math.cos(a1) * rad, y, Math.sin(a1) * rad);
        Vec3 dir = p1.subtract(p0);
        Vec3 u = dir.cross(new Vec3(0, 1, 0)).normalize();
        BeamRenderer.box(c, pose, p0, p1, u, new Vec3(0, 1, 0), 0.025f, i % 2 == 0 ? r : 255, i % 2 == 0 ? g : 255, i % 2 == 0 ? b : 255, (int) (230 * vis));
      }
      ring(c, pose, f.pos.add(0, y, 0), rad * 1.25f, 0.015f, spin * 0.5f, spin * 0.5f + TAU, 18, r, g, b, (int) (110 * vis));
    }
  }

  private static void drawRitual(VertexConsumer c, PoseStack.Pose pose, Fx f, float life, float fade, int r, int g, int b) {
    // fase 1 (0-35%): aneis abrem do centro para fora, um depois do outro; fase 2: coluna de luz sobe; fase 3: tudo apaga
    for (int i = 0; i < 3; i++) {
      float start = i * 0.12f;
      float t = Mth.clamp((life - start) / 0.35f, 0f, 1f);
      if (t <= 0f) continue;
      float rad = f.size * (i + 1) / 3f * ease(t);
      ring(c, pose, f.pos.add(0, 0.12, 0), rad, 0.14f, life * 2f * (i % 2 == 0 ? 1 : -1), life * 2f * (i % 2 == 0 ? 1 : -1) + TAU, 48, r, g, b, (int) (230 * fade));
    }
    float colT = Mth.clamp((life - 0.2f) / 0.2f, 0f, 1f);
    float height = 16f * ease(colT);
    float w = (0.35f + 0.25f * Mth.sin(life * 18f)) * fade;
    if (height > 0.1f && w > 0.01f) {
      Vec3 top = f.pos.add(0, height, 0);
      BeamRenderer.box(c, pose, f.pos, top, new Vec3(1, 0, 0), new Vec3(0, 0, 1), w, r, g, b, 90);
      BeamRenderer.box(c, pose, f.pos, top, new Vec3(1, 0, 0), new Vec3(0, 0, 1), w * 0.35f, 255, 255, 255, 255);
    }
    // aneis de runas subindo pela coluna
    for (int i = 0; i < 4; i++) {
      float h = ((life * 1.5f + i * 0.25f) % 1f) * height;
      ring(c, pose, f.pos.add(0, h, 0), 1.0f * fade + 0.2f, 0.06f, life * 6f + i, life * 6f + i + TAU, 12, 255, 255, 255, (int) (200 * fade));
    }
  }

  // ---------------------------------------------------------------- geometria
  private static Vec3 pt(Vec3 center, float angle, float radius, float y) {
    return new Vec3(center.x + Math.cos(angle) * radius, center.y + y, center.z + Math.sin(angle) * radius);
  }

  /** Faixa horizontal (anel ou arco) de largura 2*half no plano XZ. */
  private static void ring(VertexConsumer c, PoseStack.Pose pose, Vec3 center, float radius, float half, float a0, float a1, int seg,
                           int r, int g, int b, int a) {
    if (radius <= 0.02f || a <= 0) return;
    for (int i = 0; i < seg; i++) {
      float s0 = Mth.lerp(i / (float) seg, a0, a1), s1 = Mth.lerp((i + 1) / (float) seg, a0, a1);
      BeamRenderer.face(c, pose, pt(center, s0, radius + half, 0), pt(center, s1, radius + half, 0),
          pt(center, s1, radius - half, 0), pt(center, s0, radius - half, 0), r, g, b, a);
    }
  }

  /** Parede vertical (cilindro ou fatia) que some para cima. */
  private static void wall(VertexConsumer c, PoseStack.Pose pose, Vec3 center, float radius, float height, float a0, float a1, int seg,
                           int r, int g, int b, int alphaBottom, int alphaTop) {
    if (radius <= 0.02f || alphaBottom <= 0) return;
    for (int i = 0; i < seg; i++) {
      float s0 = Mth.lerp(i / (float) seg, a0, a1), s1 = Mth.lerp((i + 1) / (float) seg, a0, a1);
      Vec3 b0 = pt(center, s0, radius, 0), b1 = pt(center, s1, radius, 0);
      Vec3 t0 = pt(center, s0, radius, height), t1 = pt(center, s1, radius, height);
      gradQuad(c, pose, b0, b1, t1, t0, r, g, b, alphaBottom, alphaTop);
    }
  }

  private static void gradQuad(VertexConsumer c, PoseStack.Pose pose, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, int r, int g, int b, int a1, int a2) {
    BeamRenderer.vtx(c, pose, p1, r, g, b, a1);
    BeamRenderer.vtx(c, pose, p2, r, g, b, a1);
    BeamRenderer.vtx(c, pose, p3, r, g, b, a2);
    BeamRenderer.vtx(c, pose, p4, r, g, b, a2);
    BeamRenderer.vtx(c, pose, p4, r, g, b, a2);
    BeamRenderer.vtx(c, pose, p3, r, g, b, a2);
    BeamRenderer.vtx(c, pose, p2, r, g, b, a1);
    BeamRenderer.vtx(c, pose, p1, r, g, b, a1);
  }

  private static void octahedron(VertexConsumer c, PoseStack.Pose pose, Vec3 o, float s, int r, int g, int b, int a) {
    if (a <= 0 || s <= 0.01f) return;
    Vec3[] p = {o.add(s, 0, 0), o.add(-s, 0, 0), o.add(0, s, 0), o.add(0, -s, 0), o.add(0, 0, s), o.add(0, 0, -s)};
    int[][] tri = {{0, 2, 4}, {4, 2, 1}, {1, 2, 5}, {5, 2, 0}, {0, 3, 4}, {4, 3, 1}, {1, 3, 5}, {5, 3, 0}};
    float[] shade = {1f, 0.8f, 0.6f, 0.9f, 0.7f, 0.5f, 0.85f, 0.65f};
    for (int i = 0; i < tri.length; i++) {
      float k = shade[i];
      BeamRenderer.face(c, pose, p[tri[i][0]], p[tri[i][1]], p[tri[i][2]], p[tri[i][2]], (int) (r * k), (int) (g * k), (int) (b * k), a);
    }
  }
}
