package com.arcane.client.render;

import com.arcane.registry.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Petrificar: enquanto a criatura (ou o jogador) tem o efeito Petrificado, o PROPRIO modelo dela e desenhado de novo em
 * pedra, por cima da textura original. Funciona com qualquer criatura e com a skin do jogador, sem modelo novo.
 */
public class PetrifyLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
  private static final ResourceLocation STONE = new ResourceLocation("minecraft", "textures/block/stone.png");

  public PetrifyLayer(RenderLayerParent<T, M> parent) {
    super(parent);
  }

  @Override
  public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                     float partial, float age, float headYaw, float headPitch) {
    if (!entity.hasEffect(ModEffects.PETRIFIED.get())) return;
    // levemente mais escuro nas dobras, azulado frio: pedra cinza, nao um cinza chapado
    float tone = 0.74f + 0.06f * (float) Math.sin(age * 0.05);
    getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(STONE)), light, OverlayTexture.NO_OVERLAY,
        tone, tone, tone + 0.04f, 1f);
  }
}
