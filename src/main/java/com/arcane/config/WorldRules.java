package com.arcane.config;

import com.arcane.ritual.RitualSettings;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Regras do mundo editaveis em jogo (so por administradores). O servidor e quem muda: o cliente pede "avancar a regra X"
 * e recebe o estado novo. Cada mudanca e salva na hora em serverconfig/arcane-server.toml (ConfigValue.set grava).
 * Regra booleana: 0/1. Regra numerica: percorre os passos; 0 = sem limite, sempre o ultimo passo.
 */
public final class WorldRules {
  public record Rule(String key, double[] steps, DoubleSupplier get, DoubleConsumer set) {
    boolean isFlag() { return steps == null; }
  }

  private static final double[] AREA_CAPS = {1, 2, 4, 8, 16, 32, 0};
  private static final double[] CHARGE_CAPS = {3, 6, 10, 20, 50, 0};
  private static final double[] RITUAL_CAPS = {500, 1000, 1500, 3000, 10000, 0};

  public static final List<Rule> ALL = List.of(
      flag("world_altering", ArcaneConfig.WORLD_ALTERING_SPELLS),
      flag("block_breaking", ArcaneConfig.BLOCK_BREAKING_SPELLS),
      flag("interactions", ArcaneConfig.ELEMENT_INTERACTIONS),
      flag("scars", ArcaneConfig.SCARS_ENABLED),
      flag("hard_caps", ArcaneConfig.AREA_HARD_CAPS),
      new Rule("crystal_cap", AREA_CAPS, ArcaneConfig.CRYSTAL_AREA_CAP::get, ArcaneConfig.CRYSTAL_AREA_CAP::set),
      new Rule("charge_cap", CHARGE_CAPS, ArcaneConfig.CHARGE_MAX_SCALE::get, ArcaneConfig.CHARGE_MAX_SCALE::set),
      new Rule("ritual_cap", RITUAL_CAPS, ArcaneConfig.RITUAL_MAX_PERCENT::get, v -> ArcaneConfig.RITUAL_MAX_PERCENT.set((int) v)),
      flag("unique_break", ArcaneConfig.UNIQUE_OFFERINGS_BREAK_CAP));

  private WorldRules() {}

  private static Rule flag(String key, net.minecraftforge.common.ForgeConfigSpec.BooleanValue value) {
    return new Rule(key, null, () -> value.get() ? 1.0 : 0.0, v -> value.set(v > 0.5));
  }

  /** Quem pode mudar: operador (nivel 2) ou o dono de um mundo local. */
  public static boolean canEdit(ServerPlayer player) {
    var server = player.getServer();
    return player.hasPermissions(2) || (server != null && server.isSingleplayerOwner(player.getGameProfile()));
  }

  public static Map<String, Double> snapshot() {
    Map<String, Double> out = new LinkedHashMap<>();
    for (Rule r : ALL) out.put(r.key(), r.get().getAsDouble());
    return out;
  }

  /** Avanca a regra (inverte o booleano ou vai ao proximo passo). Devolve false se a chave nao existe. */
  public static boolean advance(String key) {
    for (Rule r : ALL) {
      if (!r.key().equals(key)) continue;
      double now = r.get().getAsDouble();
      r.set().accept(r.isFlag() ? (now > 0.5 ? 0.0 : 1.0) : next(now, r.steps()));
      RitualSettings.setMaxPercent(ArcaneConfig.RITUAL_MAX_PERCENT.get());
      RitualSettings.setUnique(ArcaneConfig.UNIQUE_OFFERINGS_BREAK_CAP.get(), ArcaneConfig.UNIQUE_OFFERINGS.get());
      return true;
    }
    return false;
  }

  /** Proximo valor; 0 (sem limite) e o ultimo e depois dele volta ao primeiro. */
  public static double next(double now, double[] steps) {
    if (now <= 0) return steps[0];
    for (double s : steps) if (s > now + 1.0e-9) return s;
    return steps[steps.length - 1] == 0 ? 0 : steps[0];
  }
}
