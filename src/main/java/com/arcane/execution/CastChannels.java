package com.arcane.execution;

import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.grimoire.SpellPrefs.CastMode;
import com.arcane.inscription.Inscription;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modos de conjuracao (canalizar): a tecla de conjurar age conforme o modo escolhido para a magia.
 * TOQUE: lanca uma vez. CARREGAR: segurar drena mana aos poucos e ao soltar lanca uma vez, mais forte quanto mais
 * carregou. SEGURAR: repete a conjuracao enquanto a tecla estiver pressionada (cada repeticao custa a mana normal).
 * ALTERNAR: so para magias de efeito continuo (Flutuar, Telecinese): lanca e, no segundo toque, cancela.
 * Tudo e decidido aqui no servidor; o cliente so avisa "pressionou" e "soltou".
 */
public final class CastChannels {
  public static final int CHARGE_MAX_TICKS = 60;
  public static final double CHARGE_MAX_SCALE = 3.0;
  public static final int HOLD_INTERVAL_TICKS = 10;
  /** Magias com algum destes efeitos aceitam o modo ALTERNAR. */
  public static final Set<String> TOGGLE_GLYPHS = Set.of("float", "telekinesis");

  private static final class Active {
    final UUID spell;
    ServerPlayer owner;
    final CastMode mode;
    final long start;
    long next;
    double drained;
    Active(UUID spell, CastMode mode, long start, ServerPlayer owner) { this.owner = owner; this.spell = spell; this.mode = mode; this.start = start; this.next = start + HOLD_INTERVAL_TICKS; }
  }

  private static final Map<UUID, Active> ACTIVE = new ConcurrentHashMap<>();

  private CastChannels() {}

  public static boolean toggleable(Inscription inscription) {
    return inscription.glyphs().stream().anyMatch(g -> TOGGLE_GLYPHS.contains(g.glyphId().getPath()));
  }

  public static boolean isActive(UUID player) { return ACTIVE.containsKey(player); }

  private static boolean flies(Inscription inscription) {
    return inscription.glyphs().stream().anyMatch(g -> g.glyphId().getPath().equals("float"));
  }

