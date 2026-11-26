package com.arcane.client.render;

import com.arcane.entity.RuinGuardianEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Guardiao de Ruina (Fase N): corpo humanoide de pedra rachada; com a casca de gelo de pe, a textura coberta de gelo;
 * na janela de vulnerabilidade, so a pedra. NAO visto em jogo.
 */
public class RuinGuardianRenderer extends HumanoidMobRenderer<RuinGuardianEntity, HumanoidModel<RuinGuardianEntity>> {
  private static final ResourceLocation SHELL = new ResourceLocation("arcane", "textures/entity/ruin_guardian_shell.png");
  private static final ResourceLocation BARE = new ResourceLocation("arcane", "textures/entity/ruin_guardian.png");

  public RuinGuardianRenderer(EntityRendererProvider.Context ctx) {
    super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.6f);
  }

  @Override
  public ResourceLocation getTextureLocation(RuinGuardianEntity entity) { return entity.shellUp() ? SHELL : BARE; }
}
