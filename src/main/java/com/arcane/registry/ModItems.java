package com.arcane.registry;

import com.arcane.ArcaneMod;
import com.arcane.item.ArcaneArmorMaterial;
import com.arcane.item.ArcaneCompassItem;
import com.arcane.item.ArcanePageItem;
import com.arcane.item.ArcanePenItem;
import com.arcane.item.GlyphNoteItem;
import com.arcane.item.GrimoireItem;
import com.arcane.item.InkItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
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
    public static final RegistryObject<Item> ARCANE_MASK = ITEMS.register("arcane_mask",
            () -> new ArmorItem(ArcaneArmorMaterial.ARCANE_WEAVE, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> INSULATING_CLOAK = ITEMS.register("insulating_cloak",
            () -> new ArmorItem(ArcaneArmorMaterial.ARCANE_WEAVE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> GLYPH_PROJECTOR = ITEMS.register("glyph_projector", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SYMMETRY_MIRROR = ITEMS.register("symmetry_mirror", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GRIMOIRE = ITEMS.register("grimoire", () -> new GrimoireItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ARCANE_PAGE = ITEMS.register("arcane_page", () -> new ArcanePageItem(new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_ALTAR = ITEMS.register("arcane_altar",
            () -> new BlockItem(ModBlocks.ARCANE_ALTAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_CHALK = ITEMS.register("arcane_chalk",
            () -> new BlockItem(ModBlocks.ARCANE_TRACE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_COMPASS = ITEMS.register("arcane_compass",
            () -> new ArcaneCompassItem(new Item.Properties()));
    public static final RegistryObject<Item> NPC_MAGE_SPAWN_EGG = ITEMS.register("npc_mage_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.NPC_MAGE, 0x2D1946, 0x5ADCF0, new Item.Properties()));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
