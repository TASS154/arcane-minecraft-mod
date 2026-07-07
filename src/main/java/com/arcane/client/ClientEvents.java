package com.arcane.client;

import com.arcane.ArcaneMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public class ClientEvents {
  @SubscribeEvent
  public static void onKey(InputEvent.Key event) {
    if (ClientSetup.CAST_KEY != null && ClientSetup.CAST_KEY.consumeClick()) {
      ClientSetup.tryCast();
    }
  }
}
