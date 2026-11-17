package com.arcane.restore;

import com.arcane.ArcaneMod;
import com.arcane.config.ArcaneConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Diario dos blocos que magias destruiram ou transformaram ha pouco (so em memoria, por dimensao), usado pelo glifo Restaurar.
 * Cada entrada guarda o estado de antes e o estado em que a magia deixou a posicao: so se restaura se a posicao ainda estiver
 * nesse estado, para nunca sobrescrever uma construcao feita depois. Escavar nao entra.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class BlockJournal {
  public record Entry(BlockPos pos, BlockState before, BlockState after) {}

  private static final Map<ResourceKey<Level>, JournalLog<Entry>> LOGS = new HashMap<>();

  private BlockJournal() {}

  private static long window() { return ArcaneConfig.RESTORE_WINDOW_SECONDS.get() * 20L; }

  private static JournalLog<Entry> log(ServerLevel level) {
    return LOGS.computeIfAbsent(level.dimension(), k -> new JournalLog<>());
  }

  /** Registra uma alteracao ja feita: {@code before} e o estado de antes; o de depois e o que esta no mundo agora. */
  public static void record(ServerLevel level, BlockPos pos, BlockState before) {
    if (!ArcaneConfig.RESTORE_ENABLED.get() || before.isAir()) return;
    var key = ForgeRegistries.BLOCKS.getKey(before.getBlock());
    if (key != null && key.getNamespace().equals(ArcaneMod.MODID)) return; // nunca os blocos do proprio mod (barreiras, tracos, sinais)
    BlockState after = level.getBlockState(pos);
    if (after == before) return;
    log(level).add(new Entry(pos.immutable(), before, after), level.getGameTime(), ArcaneConfig.RESTORE_JOURNAL_CAP.get(), window());
  }

  /**
   * Quebra um bloco por magia. Sem a config {@code blastDropsLoot} (padrao): nao solta item e entra no diario.
   * Com ela: solta item e NAO entra no diario (senao daria para pegar o loot e restaurar o bloco).
   */
  public static boolean destroy(ServerLevel level, BlockPos pos, Entity breaker) {
    if (ArcaneConfig.BLAST_DROPS_LOOT.get()) return level.destroyBlock(pos, true, breaker);
    BlockState before = level.getBlockState(pos);
    boolean ok = level.destroyBlock(pos, false, breaker);
    if (ok) record(level, pos, before);
    return ok;
  }

  /** Estado do diario da dimensao (para testes e depuracao). */
  public static int size(ServerLevel level) { return log(level).size(); }

  public static void clear(ServerLevel level) { log(level).clear(); }

  /**
   * Recompõe, da mais nova para a mais antiga (assim pilhas de mudancas na mesma posicao se desfazem na ordem certa), ate
   * {@code maxBlocks} blocos dentro do raio, onde a posicao ainda estiver como a magia a deixou. Devolve quantos voltaram.
   */
  public static int restore(ServerLevel level, Vec3 center, double radius, int maxBlocks) {
    JournalLog<Entry> log = log(level);
    long now = level.getGameTime();
    log.trim(now, ArcaneConfig.RESTORE_JOURNAL_CAP.get(), window());
    double r2 = radius * radius;
    List<JournalLog.Stamped<Entry>> used = new ArrayList<>();
    for (JournalLog.Stamped<Entry> s : log.live(now, window())) {
      if (used.size() >= maxBlocks) break;
      Entry e = s.value();
      if (Vec3.atCenterOf(e.pos()).distanceToSqr(center) > r2) continue;
      if (!level.isLoaded(e.pos())) continue;
      if (level.getBlockState(e.pos()) != e.after()) continue; // alguem construiu ali: nao mexe
      level.setBlock(e.pos(), e.before(), 3);
      used.add(s);
    }
    log.removeAll(used);
    return used.size();
  }

  @SubscribeEvent
  public static void stopped(ServerStoppedEvent event) { LOGS.clear(); }
}
