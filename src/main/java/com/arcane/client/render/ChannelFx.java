package com.arcane.client.render;

import com.arcane.ArcaneMod;
import com.arcane.network.packet.S2CChannelPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Runas de canalizacao do proprio jogador. No modo CARREGAR surgem as runas da base da forma da magia (a da base do
 * feixe, a da base da coluna, o circulo da area) e crescem com a mana gasta; no modo SEGURAR ficam fixas, pulsando, enquanto
 * a magia se mantem. Tudo e so desenho: quem decide o que acontece e o servidor, que manda o estado a cada 5 ticks.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public final class ChannelFx {
  private static int state = S2CChannelPacket.OFF;
  private static int shape;
  private static int rgb = 0xB48CFF;
  private static float progress;
  private static float shown;
  private static int sinceUpdate;
  private static float spin;

  private ChannelFx() {}

  public static void set(int newState, float newProgress, int newShape, int newRgb) {
    if (state == S2CChannelPacket.OFF && newState != S2CChannelPacket.OFF) shown = 0f;
    state = newState;
    progress = newProgress;
    shape = newShape;
    rgb = newRgb;
    sinceUpdate = 0;
  }

  /** Progresso da carga (0 a 1) enquanto carrega, ou -1. */
  public static double chargeProgress() {
    return state == S2CChannelPacket.CHARGING ? Math.max(0f, Math.min(1f, progress)) : -1;
  }

  public static boolean active() { return state != S2CChannelPacket.OFF; }

  @SubscribeEvent
  public static void tick(TickEvent.ClientTickEvent e) {
    if (e.phase != TickEvent.Phase.END) return;
    Minecraft mc = Minecraft.getInstance();
    if (state != S2CChannelPacket.OFF && (mc.player == null || ++sinceUpdate > 25)) state = S2CChannelPacket.OFF; // o servidor parou de avisar
    if (state == S2CChannelPacket.OFF) { shown = 0f; return; }
    spin += 0.05f + 0.07f * shown;
    shown += (target() - shown) * 0.12f;
    if (mc.level != null && mc.player != null && state == S2CChannelPacket.CHARGING && mc.level.random.nextInt(2) == 0) {
      Anchor a = anchor(mc, 1f);
      double r = radius(a) * (0.6 + 0.4 * mc.level.random.nextDouble());
      double ang = mc.level.random.nextDouble() * Math.PI * 2;
      Vec3 p = a.center.add(a.u.scale(Math.cos(ang) * r)).add(a.v.scale(Math.sin(ang) * r));
      mc.level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, a.n.x * 0.05, a.n.y * 0.05 + 0.02, a.n.z * 0.05);
    }
  }

  private static float target() { return state == S2CChannelPacket.HOLDING ? 1f : 0.25f + 0.75f * Mth.clamp(progress, 0f, 1f); }

  private record Anchor(Vec3 center, Vec3 u, Vec3 v, Vec3 n, float base, boolean grounded) {
    Anchor(Vec3 center, Vec3 u, Vec3 v, Vec3 n, float base) { this(center, u, v, n, base, true); }
  }

  /** Ha chao (bloco firme) ate 4 blocos abaixo do ponto? A coluna nao se manifesta no ar. */
  private static boolean hasGround(Minecraft mc, Vec3 at) {
    var pos = net.minecraft.core.BlockPos.containing(at.add(0, 0.5, 0));
    for (int i = 0; i <= 5; i++) {
      var below = pos.below();
      var st = mc.level.getBlockState(below);
      if (!st.isAir() && st.isFaceSturdy(mc.level, below, net.minecraft.core.Direction.UP)) return true;
      pos = below;
    }
    return false;
  }

  /** Onde as runas ficam: o circulo e o plano dependem da forma da magia. */
  private static Anchor anchor(Minecraft mc, float partial) {
    var player = mc.player;
    Vec3 feet = player.getPosition(partial);
    Vec3 eye = player.getEyePosition(partial);
    Vec3 look = player.getViewVector(partial);
    Vec3 ux = new Vec3(1, 0, 0), vz = new Vec3(0, 0, 1);
    switch (shape) {
      case S2CChannelPacket.SELF:
        return new Anchor(feet.add(0, 0.06, 0), ux, vz, new Vec3(0, 1, 0), 1.0f);
      case S2CChannelPacket.AURA:
        return new Anchor(feet.add(0, 0.06, 0), ux, vz, new Vec3(0, 1, 0), 1.8f);
      case S2CChannelPacket.WAVE: {
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        return new Anchor(feet.add(flat.scale(1.8)).add(0, 0.06, 0), ux, vz, new Vec3(0, 1, 0), 1.4f);
      }
      case S2CChannelPacket.PILLAR: {
        HitResult hit = player.pick(32.0, partial, false);
        Vec3 at = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(10.0)) : hit.getLocation();
        return new Anchor(at.add(0, 0.07, 0), ux, vz, new Vec3(0, 1, 0), 1.5f, hasGround(mc, at));
      }
      default: {
        // base do feixe: circulo de runas na frente da mao, de frente para onde vai a magia
        Vec3 u = Math.abs(look.y) > 0.95 ? new Vec3(1, 0, 0) : look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 v = look.cross(u).normalize();
        Vec3 c = eye.add(look.scale(1.5)).add(0, -0.25, 0);
        return new Anchor(c, u, v, look, 0.55f);
      }
    }
  }

  private static float radius(Anchor a) {
    return a.base * (0.55f + 0.9f * shown);
  }

  @SubscribeEvent
  public static void render(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || state == S2CChannelPacket.OFF) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null) return;
    float partial = event.getPartialTick();
    Anchor a = anchor(mc, partial);
    if (!a.grounded) return; // coluna mirando o ar: nao ha o que mostrar (e a magia falharia)
    PoseStack pose = event.getPoseStack();
    Vec3 cam = event.getCamera().getPosition();
    MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
    pose.pushPose();
    pose.translate(-cam.x, -cam.y, -cam.z);
    VertexConsumer c = buffers.getBuffer(RenderType.lightning());
    PoseStack.Pose last = pose.last();
    int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
    float pulse = state == S2CChannelPacket.HOLDING ? 0.82f + 0.18f * Mth.sin((mc.player.tickCount + partial) * 0.4f) : 1f;
    int alpha = (int) (230 * pulse * Math.min(1f, 0.35f + shown));
    float rad = radius(a);
    float s = spin + partial * 0.06f;
    RuneCircle.draw(c, last, a.center, a.u, a.v, rad, s, r, g, b, alpha);
    // segundo circulo, menor e girando ao contrario, mais perto do conjurador: da profundidade
    if (shape == S2CChannelPacket.BEAM) {
      RuneCircle.draw(c, last, a.center.add(a.n.scale(0.55)), a.u, a.v, rad * 0.62f, -s * 1.3f, 255, 255, 255, (int) (alpha * 0.8f));
      RuneCircle.draw(c, last, a.center.subtract(a.n.scale(0.45)), a.u, a.v, rad * 0.8f, s * 0.8f, r, g, b, (int) (alpha * 0.6f));
    } else if (shape == S2CChannelPacket.PILLAR) {
      // previa da coluna: um fio de luz subindo da runa, mais grosso quanto mais carregada
      float w = 0.05f + 0.22f * shown;
      ShapeFxRenderer.skyColumn(c, last, a.center, a.center.add(0, ShapeFxRenderer.skyHeight(a.center), 0), w, r, g, b, 0.7f * pulse);
    } else {
      RuneCircle.draw(c, last, a.center.add(0, 0.02, 0), a.u, a.v, rad * 0.55f, -s * 1.2f, 255, 255, 255, (int) (alpha * 0.8f));
    }
    pose.popPose();
    buffers.endBatch(RenderType.lightning());
  }
}
