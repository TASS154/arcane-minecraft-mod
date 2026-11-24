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
 * Hidra arcana: escamas de obsidiana com veias de runas ciano (que brilham, junto com os olhos, na camada emissiva),
 * espinhos, chifres e dentes de osso. O estagio de vida escolhe quantos pescocos aparecem, o tamanho e um leve tom
 * na obsidiana: verde na recem-nascida, azul no meio da vida e carmesim na ancia.
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

  /** Cor do couro por estagio (1 a 14), em tres paradas; aplicada de leve sobre a obsidiana. */
  static float[] tint(int stage) {
    float t = (Math.max(1, Math.min(14, stage)) - 1) / 13f;
    float[] a = {0.62f, 1.0f, 0.58f}, b = {0.42f, 0.72f, 1.0f}, c = {1.0f, 0.5f, 0.32f};
    float[] out = new float[3];
    for (int i = 0; i < 3; i++) out[i] = t < 0.5f ? Mth.lerp(t * 2, a[i], b[i]) : Mth.lerp((t - 0.5f) * 2, b[i], c[i]);
    return out;
  }

  /**
   * Modelo: corpo de obsidiana com espinhos no dorso, peito saliente, patas com coxa, canela e garras, cauda de quatro
   * gomos terminando em lamina, e ate 9 pescocos de tres gomos com espinhos, cada cabeca com focinho, chifres, olhos
   * (camada que brilha) e dentes serrilhados em cima e embaixo. Animacoes: andar (joelho dobra), respirar, pescocos em
   * onda, mordida (golpe corpo a corpo), rugido, sopro, golpe de cauda, conjurar (cabecas para o ceu) e dormir selada.
   */
  public static final class HydraModel extends HierarchicalModel<HydraEntity> {
    public static final int NECKS = 9;
    private static final int BONE_U = 224, BONE_V = 0, EYE_U = 224, EYE_V = 96;
    private final ModelPart root, body, tail1, tail2, tail3, tail4;
    private final ModelPart[] thigh = new ModelPart[4], shin = new ModelPart[4];
    private final ModelPart[] neck = new ModelPart[NECKS], neck2 = new ModelPart[NECKS], neck3 = new ModelPart[NECKS],
        head = new ModelPart[NECKS], jaw = new ModelPart[NECKS];
    private float r = 1, g = 1, b = 1;

    public HydraModel(ModelPart root) {
      this.root = root;
      this.body = root.getChild("body");
      for (int i = 0; i < 4; i++) {
        thigh[i] = root.getChild("leg" + i);
        shin[i] = thigh[i].getChild("shin");
      }
      tail1 = root.getChild("tail1");
      tail2 = tail1.getChild("tail2");
      tail3 = tail2.getChild("tail3");
      tail4 = tail3.getChild("tail4");
      for (int i = 0; i < NECKS; i++) {
        neck[i] = root.getChild("neck" + i);
        neck2[i] = neck[i].getChild("neck2");
        neck3[i] = neck2[i].getChild("neck3");
        head[i] = neck3[i].getChild("head");
        jaw[i] = head[i].getChild("jaw");
      }
    }

    private static CubeListBuilder spikes(CubeListBuilder b, float y, float zFrom, float zTo, float step, float size) {
      for (float z = zFrom; z <= zTo; z += step) b.texOffs(BONE_U, BONE_V).addBox(-size / 2, y - size * 1.5f, z, size, size * 1.5f, size);
      return b;
    }

    public static LayerDefinition createLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition p = mesh.getRoot();
      CubeListBuilder bodyCubes = CubeListBuilder.create()
          .texOffs(0, 0).addBox(-8, -6, -12, 16, 12, 24)
          .texOffs(0, 40).addBox(-9, -5, -15, 18, 10, 6);
      spikes(bodyCubes, -6, -10, 10, 4, 2);
      p.addOrReplaceChild("body", bodyCubes, PartPose.offset(0, 12, 0));
      int[][] legPos = {{-7, -8}, {7, -8}, {-7, 8}, {7, 8}};
      for (int i = 0; i < 4; i++) {
        PartDefinition th = p.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 60).addBox(-3.5f, -1, -3.5f, 7, 7, 7),
            PartPose.offset(legPos[i][0], 15, legPos[i][1]));
        th.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(28, 60).addBox(-2.5f, 0, -2.5f, 5, 4.5f, 5)
            .texOffs(BONE_U, BONE_V + 8).addBox(-2.5f, 3.5f, -4, 1, 1, 2).texOffs(BONE_U, BONE_V + 8).addBox(-0.5f, 3.5f, -4.5f, 1, 1, 2)
            .texOffs(BONE_U, BONE_V + 8).addBox(1.5f, 3.5f, -4, 1, 1, 2), PartPose.offset(0, 4.5f, 0));
      }
      PartDefinition t1 = p.addOrReplaceChild("tail1", spikes(CubeListBuilder.create().texOffs(0, 76).addBox(-4.5f, -4.5f, 0, 9, 9, 10), -4.5f, 2, 8, 3, 1.5f),
          PartPose.offset(0, 11, 11));
      PartDefinition t2 = t1.addOrReplaceChild("tail2", spikes(CubeListBuilder.create().texOffs(40, 76).addBox(-3.5f, -3.5f, 0, 7, 7, 10), -3.5f, 2, 8, 3, 1.5f),
          PartPose.offset(0, 0, 9));
      PartDefinition t3 = t2.addOrReplaceChild("tail3", spikes(CubeListBuilder.create().texOffs(76, 76).addBox(-2.5f, -2.5f, 0, 5, 5, 9), -2.5f, 2, 6, 4, 1f),
          PartPose.offset(0, 0, 9));
      t3.addOrReplaceChild("tail4", CubeListBuilder.create().texOffs(104, 76).addBox(-1.5f, -1.5f, 0, 3, 3, 8)
          .texOffs(BONE_U, BONE_V + 16).addBox(-0.5f, -3.5f, 4, 1, 7, 6), PartPose.offset(0, 0, 8));
      for (int i = 0; i < NECKS; i++) {
        PartDefinition n1 = p.addOrReplaceChild("neck" + i, spikes(CubeListBuilder.create().texOffs(120, 0).addBox(-2.5f, -2.5f, -8, 5, 5, 8), -2.5f, -7, -1, 3, 1.2f),
            PartPose.offset(0, 8, -10));
        PartDefinition n2 = n1.addOrReplaceChild("neck2", spikes(CubeListBuilder.create().texOffs(120, 16).addBox(-2, -2, -8, 4, 4, 8), -2, -7, -1, 3, 1f),
            PartPose.offset(0, 0, -7.5f));
        PartDefinition n3 = n2.addOrReplaceChild("neck3", CubeListBuilder.create().texOffs(120, 30).addBox(-1.75f, -1.75f, -7, 3.5f, 3.5f, 7),
            PartPose.offset(0, 0, -7.5f));
        CubeListBuilder skull = CubeListBuilder.create()
            .texOffs(160, 0).addBox(-3.5f, -3, -7, 7, 6, 7)          // cranio
            .texOffs(160, 16).addBox(-2.5f, -1.5f, -12, 5, 3, 5)     // focinho
            .texOffs(BONE_U, BONE_V + 32).addBox(-3.5f, -6, -2, 1, 4, 2) // chifres
            .texOffs(BONE_U, BONE_V + 32).addBox(2.5f, -6, -2, 1, 4, 2)
            .texOffs(BONE_U, BONE_V + 32).addBox(-0.5f, -5, -4, 1, 2, 3) // crista
            .texOffs(EYE_U, EYE_V).addBox(-3.7f, -1.8f, -6, 1, 1, 2)     // olhos
            .texOffs(EYE_U, EYE_V).addBox(2.7f, -1.8f, -6, 1, 1, 2);
        for (int k = 0; k < 4; k++) skull.texOffs(BONE_U + 8, BONE_V).addBox(-2 + k * 1.2f, 1.5f, -11.5f, 0.8f, 1.2f, 0.8f); // dentes de cima
        PartDefinition hd = n3.addOrReplaceChild("head", skull, PartPose.offset(0, 0, -6.5f));
        CubeListBuilder jawCubes = CubeListBuilder.create().texOffs(160, 26).addBox(-2.5f, 0, -11, 5, 1.5f, 11);
        for (int k = 0; k < 4; k++) jawCubes.texOffs(BONE_U + 8, BONE_V).addBox(-1.8f + k * 1.2f, -1, -10.5f, 0.8f, 1.2f, 0.8f); // dentes de baixo
        hd.addOrReplaceChild("jaw", jawCubes, PartPose.offset(0, 1.5f, 0));
      }
      return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(HydraEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
      root.getAllParts().forEach(ModelPart::resetPose);
      float[] c = tint(e.stage());
      r = Mth.lerp(0.35f, 1f, c[0]); g = Mth.lerp(0.35f, 1f, c[1]); b = Mth.lerp(0.35f, 1f, c[2]);
      boolean asleep = e.sealed();
      int power = e.power();
      // respiracao: o peito incha devagar (mais devagar dormindo)
      float breathe = 1f + Mth.sin(age * (asleep ? 0.04f : 0.09f)) * 0.025f;
      body.yScale = breathe;
      body.xScale = 1f + (breathe - 1f) * 0.6f;
      // patas: passada de reptil (diagonais juntas) com o joelho dobrando na volta
      float swing = limbSwing * 0.55f;
      for (int i = 0; i < 4; i++) {
        float phase = (i == 0 || i == 3) ? 0f : Mth.PI;
        float w = Mth.cos(swing + phase) * 0.8f * limbSwingAmount;
        thigh[i].xRot = w;
        shin[i].xRot = -Math.max(0f, Mth.sin(swing + phase)) * 0.9f * limbSwingAmount;
      }
      body.zRot = Mth.sin(swing) * 0.04f * limbSwingAmount;
      // cauda: serpenteia em onda; no golpe de cauda varre de um lado ao outro
      float tailAmp = power == HydraEntity.SWEEP ? 1.0f : asleep ? 0.08f : 0.25f;
      float tailSpeed = power == HydraEntity.SWEEP ? 0.45f : 0.07f;
      tail1.xRot = -0.25f + Mth.sin(age * 0.05f) * 0.05f;
      tail1.yRot = Mth.sin(age * tailSpeed) * tailAmp;
      tail2.yRot = Mth.sin(age * tailSpeed - 0.8f) * tailAmp * 1.1f;
      tail3.yRot = Mth.sin(age * tailSpeed - 1.6f) * tailAmp * 1.2f;
      tail4.yRot = Mth.sin(age * tailSpeed - 2.4f) * tailAmp * 1.3f;
      tail2.xRot = 0.12f; tail3.xRot = 0.1f; tail4.xRot = 0.08f;
      if (power == HydraEntity.SWEEP) body.yRot = Mth.sin(age * tailSpeed) * 0.15f;
      int n = e.heads();
      float lunge = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0f;
      boolean roar = power == HydraEntity.ROAR, breath = power == HydraEntity.BREATH;
      boolean skyward = power == HydraEntity.STORM || power == HydraEntity.METEOR || power == HydraEntity.CATACLYSM
          || power == HydraEntity.DEVASTATE || power == HydraEntity.DEVOURED_SPELL;
      for (int i = 0; i < NECKS; i++) {
        boolean show = i < n;
        neck[i].visible = show;
        if (!show) continue;
        // leque bem aberto na frente do corpo; com muitas cabecas, duas fileiras (as pares mais altas e mais atras)
        float spread = n == 1 ? 0f : (i / (float) (n - 1)) * 2f - 1f;
        float row = n > 4 ? (i % 2) : 0;
        neck[i].x = spread * 8f;
        neck[i].y = 8f - row * 3f;
        neck[i].z = -12f + row * 3f;
        neck[i].yRot = spread * (n > 3 ? 1.05f : 0.6f) + netHeadYaw * Mth.DEG_TO_RAD * 0.3f;
        neck[i].zRot = -spread * 0.35f;
        float wave = age * 0.09f + i * 1.3f;
        if (asleep) {
          // dormindo: pescocos caidos ate o chao, cabecas apoiadas, so a respiracao mexe
          neck[i].xRot = 0.35f + Mth.sin(age * 0.04f + i) * 0.03f;
          neck2[i].xRot = 0.35f;
          neck3[i].xRot = -0.1f;
          head[i].xRot = -0.3f;
          jaw[i].xRot = 0f;
          continue;
        }
        float baseX = -0.75f - row * 0.35f - (1f - Math.abs(spread)) * 0.2f;
        // onda que corre do ombro ate a cabeca
        neck[i].xRot = baseX + Mth.sin(wave) * 0.1f;
        neck2[i].xRot = 0.45f + Mth.sin(wave - 0.9f) * 0.12f;
        neck3[i].xRot = 0.3f + Mth.sin(wave - 1.8f) * 0.12f;
        neck2[i].yRot = Mth.sin(age * 0.06f + i * 2.1f) * 0.15f;
        neck3[i].yRot = Mth.sin(age * 0.06f + i * 2.1f - 0.7f) * 0.12f;
        head[i].xRot = 0.3f + headPitch * Mth.DEG_TO_RAD * 0.5f;
        float jawOpen = Math.max(0f, Mth.sin(age * 0.05f + i * 3f)) * 0.12f;
        if (roar) {
          // rugido: pescocos erguidos, bocas escancaradas, cabecas tremendo
          neck[i].xRot = baseX - 0.45f;
          head[i].xRot = -0.2f;
          head[i].yRot = Mth.sin(age * 2.2f + i) * 0.12f;
          jawOpen = 0.95f;
        } else if (breath) {
          // sopro: pescocos esticados para a frente, boca aberta, tremor leve
          neck[i].xRot = baseX + 0.4f;
          neck2[i].xRot = 0.15f;
          neck3[i].xRot = 0.1f;
          head[i].xRot = 0.05f + Mth.sin(age * 1.5f + i) * 0.04f;
          jawOpen = 0.75f;
        } else if (skyward) {
          // conjurando: cabecas para o ceu
          neck[i].xRot = baseX - 0.6f;
          neck2[i].xRot = -0.1f;
          neck3[i].xRot = -0.15f;
          head[i].xRot = -0.6f;
          jawOpen = 0.55f + Mth.sin(age * 0.8f + i) * 0.1f;
        }
        // mordida: a cabeca do meio (e as vizinhas, menos) da o bote
        if (lunge > 0f) {
          float near = 1f - Math.min(1f, Math.abs(spread) * 1.5f);
          neck[i].xRot += lunge * 0.7f * near;
          neck2[i].xRot -= lunge * 0.4f * near;
          jawOpen = Math.max(jawOpen, lunge * 0.8f * near);
        }
        jaw[i].xRot = jawOpen;
      }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
      root.render(pose, consumer, light, overlay, red * r, green * g, blue * b, alpha);
    }
  }
}
