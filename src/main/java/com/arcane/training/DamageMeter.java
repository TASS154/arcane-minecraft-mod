package com.arcane.training;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Medidor de dano (Fase H): instrumento de medida do Boneco de Treino e da simulacao de balanco
 * ({@code BalanceSimulationGameTests}). Guarda os golpes de uma "sessao" (do primeiro golpe ate o alvo ficar ocioso),
 * as reacoes disparadas e o custo de mana da ultima conjuracao de quem bateu. Os valores sao em HP (o Boneco converte
 * para coracoes so na tela: {@link HeartText}).
 *
 * <p>Nucleo puro (o tempo entra como tick de jogo), testado em JUnit. A ligacao com o jogo esta em
 * {@link DamageMeterEvents} (dano depois da armadura, pelo {@code LivingDamageEvent}) e em {@link #track}.
 */
public final class DamageMeter {
  /** Janela da media de dano por segundo: 5 s. */
  public static final int WINDOW_TICKS = 100;
  private static final int MAX_HITS = 256;

  public record Hit(long tick, float hp, String source) {}

  private final Deque<Hit> hits = new ArrayDeque<>();
  private final Map<String, Integer> reactions = new LinkedHashMap<>();
  private float total;
  private int count;
  private float lastHit;
  private long lastTick = Long.MIN_VALUE;
  private int lastManaCost = -1;
  private boolean idle = true;

  /** Registra um golpe de {@code hp} pontos de vida. Se o medidor estava ocioso, comeca uma sessao nova. */
  public void record(long tick, float hp, String source) {
    if (hp <= 0) return;
    if (idle) clear();
    idle = false;
    hits.addLast(new Hit(tick, hp, source == null ? "?" : source));
    while (hits.size() > MAX_HITS) hits.removeFirst();
    total += hp;
    count++;
    lastHit = hp;
    lastTick = tick;
  }

  /** Reacao elemental disparada no alvo (vapor, estilhaco, chiado...). */
  public void noteReaction(String id) {
    if (idle) clear();
    idle = false;
    reactions.merge(id, 1, Integer::sum);
  }

  /** Custo de mana da conjuracao que acertou por ultimo. */
  public void noteManaCost(int mana) { lastManaCost = mana; }

  /** Fecha a sessao: os numeros continuam visiveis ate o proximo golpe, que comeca outra. */
  public void markIdle() { idle = true; }

  public boolean idle() { return idle; }

  public void clear() {
    hits.clear();
    reactions.clear();
    total = 0;
    count = 0;
    lastHit = 0;
    lastTick = Long.MIN_VALUE;
    lastManaCost = -1;
  }

  public float total() { return total; }

  public int count() { return count; }

  public float lastHit() { return lastHit; }

  public long lastTick() { return lastTick; }

  public int lastManaCost() { return lastManaCost; }

  /** Ticks desde o ultimo golpe ({@link Long#MAX_VALUE} sem golpe). */
  public long sinceLast(long now) { return lastTick == Long.MIN_VALUE ? Long.MAX_VALUE : now - lastTick; }

  /** Media de HP por segundo nos ultimos 5 s (soma da janela dividida por 5). */
  public float perSecond(long now) {
    float sum = 0;
    for (Hit h : hits) if (now - h.tick() < WINDOW_TICKS) sum += h.hp();
    return sum / (WINDOW_TICKS / 20f);
  }

  public Map<String, Integer> reactions() { return Collections.unmodifiableMap(reactions); }

  /** Ultimos golpes, do mais antigo ao mais novo. */
  public List<Hit> recent(int max) {
    List<Hit> all = new ArrayList<>(hits);
    return all.subList(Math.max(0, all.size() - max), all.size());
  }

  // ------------------------------------------------------------------ medidores ligados a entidades
  private static final Map<Object, DamageMeter> TRACKED = Collections.synchronizedMap(new WeakHashMap<>());

  /** Passa a medir o dano de {@code entity} (o Boneco se registra sozinho; a simulacao registra os alvos). */
  public static DamageMeter track(Object entity) {
    return TRACKED.computeIfAbsent(entity, e -> new DamageMeter());
  }

  /** Medidor da entidade, ou null se ninguem a mede. */
  public static DamageMeter of(Object entity) { return TRACKED.get(entity); }

  public static void untrack(Object entity) { TRACKED.remove(entity); }

  // ------------------------------------------------------------------ ultima conjuracao de cada conjurador
  private static final Map<java.util.UUID, Integer> LAST_COST = new java.util.concurrent.ConcurrentHashMap<>();

  /** Chamado pela conjuracao (CastingService): o custo vai para o medidor do alvo que ela acertar. */
  public static void noteCast(java.util.UUID caster, int mana) { LAST_COST.put(caster, mana); }

  public static int lastCastCost(java.util.UUID caster) { return LAST_COST.getOrDefault(caster, -1); }
}
