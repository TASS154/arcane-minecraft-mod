package com.arcane.execution;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Barramento de eventos das magias: quem produz um evento (uma zona que termina, uma barreira que quebra...) chama
 * {@link #emit}; quem espera por ele (um trecho com condicao "Quando a zona acabar") se inscreve com {@link #once}.
 * As chaves sao textos livres (ex.: "zone_end:<id>"), entao novos eventos nao exigem mudar este codigo.
 */
public final class SpellEvents {
  public record Event(String key, ServerLevel level, Vec3 pos) {}

  private static final Map<String, List<Consumer<Event>>> ONCE = new ConcurrentHashMap<>();

  private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

  private SpellEvents() {}

  /** Inscreve um ouvinte que dispara uma unica vez quando o evento da chave acontecer. */
  public static void once(String key, Consumer<Event> listener) {
    ONCE.computeIfAbsent(key, k -> new ArrayList<>()).add(listener);
  }

  /** Dispara e descarta os ouvintes da chave. Uma falha em um ouvinte nao impede os demais. */
  public static void emit(Event event) {
    List<Consumer<Event>> listeners = ONCE.remove(event.key());
    if (listeners == null) return;
    for (Consumer<Event> l : listeners) {
      try {
        l.accept(event);
      } catch (RuntimeException e) {
        LOG.warn("Ouvinte de evento {} falhou", event.key(), e);
      }
    }
  }

  /** Descarta ouvintes de uma chave sem dispara-los (zona removida por limpeza). */
  public static void drop(String key) { ONCE.remove(key); }

  public static int listenerCount(String key) {
    List<Consumer<Event>> l = ONCE.get(key);
    return l == null ? 0 : l.size();
  }
}
