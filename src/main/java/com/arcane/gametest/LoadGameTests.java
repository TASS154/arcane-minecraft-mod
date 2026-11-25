package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.config.ArcaneConfig;
import com.arcane.execution.CraterQueue;
import com.arcane.execution.RoundCrater;
import com.arcane.restore.BlockJournal;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Carga do servidor (Fase D): crateras de raio 64 e 128 com os tetos de area desligados (perfil "Sem limites").
 * Mede e imprime ("[carga]" no log): tempo para calcular os blocos da cratera (um tick so), memoria da lista, pior tick
 * enquanto a frente cava e tamanho do diario do Restaurar. Afirma so o que nao pode piorar sem aviso; os numeros vao
 * para docs/CARGA.md.
 */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class LoadGameTests {
  private static final int SAMPLE_TICKS = 40;

  @GameTest(template = "empty", batch = "load_crater_1", timeoutTicks = 400)
  public static void craterRadius64And128(GameTestHelper h) {
    ServerLevel level = h.getLevel();
    boolean caps = ArcaneConfig.AREA_HARD_CAPS.get();
    int maxR = ArcaneConfig.CRATER_MAX_RADIUS.get();
    ArcaneConfig.AREA_HARD_CAPS.set(false);
    ArcaneConfig.CRATER_MAX_RADIUS.set(0);
    StringBuilder report = new StringBuilder();
    Runnable restore = () -> {
      ArcaneConfig.AREA_HARD_CAPS.set(caps);
      ArcaneConfig.CRATER_MAX_RADIUS.set(maxR);
      CraterQueue.clear(level);
      BlockJournal.clear(level);
    };
    try {
      measure(h, level, 64, report, () -> measure(h, level, 128, report, () -> {
        restore.run();
        System.out.println("[carga] " + report.toString().replace("\n", "\n[carga] "));
        h.succeed();
      }));
    } catch (RuntimeException e) {
      restore.run();
      throw e;
    }
  }

  /** Uma cratera longe dos outros testes (no subsolo), medida por SAMPLE_TICKS ticks; depois roda {@code next}. */
  private static void measure(GameTestHelper h, ServerLevel level, int radius, StringBuilder report, Runnable next) {
    // y = 64: a esfera inteira cabe no mundo (-64 a 320) e a metade de baixo e terreno solido
    Vec3 base = h.absoluteVec(new Vec3(3, 0, 3));
    Vec3 centre = new Vec3(base.x + 6000 + radius * 3, 64, base.z + 6000);
    CraterQueue.clear(level);
    BlockJournal.clear(level);
    long t0 = System.nanoTime();
    var blocks = RoundCrater.blockLongs(level, centre, radius);
    double listMs = (System.nanoTime() - t0) / 1.0e6;
    long listBytes = (long) blocks.size() * 8;
    long t1 = System.nanoTime();
    CraterQueue.submit(level, blocks, centre);
    double submitMs = (System.nanoTime() - t1) / 1.0e6;
    var server = level.getServer();
    int startTick = server.getTickCount();
    int pendingAtStart = CraterQueue.pending(level);
    h.runAfterDelay(SAMPLE_TICKS, () -> {
      long worst = 0;
      for (int t = startTick + 1; t <= server.getTickCount(); t++) worst = Math.max(worst, server.tickTimes[t % server.tickTimes.length]);
      int removed = pendingAtStart - CraterQueue.pending(level);
      double perTick = removed / (double) SAMPLE_TICKS;
      double secondsLeft = perTick <= 0 ? Double.POSITIVE_INFINITY : CraterQueue.pending(level) / perTick / 20.0;
      report.append(String.format(java.util.Locale.ROOT,
          "raio %d: %d blocos (lista %.1f MB, calculada em %.0f ms num tick; submit %.0f ms); %d removidos em %d ticks "
              + "(pior tick %.1f ms); faltam %.0f s; diario %d entradas (teto %d)%n",
          radius, blocks.size(), listBytes / 1048576.0, listMs, submitMs, removed, SAMPLE_TICKS, worst / 1.0e6, secondsLeft,
          BlockJournal.size(level), ArcaneConfig.RESTORE_JOURNAL_CAP.get()));
      h.assertTrue(BlockJournal.size(level) <= ArcaneConfig.RESTORE_JOURNAL_CAP.get(), "o diario respeita o teto");
      h.assertTrue(removed <= ArcaneConfig.CRATER_BLOCKS_PER_TICK.get() * (SAMPLE_TICKS + 1), "a frente respeita o orcamento por tick");
      CraterQueue.clear(level);
      next.run();
    });
  }
}
