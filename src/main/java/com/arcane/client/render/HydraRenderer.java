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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
    addLayer(new Veins(this));
  }

  @Override
  public ResourceLocation getTextureLocation(HydraEntity entity) { return TEXTURE; }

  /**
   * Selada: correntes de luz carmesim da Hidra ate cada selo inteiro nos pilares, e aneis de runas apertando o corpo.
   * Desperta a partir do estagio 5: uma aura de brasas girando em volta (sinal de que ela devasta a regiao).
   */
  /** Altura suavizada de cada Hidra (cliente): o corpo sobe e desce devagar quando ela pisa num degrau grande. */
  private static final java.util.Map<Integer, double[]> SMOOTH_Y = new java.util.HashMap<>();

  @Override
  public void render(HydraEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
    double ey = net.minecraft.util.Mth.lerp(partial, e.yo, e.getY());
    double[] sy = SMOOTH_Y.computeIfAbsent(e.getId(), id -> new double[] {ey, net.minecraft.Util.getMillis()});
    if (SMOOTH_Y.size() > 32) SMOOTH_Y.keySet().removeIf(id -> e.level().getEntity(id) == null);
    long now = net.minecraft.Util.getMillis();
    double dt = Math.min(0.1, (now - sy[1]) / 1000.0);
    sy[1] = now;
    if (Math.abs(ey - sy[0]) > 12) sy[0] = ey; // teleporte: nao suaviza
    sy[0] += (ey - sy[0]) * (1 - Math.exp(-dt / 0.35));
    pose.pushPose();
    pose.translate(0, sy[0] - ey, 0);
    super.render(e, yaw, partial, pose, buffers, light);
    pose.popPose();
    float age = e.tickCount + partial;
    VertexConsumer c = buffers.getBuffer(RenderType.lightning());
    PoseStack.Pose last = pose.last();
    net.minecraft.world.phys.Vec3 body = new net.minecraft.world.phys.Vec3(0, e.getBbHeight() * 0.5, 0);
    float w = e.getBbWidth();
    if (e.sealed()) {
      net.minecraft.world.phys.Vec3 me = e.getPosition(partial);
      for (int i = 0; i < HydraEntity.PILLARS; i++) {
        if ((e.sealMask() & (1 << i)) == 0) continue;
        var top = e.home().offset(HydraEntity.pillarTop(i, e.stage()));
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

  /** Nao tomba de lado ao morrer: ela se desfaz em runas de pe (o servidor cuida das particulas). */
  @Override
  protected float getFlipDegrees(HydraEntity entity) { return 0f; }

  @Override
  protected void scale(HydraEntity entity, PoseStack pose, float partialTick) {
    float s = HydraEntity.scaleFor(entity.stage());
    if (entity.deathTime > 0) {
      float f = Math.min(1f, (entity.deathTime + partialTick) / HydraEntity.DEATH_TICKS);
      s *= 1f - f * f * 0.85f; // desfaz devagar, depois cada vez mais rapido
    }
    pose.scale(s, s, s);
    this.shadowRadius = 1.2f * s;
  }


  /** Cor de cada glifo original (0..6, ordem de HydraLairs.ORIGINALS): tinge de leve o couro e da a cor das veias. */
  static final int[] GLYPH_COLORS = {0xFFD25A, 0x7FE8FF, 0xB070FF, 0xFF5A30, 0x9030C0, 0x60E070, 0xB8D8FF};
  static final int DEFAULT_VEINS = 0x40E8FF;

  static int veinColor(HydraEntity e) {
    if (e.flag(HydraEntity.F_BERSERK)) return 0xFF2A2A;
    int v = e.variant();
    return v >= 0 && v < GLYPH_COLORS.length ? GLYPH_COLORS[v] : DEFAULT_VEINS;
  }

  /** Veias e olhos que pulsam com a respiracao, acendem antes dos poderes, piscam na fraqueza e ficam vermelhos na furia. */
  private static final class Veins extends net.minecraft.client.renderer.entity.layers.RenderLayer<HydraEntity, HydraModel> {
    Veins(RenderLayerParent<HydraEntity, HydraModel> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, HydraEntity e, float limb, float limbAmount, float partial, float age, float yaw, float pitch) {
      float pulse = 0.55f + 0.45f * Mth.sin(age * (e.sealed() ? 0.04f : 0.12f));
      if (e.power() != 0) pulse = 1f;
      if (e.flag(HydraEntity.F_WEAK)) pulse = (Mth.sin(age * 1.7f) > 0.3f) ? 0.15f : 0.5f;
      if (e.flag(HydraEntity.F_DYING)) pulse = 1f;
      int c = veinColor(e);
      float r = ((c >> 16) & 255) / 255f * pulse, g = ((c >> 8) & 255) / 255f * pulse, b = (c & 255) / 255f * pulse;
      getParentModel().root().render(pose, buffers.getBuffer(RenderType.eyes(EYES)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, 1f);
    }
  }

  /** Cor do couro por estagio (1 a 14); aplicada de leve sobre a obsidiana. */
  static float[] tint(int stage) {
    float t = (Math.max(1, Math.min(14, stage)) - 1) / 13f;
    float[] a = {0.62f, 1.0f, 0.58f}, b = {0.42f, 0.72f, 1.0f}, c = {1.0f, 0.5f, 0.32f};
    float[] out = new float[3];
    for (int i = 0; i < 3; i++) out[i] = t < 0.5f ? Mth.lerp(t * 2, a[i], b[i]) : Mth.lerp((t - 0.5f) * 2, b[i], c[i]);
    return out;
  }

  /** Tipo de cada cabeca: a maioria comum; algumas com chifres grandes, crista ou cegas (placa de osso, mais dentes). */
  static int headKind(HydraEntity e, int i) {
    int h = Math.floorMod((int) (e.getUUID().getLeastSignificantBits() >>> 8) + i * 7919, 13);
    return h < 9 ? 0 : 1 + (h % 3);
  }

  /**
   * Silhueta de dragao sem asas: corpo alto e largo, peito saliente, patas longas de tres partes com dedos, cauda de
   * 6 gomos com lamina e pescocos longos de 6 gomos. O dorso muda com o glifo (espinhos, barbatanas ou cristais) e
   * algumas cabecas sao diferentes (chifres grandes, crista, cega). Animacao procedural (ver setupAnim).
   */
  public static final class HydraModel extends HierarchicalModel<HydraEntity> {
    public static final int NECKS = 9, SEG = 6, TAIL = 6;
    private static final int BONE_U = 224, BONE_V = 0, EYE_U = 224, EYE_V = 96;
    private final ModelPart root, body, chest, spikes, fins, crystals;
    private final ModelPart[] tail = new ModelPart[TAIL];
    private final ModelPart[] thigh = new ModelPart[4], shin = new ModelPart[4], foot = new ModelPart[4];
    private final ModelPart[][] neck = new ModelPart[NECKS][SEG];
    private final ModelPart[] head = new ModelPart[NECKS], jaw = new ModelPart[NECKS], eyes = new ModelPart[NECKS],
        bigHorns = new ModelPart[NECKS], crest = new ModelPart[NECKS], blind = new ModelPart[NECKS];
    private float r = 1, g = 1, b = 1;

    /** Estado suavizado de cada cabeca (por entidade): yaw e pitch atuais da mira. */
    private static final java.util.Map<Integer, float[][]> AIM = new java.util.HashMap<>();

    public HydraModel(ModelPart root) {
      this.root = root;
      this.body = root.getChild("body");
      this.chest = body.getChild("chest");
      this.spikes = body.getChild("spikes");
      this.fins = body.getChild("fins");
      this.crystals = body.getChild("crystals");
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
        eyes[i] = head[i].getChild("eyes");
        bigHorns[i] = head[i].getChild("big_horns");
        crest[i] = head[i].getChild("crest");
        blind[i] = head[i].getChild("blind");
      }
    }

    public static LayerDefinition createLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition p = mesh.getRoot();
      // corpo alto de dragao: tronco, ombros largos, quadril, ventre
      PartDefinition bd = p.addOrReplaceChild("body", CubeListBuilder.create()
          .texOffs(0, 0).addBox(-9, -7, -18, 18, 14, 36)            // tronco longo
          .texOffs(108, 0).addBox(-11, -6, -21, 22, 11, 8)          // ombros
          .texOffs(108, 20).addBox(-9.5f, -6, 13, 19, 11, 8)        // quadril
          .texOffs(0, 52).addBox(-7, 6.5f, -16, 14, 2, 32), PartPose.offset(0, 4, 0)); // ventre
      bd.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(104, 38).addBox(-7, -3, -4, 14, 10, 5)
          .texOffs(104, 54).addBox(-5, 1, -5, 10, 6, 2), PartPose.offset(0, 0, -20));
      CubeListBuilder sp = CubeListBuilder.create(), fn = CubeListBuilder.create(), cr = CubeListBuilder.create();
      for (int k = 0; k < 11; k++) {
        float z = -17 + k * 3.4f, sz = 3.2f - Math.abs(k - 5) * 0.25f;
        sp.texOffs(BONE_U, BONE_V).addBox(-sz / 2, -7 - sz * 1.6f, z, sz, sz * 1.6f, sz);
        fn.texOffs(BONE_U, BONE_V + 16).addBox(-0.5f, -7 - sz * 2.2f, z, 1, sz * 2.2f, 3.2f);
        cr.texOffs(EYE_U, EYE_V).addBox(-sz / 2 + (k % 2) * 1.5f - 0.75f, -7 - sz * 1.9f, z, sz * 0.8f, sz * 1.9f, sz * 0.8f);
      }
      bd.addOrReplaceChild("spikes", sp, PartPose.ZERO);
      bd.addOrReplaceChild("fins", fn, PartPose.ZERO);
      bd.addOrReplaceChild("crystals", cr, PartPose.ZERO);
      // patas longas: coxa, canela, pe com tres dedos e garras
      int[][] legPos = {{-10, -13}, {10, -13}, {-10, 13}, {10, 13}};
      for (int i = 0; i < 4; i++) {
        PartDefinition th = p.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 80).addBox(-4, -2, -4.5f, 8, 11, 9),
            PartPose.offset(legPos[i][0], 9, legPos[i][1]));
        PartDefinition sh = th.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(36, 80).addBox(-3, 0, -3, 6, 5, 6),
            PartPose.offset(0, 8.5f, 0.5f));
        CubeListBuilder ft = CubeListBuilder.create().texOffs(60, 80).addBox(-3.5f, -1, -5, 7, 2, 7);
        for (int k = -1; k <= 1; k++) ft.texOffs(BONE_U, BONE_V + 8).addBox(k * 2.3f - 0.5f, -0.5f, -7, 1, 1.5f, 2.5f);
        sh.addOrReplaceChild("foot", ft, PartPose.offset(0, 5.5f, 0));
      }
      // cauda: 6 gomos que afinam, espinhos, lamina na ponta
      PartDefinition parent = p;
      float tw = 10f, tz = 10f;
      for (int k = 0; k < TAIL; k++) {
        CubeListBuilder seg = CubeListBuilder.create().texOffs(0, 100 + (k % 2) * 14).addBox(-tw / 2, -tw / 2, 0, tw, tw, tz);
        seg.texOffs(BONE_U, BONE_V).addBox(-0.6f, -tw / 2 - 2.2f, tz * 0.3f, 1.2f, 2.2f, 2.2f);
        if (k == TAIL - 1) seg.texOffs(BONE_U, BONE_V + 16).addBox(-0.5f, -5, tz - 1, 1, 10, 8);
        parent = parent.addOrReplaceChild("tail" + k, seg, k == 0 ? PartPose.offset(0, 2, 19) : PartPose.offset(0, 0, tz - 1.2f));
        tz = Math.max(5f, tz - 0.9f);
        tw = Math.max(2.5f, tw * 0.78f);
      }
      // pescocos longos e cabecas
      for (int i = 0; i < NECKS; i++) {
        float w = 6f;
        PartDefinition seg = null;
        for (int k = 0; k < SEG; k++) {
          float len = 6.2f - k * 0.3f;
          CubeListBuilder cube = CubeListBuilder.create().texOffs(120, (k % 3) * 15).addBox(-w / 2, -w / 2, -len, w, w, len);
          if (k % 2 == 1) cube.texOffs(BONE_U, BONE_V).addBox(-0.5f, -w / 2 - 1.6f, -len * 0.7f, 1, 1.8f, 1.8f);
          PartPose pose = k == 0 ? PartPose.offset(0, -2, -21) : PartPose.offset(0, 0, -(6.2f - (k - 1) * 0.3f) + 0.5f);
          seg = k == 0 ? p.addOrReplaceChild("neck" + i, cube, pose) : seg.addOrReplaceChild("seg" + k, cube, pose);
          w = Math.max(3.4f, w - 0.45f);
        }
        CubeListBuilder skull = CubeListBuilder.create()
            .texOffs(160, 0).addBox(-3.8f, -3.2f, -6.5f, 7.6f, 6, 6.5f)
            .texOffs(160, 14).addBox(-4.5f, -3.9f, -6, 9, 1.5f, 3.2f)
            .texOffs(160, 20).addBox(-4.3f, -1, -5, 1.3f, 3.2f, 4.5f)
            .texOffs(160, 20).addBox(3, -1, -5, 1.3f, 3.2f, 4.5f)
            .texOffs(188, 0).addBox(-2.7f, -2.2f, -12, 5.4f, 3.2f, 6)
            .texOffs(188, 10).addBox(-2.2f, -2.9f, -12, 4.4f, 0.8f, 3.2f)
            .texOffs(BONE_U, BONE_V + 32).addBox(-3.8f, -5.2f, -1.5f, 1, 1.3f, 5.5f)
            .texOffs(BONE_U, BONE_V + 32).addBox(2.8f, -5.2f, -1.5f, 1, 1.3f, 5.5f);
        for (int k = 0; k < 5; k++) skull.texOffs(BONE_U + 8, BONE_V).addBox(-2.3f + k * 1.1f, 1.0f, -11.6f + (k % 2) * 0.4f, 0.6f, 1.4f, 0.6f);
        PartDefinition hd = seg.addOrReplaceChild("head", skull, PartPose.offset(0, 0, -4.2f));
        hd.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(EYE_U, EYE_V).addBox(-3.85f, -2.6f, -5.6f, 0.6f, 0.9f, 1.8f)
            .texOffs(EYE_U, EYE_V).addBox(3.25f, -2.6f, -5.6f, 0.6f, 0.9f, 1.8f), PartPose.ZERO);
        hd.addOrReplaceChild("big_horns", CubeListBuilder.create().texOffs(BONE_U, BONE_V + 32).addBox(-5.5f, -6.5f, -1, 1.6f, 1.6f, 4)
            .texOffs(BONE_U, BONE_V + 32).addBox(-6.2f, -9, 2, 1.4f, 3.5f, 1.4f)
            .texOffs(BONE_U, BONE_V + 32).addBox(3.9f, -6.5f, -1, 1.6f, 1.6f, 4)
            .texOffs(BONE_U, BONE_V + 32).addBox(4.8f, -9, 2, 1.4f, 3.5f, 1.4f), PartPose.ZERO);
        hd.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(BONE_U, BONE_V + 16).addBox(-0.5f, -7.5f, -6, 1, 4, 9), PartPose.ZERO);
        CubeListBuilder bl = CubeListBuilder.create().texOffs(BONE_U, BONE_V + 24).addBox(-4.1f, -3.4f, -6.2f, 8.2f, 2.2f, 1.2f);
        for (int k = 0; k < 4; k++) bl.texOffs(BONE_U + 8, BONE_V).addBox(-2.6f + k * 1.6f, 1.2f, -9.4f, 0.7f, 1.6f, 0.7f);
        hd.addOrReplaceChild("blind", bl, PartPose.ZERO);
        CubeListBuilder jawCubes = CubeListBuilder.create().texOffs(188, 18).addBox(-2.5f, 0, -11, 5, 1.8f, 10.5f)
            .texOffs(188, 32).addBox(-3.2f, 0.2f, -1.5f, 6.4f, 2, 2.5f);
        for (int k = 0; k < 5; k++) jawCubes.texOffs(BONE_U + 8, BONE_V).addBox(-2.1f + k * 1.05f, -1.2f, -10.6f + (k % 2) * 0.4f, 0.6f, 1.3f, 0.6f);
        hd.addOrReplaceChild("jaw", jawCubes, PartPose.offset(0, 1.8f, 0));
      }
      return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public ModelPart root() { return root; }

    private static float approach(float from, float to, float k) {
      float d = Mth.wrapDegrees((to - from) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
      return from + d * k;
    }

    private static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3 - 2 * t); }

    @Override
    public void setupAnim(HydraEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
      root.getAllParts().forEach(ModelPart::resetPose);
      float[] c = tint(e.stage());
      int v = e.variant();
      float gr = 1, gg = 1, gb = 1;
      if (v >= 0 && v < GLYPH_COLORS.length) { gr = ((GLYPH_COLORS[v] >> 16) & 255) / 255f; gg = ((GLYPH_COLORS[v] >> 8) & 255) / 255f; gb = (GLYPH_COLORS[v] & 255) / 255f; }
      r = Mth.lerp(0.25f, 1f, c[0]) * Mth.lerp(0.3f, 1f, gr);
      g = Mth.lerp(0.25f, 1f, c[1]) * Mth.lerp(0.3f, 1f, gg);
      b = Mth.lerp(0.25f, 1f, c[2]) * Mth.lerp(0.3f, 1f, gb);
      // o dorso muda com o glifo: espinhos (padrao), barbatanas (tempo, retroceder, gravidade) ou cristais (espaco, eclipse)
      int dorsal = v < 0 ? 0 : (v == 0 || v == 1 || v == 6) ? 1 : (v == 2 || v == 4) ? 2 : 0;
      spikes.visible = dorsal == 0;
      fins.visible = dorsal == 1;
      crystals.visible = dorsal == 2;
      boolean asleep = e.sealed();
      boolean strain = asleep && e.flag(HydraEntity.F_STRAIN);
      boolean berserk = e.flag(HydraEntity.F_BERSERK);
      int power = e.power();
      float breathe = Mth.sin(age * (asleep ? 0.04f : 0.09f));
      body.yScale = 1f + breathe * 0.025f;
      body.xScale = 1f + breathe * 0.015f;
      chest.z += breathe * 0.3f;
      // patas: passada diagonal pesada
      float swing = limbSwing * (e.stage() >= 8 ? 0.35f : 0.55f);
      for (int i = 0; i < 4; i++) {
        float phase = (i == 0 || i == 3) ? 0f : Mth.PI;
        float w = Mth.cos(swing + phase) * 0.65f * limbSwingAmount;
        float lift = Math.max(0f, Mth.sin(swing + phase)) * limbSwingAmount;
        thigh[i].xRot = w;
        shin[i].xRot = -lift * 0.9f;
        foot[i].xRot = -w + lift * 0.6f;
      }
      body.zRot = Mth.sin(swing) * 0.05f * limbSwingAmount;
      body.y += Math.abs(Mth.cos(swing)) * limbSwingAmount * 0.8f;
      feetOnGround(e);
      boolean sweep = power == HydraEntity.SWEEP;
      float amp = sweep ? 0.55f : asleep ? 0.05f : 0.16f, speed = sweep ? 0.45f : 0.07f;
      for (int k = 0; k < TAIL; k++) {
        tail[k].yRot = Mth.sin(age * speed - k * 0.7f) * amp * (1f + k * 0.15f);
        tail[k].xRot = k == 0 ? -0.25f + Mth.sin(age * 0.05f) * 0.04f : 0.07f + Mth.sin(age * 0.06f - k) * 0.02f;
      }
      if (sweep) body.yRot = Mth.sin(age * speed) * 0.15f;

      int n = e.heads();
      boolean roar = power == HydraEntity.ROAR, breath = power == HydraEntity.BREATH;
      boolean whole = power == HydraEntity.STORM || power == HydraEntity.METEOR || power == HydraEntity.CATACLYSM || power == HydraEntity.DEVASTATE
          || power == HydraEntity.DEVOURED_SPELL || power == HydraEntity.HEAL || power == HydraEntity.ORIGINAL;
      boolean awakening = e.flag(HydraEntity.F_AWAKEN);
      float sinceAwake = age - e.animStart();
      float[][] aim = AIM.computeIfAbsent(e.getId(), id -> new float[NECKS][2]);
      if (AIM.size() > 32) AIM.keySet().removeIf(id -> e.level().getEntity(id) == null);
      var level = e.level();
      // quantas cabecas mordem o mesmo alvo e a posicao de cada uma no leque em volta dele
      java.util.Map<Integer, Integer> shareCount = new java.util.HashMap<>(), shareRank = new java.util.HashMap<>();
      int[] rank = new int[NECKS];
      for (int i = 0; i < n; i++) {
        if (!e.headAlive(i)) continue;
        int t = e.headTarget(i);
        if (t < 0) continue;
        rank[i] = shareCount.getOrDefault(t, 0);
        shareCount.merge(t, 1, Integer::sum);
      }
      double scale = HydraEntity.scaleFor(e.stage());
      for (int i = 0; i < NECKS; i++) {
        boolean show = i < n;
        neck[i][0].visible = show;
        if (!show) continue;
        int kind = headKind(e, i);
        eyes[i].visible = kind != 3;
        blind[i].visible = kind == 3;
        bigHorns[i].visible = kind == 1;
        crest[i].visible = kind == 2;
        float spread = n == 1 ? 0f : (i / (float) (n - 1)) * 2f - 1f;
        float row = n > 4 ? (i % 2) : 0;
        neck[i][0].x = spread * (n > 4 ? 11.5f : 9f);
        neck[i][0].y = -2f - row * 3f;
        neck[i][0].z = -21f + row * 3f;
        float baseYaw = spread * (n > 3 ? 0.95f : 0.55f);
        float basePitch = -1.05f - row * 0.3f - (1f - Math.abs(spread)) * 0.15f;
        for (int k = 1; k < SEG; k++) neck[i][k].visible = true;
        if (!e.headAlive(i)) {
          // cabeca arrancada: so o toco do pescoco, caido
          neck[i][0].xRot = 0.7f;
          neck[i][0].yRot = baseYaw;
          neck[i][1].visible = false;
          continue;
        }
        if (asleep) {
          // dormindo: pescocos no chao; perto de alguem, as cabecas tentam se erguer e as correntes seguram
          float pull = strain ? Math.abs(Mth.sin(age * 0.9f + i)) : 0f;
          neck[i][0].yRot = baseYaw * 0.8f + (strain ? Mth.sin(age * 2.3f + i) * 0.08f : 0f);
          neck[i][0].xRot = 0.5f - pull * 0.45f;
          for (int k = 1; k < SEG; k++) { neck[i][k].xRot = 0.08f + Mth.sin(age * 0.04f + i + k) * 0.01f - pull * 0.05f; neck[i][k].yRot = Mth.sin(i * 1.7f + k) * 0.1f; }
          head[i].xRot = -0.35f - pull * 0.3f;
          jaw[i].xRot = strain ? pull * 0.5f : 0f;
          continue;
        }
        // mira desta cabeca: o alvo vindo do servidor, deslocado para o lado conforme a posicao no leque
        float wantYaw = baseYaw + Mth.sin(age * 0.013f + i * 2.3f) * 0.15f;
        float wantPitch = 0f;
        var target = level.getEntity(e.headTarget(i));
        if (target != null) {
          int k = shareCount.getOrDefault(target.getId(), 1);
          double dx = target.getX() - e.getX(), dz = target.getZ() - e.getZ();
          double dist = Math.max(1, Math.sqrt(dx * dx + dz * dz));
          double off = (rank[i] - (k - 1) / 2.0) * Math.max(1.2, scale * 0.7); // em arco em volta da presa
          double px = target.getX() + (-dz / dist) * off, pz = target.getZ() + (dx / dist) * off;
          double ddx = px - e.getX(), ddz = pz - e.getZ();
          double dy = target.getEyeY() - (e.getY() + e.getBbHeight() * 0.8);
          float worldYaw = (float) (Mth.atan2(ddz, ddx) * Mth.RAD_TO_DEG) - 90f;
          float rel = Mth.wrapDegrees(worldYaw - e.yBodyRot) * Mth.DEG_TO_RAD;
          float sector = n == 1 ? 1.4f : Math.max(0.3f, 1.1f / (n - 1) * 1.5f);
          wantYaw = Mth.clamp(rel, baseYaw - sector, baseYaw + sector);
          wantPitch = (float) -Mth.atan2(dy, Math.sqrt(ddx * ddx + ddz * ddz));
        }
        boolean sky = (whole && i % 2 == 0) || roar;
        if (sky) { wantPitch = -0.9f; wantYaw = baseYaw * 0.6f; }
        if (berserk) { wantYaw += Mth.sin(age * 1.9f + i * 3) * 0.25f; wantPitch += Mth.sin(age * 2.3f + i) * 0.15f; }
        aim[i][0] = approach(aim[i][0], wantYaw, berserk ? 0.18f : 0.08f);
        aim[i][1] += (wantPitch - aim[i][1]) * 0.08f;
        // despertar: cada cabeca se ergue na sua vez e ruge para o alvo
        float rise = awakening ? smooth((sinceAwake - i * 8f) / 14f) : 1f;
        float wave = age * 0.09f + i * 1.3f;
        float bite = e.headBiting(i) ? 1f : 0f;
        neck[i][0].yRot = Mth.lerp(rise, baseYaw * 0.8f, aim[i][0] * 0.45f);
        neck[i][0].zRot = -spread * 0.3f;
        neck[i][0].xRot = Mth.lerp(rise, 0.5f, basePitch + aim[i][1] * 0.3f + Mth.sin(wave) * 0.08f + bite * 0.5f);
        for (int k = 1; k < SEG; k++) {
          float lag = Mth.sin(wave - k * 0.6f);
          neck[i][k].yRot = Mth.lerp(rise, 0f, aim[i][0] * 0.11f + lag * 0.05f);
          neck[i][k].xRot = Mth.lerp(rise, 0.08f, (k < 3 ? 0.05f : 0.25f) + aim[i][1] * 0.09f + lag * 0.045f - bite * (k < 3 ? 0.05f : -0.12f));
          if (breath && !sky) neck[i][k].xRot = 0.2f;
        }
        float twitch = Mth.sin(age * 0.7f + i * 5f) > 0.97f ? 0.12f : 0f;
        head[i].xRot = 0.15f + aim[i][1] * 0.25f + twitch;
        head[i].zRot = Mth.sin(age * 0.05f + i) * 0.08f;
        float jawOpen = Math.max(0f, Mth.sin(age * 0.05f + i * 3f)) * 0.1f;
        if (awakening && rise > 0.2f) jawOpen = 0.9f * (1f - Math.max(0f, (sinceAwake - i * 8f - 30f) / 20f));
        if (roar) { jawOpen = 0.95f; head[i].yRot = Mth.sin(age * 2.2f + i) * 0.12f; }
        else if (breath && !sky) { jawOpen = 0.75f; head[i].xRot = 0.05f + Mth.sin(age * 1.5f + i) * 0.04f; }
        else if (sky) jawOpen = 0.6f + Mth.sin(age * 0.8f + i) * 0.1f;
        if (bite > 0) jawOpen = 0.9f;
        jaw[i].xRot = Math.max(0f, jawOpen);
      }
      idleGesture(e, age, n);
    }

    /** Pes no chao: cada pata procura o chao embaixo dela; o corpo inclina e as patas esticam ou dobram. */
    private void feetOnGround(HydraEntity e) {
      var level = e.level();
      if (level == null || e.isPassenger()) return;
      double scale = HydraEntity.scaleFor(e.stage());
      double yaw = Math.toRadians(e.yBodyRot);
      double cos = Math.cos(yaw), sin = Math.sin(yaw);
      float[] off = new float[4];
      int[][] legPos = {{-10, -13}, {10, -13}, {-10, 13}, {10, 13}};
      for (int i = 0; i < 4; i++) {
        // posicao do pe no mundo (modelo: -Z e a frente; o renderer gira 180 - yaw)
        double lx = -legPos[i][0] * scale / 16.0, lz = -legPos[i][1] * scale / 16.0;
        double wx = e.getX() + lx * cos - lz * sin, wz = e.getZ() + lx * sin + lz * cos;
        double ground = groundAt(level, wx, e.getY(), wz, Math.max(2, scale * 1.2));
        off[i] = (float) Mth.clamp((ground - e.getY()) * 16.0 / scale, -9, 9); // em pixels do modelo
      }
      for (int i = 0; i < 4; i++) {
        thigh[i].y -= off[i];
        shin[i].xRot += Math.min(0f, off[i]) * 0.04f;
      }
      float front = (off[0] + off[1]) / 2f, back = (off[2] + off[3]) / 2f, left = (off[0] + off[2]) / 2f, right = (off[1] + off[3]) / 2f;
      body.xRot += (float) Math.atan2(front - back, 26) * 0.8f;
      body.zRot += (float) Math.atan2(right - left, 20) * 0.6f;
      body.y -= (front + back) / 2f * 0.5f;
    }

    /** Altura do chao numa coluna (o primeiro bloco solido descendo de cima do corpo). */
    private static double groundAt(net.minecraft.world.level.Level level, double x, double y, double z, double range) {
      var p = new net.minecraft.core.BlockPos.MutableBlockPos();
      int top = (int) Math.floor(y + range), bottom = (int) Math.floor(y - range);
      for (int yy = top; yy >= bottom; yy--) {
        p.set(Math.floor(x), yy, Math.floor(z));
        if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return yy + 1;
      }
      return y;
    }

    /** Gestos ociosos (vindos do servidor): rosnar, rugir, bater a cauda, sacudir, farejar, bocejar e roncar. */
    private void idleGesture(HydraEntity e, float age, int n) {
      int a = e.idleAction();
      if (a == 0) return;
      float t = (age - e.idleStart()) / HydraEntity.IDLE_TICKS;
      if (t < 0 || t > 1) return;
      float env = Mth.sin(t * Mth.PI); // sobe e desce
      int one = Math.floorMod(e.idleStart() * 31, Math.max(1, n));
      switch (a) {
        case HydraEntity.IDLE_GROWL -> {
          for (int i = 0; i < n; i++) {
            neck[i][0].xRot += env * 0.15f;
            jaw[i].xRot = Math.max(jaw[i].xRot, env * 0.25f + Mth.sin(age * 3f + i) * 0.04f * env);
          }
        }
        case HydraEntity.IDLE_ROAR -> {
          neck[one][0].xRot -= env * 0.45f;
          head[one].xRot -= env * 0.35f;
          jaw[one].xRot = Math.max(jaw[one].xRot, env * 0.95f);
        }
        case HydraEntity.IDLE_TAIL -> {
          float lift = t < 0.6f ? smooth(t / 0.6f) : 1f - smooth((t - 0.6f) / 0.15f);
          tail[0].xRot -= lift * 0.6f;
          for (int k = 1; k < TAIL; k++) tail[k].xRot -= lift * 0.12f;
        }
        case HydraEntity.IDLE_SHAKE -> {
          for (int i = 0; i < n; i++) head[i].yRot += Mth.sin(age * 2.6f + i) * 0.35f * env;
        }
        case HydraEntity.IDLE_SNIFF -> {
          for (int i = 0; i < n; i++) {
            neck[i][0].xRot += env * 0.5f;
            head[i].xRot += env * 0.4f + Mth.sin(age * 4f + i) * 0.03f * env;
          }
        }
        case HydraEntity.IDLE_YAWN -> {
          head[one].xRot -= env * 0.4f;
          jaw[one].xRot = Math.max(jaw[one].xRot, env * 1.1f);
        }
        case HydraEntity.IDLE_SNORE -> {
          for (int i = 0; i < n; i++) {
            neck[i][0].xRot -= env * 0.08f;
            jaw[i].xRot = Math.max(jaw[i].xRot, env * 0.2f);
          }
        }
        default -> { }
      }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
      root.render(pose, consumer, light, overlay, red * r, green * g, blue * b, alpha);
    }
  }
}
