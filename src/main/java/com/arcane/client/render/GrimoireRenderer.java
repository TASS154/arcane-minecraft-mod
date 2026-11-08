package com.arcane.client.render;

import com.arcane.entity.GrimoireEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Grimorio flutuante: usa o modelo de livro vanilla com textura propria, girando e folheando. */
public class GrimoireRenderer extends EntityRenderer<GrimoireEntity> {
  private static final ResourceLocation TEXTURE = new ResourceLocation("arcane", "textures/entity/grimoire.png");
  private static final int FRAMES = 8;
  /** [estagio-1][quadro]: o brilho varre a capa e as runas pulsam (tools/gen_art_anim.py). */
  private static final ResourceLocation[][] COVERS = new ResourceLocation[4][FRAMES];

  static {
    for (int s = 0; s < 4; s++) {
      for (int f = 0; f < FRAMES; f++) {
        COVERS[s][f] = new ResourceLocation("arcane", "textures/entity/grimoire_s" + (s + 1) + "_f" + f + ".png");
      }
    }
  }

  /** Estagios 1-4 mudam a capa; do 5 em diante usa a capa mais elaborada e cresce um pouco. */
  private static ResourceLocation cover(int stage, float time) {
    int frame = Math.floorMod((int) (time / 3f), FRAMES);
    return COVERS[Math.min(COVERS.length, Math.max(1, stage)) - 1][frame];
  }
  private final BookModel book;

