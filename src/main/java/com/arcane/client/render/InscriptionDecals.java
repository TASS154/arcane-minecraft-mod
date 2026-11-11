package com.arcane.client.render;

import com.arcane.ArcaneMod;
import com.arcane.network.packet.S2CInscriptionDecalsPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Inscricoes escritas com a pena: o desenho fica NA superficie em que foi feito (um selo com os glifos da magia: o
 * primeiro no centro e os outros em volta), sem particulas. Cor pela tinta; a tinta invisivel nao e enviada.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public final class InscriptionDecals {
  private static volatile List<S2CInscriptionDecalsPacket.Decal> decals = List.of();

  private InscriptionDecals() {}

  public static void set(List<S2CInscriptionDecalsPacket.Decal> list) { decals = list; }

  private static ResourceLocation icon(ResourceLocation glyph) {
    return new ResourceLocation(glyph.getNamespace(), "textures/gui/glyphs/" + glyph.getPath() + ".png");
  }

  @SubscribeEvent
  public static void onRender(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
    List<S2CInscriptionDecalsPacket.Decal> list = decals;
    if (list.isEmpty()) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;
    Vec3 cam = event.getCamera().getPosition();
    PoseStack pose = event.getPoseStack();
    MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
    float time = (mc.level.getGameTime() + event.getPartialTick()) * 0.05f;
    for (S2CInscriptionDecalsPacket.Decal d : list) {
      Vec3 center = Vec3.atCenterOf(d.pos()).add(d.face().getStepX() * 0.505, d.face().getStepY() * 0.505, d.face().getStepZ() * 0.505);
      if (center.distanceToSqr(cam) > 48.0 * 48.0) continue;
      draw(pose, buffers, d, center.subtract(cam), time);
    }
    buffers.endBatch();
  }

  private static void draw(PoseStack pose, MultiBufferSource buffers, S2CInscriptionDecalsPacket.Decal d, Vec3 at, float time) {
    Direction f = d.face();
    Vec3 n = new Vec3(f.getStepX(), f.getStepY(), f.getStepZ());
    Vec3 v = f.getAxis() == Direction.Axis.Y ? new Vec3(0, 0, -1) : new Vec3(0, 1, 0);
    Vec3 u = v.cross(n).normalize();
    int glyphs = d.glyphs().size();
    if (glyphs == 0) return;
    float pulse = 0.82f + 0.18f * (float) Math.sin(time * 2.0f + d.pos().asLong() % 7);
    int r = (d.color() >> 16) & 255, g = (d.color() >> 8) & 255, b = d.color() & 255;
    // centro: o primeiro glifo (a forma); em volta, os demais num anel
    quad(buffers, pose, d.glyphs().get(0), at, u, v, n, 0.30f, r, g, b, (int) (255 * pulse));
    int ring = glyphs - 1;
    for (int i = 0; i < ring; i++) {
      double a = Math.PI * 2 * i / Math.max(1, ring) + Math.PI / 2;
      float rr = 0.34f;
      Vec3 p = at.add(u.scale(Math.cos(a) * rr)).add(v.scale(Math.sin(a) * rr));
      quad(buffers, pose, d.glyphs().get(i + 1), p, u, v, n, 0.12f, r, g, b, (int) (230 * pulse));
    }
  }

  /** Quadrado com a textura do glifo, de lado 2*half, no plano (u, v). */
  private static void quad(MultiBufferSource buffers, PoseStack pose, ResourceLocation glyph, Vec3 c, Vec3 u, Vec3 v, Vec3 n,
                           float half, int r, int g, int b, int a) {
    VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(icon(glyph)));
    Matrix4f m = pose.last().pose();
    Matrix3f nm = pose.last().normal();
    Vec3 p0 = c.subtract(u.scale(half)).subtract(v.scale(half));
    Vec3 p1 = c.add(u.scale(half)).subtract(v.scale(half));
    Vec3 p2 = c.add(u.scale(half)).add(v.scale(half));
    Vec3 p3 = c.subtract(u.scale(half)).add(v.scale(half));
    int light = LightTexture.FULL_BRIGHT;
    vert(vc, m, nm, p0, 0f, 1f, r, g, b, a, light, n);
    vert(vc, m, nm, p1, 1f, 1f, r, g, b, a, light, n);
    vert(vc, m, nm, p2, 1f, 0f, r, g, b, a, light, n);
    vert(vc, m, nm, p3, 0f, 0f, r, g, b, a, light, n);
    // verso (vista de tras da superficie translucida e para o caso de a face ficar de costas)
    vert(vc, m, nm, p3, 0f, 0f, r, g, b, a, light, n.scale(-1));
    vert(vc, m, nm, p2, 1f, 0f, r, g, b, a, light, n.scale(-1));
    vert(vc, m, nm, p1, 1f, 1f, r, g, b, a, light, n.scale(-1));
    vert(vc, m, nm, p0, 0f, 1f, r, g, b, a, light, n.scale(-1));
  }

  private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3 p, float u, float v, int r, int g, int b, int a, int light, Vec3 n) {
    vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light).normal(nm, (float) n.x, (float) n.y, (float) n.z).endVertex();
  }
}
