package com.arcane.client.render;

import com.arcane.entity.GrimoireEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Grimorio flutuante: usa o modelo de livro vanilla com textura propria, girando e folheando. */
public class GrimoireRenderer extends EntityRenderer<GrimoireEntity> {
  private static final ResourceLocation TEXTURE = new ResourceLocation("arcane", "textures/entity/grimoire.png");
  private static final int FRAMES = 8;
  /** [estagio-1][quadro]: o brilho varre a capa e as runas pulsam (tools/gen_art_anim.py). */
  private static final ResourceLocation[][] COVERS = new ResourceLocation[4][FRAMES];

  static {
    for (int s = 0; s < 4; s++) {
      for (int f = 0; f < FRAMES; f++) {
        COVERS[s][f] = new ResourceLocation("arcane", "textures/entity/grimoire_s" + (s + 1) + "_f" + f + ".png");
      }
    }
  }

  /** Estagios 1-4 mudam a capa; do 5 em diante usa a capa mais elaborada e cresce um pouco. */
  private static ResourceLocation cover(int stage, float time) {
    int frame = Math.floorMod((int) (time / 3f), FRAMES);
    return COVERS[Math.min(COVERS.length, Math.max(1, stage)) - 1][frame];
  }
  private final BookModel book;

  public GrimoireRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.book = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    this.shadowRadius = 0.15f;
  }

  @Override
  public void render(GrimoireEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
    float time = entity.tickCount + partialTick;
    pose.pushPose();
    pose.translate(0.0, 0.4 + Mth.sin(time * 0.08f) * 0.08, 0.0);
    pose.mulPose(Axis.YP.rotationDegrees(-time * 1.5f));
    pose.mulPose(Axis.ZP.rotationDegrees(80.0f));
    int stage = entity.visualStage();
    float size = 0.8f + Math.min(5, Math.max(0, stage - 4)) * 0.05f;
    pose.scale(size, size, size);

    float flipA = Mth.frac(time * 0.015f + 0.25f) * 1.6f - 0.3f;
    float flipB = Mth.frac(time * 0.015f + 0.75f) * 1.6f - 0.3f;
    float open = 0.9f + Mth.sin(time * 0.05f) * 0.08f;
    book.setupAnim(time, Mth.clamp(flipA, 0f, 1f), Mth.clamp(flipB, 0f, 1f), open);

    VertexConsumer consumer = buffers.getBuffer(book.renderType(cover(stage, time)));
    book.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
    pose.popPose();
    super.render(entity, yaw, partialTick, pose, buffers, light);
  }

  @Override
  public ResourceLocation getTextureLocation(GrimoireEntity entity) {
    return cover(entity.visualStage(), entity.tickCount);
  }
}
