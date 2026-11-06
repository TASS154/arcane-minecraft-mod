package com.arcane.client.render;

import com.arcane.ArcaneMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Feixe 3D do Raio: uma fita volumetrica (dois planos cruzados) em volta da linha da mira, com nucleo branco,
 * brilho na cor do elemento, tremor em zigue-zague (mais forte na tempestade) e esmaecimento em poucos ticks.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public class BeamRenderer {
  private static final int SEGMENT = 1; // blocos por segmento
  private static final List<Beam> BEAMS = new ArrayList<>();

  private static final class Beam {
    final Vec3 from;
    final Vec3 to;
    final int rgb;
    final boolean jagged;
    final int total;
    int age;
    final long seed;

    Beam(Vec3 from, Vec3 to, int rgb, boolean jagged, int total) {
      this.from = from;
      this.to = to;
      this.rgb = rgb;
      this.jagged = jagged;
      this.total = total;
      this.seed = Double.doubleToLongBits(from.x + to.z * 31.0);
    }
  }

  public static void add(Vec3 from, Vec3 to, int rgb, boolean jagged, int ticks) {
    synchronized (BEAMS) {
      if (BEAMS.size() < 64) BEAMS.add(new Beam(from, to, rgb, jagged, Math.max(2, ticks)));
    }
  }

  @SubscribeEvent
  public static void tick(TickEvent.ClientTickEvent e) {
    if (e.phase != TickEvent.Phase.END) return;
    synchronized (BEAMS) {
      BEAMS.removeIf(b -> ++b.age >= b.total);
    }
  }

  @SubscribeEvent
  public static void render(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
    synchronized (BEAMS) {
      if (BEAMS.isEmpty()) return;
      Minecraft mc = Minecraft.getInstance();
      PoseStack pose = event.getPoseStack();
      Vec3 cam = event.getCamera().getPosition();
      MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
      VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
      pose.pushPose();
      pose.translate(-cam.x, -cam.y, -cam.z);
      Matrix4f m = pose.last().pose();
      for (Beam b : BEAMS) draw(consumer, m, b, event.getPartialTick());
      pose.popPose();
      buffers.endBatch(RenderType.lightning());
    }
  }

  private static void draw(VertexConsumer c, Matrix4f m, Beam b, float partial) {
    float life = (b.age + partial) / b.total;
    float fade = 1.0f - life * life;
    Vec3 delta = b.to.subtract(b.from);
    double length = delta.length();
    if (length < 0.2) return;
    Vec3 dir = delta.scale(1.0 / length);
    Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
    Vec3 u = dir.cross(up).normalize();
    Vec3 v = dir.cross(u).normalize();

    int segments = Math.max(1, (int) Math.ceil(length / SEGMENT));
    Random rnd = new Random(b.seed + (long) ((b.age + partial) * 2.0f)); // retrema a cada meio tick
    double amp = b.jagged ? 0.22 : 0.05;
    Vec3[] pts = new Vec3[segments + 1];
    for (int i = 0; i <= segments; i++) {
      Vec3 p = b.from.add(delta.scale((double) i / segments));
      if (i > 0 && i < segments) {
        p = p.add(u.scale((rnd.nextDouble() - 0.5) * 2 * amp)).add(v.scale((rnd.nextDouble() - 0.5) * 2 * amp));
      }
      pts[i] = p;
    }
    float pulse = 0.85f + 0.15f * (float) Math.sin((b.age + partial) * 1.7f);
    int r = (b.rgb >> 16) & 255, g = (b.rgb >> 8) & 255, bl = b.rgb & 255;
    for (int i = 0; i < segments; i++) {
      double dist = i * length / segments;
      // fino perto de quem atira (nao tampa a tela), cheio a partir de 3 blocos, afina de leve na ponta
      float taper = (float) Math.min(1.0, Math.max(0.1, dist / 3.0)) * (1.0f - 0.3f * (float) i / segments);
      ribbons(c, m, pts[i], pts[i + 1], u, v, 0.30f * pulse * taper, r, g, bl, (int) (70 * fade));  // halo
      ribbons(c, m, pts[i], pts[i + 1], u, v, 0.15f * pulse * taper, r, g, bl, (int) (150 * fade)); // corpo
      ribbons(c, m, pts[i], pts[i + 1], u, v, 0.06f * taper, 255, 255, 255, (int) (255 * fade));   // nucleo
    }
  }

  /** Dois planos cruzados (u e v) de largura 2*w: de qualquer angulo a fita parece ter volume. */
  private static void ribbons(VertexConsumer c, Matrix4f m, Vec3 a, Vec3 b, Vec3 u, Vec3 v, float w, int r, int g, int bl, int alpha) {
    if (alpha <= 0) return;
    quad(c, m, a.add(u.scale(w)), a.subtract(u.scale(w)), b.subtract(u.scale(w)), b.add(u.scale(w)), r, g, bl, alpha);
    quad(c, m, a.add(v.scale(w)), a.subtract(v.scale(w)), b.subtract(v.scale(w)), b.add(v.scale(w)), r, g, bl, alpha);
  }

  private static void quad(VertexConsumer c, Matrix4f m, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, int r, int g, int b, int a) {
    vtx(c, m, p1, r, g, b, a);
    vtx(c, m, p2, r, g, b, a);
    vtx(c, m, p3, r, g, b, a);
    vtx(c, m, p4, r, g, b, a);
  }

  private static void vtx(VertexConsumer c, Matrix4f m, Vec3 p, int r, int g, int b, int a) {
    c.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).endVertex();
  }
}
