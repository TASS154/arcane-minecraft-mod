package com.arcane.client.render;

import com.arcane.entity.ArcaneDummyEntity;
import com.arcane.training.HeartText;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Boneco de Treino: corpo humanoide com armadura e um letreiro sobre a cabeca (barra de coracoes, ultimo golpe em
 * coracoes, media por segundo, tags de estado, mana da ultima conjuracao, armadura e perfil). NAO visto em jogo.
 */
public class ArcaneDummyRenderer extends HumanoidMobRenderer<ArcaneDummyEntity, HumanoidModel<ArcaneDummyEntity>> {
  private static final ResourceLocation TEXTURE = new ResourceLocation("arcane", "textures/entity/training_dummy.png");
  private static final int RED = 0xFF3A3A, HALF = 0xFF9A9A, EMPTY = 0x4A4A4A;
  private static final int[] TAG_COLORS = {0x4AA8FF, 0xFF8C28, 0xA8E4FF, 0xE0B0FF, 0x78EB46, 0xB0B0B0};

  public ArcaneDummyRenderer(EntityRendererProvider.Context ctx) {
    super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
        new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)), ctx.getModelManager()));
  }

  @Override
  public ResourceLocation getTextureLocation(ArcaneDummyEntity entity) { return TEXTURE; }

  @Override
  protected boolean shouldShowName(ArcaneDummyEntity entity) { return false; }

  @Override
  public void render(ArcaneDummyEntity dummy, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
    super.render(dummy, yaw, partial, pose, buffers, light);
    if (entityRenderDispatcher.distanceToSqr(dummy) > 24 * 24) return;
    List<Component> lines = lines(dummy);
    pose.pushPose();
    pose.translate(0, dummy.getBbHeight() + 0.35 + lines.size() * 0.25, 0);
    pose.mulPose(entityRenderDispatcher.cameraOrientation());
    pose.scale(-0.025f, -0.025f, 0.025f);
    Font font = getFont();
    var matrix = pose.last().pose();
    int bg = (int) (0.35f * 255) << 24;
    for (int i = 0; i < lines.size(); i++) {
      Component line = lines.get(i);
      float x = -font.width(line) / 2f;
      font.drawInBatch(line, x, i * 10, 0xFFFFFFFF, false, matrix, buffers, Font.DisplayMode.NORMAL, bg, light);
    }
    pose.popPose();
  }

  private static List<Component> lines(ArcaneDummyEntity d) {
    List<Component> out = new ArrayList<>();
    out.add(bar(d.getHealth(), d.getMaxHealth()));
    out.add(Component.translatable("gui.arcane.dummy.last").withStyle(ChatFormatting.GRAY).append(" ").append(hearts(d.syncedLastHit())));
    out.add(Component.translatable("gui.arcane.dummy.rate", HeartText.rate(d.syncedPerSecond())).withStyle(ChatFormatting.GRAY));
    int bits = d.syncedState();
    if (bits != 0) {
      MutableComponent tags = Component.empty();
      boolean first = true;
      for (int i = 0; i < ArcaneDummyEntity.TAGS.length; i++) {
        if ((bits & (1 << i)) == 0) continue;
        if (!first) tags.append(" ");
        first = false;
        final int color = colorOf(i);
        tags.append(Component.translatable("gui.arcane.state." + ArcaneDummyEntity.TAGS[i]).withStyle(s -> s.withColor(TextColor.fromRgb(color))));
      }
      out.add(tags);
    }
    if (d.syncedMana() >= 0) out.add(Component.translatable("gui.arcane.dummy.mana", d.syncedMana()).withStyle(ChatFormatting.AQUA));
    String profile = d.affinityProfile();
    out.add(Component.translatable("gui.arcane.dummy.armor." + ArcaneDummyEntity.ARMOR_NAMES[Math.floorMod(d.armorTier(), 4)])
        .append(" · ").append(profile.isEmpty() ? Component.translatable("affinity.arcane.neutral") : Component.translatableWithFallback("affinity." + profile.replace(':', '.'), profile))
        .withStyle(ChatFormatting.DARK_GRAY));
    return out;
  }

  private static int colorOf(int i) { return TAG_COLORS[i % TAG_COLORS.length]; }

  /** Barra de 10 coracoes (cada um = 1/10 da vida maxima): cheio vermelho, meio rosa, vazio cinza. */
  static Component bar(float health, float max) {
    MutableComponent c = Component.empty();
    float slots = max <= 0 ? 0 : 10f * health / max;
    for (int i = 0; i < 10; i++) {
      float fill = slots - i;
      int color = fill >= 0.75f ? RED : fill >= 0.25f ? HALF : EMPTY;
      c.append(Component.literal(HeartText.FULL).withStyle(s -> s.withColor(TextColor.fromRgb(color))));
    }
    return c;
  }

  /** Dano do ultimo golpe em coracoes: cheios em vermelho, o meio em rosa; acima de 10, "♥×N". */
  static Component hearts(float hp) {
    String text = HeartText.symbols(hp);
    if (text.startsWith(HeartText.SMALL) || text.equals("-")) return Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(RED)));
    MutableComponent c = Component.empty();
    var hs = HeartText.hearts(hp);
    c.append(Component.literal(HeartText.FULL.repeat(hs.full())).withStyle(s -> s.withColor(TextColor.fromRgb(RED))));
    if (hs.half()) c.append(Component.literal("½").withStyle(s -> s.withColor(TextColor.fromRgb(HALF))));
    return c;
  }
}
