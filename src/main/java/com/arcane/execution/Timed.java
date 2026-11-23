package com.arcane.execution;

import com.arcane.ArcaneMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

/**
 * Temporizador simples para efeitos curtos com duracao (charme, pastorear, portal...): roda {@code each} a cada tick
 * (com o tick do jogo) e {@code end} uma vez no fim. Nao e salvo: ao reiniciar o servidor os efeitos curtos acabam.
 * Limite de 512 tarefas; a mais antiga termina antes se passar disso.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class Timed {
  public static final int MAX = 512;

  private static final class Task {
    final ServerLevel level;
    final long until;
    final LongConsumer each;
    final Runnable end;
    Task(ServerLevel level, long until, LongConsumer each, Runnable end) { this.level = level; this.until = until; this.each = each; this.end = end; }
  }

  private static final List<Task> TASKS = new ArrayList<>();

  private Timed() {}

  public static void start(ServerLevel level, int ticks, LongConsumer each, Runnable end) {
    if (TASKS.size() >= MAX) finish(TASKS.remove(0));
    TASKS.add(new Task(level, level.getGameTime() + Math.max(1, ticks), each, end));
  }

  public static int running() { return TASKS.size(); }

  private static void finish(Task t) {
    if (t.end == null) return;
    try { t.end.run(); } catch (RuntimeException ignored) { }
  }

  @SubscribeEvent
  public static void onTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || TASKS.isEmpty()) return;
    for (Task t : new ArrayList<>(TASKS)) {
      long now = t.level.getGameTime();
      if (now >= t.until) {
        TASKS.remove(t);
        finish(t);
        continue;
      }
      if (t.each != null) {
        try { t.each.accept(now); } catch (RuntimeException e) { TASKS.remove(t); }
      }
    }
  }

  @SubscribeEvent
  public static void onStopped(ServerStoppedEvent event) { TASKS.clear(); }
}
