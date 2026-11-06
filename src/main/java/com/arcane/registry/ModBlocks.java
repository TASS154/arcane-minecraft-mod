package com.arcane.registry;

import com.arcane.ArcaneMod;
import com.arcane.block.ArcaneWorkbenchBlock;
import com.arcane.block.ManaCrystalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ArcaneMod.MODID);

    public static final RegistryObject<Block> ARCANE_WORKBENCH = BLOCKS.register("arcane_workbench",
            () -> new ArcaneWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f).noOcclusion()));
    public static final RegistryObject<Block> MANA_CRYSTAL = BLOCKS.register("mana_crystal",
            () -> new ManaCrystalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(5.0f).lightLevel(s -> 12).noOcclusion()));

    public static final RegistryObject<Block> ARCANE_BARRIER = BLOCKS.register("arcane_barrier",
            () -> new com.arcane.block.ArcaneBarrierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.8f).noLootTable().noOcclusion().lightLevel(s -> 6)
                    .sound(net.minecraft.world.level.block.SoundType.AMETHYST)
                    .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
