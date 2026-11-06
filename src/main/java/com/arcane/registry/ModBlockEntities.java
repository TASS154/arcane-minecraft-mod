package com.arcane.registry;

import com.arcane.ArcaneMod;
import com.arcane.block.AltarBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
      DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ArcaneMod.MODID);

  public static final RegistryObject<BlockEntityType<AltarBlockEntity>> ARCANE_ALTAR = BLOCK_ENTITIES.register("arcane_altar",
      () -> BlockEntityType.Builder.of(AltarBlockEntity::new, ModBlocks.ARCANE_ALTAR.get()).build(null));

  private ModBlockEntities() {}

  public static void register(IEventBus bus) {
    BLOCK_ENTITIES.register(bus);
  }
}
