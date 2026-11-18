package com.arcane.client;

import com.arcane.ArcaneMod;
import com.arcane.registry.ModEffects;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
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
  private static final Set<UUID> PUSHED = new HashSet<>();
  private static boolean wasFloating;
  /** Jogadores em voo rapido (sprint): a onda de choque dispara quando entram nesta lista. */
  private static final Set<UUID> FAST = new HashSet<>();
  /** Aumento de FOV do jogador local no voo rapido (0 a 1), suavizado. */
  private static float fovBoost;
  private static final int CYAN = 0x9FE8FF;

  private FloatFlightClient() {}

  private static double speedOf(MobEffectInstance effect) {
    return com.arcane.execution.FlightService.speedOf(effect.getAmplifier());
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
    double speed = speedOf(effect) * (player.isSprinting() ? 2.0 : 1.0);
    Vec3 look = player.getLookAngle();
    Vec3 left = new Vec3(look.z, 0, -look.x).normalize();
    float fwd = player.input.forwardImpulse;
    float side = player.input.leftImpulse;
    double up = (player.input.jumping ? 1.0 : 0.0) - (player.input.shiftKeyDown ? 1.0 : 0.0);
    double glide = com.arcane.execution.FlightService.glideOf(effect.getAmplifier());
    Vec3 target = look.scale(fwd * speed).add(left.scale(side * speed * 0.7)).add(0, up * speed * 0.6, 0);
    if (glide > 0 && fwd <= 0 && side == 0 && up == 0) {
      // Planagem: sem comandos, o corpo segue planando na direcao do olhar (como uma elytra) e perde altura devagar,
      // em vez de parar no ar. Quanto maior a planagem, mais rapido ele plana e mais devagar cai.
      Vec3 flat = new Vec3(look.x, 0, look.z);
      Vec3 forward = flat.lengthSqr() < 1.0e-4 ? Vec3.ZERO : flat.normalize().scale(speed * 0.55 * glide);
      target = forward.add(0, -0.06 * (1.0 - 0.8 * glide), 0);
    }
    // Aceleracao suave: chega perto do alvo em ~1 s; com planagem ha mais inercia (o embalo dura mais).
    Vec3 velocity = player.getDeltaMovement().lerp(target, 0.09 * (1.0 - 0.7 * glide));
    player.setDeltaMovement(velocity);
  }

  private static double horizontalSpeed(Player p) {
    double dx = p.getX() - p.xo, dy = p.getY() - p.yo, dz = p.getZ() - p.zo;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  /** Efeitos de todos os jogadores em voo: corpo alinhado ao olhar, onda de choque, rastro e FOV. */
  @SubscribeEvent
  public static void onLevelTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null) { FAST.clear(); fovBoost = 0f; return; }
    float fovTarget = 0f;
    for (Player p : mc.level.players()) {
      if (!p.hasEffect(ModEffects.FLOAT_FLIGHT.get())) { FAST.remove(p.getUUID()); continue; }
      double speed = horizontalSpeed(p);
      if (speed > 0.06) { p.yBodyRot = p.getYRot(); p.yBodyRotO = p.getYRot(); }
      boolean fast = p.isSprinting() && speed > 0.15;
      if (fast) {
        if (FAST.add(p.getUUID())) shockwave(mc, p);
        trail(mc, p);
        if (p == mc.player) fovTarget = 1f;
      } else {
        FAST.remove(p.getUUID());
      }
    }
    fovBoost += (fovTarget - fovBoost) * 0.12f;
  }

  /** Anéis de luz que se abrem para os lados, num plano perpendicular ao voo, mais um estouro. */
  private static void shockwave(Minecraft mc, Player p) {
    Vec3 look = p.getLookAngle();
    Vec3 u = Math.abs(look.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(look.z, 0, -look.x).normalize();
    Vec3 v = look.cross(u).normalize();
    Vec3 c = p.position().add(0, p.getBbHeight() * 0.6, 0).add(look.scale(0.8));
    var level = mc.level;
    for (int ring = 0; ring < 3; ring++) {
      double r0 = 0.4 + ring * 0.5, out = 0.55 + ring * 0.25;
      for (int i = 0; i < 40; i++) {
        double a = Math.PI * 2 * i / 40;
        Vec3 d = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
        level.addParticle(ring == 0 ? ParticleTypes.END_ROD : ring == 1 ? ParticleTypes.CLOUD : ParticleTypes.GLOW,
            c.x + d.x * r0, c.y + d.y * r0, c.z + d.z * r0, d.x * out, d.y * out, d.z * out);
      }
    }
    level.addParticle(ParticleTypes.FLASH, c.x, c.y, c.z, 0, 0, 0);
    level.playLocalSound(p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.0f, 0.7f, false);
    level.playLocalSound(p.getX(), p.getY(), p.getZ(), com.arcane.registry.ModSounds.FLIGHT_BOOST.get(), SoundSource.PLAYERS, 1.0f, 1.0f, false);
  }

  /** Rastro de luz ciano ao longo do caminho percorrido neste tick. */
  private static void trail(Minecraft mc, Player p) {
    var level = mc.level;
    var rnd = level.random;
    Vec3 from = new Vec3(p.xo, p.yo + p.getBbHeight() * 0.55, p.zo), to = p.position().add(0, p.getBbHeight() * 0.55, 0);
    DustParticleOptions dust = new DustParticleOptions(new org.joml.Vector3f(0.62f, 0.91f, 1.0f), 1.4f);
    for (int i = 0; i < 5; i++) {
      Vec3 at = from.lerp(to, i / 5.0);
      double sx = (rnd.nextDouble() - 0.5) * 0.35, sy = (rnd.nextDouble() - 0.5) * 0.35, sz = (rnd.nextDouble() - 0.5) * 0.35;
      level.addParticle(dust, at.x + sx, at.y + sy, at.z + sz, 0, 0, 0);
      if (i % 2 == 0) level.addParticle(ParticleTypes.END_ROD, at.x + sx, at.y + sy, at.z + sz, 0, 0.005, 0);
    }
    if (rnd.nextInt(3) == 0) level.addParticle(ParticleTypes.CLOUD, to.x, to.y, to.z, 0, 0, 0);
  }

  @SubscribeEvent
  public static void onFov(ComputeFovModifierEvent event) {
    if (fovBoost > 0.001f) event.setNewFovModifier(event.getNewFovModifier() * (1.0f + 0.35f * fovBoost));
  }

  /** Movimento suavizado por jogador: {inclinacao (graus), rolagem (graus), frente, lado} (frente e lado de -1 a 1). */
  private static final Map<UUID, float[]> MOTION = new HashMap<>();

  /** Estado de movimento para o modelo (nunca nulo). */
  public static float[] motion(UUID id) {
    float[] m = MOTION.get(id);
    return m == null ? new float[4] : m;
  }

  /**
   * Estilo Frieren: so ir para a FRENTE deita o corpo (como quem voa olhando adiante, com a cabeca sempre em pe, ver
   * PlayerAnimClient). Subir e descer nao deitam; ir para os lados ou para tras mantem o corpo em pe (so uma leve
   * inclinacao) e mexe tronco, pernas e bracos. Tudo medido em relacao a para onde o corpo esta virado.
   */
  @SubscribeEvent
  public static void onRenderPre(RenderPlayerEvent.Pre event) {
    Player player = event.getEntity();
    if (!player.hasEffect(ModEffects.FLOAT_FLIGHT.get())) {
      MOTION.remove(player.getUUID());
      return;
    }
    double dx = player.getX() - player.xo, dz = player.getZ() - player.zo;
    double yawRad = Math.toRadians(player.getYRot());
    double fwdSpeed = dx * -Math.sin(yawRad) + dz * Math.cos(yawRad);
    double sideSpeed = dx * -Math.cos(yawRad) + dz * -Math.sin(yawRad); // positivo = para a direita de quem voa
    float fwd = (float) Mth.clamp(fwdSpeed / 0.2, -1.0, 1.0);
    float side = (float) Mth.clamp(sideSpeed / 0.15, -1.0, 1.0);
    float leanTarget;
    if (fwd > 0.2f) leanTarget = 78.0f * Math.min(1f, (fwd - 0.2f) / 0.5f) + 10.0f * (float) Mth.clamp((fwdSpeed - 0.2) / 0.2, 0.0, 1.0);
    else leanTarget = fwd < -0.2f ? 10.0f * fwd : 0.0f; // para tras: so um leve recuo do tronco
    float rollTarget = 12.0f * side;
    float[] m = MOTION.computeIfAbsent(player.getUUID(), k -> new float[4]);
    m[0] += (leanTarget - m[0]) * 0.08f; // as pernas demoram: o corpo acompanha aos poucos
    m[1] += (rollTarget - m[1]) * 0.08f;
    m[2] += (fwd - m[2]) * 0.12f;
    m[3] += (side - m[3]) * 0.12f;
    float yaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
    var pose = event.getPoseStack();
    pose.pushPose();
    pose.translate(0.0, PIVOT, 0.0);
    pose.mulPose(Axis.YP.rotationDegrees(-yaw));
    pose.mulPose(Axis.XP.rotationDegrees(m[0]));
    pose.mulPose(Axis.ZP.rotationDegrees(m[1]));
    pose.mulPose(Axis.YP.rotationDegrees(yaw));
    pose.translate(0.0, -PIVOT, 0.0);
    PUSHED.add(player.getUUID());
  }

  @SubscribeEvent
  public static void onRenderPost(RenderPlayerEvent.Post event) {
    if (PUSHED.remove(event.getEntity().getUUID())) event.getPoseStack().popPose();
  }
}
