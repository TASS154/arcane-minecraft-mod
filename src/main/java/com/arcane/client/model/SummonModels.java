package com.arcane.client.model;

import com.arcane.entity.ArcaneGolemEntity;
import com.arcane.entity.SpectralWolfEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Modelos dos invocados (a geometria vem de SummonLayers, gerada por tools/gen_summon_models.py). */
public final class SummonModels {
  public static final ModelLayerLocation WOLF_LAYER = new ModelLayerLocation(new ResourceLocation("arcane", "spectral_wolf"), "main");
  public static final ModelLayerLocation GOLEM_LAYER = new ModelLayerLocation(new ResourceLocation("arcane", "arcane_golem"), "main");

  private SummonModels() {}

  private static void show(boolean visible, ModelPart... parts) {
    for (ModelPart p : parts) p.visible = visible;
  }

  public static class WolfModel extends EntityModel<SpectralWolfEntity> {
    private final ModelPart root, head, tail, legFR, legFL, legBR, legBL;
    private final ModelPart[] tier1, tier2;

    public WolfModel(ModelPart root) {
      this.root = root;
      this.head = root.getChild("head");
      this.tail = root.getChild("tail");
      this.legFR = root.getChild("leg_fr");
      this.legFL = root.getChild("leg_fl");
      this.legBR = root.getChild("leg_br");
      this.legBL = root.getChild("leg_bl");
      this.tier1 = new ModelPart[]{head.getChild("horn1_l"), head.getChild("horn1_r"), root.getChild("pauld1_l"), root.getChild("pauld1_r")};
      this.tier2 = new ModelPart[]{head.getChild("crown2"), root.getChild("chest2"), root.getChild("bigspike0"), root.getChild("bigspike1"), root.getChild("bigspike2")};
    }

    @Override
    public void setupAnim(SpectralWolfEntity wolf, float limb, float limbAmount, float age, float yaw, float pitch) {
      int tier = wolf.tier();
      show(tier >= 1, tier1);
      show(tier >= 2, tier2);
      head.yRot = yaw * Mth.DEG_TO_RAD;
      head.xRot = pitch * Mth.DEG_TO_RAD;
      float swing = 1.4f * limbAmount;
      legBR.xRot = Mth.cos(limb * 0.6662f) * swing;
      legFL.xRot = Mth.cos(limb * 0.6662f) * swing;
      legBL.xRot = Mth.cos(limb * 0.6662f + Mth.PI) * swing;
      legFR.xRot = Mth.cos(limb * 0.6662f + Mth.PI) * swing;
      tail.xRot = 1.15f - limbAmount * 0.5f + Mth.sin(age * 0.11f) * 0.06f;
      tail.zRot = Mth.sin(age * 0.23f) * (wolf.isAggressive() ? 0.05f : 0.22f);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
      root.render(pose, buffer, light, overlay, r, g, b, a);
    }
  }

  public static class GolemModel extends EntityModel<ArcaneGolemEntity> {
    private final ModelPart root, head, armR, armL, legR, legL;
    private final ModelPart[] tier1, tier2;

    public GolemModel(ModelPart root) {
      this.root = root;
      this.head = root.getChild("head");
      this.armR = root.getChild("arm_r");
      this.armL = root.getChild("arm_l");
      this.legR = root.getChild("leg_r");
      this.legL = root.getChild("leg_l");
      this.tier1 = new ModelPart[]{head.getChild("horn1_l"), head.getChild("horn1_r"), armL.getChild("pspike1_l"), armR.getChild("pspike1_r")};
      this.tier2 = new ModelPart[]{root.getChild("halo2_0"), root.getChild("halo2_1"), root.getChild("halo2_2"), armL.getChild("knuck2_l"), armR.getChild("knuck2_r")};
    }

    @Override
    public void setupAnim(ArcaneGolemEntity golem, float limb, float limbAmount, float age, float yaw, float pitch) {
      int tier = golem.tier();
      show(tier >= 1, tier1);
      show(tier >= 2, tier2);
      head.yRot = yaw * Mth.DEG_TO_RAD;
      head.xRot = pitch * Mth.DEG_TO_RAD;
      legR.xRot = -1.5f * Mth.triangleWave(limb, 13.0f) * limbAmount;
      legL.xRot = 1.5f * Mth.triangleWave(limb, 13.0f) * limbAmount;
      int attack = golem.getAttackAnimationTick();
      if (attack > 0) {
        float t = Mth.triangleWave(attack, 10.0f);
        armR.xRot = -2.0f + 1.5f * t;
        armL.xRot = -2.0f + 1.5f * t;
      } else {
        armR.xRot = (-0.2f + 1.5f * Mth.triangleWave(limb, 13.0f)) * limbAmount;
        armL.xRot = (-0.2f - 1.5f * Mth.triangleWave(limb, 13.0f)) * limbAmount;
      }
      // coroa de cristais pulsa
      for (int i = 0; i < 3; i++) {
        ModelPart halo = root.getChild("halo2_" + i);
        halo.yRot = age * 0.08f + i * 2.1f;
      }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
      root.render(pose, buffer, light, overlay, r, g, b, a);
    }
  }
}
