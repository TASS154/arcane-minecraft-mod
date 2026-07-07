package com.arcane.registry;

import com.arcane.ArcaneMod;
import com.arcane.item.ArcanePenItem;
import com.arcane.item.GlyphNoteItem;
import com.arcane.item.InkItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ArcaneMod.MODID);

    public static final RegistryObject<Item> ARCANE_WORKBENCH = ITEMS.register("arcane_workbench",
            () -> new BlockItem(ModBlocks.ARCANE_WORKBENCH.get(), new Item.Properties()));
    public static final RegistryObject<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal",
            () -> new BlockItem(ModBlocks.MANA_CRYSTAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_PEN = ITEMS.register("arcane_pen", () -> new ArcanePenItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> COMMON_INK = ITEMS.register("common_ink", () -> new InkItem(new Item.Properties(), "common"));
    public static final RegistryObject<Item> BLOOD_INK = ITEMS.register("blood_ink", () -> new InkItem(new Item.Properties(), "blood"));
    public static final RegistryObject<Item> INVISIBLE_INK = ITEMS.register("invisible_ink", () -> new InkItem(new Item.Properties(), "invisible"));
    public static final RegistryObject<Item> GLYPH_NOTE = ITEMS.register("glyph_note", () -> new GlyphNoteItem(new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_MASK = ITEMS.register("arcane_mask", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INSULATING_CLOAK = ITEMS.register("insulating_cloak", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GLYPH_PROJECTOR = ITEMS.register("glyph_projector", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SYMMETRY_MIRROR = ITEMS.register("symmetry_mirror", () -> new Item(new Item.Properties().stacksTo(1)));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
