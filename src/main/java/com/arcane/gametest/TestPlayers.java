package com.arcane.gametest;

import com.arcane.ArcaneMod;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Jogadores falsos criados pelos testes saem do mundo quando o teste deles termina.
 *
 * <p>Causa de testes instaveis (25 nov): o GameTest limpa as entidades da area antes de cada lote, MENOS jogadores
 * ({@code StructureUtils.clearSpaceForStructure}), e os lotes reaproveitam as mesmas posicoes da grade. Os jogadores de
 * testes antigos ficavam parados onde os testes seguintes rodavam: o desenho "Ao Chegar Perto" disparava neles em vez do
 * porco, o golpe de cauda da Hidra podia mirar neles, e por ai vai, conforme a ordem dos lotes.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class TestPlayers {
  private record Tracked(ServerPlayer player, GameTestInfo info) {}

  private static final List<Tracked> TRACKED = new ArrayList<>();
  private static Field infoField;

  private TestPlayers() {}

  /** Poe o jogador no mundo do teste e o tira quando o teste acabar. */
  public static ServerPlayer add(GameTestHelper h, ServerPlayer player) {
    h.getLevel().addNewPlayer(player);
    GameTestInfo info = infoOf(h);
    if (info != null) synchronized (TRACKED) { TRACKED.add(new Tracked(player, info)); }
    return player;
  }

  private static GameTestInfo infoOf(GameTestHelper h) {
    try {
      if (infoField == null) {
        for (Field f : GameTestHelper.class.getDeclaredFields()) {
          if (f.getType() == GameTestInfo.class) {
            f.setAccessible(true);
            infoField = f;
          }
        }
      }
      return infoField == null ? null : (GameTestInfo) infoField.get(h);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  @SubscribeEvent
  public static void onTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    synchronized (TRACKED) {
      if (TRACKED.isEmpty()) return;
      TRACKED.removeIf(t -> {
        if (!t.info().isDone()) return false;
        t.player().serverLevel().removePlayerImmediately(t.player(), Entity.RemovalReason.DISCARDED);
        return true;
      });
    }
  }
}
