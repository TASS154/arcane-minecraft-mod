"""Arte do sistema de rituais: Altar Arcano (modelo 3D), Traco de Giz (normal e aceso, animado), Giz e Compasso Arcano.

Chamado por gen_textures.py. Tudo procedural (sem arquivos de terceiros).
"""
from __future__ import annotations

import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "arcane"
TEX = ASSETS / "textures"
T = (0, 0, 0, 0)

LAV = (150, 110, 220, 255)
LAV_L = (205, 165, 245, 255)
CYAN = (90, 220, 240, 255)
CYAN_D = (40, 150, 190, 255)
WHITE = (235, 250, 255, 255)
GOLD = (230, 190, 70, 255)
GOLD_D = (150, 110, 30, 255)
STONE = (66, 52, 86, 255)
STONE_D = (44, 34, 60, 255)
STONE_L = (92, 76, 116, 255)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


def save(img, rel):
    path = TEX / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("  ", path.relative_to(ASSETS.parents[4]))


def write_json(rel, obj):
    path = ASSETS / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


# ---------------------------------------------------------------- altar
def altar_side():
    img = Image.new("RGBA", (16, 16), STONE)
    rnd = random.Random(11)
    d = ImageDraw.Draw(img)
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade(STONE, 1 + rnd.uniform(-0.09, 0.09)))
    # tijolos: fiadas de 8 px, juntas escuras, deslocadas
    for row, y0 in enumerate((0, 8)):
        d.line([(0, y0), (15, y0)], fill=STONE_D)
        off = 0 if row == 0 else 4
        for x in range(off, 16, 8):
            d.line([(x, y0), (x, y0 + 7)], fill=STONE_D)
        d.line([(0, y0 + 1), (15, y0 + 1)], fill=STONE_L)
    # faixa dourada e runa
    d.line([(0, 14), (15, 14)], fill=GOLD_D)
    d.line([(0, 15), (15, 15)], fill=GOLD)
    for x, y in [(7, 5), (8, 5), (6, 6), (9, 6), (7, 7), (8, 7), (7, 4), (8, 4)]:
        img.putpixel((x, y), CYAN)
    img.putpixel((7, 6), WHITE)
    img.putpixel((8, 6), WHITE)
    return img


def altar_top():
    img = Image.new("RGBA", (16, 16), STONE_D)
    rnd = random.Random(12)
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade(STONE_D, 1 + rnd.uniform(-0.1, 0.1)))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), outline=GOLD_D)
    for cx, cy in ((1, 1), (14, 1), (1, 14), (14, 14)):
        img.putpixel((cx, cy), GOLD)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if abs(r - 6.0) < 0.6:
                img.putpixel((x, y), CYAN_D)
            elif abs(r - 3.4) < 0.55:
                img.putpixel((x, y), GOLD)
    for a in range(0, 360, 45):
        for r in (4.5, 5.0, 6.8):
            x = round(cx + math.cos(math.radians(a)) * r)
            y = round(cy + math.sin(math.radians(a)) * r)
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), CYAN if r > 6 else LAV_L)
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        img.putpixel((x, y), WHITE)
    return img


def altar_trim():
    img = Image.new("RGBA", (16, 16), GOLD)
    rnd = random.Random(13)
    for y in range(16):
        for x in range(16):
            f = 1.15 if y < 3 else (0.8 if y > 12 else 1.0)
            img.putpixel((x, y), shade(GOLD, f * (1 + rnd.uniform(-0.05, 0.05))))
    return img


def altar_assets():
    save(altar_side(), "block/arcane_altar_side.png")
    save(altar_top(), "block/arcane_altar_top.png")
    save(altar_trim(), "block/arcane_altar_trim.png")

    def box(frm, to, **faces):
        return {"from": frm, "to": to, "faces": faces}

    def all_sides(tex, top=None, bottom=None, uv=(0, 0, 16, 16)):
        f = {s: {"uv": list(uv), "texture": tex} for s in ("north", "south", "east", "west")}
        f["up"] = {"uv": [0, 0, 16, 16], "texture": top or tex}
        f["down"] = {"uv": [0, 0, 16, 16], "texture": bottom or tex, "cullface": "down"}
        return f

    write_json("models/block/arcane_altar.json", {
        "parent": "minecraft:block/block",
        "textures": {"side": "arcane:block/arcane_altar_side", "top": "arcane:block/arcane_altar_top",
                     "trim": "arcane:block/arcane_altar_trim", "gem": "arcane:block/mana_crystal",
                     "particle": "arcane:block/arcane_altar_side"},
        "elements": [
            {"from": [1, 0, 1], "to": [15, 3, 15], "faces": all_sides("#side", uv=(1, 0, 15, 3))},
            {"from": [3, 3, 3], "to": [13, 8, 13], "faces": all_sides("#side", uv=(3, 3, 13, 8))},
            {"from": [0.5, 8, 0.5], "to": [15.5, 9, 15.5], "faces": all_sides("#trim", uv=(0, 7, 16, 8))},
            {"from": [1, 9, 1], "to": [15, 12, 15], "faces": all_sides("#side", top="#top", uv=(1, 4, 15, 7))},
            {"from": [6.5, 12, 6.5], "to": [9.5, 15.5, 9.5], "faces": all_sides("#gem", uv=(4, 2, 12, 14))},
        ],
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, -1, 0], "scale": [0.62, 0.62, 0.62]},
            "ground": {"translation": [0, 3, 0], "scale": [0.4, 0.4, 0.4]},
            "fixed": {"rotation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]},
        },
    })
    write_json("blockstates/arcane_altar.json", {"variants": {"": {"model": "arcane:block/arcane_altar"}}})
    write_json("models/item/arcane_altar.json", {"parent": "arcane:block/arcane_altar"})


