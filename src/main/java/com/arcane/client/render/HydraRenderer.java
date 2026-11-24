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
   * Modelo organico (para o estilo do Minecraft): em vez de poucas caixas grandes, muitas caixas que afinam em
   * sequencia. Pescocos de 6 gomos, cada um um pouco mais fino e girado em relacao ao anterior, o que da a curva de
   * serpente; cabeca com sobrancelha saliente, bochechas, narinas, chifres para tras, olhos fundos que brilham e duas
   * fileiras de dentes; corpo com ombros, placas no peito e escamas no dorso; patas de tres partes com dedos e garras;
   * cauda de 6 gomos que termina numa lamina.
   *
   * <p>Animacao procedural: cada cabeca mira o SEU alvo (vindo do servidor) e o pescoco se dobra ate la como uma
   * corrente, cada gomo seguindo o anterior com atraso suave; respiracao, pequenos tremores e olhadas aleatorias nas
   * cabecas sem alvo; bote individual quando a cabeca morde; rugido, sopro, golpe de cauda, conjurar e dormir.
   */
  public static final class HydraModel extends HierarchicalModel<HydraEntity> {
    public static final int NECKS = 9, SEG = 6, TAIL = 6;
    private static final int BONE_U = 224, BONE_V = 0, EYE_U = 224, EYE_V = 96;
    private final ModelPart root, body, chest;
    private final ModelPart[] tail = new ModelPart[TAIL];
    private final ModelPart[] thigh = new ModelPart[4], shin = new ModelPart[4], foot = new ModelPart[4];
    private final ModelPart[][] neck = new ModelPart[NECKS][SEG];
    private final ModelPart[] head = new ModelPart[NECKS], jaw = new ModelPart[NECKS];
    private float r = 1, g = 1, b = 1;

    /** Estado suavizado de cada cabeca (por entidade): yaw e pitch atuais da mira. */
    private static final java.util.Map<Integer, float[][]> AIM = new java.util.HashMap<>();

    public HydraModel(ModelPart root) {
      this.root = root;
      this.body = root.getChild("body");
      this.chest = body.getChild("chest");
      for (int i = 0; i < 4; i++) {
        thigh[i] = root.getChild("leg" + i);
        shin[i] = thigh[i].getChild("shin");
        foot[i] = shin[i].getChild("foot");
      }
      ModelPart t = root.getChild("tail0");
      tail[0] = t;
      for (int k = 1; k < TAIL; k++) { t = t.getChild("tail" + k); tail[k] = t; }
      for (int i = 0; i < NECKS; i++) {
        ModelPart n = root.getChild("neck" + i);
        neck[i][0] = n;
        for (int k = 1; k < SEG; k++) { n = n.getChild("seg" + k); neck[i][k] = n; }
        head[i] = n.getChild("head");
        jaw[i] = head[i].getChild("jaw");
      }
    }

    public static LayerDefinition createLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition p = mesh.getRoot();
      // corpo: tronco, ombros, quadril, escamas do dorso
      CubeListBuilder bodyCubes = CubeListBuilder.create()
          .texOffs(0, 0).addBox(-8, -6, -11, 16, 12, 22)
          .texOffs(0, 34).addBox(-9.5f, -5, -12, 19, 9, 7)      // ombros
          .texOffs(52, 34).addBox(-8.5f, -4.5f, 6, 17, 9, 6)     // quadril
          .texOffs(0, 50).addBox(-6, 5.5f, -9, 12, 2, 18);       // ventre
      for (int k = 0; k < 6; k++) {
        float z = -9 + k * 3.4f, sz = 2.6f - Math.abs(k - 2.5f) * 0.3f;
        bodyCubes.texOffs(BONE_U, BONE_V).addBox(-sz / 2, -6 - sz * 1.4f, z, sz, sz * 1.4f, sz * 0.9f);
      }
      PartDefinition bd = p.addOrReplaceChild("body", bodyCubes, PartPose.offset(0, 12, 0));
      bd.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(100, 34).addBox(-6, -3, -3, 12, 8, 4)
          .texOffs(100, 46).addBox(-4.5f, 0, -4, 9, 5, 2), PartPose.offset(0, 1, -11));
      // patas: coxa grossa, canela, pe com tres dedos e garras de osso
      int[][] legPos = {{-8, -7}, {8, -7}, {-8, 7}, {8, 7}};
      for (int i = 0; i < 4; i++) {
        PartDefinition th = p.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 70).addBox(-3.5f, -2, -4, 7, 8, 8),
            PartPose.offset(legPos[i][0], 14, legPos[i][1]));
        PartDefinition sh = th.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(32, 70).addBox(-2.5f, 0, -2.5f, 5, 5, 5),
            PartPose.offset(0, 5.5f, 0.5f));
        CubeListBuilder ft = CubeListBuilder.create().texOffs(52, 70).addBox(-3, 0, -4, 6, 2, 6);
        for (int k = -1; k <= 1; k++) ft.texOffs(BONE_U, BONE_V + 8).addBox(k * 2 - 0.5f, 0.5f, -5.5f, 1, 1.5f, 2);
        sh.addOrReplaceChild("foot", ft, PartPose.offset(0, 4.5f, 0));
      }
      // cauda: 6 gomos que afinam, espinhos alternados, lamina na ponta
      PartDefinition parent = p;
      String name = "tail0";
      float tw = 9f, tz = 10f;
      for (int k = 0; k < TAIL; k++) {
        CubeListBuilder seg = CubeListBuilder.create().texOffs(0, 90 + (k % 2) * 18).addBox(-tw / 2, -tw / 2, 0, tw, tw, tz);
        if (k % 2 == 0) seg.texOffs(BONE_U, BONE_V).addBox(-0.6f, -tw / 2 - 2, tz * 0.3f, 1.2f, 2, 2);
        if (k == TAIL - 1) seg.texOffs(BONE_U, BONE_V + 16).addBox(-0.5f, -4, tz - 1, 1, 8, 7);
        parent = parent.addOrReplaceChild(name, seg, k == 0 ? PartPose.offset(0, 10, 10) : PartPose.offset(0, 0, tz - 1.5f * 0.5f - 0.5f));
        tz = Math.max(5f, tz - 0.8f);
        tw = Math.max(2.5f, tw * 0.8f);
        name = "tail" + (k + 1);
      }
      // pescocos: 6 gomos que afinam; cabeca detalhada
      for (int i = 0; i < NECKS; i++) {
        float w = 5.5f;
        PartDefinition seg = null;
        for (int k = 0; k < SEG; k++) {
          float len = 5.2f - k * 0.25f;
          CubeListBuilder cube = CubeListBuilder.create().texOffs(120, (k % 3) * 14).addBox(-w / 2, -w / 2, -len, w, w, len);
          if (k % 2 == 1) cube.texOffs(BONE_U, BONE_V).addBox(-0.5f, -w / 2 - 1.4f, -len * 0.7f, 1, 1.6f, 1.6f);
          String segName = k == 0 ? "neck" + i : "seg" + k;
          PartPose pose = k == 0 ? PartPose.offset(0, 8, -11) : PartPose.offset(0, 0, -(5.2f - (k - 1) * 0.25f) + 0.4f);
          seg = k == 0 ? p.addOrReplaceChild(segName, cube, pose) : seg.addOrReplaceChild(segName, cube, pose);
          w = Math.max(3.2f, w - 0.42f);
        }
        CubeListBuilder skull = CubeListBuilder.create()
            .texOffs(160, 0).addBox(-3.5f, -3, -6, 7, 5.5f, 6)            // cranio
            .texOffs(160, 14).addBox(-4.2f, -3.6f, -5.5f, 8.4f, 1.4f, 3)   // sobrancelha saliente
            .texOffs(160, 20).addBox(-4, -1, -4.5f, 1.2f, 3, 4)            // bochechas
            .texOffs(160, 20).addBox(2.8f, -1, -4.5f, 1.2f, 3, 4)
            .texOffs(186, 0).addBox(-2.5f, -2, -11, 5, 3, 5.5f)            // focinho
            .texOffs(186, 10).addBox(-2, -2.6f, -11, 4, 0.8f, 3)           // narinas
            .texOffs(BONE_U, BONE_V + 32).addBox(-3.6f, -4.8f, -1.5f, 1, 1.2f, 5)  // chifres para tras
            .texOffs(BONE_U, BONE_V + 32).addBox(2.6f, -4.8f, -1.5f, 1, 1.2f, 5)
            .texOffs(BONE_U, BONE_V + 32).addBox(-3.6f, -5.4f, 3, 1, 1, 2.5f)
            .texOffs(BONE_U, BONE_V + 32).addBox(2.6f, -5.4f, 3, 1, 1, 2.5f)
            .texOffs(EYE_U, EYE_V).addBox(-3.55f, -2.4f, -5.2f, 0.6f, 0.8f, 1.6f)  // olhos fundos
            .texOffs(EYE_U, EYE_V).addBox(2.95f, -2.4f, -5.2f, 0.6f, 0.8f, 1.6f);
        for (int k = 0; k < 5; k++) skull.texOffs(BONE_U + 8, BONE_V).addBox(-2.2f + k * 1.05f, 0.9f, -10.6f + (k % 2) * 0.4f, 0.6f, 1.3f, 0.6f);
        PartDefinition hd = seg.addOrReplaceChild("head", skull, PartPose.offset(0, 0, -3.6f));
        CubeListBuilder jawCubes = CubeListBuilder.create().texOffs(186, 18).addBox(-2.3f, 0, -10, 4.6f, 1.6f, 9.5f)
            .texOffs(186, 30).addBox(-3, 0.2f, -1.5f, 6, 1.8f, 2.5f);
        for (int k = 0; k < 5; k++) jawCubes.texOffs(BONE_U + 8, BONE_V).addBox(-2 + k * 1.0f, -1.1f, -9.6f + (k % 2) * 0.4f, 0.6f, 1.2f, 0.6f);
        hd.addOrReplaceChild("jaw", jawCubes, PartPose.offset(0, 1.6f, 0));
      }
      return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public ModelPart root() { return root; }

    /** Angulo (radianos) suavizado em direcao a {@code to}, com volta de angulo. */
    private static float approach(float from, float to, float k) {
      float d = Mth.wrapDegrees((to - from) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
      return from + d * k;
    }

    @Override
    public void setupAnim(HydraEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
      root.getAllParts().forEach(ModelPart::resetPose);
      float[] c = tint(e.stage());
      r = Mth.lerp(0.35f, 1f, c[0]); g = Mth.lerp(0.35f, 1f, c[1]); b = Mth.lerp(0.35f, 1f, c[2]);
      boolean asleep = e.sealed();
      int power = e.power();
      // respiracao: peito e ombros incham devagar
      float breathe = Mth.sin(age * (asleep ? 0.04f : 0.09f));
      body.yScale = 1f + breathe * 0.025f;
      body.xScale = 1f + breathe * 0.015f;
      chest.z += breathe * 0.3f;
      // patas: passada diagonal; joelho dobra na volta e o pe se ajusta ao chao
      float swing = limbSwing * 0.5f;
      for (int i = 0; i < 4; i++) {
        float phase = (i == 0 || i == 3) ? 0f : Mth.PI;
        float w = Mth.cos(swing + phase) * 0.7f * limbSwingAmount;
        float lift = Math.max(0f, Mth.sin(swing + phase)) * limbSwingAmount;
        thigh[i].xRot = w;
        shin[i].xRot = -lift * 0.9f;
        foot[i].xRot = -w + lift * 0.6f;
      }
      body.zRot = Mth.sin(swing) * 0.05f * limbSwingAmount;
      body.y += Math.abs(Mth.cos(swing)) * limbSwingAmount * 0.6f;
      // cauda em onda (atraso por gomo); no golpe de cauda varre forte
      boolean sweep = power == HydraEntity.SWEEP;
      float amp = sweep ? 0.55f : asleep ? 0.05f : 0.16f, speed = sweep ? 0.45f : 0.07f;
      for (int k = 0; k < TAIL; k++) {
        tail[k].yRot = Mth.sin(age * speed - k * 0.7f) * amp * (1f + k * 0.15f);
        tail[k].xRot = k == 0 ? -0.3f + Mth.sin(age * 0.05f) * 0.04f : 0.06f + Mth.sin(age * 0.06f - k) * 0.02f;
      }
      if (sweep) body.yRot = Mth.sin(age * speed) * 0.15f;

      int n = e.heads();
      boolean roar = power == HydraEntity.ROAR, breath = power == HydraEntity.BREATH;
      boolean skyward = power == HydraEntity.STORM || power == HydraEntity.METEOR || power == HydraEntity.CATACLYSM
          || power == HydraEntity.DEVASTATE || power == HydraEntity.DEVOURED_SPELL;
      float[][] aim = AIM.computeIfAbsent(e.getId(), id -> new float[NECKS][2]);
      if (AIM.size() > 32) AIM.keySet().removeIf(id -> e.level().getEntity(id) == null);
      var level = e.level();
      for (int i = 0; i < NECKS; i++) {
        boolean show = i < n;
        neck[i][0].visible = show;
        if (!show) continue;
        // posicao de base: leque bem aberto na frente, duas fileiras quando ha muitas cabecas
        float spread = n == 1 ? 0f : (i / (float) (n - 1)) * 2f - 1f;
        float row = n > 4 ? (i % 2) : 0;
        neck[i][0].x = spread * (n > 4 ? 10.5f : 8.5f);
        neck[i][0].y = 8f - row * 3f;
        neck[i][0].z = -11f + row * 3f;
        float baseYaw = spread * (n > 3 ? 0.95f : 0.55f);
        float basePitch = -0.95f - row * 0.3f - (1f - Math.abs(spread)) * 0.15f;
        if (asleep) {
          // dormindo: pescocos estirados no chao, so a respiracao
          neck[i][0].yRot = baseYaw * 0.8f;
          neck[i][0].xRot = 0.5f;
          for (int k = 1; k < SEG; k++) { neck[i][k].xRot = 0.08f + Mth.sin(age * 0.04f + i + k) * 0.01f; neck[i][k].yRot = Mth.sin(i * 1.7f + k) * 0.1f; }
          head[i].xRot = -0.35f;
          jaw[i].xRot = 0f;
          continue;
        }
        // mira desta cabeca: o alvo vindo do servidor; sem alvo, olhadas lentas e aleatorias
        float wantYaw = baseYaw + Mth.sin(age * 0.013f + i * 2.3f) * 0.15f;
        float wantPitch = 0f;
        var target = level.getEntity(e.headTarget(i));
        if (target != null) {
          double dx = target.getX() - e.getX(), dz = target.getZ() - e.getZ();
          double dy = target.getEyeY() - (e.getY() + e.getBbHeight() * 0.8);
          float worldYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
          float rel = Mth.wrapDegrees(worldYaw - e.yBodyRot) * Mth.DEG_TO_RAD;
          // cada cabeca so mira dentro do seu setor do leque: os pescocos nao se cruzam nem se atravessam
          float sector = n == 1 ? 1.4f : Math.max(0.3f, 1.1f / (n - 1) * 1.5f);
          wantYaw = Mth.clamp(rel, baseYaw - sector, baseYaw + sector);
          wantPitch = (float) -Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        }
        if (roar || skyward) { wantPitch = -0.9f; wantYaw = baseYaw * 0.6f; }
        aim[i][0] = approach(aim[i][0], wantYaw, 0.08f);
        aim[i][1] += (wantPitch - aim[i][1]) * 0.08f;
        // a mira se espalha pelo pescoco: o gomo da base gira mais, os outros seguem com atraso (corrente)
        float wave = age * 0.09f + i * 1.3f;
        float bite = e.headBiting(i) ? 1f : 0f;
        neck[i][0].yRot = aim[i][0] * 0.45f;
        neck[i][0].zRot = -spread * 0.3f;
        neck[i][0].xRot = basePitch + aim[i][1] * 0.3f + Mth.sin(wave) * 0.06f - bite * -0.5f;
        for (int k = 1; k < SEG; k++) {
          float lag = Mth.sin(wave - k * 0.55f);
          neck[i][k].yRot = aim[i][0] * 0.11f + lag * 0.03f;
          // o pescoco sobe em S: os primeiros gomos erguem, os ultimos trazem a cabeca para a frente
          neck[i][k].xRot = (k < 3 ? 0.06f : 0.24f) + aim[i][1] * 0.09f + lag * 0.035f - bite * (k < 3 ? 0.05f : -0.12f);
          if (breath) neck[i][k].xRot = 0.2f;
        }
        // tremor de predador: a cabeca da pequenos trancos
        float twitch = Mth.sin(age * 0.7f + i * 5f) > 0.97f ? 0.12f : 0f;
        head[i].xRot = 0.15f + aim[i][1] * 0.25f + twitch;
        head[i].zRot = Mth.sin(age * 0.05f + i) * 0.08f;
        float jawOpen = Math.max(0f, Mth.sin(age * 0.05f + i * 3f)) * 0.1f;
        if (roar) { jawOpen = 0.95f; head[i].yRot = Mth.sin(age * 2.2f + i) * 0.12f; }
        else if (breath) { jawOpen = 0.75f; head[i].xRot = 0.05f + Mth.sin(age * 1.5f + i) * 0.04f; }
        else if (skyward) jawOpen = 0.55f + Mth.sin(age * 0.8f + i) * 0.1f;
        if (bite > 0) jawOpen = 0.85f;
        jaw[i].xRot = jawOpen;
      }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
      root.render(pose, consumer, light, overlay, red * r, green * g, blue * b, alpha);
    }
  }
}