  public GrimoireRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.book = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    this.shadowRadius = 0.15f;
  }

  @Override
  public void render(GrimoireEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
    float time = entity.tickCount + partialTick;
    pose.pushPose();
    follow(entity, partialTick, pose);
    pose.translate(0.0, 0.4 + Mth.sin(time * 0.08f) * 0.08, 0.0);
    pose.mulPose(Axis.YP.rotationDegrees(-time * 1.5f));
    pose.mulPose(Axis.ZP.rotationDegrees(80.0f));
    int stage = entity.visualStage();
    float size = 0.8f + Math.min(5, Math.max(0, stage - 4)) * 0.05f;
    pose.scale(size, size, size);

    float flipA = Mth.frac(time * 0.015f + 0.25f) * 1.6f - 0.3f;
    float flipB = Mth.frac(time * 0.015f + 0.75f) * 1.6f - 0.3f;
    float open = 0.9f + Mth.sin(time * 0.05f) * 0.08f;
    book.setupAnim(time, Mth.clamp(flipA, 0f, 1f), Mth.clamp(flipB, 0f, 1f), open);

    if (stage < 6) { // do estagio 6 em diante so ficam as paginas e os aneis (o livro "se desfaz")
      VertexConsumer consumer = buffers.getBuffer(book.renderType(cover(stage, time)));
      book.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
    }
    pose.popPose();
    pose.pushPose();
    follow(entity, partialTick, pose);
    renderOrbit(entity, time, pose, buffers, stage);
    pose.popPose();
    super.render(entity, yaw, partialTick, pose, buffers, light);
  }

  /** Estado suavizado de cada livro (posicao no mundo e direcao da visao), atualizado a cada quadro. */
  private static final class Smooth {
    double x, y, z;
    float yaw;
    long lastMs;
    boolean init;
  }

  private static final java.util.Map<Integer, Smooth> SMOOTH = new java.util.HashMap<>();
  /** Constante de tempo do atraso (s): quanto maior, mais o livro "arrasta" atras da visao. */
  private static final double FOLLOW_TAU = 0.22;

  /**
   * Move o livro para onde ele deve ficar em relacao ao dono, com suavizacao exponencial por tempo real (ease in/out):
   * acompanha a posicao e a direcao da visao com um pequeno atraso, sem saltos de um tick para o outro.
   */
  private static void follow(GrimoireEntity entity, float partial, PoseStack pose) {
    var mc = net.minecraft.client.Minecraft.getInstance();
    var ownerId = entity.ownerUuid().orElse(null);
    net.minecraft.world.entity.player.Player owner = ownerId == null || mc.level == null ? null : mc.level.getPlayerByUUID(ownerId);
    if (owner == null) return;
    Smooth s = SMOOTH.computeIfAbsent(entity.getId(), id -> new Smooth());
    if (SMOOTH.size() > 64) SMOOTH.keySet().removeIf(id -> mc.level.getEntity(id) == null);
    double ox = Mth.lerp(partial, owner.xo, owner.getX()), oy = Mth.lerp(partial, owner.yo, owner.getY()), oz = Mth.lerp(partial, owner.zo, owner.getZ());
    float viewYaw = Mth.rotLerp(partial, owner.yHeadRotO, owner.yHeadRot);
    boolean first = owner == mc.player && mc.options.getCameraType().isFirstPerson();
    double eye = owner.getEyeHeight();

    long now = net.minecraft.Util.getMillis();
    double dt = s.init ? Math.min(0.1, (now - s.lastMs) / 1000.0) : 1.0;
    s.lastMs = now;
    double k = 1.0 - Math.exp(-dt / FOLLOW_TAU);
    if (!s.init) { s.x = ox; s.y = oy + eye; s.z = oz; s.yaw = viewYaw; s.init = true; k = 1.0; }
    // direcao suavizada (com volta de angulo)
    s.yaw += (float) (Mth.wrapDegrees(viewYaw - s.yaw) * k);
    double rad = Math.toRadians(s.yaw);
    double fwdX = -Math.sin(rad), fwdZ = Math.cos(rad), rightX = -Math.cos(rad), rightZ = -Math.sin(rad);
    // primeira pessoa: a frente e a direita, perto da mao; terceira: ao lado do ombro
    double side = first ? 0.85 : 1.05, ahead = first ? 0.95 : 0.25, up = first ? eye - 0.25 : eye * 0.9 + 0.1;
    double tx = ox + rightX * side + fwdX * ahead, ty = oy + up, tz = oz + rightZ * side + fwdZ * ahead;
    s.x += (tx - s.x) * k;
    s.y += (ty - s.y) * k;
    s.z += (tz - s.z) * k;
    double ex = Mth.lerp(partial, entity.xo, entity.getX()), ey = Mth.lerp(partial, entity.yo, entity.getY()), ez = Mth.lerp(partial, entity.zo, entity.getZ());
    pose.translate(s.x - ex, s.y - ey - 0.4, s.z - ez);
  }

  private static final ResourceLocation PAGE = new ResourceLocation("arcane", "textures/entity/grimoire_page.png");

  /**
   * Extras 3D do grimorio: paginas soltas em orbita (mais paginas a cada estagio), anel de runas luminoso girando
   * na horizontal e, do estagio 3 em diante, um segundo anel inclinado em volta do livro.
   */
  private void renderOrbit(GrimoireEntity entity, float time, PoseStack pose, MultiBufferSource buffers, int stage) {
    // perto da camera (primeira pessoa) as paginas tampariam a tela: so o livro aparece
    if (this.entityRenderDispatcher.distanceToSqr(entity) < 0.6 * 0.6) return;
    int pages = 2 + stage * 2;
    pose.pushPose();
    pose.translate(0.0, 0.45 + Mth.sin(time * 0.08f) * 0.08, 0.0);
    VertexConsumer pageBuf = buffers.getBuffer(RenderType.entityCutoutNoCull(PAGE));
    for (int i = 0; i < pages; i++) {
      float a = time * 0.035f + i * Mth.TWO_PI / pages;
      float r = 0.75f + 0.08f * Mth.sin(time * 0.1f + i);
      float y = 0.15f * Mth.sin(time * 0.06f + i * 1.7f) + (i % 2 == 0 ? 0.12f : -0.1f);
      pose.pushPose();
      pose.translate(Mth.cos(a) * r, y, Mth.sin(a) * r);
      pose.mulPose(Axis.YP.rotationDegrees(-a * 57.2958f + 90f));
      pose.mulPose(Axis.XP.rotationDegrees(20f * Mth.sin(time * 0.07f + i)));
      pose.scale(0.34f, 0.34f, 0.34f);
      quad(pageBuf, pose, LightTexture.FULL_BRIGHT, 255, 255, 255, 255, true);
      pose.popPose();
    }
    VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
    ring(glow, pose, time * 0.9f, 0f, 0.95f, 12, 0.07f, 150, 120, 255, 170);
    if (stage >= 3) {
      pose.pushPose();
      pose.mulPose(Axis.ZP.rotationDegrees(65f));
      ring(glow, pose, -time * 1.3f, 0f, 0.85f, 10, 0.06f, 120, 230, 255, 150);
      pose.popPose();
    }
    if (stage >= 7) { // estagio final: terceiro anel cruzado e um nucleo de luz
      pose.pushPose();
      pose.mulPose(Axis.XP.rotationDegrees(65f));
      ring(glow, pose, time * 1.7f, 0f, 0.95f, 14, 0.06f, 255, 230, 140, 150);
      pose.popPose();
      ring(glow, pose, time * 2.2f, 0f, 0.25f, 6, 0.09f, 255, 255, 255, 200);
    }
    pose.popPose();
  }

  /** Anel de losangos luminosos no plano XZ. */
  private static void ring(VertexConsumer c, PoseStack pose, float spinDeg, float y, float radius, int count, float size,
                           int r, int g, int b, int a) {
    Matrix4f m = pose.last().pose();
    float spin = spinDeg * 0.017453f;
    for (int i = 0; i < count; i++) {
      float ang = spin + i * Mth.TWO_PI / count;
      float cx = Mth.cos(ang) * radius, cz = Mth.sin(ang) * radius;
      float tx = -Mth.sin(ang) * size, tz = Mth.cos(ang) * size; // tangente
      float rx = Mth.cos(ang) * size, rz = Mth.sin(ang) * size;  // radial
      float pulse = 0.6f + 0.4f * Mth.sin(spinDeg * 0.05f + i);
      int al = (int) (a * pulse);
      // losango deitado (visto de cima) e outro em pe (visto de lado): bom de qualquer angulo
      v(c, m, cx + tx, y, cz + tz, r, g, b, al);
      v(c, m, cx + rx, y, cz + rz, r, g, b, al);
      v(c, m, cx - tx, y, cz - tz, r, g, b, al);
      v(c, m, cx - rx, y, cz - rz, r, g, b, al);
      v(c, m, cx + tx, y, cz + tz, r, g, b, al);
      v(c, m, cx, y + size, cz, r, g, b, al);
      v(c, m, cx - tx, y, cz - tz, r, g, b, al);
      v(c, m, cx, y - size, cz, r, g, b, al);
    }
  }

  private static void v(VertexConsumer c, Matrix4f m, float x, float y, float z, int r, int g, int b, int a) {
    c.vertex(m, x, y, z).color(r, g, b, a).endVertex();
  }

  /** Quadrado unitario centrado, com UV completo; double-sided por chamada dupla. */
  private static void quad(VertexConsumer c, PoseStack pose, int light, int r, int g, int b, int a, boolean both) {
    PoseStack.Pose last = pose.last();
    Matrix4f m = last.pose();
    Matrix3f n = last.normal();
    float[][] pts = {{-0.5f, -0.5f, 0, 1}, {0.5f, -0.5f, 1, 1}, {0.5f, 0.5f, 1, 0}, {-0.5f, 0.5f, 0, 0}};
    for (float[] p : pts) {
      c.vertex(m, p[0], p[1], 0f).color(r, g, b, a).uv(p[2], p[3]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0f, 0f, 1f).endVertex();
    }
  }

  @Override
  public ResourceLocation getTextureLocation(GrimoireEntity entity) {
    return cover(entity.visualStage(), entity.tickCount);
  }
}
