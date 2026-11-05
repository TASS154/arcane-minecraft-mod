package com.arcane.client.render;

import com.arcane.entity.NPCMageEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Mago NPC: modelo humanoide padrao (camada do zumbi, 64x64) com skin propria de manto e capuz. */
public class NPCMageRenderer extends HumanoidMobRenderer<NPCMageEntity, HumanoidModel<NPCMageEntity>> {
  private static final ResourceLocation TEXTURE = new ResourceLocation("arcane", "textures/entity/npc_mage.png");

  public NPCMageRenderer(EntityRendererProvider.Context context) {
    super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
  }

  @Override
  public ResourceLocation getTextureLocation(NPCMageEntity entity) {
    return TEXTURE;
  }
}
