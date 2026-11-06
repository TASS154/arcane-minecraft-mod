package com.arcane.client.render;

import com.arcane.entity.NPCMageEntity;
import com.arcane.npc.MageElement;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Mago NPC: modelo de jogador (com camadas de manga, calca e jaqueta) e uma entre 18 skins
 * (textures/entity/npc_mage/mage_NN.png), escolhida pela variante da entidade.
 */
public class NPCMageRenderer extends HumanoidMobRenderer<NPCMageEntity, PlayerModel<NPCMageEntity>> {
  private static final ResourceLocation[] TEXTURES = new ResourceLocation[MageElement.VARIANTS];

  static {
    for (int i = 0; i < TEXTURES.length; i++) {
      TEXTURES[i] = new ResourceLocation("arcane", String.format("textures/entity/npc_mage/mage_%02d.png", i));
    }
  }

  public NPCMageRenderer(EntityRendererProvider.Context context) {
    super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
  }

  @Override
  public ResourceLocation getTextureLocation(NPCMageEntity entity) {
    return TEXTURES[Math.floorMod(entity.variant(), TEXTURES.length)];
  }
}
