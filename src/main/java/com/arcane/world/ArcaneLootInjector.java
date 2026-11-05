package com.arcane.world;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;

/**
 * Injeta notas de glifo e tintas em baus vanilla. O conteudo fica no datapack
 * arcane:chests/simple_dungeon_glyph, assim o balanceamento nao exige recompilar.
 */
public class ArcaneLootInjector {
  private static final ResourceLocation GLYPH_LOOT = new ResourceLocation("arcane", "chests/simple_dungeon_glyph");
  private static final Set<ResourceLocation> TARGETS = Set.of(
      new ResourceLocation("minecraft", "chests/simple_dungeon"),
      new ResourceLocation("minecraft", "chests/abandoned_mineshaft"),
      new ResourceLocation("minecraft", "chests/stronghold_library"),
      new ResourceLocation("minecraft", "chests/woodland_mansion"),
      new ResourceLocation("minecraft", "chests/desert_pyramid"),
      new ResourceLocation("minecraft", "chests/jungle_temple"),
      new ResourceLocation("minecraft", "chests/ancient_city")
  );

  @SubscribeEvent
  public void onLootLoad(LootTableLoadEvent event) {
    if (!TARGETS.contains(event.getName())) return;
    event.getTable().addPool(LootPool.lootPool()
        .name("arcane_glyphs")
        .setRolls(ConstantValue.exactly(1))
        .add(LootTableReference.lootTableReference(GLYPH_LOOT))
        .build());
  }
}