# ---------------------------------------------------------------- tracos de giz
def trace_img(color, core, alpha_halo=0, pulse=1.0):
    img = Image.new("RGBA", (16, 16), T)
    d = ImageDraw.Draw(img)
    c = shade(color, pulse)
    k = shade(core, pulse)
    if alpha_halo:
        halo = c[:3] + (alpha_halo,)
        for y in range(5, 11):
            d.line([(0, y), (15, y)], fill=halo)
        for x in range(5, 11):
            d.line([(x, 0), (x, 15)], fill=halo)
    for y in (7, 8):
        d.line([(0, y), (15, y)], fill=c)
    for x in (7, 8):
        d.line([(x, 0), (x, 15)], fill=c)
    # no central em losango
    for x, y in ((7, 5), (8, 5), (5, 7), (5, 8), (10, 7), (10, 8), (7, 10), (8, 10), (6, 6), (9, 6), (6, 9), (9, 9)):
        img.putpixel((x, y), k)
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        img.putpixel((x, y), WHITE if alpha_halo else LAV_L)
    return img


def trace_assets():
    save(trace_img(LAV, LAV_L), "block/arcane_trace.png")
    frames = [trace_img(CYAN, WHITE, alpha_halo=70, pulse=p) for p in (0.85, 1.0, 1.15, 1.0)]
    strip = Image.new("RGBA", (16, 16 * len(frames)), T)
    for i, f in enumerate(frames):
        strip.paste(f, (0, 16 * i))
    save(strip, "block/arcane_trace_lit.png")
    (TEX / "block" / "arcane_trace_lit.png.mcmeta").write_text(
        json.dumps({"animation": {"frametime": 3, "interpolate": True}}, indent=2) + "\n", encoding="utf-8")

    flat = lambda tex: {  # noqa: E731
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {"t": tex, "particle": tex},
        "elements": [{"from": [0, 0, 0], "to": [16, 0.2, 16], "shade": False,
                      "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#t"}}}],
    }
    write_json("models/block/arcane_trace.json", flat("arcane:block/arcane_trace"))
    write_json("models/block/arcane_trace_lit.json", flat("arcane:block/arcane_trace_lit"))
    write_json("blockstates/arcane_trace.json", {"variants": {
        "lit=false": {"model": "arcane:block/arcane_trace"},
        "lit=true": {"model": "arcane:block/arcane_trace_lit"}}})


# ---------------------------------------------------------------- giz e compasso (itens)
def chalk_item():
    img = Image.new("RGBA", (16, 16), T)
    d = ImageDraw.Draw(img)
    # bastao de giz na diagonal, com ponta que brilha
    for i in range(11):
        x, y = 3 + i, 12 - i
        for dx, dy, col in ((0, 0, (225, 220, 240, 255)), (1, 0, (190, 185, 215, 255)), (0, 1, (205, 200, 228, 255))):
            img.putpixel((x + dx, y + dy), col)
        img.putpixel((x - 1, y), (130, 120, 160, 255)) if i < 10 else None
    for x, y in ((13, 2), (14, 2), (13, 1), (14, 1)):
        img.putpixel((x, y), CYAN)
    img.putpixel((14, 1), WHITE)
    d.point((2, 13), fill=(120, 110, 150, 255))
    # riscos de po
    for x, y in ((1, 14), (3, 14), (2, 15)):
        img.putpixel((x, y), (190, 185, 215, 130))
    return img


def compass_item():
    img = Image.new("RGBA", (16, 16), T)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if r <= 7.2:
                if r > 6.0:
                    img.putpixel((x, y), GOLD if (x + y) % 5 else shade(GOLD, 1.2))
                elif r > 5.2:
                    img.putpixel((x, y), GOLD_D)
                else:
                    img.putpixel((x, y), (28, 18, 44, 255))
    for a in range(0, 360, 45):
        for r in (4.6,):
            x = round(cx + math.cos(math.radians(a)) * r)
            y = round(cy + math.sin(math.radians(a)) * r)
            img.putpixel((x, y), LAV if a % 90 else LAV_L)
    # agulha: ciano para o norte (cima), lilas para o sul
    for i in range(0, 5):
        img.putpixel((8, 7 - i), CYAN if i < 4 else WHITE)
        img.putpixel((7, 7 - i + 1), CYAN_D) if i < 3 else None
    for i in range(1, 4):
        img.putpixel((7, 8 + i), LAV)
    img.putpixel((7, 7), WHITE)
    img.putpixel((8, 7), WHITE)
    # argola
    for x, y in ((7, 0), (8, 0), (6, 1), (9, 1)):
        img.putpixel((x, y), GOLD_D)
    return img


def item_assets():
    save(chalk_item(), "item/arcane_chalk.png")
    save(compass_item(), "item/arcane_compass.png")
    for name in ("arcane_chalk", "arcane_compass"):
        write_json(f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"arcane:item/{name}"}})


def generate():
    altar_assets()
    trace_assets()
    item_assets()


if __name__ == "__main__":
    generate()
