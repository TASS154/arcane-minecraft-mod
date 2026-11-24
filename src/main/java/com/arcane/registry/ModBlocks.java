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

    // Ruinas Arcanas: tijolos de pedra com ametista, a versao entalhada com runa e a lampada runica (luz 15)
    public static final RegistryObject<Block> ARCANE_BRICKS = BLOCKS.register("arcane_bricks",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5f, 9.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CHISELED_ARCANE_BRICKS = BLOCKS.register("chiseled_arcane_bricks",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5f, 9.0f).requiresCorrectToolForDrops().lightLevel(s -> 4)));
    public static final RegistryObject<Block> RUNE_LAMP = BLOCKS.register("rune_lamp",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5f).requiresCorrectToolForDrops().lightLevel(s -> 15)));
    public static final RegistryObject<Block> ARCANE_WORKBENCH = BLOCKS.register("arcane_workbench",
            () -> new ArcaneWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f).noOcclusion()));
    public static final RegistryObject<Block> MANA_CRYSTAL = BLOCKS.register("mana_crystal",
            () -> new ManaCrystalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(5.0f).lightLevel(s -> 12).noOcclusion()));

    public static final RegistryObject<Block> ARCANE_BARRIER = BLOCKS.register("arcane_barrier",
            () -> new com.arcane.block.ArcaneBarrierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.8f, 1200.0f).noLootTable().noOcclusion().lightLevel(s -> 6)
                    .sound(net.minecraft.world.level.block.SoundType.AMETHYST)
                    .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));

    public static final RegistryObject<Block> ARCANE_TRACE = BLOCKS.register("arcane_trace",
            () -> new com.arcane.block.ArcaneTraceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.1f).noCollission().noOcclusion().noLootTable()
                    .lightLevel(s -> s.getValue(com.arcane.block.ArcaneTraceBlock.LIT) ? 12 : 0)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final RegistryObject<Block> ARCANE_SIGNAL = BLOCKS.register("arcane_signal",
            () -> new com.arcane.block.ArcaneSignalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.0f).noCollission().noOcclusion().noLootTable().lightLevel(s -> 10)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final RegistryObject<Block> ARCANE_ALTAR = BLOCKS.register("arcane_altar",
            () -> new com.arcane.block.ArcaneAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(4.0f, 1200.0f).requiresCorrectToolForDrops().lightLevel(s -> 7).noOcclusion()));

    /** Selo do ritual da Hidra (sem item: so aparece nos covis). Duro, imune a explosoes comuns e a pistoes. */
    public static final RegistryObject<Block> HYDRA_SEAL = BLOCKS.register("hydra_seal",
            () -> new com.arcane.block.HydraSealBlock(BlockBehaviour.Properties.of().mapColor(MapColor.CRIMSON_NYLIUM)
                    .strength(12.0f, 3600000.0f).noLootTable().noOcclusion().lightLevel(s -> 13)
                    .sound(net.minecraft.world.level.block.SoundType.AMETHYST)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
