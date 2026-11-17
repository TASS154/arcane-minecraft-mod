package com.arcane.restore;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalLogTest {
  @Test
  void capDropsTheOldestFirst() {
    var log = new JournalLog<String>();
    for (int i = 0; i < 6; i++) log.add("e" + i, 100, 4, 1000);
    assertEquals(4, log.size());
    var live = log.live(100, 1000);
    assertEquals("e5", live.get(0).value(), "a mais nova vem primeiro");
    assertEquals("e2", live.get(3).value(), "as duas mais antigas sairam");
  }

  @Test
  void entriesExpireAfterTheWindow() {
    var log = new JournalLog<String>();
    log.add("old", 0, 100, 2400);
    log.add("new", 2000, 100, 2400);
    assertEquals(2, log.live(2400, 2400).size());
    var live = log.live(2500, 2400);
    assertEquals(1, live.size(), "a primeira passou de 120 s");
    assertEquals("new", live.get(0).value());
    log.trim(2500, 100, 2400);
    assertEquals(1, log.size(), "trim apaga as expiradas de verdade");
  }

  @Test
  void removeAllTakesOnlyTheGivenEntries() {
    var log = new JournalLog<String>();
    var a = log.add("a", 0, 10, 1000);
    log.add("b", 1, 10, 1000);
    var c = log.add("c", 2, 10, 1000);
    log.removeAll(List.of(a, c));
    assertEquals(1, log.size());
    assertEquals("b", log.live(2, 1000).get(0).value());
  }

  @Test
  void duplicatedValuesAreRemovedByIdentity() {
    var log = new JournalLog<String>();
    var first = log.add("same", 0, 10, 1000);
    log.add("same", 1, 10, 1000);
    log.removeAll(List.of(first));
    assertEquals(1, log.size(), "so a entrada pedida sai, nao a outra igual");
    assertTrue(log.live(1, 1000).get(0).tick() == 1);
  }
}
