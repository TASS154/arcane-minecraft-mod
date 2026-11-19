package com.arcane.client.render;

import com.arcane.ArcaneMod;
import com.arcane.registry.ModItems;
import com.arcane.ritual.RitualService;
import com.arcane.ritual.RitualTemplate;
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
 * Animacao do compasso (so visual): a tinta e a caneta saem da mao de quem desenha e flutuam ate o altar; a caneta
 * mergulha na tinta e so entao comeca a traçar, passando exatamente pelo traco que o servidor esta colocando.
 * O ritmo e o mesmo do servidor (mesma ordem de varredura, mesma duracao).
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public final class CompassDrawFx {
  private static final class Anim {
    final BlockPos altar;
    final int duration;
    final int ownerId;
    final long startTick;
    final List<RitualTemplate.Cell> order;
    Vec3 from;

    Anim(BlockPos altar, int radius, int duration, int ownerId, long startTick) {
      this.altar = altar;
      this.duration = duration;
      this.ownerId = ownerId;
      this.startTick = startTick;
      this.order = RitualService.sweepOrder(radius);
    }
  }

  /** Fases: subir ate o altar, mergulhar a caneta na tinta, tracar. */
  private static final double RISE = 0.12;
  private static final double DIP = 0.12;
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
    Vec3 look = owner.getLookAngle();
    return owner.getEyePosition().add(look.scale(0.6)).add(0, -0.35, 0);
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
      double p = (now - a.startTick) / (double) a.duration;
      if (p > RISE + DIP) {
        Vec3 tip = penTip(a, p, 0f);
        mc.level.addParticle(ParticleTypes.ENCHANT, tip.x, tip.y, tip.z, 0.0, 0.02, 0.0);
      }
    }
  }

  /** Posicao da ponta da caneta durante o traco. */
  private static Vec3 penTip(Anim a, double p, float partial) {
    double drawP = Mth.clamp((p - RISE - DIP) / (1.0 - RISE - DIP), 0.0, 1.0);
    int idx = Math.min(a.order.size() - 1, (int) (drawP * a.order.size()));
    RitualTemplate.Cell cell = a.order.get(idx);
    return new Vec3(a.altar.getX() + 0.5 + cell.dx(), a.altar.getY() + 0.15, a.altar.getZ() + 0.5 + cell.dz());
  }

  @SubscribeEvent
  public static void onRender(RenderLevelStageEvent event) {
    if (ANIMS.isEmpty() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;
    float partial = event.getPartialTick();
    double now = mc.level.getGameTime() + partial;
    Vec3 cam = event.getCamera().getPosition();
    PoseStack pose = event.getPoseStack();
    MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
    ItemStack pen = new ItemStack(ModItems.ARCANE_PEN.get());
    ItemStack ink = new ItemStack(ModItems.COMMON_INK.get());

    for (Anim a : ANIMS) {
      double p = Mth.clamp((now - a.startTick) / a.duration, 0.0, 1.0);
      Vec3 hover = Vec3.atCenterOf(a.altar).add(0, 1.4, 0);
      if (a.from == null || p < RISE) a.from = handPos(mc, a);
      Vec3 inkPos;
      Vec3 penPos;
      float penTilt = 25f;
      if (p < RISE) {
        double t = ease(p / RISE);
        inkPos = a.from.lerp(hover.add(0, -0.25, 0), t);
        penPos = a.from.add(0.18, 0.1, 0).lerp(hover.add(0.0, 0.45, 0), t);
      } else if (p < RISE + DIP) {
        double t = (p - RISE) / DIP;
        double dip = Math.sin(t * Math.PI);
        inkPos = hover.add(0, -0.25, 0);
        penPos = hover.add(0, 0.45 - 0.55 * dip, 0);
        penTilt = 25f - (float) (15 * dip);
        if (dip > 0.9 && mc.level.random.nextInt(3) == 0) {
          mc.level.addParticle(ParticleTypes.ENCHANT, inkPos.x, inkPos.y + 0.3, inkPos.z, 0.0, 0.05, 0.0);
        }
      } else {
        Vec3 tip = penTip(a, p, partial);
        double drawP = (p - RISE - DIP) / (1.0 - RISE - DIP);
        penPos = tip.add(0, 0.42 + 0.04 * Math.sin(now * 0.9), 0);
        Vec3 center = Vec3.atCenterOf(a.altar);
        Vec3 inward = new Vec3(center.x - tip.x, 0, center.z - tip.z);
        inward = inward.lengthSqr() < 1e-4 ? new Vec3(0, 0, 0) : inward.normalize().scale(0.55);
        inkPos = tip.add(inward).add(0, 0.55 + 0.05 * Math.sin(now * 0.7 + 1.0), 0);
        penTilt = 25f;
        if (drawP >= 1.0) continue;
      }
      draw(pose, buffers, cam, ink, inkPos, 0.55f, (float) (now * 4.0) % 360f, 0f);
      draw(pose, buffers, cam, pen, penPos, 0.6f, 0f, penTilt);
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
