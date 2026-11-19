package com.arcane.ritual;

import com.arcane.ArcaneMod;
import com.arcane.config.ArcaneConfig;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.S2CCompassDrawPacket;
import com.arcane.registry.ModItems;
import com.arcane.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Desenho do circulo pelo Compasso Arcano, ao longo do tempo (nao e instantaneo). O servidor poe os tracos aos poucos,
 * em varredura, no ritmo que o cliente anima (caneta e tinta flutuando). Nao se interrompe: so existe para o altar.
 * So custa algo enquanto ha um desenho em andamento; sem sessoes, o tick sai na primeira linha.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class CompassDrawing {
  private static final class Session {
    final ServerPlayer player;
    final ServerLevel level;
    final BlockPos altar;
    final long start;
    final int duration;
    final CompassPath path;
    int next = 0;
    int placed = 0;

    Session(ServerPlayer player, ServerLevel level, BlockPos altar, int radius, int duration) {
      this.player = player;
      this.level = level;
      this.altar = altar;
      this.start = level.getGameTime();
      this.duration = duration;
      this.path = CompassPath.of(radius);
    }
  }

  /** Fracao do tempo gasta antes do traco: subir ate o altar e mergulhar a caneta (o cliente usa o mesmo valor). */
  public static final double LEAD = 0.26;

  private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

  private CompassDrawing() {}

  /** Duracao do desenho em ticks (0 = instantaneo). */
  public static int durationTicks(int radius) {
    int per = ArcaneConfig.COMPASS_TICKS_PER_BLOCK.get();
    return per <= 0 ? 0 : Math.max(20, radius * per);
  }

  public static boolean isDrawing(UUID player) {
    return SESSIONS.containsKey(player);
  }

  /** Comeca o desenho. O chamador ja checou a tinta. Devolve false se o jogador ja esta desenhando. */
  public static boolean start(ServerPlayer player, ServerLevel level, BlockPos altar, int radius) {
    if (SESSIONS.containsKey(player.getUUID())) return false;
    int duration = durationTicks(radius);
    if (duration <= 0) {
      conclude(player, level, altar, RitualService.drawCircle(level, altar, radius));
      return true;
    }
    SESSIONS.put(player.getUUID(), new Session(player, level, altar, radius, duration));
    level.playSound(null, altar, ModSounds.PEN_STROKE.get(), SoundSource.BLOCKS, 1.0f, 0.6f);
    PacketDistributor.TargetPoint near = new PacketDistributor.TargetPoint(altar.getX() + 0.5, altar.getY() + 0.5, altar.getZ() + 0.5, 128.0, level.dimension());
    ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> near), new S2CCompassDrawPacket(altar, radius, duration, player.getId()));
    return true;
  }

  private static void conclude(ServerPlayer player, ServerLevel level, BlockPos altar, int placed) {
    int cost = Math.max(1, (int) Math.ceil(placed / 16.0));
    boolean creative = player != null && player.getAbilities().instabuild;
    if (player != null && !creative && placed > 0) takeInk(player, cost);
    level.playSound(null, altar, ModSounds.PEN_STROKE.get(), SoundSource.BLOCKS, 1.0f, 0.8f);
    if (player != null) {
      player.displayClientMessage(Component.translatable("message.arcane.compass.drawn", placed, creative ? 0 : cost)
          .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }
  }

  public static int countInk(ServerPlayer player) {
    int total = 0;
    for (ItemStack s : player.getInventory().items) if (s.is(ModItems.COMMON_INK.get())) total += s.getCount();
    return total;
  }

  private static void takeInk(ServerPlayer player, int amount) {
    int left = amount;
    for (ItemStack s : player.getInventory().items) {
      if (left <= 0) break;
      if (s.is(ModItems.COMMON_INK.get())) {
        int n = Math.min(left, s.getCount());
        s.shrink(n);
        left -= n;
      }
    }
  }

  /** Poe as celulas devidas ate o instante atual. Devolve true quando terminou. */
  private static boolean advance(Session s, boolean force) {
    long elapsed = s.level.getGameTime() - s.start;
    int size = s.path.cells.size();
    int target;
    if (force || elapsed >= s.duration) {
      target = size;
    } else {
      // So comeca a por tracos depois da caneta subir, mergulhar na tinta e chegar ao primeiro traco; fica poucos ticks atras da caneta.
      double lead = LEAD * s.duration + 3;
      double f = (elapsed - lead) / (s.duration - lead);
      target = f <= 0 ? 0 : s.path.reached(f * s.path.total);
    }
    while (s.next < target) {
      if (RitualService.drawCell(s.level, s.altar, s.path.cells.get(s.next))) s.placed++;
      s.next++;
    }
    return s.next >= size;
  }

  @SubscribeEvent
  public static void onTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || SESSIONS.isEmpty()) return;
    Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
    List<Runnable> after = new ArrayList<>();
    while (it.hasNext()) {
      Map.Entry<UUID, Session> e = it.next();
      Session s = e.getValue();
      if (!s.level.hasChunkAt(s.altar)) continue;
      boolean done = advance(s, false);
      if ((s.level.getGameTime() - s.start) % 8 == 0 && !done) {
        s.level.playSound(null, s.altar, ModSounds.PEN_STROKE.get(), SoundSource.BLOCKS, 0.5f, 0.9f + s.level.random.nextFloat() * 0.3f);
      }
      if (done) {
        it.remove();
        ServerPlayer online = s.level.getServer().getPlayerList().getPlayer(e.getKey());
        ServerPlayer p = online != null ? online : s.player;
        after.add(() -> conclude(p, s.level, s.altar, s.placed));
      }
    }
    after.forEach(Runnable::run);
  }

  /** Quem sai do jogo no meio: o circulo e terminado na hora (nao fica pela metade). */
  @SubscribeEvent
  public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    Session s = SESSIONS.remove(player.getUUID());
    if (s == null) return;
    advance(s, true);
    conclude(player, s.level, s.altar, s.placed);
  }

  @SubscribeEvent
  public static void onStopped(ServerStoppedEvent event) {
    SESSIONS.clear();
  }
}
