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

import java.util.ArrayList;
import java.util.List;

/**
 * Feixe 3D do Raio: uma barra retangular solida na linha da mira, com nucleo branco e casca na cor do elemento,
 * que surge inteira e encolhe ate desaparecer.
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
      pose.pushPose();
      pose.translate(-cam.x, -cam.y, -cam.z);
      for (Beam b : BEAMS) draw(buffers, pose, b, event.getPartialTick());
      pose.popPose();
      buffers.endBatch(RenderType.lightning());
    }
  }

  /**
   * Barra retangular solida: surge inteira e vai encolhendo (largura e altura) ate sumir. Dois paralelepipedos
   * (casca colorida translucida e nucleo branco), cada face com um brilho diferente para dar volume.
   */
  private static void draw(MultiBufferSource.BufferSource buffers, PoseStack pose, Beam b, float partial) {
    float life = Math.min(1f, (b.age + partial) / b.total);
    float scale = 1.0f - life * life; // cheia no inicio, encolhe ate 0
    if (scale <= 0.02f) return;
    Vec3 delta = b.to.subtract(b.from);
    double length = delta.length();
    if (length < 0.2) return;
    Vec3 dir = delta.scale(1.0 / length);
    Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
    Vec3 u = dir.cross(up).normalize();
    Vec3 v = dir.cross(u).normalize();
    int r = (b.rgb >> 16) & 255, g = (b.rgb >> 8) & 255, bl = b.rgb & 255;
    VertexConsumer c = buffers.getBuffer(RenderType.lightning());
    PoseStack.Pose last = pose.last();
    box(c, last, b.from, b.to, u, v, 0.16f * scale, r, g, bl, 90);
    box(c, last, b.from, b.to, u, v, 0.06f * scale, 255, 255, 255, 255);
  }

  private static void box(VertexConsumer c, PoseStack.Pose pose, Vec3 from, Vec3 to, Vec3 u, Vec3 v, float w, int r, int g, int b, int a) {
    Vec3 uw = u.scale(w), vw = v.scale(w);
    Vec3[] f = {from.add(uw).add(vw), from.subtract(uw).add(vw), from.subtract(uw).subtract(vw), from.add(uw).subtract(vw)};
    Vec3[] t = {to.add(uw).add(vw), to.subtract(uw).add(vw), to.subtract(uw).subtract(vw), to.add(uw).subtract(vw)};
    float[] shade = {1.0f, 0.62f, 0.8f, 0.45f};
    for (int i = 0; i < 4; i++) {
      int j = (i + 1) % 4;
      float k = shade[i];
      face(c, pose, f[i], f[j], t[j], t[i], (int) (r * k), (int) (g * k), (int) (b * k), a);
    }
    face(c, pose, f[3], f[2], f[1], f[0], (int) (r * 0.9f), (int) (g * 0.9f), (int) (b * 0.9f), a); // tampa de tras
    face(c, pose, t[0], t[1], t[2], t[3], r, g, b, a); // ponta
  }

  /** Cada face e desenhada nos dois sentidos, para nao sumir por culling. */
  private static void face(VertexConsumer c, PoseStack.Pose pose, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, int r, int g, int b, int a) {
    vtx(c, pose, p1, r, g, b, a);
    vtx(c, pose, p2, r, g, b, a);
    vtx(c, pose, p3, r, g, b, a);
    vtx(c, pose, p4, r, g, b, a);
    vtx(c, pose, p4, r, g, b, a);
    vtx(c, pose, p3, r, g, b, a);
    vtx(c, pose, p2, r, g, b, a);
    vtx(c, pose, p1, r, g, b, a);
  }

  private static void vtx(VertexConsumer c, PoseStack.Pose pose, Vec3 p, int r, int g, int b, int a) {
    c.vertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).endVertex();
  }
}
