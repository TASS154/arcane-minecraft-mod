package com.arcane.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Injeta notas de glifo e tintas em baus vanilla, por estrutura: cada estrutura tem um NIVEL (raridade maxima
 * dos glifos) e ESPECIALIDADES (familias favorecidas). O mapa vem de data/arcane/glyph_loot_targets.json e as
 * tabelas de data/arcane/loot_tables/chests/glyphs/ (geradas por tools/gen_data.py), entao o balanceamento
 * nao exige recompilar.
 */
public class ArcaneLootInjector {
  private static final String PREFIX = "chests/";
  private static Map<String, ResourceLocation> targets;

  /** minecraft:chests/<chave> -> arcane:chests/glyphs/<nome>. */
  static synchronized Map<String, ResourceLocation> targets() {
    if (targets != null) return targets;
    Map<String, ResourceLocation> map = new HashMap<>();
    try (InputStream in = ArcaneLootInjector.class.getResourceAsStream("/data/arcane/glyph_loot_targets.json")) {
      if (in != null) {
        JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        json.entrySet().forEach(e -> map.put(e.getKey(), new ResourceLocation("arcane", e.getValue().getAsString())));
      }
    } catch (Exception e) {
      LogUtils.getLogger().error("Nao foi possivel ler glyph_loot_targets.json", e);
    }
    targets = Map.copyOf(map);
    return targets;
  }

  @SubscribeEvent
  public void onLootLoad(LootTableLoadEvent event) {
    ResourceLocation name = event.getName();
    if (!"minecraft".equals(name.getNamespace()) || !name.getPath().startsWith(PREFIX)) return;
    ResourceLocation table = targets().get(name.getPath().substring(PREFIX.length()));
    if (table == null) return;
    event.getTable().addPool(LootPool.lootPool()
        .name("arcane_glyphs")
        .setRolls(ConstantValue.exactly(1))
        .add(LootTableReference.lootTableReference(table))
        .build());
  }
}
