package com.arcane.client.render;

import com.arcane.ArcaneMod;
import com.arcane.client.CameraShake;
import com.arcane.client.ClientPrefs;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

import static com.arcane.network.packet.S2CShapeFxPacket.*;

/**
 * Visual proprio de cada glifo original e dos poderes da Hidra. Nada aqui reaproveita o efeito generico das magias:
 * <ul>
 *   <li>Parar o Tempo: cupula de vidro com meridianos, relogio no chao cujos ponteiros freiam e param, cacos de luz
 *   parados no ar e o mundo em preto e branco para quem esta dentro.</li>
 *   <li>Retroceder: helice dupla do destino ate a origem, relogio girando ao contrario e um eco do corpo que apaga.</li>
 *   <li>Colapso Espacial: singularidade negra com disco de acrecao, linhas de espaco sugadas e um clarao no fim.</li>
 *   <li>Aniquilacao: esfera branca que vira um vazio negro, coluna ate o ceu, onda no chao e clarao na tela.</li>
 *   <li>Eclipse: disco negro com coroa vermelha no ceu, cone de luz carmesim e circulo de runas.</li>
 *   <li>Genese: mandala de petalas, arvore de luz que cresce e colunas douradas.</li>
 *   <li>Inversao da Gravidade: cilindro com setas subindo, chuva ao contrario e um teto de runas.</li>
 *   <li>Hidra: aviso de devastacao (circulo que se fecha), impacto, rompimento, despertar, rugido e selo quebrado.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public final class OriginalFxRenderer {
  private static final float TAU = 6.2831855f;
  private static final ResourceLocation DESATURATE = new ResourceLocation("minecraft", "shaders/post/desaturate.json");
  private static final List<Fx> FX = new ArrayList<>();
  private static boolean ownPostEffect;
  private static float flash, flashR = 1f, flashG = 1f, flashB = 1f;
  private static float vignette;

  private OriginalFxRenderer() {}

  private static final class Fx {
    final int kind;
    final Vec3 pos, dir;
    final float size;
    final int rgb, total;
    int age;

    Fx(int kind, Vec3 pos, Vec3 dir, float size, int rgb, int total) {
      this.kind = kind;
      this.pos = pos;
      this.dir = dir;
      this.size = size;
      this.rgb = rgb;
      this.total = total;
    }

    float t(float partial) { return Mth.clamp((age + partial) / total, 0f, 1f); }

    /** Entra em {@code in} ticks e sai nos ultimos {@code out}. */
    float fade(float partial, float in, float out) {
      float a = age + partial;
      return Mth.clamp(a / in, 0f, 1f) * Mth.clamp((total - a) / out, 0f, 1f);
    }
  }

  public static void add(int kind, Vec3 pos, Vec3 dir, float size, int rgb, int ticks) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player != null) {
      double d = mc.player.position().distanceTo(pos);
      float near = (float) Mth.clamp(1.0 - d / Math.max(24.0, size * 3.0), 0.0, 1.0);
      switch (kind) {
        case FX_ANNIHILATE -> { CameraShake.add(0.9f, d * 0.4); flash(0.9f * near, 1f, 0.97f, 0.9f); }
        case FX_SPACE_COLLAPSE -> CameraShake.add(0.6f, d * 0.5);
        case FX_DEVASTATE -> { CameraShake.add(Math.min(0.9f, 0.3f + size * 0.04f), d * 0.5); flash(0.35f * near, 1f, 0.45f, 0.2f); }
        case FX_AWAKEN -> { CameraShake.add(0.8f, d * 0.3); flash(0.5f * near, 0.9f, 0.1f, 0.2f); }
        case FX_BREAKOUT -> CameraShake.add(0.7f, d * 0.3);
        case FX_ROAR -> CameraShake.add(0.5f, d * 0.4);
        case FX_TIME_STOP -> flash(0.25f * near, 0.9f, 0.85f, 0.6f);
        case FX_SEAL_BREAK -> CameraShake.add(0.3f, d);
        case FX_SHOCKWAVE -> CameraShake.add(Math.min(0.8f, 0.2f + size * 0.03f), d * 0.5);
        default -> { }
      }
    }
    synchronized (FX) {
      if (FX.size() < 40) FX.add(new Fx(kind, pos, dir, Math.max(0.5f, size), rgb, Math.max(6, Math.min(1200, ticks))));
    }
  }

  private static void flash(float amount, float r, float g, float b) {
    if (ClientPrefs.reduceFlashes() || amount <= flash) return;
    flash = Math.min(1f, amount);
    flashR = r;
    flashG = g;
    flashB = b;
  }

  // ---------------------------------------------------------------- tick: particulas, filtro da tela
  @SubscribeEvent
  public static void tick(TickEvent.ClientTickEvent e) {
    if (e.phase != TickEvent.Phase.END) return;
    Minecraft mc = Minecraft.getInstance();
    flash = Math.max(0f, flash - 0.06f);
    boolean grey = false;
    float tint = 0f;
    synchronized (FX) {
      if (mc.level != null) {
        for (Fx f : FX) {
          emit(mc, f);
          if (mc.player != null && mc.player.position().distanceTo(f.pos) <= f.size) {
            if (f.kind == FX_TIME_STOP && f.age < f.total - 4) grey = true;
            if (f.kind == FX_ECLIPSE) tint = Math.max(tint, f.fade(0, 20, 30));
          }
        }
      }
      FX.removeIf(f -> ++f.age >= f.total);
    }
    vignette = tint;
    // mundo cinza dentro do Parar o Tempo (filtro do proprio jogo; nao mexe se outro filtro ja estiver ligado)
    if (mc.gameRenderer == null) return;
    if (grey && !ClientPrefs.reduceFlashes() && !ownPostEffect && mc.gameRenderer.currentEffect() == null) {
      mc.gameRenderer.loadEffect(DESATURATE);
      ownPostEffect = true;
    } else if (!grey && ownPostEffect) {
      mc.gameRenderer.shutdownEffect();
      ownPostEffect = false;
    }
  }

  private static void emit(Minecraft mc, Fx f) {
    var level = mc.level;
    var rnd = level.random;
    switch (f.kind) {
      case FX_GENESIS -> {
        for (int i = 0; i < 4; i++) {
          double a = rnd.nextDouble() * TAU, r = f.size * Math.sqrt(rnd.nextDouble());
          level.addParticle(i % 2 == 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.END_ROD,
              f.pos.x + Math.cos(a) * r, f.pos.y + 0.2, f.pos.z + Math.sin(a) * r, 0, 0.08 + rnd.nextDouble() * 0.06, 0);
        }
      }
      case FX_GRAVITY -> {
        for (int i = 0; i < 5; i++) {
          double a = rnd.nextDouble() * TAU, r = f.size * Math.sqrt(rnd.nextDouble());
          level.addParticle(ParticleTypes.REVERSE_PORTAL, f.pos.x + Math.cos(a) * r, f.pos.y + rnd.nextDouble() * 2, f.pos.z + Math.sin(a) * r, 0, 0.25, 0);
        }
      }
      case FX_SPACE_COLLAPSE -> {
        if (f.age < f.total * 0.8f) {
          for (int i = 0; i < 6; i++) {
            Vec3 d = new Vec3(rnd.nextGaussian(), rnd.nextGaussian() * 0.6, rnd.nextGaussian()).normalize().scale(f.size * (0.6 + rnd.nextDouble() * 0.4));
            Vec3 at = f.pos.add(0, 1, 0).add(d);
            level.addParticle(ParticleTypes.PORTAL, at.x, at.y, at.z, -d.x * 0.9, -d.y * 0.9, -d.z * 0.9);
          }
        }
      }
      case FX_ECLIPSE -> {
        if (rnd.nextInt(2) == 0) {
          double a = rnd.nextDouble() * TAU, r = f.size * 0.5 * Math.sqrt(rnd.nextDouble());
          level.addParticle(ParticleTypes.ASH, f.pos.x + Math.cos(a) * r, f.pos.y + 6 + rnd.nextDouble() * 6, f.pos.z + Math.sin(a) * r, 0, -0.05, 0);
        }
      }
      case FX_DEVASTATE -> {
        if (f.age < 3) {
          for (int i = 0; i < 30; i++) {
            Vec3 d = new Vec3(rnd.nextGaussian(), Math.abs(rnd.nextGaussian()) + 0.4, rnd.nextGaussian()).normalize().scale(0.4 + rnd.nextDouble() * f.size * 0.08);
            level.addParticle(i % 3 == 0 ? ParticleTypes.LAVA : ParticleTypes.LARGE_SMOKE, f.pos.x, f.pos.y + 0.5, f.pos.z, d.x, d.y, d.z);
          }
        }
      }
      case FX_BREAKOUT -> {
        for (int i = 0; i < 6; i++) {
          level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, f.pos.x + (rnd.nextDouble() - 0.5) * f.size, f.pos.y + f.dir.y + 1,
              f.pos.z + (rnd.nextDouble() - 0.5) * f.size, 0, 0.12, 0);
        }
      }
      default -> { }
    }
  }

  // ---------------------------------------------------------------- tela: clarao e vinheta
  @SubscribeEvent
  public static void hud(RenderGuiEvent.Post event) {
    var g = event.getGuiGraphics();
    int w = g.guiWidth(), h = g.guiHeight();
    if (vignette > 0.01f && !ClientPrefs.reduceFlashes()) {
      int a = (int) (110 * vignette);
      int edge = (a << 24) | 0x300008;
      g.fillGradient(0, 0, w, h / 3, edge, 0x00300008);
      g.fillGradient(0, h - h / 3, w, h, 0x00300008, edge);
    }
    if (flash > 0.01f) {
      int a = (int) (200 * flash);
      int col = (a << 24) | ((int) (flashR * 255) << 16) | ((int) (flashG * 255) << 8) | (int) (flashB * 255);
      g.fill(0, 0, w, h, col);
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
      float partial = event.getPartialTick();
      MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
      pose.pushPose();
      pose.translate(-cam.x, -cam.y, -cam.z);
      PoseStack.Pose last = pose.last();
      // primeiro o que e escuro (vazio, disco do eclipse): mistura normal; depois a luz, que soma
      VertexConsumer dark = buffers.getBuffer(RenderType.debugQuads());
      for (Fx f : FX) drawDark(dark, last, f, partial);
      buffers.endBatch(RenderType.debugQuads());
      VertexConsumer c = buffers.getBuffer(RenderType.lightning());
      for (Fx f : FX) drawLight(c, last, f, partial);
      pose.popPose();
      buffers.endBatch(RenderType.lightning());
    }
  }

  private static void drawDark(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float t = f.t(p);
    switch (f.kind) {
      case FX_SPACE_COLLAPSE -> {
        float core = coreRadius(f, t);
        if (core > 0.05f) solidSphere(c, pose, f.pos.add(0, 1, 0), core, 4, 0, 10, 245);
      }
      case FX_ANNIHILATE -> {
        if (t > 0.12f) {
          float k = (t - 0.12f) / 0.88f;
          float r = f.size * 0.8f * (1f - k * k);
          solidSphere(c, pose, f.pos, r, 0, 0, 0, (int) (235 * Math.min(1f, (1f - k) * 3f)));
        }
      }
      case FX_ECLIPSE -> {
        float fade = f.fade(p, 20, 30);
        Vec3 sky = eclipseSky(f);
        disc(c, pose, sky, 8f, 24, 2, 0, 4, (int) (240 * fade));
      }
      case FX_DEVASTATE -> {
        float k = Math.min(1f, t * 1.6f);
        float r = f.size * 0.75f * (1f - k);
        if (r > 0.1f) solidSphere(c, pose, f.pos.add(0, 0.5, 0), r, 30, 4, 2, (int) (200 * (1f - k)));
      }
      default -> { }
    }
  }

  private static void drawLight(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    switch (f.kind) {
      case FX_TIME_STOP -> timeStop(c, pose, f, p);
      case FX_REWIND -> rewind(c, pose, f, p);
      case FX_SPACE_COLLAPSE -> spaceCollapse(c, pose, f, p);
      case FX_ANNIHILATE -> annihilate(c, pose, f, p);
      case FX_ECLIPSE -> eclipse(c, pose, f, p);
      case FX_GENESIS -> genesis(c, pose, f, p);
      case FX_GRAVITY -> gravity(c, pose, f, p);
      case FX_WARN -> warn(c, pose, f, p);
      case FX_DEVASTATE -> devastate(c, pose, f, p);
      case FX_BREAKOUT -> breakout(c, pose, f, p);
      case FX_AWAKEN -> awaken(c, pose, f, p);
      case FX_ROAR -> roar(c, pose, f, p);
      case FX_SEAL_BREAK -> sealBreak(c, pose, f, p);
      case FX_SHOCKWAVE -> shockwave(c, pose, f, p);
      default -> { }
    }
  }

  // ================================================================ Parar o Tempo
  private static void timeStop(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float age = f.age + p;
    float fade = f.fade(p, 4, 10);
    float open = ShapeFxRenderer.ease(Math.min(1f, age / 10f));
    float R = f.size * open;
    int a = (int) (110 * fade);
    // cupula: paralelos e meridianos
    for (int i = -2; i <= 4; i++) {
      float lat = i * 0.26f;
      ShapeFxRenderer.ring(c, pose, f.pos.add(0, Math.sin(lat) * R, 0), (float) Math.cos(lat) * R, 0.035f, 0f, TAU, 48, 220, 235, 255, a);
    }
    for (int m = 0; m < 10; m++) meridian(c, pose, f.pos, R, m * TAU / 10 + 0.002f * age, 0.03f, 230, 220, 170, a);
    // onda de congelamento na abertura
    if (age < 14) {
      float k = age / 14f;
      ShapeFxRenderer.ring(c, pose, f.pos.add(0, 0.1, 0), f.size * k * 1.05f, 0.3f * (1f - k) + 0.05f, 0f, TAU, 64, 255, 255, 255, (int) (230 * (1f - k)));
    }
    // relogio no chao: ponteiros giram rapido e freiam ate parar
    Vec3 g = f.pos.add(0, 0.06, 0);
    float face = Math.max(1.5f, f.size * 0.55f) * open;
    int ca = (int) (235 * fade);
    ShapeFxRenderer.ring(c, pose, g, face, 0.09f, 0f, TAU, 64, 255, 215, 140, ca);
    ShapeFxRenderer.ring(c, pose, g, face * 0.92f, 0.03f, 0f, TAU, 64, 255, 240, 200, ca);
    for (int h = 0; h < 12; h++) {
      float ang = h * TAU / 12;
      float inner = h % 3 == 0 ? 0.76f : 0.84f;
      flatLine(c, pose, g, ang, face * inner, ang, face * 0.9f, h % 3 == 0 ? 0.08f : 0.04f, 255, 230, 170, ca);
    }
    float spin = 9f * (1f - (float) Math.exp(-age / 6f)); // freia exponencialmente
    float tremble = age > 25 ? Mth.sin(age * 2.7f) * 0.01f : 0f;
    flatLine(c, pose, g, 0, 0, -spin * 1.0f + tremble, face * 0.45f, 0.07f, 255, 255, 255, ca);
    flatLine(c, pose, g, 0, 0, -spin * 12f + 1.2f + tremble, face * 0.72f, 0.04f, 255, 255, 255, ca);
    // cacos de luz parados no ar (o tempo parou para eles)
    for (int i = 0; i < 24; i++) {
      float u = ShapeFxRenderer.hash01(i, 11), v = ShapeFxRenderer.hash01(i, 23), w = ShapeFxRenderer.hash01(i, 37);
      double ang = u * TAU, rr = Math.sqrt(v) * R * 0.85, y = 0.5 + w * Math.max(1.5, R * 0.6);
      float tw = 0.6f + 0.4f * Mth.sin(age * 0.3f + i);
      ShapeFxRenderer.octahedron(c, pose, f.pos.add(Math.cos(ang) * rr, y, Math.sin(ang) * rr), 0.08f + 0.06f * w, 255, 235, 190, (int) (200 * fade * tw));
    }
    // no fim a cupula estoura para fora
    float end = f.total - age;
    if (end < 8) {
      float k = 1f - end / 8f;
      for (int i = 0; i <= 3; i++) {
        float lat = i * 0.35f;
        ShapeFxRenderer.ring(c, pose, f.pos.add(0, Math.sin(lat) * R * (1 + k), 0), (float) Math.cos(lat) * R * (1 + k), 0.1f, 0f, TAU, 48, 255, 255, 255, (int) (200 * (1f - k)));
      }
    }
  }

  // ================================================================ Retroceder
  private static void rewind(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float age = f.age + p, t = f.t(p);
    float fade = f.fade(p, 2, 12);
    Vec3 from = f.dir.lengthSqr() > 0 ? f.dir : f.pos.add(0, 0, 0.01);
    Vec3 to = f.pos;
    Vec3 span = to.subtract(from);
    double len = Math.max(0.5, span.length());
    Vec3 n = span.normalize();
    Vec3 up = Math.abs(n.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
    Vec3 u = n.cross(up).normalize(), v = n.cross(u).normalize();
    // helice dupla (cresce da origem ao destino, girando para tras)
    int seg = (int) Math.min(80, 12 + len * 4);
    float grow = Math.min(1f, t * 2.5f);
    for (int strand = 0; strand < 2; strand++) {
      Vec3 prev = null;
      for (int i = 0; i <= seg * grow; i++) {
        float k = i / (float) seg;
        float ang = -age * 0.5f + k * (float) len * 1.6f + strand * Mth.PI;
        double rad = 0.45 + 0.15 * Mth.sin(k * 9f);
        Vec3 pt = from.add(span.scale(k)).add(0, 1, 0).add(u.scale(Math.cos(ang) * rad)).add(v.scale(Math.sin(ang) * rad));
        if (prev != null) line(c, pose, prev, pt, 0.04f, strand == 0 ? 90 : 220, strand == 0 ? 255 : 255, strand == 0 ? 220 : 255, (int) (220 * fade));
        prev = pt;
      }
    }
    // relogio no destino com ponteiros voltando
    Vec3 g = to.add(0, 0.06, 0);
    ShapeFxRenderer.ring(c, pose, g, 1.4f, 0.06f, 0f, TAU, 40, 90, 255, 220, (int) (230 * fade));
    for (int h = 0; h < 12; h++) flatLine(c, pose, g, h * TAU / 12, 1.15f, h * TAU / 12, 1.3f, 0.03f, 200, 255, 240, (int) (200 * fade));
    flatLine(c, pose, g, 0, 0, age * 0.9f, 0.7f, 0.05f, 255, 255, 255, (int) (230 * fade));
    flatLine(c, pose, g, 0, 0, age * 0.35f, 1.0f, 0.03f, 255, 255, 255, (int) (230 * fade));
    // eco do corpo na origem: pilha de aneis que sobe e apaga
    float ghost = (1f - t) * fade;
    for (int i = 0; i < 6; i++) {
      ShapeFxRenderer.ring(c, pose, from.add(0, 0.2 + i * 0.32, 0), 0.35f + 0.08f * Mth.sin(i * 1.3f), 0.03f, 0f, TAU, 20, 140, 255, 230, (int) (180 * ghost));
    }
    // estouro de chegada
    if (age < 10) {
      float k = age / 10f;
      ShapeFxRenderer.ring(c, pose, to.add(0, 1, 0), 0.5f + 2.5f * k, 0.12f * (1 - k), 0f, TAU, 32, 255, 255, 255, (int) (230 * (1 - k)));
    }
  }

  // ================================================================ Colapso Espacial
  private static float coreRadius(Fx f, float t) {
    float max = 1.0f + f.size * 0.05f;
    if (t < 0.3f) return max * ShapeFxRenderer.ease(t / 0.3f);
    if (t < 0.85f) return max;
    return max * Math.max(0f, 1f - (t - 0.85f) / 0.05f);
  }

  private static void spaceCollapse(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float age = f.age + p, t = f.t(p);
    Vec3 o = f.pos.add(0, 1, 0);
    float core = coreRadius(f, t);
    float live = t < 0.88f ? 1f : 0f;
    // disco de acrecao inclinado, aneis girando em velocidades diferentes
    Vec3 du = new Vec3(1, 0, 0), dv = new Vec3(0, Math.sin(0.35), Math.cos(0.35));
    for (int i = 0; i < 6; i++) {
      float rr = core * (1.35f + i * 0.35f);
      int r = i < 2 ? 255 : 170 + i * 10, g = i < 2 ? 230 : 90 + i * 8, b = 255;
      tiltedRing(c, pose, o, du, dv, rr, 0.05f + 0.03f * (5 - i), age * (0.3f - i * 0.04f), 48, r, g, b, (int) ((200 - i * 25) * live));
    }
    // horizonte de eventos: aro claro em volta do nucleo
    tiltedRing(c, pose, o, du, new Vec3(0, 1, 0), core * 1.04f, 0.04f, 0f, 40, 255, 255, 255, (int) (220 * live));
    // linhas do espaco sendo sugadas
    for (int i = 0; i < 28; i++) {
      float u = ShapeFxRenderer.hash01(i, 5), v = ShapeFxRenderer.hash01(i, 9);
      double th = u * TAU, ph = (v - 0.5) * 2.2;
      Vec3 d = new Vec3(Math.cos(th) * Math.cos(ph), Math.sin(ph) * 0.7, Math.sin(th) * Math.cos(ph));
      float k = (t * 3f + ShapeFxRenderer.hash01(i, 77)) % 1f;
      double far = f.size * (1f - k), near = Math.max(core, far - 1.5 - f.size * 0.15);
      if (far <= core) continue;
      line(c, pose, o.add(d.scale(far)), o.add(d.scale(near)), 0.03f, 200, 140, 255, (int) (200 * k * live));
    }
    // lente: aneis horizontais que encolhem ate o nucleo
    for (int i = 0; i < 3; i++) {
      float k = 1f - ((t * 2f + i / 3f) % 1f);
      ShapeFxRenderer.ring(c, pose, o, Math.max(core, f.size * k), 0.06f, 0f, TAU, 48, 150, 90, 255, (int) (150 * (1 - k) * live));
    }
    // implosao final: clarao branco e anel
    if (t >= 0.86f) {
      float k = (t - 0.86f) / 0.14f;
      ShapeFxRenderer.octahedron(c, pose, o, 0.5f + 3f * k, 255, 255, 255, (int) (240 * (1 - k)));
      ShapeFxRenderer.ring(c, pose, o, f.size * 0.2f + f.size * k, 0.25f * (1 - k) + 0.03f, 0f, TAU, 64, 230, 200, 255, (int) (230 * (1 - k)));
    }
  }

  // ================================================================ Aniquilacao
  private static void annihilate(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float age = f.age + p, t = f.t(p);
    float R = f.size;
    // 1) esfera branca que se expande
    if (t < 0.25f) {
      float k = t / 0.25f;
      float r = R * ShapeFxRenderer.ease(Math.min(1f, k * 2f));
      int a = (int) (240 * (1 - k));
      for (int i = -3; i <= 3; i++) {
        float lat = i * 0.42f;
        ShapeFxRenderer.ring(c, pose, f.pos.add(0, Math.sin(lat) * r, 0), (float) Math.cos(lat) * r, 0.3f, 0f, TAU, 48, 255, 250, 235, a);
      }
      ShapeFxRenderer.octahedron(c, pose, f.pos, r * 0.5f, 255, 255, 255, a);
    }
    // 2) aro do vazio (a esfera negra e desenhada em drawDark)
    if (t > 0.12f) {
      float k = (t - 0.12f) / 0.88f;
      float r = R * 0.8f * (1f - k * k);
      int a = (int) (230 * Math.min(1f, (1f - k) * 3f));
      for (int i = 0; i < 3; i++) {
        float tilt = i * 1.05f + age * 0.05f;
        tiltedRing(c, pose, f.pos, new Vec3(Math.cos(tilt), 0, Math.sin(tilt)), new Vec3(0, 1, 0), r * 1.03f, 0.06f, 0f, 48, 255, 200, 150, a);
      }
      ShapeFxRenderer.ring(c, pose, f.pos, r * 1.03f, 0.08f, 0f, TAU, 48, 255, 220, 180, a);
    }
    // 3) coluna ate o ceu e onda no chao
    float col = Math.max(0f, 1f - t * 1.6f);
    if (col > 0) {
      ShapeFxRenderer.skyColumn(c, pose, f.pos, f.pos.add(0, ShapeFxRenderer.skyHeight(f.pos), 0), 0.6f + R * 0.12f, 255, 245, 230, col);
    }
    float wave = Math.min(1f, t * 1.8f);
    ShapeFxRenderer.ring(c, pose, f.pos.add(0, -R * 0.6 + 0.2, 0), R * (0.8f + 2.2f * ShapeFxRenderer.ease(wave)), 0.5f * (1 - wave) + 0.05f, 0f, TAU, 72, 255, 230, 200, (int) (220 * (1 - wave)));
    // 4) estilhacos voando
    for (int i = 0; i < 16; i++) {
      float y = 1f - (i + 0.5f) * 2f / 16;
      float rad = (float) Math.sqrt(Math.max(0f, 1f - y * y));
      float ang = i * 2.3999631f;
      Vec3 d = new Vec3(Math.cos(ang) * rad, y, Math.sin(ang) * rad);
      float k = ShapeFxRenderer.ease(Math.min(1f, t * 1.4f));
      ShapeFxRenderer.octahedron(c, pose, f.pos.add(d.scale(R * (0.3f + 1.6f * k))), 0.15f + R * 0.02f, 255, 210, 160, (int) (220 * (1 - t)));
    }
  }

  // ================================================================ Eclipse
  private static Vec3 eclipseSky(Fx f) {
    return f.pos.add(0, Math.min(56.0, ShapeFxRenderer.skyHeight(f.pos) * 0.5), 0);
  }

  private static void eclipse(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float age = f.age + p;
    float fade = f.fade(p, 20, 30);
    Vec3 sky = eclipseSky(f);
    // coroa: aneis tremulando e raios de luz em volta do disco negro
    for (int i = 0; i < 4; i++) {
      float rr = 8.3f + i * 0.7f + 0.2f * Mth.sin(age * 0.2f + i);
      ShapeFxRenderer.ring(c, pose, sky.add(0, -0.05, 0), rr, 0.35f - i * 0.06f, 0f, TAU, 64, 255, 90 + i * 40, 40 + i * 15, (int) ((220 - i * 40) * fade));
    }
    for (int i = 0; i < 20; i++) {
      float ang = i * TAU / 20 + age * 0.004f;
      float len = 3f + 3f * (0.5f + 0.5f * Mth.sin(age * 0.13f + i * 2.1f));
      flatLine(c, pose, sky.add(0, -0.1, 0), ang, 8.6f, ang, 8.6f + len, 0.12f, 255, 120, 60, (int) (170 * fade));
    }
    // cone carmesim do disco ate o chao
    float ground = Math.max(4f, f.size * 0.45f);
    int seg = 32;
    double h = sky.y - f.pos.y;
    for (int i = 0; i < seg; i++) {
      float a0 = i * TAU / seg, a1 = (i + 1) * TAU / seg;
      Vec3 t0 = ShapeFxRenderer.pt(sky, a0, 8f, 0), t1 = ShapeFxRenderer.pt(sky, a1, 8f, 0);
      Vec3 b0 = ShapeFxRenderer.pt(f.pos, a0, ground, 0.1f), b1 = ShapeFxRenderer.pt(f.pos, a1, ground, 0.1f);
      ShapeFxRenderer.gradQuad(c, pose, b0, b1, t1, t0, 200, 20, 30, (int) (30 * fade), (int) (6 * fade));
    }
    // circulo de runas no chao e chuva de cinzas de luz caindo pelo cone
    RuneCircle.draw(c, pose, f.pos.add(0, 0.08, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1), ground, age * 0.02f, 255, 40, 50, (int) (230 * fade));
    RuneCircle.draw(c, pose, f.pos.add(0, 0.09, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1), ground * 0.5f, -age * 0.035f, 255, 170, 80, (int) (200 * fade));
    for (int i = 0; i < 12; i++) {
      float k = (age * 0.01f + ShapeFxRenderer.hash01(i, 3)) % 1f;
      float ang = ShapeFxRenderer.hash01(i, 8) * TAU;
      float rad = Mth.lerp(k, 6f, ground * 0.8f);
      Vec3 at = ShapeFxRenderer.pt(f.pos, ang, rad, (float) (h * (1 - k)));
      line(c, pose, at, at.add(0, 1.2, 0), 0.04f, 255, 80, 60, (int) (200 * fade));
    }
  }

  // ================================================================ Genese
  private static void genesis(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float age = f.age + p, t = f.t(p);
    float fade = f.fade(p, 6, 16);
    float open = ShapeFxRenderer.ease(Math.min(1f, t * 3f));
    Vec3 g = f.pos.add(0, 0.07, 0);
    // mandala: tres rosaceas (curvas de petalas) girando
    rose(c, pose, g, f.size * 0.9f * open, 6, age * 0.01f, 0.06f, 255, 220, 120, (int) (230 * fade));
    rose(c, pose, g, f.size * 0.65f * open, 8, -age * 0.015f, 0.05f, 120, 255, 140, (int) (220 * fade));
    rose(c, pose, g, f.size * 0.35f * open, 5, age * 0.03f, 0.05f, 255, 255, 220, (int) (220 * fade));
    ShapeFxRenderer.ring(c, pose, g, f.size * 0.95f * open, 0.07f, 0f, TAU, 72, 255, 230, 150, (int) (200 * fade));
    // arvore de luz que cresce do centro
    float grow = ShapeFxRenderer.ease(Math.min(1f, t * 2f));
    branch(c, pose, f.pos, new Vec3(0, 1, 0), Math.max(2.5f, f.size * 0.35f) * grow, 0, (int) (230 * fade), age);
    // colunas douradas em volta, uma depois da outra
    for (int i = 0; i < 7; i++) {
      float start = i * 0.06f;
      float k = Mth.clamp((t - start) * 4f, 0f, 1f);
      if (k <= 0) continue;
      float ang = i * TAU / 7 + 0.3f;
      Vec3 base = ShapeFxRenderer.pt(f.pos, ang, f.size * 0.7f, 0f);
      float pulse = 0.75f + 0.25f * Mth.sin(age * 0.25f + i);
      ShapeFxRenderer.skyColumn(c, pose, base, base.add(0, 4 + 8 * k, 0), 0.25f * pulse, 255, 215, 110, 0.8f * fade);
      ShapeFxRenderer.ring(c, pose, base.add(0, 0.05, 0), 0.6f * k, 0.05f, 0f, TAU, 20, 180, 255, 150, (int) (200 * fade));
    }
  }

  /** Galho recursivo: tronco, depois tres galhos menores que se abrem e giram devagar. */
  private static void branch(VertexConsumer c, PoseStack.Pose pose, Vec3 from, Vec3 dir, float len, int depth, int alpha, float age) {
    if (len < 0.25f || depth > 3 || alpha <= 0) return;
    Vec3 to = from.add(dir.scale(len));
    int r = depth < 2 ? 255 : 150, g = depth < 2 ? 220 : 255, b = depth < 2 ? 130 : 150;
    line(c, pose, from, to, 0.09f / (depth + 1) + 0.02f, r, g, b, alpha);
    if (depth == 3) { ShapeFxRenderer.octahedron(c, pose, to, 0.12f, 255, 250, 200, alpha); return; }
    for (int k = 0; k < 3; k++) {
      float ang = k * TAU / 3 + depth * 0.9f + age * 0.01f;
      Vec3 side = new Vec3(Math.cos(ang), 0, Math.sin(ang));
      Vec3 nd = dir.scale(0.75).add(side.scale(0.65)).normalize();
      branch(c, pose, to, nd, len * 0.62f, depth + 1, (int) (alpha * 0.9f), age);
    }
  }

  // ================================================================ Inversao da Gravidade
  private static void gravity(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float age = f.age + p;
    float fade = f.fade(p, 6, 14);
    float R = f.size;
    float H = Math.max(8f, R * 0.8f);
    Vec3 top = f.pos.add(0, H, 0);
    RuneCircle.draw(c, pose, f.pos.add(0, 0.07, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1), R, age * 0.03f, 110, 90, 255, (int) (220 * fade));
    RuneCircle.draw(c, pose, top, new Vec3(0, 0, 1), new Vec3(1, 0, 0), R * 0.8f, -age * 0.03f, 120, 220, 255, (int) (200 * fade));
    // chuva ao contrario: tracos subindo
    for (int i = 0; i < 48; i++) {
      float ang = ShapeFxRenderer.hash01(i, 13) * TAU;
      float rad = (float) Math.sqrt(ShapeFxRenderer.hash01(i, 29)) * R;
      float k = (age * 0.03f * (0.6f + ShapeFxRenderer.hash01(i, 41)) + ShapeFxRenderer.hash01(i, 53)) % 1f;
      Vec3 at = ShapeFxRenderer.pt(f.pos, ang, rad, k * H);
      line(c, pose, at, at.add(0, 0.8 + 0.8 * ShapeFxRenderer.hash01(i, 61), 0), 0.03f, 150, 200, 255, (int) (200 * fade * Math.sin(k * Math.PI)));
    }
    // setas (chevrons) apontando para cima, girando na parede do cilindro
    for (int i = 0; i < 12; i++) {
      float ang = i * TAU / 12 + age * 0.02f;
      float y = ((age * 0.08f + i * 0.37f) % 1f) * H;
      Vec3 tip = ShapeFxRenderer.pt(f.pos, ang, R, y + 0.8f);
      Vec3 tan = new Vec3(-Math.sin(ang), 0, Math.cos(ang)).scale(0.6);
      Vec3 low = ShapeFxRenderer.pt(f.pos, ang, R, y);
      line(c, pose, low.add(tan), tip, 0.06f, 180, 140, 255, (int) (220 * fade));
      line(c, pose, low.subtract(tan), tip, 0.06f, 180, 140, 255, (int) (220 * fade));
    }
    // aneis subindo do chao ao teto
    for (int i = 0; i < 4; i++) {
      float k = (age * 0.025f + i * 0.25f) % 1f;
      ShapeFxRenderer.ring(c, pose, f.pos.add(0, k * H, 0), R * (1f - 0.2f * k), 0.08f, 0f, TAU, 56, 120, 220, 255, (int) (200 * fade * (1 - k)));
    }
  }

  // ================================================================ Hidra: aviso de devastacao
  private static void warn(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float t = f.t(p);
    float age = f.age + p;
    Vec3 g = f.pos.add(0, 0.1, 0);
    float blink = 0.6f + 0.4f * Mth.sin(age * (0.6f + t * 1.6f));
    int a = (int) (230 * blink);
    RuneCircle.draw(c, pose, g, new Vec3(1, 0, 0), new Vec3(0, 0, 1), f.size, age * 0.05f, 255, 30, 30, a);
    // circulo que se fecha: quando encosta no centro, o golpe cai
    ShapeFxRenderer.ring(c, pose, g, f.size * (1f - t), 0.12f, 0f, TAU, 56, 255, 120, 60, a);
    ShapeFxRenderer.wall(c, pose, f.pos, f.size, 1.6f + t * 2f, 0f, TAU, 48, 255, 20, 20, (int) (120 * blink), 0);
  }

  // ================================================================ Hidra: impacto
  private static void devastate(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float t = f.t(p);
    float R = f.size;
    float k = ShapeFxRenderer.ease(Math.min(1f, t * 2.2f));
    int a = (int) (230 * (1 - t));
    for (int i = 0; i <= 4; i++) {
      float lat = i * 0.33f;
      ShapeFxRenderer.ring(c, pose, f.pos.add(0, Math.sin(lat) * R * k, 0), (float) Math.cos(lat) * R * k, 0.25f, 0f, TAU, 48, 255, 110 + i * 25, 40, a);
    }
    ShapeFxRenderer.ring(c, pose, f.pos.add(0, 0.2, 0), R * (1f + 1.6f * k), 0.4f * (1 - t) + 0.05f, 0f, TAU, 64, 255, 180, 90, a);
    float col = Math.max(0f, 1f - t * 2f);
    if (col > 0) ShapeFxRenderer.skyColumn(c, pose, f.pos, f.pos.add(0, Math.min(64, R * 5), 0), 0.4f + R * 0.1f, 255, 90, 30, col);
  }

  // ================================================================ Hidra: rompendo ate a superficie
  private static void breakout(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float age = f.age + p, t = f.t(p);
    float H = (float) Math.max(8, f.dir.y + 12);
    float fade = f.fade(p, 4, 12);
    ShapeFxRenderer.skyColumn(c, pose, f.pos, f.pos.add(0, H * ShapeFxRenderer.ease(t), 0), f.size * 0.45f, 255, 60, 30, 0.8f * fade);
    for (int i = 0; i < 6; i++) {
      float k = (age * 0.06f + i / 6f) % 1f;
      ShapeFxRenderer.ring(c, pose, f.pos.add(0, k * H * t, 0), f.size * (0.7f + 0.3f * k), 0.12f, 0f, TAU, 40, 255, 150, 60, (int) (220 * fade * (1 - k)));
    }
  }

  // ================================================================ Hidra: despertar (o selo estilhaca)
  private static void awaken(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float age = f.age + p, t = f.t(p);
    float R = f.size;
    float k = ShapeFxRenderer.ease(t);
    Vec3 g = f.pos.add(0, 0.1, 0);
    // o circulo do selo se parte em arcos que voam para fora girando
    for (int i = 0; i < 12; i++) {
      float a0 = i * TAU / 12 + age * 0.02f * (i % 2 == 0 ? 1 : -1);
      float rr = R * 0.5f + R * 0.9f * k;
      Vec3 lift = new Vec3(0, k * (1 + ShapeFxRenderer.hash01(i, 4) * 4), 0);
      ShapeFxRenderer.ring(c, pose, g.add(lift), rr, 0.12f, a0, a0 + TAU / 14, 6, 255, 50, 90, (int) (230 * (1 - t)));
    }
    RuneCircle.draw(c, pose, g, new Vec3(1, 0, 0), new Vec3(0, 0, 1), R * 0.5f * (1 - k * 0.5f), age * 0.1f, 200, 40, 255, (int) (230 * (1 - t)));
    for (int i = 0; i <= 3; i++) {
      float lat = i * 0.4f;
      ShapeFxRenderer.ring(c, pose, f.pos.add(0, Math.sin(lat) * R * k, 0), (float) Math.cos(lat) * R * k, 0.2f, 0f, TAU, 48, 255, 60, 80, (int) (200 * (1 - t)));
    }
  }

  // ================================================================ Hidra: rugido (ondas sonoras)
  private static void roar(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float t = f.t(p);
    Vec3 o = f.pos.add(0, 2, 0);
    for (int i = 0; i < 4; i++) {
      float k = Mth.clamp(t * 1.6f - i * 0.15f, 0f, 1f);
      if (k <= 0 || k >= 1) continue;
      float r = f.size * ShapeFxRenderer.ease(k);
      int a = (int) (180 * (1 - k));
      ShapeFxRenderer.ring(c, pose, o, r, 0.2f, 0f, TAU, 56, 190, 90, 255, a);
      tiltedRing(c, pose, o, new Vec3(1, 0, 0), new Vec3(0, Math.sin(0.6), Math.cos(0.6)), r * 0.9f, 0.12f, 0f, 48, 230, 160, 255, a);
      tiltedRing(c, pose, o, new Vec3(1, 0, 0), new Vec3(0, -Math.sin(0.6), Math.cos(0.6)), r * 0.9f, 0.12f, 0f, 48, 230, 160, 255, a);
    }
  }

  // ================================================================ Hidra: um selo quebrou (a corrente arrebenta)
  private static void sealBreak(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float t = f.t(p);
    Vec3 to = f.dir;
    Vec3 span = to.subtract(f.pos);
    int links = 14;
    for (int i = 0; i < links; i++) {
      float k0 = i / (float) links, k1 = (i + 0.6f) / links;
      Vec3 fly = new Vec3(ShapeFxRenderer.hash01(i, 2) - 0.5, ShapeFxRenderer.hash01(i, 6) - 0.2, ShapeFxRenderer.hash01(i, 9) - 0.5).scale(4 * t);
      Vec3 a0 = f.pos.add(span.scale(k0)).add(fly), a1 = f.pos.add(span.scale(k1)).add(fly);
      line(c, pose, a0, a1, 0.08f, 255, 60, 70, (int) (230 * (1 - t)));
    }
    ShapeFxRenderer.octahedron(c, pose, f.pos, 0.4f + 1.6f * t, 255, 80, 90, (int) (230 * (1 - t)));
    ShapeFxRenderer.ring(c, pose, f.pos, 0.6f + 3f * t, 0.1f, 0f, TAU, 32, 255, 200, 120, (int) (220 * (1 - t)));
  }

  // ================================================================ onda de choque das crateras
  /** Casca de ar quente que corre junto com a frente da cratera (dir.x = velocidade em blocos/tick) e um anel no chao. */
  private static void shockwave(VertexConsumer c, PoseStack.Pose pose, Fx f, float p) {
    float age = f.age + p;
    float speed = (float) Math.max(0.1, f.dir.x);
    float r = Math.min(f.size + 2f, 1.5f + speed * age);
    float k = Mth.clamp(r / (f.size + 2f), 0f, 1f);
    float fade = (1f - k * k) * Mth.clamp((f.total - age) / 6f, 0f, 1f);
    int a = (int) (200 * fade);
    if (a <= 0) return;
    for (int i = -2; i <= 4; i++) {
      float lat = i * 0.3f;
      ShapeFxRenderer.ring(c, pose, f.pos.add(0, Math.sin(lat) * r, 0), (float) Math.cos(lat) * r, 0.12f + 0.25f * (1 - k), 0f, TAU, 56,
          255, 200 - i * 12, 140 - i * 15, (int) (a * (i == 0 ? 1f : 0.45f)));
    }
    ShapeFxRenderer.ring(c, pose, f.pos.add(0, -r * 0.3 + 0.1, 0), r * 1.08f, 0.35f * (1 - k) + 0.08f, 0f, TAU, 64, 255, 235, 200, a);
    // clarao no centro no comeco
    if (age < 6) ShapeFxRenderer.octahedron(c, pose, f.pos, 0.6f + age * 0.5f, 255, 240, 200, (int) (230 * (1 - age / 6f)));
  }

  // ================================================================ geometria
  /** Traco reto 3D com espessura {@code w}. */
  static void line(VertexConsumer c, PoseStack.Pose pose, Vec3 a, Vec3 b, float w, int r, int g, int bl, int al) {
    if (al <= 0) return;
    Vec3 d = b.subtract(a);
    if (d.lengthSqr() < 1.0e-6) return;
    Vec3 n = d.normalize();
    Vec3 up = Math.abs(n.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
    Vec3 u = n.cross(up).normalize(), v = n.cross(u).normalize();
    BeamRenderer.box(c, pose, a, b, u, v, w, r, g, bl, al);
  }

  /** Traco no chao entre dois pontos dados em (angulo, raio). */
  private static void flatLine(VertexConsumer c, PoseStack.Pose pose, Vec3 o, float a0, float r0, float a1, float r1, float w, int r, int g, int b, int al) {
    line(c, pose, ShapeFxRenderer.pt(o, a0, r0, 0), ShapeFxRenderer.pt(o, a1, r1, 0), w, r, g, b, al);
  }

  /** Anel num plano qualquer (u, v). */
  private static void tiltedRing(VertexConsumer c, PoseStack.Pose pose, Vec3 o, Vec3 u, Vec3 v, float radius, float half, float spin, int seg,
                                 int r, int g, int b, int a) {
    if (radius <= 0.02f || a <= 0) return;
    for (int i = 0; i < seg; i++) {
      float s0 = spin + i * TAU / seg, s1 = spin + (i + 1) * TAU / seg;
      Vec3 d0 = u.scale(Math.cos(s0)).add(v.scale(Math.sin(s0))), d1 = u.scale(Math.cos(s1)).add(v.scale(Math.sin(s1)));
      BeamRenderer.face(c, pose, o.add(d0.scale(radius + half)), o.add(d1.scale(radius + half)), o.add(d1.scale(radius - half)), o.add(d0.scale(radius - half)), r, g, b, a);
    }
  }

  /** Meridiano de uma esfera (meia volta, do chao ao topo e de volta ao chao do outro lado). */
  private static void meridian(VertexConsumer c, PoseStack.Pose pose, Vec3 o, float R, float lon, float w, int r, int g, int b, int a) {
    if (R <= 0.05f || a <= 0) return;
    Vec3 prev = null;
    for (int i = 0; i <= 16; i++) {
      float lat = -0.55f + i * (Mth.PI + 1.1f) / 16f;
      // lat de -0,55 a pi+0,55: passa por cima; abaixo de 0 entra um pouco no chao
      Vec3 pt = o.add(Math.cos(lon) * Math.cos(lat) * R, Math.sin(lat) * R, Math.sin(lon) * Math.cos(lat) * R);
      if (prev != null) line(c, pose, prev, pt, w, r, g, b, a);
      prev = pt;
    }
  }

  /** Rosacea r = R |cos(n theta / 2)| (petalas), desenhada como linha. */
  private static void rose(VertexConsumer c, PoseStack.Pose pose, Vec3 o, float R, int petals, float spin, float w, int r, int g, int b, int a) {
    if (R <= 0.1f || a <= 0) return;
    int seg = petals * 16;
    Vec3 prev = null;
    for (int i = 0; i <= seg; i++) {
      float th = i * TAU / seg;
      float rr = R * Math.abs(Mth.cos(petals * th / 2f));
      Vec3 pt = ShapeFxRenderer.pt(o, th + spin, rr, 0);
      if (prev != null) line(c, pose, prev, pt, w, r, g, b, a);
      prev = pt;
    }
  }

  /** Esfera solida (para o escuro). */
  private static void solidSphere(VertexConsumer c, PoseStack.Pose pose, Vec3 o, float R, int r, int g, int b, int a) {
    if (R <= 0.02f || a <= 0) return;
    int lat = 10, lon = 18;
    for (int i = 0; i < lat; i++) {
      double p0 = -Math.PI / 2 + Math.PI * i / lat, p1 = -Math.PI / 2 + Math.PI * (i + 1) / lat;
      for (int j = 0; j < lon; j++) {
        double t0 = 2 * Math.PI * j / lon, t1 = 2 * Math.PI * (j + 1) / lon;
        BeamRenderer.face(c, pose, sp(o, R, p0, t0), sp(o, R, p0, t1), sp(o, R, p1, t1), sp(o, R, p1, t0), r, g, b, a);
      }
    }
  }

  private static Vec3 sp(Vec3 o, float R, double phi, double th) {
    return o.add(Math.cos(phi) * Math.cos(th) * R, Math.sin(phi) * R, Math.cos(phi) * Math.sin(th) * R);
  }

  /** Disco horizontal solido. */
  private static void disc(VertexConsumer c, PoseStack.Pose pose, Vec3 o, float R, int seg, int r, int g, int b, int a) {
    if (a <= 0) return;
    for (int i = 0; i < seg; i++) {
      float a0 = i * TAU / seg, a1 = (i + 1) * TAU / seg;
      BeamRenderer.face(c, pose, o, o, ShapeFxRenderer.pt(o, a1, R, 0), ShapeFxRenderer.pt(o, a0, R, 0), r, g, b, a);
    }
  }

  /** Para testes no cliente: o filtro cinza do Parar o Tempo esta ligado por este codigo? */
  public static boolean greyOn() { return ownPostEffect; }

  /** Para testes no cliente: quantos efeitos unicos estao ativos. */
  public static int active() {
    synchronized (FX) { return FX.size(); }
  }
}
