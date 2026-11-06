"""Gera os JSONs de dados do mod Arcane: receitas, loot de blocos, tags,
worldgen (cristais de mana) e loot de baus.

Uso:  python tools/gen_data.py
Os valores aqui sao provisorios e devem ser revisados no balanceamento.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from glyph_table import G  # noqa: E402

DATA = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "data"


def write(rel: str, obj) -> None:
    path = DATA / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("  ", path.relative_to(DATA.parents[2]))


def item(i):
    return {"tag": i[1:]} if i.startswith("#") else {"item": i}


def shaped(name, pattern, keys, count=1):
    write(f"arcane/recipes/{name}.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": pattern,
        "key": {k: item(v) for k, v in keys.items()},
        "result": {"item": f"arcane:{name}", "count": count},
    })


def shapeless(name, ingredients, count=1):
    write(f"arcane/recipes/{name}.json", {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [item(i) for i in ingredients],
        "result": {"item": f"arcane:{name}", "count": count},
    })


def note(glyph, weight):
    return {"type": "minecraft:item", "name": "arcane:glyph_note", "weight": weight,
            "functions": [{"function": "minecraft:set_nbt", "tag": "{glyph:\"%s\"}" % glyph}]}


def main():
    # ---------------------------------------------------------- receitas
    shaped("arcane_workbench", ["ABA", "PCP", "P P"],
           {"A": "minecraft:amethyst_shard", "B": "minecraft:book", "C": "minecraft:crafting_table", "P": "#minecraft:planks"})
    shaped("arcane_pen", ["  A", " F ", "G  "],
           {"A": "minecraft:amethyst_shard", "F": "minecraft:feather", "G": "minecraft:gold_nugget"})
    shaped("common_ink", ["I", "A", "B"],
           {"I": "minecraft:ink_sac", "A": "minecraft:amethyst_shard", "B": "minecraft:glass_bottle"}, 2)
    shaped("glyph_note", ["PPP", "PIP", "PPP"], {"P": "minecraft:paper", "I": "arcane:common_ink"}, 4)
    shaped("arcane_mask", ["LAL", "G G"],
           {"L": "minecraft:leather", "A": "minecraft:amethyst_shard", "G": "minecraft:tinted_glass"})
    shaped("insulating_cloak", ["W W", "WAW", "WWW"], {"W": "#minecraft:wool", "A": "minecraft:amethyst_shard"})
    shaped("glyph_projector", [" G ", "IAI", "PPP"],
           {"G": "minecraft:glass", "I": "minecraft:gold_ingot", "A": "minecraft:amethyst_shard", "P": "#minecraft:planks"})
    shaped("symmetry_mirror", [" G ", "GSG", " T "],
           {"G": "minecraft:gold_ingot", "S": "minecraft:glass_pane", "T": "minecraft:stick"})
    shapeless("grimoire", ["minecraft:book", "minecraft:amethyst_shard", "arcane:common_ink"])
    shapeless("blood_ink", ["arcane:common_ink", "minecraft:redstone", "minecraft:rotten_flesh"])
    shapeless("invisible_ink", ["arcane:common_ink", "minecraft:glowstone_dust", "minecraft:phantom_membrane"])

    # ---------------------------------------------------------- loot de blocos
    write("arcane/loot_tables/blocks/arcane_workbench.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "arcane:arcane_workbench"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    })
    write("arcane/loot_tables/blocks/mana_crystal.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:alternatives",
            "children": [
                {"type": "minecraft:item", "name": "arcane:mana_crystal",
                 "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                     "enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}]},
                {"type": "minecraft:item", "name": "minecraft:amethyst_shard",
                 "functions": [
                     {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 4}},
                     {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                      "formula": "minecraft:ore_drops"},
                     {"function": "minecraft:explosion_decay"}]},
            ]}]}],
    })

    # ---------------------------------------------------------- tags
    write("minecraft/tags/blocks/mineable/pickaxe.json", {"replace": False, "values": ["arcane:mana_crystal"]})
    write("minecraft/tags/blocks/needs_iron_tool.json", {"replace": False, "values": ["arcane:mana_crystal"]})
    write("minecraft/tags/blocks/mineable/axe.json", {"replace": False, "values": ["arcane:arcane_workbench"]})

    # ---------------------------------------------------------- worldgen
    write("arcane/worldgen/configured_feature/mana_crystal.json", {
        "type": "minecraft:ore",
        "config": {
            "size": 2,
            "discard_chance_on_air_exposure": 0.0,
            "targets": [
                {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
                 "state": {"Name": "arcane:mana_crystal"}},
                {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
                 "state": {"Name": "arcane:mana_crystal"}},
            ],
        },
    })
    write("arcane/worldgen/placed_feature/mana_crystal.json", {
        "feature": "arcane:mana_crystal",
        "placement": [
            {"type": "minecraft:rarity_filter", "chance": 4},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {
                "type": "minecraft:trapezoid", "min_inclusive": {"absolute": -56}, "max_inclusive": {"absolute": 24}}},
            {"type": "minecraft:biome"},
        ],
    })
    write("arcane/forge/biome_modifier/add_mana_crystal.json", {
        "type": "forge:add_features",
        "biomes": "#minecraft:is_overworld",
        "features": "arcane:mana_crystal",
        "step": "underground_ores",
    })

    # ---------------------------------------------------------- loot de baus
    starters = {"self", "projectile", "damage"}
    notes = [note(f"arcane:{gid}", max(1, 9 - g["cost"])) for gid, g in G.items() if gid not in starters]
    write("arcane/loot_tables/chests/simple_dungeon_glyph.json", {
        "type": "minecraft:chest",
        "pools": [
            {"rolls": {"type": "minecraft:uniform", "min": 0, "max": 1}, "entries": notes},
            {"rolls": {"type": "minecraft:uniform", "min": 0, "max": 2},
             "entries": [
                 {"type": "minecraft:item", "name": "arcane:common_ink", "weight": 4},
                 {"type": "minecraft:item", "name": "minecraft:amethyst_shard", "weight": 3,
                  "functions": [{"function": "minecraft:set_count",
                                 "count": {"type": "minecraft:uniform", "min": 1, "max": 4}}]},
                 {"type": "minecraft:item", "name": "arcane:blood_ink", "weight": 1}]},
        ],
    })

    # ---------------------------------------------------------- spawn natural do Mago
    write("arcane/forge/biome_modifier/add_mage_spawn.json", {
        "type": "forge:add_spawns",
        "biomes": "#minecraft:is_overworld",
        "spawners": {"type": "arcane:npc_mage", "weight": 4, "minCount": 1, "maxCount": 1},
    })

if __name__ == "__main__":
    main()
