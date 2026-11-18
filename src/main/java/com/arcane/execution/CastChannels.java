package com.arcane.execution;

import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.grimoire.SpellPrefs.CastMode;
import com.arcane.inscription.CompiledPlanCache;
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
 * TOQUE: lanca uma vez. CARREGAR: segurar faz surgir as runas da forma da magia e drena mana aos poucos, sem limite de
 * tempo (so a mana e o teto chargeMaxScale limitam); ao soltar, lanca uma vez, mais forte quanto mais carregou.
 * SEGURAR: conjura UMA vez (custo normal) e mantem: enquanto a tecla estiver pressionada a magia continua agindo, em
 * silencio, e a mana drena aos poucos (holdDrainPerSecond); soltar encerra.
 * ALTERNAR: so para magias de efeito continuo (Flutuar, Telecinese): lanca e, no segundo toque, cancela.
 * Tudo e decidido aqui no servidor; o cliente so avisa "pressionou" e "soltou".
 */
public final class CastChannels {
  /** Drena 1,5x o custo por 60 ticks de carga (a mesma taxa de antes); a potencia sobe 2 a cada 1,5x o custo gasto. */
  public static final double CHARGE_DRAIN_PER_TICK = 1.5 / 60.0;
  public static final int HOLD_INTERVAL_TICKS = 10;
  public static final int FX_INTERVAL_TICKS = 5;
  /** Magias com algum destes efeitos aceitam o modo ALTERNAR. */
  public static final Set<String> TOGGLE_GLYPHS = Set.of("float", "telekinesis");

  private static final class Active {
    final UUID spell;
    ServerPlayer owner;
    final CastMode mode;
    final long start;
    long next;
    double drained;
    double cost = 1;
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

  /** Potencia de uma carga que gastou {@code drained} de mana numa magia de custo {@code cost}. */
  public static double chargeScale(double drained, double cost) {
    double max = com.arcane.config.ArcaneConfig.CHARGE_MAX_SCALE.get();
    return Math.min(max, 1.0 + (2.0 / 1.5) * drained / Math.max(1.0, cost));
  }

  /** Fracao carregada (0 a 1) de quem esta no modo CARREGAR, ou -1. */
  public static double chargeFraction(ServerPlayer player) {
    Active a = ACTIVE.get(player.getUUID());
    if (a == null || a.mode != CastMode.CHARGE) return -1;
    double max = com.arcane.config.ArcaneConfig.CHARGE_MAX_SCALE.get();
    return (chargeScale(a.drained, a.cost) - 1.0) / Math.max(0.01, max - 1.0);
  }

  /** Forma da magia para as runas (ver S2CChannelPacket). */
  private static int shapeOf(Inscription inscription) {
    String form = DeliverySpec.fromPlan(CompiledPlanCache.getOrCompile(inscription).delivery()).handler().getPath();
    return switch (form) {
      case "self" -> com.arcane.network.packet.S2CChannelPacket.SELF;
      case "aura" -> com.arcane.network.packet.S2CChannelPacket.AURA;
      case "wave" -> com.arcane.network.packet.S2CChannelPacket.WAVE;
      case "pillar" -> com.arcane.network.packet.S2CChannelPacket.PILLAR;
      default -> com.arcane.network.packet.S2CChannelPacket.BEAM;
    };
  }

  private static int colorOf(Inscription inscription) {
    return SpellElement.of(CompiledPlanCache.getOrCompile(inscription)).color();
  }

  private static void sendFx(ServerPlayer player, Inscription inscription, int state, double progress) {
    com.arcane.network.ModNetwork.sendChannel(player, state, (float) progress, shapeOf(inscription), colorOf(inscription));
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
        Active charging = new Active(spellId, mode, now, player);
        charging.cost = Math.max(1, cost);
        ACTIVE.put(player.getUUID(), charging);
        sendFx(player, inscription, com.arcane.network.packet.S2CChannelPacket.CHARGING, 0.0);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), com.arcane.registry.ModSounds.CHARGE_START.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);
      }
      case HOLD -> {
        var r = CastingService.tryCast(player, inscription, precision);
        if (!r.success()) { fail(player, r.message()); return; }
        Active holding = new Active(spellId, mode, now, player);
        holding.cost = Math.max(1, CastingService.manaCostOf(inscription, precision));
        ACTIVE.put(player.getUUID(), holding);
        sendFx(player, inscription, com.arcane.network.packet.S2CChannelPacket.HOLDING, 1.0);
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
    double scale = chargeScale(a.drained, a.cost);
    com.arcane.network.ModNetwork.sendChannel(player, com.arcane.network.packet.S2CChannelPacket.OFF, 0f, 0, 0);
    var r = CastingService.cast(player, inscription, PrecisionSnapshot.of(inscription), scale, true);
    if (!r.success()) fail(player, r.message());
    else player.level().playSound(null, player.getX(), player.getY(), player.getZ(), com.arcane.registry.ModSounds.CHARGE_RELEASE.get(), net.minecraft.sounds.SoundSource.PLAYERS, (float) Math.min(1.3, 0.6 + 0.15 * scale), (float) Math.max(0.7, 1.15 - 0.06 * scale));
  }

  /** Cancela o canal do jogador; no modo ALTERNAR, tambem desliga o efeito continuo. */
  public static void stop(ServerPlayer player, boolean endEffects) {
    Active a = ACTIVE.remove(player.getUUID());
    if (a == null) return;
    com.arcane.network.ModNetwork.sendChannel(player, com.arcane.network.packet.S2CChannelPacket.OFF, 0f, 0, 0);
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
          // Sem limite de tempo: a carga so para quando a mana acaba ou a potencia chega ao teto. Nao dispara sozinha:
          // as runas ficam la, prontas, ate o jogador soltar.
          var mana = GrimoireAttachments.getManaOrCreate(player);
          double step = a.cost * CHARGE_DRAIN_PER_TICK;
          boolean capped = chargeScale(a.drained, a.cost) >= com.arcane.config.ArcaneConfig.CHARGE_MAX_SCALE.get() - 1.0e-9;
          if (!capped && mana.current() >= step) {
            mana.set((float) (mana.current() - step));
            a.drained += step;
          }
          if ((now - a.start) % FX_INTERVAL_TICKS == 0) sendFx(player, inscription, com.arcane.network.packet.S2CChannelPacket.CHARGING, chargeFraction(player));
        }
        case HOLD -> {
          if (flies(inscription)) {
            sustainFlight(player); // voar ja custa por tick: nao relanca a magia
          } else {
            // Conjurou uma vez; agora so mantem: drena aos poucos e reaplica o efeito em silencio.
            var mana = GrimoireAttachments.getManaOrCreate(player);
            double step = a.cost * com.arcane.config.ArcaneConfig.HOLD_DRAIN_PER_SECOND.get() / 20.0;
            if (mana.current() < step) {
              fail(player, "message.arcane.mana.insufficient");
              stop(player, false);
              continue;
            }
            mana.set((float) (mana.current() - step));
            if (now >= a.next) {
              a.next = now + HOLD_INTERVAL_TICKS;
              var r = CastingService.cast(player, inscription, PrecisionSnapshot.of(inscription), 1.0, true, true);
              if (!r.success()) { fail(player, r.message()); stop(player, false); continue; }
            }
            if ((now - a.start) % FX_INTERVAL_TICKS == 0) sendFx(player, inscription, com.arcane.network.packet.S2CChannelPacket.HOLDING, 1.0);
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
