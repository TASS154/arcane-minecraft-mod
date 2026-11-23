package com.arcane.execution;

import com.arcane.ArcaneMod;
import com.arcane.config.ArcaneConfig;
import com.arcane.restore.BlockJournal;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Fila de blocos de crateras grandes. Uma explosao enorme tira milhoes de blocos; fazer tudo no mesmo tick trava o
 * servidor. Aqui cada tick remove no maximo craterBlocksPerTick blocos e o resto fica para os proximos (a cratera
 * "abre" em poucos segundos). Os blocos entram no diario do Restaurar como nas explosoes pequenas.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class CraterQueue {
  private static final Map<ServerLevel, ArrayDeque<LongArrayList>> PENDING = new HashMap<>();
  private static final Map<ServerLevel, int[]> CURSOR = new HashMap<>();

  private CraterQueue() {}

  /** Entrega a cratera. Remove ja o que couber no orcamento deste tick e deixa o resto para os proximos. */
  public static void submit(ServerLevel level, LongArrayList blocks) {
    if (blocks.isEmpty()) return;
    PENDING.computeIfAbsent(level, l -> new ArrayDeque<>()).add(blocks);
    if (!CURSOR.containsKey(level)) CURSOR.put(level, new int[] {0});
    process(level, ArcaneConfig.CRATER_BLOCKS_PER_TICK.get());
  }

  public static int pending(ServerLevel level) {
    var q = PENDING.get(level);
    if (q == null) return 0;
    int n = 0;
    for (LongArrayList l : q) n += l.size();
    return n - (CURSOR.getOrDefault(level, new int[] {0})[0]);
  }

  private static void process(ServerLevel level, int budget) {
    var queue = PENDING.get(level);
    if (queue == null) return;
    int[] cursor = CURSOR.computeIfAbsent(level, l -> new int[] {0});
    while (budget > 0 && !queue.isEmpty()) {
      LongArrayList head = queue.peek();
      while (budget > 0 && cursor[0] < head.size()) {
        BlockPos pos = BlockPos.of(head.getLong(cursor[0]++));
        BlockState before = level.getBlockState(pos);
        if (before.isAir()) continue;
        level.removeBlock(pos, false);
        BlockJournal.record(level, pos, before);
        budget--;
      }
      if (cursor[0] >= head.size()) {
        queue.poll();
        cursor[0] = 0;
      }
    }
    if (queue.isEmpty()) {
      PENDING.remove(level);
      CURSOR.remove(level);
    }
  }

  @SubscribeEvent
  public static void onTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
    for (ServerLevel level : new java.util.ArrayList<>(PENDING.keySet())) process(level, ArcaneConfig.CRATER_BLOCKS_PER_TICK.get());
  }

  @SubscribeEvent
  public static void onStopped(ServerStoppedEvent event) {
    PENDING.clear();
    CURSOR.clear();
  }
}
