package com.arcane;

import com.arcane.ambient.ManaPoisoningService;
import com.arcane.ambient.AmbientFieldResolver;
import com.arcane.execution.scheduler.SpellTickScheduler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public class ServerTickHandler {
  @SubscribeEvent
  public static void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
      SpellTickScheduler.tick(event.getServer());
      event.getServer().getPlayerList().getPlayers().forEach(player -> {
        var env = AmbientFieldResolver.resolve(player.serverLevel(), player.blockPosition());
        ManaPoisoningService.tick(player, env);
      });
    }
  }
}
