package com.arcane.client.render;

import com.arcane.client.model.SummonModels;
import com.arcane.entity.ArcaneGolemEntity;
import com.arcane.entity.SpectralWolfEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

/** Renderizadores dos invocados: modelo proprio por nivel + camada luminosa (olhos, veias, nucleo). */
public final class SummonRenderers {
  private static final ResourceLocation[] WOLF = new ResourceLocation[3], WOLF_GLOW = new ResourceLocation[3];
  private static final ResourceLocation[] GOLEM = new ResourceLocation[3], GOLEM_GLOW = new ResourceLocation[3];

  static {
    for (int i = 0; i < 3; i++) {
      WOLF[i] = new ResourceLocation("arcane", "textures/entity/summon/wolf_" + i + ".png");
      WOLF_GLOW[i] = new ResourceLocation("arcane", "textures/entity/summon/wolf_" + i + "_glow.png");
      GOLEM[i] = new ResourceLocation("arcane", "textures/entity/summon/golem_" + i + ".png");
      GOLEM_GLOW[i] = new ResourceLocation("arcane", "textures/entity/summon/golem_" + i + "_glow.png");
    }
  }

  private SummonRenderers() {}

  /** Camada que redesenha o modelo com a textura luminosa, sempre no brilho maximo. */
  private static final class GlowLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private final Function<T, ResourceLocation> texture;

    GlowLayer(RenderLayerParent<T, M> parent, Function<T, ResourceLocation> texture) {
      super(parent);
      this.texture = texture;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limb, float limbAmount, float partial,
                       float age, float yaw, float pitch) {
      if (entity.isInvisible()) return;
      getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(texture.apply(entity))), 15728640, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
    }
  }

  public static class Wolf extends MobRenderer<SpectralWolfEntity, SummonModels.WolfModel> {
    public Wolf(EntityRendererProvider.Context ctx) {
      super(ctx, new SummonModels.WolfModel(ctx.bakeLayer(SummonModels.WOLF_LAYER)), 0.5f);
      addLayer(new GlowLayer<>(this, w -> WOLF_GLOW[w.tier()]));
    }

    @Override
    protected void scale(SpectralWolfEntity wolf, PoseStack pose, float partial) {
      float s = wolf.visualScale();
      pose.scale(s, s, s);
      this.shadowRadius = 0.5f * s;
    }

    @Override
    public ResourceLocation getTextureLocation(SpectralWolfEntity wolf) {
      return WOLF[wolf.tier()];
    }
  }

  public static class Golem extends MobRenderer<ArcaneGolemEntity, SummonModels.GolemModel> {
    public Golem(EntityRendererProvider.Context ctx) {
      super(ctx, new SummonModels.GolemModel(ctx.bakeLayer(SummonModels.GOLEM_LAYER)), 0.7f);
      addLayer(new GlowLayer<>(this, g -> GOLEM_GLOW[g.tier()]));
    }

    @Override
    protected void scale(ArcaneGolemEntity golem, PoseStack pose, float partial) {
      float s = golem.visualScale();
      pose.scale(s, s, s);
      this.shadowRadius = 0.7f * s;
    }

    @Override
    public ResourceLocation getTextureLocation(ArcaneGolemEntity golem) {
      return GOLEM[golem.tier()];
    }
  }
}
