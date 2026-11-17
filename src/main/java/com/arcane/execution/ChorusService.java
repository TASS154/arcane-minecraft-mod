package com.arcane.execution;

import com.arcane.config.ArcaneConfig;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coro: magia marcada com o modificador Coro so dispara se N jogadores (N >= min) a conjurarem juntos.
 * O primeiro a conjurar abre uma sessao de curta duracao e o servidor desenha o circulo (so particulas); quem estiver dentro
 * do circulo com a MESMA magia e a conjurar ate o fim da janela entra. No fim, com participantes suficientes, a magia dispara
 * uma vez, com o iniciador como conjurador, mais forte por participante, e cada um paga sua cota de mana. Sem participantes
 * suficientes nao acontece nada e ninguem paga. Tudo e decidido no servidor; so existe estado enquanto ha uma sessao aberta.
 */
public final class ChorusService {
  private static final net.minecraft.resources.ResourceLocation CHORUS = new net.minecraft.resources.ResourceLocation("arcane", "chorus");

  private static final class Session {
    final UUID initiator;
    final Inscription spell;
    final String fingerprint;
    final ResourceKey<Level> dimension;
    final Vec3 center;
    final long end;
    final int min;
    final double scale;
    final Set<UUID> members = new LinkedHashSet<>();

    Session(UUID initiator, Inscription spell, ResourceKey<Level> dimension, Vec3 center, long end, int min, double scale) {
      this.initiator = initiator;
      this.spell = spell;
      this.fingerprint = spell.fingerprint();
      this.dimension = dimension;
      this.center = center;
      this.end = end;
      this.min = min;
      this.scale = scale;
    }
  }

  private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
  private static final ThreadLocal<Boolean> FIRING = new ThreadLocal<>();

  private ChorusService() {}

  public static boolean isChorus(Inscription inscription) {
    for (GlyphInstance g : inscription.glyphs()) if (CHORUS.equals(g.glyphId())) return true;
    return false;
  }

  public static int minOf(Inscription inscription) {
    for (GlyphInstance g : inscription.glyphs()) if (CHORUS.equals(g.glyphId())) return (int) Math.max(2, Math.min(4, Math.round(g.param("min", 2.0))));
    return 2;
  }

  /** Verdadeiro enquanto o servico dispara a magia de um coro (para o CastingService nao abrir outra sessao). */
  public static boolean firing() { return Boolean.TRUE.equals(FIRING.get()); }

  public static int activeSessions() { return SESSIONS.size(); }

  public static void clearAll() { SESSIONS.clear(); }

  private static void say(ServerPlayer p, String key, Object... args) {
    p.displayClientMessage(Component.translatable(key, args), true);
  }

  /** Chamado quando um jogador conjura uma magia de Coro (sem gastar mana agora): entra numa sessao ou abre uma. */
  public static ExecutionResult handle(ServerPlayer player, Inscription spell, double scale) {
    if (!ArcaneConfig.CHORUS_ENABLED.get()) return ExecutionResult.fail("message.arcane.chorus.disabled");
    ServerLevel level = player.serverLevel();
    long now = level.getGameTime();
    double radius = ArcaneConfig.CHORUS_RADIUS.get();
    String fp = spell.fingerprint();
    int max = ArcaneConfig.CHORUS_MAX_PARTICIPANTS.get();

    for (Session s : SESSIONS.values()) {
      if (s.dimension != level.dimension() || now > s.end || !s.fingerprint.equals(fp)) continue;
      if (player.position().distanceToSqr(s.center) > radius * radius) continue;
      if (s.members.contains(player.getUUID())) { say(player, "message.arcane.chorus.already"); return new ExecutionResult(true, "", 0); }
      if (s.members.size() >= max) { say(player, "message.arcane.chorus.full", max); return ExecutionResult.fail("message.arcane.chorus.full"); }
      s.members.add(player.getUUID());
      say(player, "message.arcane.chorus.joined", s.members.size(), s.min);
      ServerPlayer lead = playerIn(level, s.initiator);
      if (lead != null && lead != player) say(lead, "message.arcane.chorus.count", s.members.size(), s.min);
      return new ExecutionResult(true, "", 0);
    }

    Session own = SESSIONS.get(player.getUUID());
    if (own != null && now <= own.end) { say(player, "message.arcane.chorus.already"); return new ExecutionResult(true, "", 0); }
    Session s = new Session(player.getUUID(), spell, level.dimension(), player.position(), now + ArcaneConfig.CHORUS_WINDOW_TICKS.get(), minOf(spell), scale);
    s.members.add(player.getUUID());
    SESSIONS.put(player.getUUID(), s);
    say(player, "message.arcane.chorus.opened", s.min);
    drawCircle(level, s, radius);
    return new ExecutionResult(true, "", 0);
  }

