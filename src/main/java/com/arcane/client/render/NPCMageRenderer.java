package com.arcane.client.render;

import com.arcane.entity.NPCMageEntity;
import com.arcane.npc.MageElement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Mago NPC: modelo de jogador com uma entre 18 skins, camada luminosa animada (olhos, maos e runa no peito, na cor
 * do elemento) e animacao de conjuracao: os bracos sobem e o brilho acompanha.
 */
public class NPCMageRenderer extends HumanoidMobRenderer<NPCMageEntity, NPCMageRenderer.MageModel> {
  private static final ResourceLocation[] TEXTURES = new ResourceLocation[MageElement.VARIANTS];
  private static final int GLOW_FRAMES = 6;
  private static final ResourceLocation[][] GLOW = new ResourceLocation[6][GLOW_FRAMES];

  static {
    for (int i = 0; i < TEXTURES.length; i++) {
      TEXTURES[i] = new ResourceLocation("arcane", String.format("textures/entity/npc_mage/mage_%02d.png", i));
    }
    for (int e = 0; e < GLOW.length; e++) {
      for (int k = 0; k < GLOW_FRAMES; k++) {
        GLOW[e][k] = new ResourceLocation("arcane", "textures/entity/npc_mage/glow_" + e + "_" + k + ".png");
      }
    }
  }

  /** Bracos erguidos enquanto conjura, balanco leve de "flutuar" parado. */
  public static class MageModel extends PlayerModel<NPCMageEntity> {
    public MageModel(ModelPart root) {
      super(root, false);
    }

    @Override
    public void setupAnim(NPCMageEntity mage, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
      super.setupAnim(mage, limbSwing, limbSwingAmount, age, yaw, pitch);
      int casting = mage.castingTicks();
      if (casting > 0) {
        float lift = Mth.clamp(casting / 14f, 0f, 1f);
        float wave = Mth.sin(age * 0.6f) * 0.08f;
        this.rightArm.xRot = Mth.lerp(lift, this.rightArm.xRot, -2.4f + wave);
        this.leftArm.xRot = Mth.lerp(lift, this.leftArm.xRot, -2.4f - wave);
        this.rightArm.zRot = Mth.lerp(lift, this.rightArm.zRot, 0.25f);
        this.leftArm.zRot = Mth.lerp(lift, this.leftArm.zRot, -0.25f);
      } else {
        this.rightArm.zRot += Mth.cos(age * 0.09f) * 0.04f + 0.04f;
        this.leftArm.zRot -= Mth.cos(age * 0.09f) * 0.04f + 0.04f;
        this.rightArm.xRot += Mth.sin(age * 0.067f) * 0.04f;
        this.leftArm.xRot -= Mth.sin(age * 0.067f) * 0.04f;
      }
      this.rightSleeve.copyFrom(this.rightArm);
      this.leftSleeve.copyFrom(this.leftArm);
    }
  }

  /** Brilho de elemento por cima da skin, sempre aceso (nao depende da luz do ambiente). */
  private static class GlowLayer extends RenderLayer<NPCMageEntity, MageModel> {
    GlowLayer(RenderLayerParent<NPCMageEntity, MageModel> parent) {
      super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, NPCMageEntity mage, float limbSwing,
                       float limbSwingAmount, float partialTick, float age, float yaw, float pitch) {
      if (mage.isInvisible()) return;
      int element = Math.floorMod(mage.element().ordinal(), GLOW.length);
      int speed = mage.castingTicks() > 0 ? 1 : 3;
      ResourceLocation tex = GLOW[element][Math.floorMod((int) (age / speed), GLOW_FRAMES)];
      VertexConsumer consumer = buffers.getBuffer(RenderType.eyes(tex));
      getParentModel().renderToBuffer(pose, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
    }
  }

  public NPCMageRenderer(EntityRendererProvider.Context context) {
    super(context, new MageModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    this.addLayer(new GlowLayer(this));
  }

  @Override
  public ResourceLocation getTextureLocation(NPCMageEntity entity) {
    return TEXTURES[Math.floorMod(entity.variant(), TEXTURES.length)];
  }
}
