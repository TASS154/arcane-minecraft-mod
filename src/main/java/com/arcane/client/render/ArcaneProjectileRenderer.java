package com.arcane.client.render;

import com.arcane.entity.ArcaneProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Orbe brilhante voltado para a camera (mesma tecnica do DragonFireballRenderer). */
public class ArcaneProjectileRenderer extends EntityRenderer<ArcaneProjectileEntity> {
  private static final ResourceLocation TEXTURE = new ResourceLocation("arcane", "textures/entity/arcane_projectile.png");
  private static final RenderType[] ORB = frames("arcane_projectile_f");
  private static final RenderType[] STORM = frames("arcane_projectile_storm_f");

  private static RenderType[] frames(String prefix) {
    RenderType[] all = new RenderType[4];
    for (int i = 0; i < 4; i++) {
      all[i] = RenderType.entityTranslucentEmissive(new ResourceLocation("arcane", "textures/entity/" + prefix + i + ".png"));
    }
    return all;
  }

  public ArcaneProjectileRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  // ------------------------------------------------------------------ rastro
  /** Ultimas posicoes de cada projetil (no cliente), para o rastro em fita. */
  private static final java.util.Map<Integer, java.util.ArrayDeque<net.minecraft.world.phys.Vec3>> TRAILS = new java.util.HashMap<>();
  private static final int TRAIL_POINTS = 16;

  /**
   * Rastro persistente: uma fita aditiva virada para a camera, ligando as ultimas posicoes do projetil. Afina e
   * esmaece da cabeca para a cauda; o miolo tem a cor do elemento e a borda, a cor secundaria. Com "reduzir clares"
   * o rastro fica mais curto e mais fraco.
   */
  private void renderTrail(ArcaneProjectileEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers) {
    if (TRAILS.size() > 128) TRAILS.keySet().removeIf(id -> entity.level().getEntity(id) == null);
    var here = entity.getPosition(partialTick);
    var points = TRAILS.computeIfAbsent(entity.getId(), id -> new java.util.ArrayDeque<>());
    if (points.isEmpty() || points.peekFirst().distanceToSqr(here) > 0.02) points.addFirst(here);
    boolean soft = com.arcane.client.ClientPrefs.reduceFlashes();
    int max = soft ? TRAIL_POINTS / 2 : TRAIL_POINTS;
    while (points.size() > max) points.pollLast();
    if (points.size() < 2) return;
    var cam = this.entityRenderDispatcher.camera.getPosition();
    VertexConsumer c = buffers.getBuffer(RenderType.lightning());
    Matrix4f m = pose.last().pose();
    int core = entity.element().color(), edge = entity.element().secondary();
    float alphaScale = soft ? 0.45f : 1f;
    var list = new java.util.ArrayList<>(points);
    for (int layer = 0; layer < 2; layer++) {
      int rgb = layer == 0 ? edge : core;
      float widthMul = layer == 0 ? 0.32f : 0.16f;
      int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
      for (int i = 0; i < list.size() - 1; i++) {
        var p0 = list.get(i);
        var p1 = list.get(i + 1);
        float f0 = 1f - i / (float) list.size(), f1 = 1f - (i + 1) / (float) list.size();
        var dir = p1.subtract(p0);
        var side = dir.cross(p0.subtract(cam));
        if (side.lengthSqr() < 1e-6) continue;
        side = side.normalize();
        var s0 = side.scale(widthMul * f0);
        var s1 = side.scale(widthMul * f1);
        var o0 = p0.subtract(here);
        var o1 = p1.subtract(here);
        int a0 = (int) (200 * f0 * alphaScale * (layer == 0 ? 0.6f : 1f)), a1 = (int) (200 * f1 * alphaScale * (layer == 0 ? 0.6f : 1f));
        c.vertex(m, (float) (o0.x - s0.x), (float) (o0.y - s0.y), (float) (o0.z - s0.z)).color(r, g, b, a0).endVertex();
        c.vertex(m, (float) (o0.x + s0.x), (float) (o0.y + s0.y), (float) (o0.z + s0.z)).color(r, g, b, a0).endVertex();
        c.vertex(m, (float) (o1.x + s1.x), (float) (o1.y + s1.y), (float) (o1.z + s1.z)).color(r, g, b, a1).endVertex();
        c.vertex(m, (float) (o1.x - s1.x), (float) (o1.y - s1.y), (float) (o1.z - s1.z)).color(r, g, b, a1).endVertex();
      }
    }
  }

  @Override
  public void render(ArcaneProjectileEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
    renderTrail(entity, partialTick, pose, buffers);
    pose.pushPose();
    float time = entity.tickCount + partialTick;
    float scale = 0.55f + 0.06f * (float) Math.sin(time * 0.6f);
    pose.scale(scale, scale, scale);
    pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
    pose.mulPose(Axis.YP.rotationDegrees(180.0f));
    pose.mulPose(Axis.ZP.rotationDegrees(time * 12f));
    PoseStack.Pose last = pose.last();
    Matrix4f matrix = last.pose();
    Matrix3f normal = last.normal();
    VertexConsumer consumer = buffers.getBuffer((entity.element() == com.arcane.execution.SpellElement.STORM ? STORM : ORB)[Math.floorMod(entity.tickCount / 2, 4)]);
    int full = LightTexture.FULL_BRIGHT;
    int rgb = entity.element().color();
    int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
    vertex(consumer, matrix, normal, full, r, g, b, 0f, 0, 0, 1);
    vertex(consumer, matrix, normal, full, r, g, b, 1f, 0, 1, 1);
    vertex(consumer, matrix, normal, full, r, g, b, 1f, 1, 1, 0);
    vertex(consumer, matrix, normal, full, r, g, b, 0f, 1, 0, 0);
    pose.popPose();
    super.render(entity, yaw, partialTick, pose, buffers, light);
  }

  private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int light, int r, int g, int b, float x, int y, int u, int v) {
    consumer.vertex(matrix, x - 0.5f, y - 0.5f, 0.0f)
        .color(r, g, b, 255)
        .uv((float) u, (float) v)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light)
        .normal(normal, 0.0f, 1.0f, 0.0f)
        .endVertex();
  }

  @Override
  public ResourceLocation getTextureLocation(ArcaneProjectileEntity entity) {
    return TEXTURE;
  }
}
