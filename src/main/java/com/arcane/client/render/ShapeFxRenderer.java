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
  /** Altura de onde a coluna cai (ceu). */
  private static final float SKY = 48f;
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
    /** Efeito de magia mantida: abre uma vez, fica de pe e so some quando para de ser renovado. */
    boolean sustained;
    int born;

    Fx(int type, Vec3 pos, Vec3 dir, float size, int rgb, SpellElement element, int total) {
      this.type = type;
      this.pos = pos;
      this.dir = dir;
      this.size = size;
      this.rgb = rgb;
      this.element = element;
      this.total = total;
    }

    float life(float partial) {
      if (sustained) {
        float open = Math.min(1f, (born + partial) / 10f);
        return type == S2CShapeFxPacket.PILLAR ? 0.3f * open : open;
      }
      return Math.min(1f, (age + partial) / total);
    }

    /** Transparencia: constante enquanto renovado; apaga nos ultimos ticks depois que o conjurador solta. */
    float fade(float partial) {
      if (!sustained) { float l = life(partial); return 1f - l * l; }
      float remaining = total - age - partial;
      return 0.85f * Math.min(1f, Math.max(0f, remaining / 3f));
    }
  }

  public static void add(int type, Vec3 pos, Vec3 dir, float size, int rgb, int element, int ticks) {
    if (type == S2CShapeFxPacket.ORIGINAL) { OriginalFxRenderer.add(element, pos, dir, size, rgb, ticks); return; }
    var mc = Minecraft.getInstance();
    if (mc.player != null) {
      double d = mc.player.position().distanceTo(pos);
      if (type == S2CShapeFxPacket.IMPACT) com.arcane.client.CameraShake.add(Math.min(0.7f, 0.15f + size * 0.1f), d);
      else if (type == S2CShapeFxPacket.RITUAL) com.arcane.client.CameraShake.add(0.5f, d);
      else if (type == S2CShapeFxPacket.AURA && size > 2.5f) com.arcane.client.CameraShake.add(Math.min(0.4f, size * 0.05f), d);
    }
    boolean sustain = ticks >= S2CShapeFxPacket.SUSTAIN;
    if (sustain) ticks -= S2CShapeFxPacket.SUSTAIN;
    synchronized (FX) {
      if (sustain) {
        for (Fx f : FX) {
          if (f.sustained && f.type == type && f.pos.distanceToSqr(pos) < 0.01) { f.age = 0; return; } // renova o que ja esta de pe
        }
      }
      if (FX.size() < 48) {
        Fx fx = new Fx(type, pos, dir, size, rgb, SpellElement.byOrdinal(element), Math.max(4, ticks));
        fx.sustained = sustain;
        FX.add(fx);
      }
    }
  }

  static float ease(float t) { return 1f - (1f - t) * (1f - t); }

  // ---------------------------------------------------------------- particulas (tick)
  @SubscribeEvent
  public static void tick(TickEvent.ClientTickEvent e) {
    if (e.phase != TickEvent.Phase.END) return;
    Minecraft mc = Minecraft.getInstance();
    synchronized (FX) {
      if (mc.level != null) for (Fx f : FX) emit(mc, f);
      for (Fx f : FX) f.born++;
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
          level.addParticle(particle, f.pos.x + Math.cos(a) * r, f.pos.y + rnd.nextDouble() * 9, f.pos.z + Math.sin(a) * r, 0, -0.12, 0);
        }
      }
      case S2CShapeFxPacket.CAST -> {
        // motes de luz sobem em espiral em volta do conjurador
        for (int i = 0; i < 2; i++) {
          double a = life * 18.0 + i * Math.PI + rnd.nextDouble() * 0.4;
          double r = f.size * (0.9 - 0.5 * life);
          level.addParticle(particle, f.pos.x + Math.cos(a) * r, f.pos.y + 0.1 + life * 1.8, f.pos.z + Math.sin(a) * r, 0, 0.05, 0);
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
          level.addParticle(particle, f.pos.x + (rnd.nextDouble() - 0.5) * 0.5, f.pos.y + rnd.nextDouble() * 16, f.pos.z + (rnd.nextDouble() - 0.5) * 0.5, 0, -0.15, 0);
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
    float fade = f.fade(partial);
    int r = (f.rgb >> 16) & 255, g = (f.rgb >> 8) & 255, b = f.rgb & 255;
    switch (f.type) {
      case S2CShapeFxPacket.AURA -> {
        float rad = f.size * ease(life);
        wall(c, pose, f.pos, rad, 0.9f, 0f, TAU, 36, r, g, b, (int) (170 * fade), 0);
        ring(c, pose, f.pos.add(0, 0.06, 0), rad, 0.34f, 0f, TAU, 40, r, g, b, (int) (40 * fade));   // halo
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
        // A coluna DESCE do ceu: a ponta cai do alto ate o chao (primeiros ~20%), depois fica e some. Circulo de runas na base.
        float drop = Math.min(1f, life * 5f);
        float top = skyHeight(f.pos);
        float headY = top * (1f - ease(drop));
        float w = f.size * 0.5f * Math.min(1f, life * 8f) * fade;
        Vec3 bottom = f.pos.add(0, headY, 0), topV = f.pos.add(0, top, 0);
        Vec3 ux = new Vec3(1, 0, 0), vz = new Vec3(0, 0, 1);
        if (w > 0.01f) {
          // Gravidade (elemento FORCE) + coluna: luz semitransparente, arroxeada, com glitch descendo.
          if (f.element == SpellElement.FORCE) glitchColumn(c, pose, bottom, topV, w, f.age + partial, f.pos);
          else skyColumn(c, pose, bottom, topV, w, r, g, b, 1f);
        }
        for (int i = 0; i < 3; i++) {
          float h = (1f - ((life * 2.2f + i / 3f) % 1f)) * 10f; // aneis descendo junto com a luz
          if (f.pos.y + h < f.pos.y + headY) continue;
          ring(c, pose, f.pos.add(0, h, 0), f.size * 0.6f * fade + 0.1f, 0.08f, (life + i) * 3f, (life + i) * 3f + TAU, 28, 255, 255, 255, (int) (220 * fade));
        }
        float circle = Math.max(1.2f, f.size * 1.4f) * ease(Math.min(1f, life * 4f));
        RuneCircle.draw(c, pose, f.pos.add(0, 0.07, 0), ux, vz, circle, life * 4f, r, g, b, (int) (235 * Math.min(1f, fade * 1.4f)));
        if (drop >= 1f && life < 0.35f) ring(c, pose, f.pos.add(0, 0.05, 0), f.size * (0.5f + 2f * (life - 0.2f)), 0.1f, 0f, TAU, 32, 255, 255, 255, (int) (200 * (0.35f - life) * 6f));
      }
      case S2CShapeFxPacket.CAST -> {
        // circulo de runas no chao: abre rapido, gira, some; um segundo menor gira ao contrario e um anel de luz sobe
        float open = ease(Math.min(1f, life * 4f));
        float rad = Math.max(0.8f, f.size) * open;
        Vec3 ux = new Vec3(1, 0, 0), vz = new Vec3(0, 0, 1);
        RuneCircle.draw(c, pose, f.pos.add(0, 0.05, 0), ux, vz, rad, life * 5f, r, g, b, (int) (235 * fade));
        RuneCircle.draw(c, pose, f.pos.add(0, 0.06, 0), ux, vz, rad * 0.55f, -life * 7f, 255, 255, 255, (int) (200 * fade));
        ring(c, pose, f.pos.add(0, 0.05 + 1.8 * life, 0), rad * (0.9f - 0.3f * life), 0.04f, 0f, TAU, 28, r, g, b, (int) (170 * fade));
        ring(c, pose, f.pos.add(0, 0.05 + 1.8 * life, 0), rad * (0.9f - 0.3f * life) * 0.8f, 0.02f, 0f, TAU, 24, 255, 255, 255, (int) (140 * fade));
      }
      case S2CShapeFxPacket.IMPACT -> {
        float rr = (0.25f + 0.9f * ease(life)) * f.size;
        octahedron(c, pose, f.pos, rr, r, g, b, (int) (200 * fade));
        octahedron(c, pose, f.pos, rr * 0.5f, 255, 255, 255, (int) (150 * fade)); // nucleo mais fraco: reduzir claroes
        // estilhacos radiais na cor do elemento clareada (antes brancos e opacos: dominavam a cena e escondiam o impacto)
        int shards = f.element == SpellElement.FIRE || f.element == SpellElement.LIFE ? 4 : 6;
        int sr = (r + 255) / 2, sg = (g + 255) / 2, sb = (b + 255) / 2;
        for (int i = 0; i < shards; i++) {
          float y = 1f - (i + 0.5f) * 2f / shards;
          float rad = (float) Math.sqrt(Math.max(0f, 1f - y * y));
          float ang = i * 2.3999631f; // angulo aureo
          Vec3 d = new Vec3(Math.cos(ang) * rad, y, Math.sin(ang) * rad);
          float start = rr * 0.6f, end = rr * (1.4f + 0.8f * ease(life));
          Vec3 u = Math.abs(d.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
          Vec3 pu = d.cross(u).normalize(), pv = d.cross(pu).normalize();
          float w = (f.element == SpellElement.STORM ? 0.03f : 0.05f) * fade + 0.005f;
          BeamRenderer.box(c, pose, f.pos.add(d.scale(start)), f.pos.add(d.scale(end)), pu, pv, w, sr, sg, sb, (int) (140 * fade));
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
    if (colT > 0f && w > 0.01f) {
      // a coluna cai do ceu ate o altar
      Vec3 bottom = f.pos.add(0, skyHeight(f.pos) * (1f - ease(colT)), 0), top = f.pos.add(0, skyHeight(f.pos), 0);
      skyColumn(c, pose, bottom, f.pos.add(0, skyHeight(f.pos), 0), w, r, g, b, 1f);
    }
    RuneCircle.draw(c, pose, f.pos.add(0, 0.1, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1), Math.max(1.5f, f.size * 0.45f) * ease(Math.min(1f, life * 3f)), life * 3f, r, g, b, (int) (230 * fade));
    // aneis de runas descendo pela coluna
    for (int i = 0; i < 4; i++) {
      float h = (1f - ((life * 1.5f + i * 0.25f) % 1f)) * height;
      ring(c, pose, f.pos.add(0, h, 0), 1.0f * fade + 0.2f, 0.06f, life * 6f + i, life * 6f + i + TAU, 12, 255, 255, 255, (int) (200 * fade));
    }
  }

  // ---------------------------------------------------------------- geometria
  /** Altura da coluna acima de {@code base}: ate o limite de construcao do mundo (o ceu). */
  static float skyHeight(Vec3 base) {
    var level = Minecraft.getInstance().level;
    float limit = level == null ? 320f : level.getMaxBuildHeight();
    return Math.max(64f, limit - (float) base.y);
  }

  /**
   * Coluna de luz que vai ate o ceu sem ponta visivel: quatro faces (casca, nucleo branco por dentro) em que a opacidade
   * cai com a altura ate sumir no topo. {@code strength} (0 a 1) escala tudo.
   */
  static void skyColumn(VertexConsumer c, PoseStack.Pose pose, Vec3 bottom, Vec3 top, float w, int r, int g, int b, float strength) {
    float span = (float) Math.max(1.0, top.y - bottom.y);
    for (int layer = 0; layer < 2; layer++) {
      float half = layer == 0 ? w : w * 0.35f;
      int cr = layer == 0 ? r : 255, cg = layer == 0 ? g : 255, cb = layer == 0 ? b : 255;
      int a0 = (int) ((layer == 0 ? 95 : 235) * strength);
      Vec3[] bc = {bottom.add(-half, 0, -half), bottom.add(half, 0, -half), bottom.add(half, 0, half), bottom.add(-half, 0, half)};
      Vec3[] tc = {top.add(-half, 0, -half), top.add(half, 0, -half), top.add(half, 0, half), top.add(-half, 0, half)};
      for (int i = 0; i < 4; i++) {
        int j = (i + 1) % 4;
        gradQuad(c, pose, bc[i], bc[j], tc[j], tc[i], cr, cg, cb, a0, 0);
      }
    }
  }

  static float hash01(int a, int b) {
    int h = a * 73856093 ^ b * 19349663;
    h ^= h >>> 13;
    h *= 0x5bd1e995;
    h ^= h >>> 15;
    return (h & 0xFFFF) / 65535f;
  }

  /**
   * Coluna da gravidade: casca fina semitransparente e arroxeada (uma coluna lisa bem fraca por baixo) mais fatias que
   * tremem de lado e piscam em passos curtos. O padrao desliza para baixo e uma faixa clara de "varredura" desce pela luz.
   * So as fatias proximas da ponta sao desenhadas (limite de geometria); acima, so a casca fraca.
   */
  static void glitchColumn(VertexConsumer c, PoseStack.Pose pose, Vec3 bottom, Vec3 top, float w, float time, Vec3 seed) {
    final int pr = 150, pg = 95, pb = 235;     // roxo
    final int cr = 225, cg = 205, cb = 255;    // miolo claro
    skyColumn(c, pose, bottom, top, w * 0.9f, pr, pg, pb, 0.35f);
    float sliceH = 1.1f;
    int slices = Math.min(44, (int) Math.ceil((top.y - bottom.y) / sliceH));
    int step = (int) (time * 1.2f);            // passo do glitch (~24 por segundo / 20 tps)
    int shift = (int) (time * 2.4f);           // deslocamento do padrao para baixo
    int salt = (int) (seed.x * 7 + seed.z * 13);
    for (int i = 0; i < slices; i++) {
      float yLo = (float) bottom.y + i * sliceH;
      float yHi = Math.min((float) top.y, yLo + sliceH * 0.85f);
      int pat = i + shift;                      // i cresce para cima; pat maior em cima => padrao desce
      float n = hash01(pat + salt, step / 2);
      if (n < 0.18f) continue;                  // buraco no sinal
      float jx = (hash01(pat * 3 + salt, step) - 0.5f) * w * (n > 0.85f ? 1.6f : 0.5f);
      float jz = (hash01(pat * 5 + salt, step + 7) - 0.5f) * w * (n > 0.85f ? 1.6f : 0.5f);
      float half = w * (0.75f + 0.5f * hash01(pat, salt));
      boolean hot = n > 0.9f;
      int a = (int) ((hot ? 95 : 48) * (0.6f + 0.4f * hash01(pat + 11, step)));
      int rr = hot ? cr : pr, gg = hot ? cg : pg, bb = hot ? cb : pb;
      Vec3 b0 = new Vec3(bottom.x + jx, yLo, bottom.z + jz), t0 = new Vec3(bottom.x + jx, yHi, bottom.z + jz);
      Vec3[] bc = {b0.add(-half, 0, -half), b0.add(half, 0, -half), b0.add(half, 0, half), b0.add(-half, 0, half)};
      Vec3[] tc = {t0.add(-half, 0, -half), t0.add(half, 0, -half), t0.add(half, 0, half), t0.add(-half, 0, half)};
      for (int k = 0; k < 4; k++) {
        int j = (k + 1) % 4;
        gradQuad(c, pose, bc[k], bc[j], tc[j], tc[k], rr, gg, bb, a, a);
      }
    }
  }

  static Vec3 pt(Vec3 center, float angle, float radius, float y) {
    return new Vec3(center.x + Math.cos(angle) * radius, center.y + y, center.z + Math.sin(angle) * radius);
  }

  /** Faixa horizontal (anel ou arco) de largura 2*half no plano XZ. */
  static void ring(VertexConsumer c, PoseStack.Pose pose, Vec3 center, float radius, float half, float a0, float a1, int seg,
                           int r, int g, int b, int a) {
    if (radius <= 0.02f || a <= 0) return;
    for (int i = 0; i < seg; i++) {
      float s0 = Mth.lerp(i / (float) seg, a0, a1), s1 = Mth.lerp((i + 1) / (float) seg, a0, a1);
      BeamRenderer.face(c, pose, pt(center, s0, radius + half, 0), pt(center, s1, radius + half, 0),
          pt(center, s1, radius - half, 0), pt(center, s0, radius - half, 0), r, g, b, a);
    }
  }

  /** Parede vertical (cilindro ou fatia) que some para cima. */
  static void wall(VertexConsumer c, PoseStack.Pose pose, Vec3 center, float radius, float height, float a0, float a1, int seg,
                           int r, int g, int b, int alphaBottom, int alphaTop) {
    if (radius <= 0.02f || alphaBottom <= 0) return;
    for (int i = 0; i < seg; i++) {
      float s0 = Mth.lerp(i / (float) seg, a0, a1), s1 = Mth.lerp((i + 1) / (float) seg, a0, a1);
      Vec3 b0 = pt(center, s0, radius, 0), b1 = pt(center, s1, radius, 0);
      Vec3 t0 = pt(center, s0, radius, height), t1 = pt(center, s1, radius, height);
      gradQuad(c, pose, b0, b1, t1, t0, r, g, b, alphaBottom, alphaTop);
    }
  }

  static void gradQuad(VertexConsumer c, PoseStack.Pose pose, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, int r, int g, int b, int a1, int a2) {
    BeamRenderer.vtx(c, pose, p1, r, g, b, a1);
    BeamRenderer.vtx(c, pose, p2, r, g, b, a1);
    BeamRenderer.vtx(c, pose, p3, r, g, b, a2);
    BeamRenderer.vtx(c, pose, p4, r, g, b, a2);
    BeamRenderer.vtx(c, pose, p4, r, g, b, a2);
    BeamRenderer.vtx(c, pose, p3, r, g, b, a2);
    BeamRenderer.vtx(c, pose, p2, r, g, b, a1);
    BeamRenderer.vtx(c, pose, p1, r, g, b, a1);
  }

  static void octahedron(VertexConsumer c, PoseStack.Pose pose, Vec3 o, float s, int r, int g, int b, int a) {
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
