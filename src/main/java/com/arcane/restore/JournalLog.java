package com.arcane.restore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;

/**
 * Registro com teto e validade (logica pura, sem Minecraft): as entradas mais antigas saem quando passa do teto
 * e as que passaram da janela expiram. Guarda a ordem de insercao, do mais antigo para o mais novo.
 */
public final class JournalLog<E> {
  public record Stamped<E>(E value, long tick) {}

  private final ArrayDeque<Stamped<E>> entries = new ArrayDeque<>();

  public Stamped<E> add(E value, long now, int cap, long window) {
    Stamped<E> s = new Stamped<>(value, now);
    entries.addLast(s);
    trim(now, cap, window);
    return s;
  }

  /** Aplica o teto de entradas e a validade. */
  public void trim(long now, int cap, long window) {
    while (entries.size() > Math.max(1, cap)) entries.removeFirst();
    while (!entries.isEmpty() && now - entries.peekFirst().tick() > window) entries.removeFirst();
  }

  /** Entradas ainda validas, da mais nova para a mais antiga. */
  public List<Stamped<E>> live(long now, long window) {
    List<Stamped<E>> out = new ArrayList<>();
    Iterator<Stamped<E>> it = entries.descendingIterator();
    while (it.hasNext()) {
      Stamped<E> s = it.next();
      if (now - s.tick() > window) break;
      out.add(s);
    }
    return out;
  }

  public void removeAll(Collection<Stamped<E>> gone) {
    if (gone.isEmpty()) return;
    Set<Stamped<E>> set = Collections.newSetFromMap(new IdentityHashMap<>());
    set.addAll(gone);
    entries.removeIf(set::contains);
  }

  public int size() { return entries.size(); }

  /** Todas as entradas, da mais antiga para a mais nova (para salvar). */
  public List<Stamped<E>> oldestFirst() { return new ArrayList<>(entries); }

  public void clear() { entries.clear(); }
}
