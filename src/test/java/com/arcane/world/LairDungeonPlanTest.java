package com.arcane.world;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A planta da masmorra do covil em milhares de posicoes. Causa do GameTest instavel de 25 nov: a posicao do teste muda
 * quando outros testes entram, e havia covis em que a planta saia com 1 sala (salas sem lugar eram puladas).
 */
class LairDungeonPlanTest {
  @Test
  void everyLairGetsAllItsRoomsFarFromTheChamberAndDeterministic() {
    int worst = Integer.MAX_VALUE;
    for (int i = 0; i < 4000; i++) {
      BlockPos chamber = new BlockPos(i * 977 - 1_000_000, -20 + (i % 40), i * 613 - 700_000);
      BlockPos entrance = chamber.offset(38, 0, 0);
      List<LairDungeon.Step> plan = LairDungeon.plan(chamber, entrance);
      worst = Math.min(worst, plan.size());
      assertTrue(plan.size() >= LairDungeon.MIN_ROOMS && plan.size() <= LairDungeon.MAX_ROOMS,
          "covil em " + chamber + ": " + plan.size() + " salas");
      assertEquals(LairDungeon.Room.GUARD, plan.get(0).type(), "a primeira e a guarita");
      Set<Long> cells = new HashSet<>();
      for (var s : plan) {
        assertTrue(Math.hypot(s.at().getX() - chamber.getX(), s.at().getZ() - chamber.getZ()) >= 52, "longe da camara");
        assertTrue(cells.add(((long) Math.floorDiv(s.at().getX(), 12) << 32) ^ (Math.floorDiv(s.at().getZ(), 12) & 0xffffffffL)),
            "uma sala por celula");
      }
      assertEquals(plan, LairDungeon.plan(chamber, entrance), "o mesmo covil gera sempre a mesma planta");
    }
    System.out.println("[masmorra] pior caso em 4000 covis: " + worst + " salas");
  }
}
