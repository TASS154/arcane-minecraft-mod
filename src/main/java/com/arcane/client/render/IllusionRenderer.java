package com.arcane.client.render;

import com.arcane.entity.IllusionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** Ilusao: desenhada como o dono (mesma skin, modelo de jogador, armadura e item na mao). */
public class IllusionRenderer extends HumanoidMobRenderer<IllusionEntity, PlayerModel<IllusionEntity>> {
  public IllusionRenderer(EntityRendererProvider.Context ctx) {
    super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
        new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
  }

  @Override
  public ResourceLocation getTextureLocation(IllusionEntity entity) {
    var id = entity.owner().orElse(null);
    if (id == null) return DefaultPlayerSkin.getDefaultSkin();
    var connection = Minecraft.getInstance().getConnection();
    var info = connection == null ? null : connection.getPlayerInfo(id);
    return info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
  }
}
