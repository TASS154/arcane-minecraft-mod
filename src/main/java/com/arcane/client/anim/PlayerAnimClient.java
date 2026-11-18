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

    /**
     * Pose de canalizacao (so do proprio jogador, que e quem recebe o estado): as maos se erguem para a forma da magia e
     * tremem mais quanto mais carregada; ao segurar uma magia ficam firmes, com o corpo levemente inclinado para tras.
     */
    private void channelPose(AbstractClientPlayer p, float age) {
      if (!com.arcane.client.render.ChannelFx.active() || p != Minecraft.getInstance().player) return;
      float charge = (float) Math.max(0.0, com.arcane.client.render.ChannelFx.chargeProgress());
      float shake = Mth.sin(age * 1.9f) * (0.01f + 0.05f * charge);
      float arm = -1.45f - 0.35f * charge;
      rightArm.xRot = arm + shake; leftArm.xRot = arm - shake;
      rightArm.zRot = 0.18f + 0.18f * charge; leftArm.zRot = -0.18f - 0.18f * charge;
      rightArm.yRot = -0.10f; leftArm.yRot = 0.10f;
      body.xRot = -0.10f - 0.08f * charge;
      head.xRot += -0.12f;
      leftSleeve.copyFrom(leftArm); rightSleeve.copyFrom(rightArm);
      jacket.copyFrom(body); hat.copyFrom(head);
    }

    @Override
    public void setupAnim(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
      boolean flying = p.hasEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get());
      // Em voo nao ha passada: sem balanco de pernas e bracos (o corpo e o ponto de apoio, ver FloatFlightClient).
      super.setupAnim(p, flying ? 0f : limbSwing, flying ? 0f : limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      if (flying) {
        float[] mo = com.arcane.client.FloatFlightClient.motion(p.getUUID());
        float lean = mo[0] * Mth.DEG_TO_RAD, fwd = mo[2], side = mo[3];
        float sway = Mth.sin(ageInTicks * 0.08f) * 0.04f;
        float back = Math.max(0f, -fwd), ahead = Math.max(0f, fwd), sideAbs = Math.abs(side);
        // A cabeca fica SEMPRE em pe, olhando adiante: desfaz a inclinacao do corpo (a pose inteira esta girada em FloatFlightClient)
        head.xRot = -lean + headPitch * Mth.DEG_TO_RAD * 0.35f;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5f;
        // tronco: gira para o lado em que se vai, e recua um pouco para tras
        body.yRot = side * 0.30f;
        body.xRot = back * 0.12f;
        // pernas: arrastam atras ao avancar; abrem para os lados ao deslizar; passo curto para tras
        leftLeg.xRot = 0.10f + ahead * 0.18f - back * 0.55f + sway;
        rightLeg.xRot = 0.05f + ahead * 0.10f - back * 0.30f - sway;
        leftLeg.zRot = -0.05f - sideAbs * 0.30f + side * 0.10f;
        rightLeg.zRot = 0.05f + sideAbs * 0.30f + side * 0.10f;
        // bracos: esticados a frente ao avancar; abertos para equilibrar nos lados; para tras vao um pouco a frente
        leftArm.xRot = 0.15f - ahead * 0.55f - back * 0.45f - sway;
        rightArm.xRot = 0.15f - ahead * 0.55f - back * 0.45f + sway;
        leftArm.zRot = -0.12f - sideAbs * 0.45f + side * 0.20f;
        rightArm.zRot = 0.12f + sideAbs * 0.45f + side * 0.20f;
        leftSleeve.copyFrom(leftArm); rightSleeve.copyFrom(rightArm);
        leftPants.copyFrom(leftLeg); rightPants.copyFrom(rightLeg);
        jacket.copyFrom(body); hat.copyFrom(head);
      }
      Playing playing = PLAYING.get(p.getUUID());
      if (playing == null) { channelPose(p, ageInTicks); return; }
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
