// GERADO por tools/gen_summon_models.py - nao edite a mao.
package com.arcane.client.model;

import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;

public final class SummonLayers {
  private SummonLayers() {}

  public static LayerDefinition wolf() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
    PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create()
        .texOffs(0, 0).addBox(-4F, -3F, -7F, 8F, 7F, 14F)
        , PartPose.offsetAndRotation(0F, 13F, 2F, 0F, 0F, 0F));
    PartDefinition p_mane = root.addOrReplaceChild("mane", CubeListBuilder.create()
        .texOffs(44, 0).addBox(-5F, -4F, -3.5F, 10F, 9F, 7F)
        , PartPose.offsetAndRotation(0F, 13F, -3.5F, 0F, 0F, 0F));
    PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create()
        .texOffs(94, 0).addBox(-3.5F, -3F, -5F, 7F, 6F, 5F)
        , PartPose.offsetAndRotation(0F, 11.5F, -5.5F, 0F, 0F, 0F));
    PartDefinition p_snout = p_head.addOrReplaceChild("snout", CubeListBuilder.create()
        .texOffs(62, 21).addBox(-2F, 0F, -9F, 4F, 3F, 4F)
        .texOffs(106, 21).addBox(-1.5F, 3F, -8.5F, 3F, 1F, 3F)
        .texOffs(4, 32).addBox(-1.5F, 3F, -9F, 1F, 2F, 1F)
        .texOffs(4, 32).addBox(0.5F, 3F, -9F, 1F, 2F, 1F)
        , PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
    PartDefinition p_ear_l = p_head.addOrReplaceChild("ear_l", CubeListBuilder.create()
        .texOffs(118, 21).addBox(1.5F, -6F, -1F, 2F, 3F, 1F)
        , PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
    PartDefinition p_ear_r = p_head.addOrReplaceChild("ear_r", CubeListBuilder.create()
        .texOffs(118, 21).mirror().addBox(-3.5F, -6F, -1F, 2F, 3F, 1F).mirror(false)
        , PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
    PartDefinition p_leg_fr = root.addOrReplaceChild("leg_fr", CubeListBuilder.create()
        .texOffs(12, 21).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F)
        .texOffs(78, 21).addBox(-2F, 6F, -2.2F, 4F, 1F, 4F)
        , PartPose.offsetAndRotation(-2.5F, 17F, -2F, 0F, 0F, 0F));
    PartDefinition p_leg_fl = root.addOrReplaceChild("leg_fl", CubeListBuilder.create()
        .texOffs(12, 21).mirror().addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F).mirror(false)
        .texOffs(78, 21).mirror().addBox(-2F, 6F, -2.2F, 4F, 1F, 4F).mirror(false)
        , PartPose.offsetAndRotation(2.5F, 17F, -2F, 0F, 0F, 0F));
    PartDefinition p_leg_br = root.addOrReplaceChild("leg_br", CubeListBuilder.create()
        .texOffs(12, 21).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F)
        .texOffs(78, 21).addBox(-2F, 6F, -2.2F, 4F, 1F, 4F)
        , PartPose.offsetAndRotation(-2.5F, 17F, 6.5F, 0F, 0F, 0F));
    PartDefinition p_leg_bl = root.addOrReplaceChild("leg_bl", CubeListBuilder.create()
        .texOffs(12, 21).mirror().addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F).mirror(false)
        .texOffs(78, 21).mirror().addBox(-2F, 6F, -2.2F, 4F, 1F, 4F).mirror(false)
        , PartPose.offsetAndRotation(2.5F, 17F, 6.5F, 0F, 0F, 0F));
    PartDefinition p_tail = root.addOrReplaceChild("tail", CubeListBuilder.create()
        .texOffs(0, 21).addBox(-1.5F, -0.5F, -1.5F, 3F, 8F, 3F)
        .texOffs(94, 21).addBox(-1F, 7.5F, -1F, 2F, 3F, 2F)
        , PartPose.offsetAndRotation(0F, 12F, 9F, 1.15F, 0F, 0F));
    PartDefinition p_spike0 = root.addOrReplaceChild("spike0", CubeListBuilder.create()
        .texOffs(124, 21).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F)
        , PartPose.offsetAndRotation(0F, 10F, -4F, 0.25F, 0F, 0F));
    PartDefinition p_spike1 = root.addOrReplaceChild("spike1", CubeListBuilder.create()
        .texOffs(124, 21).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F)
        , PartPose.offsetAndRotation(0F, 10F, -0.5F, 0.25F, 0F, 0F));
    PartDefinition p_spike2 = root.addOrReplaceChild("spike2", CubeListBuilder.create()
        .texOffs(124, 21).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F)
        , PartPose.offsetAndRotation(0F, 10F, 3F, 0.25F, 0F, 0F));
    PartDefinition p_spike3 = root.addOrReplaceChild("spike3", CubeListBuilder.create()
        .texOffs(124, 21).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F)
        , PartPose.offsetAndRotation(0F, 10F, 6.5F, 0.25F, 0F, 0F));
    PartDefinition p_horn1_l = p_head.addOrReplaceChild("horn1_l", CubeListBuilder.create()
        .texOffs(102, 21).addBox(2F, -9F, -2F, 1F, 4F, 1F)
        .texOffs(8, 32).addBox(2.25F, -10.5F, -2F, 1F, 2F, 1F)
        , PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
    PartDefinition p_horn1_r = p_head.addOrReplaceChild("horn1_r", CubeListBuilder.create()
        .texOffs(102, 21).mirror().addBox(-3F, -9F, -2F, 1F, 4F, 1F).mirror(false)
        .texOffs(8, 32).mirror().addBox(-3.25F, -10.5F, -2F, 1F, 2F, 1F).mirror(false)
        , PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
    PartDefinition p_pauld1_l = root.addOrReplaceChild("pauld1_l", CubeListBuilder.create()
        .texOffs(78, 0).addBox(0F, 0F, -3F, 2F, 6F, 6F)
        .texOffs(12, 32).addBox(1F, -2F, -0.5F, 1F, 2F, 1F)
        , PartPose.offsetAndRotation(4F, 11.5F, -3F, 0F, 0F, 0F));
    PartDefinition p_pauld1_r = root.addOrReplaceChild("pauld1_r", CubeListBuilder.create()
        .texOffs(78, 0).mirror().addBox(-2F, 0F, -3F, 2F, 6F, 6F).mirror(false)
        .texOffs(12, 32).mirror().addBox(-2F, -2F, -0.5F, 1F, 2F, 1F).mirror(false)
        , PartPose.offsetAndRotation(-4F, 11.5F, -3F, 0F, 0F, 0F));
    PartDefinition p_crown2 = p_head.addOrReplaceChild("crown2", CubeListBuilder.create()
        .texOffs(16, 32).addBox(-2.5F, -8F, -1F, 5F, 2F, 1F)
        .texOffs(0, 32).addBox(-0.5F, -11F, -1F, 1F, 3F, 1F)
        , PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
    PartDefinition p_chest2 = root.addOrReplaceChild("chest2", CubeListBuilder.create()
        .texOffs(24, 21).addBox(-4.5F, 3.5F, -4F, 9F, 3F, 6F)
        , PartPose.offsetAndRotation(0F, 13F, -4F, 0F, 0F, 0F));
    PartDefinition p_bigspike0 = root.addOrReplaceChild("bigspike0", CubeListBuilder.create()
        .texOffs(54, 21).addBox(-1F, -7F, -1F, 2F, 7F, 2F)
        , PartPose.offsetAndRotation(0F, 10F, -3F, 0.35F, 0F, 0F));
    PartDefinition p_bigspike1 = root.addOrReplaceChild("bigspike1", CubeListBuilder.create()
        .texOffs(54, 21).addBox(-1F, -7F, -1F, 2F, 7F, 2F)
        , PartPose.offsetAndRotation(0F, 10F, 1F, 0.35F, 0F, 0F));
    PartDefinition p_bigspike2 = root.addOrReplaceChild("bigspike2", CubeListBuilder.create()
        .texOffs(54, 21).addBox(-1F, -7F, -1F, 2F, 7F, 2F)
        , PartPose.offsetAndRotation(0F, 10F, 5F, 0.35F, 0F, 0F));
    return LayerDefinition.create(mesh, 128, 64);
  }

  public static LayerDefinition golem() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
    PartDefinition p_leg_r = root.addOrReplaceChild("leg_r", CubeListBuilder.create()
        .texOffs(28, 0).addBox(-3.5F, 0F, -3F, 7F, 14F, 6F)
        .texOffs(0, 66).addBox(-4F, 14F, -4.5F, 8F, 2F, 9F)
        , PartPose.offsetAndRotation(-4.5F, 8F, 0F, 0F, 0F, 0F));
    PartDefinition p_leg_l = root.addOrReplaceChild("leg_l", CubeListBuilder.create()
        .texOffs(28, 0).mirror().addBox(-3.5F, 0F, -3F, 7F, 14F, 6F).mirror(false)
        .texOffs(0, 66).mirror().addBox(-4F, 14F, -4.5F, 8F, 2F, 9F).mirror(false)
        , PartPose.offsetAndRotation(4.5F, 8F, 0F, 0F, 0F, 0F));
    PartDefinition p_pelvis = root.addOrReplaceChild("pelvis", CubeListBuilder.create()
        .texOffs(80, 47).addBox(-6F, -3F, -4F, 12F, 4F, 8F)
        , PartPose.offsetAndRotation(0F, 8F, 0F, 0F, 0F, 0F));
    PartDefinition p_torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
        .texOffs(54, 0).addBox(-8F, -10F, -5F, 16F, 10F, 10F)
        .texOffs(50, 66).addBox(-3F, -8F, -5.6F, 6F, 6F, 1F)
        , PartPose.offsetAndRotation(0F, 6F, 0F, 0F, 0F, 0F));
    PartDefinition p_chest = root.addOrReplaceChild("chest", CubeListBuilder.create()
        .texOffs(0, 27).addBox(-11F, -8F, -6F, 22F, 8F, 12F)
        , PartPose.offsetAndRotation(0F, -4F, 0F, 0F, 0F, 0F));
    PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create()
        .texOffs(68, 27).addBox(-5F, -9F, -5F, 10F, 9F, 10F)
        .texOffs(84, 66).addBox(-5.5F, -6F, -6F, 11F, 2F, 2F)
        , PartPose.offsetAndRotation(0F, -12F, -1F, 0F, 0F, 0F));
    PartDefinition p_arm_r = root.addOrReplaceChild("arm_r", CubeListBuilder.create()
        .texOffs(0, 0).addBox(-3.5F, 0F, -3.5F, 7F, 20F, 7F)
        .texOffs(0, 47).addBox(-5F, 18F, -5F, 10F, 9F, 10F)
        .texOffs(40, 47).addBox(-5F, -4F, -5F, 10F, 5F, 10F)
        , PartPose.offsetAndRotation(-14F, -10F, 0F, 0F, 0F, 0F));
    PartDefinition p_arm_l = root.addOrReplaceChild("arm_l", CubeListBuilder.create()
        .texOffs(0, 0).mirror().addBox(-3.5F, 0F, -3.5F, 7F, 20F, 7F).mirror(false)
        .texOffs(0, 47).mirror().addBox(-5F, 18F, -5F, 10F, 9F, 10F).mirror(false)
        .texOffs(40, 47).mirror().addBox(-5F, -4F, -5F, 10F, 5F, 10F).mirror(false)
        , PartPose.offsetAndRotation(14F, -10F, 0F, 0F, 0F, 0F));
    PartDefinition p_crystal0 = root.addOrReplaceChild("crystal0", CubeListBuilder.create()
        .texOffs(34, 66).addBox(-1F, -7F, -1F, 2F, 7F, 2F)
        , PartPose.offsetAndRotation(-6F, -6F, 6F, -0.3F, 0F, 0F));
    PartDefinition p_crystal1 = root.addOrReplaceChild("crystal1", CubeListBuilder.create()
        .texOffs(34, 66).addBox(-1F, -7F, -1F, 2F, 7F, 2F)
        , PartPose.offsetAndRotation(0F, -6F, 6F, -0.3F, 0F, 0F));
    PartDefinition p_crystal2 = root.addOrReplaceChild("crystal2", CubeListBuilder.create()
        .texOffs(34, 66).addBox(-1F, -7F, -1F, 2F, 7F, 2F)
        , PartPose.offsetAndRotation(6F, -6F, 6F, -0.3F, 0F, 0F));
    PartDefinition p_horn1_l = p_head.addOrReplaceChild("horn1_l", CubeListBuilder.create()
        .texOffs(64, 66).addBox(3F, -13F, -2F, 2F, 5F, 2F)
        .texOffs(76, 66).addBox(3F, -16F, -2F, 2F, 3F, 2F)
        , PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
    PartDefinition p_horn1_r = p_head.addOrReplaceChild("horn1_r", CubeListBuilder.create()
        .texOffs(64, 66).mirror().addBox(-5F, -13F, -2F, 2F, 5F, 2F).mirror(false)
        .texOffs(76, 66).mirror().addBox(-5F, -16F, -2F, 2F, 3F, 2F).mirror(false)
        , PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
    PartDefinition p_pspike1_l = p_arm_l.addOrReplaceChild("pspike1_l", CubeListBuilder.create()
        .texOffs(42, 66).addBox(-1F, -10F, -1F, 2F, 6F, 2F)
        , PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
    PartDefinition p_pspike1_r = p_arm_r.addOrReplaceChild("pspike1_r", CubeListBuilder.create()
        .texOffs(42, 66).mirror().addBox(-1F, -10F, -1F, 2F, 6F, 2F).mirror(false)
        , PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
    PartDefinition p_halo2_0 = root.addOrReplaceChild("halo2_0", CubeListBuilder.create()
        .texOffs(72, 66).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F)
        , PartPose.offsetAndRotation(-4F, -21F, -1F, 0F, 0F, -0.35F));
    PartDefinition p_halo2_1 = root.addOrReplaceChild("halo2_1", CubeListBuilder.create()
        .texOffs(72, 66).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F)
        , PartPose.offsetAndRotation(0F, -21F, -1F, 0F, 0F, 0F));
    PartDefinition p_halo2_2 = root.addOrReplaceChild("halo2_2", CubeListBuilder.create()
        .texOffs(72, 66).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F)
        , PartPose.offsetAndRotation(4F, -21F, -1F, 0F, 0F, 0.35F));
    PartDefinition p_knuck2_l = p_arm_l.addOrReplaceChild("knuck2_l", CubeListBuilder.create()
        .texOffs(110, 66).addBox(-4F, 21F, -6F, 2F, 2F, 1F)
        .texOffs(110, 66).addBox(2F, 21F, -6F, 2F, 2F, 1F)
        , PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
    PartDefinition p_knuck2_r = p_arm_r.addOrReplaceChild("knuck2_r", CubeListBuilder.create()
        .texOffs(110, 66).addBox(-4F, 21F, -6F, 2F, 2F, 1F)
        .texOffs(110, 66).addBox(2F, 21F, -6F, 2F, 2F, 1F)
        , PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
    return LayerDefinition.create(mesh, 128, 128);
  }
}
