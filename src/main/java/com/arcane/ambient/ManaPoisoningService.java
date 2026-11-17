package com.arcane.ambient;

import com.arcane.execution.EnvironmentalSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class ManaPoisoningService {
  private ManaPoisoningService() {}

  public static void tick(ServerPlayer player, EnvironmentalSnapshot environment) {
    if (environment.poisoningExposureRate() <= 0) return;
    float protection = getProtection(player);
    float exposure = (float) (environment.poisoningExposureRate() * (1.0 - protection));
    if (exposure <= 0) return;
    // Invoked on a 20-tick cadence; keep confusion ~every 40 ticks and damage every call.
    if ((player.tickCount / 20) % 2 == 0) {
      player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
    }
    if (exposure > 0.2f) {
      player.hurt(player.damageSources().magic(), exposure * 4f * CRYSTAL_DAMAGE());
      int zone = zoneOf(environment);
      if (zone >= 4) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, zone - 4));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, zone - 4));
      }
      if (zone >= 5) player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
      player.playNotifySound(com.arcane.registry.ModSounds.MANA_POISON.get(),
          net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 0.8f + player.getRandom().nextFloat() * 0.3f);
    }
  }

  private static float CRYSTAL_DAMAGE() { return com.arcane.config.ArcaneConfig.CRYSTAL_POISON_DAMAGE.get().floatValue(); }

  public static int zoneOf(EnvironmentalSnapshot env) {
    for (String tag : env.sourceTags()) {
      if (tag.startsWith("zone:")) {
        try { return Integer.parseInt(tag.substring(5)); } catch (NumberFormatException ignored) { return 0; }
      }
    }
    return 0;
  }

  /** Chance maxima (%/s) por zona 1..5, em fracao da config (a zona 5 usa o valor cheio). */
  public static double wildCastCeiling(int zone) {
    if (zone <= 0) return 0.0;
    return com.arcane.config.ArcaneConfig.CRYSTAL_WILD_CAST_MAX.get() * zone / 5.0;
  }

  /**
   * Perto do cristal, a mana bruta dispara magias sozinha: a cada segundo sorteia-se uma porcentagem (ate o teto da zona)
   * e rola-se essa chance; se acertar, uma magia aleatoria do grimorio do mago e lancada de graca.
   */
  public static void wildCast(ServerPlayer player, EnvironmentalSnapshot environment) {
    double ceiling = wildCastCeiling(zoneOf(environment));
    if (ceiling <= 0 || player.isSpectator()) return;
    var rnd = player.getRandom();
    double percent = rnd.nextDouble() * ceiling;
    if (rnd.nextDouble() * 100.0 >= percent) return;
    var grimoire = com.arcane.grimoire.GrimoireAttachments.get(player);
    if (grimoire.isEmpty() || grimoire.get().spellbook().isEmpty()) return;
    var spells = new java.util.ArrayList<>(grimoire.get().spellbook().values());
    for (int tries = 0; tries < 3 && !spells.isEmpty(); tries++) {
      var spell = spells.remove(rnd.nextInt(spells.size()));
      if (com.arcane.execution.CastingService.tryCast(player, spell, player.serverLevel()).success()) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.arcane.wild_cast"), true);
        return;
      }
    }
  }

  private static float getProtection(ServerPlayer player) {
    float protection = 0f;
    if (player.getInventory().getArmor(3).is(com.arcane.registry.ModItems.ARCANE_MASK.get())) protection += 0.3f;
    if (player.getInventory().getArmor(2).is(com.arcane.registry.ModItems.INSULATING_CLOAK.get())) protection += 0.4f;
    return Math.min(0.9f, protection);
  }
}