  public static void tick(MinecraftServer server) {
    if (SESSIONS.isEmpty()) return;
    double radius = ArcaneConfig.CHORUS_RADIUS.get();
    for (Iterator<Session> it = SESSIONS.values().iterator(); it.hasNext(); ) {
      Session s = it.next();
      ServerLevel level = server.getLevel(s.dimension);
      if (level == null) { it.remove(); continue; }
      long now = level.getGameTime();
      if (now >= s.end) {
        it.remove();
        resolve(server, level, s, radius);
      } else if (now % 10 == 0) {
        drawCircle(level, s, radius);
      }
    }
  }

  private static void drawCircle(ServerLevel level, Session s, double radius) {
    int points = 28;
    for (int i = 0; i < points; i++) {
      double a = Math.PI * 2 * i / points;
      level.sendParticles(ParticleTypes.END_ROD, s.center.x + Math.cos(a) * radius, s.center.y + 0.1, s.center.z + Math.sin(a) * radius, 1, 0, 0.05, 0, 0.0);
    }
  }

  private static void resolve(MinecraftServer server, ServerLevel level, Session s, double radius) {
    List<ServerPlayer> in = new ArrayList<>();
    for (UUID id : s.members) {
      ServerPlayer p = playerIn(level, id);
      if (p == null || !p.isAlive()) continue;
      if (p.position().distanceToSqr(s.center) > (radius + 1.0) * (radius + 1.0)) continue; // saiu do circulo
      in.add(p);
    }
    ServerPlayer lead = playerIn(level, s.initiator);
    if (lead == null || !in.contains(lead)) { notifyAll(in, "message.arcane.chorus.failed", in.size(), s.min); return; }
    // o iniciador vai primeiro: se o teto cortar alguem, nunca e ele
    in.remove(lead);
    in.add(0, lead);
    int max = ArcaneConfig.CHORUS_MAX_PARTICIPANTS.get();
    while (in.size() > max) in.remove(in.size() - 1);

    int cost = CastingService.manaCostOf(s.spell, PrecisionSnapshot.of(s.spell));
    // Quem nao tem a cota sai; a cota sobe quando sai gente, entao repete ate estabilizar.
    boolean changed = true;
    while (changed && in.size() >= s.min) {
      changed = false;
      int share = ChorusMath.share(cost, in.size());
      for (Iterator<ServerPlayer> it = in.iterator(); it.hasNext(); ) {
        ServerPlayer p = it.next();
        if (GrimoireAttachments.getManaOrCreate(p).current() < share) {
          if (p == lead) { in.clear(); break; }
          say(p, "message.arcane.chorus.nomana");
          it.remove();
          changed = true;
        }
      }
    }
    if (in.size() < s.min) { notifyAll(in, "message.arcane.chorus.failed", in.size(), s.min); return; }

    int share = ChorusMath.share(cost, in.size());
    for (ServerPlayer p : in) GrimoireAttachments.getManaOrCreate(p).trySpend(share);
    double mult = ChorusMath.multiplier(in.size());
    ExecutionResult result;
    FIRING.set(true);
    try {
      result = CastingService.cast(lead, s.spell, PrecisionSnapshot.of(s.spell), s.scale * mult, true);
    } finally {
      FIRING.remove();
    }
    if (!result.success()) {
      for (ServerPlayer p : in) { var m = GrimoireAttachments.getManaOrCreate(p); m.set(m.current() + share); }
      notifyAll(in, "message.arcane.chorus.error", result.message());
      return;
    }
    level.sendParticles(ParticleTypes.END_ROD, s.center.x, s.center.y + 1.0, s.center.z, 40, radius * 0.4, 0.6, radius * 0.4, 0.05);
    notifyAll(in, "message.arcane.chorus.fired", in.size(), String.format(java.util.Locale.ROOT, "%.2f", mult));
  }

  /** O jogador da sessao, se ainda estiver na mesma dimensao (quem trocou de dimensao saiu do circulo). */
  private static ServerPlayer playerIn(ServerLevel level, UUID id) {
    return level.getPlayerByUUID(id) instanceof ServerPlayer sp ? sp : null;
  }

  private static void notifyAll(List<ServerPlayer> players, String key, Object... args) {
    for (ServerPlayer p : players) say(p, key, args);
  }
}
