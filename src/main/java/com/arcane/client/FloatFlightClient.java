package com.arcane.client;

import com.arcane.ArcaneMod;
import com.arcane.registry.ModEffects;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Voo do efeito Flutuar. O movimento do jogador e do cliente (como no voo normal), entao ele roda aqui: o olhar
 * define a direcao (W avanca, A/D deslocam, Espaco sobe, Shift desce, Ctrl acelera) e a velocidade aproxima-se do
 * alvo aos poucos, o que deixa o voo suave e permite chegar a velocidades altas. No desenho, o corpo se inclina com
 * o ponto de apoio na cabeca: a cabeca vai primeiro e as pernas seguem, atrasadas.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public final class FloatFlightClient {
  private static final double PIVOT = 1.5;
  private static final Map<UUID, Float> LEAN = new HashMap<>();
  private static final Set<UUID> PUSHED = new HashSet<>();
  private static boolean wasFloating;

  private FloatFlightClient() {}

  private static double speedOf(MobEffectInstance effect) {
    return Math.max(0.3, effect.getAmplifier() / 10.0);
  }

  @SubscribeEvent
  public static void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    LocalPlayer player = Minecraft.getInstance().player;
    if (player == null) { wasFloating = false; return; }
    MobEffectInstance effect = player.getEffect(ModEffects.FLOAT_FLIGHT.get());
    if (effect == null) {
      if (wasFloating) { player.setNoGravity(false); wasFloating = false; }
      return;
    }
    wasFloating = true;
    player.setNoGravity(true);
    player.fallDistance = 0.0f;
    double speed = speedOf(effect) * (player.isSprinting() ? 1.7 : 1.0);
    Vec3 look = player.getLookAngle();
    Vec3 left = new Vec3(look.z, 0, -look.x).normalize();
    float fwd = player.input.forwardImpulse;
    float side = player.input.leftImpulse;
    double up = (player.input.jumping ? 1.0 : 0.0) - (player.input.shiftKeyDown ? 1.0 : 0.0);
    Vec3 target = look.scale(fwd * speed).add(left.scale(side * speed * 0.7)).add(0, up * speed * 0.6, 0);
    // Aceleracao suave: chega perto do alvo em ~1 s; sem comandos, freia ate pairar.
    Vec3 velocity = player.getDeltaMovement().lerp(target, 0.09);
    player.setDeltaMovement(velocity);
  }

  private static float leanTarget(Player player) {
    double dx = player.getX() - player.xo, dy = player.getY() - player.yo, dz = player.getZ() - player.zo;
    double horizontal = Math.sqrt(dx * dx + dz * dz);
    double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (speed < 0.06) return 0.0f;
    // Quanto mais rapido e quanto mais horizontal, mais deitado; subir reto deixa de pe.
    float lean = (float) Math.min(80.0, speed * 95.0 * (0.35 + 0.65 * (horizontal / Math.max(1.0e-6, speed))));
    return Math.min(lean, 80.0f);
  }

  @SubscribeEvent
  public static void onRenderPre(RenderPlayerEvent.Pre event) {
    Player player = event.getEntity();
    if (!player.hasEffect(ModEffects.FLOAT_FLIGHT.get())) {
      LEAN.remove(player.getUUID());
      return;
    }
    float current = LEAN.getOrDefault(player.getUUID(), 0.0f);
    current += (leanTarget(player) - current) * 0.06f; // as pernas demoram: o corpo acompanha a cabeca aos poucos
    LEAN.put(player.getUUID(), current);
    float yaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
    var pose = event.getPoseStack();
    pose.pushPose();
    pose.translate(0.0, PIVOT, 0.0);
    pose.mulPose(Axis.YP.rotationDegrees(-yaw));
    pose.mulPose(Axis.XP.rotationDegrees(current));
    pose.mulPose(Axis.YP.rotationDegrees(yaw));
    pose.translate(0.0, -PIVOT, 0.0);
    PUSHED.add(player.getUUID());
  }

  @SubscribeEvent
  public static void onRenderPost(RenderPlayerEvent.Post event) {
    if (PUSHED.remove(event.getEntity().getUUID())) event.getPoseStack().popPose();
  }
}
