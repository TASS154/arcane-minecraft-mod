package com.arcane.client.render;

import com.arcane.ArcaneMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Invisibilidade (decisao de Everton): o corpo, a armadura e os itens na mao somem; nao ha particulas. O que denuncia
 * quem esta invisivel e uma distorcao turva: o contorno do corpo, quase transparente, que tremula e fica mais visivel
 * quanto mais rapido ele se move (parado e quase nada; correndo da para seguir com atencao).
 * Vale so para a invisibilidade do glifo (Veu Arcano, avisado pelo servidor); a pocao vanilla continua como sempre.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public final class InvisibilityRender {
  /** Transparencia parado e o maximo em movimento rapido. */
  public static final float STILL_ALPHA = 0.035f, MAX_ALPHA = 0.26f;
  private static boolean drawing;
  private static final java.util.Set<Integer> VEILED = new java.util.HashSet<>();

  public static void setVeiled(int entityId, boolean on) {
    if (on) VEILED.add(entityId); else VEILED.remove(entityId);
  }

  private InvisibilityRender() {}

  /** Quanto a distorcao aparece, pela distancia percorrida no ultimo tick. */
  public static float alphaFor(double moved) {
    return Mth.clamp(STILL_ALPHA + (float) moved * 0.9f, STILL_ALPHA, MAX_ALPHA);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @SubscribeEvent
  public static void onRender(RenderLivingEvent.Pre<?, ?> event) {
    LivingEntity entity = event.getEntity();
    if (drawing || !entity.isInvisible() || entity.isSpectator() || !VEILED.contains(entity.getId())) return;
    event.setCanceled(true);
    float partial = event.getPartialTick();
    double moved = Math.sqrt(Mth.square(entity.getX() - entity.xo) + Mth.square(entity.getY() - entity.yo) + Mth.square(entity.getZ() - entity.zo));
    float alpha = alphaFor(moved);
    LivingEntityRenderer renderer = event.getRenderer();
    ResourceLocation texture = renderer.getTextureLocation(entity);
    MultiBufferSource source = event.getMultiBufferSource();
    MultiBufferSource ghost = type -> new Faded(source.getBuffer(RenderType.entityTranslucent(texture)), alpha);

    // esconde armadura e itens so durante este desenho (cliente; o servidor nao sabe disso)
    EquipmentSlot[] slots = EquipmentSlot.values();
    ItemStack[] saved = new ItemStack[slots.length];
    for (int i = 0; i < slots.length; i++) {
      saved[i] = entity.getItemBySlot(slots[i]);
      if (!saved[i].isEmpty()) entity.setItemSlot(slots[i], ItemStack.EMPTY);
    }
    int arrows = entity.getArrowCount(), stingers = entity.getStingerCount();
    entity.setArrowCount(0);
    entity.setStingerCount(0);
    PoseStack pose = event.getPoseStack();
    pose.pushPose();
    float time = entity.tickCount + partial;
    float wobble = 0.015f + (float) moved * 0.08f; // o ar "ondula" em volta de quem se mexe
    pose.translate(Mth.sin(time * 1.7f) * wobble, 0, Mth.cos(time * 1.3f) * wobble);
    pose.scale(1f + Mth.sin(time * 2.3f) * wobble * 0.5f, 1f, 1f + Mth.cos(time * 2.1f) * wobble * 0.5f);
    drawing = true;
    entity.setInvisible(false);
    try {
      float yaw = Mth.lerp(partial, entity.yRotO, entity.getYRot());
      renderer.render(entity, yaw, partial, pose, ghost, event.getPackedLight());
    } finally {
      entity.setInvisible(true);
      drawing = false;
      pose.popPose();
      for (int i = 0; i < slots.length; i++) if (!saved[i].isEmpty()) entity.setItemSlot(slots[i], saved[i]);
      entity.setArrowCount(arrows);
      entity.setStingerCount(stingers);
    }
  }

  /** Repassa os vertices com a transparencia multiplicada. */
  private record Faded(VertexConsumer delegate, float alpha) implements VertexConsumer {
    @Override public VertexConsumer vertex(double x, double y, double z) { return delegate.vertex(x, y, z); }
    @Override public VertexConsumer color(int r, int g, int b, int a) { return delegate.color(r, g, b, (int) (a * alpha)); }
    @Override public VertexConsumer uv(float u, float v) { return delegate.uv(u, v); }
    @Override public VertexConsumer overlayCoords(int u, int v) { return delegate.overlayCoords(u, v); }
    @Override public VertexConsumer uv2(int u, int v) { return delegate.uv2(u, v); }
    @Override public VertexConsumer normal(float x, float y, float z) { return delegate.normal(x, y, z); }
    @Override public void endVertex() { delegate.endVertex(); }
    @Override public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r, g, b, (int) (a * alpha)); }
    @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    @Override
    public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float nx, float ny, float nz) {
      delegate.vertex(x, y, z, r, g, b, a * alpha, u, v, overlay, light, nx, ny, nz);
    }
  }
}
