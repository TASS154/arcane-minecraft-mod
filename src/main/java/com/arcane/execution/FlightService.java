package com.arcane.execution;

import com.arcane.ArcaneMod;
import com.arcane.config.ArcaneConfig;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Voo canalizado (glifo Flutuar). Enquanto o efeito dura, o servidor cobra mana a cada tick (proporcional a velocidade);
 * sem mana o voo acaba. A permissao de voo so existe durante o efeito (ver ModEffects.FloatFlight) e, ao acabar, a
 * PRIMEIRA queda e amortecida: o jogador nao leva dano de queda de onde parou de voar.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class FlightService {
  public static final String SAFE_TAG = "arcane_flight_safe_until";
  /** A protecao de queda vale por no maximo 60 s depois do voo. */
  public static final long SAFE_TICKS = 1200L;
  /** O voo rapido (sprint) gasta isto vezes mais mana por tick. */
  public static final double SPRINT_COST_FACTOR = 2.5;

  private FlightService() {}

  /** Velocidade (blocos por tick) guardada na amplificacao do efeito: 5 bits em decimos. */
  public static double speedOf(int amplifier) { return Math.max(0.3, (amplifier & 31) / 10.0); }

  /** Planagem (0 a 1) guardada nos 3 bits de cima da amplificacao. */
  public static double glideOf(int amplifier) { return ((amplifier >> 5) & 7) / 7.0; }

  public static int encode(double speed, double glide) {
    int tenths = (int) Math.max(3, Math.min(30, Math.round(speed * 10.0)));
    int level = (int) Math.max(0, Math.min(7, Math.round(glide * 7.0)));
    return tenths | (level << 5);
  }

  /** Mana por tick de um voo com esta amplificacao. */
  public static double upkeep(int amplifier) {
    return ArcaneConfig.FLIGHT_MANA_PER_TICK.get() * speedOf(amplifier);
  }

  @SubscribeEvent
  public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
    var effect = player.getEffect(ModEffects.FLOAT_FLIGHT.get());
    if (effect == null || player.isSpectator()) return;
    double cost = upkeep(effect.getAmplifier()) * (player.isSprinting() ? SPRINT_COST_FACTOR : 1.0);
    if (cost <= 0) return;
    var mana = GrimoireAttachments.getManaOrCreate(player);
    if (mana.current() < cost) {
      mana.set(0f);
      player.removeEffect(ModEffects.FLOAT_FLIGHT.get());
      player.displayClientMessage(Component.translatable("message.arcane.flight_exhausted"), true);
    } else {
      mana.set((float) (mana.current() - cost));
    }
  }

  /** Marca a primeira queda depois do voo como segura. */
  public static void markSafe(ServerPlayer player) {
    player.getPersistentData().putLong(SAFE_TAG, player.level().getGameTime() + SAFE_TICKS);
  }

  @SubscribeEvent
  public static void onFall(LivingFallEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    long until = player.getPersistentData().getLong(SAFE_TAG);
    if (until == 0L) return;
    if (player.level().getGameTime() <= until) {
      event.setDamageMultiplier(0.0f);
      event.setDistance(0.0f);
    }
    player.getPersistentData().remove(SAFE_TAG); // vale uma vez: a proxima queda machuca normal
  }
}
