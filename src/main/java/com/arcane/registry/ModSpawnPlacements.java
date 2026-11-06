package com.arcane.registry;

import com.arcane.ArcaneMod;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Regras de spawn do Mago: como um monstro comum (escuro, chao solido). Os biomas ficam no biome_modifier. */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModSpawnPlacements {
  private ModSpawnPlacements() {}

  @SubscribeEvent
  public static void register(SpawnPlacementRegisterEvent event) {
    event.register(ModEntities.NPC_MAGE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
  }
}
