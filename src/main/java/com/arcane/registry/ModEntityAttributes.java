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
    event.put(ModEntities.HYDRA.get(), com.arcane.entity.HydraEntity.createAttributes().build());
    event.put(ModEntities.ILLUSION.get(), com.arcane.entity.IllusionEntity.createAttributes().build());
    event.put(ModEntities.SPECTRAL_WOLF.get(), net.minecraft.world.entity.animal.Wolf.createAttributes().build());
    event.put(ModEntities.ARCANE_GOLEM.get(), net.minecraft.world.entity.animal.IronGolem.createAttributes().build());
    event.put(ModEntities.TRAINING_DUMMY.get(), com.arcane.entity.ArcaneDummyEntity.createAttributes().build());
    event.put(ModEntities.RUIN_GUARDIAN.get(), com.arcane.entity.RuinGuardianEntity.createAttributes().build());
  }
}
