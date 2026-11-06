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
  private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

  public ArcaneProjectileRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public void render(ArcaneProjectileEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
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
    VertexConsumer consumer = buffers.getBuffer(RENDER_TYPE);
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
