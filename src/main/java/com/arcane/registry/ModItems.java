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

    public static final RegistryObject<Item> ARCANE_BRICKS = ITEMS.register("arcane_bricks", () -> new BlockItem(ModBlocks.ARCANE_BRICKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> CHISELED_ARCANE_BRICKS = ITEMS.register("chiseled_arcane_bricks", () -> new BlockItem(ModBlocks.CHISELED_ARCANE_BRICKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> RUNE_LAMP = ITEMS.register("rune_lamp", () -> new BlockItem(ModBlocks.RUNE_LAMP.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_WORKBENCH = ITEMS.register("arcane_workbench",
            () -> new BlockItem(ModBlocks.ARCANE_WORKBENCH.get(), new Item.Properties()));
    public static final RegistryObject<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal",
            () -> new BlockItem(ModBlocks.MANA_CRYSTAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> MANA_SURGE_POTION = ITEMS.register("mana_surge_potion",
            () -> new com.arcane.item.ManaPotionItem(new Item.Properties(), com.arcane.item.ManaPotionItem.Kind.SURGE));
    public static final RegistryObject<Item> MANA_VESSEL_POTION = ITEMS.register("mana_vessel_potion",
            () -> new com.arcane.item.ManaPotionItem(new Item.Properties(), com.arcane.item.ManaPotionItem.Kind.VESSEL));
    public static final RegistryObject<Item> ARCANE_PEN = ITEMS.register("arcane_pen", () -> new ArcanePenItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> COMMON_INK = ITEMS.register("common_ink", () -> new InkItem(new Item.Properties(), "common"));
    public static final RegistryObject<Item> BLOOD_INK = ITEMS.register("blood_ink", () -> new InkItem(new Item.Properties(), "blood"));
    public static final RegistryObject<Item> INVISIBLE_INK = ITEMS.register("invisible_ink", () -> new InkItem(new Item.Properties(), "invisible"));
    public static final RegistryObject<Item> EARTH_PEN = ITEMS.register("earth_pen", () -> new com.arcane.item.BlockPenItem(new Item.Properties(), "earth"));
    public static final RegistryObject<Item> STONE_PEN = ITEMS.register("stone_pen", () -> new com.arcane.item.BlockPenItem(new Item.Properties(), "stone"));
    public static final RegistryObject<Item> METAL_PEN = ITEMS.register("metal_pen", () -> new com.arcane.item.BlockPenItem(new Item.Properties(), "metal"));
    public static final RegistryObject<Item> SNOW_PEN = ITEMS.register("snow_pen", () -> new com.arcane.item.BlockPenItem(new Item.Properties(), "snow"));
    public static final RegistryObject<Item> WOOD_PEN = ITEMS.register("wood_pen", () -> new com.arcane.item.BlockPenItem(new Item.Properties(), "wood"));
    public static final RegistryObject<Item> FOOD_PEN = ITEMS.register("food_pen", () -> new com.arcane.item.ObjectPenItem(new Item.Properties(), com.arcane.inscribing.ItemDrawings.Kind.FOOD));
    public static final RegistryObject<Item> WEAPON_PEN = ITEMS.register("weapon_pen", () -> new com.arcane.item.ObjectPenItem(new Item.Properties(), com.arcane.inscribing.ItemDrawings.Kind.WEAPON));
    public static final RegistryObject<Item> POTION_PEN = ITEMS.register("potion_pen", () -> new com.arcane.item.ObjectPenItem(new Item.Properties(), com.arcane.inscribing.ItemDrawings.Kind.POTION));
    public static final RegistryObject<Item> REDSTONE_PEN = ITEMS.register("redstone_pen", () -> new com.arcane.item.BlockPenItem(new Item.Properties(), "redstone"));
    public static final RegistryObject<Item> SCEPTER = ITEMS.register("scepter", () -> new com.arcane.item.ScepterItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> PHOENIX_PEN = ITEMS.register("phoenix_pen", () -> new com.arcane.item.AirPenItem(new Item.Properties()));
    public static final RegistryObject<Item> CREATURE_PEN = ITEMS.register("creature_pen", () -> new com.arcane.item.CreaturePenItem(new Item.Properties()));
    public static final RegistryObject<Item> PERMANENT_INK = ITEMS.register("permanent_ink", () -> new InkItem(new Item.Properties(), "permanent"));
    public static final RegistryObject<Item> MIXED_COMMON_INK = ITEMS.register("mixed_common_ink", () -> new InkItem(new Item.Properties(), "common_perm"));
    public static final RegistryObject<Item> MIXED_BLOOD_INK = ITEMS.register("mixed_blood_ink", () -> new InkItem(new Item.Properties(), "blood_perm"));
    public static final RegistryObject<Item> MIXED_INVISIBLE_INK = ITEMS.register("mixed_invisible_ink", () -> new InkItem(new Item.Properties(), "invisible_perm"));
    public static final RegistryObject<Item> SEALED_CREATURE = ITEMS.register("sealed_creature", () -> new com.arcane.item.SealedCreatureItem(new Item.Properties()));
    public static final RegistryObject<Item> SOLVENT_POWDER = ITEMS.register("solvent_powder", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GLYPH_NOTE = ITEMS.register("glyph_note", () -> new GlyphNoteItem(new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_MASK = ITEMS.register("arcane_mask",
            () -> new ArmorItem(ArcaneArmorMaterial.ARCANE_WEAVE, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> INSULATING_CLOAK = ITEMS.register("insulating_cloak",
            () -> new ArmorItem(ArcaneArmorMaterial.ARCANE_WEAVE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> GLYPH_PROJECTOR = ITEMS.register("glyph_projector", () -> new com.arcane.item.GlyphProjectorItem(new Item.Properties()));
    public static final RegistryObject<Item> SYMMETRY_MIRROR = ITEMS.register("symmetry_mirror", () -> new com.arcane.item.SymmetryMirrorItem(new Item.Properties()));

    public static final RegistryObject<Item> GRIMOIRE = ITEMS.register("grimoire", () -> new GrimoireItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ARCANE_PAGE = ITEMS.register("arcane_page", () -> new ArcanePageItem(new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_ALTAR = ITEMS.register("arcane_altar",
            () -> new BlockItem(ModBlocks.ARCANE_ALTAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_CHALK = ITEMS.register("arcane_chalk",
            () -> new BlockItem(ModBlocks.ARCANE_TRACE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_COMPASS = ITEMS.register("arcane_compass",
            () -> new ArcaneCompassItem(new Item.Properties()));
    public static final RegistryObject<Item> PERFECT_COMPASS = ITEMS.register("perfect_compass",
            () -> new com.arcane.item.PerfectCompassItem(new Item.Properties()));
    public static final RegistryObject<Item> NPC_MAGE_SPAWN_EGG = ITEMS.register("npc_mage_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.NPC_MAGE, 0x2D1946, 0x5ADCF0, new Item.Properties()));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
