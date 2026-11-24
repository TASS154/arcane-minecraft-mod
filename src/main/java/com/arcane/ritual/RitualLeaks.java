package com.arcane.ritual;

import com.arcane.ArcaneMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Mana vazando: qualquer circulo de ritual de raio 16 ou mais, pelo simples fato de existir (completo em volta de um
 * altar), deixa sinais publicos. Quanto maior, mais fortes e mais longe:
 * <ul>
 *   <li>particulas azuis subindo dentro da area (densidade cresce com o raio);</li>
 *   <li>uma coluna azul fraca que sobe do altar, vista de longe (raio x 8 blocos);</li>
 *   <li>um zumbido grave a cada 10 s, ouvido mais longe quanto maior o circulo;</li>
 *   <li>quem esta a ate raio x 10 blocos sente a direcao ("mana vazando a nordeste").</li>
 * </ul>
 * Os altares carregados se registram sozinhos; o circulo e medido de novo a cada 10 s (desfazer o desenho apaga os sinais).
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class RitualLeaks {
  public static final int MIN_RADIUS = 16;
  private static final int REMEASURE = 200;
  private static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.35f, 0.65f, 1.0f), 1.4f);

  private record Key(ResourceKey<Level> dim, long pos) {}

  private static final class Leak {
    int radius = -1;
    long measuredAt = -1; // -1 = ainda nao medido
  }

  private static final Map<Key, Leak> ALTARS = new HashMap<>();

  private RitualLeaks() {}

  public static void register(Level level, BlockPos pos) {
    if (level instanceof ServerLevel) ALTARS.putIfAbsent(new Key(level.dimension(), pos.asLong()), new Leak());
  }

  public static void unregister(Level level, BlockPos pos) {
    ALTARS.remove(new Key(level.dimension(), pos.asLong()));
  }

  /** Raio medido pelo servico (sem medir de novo): -1 se ainda nao mediu. */
  public static int measuredRadius(Level level, BlockPos pos) {
    Leak l = ALTARS.get(new Key(level.dimension(), pos.asLong()));
    return l == null ? -1 : l.radius;
  }

  /** Altares desta dimensao cujo circulo (raio 16+) esta vazando mana agora. */
  public static java.util.List<BlockPos> leakingAltars(Level level) {
    java.util.List<BlockPos> out = new java.util.ArrayList<>();
    for (var e : ALTARS.entrySet()) {
      if (e.getKey().dim() == level.dimension() && e.getValue().radius >= MIN_RADIUS) out.add(BlockPos.of(e.getKey().pos()));
    }
    return out;
  }

  public static boolean registered(Level level, BlockPos pos) { return ALTARS.containsKey(new Key(level.dimension(), pos.asLong())); }

  /** Raio do circulo que esta vazando neste altar (0 se nao vaza). Mede na hora. */
  public static int leakingRadius(ServerLevel level, BlockPos altar) {
    var report = RitualService.evaluate(level, altar);
    return report.level() != RitualLevel.NONE && report.radius() >= MIN_RADIUS ? report.radius() : 0;
  }

  /** Alcance (blocos) de cada sinal para um circulo de raio r. */
  public static double soundReach(int r) { return r * 6.0; }
  public static double beamReach(int r) { return r * 8.0; }
  public static double senseReach(int r) { return r * 10.0; }

  public static void tick(MinecraftServer server) {
    if (ALTARS.isEmpty()) return;
    long t = server.getTickCount();
    for (var e : ALTARS.entrySet()) {
      ServerLevel level = server.getLevel(e.getKey().dim());
      if (level == null) continue;
      BlockPos altar = BlockPos.of(e.getKey().pos());
      if (!level.isLoaded(altar)) continue;
      Leak leak = e.getValue();
      // espalha as medicoes para nao medir todos os altares no mesmo tick
      boolean first = leak.measuredAt < 0;
      if (first ? ((altar.hashCode() ^ t) & 15) == 0 : t - leak.measuredAt >= REMEASURE) {
        leak.radius = leakingRadius(level, altar);
        leak.measuredAt = t;
      }
      if (leak.radius >= MIN_RADIUS) emit(level, altar, leak.radius, t);
    }
  }

  private static void emit(ServerLevel level, BlockPos altar, int r, long t) {
    var rnd = level.random;
    if (t % 5 == 0) {
      int n = Math.min(60, r / 2);
      for (ServerPlayer p : level.players()) {
        if (p.position().distanceTo(altar.getCenter()) > r + 32) continue;
        for (int i = 0; i < n; i++) {
          double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * r;
          double x = altar.getX() + 0.5 + Math.cos(a) * d, z = altar.getZ() + 0.5 + Math.sin(a) * d;
          double y = altar.getY() + 0.3 + rnd.nextDouble() * 2.5;
          level.sendParticles(p, BLUE, false, x, y, z, 1, 0.05, 0.3, 0.05, 0.02);
        }
      }
    }
    if (t % 10 == 0) {
      double reach = beamReach(r);
      for (ServerPlayer p : level.players()) {
        if (p.position().distanceTo(altar.getCenter()) > reach) continue;
        for (int h = 0; h < Math.min(48, r); h += 2) {
          level.sendParticles(p, ParticleTypes.SOUL_FIRE_FLAME, true, altar.getX() + 0.5, altar.getY() + 2 + h + rnd.nextDouble() * 2,
              altar.getZ() + 0.5, 1, 0.15, 0.4, 0.15, 0.0);
        }
      }
    }
    if (t % 200 == 0) {
      float volume = (float) (soundReach(r) / 16.0);
      level.playSound(null, altar, SoundEvents.BEACON_AMBIENT, SoundSource.AMBIENT, volume, 0.5f);
      level.playSound(null, altar, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, volume, 0.6f);
      for (ServerPlayer p : level.players()) {
        double dist = p.position().distanceTo(altar.getCenter());
        if (dist <= r || dist > senseReach(r)) continue;
        p.displayClientMessage(Component.translatable("message.arcane.ritual.leak",
            Component.translatable("direction.arcane." + direction(p.blockPosition(), altar))).withStyle(ChatFormatting.AQUA), true);
      }
    }
  }

  /** Direcao (8 rumos) de quem esta em {@code from} ate o altar (norte = -Z). */
  public static String direction(BlockPos from, BlockPos to) {
    double ang = Math.toDegrees(Math.atan2(to.getX() - from.getX(), -(to.getZ() - from.getZ())));
    String[] names = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
    return names[(int) Math.floorMod(Math.round(ang / 45.0), 8L)];
  }

  @SubscribeEvent
  public static void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
    if (server != null) tick(server);
  }

  @SubscribeEvent
  public static void onStopped(ServerStoppedEvent event) { ALTARS.clear(); }
}
