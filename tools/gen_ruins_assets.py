"""Blocos das Ruinas Arcanas (texturas, modelos, blockstates, drops, receitas) e as tabelas de loot das salas.

    python tools/gen_ruins_assets.py

Blocos: arcane_bricks, chiseled_arcane_bricks (runa ciano no meio), rune_lamp (luz 15).
Salas (loot em data/arcane/loot_tables/chests/ruins/): library, alchemy, armory, crypt, ritual, vault.
"""
import json
import random
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A = ROOT / "assets/arcane"
D = ROOT / "data/arcane"


def write(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


# ------------------------------------------------------------------ texturas
def bricks(seed, rune=False, lamp=False):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 4 if row % 2 else 0
            mortar = y % 4 == 3 or (x + off) % 8 == 7
            if mortar:
                c = (22, 18, 34)
            else:
                n = rnd.randint(-8, 8)
                c = (58 + n, 50 + n, 78 + n)
                if rnd.random() < 0.05:
                    c = (80, 70, 104)
            im.putpixel((x, y), c + (255,))
    if rune or lamp:
        glyph = ["................", "................", "......####......", ".....#....#.....", "....#..##..#....",
                 "....#.#..#.#....", "...##.#..#.##...", "....#..##..#....", "....#......#....", ".....#....#.....",
                 "......####......", ".......##.......", "......#..#......", "................", "................", "................"]
        for y, line in enumerate(glyph):
            for x, ch in enumerate(line):
                if ch == "#":
                    im.putpixel((x, y), (120, 255, 255, 255) if lamp else (60, 200, 220, 255))
        if lamp:
            for y in range(16):
                for x in range(16):
                    r, g, b, a = im.getpixel((x, y))
                    if (r, g, b) != (120, 255, 255):
                        im.putpixel((x, y), (min(255, r + 30), min(255, g + 40), min(255, b + 60), 255))
    return im


def main():
    tex = A / "textures/block"
    tex.mkdir(parents=True, exist_ok=True)
    bricks(1).save(tex / "arcane_bricks.png")
    bricks(2, rune=True).save(tex / "chiseled_arcane_bricks.png")
    bricks(3, lamp=True).save(tex / "rune_lamp.png")
    for name in ("arcane_bricks", "chiseled_arcane_bricks", "rune_lamp"):
        write(A / f"blockstates/{name}.json", {"variants": {"": {"model": f"arcane:block/{name}"}}})
        write(A / f"models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"arcane:block/{name}"}})
        write(A / f"models/item/{name}.json", {"parent": f"arcane:block/{name}"})
        write(D / f"loot_tables/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"arcane:{name}"}],
                                                                                     "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # receitas
    write(D / "recipes/arcane_bricks.json", {"type": "minecraft:crafting_shaped", "pattern": ["SS", "SA"],
                                             "key": {"S": {"item": "minecraft:stone_bricks"}, "A": {"item": "minecraft:amethyst_shard"}},
                                             "result": {"item": "arcane:arcane_bricks", "count": 4}})
    write(D / "recipes/chiseled_arcane_bricks.json", {"type": "minecraft:crafting_shapeless",
                                                      "ingredients": [{"item": "arcane:arcane_bricks"}, {"item": "arcane:common_ink"}],
                                                      "result": {"item": "arcane:chiseled_arcane_bricks"}})
    write(D / "recipes/rune_lamp.json", {"type": "minecraft:crafting_shaped", "pattern": [" B ", "BGB", " B "],
                                         "key": {"B": {"item": "arcane:arcane_bricks"}, "G": {"item": "minecraft:glowstone"}},
                                         "result": {"item": "arcane:rune_lamp", "count": 2}})
    write(D / "recipes/creature_pen.json", {"type": "minecraft:crafting_shaped", "pattern": [" F ", "LPL", " B "],
                                            "key": {"F": {"item": "minecraft:feather"}, "L": {"item": "minecraft:lead"},
                                                    "P": {"item": "arcane:arcane_pen"}, "B": {"item": "minecraft:bone"}},
                                            "result": {"item": "arcane:creature_pen"}})
    write(D / "recipes/perfect_compass.json", {"type": "minecraft:crafting_shaped", "pattern": ["DCD", "CMC", "DCD"],
                                               "key": {"D": {"item": "minecraft:diamond"}, "C": {"item": "arcane:arcane_compass"},
                                                       "M": {"item": "arcane:mana_crystal"}},
                                               "result": {"item": "arcane:perfect_compass"}})
    # tags: picareta
    mine = D.parent / "minecraft/tags/blocks/mineable/pickaxe.json"
    try:
        cur = json.loads(mine.read_text(encoding="utf-8"))
    except FileNotFoundError:
        cur = {"replace": False, "values": []}
    for name in ("arcane_bricks", "chiseled_arcane_bricks", "rune_lamp"):
        if f"arcane:{name}" not in cur["values"]:
            cur["values"].append(f"arcane:{name}")
    write(mine, cur)

    # ------------------------------------------------------------------ loot das salas
    def item(name, w, lo=1, hi=1, extra=None):
        e = {"type": "minecraft:item", "name": name, "weight": w}
        f = []
        if hi > 1:
            f.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
        if extra:
            f.extend(extra)
        if f:
            e["functions"] = f
        return e

    def glyphs(table, w):
        return {"type": "minecraft:loot_table", "name": f"arcane:chests/glyphs/{table}", "weight": w}

    ench = [{"function": "minecraft:enchant_with_levels", "levels": {"type": "minecraft:uniform", "min": 10, "max": 30}, "treasure": False}]
    rooms = {
        "library": (4, 7, [glyphs("stronghold_library", 6), item("arcane:arcane_page", 4, 1, 2), item("minecraft:book", 6, 1, 4),
                           item("minecraft:paper", 8, 2, 8), item("arcane:common_ink", 6, 1, 4), item("minecraft:enchanted_book", 2, 1, 1,
                           [{"function": "minecraft:enchant_randomly"}]), item("arcane:invisible_ink", 2, 1, 2), item("arcane:arcane_pen", 1)]),
        "alchemy": (4, 7, [item("arcane:mana_surge_potion", 5, 1, 2), item("arcane:mana_vessel_potion", 4, 1, 2), item("arcane:common_ink", 6, 1, 4),
                           item("arcane:blood_ink", 4, 1, 3), item("arcane:invisible_ink", 3, 1, 2), item("arcane:permanent_ink", 1),
                           item("arcane:solvent_powder", 4, 1, 4), item("minecraft:glass_bottle", 6, 1, 4), item("minecraft:blaze_powder", 3, 1, 3),
                           item("minecraft:nether_wart", 4, 1, 4), item("minecraft:ghast_tear", 1), item("minecraft:glowstone_dust", 5, 1, 6)]),
        "armory": (3, 6, [item("minecraft:iron_sword", 4, 1, 1, ench), item("minecraft:iron_chestplate", 3, 1, 1, ench),
                          item("minecraft:iron_helmet", 3, 1, 1, ench), item("minecraft:diamond_sword", 1, 1, 1, ench), item("minecraft:shield", 3),
                          item("minecraft:arrow", 6, 4, 16), item("minecraft:crossbow", 2, 1, 1, ench), item("arcane:insulating_cloak", 1),
                          item("arcane:arcane_mask", 1), item("arcane:weapon_pen", 2), item("minecraft:iron_ingot", 6, 2, 6)]),
        "crypt": (3, 6, [item("minecraft:bone", 10, 2, 8), item("minecraft:rotten_flesh", 8, 2, 6), item("arcane:blood_ink", 5, 1, 3),
                         item("minecraft:gold_ingot", 4, 1, 4), item("minecraft:emerald", 3, 1, 3), item("arcane:creature_pen", 1),
                         item("minecraft:name_tag", 2), item("minecraft:golden_apple", 1), glyphs("simple_dungeon", 3)]),
        "ritual": (3, 6, [item("arcane:arcane_chalk", 4, 1, 2), item("arcane:arcane_compass", 2), item("arcane:mana_crystal", 4, 1, 3),
                          glyphs("end_city_treasure", 4), item("arcane:arcane_page", 3, 1, 2), item("arcane:common_ink", 5, 2, 6),
                          item("minecraft:amethyst_shard", 5, 2, 8), item("arcane:symmetry_mirror", 1)]),
        "vault": (5, 8, [item("minecraft:diamond", 6, 1, 4), item("minecraft:emerald", 6, 2, 6), item("minecraft:gold_block", 2, 1, 2),
                         item("arcane:perfect_compass", 1), item("arcane:phoenix_pen", 1), item("arcane:permanent_ink", 3, 1, 2),
                         glyphs("bastion_treasure", 6), item("minecraft:enchanted_golden_apple", 1), item("arcane:scepter", 1),
                         item("arcane:arcane_page", 4, 1, 3), item("minecraft:diamond_chestplate", 1, 1, 1, ench)]),
    }
    for name, (lo, hi, entries) in rooms.items():
        write(D / f"loot_tables/chests/ruins/{name}.json", {"type": "minecraft:chest", "pools": [
            {"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": entries}]})
    print("ok")


if __name__ == "__main__":
    main()
