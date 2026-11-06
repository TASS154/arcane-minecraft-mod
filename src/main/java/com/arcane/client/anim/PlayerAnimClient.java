package com.arcane.client.anim;

import com.arcane.ArcaneMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Animacao do corpo dos jogadores (cliente). O servidor manda um clipe; aqui cada jogador guarda qual clipe toca e
 * desde quando, e o {@link ArcaneModel} (que substitui o modelo do renderer) aplica a pose por cima da animacao normal.
 * Clipes encadeados vem de {@link PlayerClips}. Funciona para o proprio jogador (em 3a pessoa) e para os outros.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PlayerAnimClient {
  private record Playing(PlayerClips.Clip clip, float startAge) {}

  private static final Map<UUID, Playing> PLAYING = new ConcurrentHashMap<>();

  private PlayerAnimClient() {}

  public static void play(UUID player, int clipId) {
    PlayerClips.Clip clip = PlayerClips.byId(clipId);
    Minecraft mc = Minecraft.getInstance();
    if (clip == null || mc.level == null) return;
    Entity e = mc.level.getPlayerByUUID(player);
    if (e == null) return;
    PLAYING.put(player, new Playing(clip, e.tickCount));
  }

  /** Modelo do jogador com a pose do clipe ativo aplicada depois do calculo normal. */
  public static final class ArcaneModel extends PlayerModel<AbstractClientPlayer> {
    private final float[] pose = new float[9];

    public ArcaneModel(net.minecraft.client.model.geom.ModelPart root, boolean slim) {
      super(root, slim);
    }

    @Override
    public void setupAnim(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
      super.setupAnim(p, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      Playing playing = PLAYING.get(p.getUUID());
      if (playing == null) return;
      float t = p.tickCount + (ageInTicks - (int) ageInTicks) - playing.startAge();
      float w = PlayerClips.sample(playing.clip(), t, pose);
      if (w <= 0f) {
        if (t >= playing.clip().total()) PLAYING.remove(p.getUUID(), playing);
        return;
      }
      float rad = Mth.DEG_TO_RAD;
      float twist = pose[6] * rad * w;
      // Braco direito do modelo fica no lado oposto ao esquerdo: espelha y/z para os dois "abrirem" para fora.
      rightArm.xRot = Mth.lerp(w, rightArm.xRot, pose[0] * rad);
      rightArm.yRot = Mth.lerp(w, rightArm.yRot, pose[1] * rad) + twist;
      rightArm.zRot = Mth.lerp(w, rightArm.zRot, pose[2] * rad);
      leftArm.xRot = Mth.lerp(w, leftArm.xRot, pose[3] * rad);
      leftArm.yRot = Mth.lerp(w, leftArm.yRot, pose[4] * rad) + twist;
      leftArm.zRot = Mth.lerp(w, leftArm.zRot, pose[5] * rad);
      body.yRot = twist;
      body.xRot = Mth.lerp(w, body.xRot, pose[7] * rad);
      head.xRot += pose[8] * rad * w;
      head.yRot -= twist * 0.5f;
      leftSleeve.copyFrom(leftArm);
      rightSleeve.copyFrom(rightArm);
      jacket.copyFrom(body);
      hat.copyFrom(head);
    }
  }

  /** Troca o modelo dos dois renderers de jogador (classico e fino) pelo {@link ArcaneModel}. */
  @SubscribeEvent
  public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
    for (String skin : event.getSkins()) {
      if (!(event.getSkin(skin) instanceof PlayerRenderer renderer)) continue;
      boolean slim = "slim".equals(skin);
      ArcaneModel model = new ArcaneModel(event.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
      if (!swapModel(renderer, model)) {
        com.mojang.logging.LogUtils.getLogger().warn("Arcane: nao consegui trocar o modelo do jogador ({}); sem animacoes", skin);
      }
    }
  }

  /** Acha o campo do modelo pelo tipo (nao depende do nome mapeado) e o substitui. */
  private static boolean swapModel(LivingEntityRenderer<?, ?> renderer, EntityModel<?> model) {
    for (Class<?> c = renderer.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field f : c.getDeclaredFields()) {
        if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || !EntityModel.class.isAssignableFrom(f.getType())) continue;
        try {
          f.setAccessible(true);
          if (f.get(renderer) instanceof PlayerModel<?>) {
            f.set(renderer, model);
            return true;
          }
        } catch (ReflectiveOperationException | RuntimeException ex) {
          return false;
        }
      }
    }
    return false;
  }
}
