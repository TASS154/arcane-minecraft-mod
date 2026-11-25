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
    int pulses;
    /** Pilar mantido: o ponto onde ele nasceu; fica ali (a mira nao o arrasta). */
    net.minecraft.world.phys.Vec3 anchor;
    double drained;
    double cost = 1;
    /** Blocos de barreira da magia mantida: renovados enquanto segura e desfeitos ao soltar. */
    final java.util.Set<net.minecraft.core.BlockPos> shell = new java.util.HashSet<>();
    Active(UUID spell, CastMode mode, long start, ServerPlayer owner) { this.owner = owner; this.spell = spell; this.mode = mode; this.start = start; this.next = start + HOLD_INTERVAL_TICKS; }
  }

  /** Magias ALTERNAR ligadas: vivem a parte do canal (carregar/segurar), entao conjurar outra magia nao as desliga. */
  private static final class Toggle {
    final UUID spell;
    final ServerPlayer owner;
    final long start;
    Toggle(UUID spell, ServerPlayer owner, long start) { this.spell = spell; this.owner = owner; this.start = start; }
  }

  private static final Map<UUID, Active> ACTIVE = new ConcurrentHashMap<>();
  /** A magia mantida que esta sendo conjurada agora (so durante a chamada de cast): ela le daqui o modo "manter". */
  private static final ThreadLocal<Active> HOLDING_NOW = new ThreadLocal<>();

  /**
   * SEGURAR de verdade: enquanto a tecla esta apertada a magia nao "repete", ela se MANTEM. Efeitos com duracao so duram
   * ate o proximo pulso (acabam logo ao soltar) e a casca de uma Barreira e renovada e desfeita ao soltar.
   * Retorna os ticks de duracao maxima dos efeitos mantidos (0 = nao e uma magia mantida).
   */
  public static int holdTicks() { return HOLDING_NOW.get() == null ? 0 : HOLD_INTERVAL_TICKS + 6; }

  /** A barreira da magia mantida registra aqui cada bloco da casca. */
  public static void holdShell(net.minecraft.core.BlockPos pos) {
    Active a = HOLDING_NOW.get();
    if (a != null) a.shell.add(pos.immutable());
  }

  private static ExecutionResult castHeld(ServerPlayer player, Inscription inscription, Active a, boolean silent) {
    HOLDING_NOW.set(a);
    try {
      return CastingService.cast(player, inscription, PrecisionSnapshot.of(inscription), 1.0, silent, silent);
    } finally {
      HOLDING_NOW.remove();
      a.pulses++;
    }
  }

  /** Esta conjuracao e a repeticao silenciosa de uma magia mantida (nao a primeira)? Fx e som nao se repetem. */
  public static boolean holdPulse() {
    Active a = HOLDING_NOW.get();
    return a != null && a.pulses > 0;
  }

  /** Ponto onde o pilar mantido deste jogador esta fixo, ou null. */
  public static net.minecraft.world.phys.Vec3 heldAnchor(UUID player) {
    Active a = ACTIVE.get(player);
    return a == null ? null : a.anchor;
  }

  /** Magia mantida em andamento (primeira conjuracao ou repeticao)? */
  public static boolean holding() { return HOLDING_NOW.get() != null; }

  /**
   * Pilar mantido: a primeira conjuracao registra onde a coluna nasceu e as repeticoes devolvem sempre esse ponto, entao
   * mirar em outro lugar nao a move. Fora do modo Segurar devolve {@code fresh} como veio.
   */
  public static net.minecraft.world.phys.Vec3 lockAnchor(net.minecraft.world.phys.Vec3 fresh) {
    Active a = HOLDING_NOW.get();
    if (a == null) return fresh;
    if (a.anchor == null) a.anchor = fresh;
    return a.anchor;
  }

  /** Raio REAL (em blocos) que a area da magia teria agora; 0 se a magia nao tem uma forma de area. */
  public static float areaRadius(ServerPlayer player, Inscription inscription, double scale) {
    var plan = CompiledPlanCache.getOrCompile(inscription);
    DeliverySpec spec = DeliverySpec.fromPlan(plan.delivery());
    double base = switch (spec.handler().getPath()) {
      case "aura" -> spec.parameters().getOrDefault("radius", 5.0);
      case "pillar" -> spec.parameters().getOrDefault("radius", 2.0);
      case "barrier" -> spec.parameters().getOrDefault("radius", 3.0);
      case "wave" -> spec.range();
      default -> 0.0;
    };
    if (base <= 0.0) return 0f;
    var environment = com.arcane.ambient.AmbientFieldResolver.resolve((net.minecraft.server.level.ServerLevel) player.level(), player.blockPosition());
    double crystal = ExecutionContext.crystalFactor(environment.potencyMultiplier());
    double factor = crystal * Math.max(1.0, scale) * AreaDial.factorFor(player.getUUID(), plan);
    return (float) Math.min(64.0, Math.max(1.0, base * factor));
  }

  /** Desfaz a casca de barreira da magia mantida (com faiscas). */
  private static void releaseShell(ServerPlayer player, Active a) {
    if (a.shell.isEmpty() || !(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
    var barrier = com.arcane.registry.ModBlocks.ARCANE_BARRIER.get();
    com.arcane.fx.Fx.shellSparks(level, player, new java.util.ArrayList<>(a.shell), com.arcane.execution.SpellElement.ARCANE);
    for (var pos : a.shell) {
      if (level.getBlockState(pos).is(barrier)) level.removeBlock(pos, false);
    }
    a.shell.clear();
  }
  private static final Map<UUID, Toggle> TOGGLES = new ConcurrentHashMap<>();

  private CastChannels() {}

  public static boolean toggleable(Inscription inscription) {
    return inscription.glyphs().stream().anyMatch(g -> TOGGLE_GLYPHS.contains(g.glyphId().getPath()));
  }

  public static boolean isActive(UUID player) { return ACTIVE.containsKey(player) || TOGGLES.containsKey(player); }

  /** O jogador tem alguma magia ALTERNAR ligada? */
  public static boolean hasToggle(UUID player) { return TOGGLES.containsKey(player); }

  /** Desliga a magia ALTERNAR do jogador (voo e telecinese terminam). */
  public static void stopToggle(ServerPlayer player) {
    if (TOGGLES.remove(player.getUUID()) == null) return;
    player.removeEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get());
    Telekinesis.release(player.getUUID());
  }

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
    double scale = 1.0 + (2.0 / 1.5) * drained / Math.max(1.0, cost);
    return max <= 0 ? scale : Math.min(max, scale);
  }

  /** Fracao carregada (0 a 1) de quem esta no modo CARREGAR, ou -1. */
  public static double chargeFraction(ServerPlayer player) {
    Active a = ACTIVE.get(player.getUUID());
    if (a == null || a.mode != CastMode.CHARGE) return -1;
    double max = com.arcane.config.ArcaneConfig.CHARGE_MAX_SCALE.get();
    if (max <= 0) max = 20.0; // sem teto: a barra usa 20x como referencia
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
    sendFx(player, inscription, state, progress, 1.0, null);
  }

  /** As runas mostram a area real: o servidor manda o raio que a magia teria agora (carga e regulador incluidos). */
  private static void sendFx(ServerPlayer player, Inscription inscription, int state, double progress, double scale, net.minecraft.world.phys.Vec3 anchor) {
    com.arcane.network.ModNetwork.sendChannel(player, state, (float) progress, shapeOf(inscription), colorOf(inscription),
        areaRadius(player, inscription, scale), anchor);
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
    String blocked = com.arcane.grimoire.GrimoireDesk.blockedReason(player);
    if (blocked != null) {
      player.displayClientMessage(net.minecraft.network.chat.Component.translatable(blocked).withStyle(net.minecraft.ChatFormatting.GRAY), true);
      return;
    }
    Inscription inscription = grimoire.getInscription(spellId).orElse(null);
    if (inscription == null) return;
    CastMode mode = grimoire.prefs().mode(spellId);
    if (mode == CastMode.TOGGLE && !toggleable(inscription)) mode = CastMode.TAP;
    long now = player.level().getGameTime();
    // Apertar de novo a magia ALTERNAR que ja esta ligada a desliga (so isso, ou a mana acabar, a desliga).
    Toggle on = TOGGLES.get(player.getUUID());
    if (mode == CastMode.TOGGLE && on != null && on.spell.equals(spellId)) {
      stopToggle(player);
      return;
    }
    Active current = ACTIVE.get(player.getUUID());
    if (current != null) return; // ja canalizando (carregar ou segurar)
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
        Active holding = new Active(spellId, mode, now, player);
        holding.cost = Math.max(1, CastingService.manaCostOf(inscription, precision));
        var r = castHeld(player, inscription, holding, false);
        if (!r.success()) { fail(player, r.message()); return; }
        ACTIVE.put(player.getUUID(), holding);
        sendFx(player, inscription, com.arcane.network.packet.S2CChannelPacket.HOLDING, 1.0, 1.0, holding.anchor);
      }
      case TOGGLE -> {
        var r = CastingService.tryCast(player, inscription, precision);
        if (!r.success()) { fail(player, r.message()); return; }
        if (on != null) stopToggle(player); // outra magia ALTERNAR: so uma por vez (voo e telecinese brigam pelo mesmo corpo)
        TOGGLES.put(player.getUUID(), new Toggle(spellId, player, now));
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
    if (!r.success()) { fail(player, r.message()); return; }
    if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
      var el = SpellElement.of(CompiledPlanCache.getOrCompile(inscription));
      com.arcane.network.ModNetwork.sendShape(sl, com.arcane.network.packet.S2CShapeFxPacket.AURA, player.position().add(0, 0.1, 0), net.minecraft.world.phys.Vec3.ZERO,
          (float) Math.min(8.0, 1.5 + scale), el.color(), el.ordinal(), 14);
    }
    if (r.success()) player.level().playSound(null, player.getX(), player.getY(), player.getZ(), com.arcane.registry.ModSounds.CHARGE_RELEASE.get(), net.minecraft.sounds.SoundSource.PLAYERS, (float) Math.min(1.3, 0.6 + 0.15 * scale), (float) Math.max(0.7, 1.15 - 0.06 * scale));
  }

  /** Cancela o canal do jogador; no modo ALTERNAR, tambem desliga o efeito continuo. */
  public static void stop(ServerPlayer player, boolean endEffects) {
    Active a = ACTIVE.remove(player.getUUID());
    if (a == null) return;
    releaseShell(player, a);
    com.arcane.network.ModNetwork.sendChannel(player, com.arcane.network.packet.S2CChannelPacket.OFF, 0f, 0, 0);
    if (endEffects && a.mode == CastMode.HOLD) {
      player.removeEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get());
      Telekinesis.release(player.getUUID());
    }
  }

  public static void clear() { ACTIVE.clear(); TOGGLES.clear(); }

  /** Mantem as magias ALTERNAR ligadas; elas acabam quando o efeito acaba (mana zerada derruba o voo, ver FlightService). */
  private static void tickToggles(MinecraftServer server) {
    for (Iterator<Map.Entry<UUID, Toggle>> it = TOGGLES.entrySet().iterator(); it.hasNext(); ) {
      var entry = it.next();
      Toggle t = entry.getValue();
      ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
      if (player == null) player = t.owner;
      if (player == null || !player.isAlive() || player.hasDisconnected()) { it.remove(); continue; }
      var grimoire = GrimoireAttachments.get(player).orElse(null);
      Inscription inscription = grimoire == null ? null : grimoire.getInscription(t.spell).orElse(null);
      if (inscription == null) { it.remove(); continue; }
      long now = player.level().getGameTime();
      if (flies(inscription)) sustainFlight(player);
      boolean floating = player.hasEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get());
      boolean holding = Telekinesis.heldCount(player.getUUID()) > 0;
      if (now - t.start > 5 && !floating && !holding) it.remove(); // acabou sozinho (sem mana, solto, etc.)
    }
  }

  public static void tick(MinecraftServer server) {
    tickToggles(server);
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
          double chargeCap = com.arcane.config.ArcaneConfig.CHARGE_MAX_SCALE.get();
          boolean capped = chargeCap > 0 && chargeScale(a.drained, a.cost) >= chargeCap - 1.0e-9;
          if (!capped && mana.current() >= step) {
            mana.set((float) (mana.current() - step));
            a.drained += step;
          }
          if ((now - a.start) % FX_INTERVAL_TICKS == 0) sendFx(player, inscription, com.arcane.network.packet.S2CChannelPacket.CHARGING, chargeFraction(player), chargeScale(a.drained, a.cost), null);
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
              var r = castHeld(player, inscription, a, true);
              if (!r.success()) { fail(player, r.message()); stop(player, false); continue; }
            }
            if ((now - a.start) % FX_INTERVAL_TICKS == 0) sendFx(player, inscription, com.arcane.network.packet.S2CChannelPacket.HOLDING, 1.0, 1.0, a.anchor);
          }
        }
        default -> it.remove();
      }
    }
  }
}