  /** Mantem o voo ligado enquanto a canalizacao durar (o custo e por tick, em FlightService). */
  private static void sustainFlight(ServerPlayer player) {
    var effect = player.getEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get());
    if (effect != null && effect.getDuration() < 40) {
      player.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get(), 80,
          effect.getAmplifier(), false, false, true));
    }
  }

  /** Fracao carregada (0 a 1) de quem esta no modo CARREGAR, ou -1. */
  public static double chargeFraction(ServerPlayer player) {
    Active a = ACTIVE.get(player.getUUID());
    if (a == null || a.mode != CastMode.CHARGE) return -1;
    return Math.min(1.0, (player.level().getGameTime() - a.start) / (double) CHARGE_MAX_TICKS);
  }

  private static void fail(ServerPlayer player, String message) {
    if (message != null && !message.isEmpty()) {
      player.displayClientMessage(Component.translatable(message).withStyle(ChatFormatting.RED), true);
    }
  }

  /** Tecla de conjurar pressionada. */
  public static void press(ServerPlayer player, UUID spellId) {
    var grimoire = GrimoireAttachments.get(player).orElse(null);
    if (grimoire == null) return;
    Inscription inscription = grimoire.getInscription(spellId).orElse(null);
    if (inscription == null) return;
    CastMode mode = grimoire.prefs().mode(spellId);
    if (mode == CastMode.TOGGLE && !toggleable(inscription)) mode = CastMode.TAP;
    long now = player.level().getGameTime();
    Active current = ACTIVE.get(player.getUUID());
    if (current != null) {
      if (current.mode == CastMode.TOGGLE && current.spell.equals(spellId)) {
        stop(player, true);
        return;
      }
      if (current.mode == CastMode.CHARGE || current.mode == CastMode.HOLD) return; // ja canalizando
      stop(player, false); // alternar de outra magia: troca
    }
    PrecisionSnapshot precision = PrecisionSnapshot.of(inscription);
    switch (mode) {
      case TAP -> {
        var r = CastingService.tryCast(player, inscription, precision);
        if (!r.success()) fail(player, r.message());
      }
      case CHARGE -> {
        int cost = CastingService.manaCostOf(inscription, precision);
        if (GrimoireAttachments.getManaOrCreate(player).current() < Math.max(1, cost * 0.25)) {
          fail(player, "message.arcane.mana.insufficient");
          return;
        }
        ACTIVE.put(player.getUUID(), new Active(spellId, mode, now, player));
      }
      case HOLD -> {
        var r = CastingService.tryCast(player, inscription, precision);
        if (!r.success()) { fail(player, r.message()); return; }
        ACTIVE.put(player.getUUID(), new Active(spellId, mode, now, player));
      }
      case TOGGLE -> {
        var r = CastingService.tryCast(player, inscription, precision);
        if (!r.success()) { fail(player, r.message()); return; }
        ACTIVE.put(player.getUUID(), new Active(spellId, mode, now, player));
      }
    }
  }

  /** Tecla de conjurar solta. */
  public static void release(ServerPlayer player) {
    Active a = ACTIVE.get(player.getUUID());
    if (a == null) return;
    if (a.mode == CastMode.CHARGE) {
      fire(player, a);
    } else if (a.mode == CastMode.HOLD) {
      var g = GrimoireAttachments.get(player).flatMap(gr -> gr.getInscription(a.spell)).orElse(null);
      stop(player, g != null && toggleable(g)); // soltar encerra o voo e a telecinese
    }
  }

  private static void fire(ServerPlayer player, Active a) {
    ACTIVE.remove(player.getUUID());
    var grimoire = GrimoireAttachments.get(player).orElse(null);
    if (grimoire == null) return;
    Inscription inscription = grimoire.getInscription(a.spell).orElse(null);
    if (inscription == null) return;
    double fraction = Math.min(1.0, (player.level().getGameTime() - a.start) / (double) CHARGE_MAX_TICKS);
    double scale = 1.0 + (CHARGE_MAX_SCALE - 1.0) * fraction;
    var r = CastingService.cast(player, inscription, PrecisionSnapshot.of(inscription), scale, true);
    if (!r.success()) fail(player, r.message());
  }

  /** Cancela o canal do jogador; no modo ALTERNAR, tambem desliga o efeito continuo. */
  public static void stop(ServerPlayer player, boolean endEffects) {
    Active a = ACTIVE.remove(player.getUUID());
    if (a == null) return;
    if (endEffects && (a.mode == CastMode.TOGGLE || a.mode == CastMode.HOLD)) {
      player.removeEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get());
      Telekinesis.release(player.getUUID());
    }
  }

  public static void clear() { ACTIVE.clear(); }

  public static void tick(MinecraftServer server) {
    if (ACTIVE.isEmpty()) return;
    for (Iterator<Map.Entry<UUID, Active>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
      var entry = it.next();
      Active a = entry.getValue();
      ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
      if (player == null) player = a.owner; // jogador fora da lista (ex.: testes) continua valendo
      if (player == null || !player.isAlive() || player.hasDisconnected()) { it.remove(); continue; }
      var grimoire = GrimoireAttachments.get(player).orElse(null);
      Inscription inscription = grimoire == null ? null : grimoire.getInscription(a.spell).orElse(null);
      if (inscription == null) { it.remove(); continue; }
      long now = player.level().getGameTime();
      switch (a.mode) {
        case CHARGE -> {
          long ticks = now - a.start;
          if (ticks < CHARGE_MAX_TICKS) {
            // Drena 1,5x o custo ao longo da carga cheia; sem mana, lanca com o que carregou.
            double step = CastingService.manaCostOf(inscription, PrecisionSnapshot.of(inscription)) * 1.5 / CHARGE_MAX_TICKS;
            var mana = GrimoireAttachments.getManaOrCreate(player);
            if (mana.current() < step) { fire(player, a); }
            else mana.set((float) (mana.current() - step));
          }
        }
        case HOLD -> {
          if (flies(inscription)) {
            sustainFlight(player); // voar ja custa por tick: nao relanca a magia
          } else if (now >= a.next) {
            a.next = now + HOLD_INTERVAL_TICKS;
            var r = CastingService.tryCast(player, inscription, PrecisionSnapshot.of(inscription));
            if (!r.success()) { fail(player, r.message()); it.remove(); }
          }
        }
        case TOGGLE -> {
          // some sozinho quando o efeito acaba (Flutuar) para o proximo toque lancar de novo
          if (flies(inscription)) sustainFlight(player);
          boolean floating = player.hasEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get());
          boolean holding = Telekinesis.heldCount(player.getUUID()) > 0;
          if (now - a.start > 5 && !floating && !holding) it.remove();
        }
        default -> it.remove();
      }
    }
  }
}
