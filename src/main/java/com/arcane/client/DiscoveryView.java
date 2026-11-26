package com.arcane.client;

import com.arcane.execution.ReactionJournal;
import com.arcane.network.packet.S2CReactionJournalPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aba "Descobertas" do Grimorio (Fase K), sem desenho: as linhas na ordem do catalogo, as descobertas com nome, dia e
 * trecho, as outras como "???" (nem nome nem dica: sem spoiler). O contador "X de Y" so conta o que esta no catalogo.
 */
public final class DiscoveryView {
  private DiscoveryView() {}

  public record Row(String id, boolean known, String nameKey, String hintKey, long day, List<String> excerpt) {}

  public static List<Row> rows(List<S2CReactionJournalPacket.Def> catalog, List<ReactionJournal.Entry> found) {
    Map<String, ReactionJournal.Entry> byId = new HashMap<>();
    for (var e : found) byId.putIfAbsent(e.id(), e);
    List<Row> out = new ArrayList<>();
    java.util.Set<String> seen = new java.util.HashSet<>();
    for (var d : catalog) {
      if (!seen.add(d.id())) continue;
      var e = byId.get(d.id());
      if (e == null) out.add(new Row(d.id(), false, "", "", 0, List.of()));
      else out.add(new Row(d.id(), true, d.nameKey(), d.hintKey(), e.gameDay(), excerpt(e.excerpt())));
    }
    return out;
  }

  public static int known(List<Row> rows) { return (int) rows.stream().filter(Row::known).count(); }

  static List<String> excerpt(String s) {
    if (s == null || s.isEmpty()) return List.of();
    return List.of(s.split(","));
  }
}
