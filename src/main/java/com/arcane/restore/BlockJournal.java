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
  /** Blocos ja escolhidos por um Restaurar, esperando a vez de voltar (alguns por tick, do mais novo para o mais antigo). */
  private static final Map<ResourceKey<Level>, java.util.ArrayDeque<Entry>> PENDING = new HashMap<>();

  /** So para testes: forca o valor de blastDropsLoot sem mexer no arquivo de config (que o Forge recarrega do disco, com corrida). */
  private static volatile Boolean lootOverride;

  public static void overrideLootForTests(Boolean value) { lootOverride = value; }

  /** Valor efetivo de blastDropsLoot: o da config, salvo override de teste. */
  public static boolean dropsLoot() {
    Boolean o = lootOverride;
    return o != null ? o : ArcaneConfig.BLAST_DROPS_LOOT.get();
  }

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
    if (dropsLoot()) return level.destroyBlock(pos, true, breaker);
    BlockState before = level.getBlockState(pos);
    boolean ok = level.destroyBlock(pos, false, breaker);
    if (ok) record(level, pos, before);
    return ok;
  }

  /** Estado do diario da dimensao (para testes e depuracao). */
  public static int size(ServerLevel level) { return log(level).size(); }

  public static void clear(ServerLevel level) { log(level).clear(); PENDING.remove(level.dimension()); }

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
      used.add(s);
    }
    log.removeAll(used);
    var queue = PENDING.computeIfAbsent(level.dimension(), k -> new java.util.ArrayDeque<>());
    for (JournalLog.Stamped<Entry> s : used) queue.add(s.value());
    return used.size();
  }

  /** Quantos blocos ainda esperam a vez de voltar nesta dimensao. */
  public static int pending(ServerLevel level) {
    var q = PENDING.get(level.dimension());
    return q == null ? 0 : q.size();
  }

  /** Poe de volta, agora, tudo o que esta na fila (testes e desligamento). */
  public static void flush(ServerLevel level) {
    while (pending(level) > 0) place(level, Integer.MAX_VALUE);
  }

  /** Poe de volta ate {@code budget} blocos da fila (um tick de recolocacao; usado nos testes de desempenho). */
  public static void flushSome(ServerLevel level, int budget) { place(level, budget); }

  private static void place(ServerLevel level, int budget) {
    var q = PENDING.get(level.dimension());
    if (q == null) return;
    int placed = 0;
    while (!q.isEmpty() && budget-- > 0) {
      Entry e = q.poll();
      if (!level.isLoaded(e.pos()) || level.getBlockState(e.pos()) != e.after()) continue; // mudou no meio do caminho: nao mexe
      level.setBlock(e.pos(), e.before(), 3);
      if (placed++ % 2 == 0) level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, e.pos().getX() + 0.5, e.pos().getY() + 0.6, e.pos().getZ() + 0.5, 2, 0.25, 0.25, 0.25, 0.01);
    }
    if (placed > 0 && level.getGameTime() % 3 == 0) {
      var first = q.peek();
      net.minecraft.core.BlockPos at = first != null ? first.pos() : null;
      if (at != null) level.playSound(null, at, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.2f);
    }
  }

  @SubscribeEvent
  public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
    if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || PENDING.isEmpty()) return;
    int budget = ArcaneConfig.RESTORE_BLOCKS_PER_TICK.get();
    for (var it = PENDING.entrySet().iterator(); it.hasNext(); ) {
      var en = it.next();
      ServerLevel lvl = event.getServer().getLevel(en.getKey());
      if (lvl == null || en.getValue().isEmpty()) { it.remove(); continue; }
      place(lvl, budget);
    }
  }

  @SubscribeEvent
  public static void stopped(ServerStoppedEvent event) { LOGS.clear(); PENDING.clear(); }
}
