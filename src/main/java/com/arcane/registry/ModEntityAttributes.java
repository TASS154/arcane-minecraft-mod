package com.arcane.registry;

import com.arcane.ArcaneMod;
import com.arcane.entity.NPCMageEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {
  @SubscribeEvent
  public static void register(EntityAttributeCreationEvent event) {
    event.put(ModEntities.NPC_MAGE.get(), NPCMageEntity.createAttributes().build());
  }
}
