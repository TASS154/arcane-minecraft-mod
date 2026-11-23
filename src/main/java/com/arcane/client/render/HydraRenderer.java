package com.arcane.client.render;

import com.arcane.entity.HydraEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Hidra: corpo de reptil com quatro patas, cauda de tres gomos e ate 9 pescocos (cada um com dois gomos, cabeca e
 * mandibula). O estagio de vida escolhe quantos pescocos aparecem, o tamanho e a cor: verde-claro na recem-nascida,
 * azul-petroleo no meio da vida e carmesim-dourado na anciã. Os olhos brilham no escuro.
 */
public class HydraRenderer extends MobRenderer<HydraEntity, HydraRenderer.HydraModel> {
  public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("arcane", "hydra"), "main");
  private static final ResourceLocation TEXTURE = new ResourceLocation("arcane", "textures/entity/hydra/hydra.png");
  private static final ResourceLocation EYES = new ResourceLocation("arcane", "textures/entity/hydra/hydra_eyes.png");

  public HydraRenderer(EntityRendererProvider.Context context) {
    super(context, new HydraModel(context.bakeLayer(LAYER)), 1.2f);
    addLayer(new Eyes(this));
  }

  @Override
  public ResourceLocation getTextureLocation(HydraEntity entity) { return TEXTURE; }

  /**
   * Selada: correntes de luz carmesim da Hidra ate cada selo inteiro nos pilares, e aneis de runas apertando o corpo.
   * Desperta a partir do estagio 5: uma aura de brasas girando em volta (sinal de que ela devasta a regiao).
   */
  @Override
  public void render(HydraEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
    super.render(e, yaw, partial, pose, buffers, light);
    float age = e.tickCount + partial;
    VertexConsumer c = buffers.getBuffer(RenderType.lightning());
    PoseStack.Pose last = pose.last();
    net.minecraft.world.phys.Vec3 body = new net.minecraft.world.phys.Vec3(0, e.getBbHeight() * 0.5, 0);
    float w = e.getBbWidth();
    if (e.sealed()) {
      net.minecraft.world.phys.Vec3 me = e.getPosition(partial);
      for (int i = 0; i < HydraEntity.PILLARS; i++) {
        if ((e.sealMask() & (1 << i)) == 0) continue;
        var top = e.home().offset(HydraEntity.pillarTop(i));
        net.minecraft.world.phys.Vec3 seal = net.minecraft.world.phys.Vec3.atCenterOf(top).subtract(me);
        // corrente: elos alternados, com uma leve curva para baixo
        int links = 18;
        for (int k = 0; k < links; k++) {
          float a = k / (float) links, b = (k + 0.7f) / links;
          var p0 = body.lerp(seal, a).add(0, -Math.sin(a * Math.PI) * 1.2, 0);
          var p1 = body.lerp(seal, b).add(0, -Math.sin(b * Math.PI) * 1.2, 0);
          float pulse = 0.6f + 0.4f * Mth.sin(age * 0.2f - k * 0.5f);
          OriginalFxRenderer.line(c, last, p0, p1, k % 2 == 0 ? 0.07f : 0.04f, 255, 40, 70, (int) (200 * pulse));
        }
      }
      for (int r = 0; r < 3; r++) {
        float y = (float) body.y + (r - 1) * e.getBbHeight() * 0.3f;
        float spin = age * 0.03f * (r % 2 == 0 ? 1 : -1);
        ShapeFxRenderer.ring(c, last, new net.minecraft.world.phys.Vec3(0, y, 0), w * 0.75f, 0.06f, spin, spin + 6.2831855f, 40, 200, 40, 255, 200);
      }
      RuneCircle.draw(c, last, new net.minecraft.world.phys.Vec3(0, 0.05, 0), new net.minecraft.world.phys.Vec3(1, 0, 0), new net.minecraft.world.phys.Vec3(0, 0, 1),
          w * 1.3f, age * 0.01f, 255, 40, 80, 220);
    } else if (HydraEntity.devastates(e.stage())) {
      for (int i = 0; i < 10; i++) {
        float ang = i * 0.628f + age * 0.05f;
        float rr = w * (0.8f + 0.2f * Mth.sin(age * 0.1f + i));
        var at = new net.minecraft.world.phys.Vec3(Math.cos(ang) * rr, (i % 5) * e.getBbHeight() * 0.2f + 0.2f, Math.sin(ang) * rr);
        ShapeFxRenderer.octahedron(c, last, at, 0.08f + 0.02f * e.stage() * 0.3f, 255, 110, 40, 200);
      }
    }
  }

  @Override
  protected void scale(HydraEntity entity, PoseStack pose, float partialTick) {
    float s = HydraEntity.scaleFor(entity.stage());
    pose.scale(s, s, s);
    this.shadowRadius = 1.2f * s;
  }

  private static final class Eyes extends EyesLayer<HydraEntity, HydraModel> {
    private static final RenderType TYPE = RenderType.eyes(EYES);

    Eyes(RenderLayerParent<HydraEntity, HydraModel> parent) { super(parent); }

    @Override
    public RenderType renderType() { return TYPE; }
  }

  /** Cor do couro por estagio (1 a 14), em tres paradas. */
  static float[] tint(int stage) {
    float t = (Math.max(1, Math.min(14, stage)) - 1) / 13f;
    float[] a = {0.62f, 1.0f, 0.58f}, b = {0.42f, 0.72f, 1.0f}, c = {1.0f, 0.5f, 0.32f};
    float[] out = new float[3];
    for (int i = 0; i < 3; i++) out[i] = t < 0.5f ? Mth.lerp(t * 2, a[i], b[i]) : Mth.lerp((t - 0.5f) * 2, b[i], c[i]);
    return out;
  }

  public static final class HydraModel extends HierarchicalModel<HydraEntity> {
    public static final int NECKS = 9;
    private final ModelPart root, body, tail1, tail2, tail3;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] neck = new ModelPart[NECKS], neck2 = new ModelPart[NECKS], head = new ModelPart[NECKS], jaw = new ModelPart[NECKS];
    private float r = 1, g = 1, b = 1;

    public HydraModel(ModelPart root) {
      this.root = root;
      this.body = root.getChild("body");
      for (int i = 0; i < 4; i++) legs[i] = root.getChild("leg" + i);
      tail1 = root.getChild("tail1");
      tail2 = tail1.getChild("tail2");
      tail3 = tail2.getChild("tail3");
      for (int i = 0; i < NECKS; i++) {
        neck[i] = root.getChild("neck" + i);
        neck2[i] = neck[i].getChild("neck2");
        head[i] = neck2[i].getChild("head");
        jaw[i] = head[i].getChild("jaw");
      }
    }

    public static LayerDefinition createLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition p = mesh.getRoot();
      p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8, -6, -12, 16, 12, 24), PartPose.offset(0, 12, 0));
      int[][] legPos = {{-6, -8}, {6, -8}, {-6, 8}, {6, 8}};
      for (int i = 0; i < 4; i++) {
        p.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 40).addBox(-3, 0, -3, 6, 8, 6), PartPose.offset(legPos[i][0], 16, legPos[i][1]));
      }
      PartDefinition t1 = p.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(0, 60).addBox(-4, -4, 0, 8, 8, 10), PartPose.offset(0, 11, 11));
      PartDefinition t2 = t1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(40, 60).addBox(-3, -3, 0, 6, 6, 10), PartPose.offset(0, 0, 9));
      t2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(72, 60).addBox(-2, -2, 0, 4, 4, 10), PartPose.offset(0, 0, 9));
      for (int i = 0; i < NECKS; i++) {
        PartDefinition n1 = p.addOrReplaceChild("neck" + i, CubeListBuilder.create().texOffs(84, 0).addBox(-2, -2, -10, 4, 4, 10), PartPose.offset(0, 8, -10));
        PartDefinition n2 = n1.addOrReplaceChild("neck2", CubeListBuilder.create().texOffs(84, 16).addBox(-1.5f, -1.5f, -9, 3, 3, 9), PartPose.offset(0, 0, -9));
        PartDefinition hd = n2.addOrReplaceChild("head", CubeListBuilder.create().texOffs(84, 30).addBox(-3, -2.5f, -8, 6, 5, 8)
            .texOffs(112, 30).addBox(-2.5f, -4.5f, -3, 1, 2, 2).texOffs(112, 30).addBox(1.5f, -4.5f, -3, 1, 2, 2), PartPose.offset(0, 0, -8));
        hd.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(84, 46).addBox(-2.5f, 0, -7, 5, 2, 7), PartPose.offset(0, 1.5f, -0.5f));
      }
      return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(HydraEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
      root.getAllParts().forEach(ModelPart::resetPose);
      float[] c = tint(e.stage());
      r = c[0]; g = c[1]; b = c[2];
      // patas: passada de reptil (diagonais juntas)
      float walk = Mth.cos(limbSwing * 0.6f) * 0.9f * limbSwingAmount;
      legs[0].xRot = walk; legs[3].xRot = walk; legs[1].xRot = -walk; legs[2].xRot = -walk;
      body.zRot = Mth.sin(limbSwing * 0.6f) * 0.04f * limbSwingAmount;
      // cauda: serpenteia
      tail1.xRot = -0.25f + Mth.sin(age * 0.05f) * 0.05f;
      tail1.yRot = Mth.sin(age * 0.07f) * 0.25f;
      tail2.yRot = Mth.sin(age * 0.07f - 0.8f) * 0.3f;
      tail3.yRot = Mth.sin(age * 0.07f - 1.6f) * 0.35f;
      tail2.xRot = 0.15f; tail3.xRot = 0.1f;
      int n = e.heads();
      boolean casting = e.power() != 0;
      for (int i = 0; i < NECKS; i++) {
        boolean show = i < n;
        neck[i].visible = show;
        if (!show) continue;
        // pescocos em leque bem aberto na frente do corpo: os de fora saem para os lados, os do meio sobem mais;
        // com muitas cabecas ha duas fileiras (as pares mais altas e mais para tras)
        float spread = n == 1 ? 0f : (i / (float) (n - 1)) * 2f - 1f;
        float row = n > 4 ? (i % 2) : 0;
        neck[i].x = spread * 7.5f;
        neck[i].y = 8f - row * 3f;
        neck[i].z = -10f + row * 3f;
        neck[i].yRot = spread * (n > 3 ? 1.05f : 0.6f) + netHeadYaw * Mth.DEG_TO_RAD * 0.3f;
        neck[i].zRot = -spread * 0.35f;
        float sway = Mth.sin(age * 0.09f + i * 1.3f) * 0.12f;
        neck[i].xRot = -0.75f - row * 0.35f - (1f - Math.abs(spread)) * 0.2f + sway + (casting ? -0.25f : 0f);
        neck2[i].xRot = 0.55f + Mth.sin(age * 0.11f + i) * 0.1f;
        neck2[i].yRot = Mth.sin(age * 0.06f + i * 2.1f) * 0.15f;
        head[i].xRot = 0.35f + headPitch * Mth.DEG_TO_RAD * 0.5f;
        float bite = casting ? 0.55f + Mth.sin(age * 0.8f + i) * 0.15f : Math.max(0f, Mth.sin(age * 0.05f + i * 3f)) * 0.15f;
        jaw[i].xRot = bite + (attackTime > 0 ? 0.5f : 0f);
      }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
      root.render(pose, consumer, light, overlay, red * r, green * g, blue * b, alpha);
    }
  }
}
