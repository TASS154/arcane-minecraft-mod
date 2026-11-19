package com.arcane.client.render;

import com.arcane.ArcaneMod;
import com.arcane.registry.ModItems;
import com.arcane.ritual.CompassDrawing;
import com.arcane.ritual.CompassPath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Animacao do compasso (so visual): o frasco e a caneta saem da mao de quem desenha e flutuam ate o altar; a caneta
 * mergulha na tinta, vai ate o primeiro traco e entao percorre o caminho (aneis primeiro, depois as hastes),
 * com movimento continuo. O servidor so poe cada traco depois que a caneta passa por ele (mesmo caminho, mesma duracao).
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public final class CompassDrawFx {
  private static final class Anim {
    final BlockPos altar;
    final int duration;
    final int ownerId;
    final long startTick;
    final CompassPath path;
    Vec3 from;
    Vec3 inkSmooth;

    Anim(BlockPos altar, int radius, int duration, int ownerId, long startTick) {
      this.altar = altar;
      this.duration = duration;
      this.ownerId = ownerId;
      this.startTick = startTick;
      this.path = CompassPath.of(radius);
    }
  }

  /** Fases (fracao do tempo): subir, mergulhar, ir ao primeiro traco; a soma e CompassDrawing.LEAD. */
  private static final double RISE = 0.10;
  private static final double DIP = 0.10;
  private static final double TRAVEL = CompassDrawing.LEAD - RISE - DIP;
  private static final float PEN_SCALE = 0.5f;
  private static final float INK_SCALE = 0.4f;
  private static final float PEN_TILT = 35f;
  private static final List<Anim> ANIMS = new ArrayList<>();

  private CompassDrawFx() {}

  public static void start(BlockPos altar, int radius, int duration, int ownerId) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;
    ANIMS.add(new Anim(altar, radius, duration, ownerId, mc.level.getGameTime()));
  }

  private static Vec3 handPos(Minecraft mc, Anim a) {
    Entity owner = mc.level.getEntity(a.ownerId);
    if (owner == null) return Vec3.atCenterOf(a.altar).add(0, 1.0, 0);
    return owner.getEyePosition().add(owner.getLookAngle().scale(0.6)).add(0, -0.4, 0);
  }

  /** Ponta da caneta no chao, na celula dada (com salto: erguida). */
  private static Vec3 groundTip(Anim a, double[] pos) {
    return new Vec3(a.altar.getX() + 0.5 + pos[0], a.altar.getY() + 0.12 + pos[2] * 0.9, a.altar.getZ() + 0.5 + pos[1]);
  }

  /** Estado da caneta: {x, y, z, desenhando (1/0)}; a ponta, nao o centro do modelo. */
  private static double[] penState(Anim a, double p, Vec3 hover) {
    Vec3 hoverPen = hover.add(0, 0.55, 0);
    Vec3 inkTop = hover.add(0, 0.15, 0);
    if (p < RISE) {
      Vec3 s = a.from.add(0.2, 0.0, 0.0).lerp(hoverPen, ease(p / RISE));
      return new double[] {s.x, s.y, s.z, 0};
    }
    if (p < RISE + DIP) {
      double t = (p - RISE) / DIP;
      double dip = Math.sin(t * Math.PI);
      Vec3 s = hoverPen.lerp(inkTop, dip);
      return new double[] {s.x, s.y, s.z, dip > 0.92 ? 2 : 0};
    }
    Vec3 first = groundTip(a, a.path.at(0));
    if (p < CompassDrawing.LEAD) {
      double t = ease((p - RISE - DIP) / TRAVEL);
      Vec3 s = hoverPen.lerp(first, t).add(0, Math.sin(t * Math.PI) * 0.5, 0);
      return new double[] {s.x, s.y, s.z, 0};
    }
    double drawP = (p - CompassDrawing.LEAD) / (1.0 - CompassDrawing.LEAD);
    double[] at = a.path.at(drawP * a.path.total);
    Vec3 s = groundTip(a, at);
    return new double[] {s.x, s.y, s.z, at[2] > 0.001 ? 0 : 1};
  }

  @SubscribeEvent
  public static void onTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END || ANIMS.isEmpty()) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) {
      ANIMS.clear();
      return;
    }
    long now = mc.level.getGameTime();
    ANIMS.removeIf(a -> now - a.startTick > a.duration + 5);
    for (Anim a : ANIMS) {
      double p = Mth.clamp((now - a.startTick) / (double) a.duration, 0.0, 1.0);
      if (a.from == null) continue;
      double[] st = penState(a, p, Vec3.atCenterOf(a.altar).add(0, 1.3, 0));
      if (st[3] == 1) mc.level.addParticle(ParticleTypes.ENCHANT, st[0], st[1] + 0.05, st[2], 0.0, 0.03, 0.0);
      else if (st[3] == 2 && mc.level.random.nextInt(2) == 0) mc.level.addParticle(ParticleTypes.ENCHANT, st[0], st[1] + 0.2, st[2], 0.0, 0.05, 0.0);
    }
  }

  @SubscribeEvent
  public static void onRender(RenderLevelStageEvent event) {
    if (ANIMS.isEmpty() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;
    double now = mc.level.getGameTime() + event.getPartialTick();
    Vec3 cam = event.getCamera().getPosition();
    PoseStack pose = event.getPoseStack();
    MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
    ItemStack pen = new ItemStack(ModItems.ARCANE_PEN.get());
    ItemStack ink = new ItemStack(ModItems.COMMON_INK.get());

    for (Anim a : ANIMS) {
      double p = Mth.clamp((now - a.startTick) / a.duration, 0.0, 1.0);
      if (p >= 1.0) continue;
      if (a.from == null || p < RISE) a.from = handPos(mc, a);
      Vec3 hover = Vec3.atCenterOf(a.altar).add(0, 1.3, 0);
      double[] st = penState(a, p, hover);
      Vec3 tip = new Vec3(st[0], st[1], st[2]);

      // Tinta: sobe da mao, fica ao lado do altar durante o mergulho e depois acompanha a caneta, suavizada.
      Vec3 inkTarget;
      if (p < RISE) inkTarget = a.from.lerp(hover, ease(p / RISE));
      else if (p < CompassDrawing.LEAD) inkTarget = hover;
      else {
        Vec3 center = Vec3.atCenterOf(a.altar);
        Vec3 inward = new Vec3(center.x - tip.x, 0, center.z - tip.z);
        inward = inward.lengthSqr() < 1e-4 ? Vec3.ZERO : inward.normalize().scale(0.6);
        inkTarget = tip.add(inward).add(0, 0.7 + 0.05 * Math.sin(now * 0.6), 0);
      }
      a.inkSmooth = a.inkSmooth == null || p < RISE + DIP ? inkTarget : a.inkSmooth.lerp(inkTarget, 0.12);

      // Centro do modelo da caneta, de modo que a PONTA fique exatamente em "tip".
      double theta = Math.toRadians(PEN_TILT - 45.0);
      Vec3 penCenter;
      penCenter = tip.add(-0.5 * PEN_SCALE * Math.sin(theta), 0.5 * PEN_SCALE * Math.cos(theta), 0.0);

      draw(pose, buffers, cam, ink, a.inkSmooth, INK_SCALE, (float) (now * 3.0) % 360f, 0f);
      draw(pose, buffers, cam, pen, penCenter, PEN_SCALE, 0f, PEN_TILT);
    }
    buffers.endBatch();
  }

  private static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack stack, Vec3 pos, float scale, float spin, float tilt) {
    pose.pushPose();
    pose.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
    pose.mulPose(Axis.YP.rotationDegrees(spin));
    pose.mulPose(Axis.ZP.rotationDegrees(tilt));
    pose.scale(scale, scale, scale);
    Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
        OverlayTexture.NO_OVERLAY, pose, buffers, Minecraft.getInstance().level, 0);
    pose.popPose();
  }

  private static double ease(double t) {
    t = Mth.clamp(t, 0.0, 1.0);
    return t * t * (3 - 2 * t);
  }
}
