package com.arcane.gametest;

import com.arcane.ArcaneMod;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * O servidor de testes reaproveita o mesmo mundo entre execucoes, entao desenhos, zonas, cicatrizes e luzes salvos por
 * testes antigos continuavam vivos e disparavam magias nos testes seguintes (falhas que mudavam a cada rodada).
 * So no servidor de GameTest: comeca sempre limpo.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class GameTestReset {
  private GameTestReset() {}

  // Depois dos outros (Zones carrega o que estava salvo no mesmo evento).
  @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
  public static void onStarted(ServerStartedEvent event) {
    if (!(event.getServer() instanceof GameTestServer)) return;
    for (ServerLevel level : event.getServer().getAllLevels()) {
      com.arcane.inscribing.WorldInscriptionIndex.get(level).clearAll();
      com.arcane.execution.LightService.clearAll(level);
    }
    // Mundo novo a cada execucao: o clima comeca sorteado. Os testes contam com tempo limpo e sem ciclo de clima.
    ServerLevel overworld = event.getServer().overworld();
    overworld.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, event.getServer());
    overworld.setWeatherParameters(1000000, 0, false, false);
    com.arcane.execution.Zones.clearAll();
    com.arcane.execution.WorldScars.clearAll();
  }
}
