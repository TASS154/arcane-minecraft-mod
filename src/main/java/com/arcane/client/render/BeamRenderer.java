package com.arcane.client.render;

import com.arcane.ArcaneMod;
import com.arcane.execution.SpellElement;
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
 * Feixe 3D do Raio: um circulo arcano abre no ponto de origem (que muda a cada conjuracao) e dele sai uma barra
 * retangular solida ate o ponto mirado, com nucleo branco e casca na cor do elemento. A barra e o circulo
 * encolhem juntos ate sumir, e particulas do elemento saem ao longo da barra, do circulo e do impacto.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public class BeamRenderer {
  private static final List<Beam> BEAMS = new ArrayList<>();

  private static final class Beam {
    final Vec3 from;
    final Vec3 to;
    final int rgb;
    final SpellElement element;
    final int total;
    final Vec3 dir;
    final Vec3 u;
    final Vec3 v;
    final double length;
    int age;

    Beam(Vec3 from, Vec3 to, int rgb, SpellElement element, int total) {
      this.from = from;
      this.to = to;
      this.rgb = rgb;
      this.element = element;
      this.total = total;
      Vec3 delta = to.subtract(from);
      this.length = delta.length();
      this.dir = length < 1.0e-4 ? new Vec3(0, 0, 1) : delta.scale(1.0 / length);
      Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
      this.u = dir.cross(up).normalize();
      this.v = dir.cross(u).normalize();
    }

    float life(float partial) { return Math.min(1f, (age + partial) / total); }

    /** 1 no inicio, 0 no fim: a barra e o circulo encolhem juntos. */
    float shrink(float partial) { float l = life(partial); return 1.0f - l * l; }
  }

  public static void add(Vec3 from, Vec3 to, int rgb, int element, int ticks) {
    synchronized (BEAMS) {
      if (BEAMS.size() < 64) BEAMS.add(new Beam(from, to, rgb, SpellElement.byOrdinal(element), Math.max(2, ticks)));
    }
  }

  @SubscribeEvent
  public static void tick(TickEvent.ClientTickEvent e) {
    if (e.phase != TickEvent.Phase.END) return;
    Minecraft mc = Minecraft.getInstance();
    synchronized (BEAMS) {
      for (Beam b : BEAMS) {
        if (mc.level != null && b.shrink(0f) > 0.15f) emit(mc, b);
      }
      BEAMS.removeIf(b -> ++b.age >= b.total);
    }
  }

  /** Particulas: ao longo da barra (saem para os lados), em volta do circulo e no impacto. */
  private static void emit(Minecraft mc, Beam b) {
    var level = mc.level;
    var rnd = level.random;
    var particle = b.element.particle();
    for (int i = 0; i < 4; i++) {
      double t = rnd.nextDouble();
      double side = rnd.nextDouble() * Math.PI * 2;
      Vec3 out = b.u.scale(Math.cos(side)).add(b.v.scale(Math.sin(side)));
      Vec3 p = b.from.add(b.dir.scale(b.length * t)).add(out.scale(0.12));
      Vec3 vel = out.scale(0.03 + rnd.nextDouble() * 0.04).add(b.dir.scale(0.02));
      level.addParticle(particle, p.x, p.y, p.z, vel.x, vel.y, vel.z);
    }
    // anel de origem: faiscas girando e subindo do circulo
    double radius = 0.55 * b.shrink(0f);
    for (int i = 0; i < 3; i++) {
      double a = rnd.nextDouble() * Math.PI * 2;
      Vec3 p = b.from.add(b.u.scale(Math.cos(a) * radius)).add(b.v.scale(Math.sin(a) * radius));
      Vec3 vel = b.dir.scale(0.06 + rnd.nextDouble() * 0.05);
      level.addParticle(particle, p.x, p.y, p.z, vel.x, vel.y, vel.z);
    }
    // impacto
    for (int i = 0; i < 2; i++) {
      Vec3 vel = new Vec3(rnd.nextGaussian() * 0.06, rnd.nextDouble() * 0.08, rnd.nextGaussian() * 0.06);
      level.addParticle(particle, b.to.x, b.to.y, b.to.z, vel.x, vel.y, vel.z);
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
      VertexConsumer c = buffers.getBuffer(RenderType.lightning());
      for (Beam b : BEAMS) draw(c, pose.last(), b, event.getPartialTick());
      pose.popPose();
      buffers.endBatch(RenderType.lightning());
    }
  }

  private static void draw(VertexConsumer c, PoseStack.Pose pose, Beam b, float partial) {
    float shrink = b.shrink(partial);
    if (shrink <= 0.02f || b.length < 0.2) return;
    int r = (b.rgb >> 16) & 255, g = (b.rgb >> 8) & 255, bl = b.rgb & 255;

    // barra: nasce no circulo, so cresce depois que ele abriu (2 ticks)
    float open = Math.min(1f, (b.age + partial) / 1.5f);
    Vec3 end = b.from.add(b.dir.scale(b.length * open));
    box(c, pose, b.from, end, b.u, b.v, 0.16f * shrink, r, g, bl, 90);
    box(c, pose, b.from, end, b.u, b.v, 0.06f * shrink, 255, 255, 255, 255);

    // circulo de origem: abre rapido, gira, encolhe junto com a barra
    float grow = Math.min(1f, (b.age + partial) / 2.5f);
    float s = grow * shrink;
    float spin = (b.age + partial) * 0.35f;
    ring(c, pose, b, 0.62f * s, 0.05f * s, spin, 28, false, r, g, bl, 235);
    ring(c, pose, b, 0.40f * s, 0.04f * s, -spin * 1.4f, 20, true, 255, 255, 255, 230);
    disc(c, pose, b, 0.34f * s, r, g, bl, (int) (70 * grow));
  }

  /** Anel (ou anel tracejado) no plano perpendicular ao feixe. */
  private static void ring(VertexConsumer c, PoseStack.Pose pose, Beam b, float radius, float half, float spin, int seg,
                           boolean dashed, int r, int g, int bl, int a) {
    if (radius <= 0.01f) return;
    for (int i = 0; i < seg; i++) {
      if (dashed && i % 2 == 1) continue;
      float a0 = spin + i * 6.2831855f / seg, a1 = spin + (i + 1) * 6.2831855f / seg;
      Vec3 o0 = pt(b, a0, radius + half), i0 = pt(b, a0, radius - half);
      Vec3 o1 = pt(b, a1, radius + half), i1 = pt(b, a1, radius - half);
      face(c, pose, o0, o1, i1, i0, r, g, bl, a);
    }
  }

  private static void disc(VertexConsumer c, PoseStack.Pose pose, Beam b, float radius, int r, int g, int bl, int a) {
    if (radius <= 0.01f || a <= 0) return;
    int seg = 16;
    for (int i = 0; i < seg; i++) {
      float a0 = i * 6.2831855f / seg, a1 = (i + 1) * 6.2831855f / seg;
      face(c, pose, b.from, pt(b, a0, radius), pt(b, a1, radius), b.from, r, g, bl, a);
    }
  }

  private static Vec3 pt(Beam b, float angle, float radius) {
    return b.from.add(b.u.scale(Math.cos(angle) * radius)).add(b.v.scale(Math.sin(angle) * radius));
  }

  /** Paralelepipedo ao longo de from->to; cada face com brilho proprio para dar volume. */
  static void box(VertexConsumer c, PoseStack.Pose pose, Vec3 from, Vec3 to, Vec3 u, Vec3 v, float w, int r, int g, int b, int a) {
    Vec3 uw = u.scale(w), vw = v.scale(w);
    Vec3[] f = {from.add(uw).add(vw), from.subtract(uw).add(vw), from.subtract(uw).subtract(vw), from.add(uw).subtract(vw)};
    Vec3[] t = {to.add(uw).add(vw), to.subtract(uw).add(vw), to.subtract(uw).subtract(vw), to.add(uw).subtract(vw)};
    float[] shade = {1.0f, 0.62f, 0.8f, 0.45f};
    for (int i = 0; i < 4; i++) {
      int j = (i + 1) % 4;
      float k = shade[i];
      face(c, pose, f[i], f[j], t[j], t[i], (int) (r * k), (int) (g * k), (int) (b * k), a);
    }
    face(c, pose, f[3], f[2], f[1], f[0], (int) (r * 0.9f), (int) (g * 0.9f), (int) (b * 0.9f), a);
    face(c, pose, t[0], t[1], t[2], t[3], r, g, b, a);
  }

  /** Cada face e desenhada nos dois sentidos, para nao sumir por culling. */
  static void face(VertexConsumer c, PoseStack.Pose pose, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, int r, int g, int b, int a) {
    vtx(c, pose, p1, r, g, b, a);
    vtx(c, pose, p2, r, g, b, a);
    vtx(c, pose, p3, r, g, b, a);
    vtx(c, pose, p4, r, g, b, a);
    vtx(c, pose, p4, r, g, b, a);
    vtx(c, pose, p3, r, g, b, a);
    vtx(c, pose, p2, r, g, b, a);
    vtx(c, pose, p1, r, g, b, a);
  }

  static void vtx(VertexConsumer c, PoseStack.Pose pose, Vec3 p, int r, int g, int b, int a) {
    c.vertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).endVertex();
  }
}
