"""Arte elaborada e efeitos visuais do Arcane (segunda passada). Chamado por gen_textures.py e SOBRESCREVE as
versoes simples de varios assets:

  - particulas por elemento (brasa, gelo, raio, veneno, forca, vazio, vida) com 4 quadros cada;
  - grimorio por estagio (4 capas) e pagina orbitante;
  - bancada arcana em 3D (livro aberto, vela, tinteiro, pena) e cristal de mana animado;
  - interfaces ornamentadas (bancada, grimorio com fita) e barra de mana;
  - itens redesenhados com luz consistente (canto superior esquerdo) e logo com o selo.

Tudo procedural, sem arquivos de terceiros.
"""
from __future__ import annotations

import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "arcane"
TEX = ASSETS / "textures"
RES = Path(__file__).resolve().parents[1] / "src" / "main" / "resources"
T = (0, 0, 0, 0)

K = (20, 12, 30, 255)
D = (45, 25, 70, 255)
P = (95, 50, 150, 255)
PL = (150, 90, 210, 255)
L = (205, 165, 245, 255)
C = (90, 220, 240, 255)
CD = (40, 150, 190, 255)
W = (235, 250, 255, 255)
G = (230, 190, 70, 255)
GD = (150, 110, 30, 255)
GL = (255, 230, 130, 255)
PARCH = (238, 224, 186, 255)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + ((c[3],) if len(c) > 3 else (255,))


def save(img, rel):
    path = TEX / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("  ", path.relative_to(ASSETS.parents[4]))


def write_json(rel, obj):
    path = ASSETS / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def sprite(rows, pal):
    h = len(rows)
    w = max(len(r) for r in rows)
    img = Image.new("RGBA", (w, h), T)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            img.putpixel((x, y), pal[ch])
    return img


# ================================================================== particulas por elemento
ELEMENTS = {
    # nome: (cor, nucleo, forma)
    "ember": ((255, 140, 40), (255, 235, 140), "spike"),
    "frost": ((150, 215, 255), (240, 252, 255), "flake"),
    "bolt": ((255, 235, 90), (255, 255, 235), "zig"),
    "venom": ((120, 235, 70), (220, 255, 170), "drop"),
    "force": ((180, 120, 255), (235, 215, 255), "ring"),
    "void": ((110, 60, 190), (245, 235, 255), "hole"),
    "life": ((255, 130, 190), (255, 235, 245), "plus"),
}


def particle_frame(kind, color, core, step):
    """Quadro 8x8 que encolhe e esmaece (0..3)."""
    img = Image.new("RGBA", (8, 8), T)
    s = 1.0 - step * 0.22
    a = int(255 * (1.0 - step * 0.18))
    c = color + (a,)
    k = core + (a,)

    def put(x, y, col):
        if 0 <= x < 8 and 0 <= y < 8:
            img.putpixel((x, y), col)

    cx = cy = 3.5
    for y in range(8):
        for x in range(8):
            r = math.hypot(x - cx, y - cy)
            if kind == "ring":
                if abs(r - 2.6 * s) < 0.7:
                    put(x, y, c)
                elif r < 1.0 * s:
                    put(x, y, k)
            elif kind == "hole":
                if r < 3.4 * s:
                    put(x, y, c if r > 1.4 * s else (30, 10, 60, a))
                if abs(r - 3.2 * s) < 0.5:
                    put(x, y, k)
            elif kind == "plus":
                if (abs(x - 3.5) < 0.9 and abs(y - 3.5) < 3.2 * s) or (abs(y - 3.5) < 0.9 and abs(x - 3.5) < 3.2 * s):
                    put(x, y, k if r < 1.5 else c)
            elif kind == "flake":
                ang = math.degrees(math.atan2(y - cy, x - cx)) % 60
                if r < 3.6 * s and (ang < 14 or ang > 46 or r < 1.2):
                    put(x, y, k if r < 1.6 else c)
            elif kind == "spike":
                if r < 3.3 * s and (abs(x - 3.5) + abs(y - 3.5) * 0.6 < 3.2 * s):
                    put(x, y, k if r < 1.4 else c)
            elif kind == "drop":
                if (y > 1 and r < 3.0 * s and y > 2.5 - 1.5 * s) or (y <= 2 and abs(x - 3.5) < (y + 0.5) * 0.6 * s):
                    put(x, y, k if (x in (3, 4) and y in (3, 4)) else c)
            elif kind == "zig":
                if abs(x - (3.5 + (2.0 if y % 4 < 2 else -2.0) * s)) < 0.9 and 0.5 < y < 7.5:
                    put(x, y, k if abs(x - 3.5) < 1.6 else c)
    return img


def particle_assets():
    names = []
    for name, (color, core, kind) in ELEMENTS.items():
        frames = []
        for i in range(4):
            img = particle_frame(kind, color, core, i)
            save(img, f"particle/{name}_{i}.png")
            frames.append(f"arcane:{name}_{i}")
        write_json(f"particles/arcane_{name}.json", {"textures": frames})
        names.append(name)
    return names


# ================================================================== grimorio por estagio (textura 64x32 do BookModel)
def grimoire_cover(stage):
    cover = {1: ((120, 75, 45), (80, 48, 28)), 2: ((80, 30, 110), (50, 18, 70)),
             3: ((70, 25, 100), (40, 14, 62)), 4: ((50, 18, 90), (25, 10, 55))}[stage]
    img = Image.new("RGBA", (64, 32), T)
    d = ImageDraw.Draw(img)
    rnd = random.Random(40 + stage)
    base, dark = cover
    d.rectangle((0, 0, 27, 9), fill=base)
    for y in range(0, 10):
        for x in range(0, 28):
            if rnd.random() < 0.2:
                img.putpixel((x, y), shade(base, 1 + rnd.uniform(-0.12, 0.1)))
    for x0 in (0, 16):
        d.rectangle((x0, 0, x0 + 11, 9), outline=GD if stage < 3 else G)
        if stage >= 2:
            d.rectangle((x0 + 4, 3, x0 + 7, 6), fill=CD)
            img.putpixel((x0 + 5, 4), W)
        if stage >= 3:
            for cx, cy in ((x0 + 1, 1), (x0 + 10, 1), (x0 + 1, 8), (x0 + 10, 8)):
                img.putpixel((cx, cy), GL)
        if stage >= 4:
            for x in range(x0 + 2, x0 + 10, 2):
                img.putpixel((x, 2), C)
                img.putpixel((x, 7), C)
    d.rectangle((12, 0, 15, 9), fill=dark)
    # miolo (paginas)
    d.rectangle((0, 10, 33, 18), fill=PARCH)
    for x in range(34):
        for y in range(10, 19):
            if rnd.random() < 0.2:
                img.putpixel((x, y), shade(PARCH, 0.93))
    for x in range(1, 33, 3):
        img.putpixel((x, 13), PL if stage < 4 else C)
        img.putpixel((x + 1, 15), CD if stage < 4 else L)
    return img


def grimoire_page():
    """Pagina solta (32x32): pergaminho com linhas de runas luminosas."""
    img = Image.new("RGBA", (32, 32), T)
    d = ImageDraw.Draw(img)
    rnd = random.Random(55)
    d.rectangle((4, 2, 27, 29), fill=PARCH, outline=(150, 120, 80, 255))
    for y in range(3, 29):
        for x in range(5, 27):
            if rnd.random() < 0.15:
                img.putpixel((x, y), shade(PARCH, 0.94))
    for row, y in enumerate(range(6, 27, 4)):
        x = 7
        while x < 24:
            w = rnd.randint(2, 5)
            col = C if (row + x) % 3 == 0 else PL
            d.line([(x, y), (min(x + w, 24), y)], fill=col)
            x += w + 2
    d.polygon([(4, 29), (9, 29), (4, 24)], fill=shade(PARCH, 0.85))
    return img


def grimoire_assets():
    for stage in (1, 2, 3, 4):
        save(grimoire_cover(stage), f"entity/grimoire_s{stage}.png")
    save(grimoire_cover(2), "entity/grimoire.png")
    save(grimoire_page(), "entity/grimoire_page.png")


# ================================================================== bancada 3D e cristal animado
def wb_wood(dark=False):
    base = (88, 58, 38, 255) if not dark else (60, 38, 26, 255)
    img = Image.new("RGBA", (16, 16), base)
    rnd = random.Random(70 if not dark else 71)
    d = ImageDraw.Draw(img)
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade(base, 1 + rnd.uniform(-0.07, 0.07)))
    for y in (3, 7, 11, 15):
        d.line([(0, y), (15, y)], fill=shade(base, 0.72))
    for x, y in ((3, 1), (11, 5), (6, 9), (13, 13)):
        img.putpixel((x, y), shade(base, 0.6))
    return img


def wb_props():
    book = sprite([
        "kkkkkkkkkkkkkkkk", "kPPPPPPPPPPPPPPk", "kPddddddddddddPk", "kPdGGGGGGGGGGdPk", "kPdGppppppppGdPk",
        "kPdGpCCCCCCpGdPk", "kPdGpCWWWWCpGdPk", "kPdGpCWWWWCpGdPk", "kPdGpCCCCCCpGdPk", "kPdGppppppppGdPk",
        "kPdGGGGGGGGGGdPk", "kPddddddddddddPk", "kPPPPPPPPPPPPPPk", "kyyyyyyyyyyyyyyk", "kkkkkkkkkkkkkkkk", "kkkkkkkkkkkkkkkk"],
        {"k": K, "P": PL, "d": D, "G": G, "p": P, "C": C, "W": W, "y": PARCH})
    candle = Image.new("RGBA", (16, 16), (240, 232, 205, 255))
    for y in range(16):
        for x in range(16):
            candle.putpixel((x, y), shade((240, 232, 205, 255), 1.08 if x < 4 else (0.86 if x > 11 else 1.0)))
    flame = Image.new("RGBA", (16, 16), T)
    d = ImageDraw.Draw(flame)
    d.ellipse((5, 2, 10, 13), fill=(255, 150, 40, 255))
    d.ellipse((6, 5, 9, 12), fill=(255, 225, 120, 255))
    d.ellipse((7, 8, 8, 12), fill=(255, 255, 230, 255))
    ink = Image.new("RGBA", (16, 16), (24, 18, 60, 255))
    d = ImageDraw.Draw(ink)
    d.rectangle((1, 1, 14, 14), outline=(60, 50, 120, 255))
    d.line([(3, 3), (3, 12)], fill=(150, 150, 230, 255))
    d.rectangle((5, 5, 10, 7), fill=(70, 70, 170, 255))
    quill = Image.new("RGBA", (16, 16), (235, 232, 245, 255))
    d = ImageDraw.Draw(quill)
    d.line([(8, 0), (8, 15)], fill=(150, 140, 175, 255))
    for y in range(1, 15, 2):
        d.line([(8, y), (8 - 6 + y % 3, y + 1)], fill=(205, 200, 225, 255))
        d.line([(8, y), (8 + 6 - y % 3, y + 1)], fill=(205, 200, 225, 255))
    return book, candle, flame, ink, quill


def workbench_assets():
    save(wb_wood(), "block/arcane_workbench_wood.png")
    save(wb_wood(True), "block/arcane_workbench_wood_dark.png")
    for name, img in zip(("book", "candle", "flame", "ink", "quill"), wb_props()):
        save(img, f"block/arcane_workbench_{name}.png")

    def box(frm, to, tex, uv=None):
        f = {s: {"uv": uv or [0, 0, 16, 16], "texture": tex} for s in ("north", "south", "east", "west", "up", "down")}
        return {"from": frm, "to": to, "faces": f}

    elements = [
        # tampo com a toalha roxa por cima (topo), laterais de madeira
        {"from": [0, 11, 0], "to": [16, 13, 16], "faces": {
            "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
            "down": {"uv": [0, 0, 16, 16], "texture": "#wood"},
            **{s: {"uv": [0, 0, 16, 2], "texture": "#side"} for s in ("north", "south", "east", "west")}}},
        # pernas
        box([0.5, 0, 0.5], [3.5, 11, 3.5], "#dark", [0, 0, 3, 11]),
        box([12.5, 0, 0.5], [15.5, 11, 3.5], "#dark", [0, 0, 3, 11]),
        box([0.5, 0, 12.5], [3.5, 11, 15.5], "#dark", [0, 0, 3, 11]),
        box([12.5, 0, 12.5], [15.5, 11, 15.5], "#dark", [0, 0, 3, 11]),
        # prateleira
        box([3.5, 3, 3.5], [12.5, 4.5, 12.5], "#wood", [0, 0, 9, 2]),
        # livro aberto (capa + miolo), vela, chama, tinteiro, pena
        box([3, 13, 4], [9, 14, 12], "#book", [0, 0, 16, 16]),
        box([3.4, 14, 4.4], [8.6, 14.6, 11.6], "#top", [4, 4, 12, 12]),
        box([11.5, 13, 2.5], [13.5, 16, 4.5], "#candle", [0, 0, 4, 8]),
        {"from": [12.1, 16, 3.1], "to": [12.9, 17.6, 3.9], "faces": {
            s: {"uv": [5, 2, 11, 13], "texture": "#flame"} for s in ("north", "south", "east", "west")}},
        box([11, 13, 9.5], [14, 15, 12.5], "#ink", [0, 0, 16, 16]),
        {"from": [9.4, 13, 6], "to": [10.4, 13.6, 13], "rotation": {"origin": [10, 13, 9], "axis": "y", "angle": 22.5},
         "faces": {"up": {"uv": [7, 0, 9, 16], "texture": "#quill"}, "down": {"uv": [7, 0, 9, 16], "texture": "#quill"}}},
    ]
    write_json("models/block/arcane_workbench.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {"top": "arcane:block/arcane_workbench_top", "side": "arcane:block/arcane_workbench_side",
                     "wood": "arcane:block/arcane_workbench_wood", "dark": "arcane:block/arcane_workbench_wood_dark",
                     "book": "arcane:block/arcane_workbench_book", "candle": "arcane:block/arcane_workbench_candle",
                     "flame": "arcane:block/arcane_workbench_flame", "ink": "arcane:block/arcane_workbench_ink",
                     "quill": "arcane:block/arcane_workbench_quill", "particle": "arcane:block/arcane_workbench_side"},
        "elements": elements,
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, -1.5, 0], "scale": [0.56, 0.56, 0.56]},
            "ground": {"translation": [0, 3, 0], "scale": [0.4, 0.4, 0.4]},
            "fixed": {"scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
        },
    })


def crystal_animated():
    """Cristal com pulso de brilho: 4 quadros empilhados (16x64) + mcmeta."""
    frames = []
    for i, pulse in enumerate((0.92, 1.0, 1.12, 1.0)):
        img = Image.new("RGBA", (16, 16))
        rnd = random.Random(7)
        for y in range(16):
            for x in range(16):
                band = (x + y) % 6
                base = [CD, C, C, CD, (30, 110, 160, 255), CD][band]
                img.putpixel((x, y), shade(base, (1 + rnd.uniform(-0.06, 0.06)) * pulse))
        sparks = [(3, 2), (4, 3), (10, 5), (11, 6), (6, 11), (13, 12), (2, 9)]
        for j, (x, y) in enumerate(sparks):
            if (j + i) % 2 == 0:
                img.putpixel((x, y), W)
        for x, y in [(8, 8), (7, 8), (8, 7), (12, 2), (2, 13)]:
            img.putpixel((x, y), shade(L, pulse))
        frames.append(img)
    strip = Image.new("RGBA", (16, 64))
    for i, f in enumerate(frames):
        strip.paste(f, (0, 16 * i))
    save(strip, "block/mana_crystal.png")
    (TEX / "block" / "mana_crystal.png.mcmeta").write_text(
        json.dumps({"animation": {"frametime": 6, "interpolate": True}}, indent=2) + "\n", encoding="utf-8")


# ================================================================== interfaces ornamentadas
def filigree(d, x, y, flip_x=False, flip_y=False, color=G):
    """Ornamento de canto 7x7."""
    pts = [(0, 0), (1, 0), (2, 0), (3, 0), (0, 1), (0, 2), (0, 3), (1, 1), (2, 2), (4, 0), (0, 4), (3, 1), (1, 3)]
    for px, py in pts:
        xx = x + (6 - px if flip_x else px)
        yy = y + (6 - py if flip_y else py)
        d.point((xx, yy), fill=color)


def border_gold(d, w, h):
    d.rectangle((0, 0, w - 1, h - 1), outline=K)
    d.rectangle((1, 1, w - 2, h - 2), outline=GD)
    d.rectangle((2, 2, w - 3, h - 3), outline=G)
    d.rectangle((3, 3, w - 4, h - 4), outline=GD)
    filigree(d, 4, 4, False, False, GL)
    filigree(d, w - 11, 4, True, False, GL)
    filigree(d, 4, h - 11, False, True, GL)
    filigree(d, w - 11, h - 11, True, True, GL)


def inset(d, box, base=(18, 10, 28, 255)):
    x0, y0, x1, y1 = box
    d.rectangle(box, fill=base)
    d.line([(x0, y0), (x1, y0)], fill=(8, 4, 12, 255))
    d.line([(x0, y0), (x0, y1)], fill=(8, 4, 12, 255))
    d.line([(x0, y1), (x1, y1)], fill=(130, 90, 180, 255))
    d.line([(x1, y0), (x1, y1)], fill=(130, 90, 180, 255))
    d.line([(x0 + 1, y1 - 1), (x1 - 1, y1 - 1)], fill=(80, 50, 120, 255))


def workbench_gui():
    img = Image.new("RGBA", (256, 256), T)
    d = ImageDraw.Draw(img)
    W_, H_ = 240, 222
    rnd = random.Random(90)
    # fundo: roxo profundo com gradiente vertical e granulado
    for y in range(H_):
        f = 1.12 - 0.3 * y / H_
        for x in range(W_):
            img.putpixel((x, y), shade((58, 34, 86, 255), f * (1 + rnd.uniform(-0.03, 0.03))))
    border_gold(d, W_, H_)
    # titulo
    d.rectangle((8, 7, 112, 22), fill=(36, 20, 56, 255), outline=GD)
    for x in range(12, 108, 6):
        d.point((x, 8), fill=GD)
    inset(d, (8, 30, 113, 163))
    inset(d, (120, 30, 231, 135))
    inset(d, (120, 140, 231, 163))
    # runas esparsas no fundo dos recuos
    for box in [(9, 31, 112, 162), (121, 31, 230, 134), (121, 141, 230, 162)]:
        for _ in range(55):
            x = rnd.randint(box[0], box[2])
            y = rnd.randint(box[1], box[3])
            img.putpixel((x, y), (40, 26, 60, 255))
            if rnd.random() < 0.3:
                img.putpixel((x + 1, y), (32, 20, 52, 255))
    # divisor com losango no rodape
    d.line([(8, 192), (231, 192)], fill=GD)
    d.line([(8, 193), (231, 193)], fill=(36, 20, 56, 255))
    for dx in (-2, -1, 0, 1, 2):
        for dy in range(-(2 - abs(dx)), 3 - abs(dx)):
            img.putpixel((119 + dx, 192 + dy), G)
    # slots do seletor (normal, hover, selecionado)
    for i, (f, o, hi) in enumerate([((40, 25, 60, 255), (110, 80, 150, 255), (70, 50, 100, 255)),
                                     ((70, 45, 105, 255), C, (120, 90, 170, 255)),
                                     ((60, 35, 90, 255), G, (110, 80, 150, 255))]):
        x = i * 20
        d.rectangle((x, 230, x + 17, 247), fill=f, outline=o)
        d.line([(x + 1, 231), (x + 16, 231)], fill=hi)
        d.line([(x + 1, 231), (x + 1, 246)], fill=hi)
    return img


def grimoire_gui():
    """Grimorio 'arcano luminoso': capa indigo com filete dourado, paginas escuras com runas tenues e lombada luminosa."""
    img = Image.new("RGBA", (256, 256), T)
    d = ImageDraw.Draw(img)
    rnd = random.Random(77)
    # capa: indigo profundo com gradiente e granulado
    for y in range(200):
        for x in range(256):
            f = 1.08 - 0.28 * y / 200 + rnd.uniform(-0.05, 0.05)
            img.putpixel((x, y), shade((34, 20, 62, 255), f))
    d.rectangle((0, 0, 255, 199), outline=K)
    d.rectangle((2, 2, 253, 197), outline=GD)
    for x in range(6, 250, 4):
        d.point((x, 4), fill=GL)
        d.point((x, 195), fill=GL)
    for y in range(6, 195, 4):
        d.point((4, y), fill=GL)
        d.point((251, y), fill=GL)
    for cx, cy in ((3, 3), (252, 3), (3, 196), (252, 196)):
        d.rectangle((cx - 2, cy - 2, cx + 2, cy + 2), fill=G, outline=GD)
    PAGE = (24, 18, 46, 255)
    for x0, x1 in ((12, 124), (132, 244)):
        d.rectangle((x0, 24, x1, 172), fill=PAGE, outline=(90, 60, 150, 255))
        for y in range(25, 172):
            for x in range(x0 + 1, x1):
                edge = min(x - x0, x1 - x, y - 24, 172 - y)
                f = 0.78 + 0.22 * min(1.0, edge / 12.0)
                img.putpixel((x, y), shade(PAGE, f * (1 + rnd.uniform(-0.04, 0.04)) + (0.0 if edge > 1 else 0.0)))
        # brilho ciano suave na borda interna (como tinta luminosa)
        for i, glow in enumerate(((52, 132, 182, 255), (38, 92, 142, 255), (30, 62, 112, 255))):
            d.rectangle((x0 + 1 + i, 25 + i, x1 - 1 - i, 171 - i), outline=glow)
        # runas tenues espalhadas
        for _ in range(26):
            rx, ry = rnd.randint(x0 + 6, x1 - 8), rnd.randint(30, 166)
            glow = (110, 90, 200, 255) if rnd.random() < 0.7 else (70, 190, 225, 255)
            for k in range(rnd.randint(2, 4)):
                if rnd.random() < 0.5:
                    d.line([(rx, ry + k), (rx + rnd.randint(1, 3), ry + k)], fill=glow)
                else:
                    d.line([(rx + k, ry), (rx + k, ry + rnd.randint(1, 3))], fill=glow)
        for y in range(39, 172, 12):
            d.line([(x0 + 3, y), (x1 - 3, y)], fill=(44, 34, 84, 255))
    for i in range(8):
        f = 0.9 - i * 0.04
        for y in range(25, 172):
            for x in (124 - i, 132 + i):
                r, g, b, _ = img.getpixel((x, y))
                img.putpixel((x, y), shade((r, g, b, 255), f))
    d.rectangle((125, 24, 131, 172), fill=(22, 14, 40, 255))
    d.line([(128, 24), (128, 172)], fill=(90, 220, 240, 255))  # lombada luminosa
    d.line([(127, 24), (127, 172)], fill=(40, 120, 160, 255))
    d.line([(129, 24), (129, 172)], fill=(40, 120, 160, 255))
    # fita de marcador caindo do topo, pela lombada
    d.polygon([(126, 168), (130, 168), (130, 186), (128, 182), (126, 186)], fill=(176, 36, 54, 255))
    d.line([(126, 168), (126, 185)], fill=(120, 20, 36, 255))
    return img


def mana_bar():
    img = Image.new("RGBA", (128, 32), T)
    d = ImageDraw.Draw(img)
    # moldura vazia 0,0 (102x10): contorno escuro, capas douradas, marcas de escala
    d.rectangle((0, 0, 101, 9), fill=(14, 9, 24, 215), outline=K)
    d.line([(1, 1), (100, 1)], fill=(44, 32, 66, 230))
    for x in range(10, 100, 10):
        d.line([(x, 2), (x, 7)], fill=(30, 22, 45, 230))
    for x in (0, 1, 100, 101):
        d.line([(x, 0), (x, 9)], fill=GD)
    d.line([(0, 0), (101, 0)], fill=GD)
    d.line([(0, 9), (101, 9)], fill=(90, 62, 20, 255))
    # preenchimento 0,10 (100x8): ciano -> lilas com brilho no topo
    for x in range(100):
        t = x / 99
        col = tuple(int(CD[i] + (PL[i] - CD[i]) * t) for i in range(3)) + (255,)
        for y in range(8):
            f = 1.35 if y == 0 else (1.15 if y < 3 else (0.78 if y > 5 else 1.0))
            if (x + y * 3) % 17 == 0:
                f *= 1.25
            img.putpixel((x, 10 + y), shade(col, f))
    # preenchimento envenenado 0,20
    for x in range(100):
        for y in range(8):
            f = 1.35 if y == 0 else (1.1 if y < 3 else (0.8 if y > 5 else 1.0))
            if (x + y * 2) % 11 == 0:
                f *= 1.3
            img.putpixel((x, 20 + y), shade((60, 110, 255, 255), f))
    gem = sprite([
        "....k....", "...kCk...", "..kCWCk..", ".kCCWCck.", "kCCCCCcck", "kcCCCCcck", ".kcCCcck.", "..kccck..", "...kkk..."],
        {"k": K, "C": C, "W": W, "c": CD, ".": T})
    img.paste(gem, (104, 0), gem)
    return img


# ================================================================== itens redesenhados
def items():
    pal = {".": T, "k": K, "d": D, "p": P, "P": PL, "L": L, "C": C, "c": CD, "W": W, "G": G, "g": GD, "y": PARCH,
           "o": (170, 140, 95, 255), "s": (205, 215, 225, 255), "S": (150, 160, 175, 255), "w": (110, 70, 40, 255),
           "b": (70, 42, 25, 255), "t": (40, 90, 95, 255), "T": (70, 140, 140, 255), "u": (120, 190, 180, 255),
           "R": (190, 30, 40, 255), "r": (110, 10, 20, 255), "N": (40, 60, 150, 255), "n": (20, 30, 80, 255),
           "l": GL, "h": (255, 255, 255, 200), "E": (50, 220, 120, 255)}
    out = {}
    out["arcane_pen"] = sprite([
        "..............lW", ".............lWW.", "............lPWL.", "...........lPPLL.", "..........lPPPLp.",
        ".........lPPpPpp.", "........lPPpPppd.", ".......lPPpPpdd..", "......lPPpPpdd...", ".....lPPpPpdd....",
        "....kPpPpddk.....", "...kGgpdkk.......", "..kGGGgk.........", ".kGlGgk..........", "kCcGk............", "kWck............."], pal)
    out["glyph_note"] = sprite([
        "................", "..kkkkkkkkkkkk..", ".kyYyyyyyyyyyok.", ".kyyyPPPPyyyyok.", ".kyyPyyyyPyyyok.",
        ".kyyPyCCyPyyyok.", ".kyyPyCWCyPyyok.", ".kyyPyyCCyPyyok.", ".kyyyPPPPyyyyok.", ".kyoyyyyyyyyyok.",
        ".kyyoooyyRRyyok.", ".kyyyyyyRrrRyok.", ".kooooooooRrRok.", "..kkkkkkkkkrrk..", "...............k", "................"],
        {**pal, "Y": (245, 232, 195, 255)})
    out["arcane_mask"] = sprite([
        "................", "...kkkkkkkkkk...", "..kddPPPPPPddk..", ".kdPPppppppPPdk.", ".kPppppppppppPk.",
        ".kpplCCkppkCClpk", ".kpkWCCkppkWCCkp", ".kppkkkppppkkkpk", "..kppppPGGPpppk.", "..kdppplGGlppdk.",
        "...kdppPllPppdk.", "....kddpppddk...", ".....kkddddkk...", ".......kkkk.....", "................", "................"], pal)
    out["insulating_cloak"] = sprite([
        "................", "....kkkkkkkk....", "...kuTTGGTTuk...", "..kuTTTlGGTTTk..", ".kTTTTtkkktTTTk.",
        ".kTTTttkkkttTTk.", "kTTuTtttttttTuTk", "kTTTTtttCCtttTTk", "kTTTTttCWCttTTTk", "kTTuTttttttttuTk",
        "kTTTTttttttttTTk", "kTtTTtttttttTtTk", "kttTTtttttttTttk", ".kttttttttttttk.", "..kkttkkkktkk...", "....kk....kk...."], pal)
    out["glyph_projector"] = sprite([
        "................", ".....kkkkkk.....", "....kCWWWWCk....", "...kCCWWWWCCk...", "..kcCCCWWCCCck..",
        "...kcCCCCCCck...", "....kcCCCCck....", ".....kGllGk.....", "....kGgSSSSgGk..", "...kgSsssssSgk..",
        "...kgSsPPPsSgk..", "...kgSsPPPsSgk..", "...kgSsssssSgk..", "....kGgSSSSgGk..", "...kbwwwwwwwbk..", "...kkkkkkkkkkkk."], pal)
    out["symmetry_mirror"] = sprite([
        "................", ".....kkkkkk.....", "...kkGGllGGkk...", "..kGGgWWWWgGGk..", ".kGgsWWsssCCgGk.",
        ".kGgWWsssssCgGk.", ".kGgWsssssCCgGk.", ".kGgsssssCCsgGk.", ".kGgssssCCssgGk.", ".kGgsssCCsssgGk.",
        "..kGggsCsssgGk..", "...kkGGGGGGkk...", ".....kGGGGk.....", "......kwbk......", "......kwbk......", ".....kGGGGk....."], pal)

    def bottle(light, dark, glow=None):
        rows = [
            "................", "......kkkk......", "......kwwk......", "......kbbk......", ".....khssh k.....".replace(" ", ""),
            ".....kssssk.....", "....ks#h#sk.....".replace("h", "h"), "...ks##h##sk....", "..ks#####%%sk...",
            "..ks######%%sk..", "..ks#######%sk..", "..ks%%####%%sk..", "..ks%%%%%%%%sk..", "...ks%%%%%%sk...",
            "....kkkkkkkk....", "................"]
        rows = [r[:16].ljust(16, ".") for r in rows]
        p2 = {**pal, "#": light, "%": dark, "h": (255, 255, 255, 220)}
        img = sprite(rows, p2)
        if glow:
            for x, y in ((5, 8), (9, 10), (7, 11)):
                img.putpixel((x, y), glow)
        return img
    out["common_ink"] = bottle((40, 60, 160, 255), (20, 30, 90, 255), (140, 160, 255, 255))
    out["blood_ink"] = bottle((200, 30, 40, 255), (110, 10, 20, 255), (255, 140, 140, 255))
    out["invisible_ink"] = bottle((205, 235, 255, 200), (150, 190, 230, 150), (255, 255, 255, 255))
    return out


def item_assets():
    for name, img in items().items():
        save(img, f"item/{name}.png")


# ================================================================== logo com o selo
def logo():
    size = 128
    img = Image.new("RGBA", (size, size), (22, 12, 36, 255))
    d = ImageDraw.Draw(img)
    cx = cy = size / 2
    rnd = random.Random(5)
    for _ in range(90):  # poeira estelar
        img.putpixel((rnd.randrange(size), rnd.randrange(size)), (120 + rnd.randrange(100), 100, 200, 255))
    for r, col in ((62, D), (58, P), (55, (22, 12, 36, 255))):
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=col)
    # aneis encaixados, do menor ao maior: cada glifo envolve o anterior
    shapes = [(3, (30, 130, 170), -90, 12), (28, (190, 70, 40), 0, 20), (4, (70, 140, 60), 15, 28),
              (4, (170, 120, 20), 45, 36), (5, (110, 60, 170), 0, 44), (6, (90, 100, 115), 30, 52)]
    for sides, color, rot, rad in shapes:
        pts = [(cx + math.cos(math.radians(rot + 360 * i / sides)) * rad, cy + math.sin(math.radians(rot + 360 * i / sides)) * rad)
               for i in range(sides)]
        d.line(pts + [pts[0]], fill=color + (255,), width=2)
        for px, py in pts:
            d.ellipse((px - 1.5, py - 1.5, px + 1.5, py + 1.5), fill=G)
    d.ellipse((cx - 5, cy - 5, cx + 5, cy + 5), fill=W)
    d.ellipse((cx - 3, cy - 3, cx + 3, cy + 3), fill=C)
    for a in range(0, 360, 30):
        x = cx + math.cos(math.radians(a)) * 60
        y = cy + math.sin(math.radians(a)) * 60
        d.rectangle((x - 1.5, y - 1.5, x + 1.5, y + 1.5), fill=GL)
    img.save(RES / "arcane_logo.png")
    print("   src/main/resources/arcane_logo.png")


# ================================================================== particulas de elemento (Fx), em tons de cinza
# Familias novas: suaves aditivas, riscos e fumaca com ruido, e uma por elemento novo (terra, agua, luz, sombra, sangue,
# tempo, natureza, som). Sao brancas/cinzas: a cor do elemento e multiplicada no jogo. Desenhadas 4x maiores e reduzidas
# (bordas suaves). Decais: marcas de 32 px deitadas no bloco atingido.
OLD_FAMILIES = {"spark": "arcane_spark", "ember": "ember", "frost": "frost", "bolt": "bolt", "venom": "venom",
                "force": "force", "void": "void", "life": "life"}
SS = 4  # supersampling


def _canvas(size):
    return Image.new("L", (size * SS, size * SS), 0)


def _finish(mask, size, alpha_curve=1.0):
    """Mascara em escala de cinza -> RGBA claro com alfa (reduzida com filtro)."""
    small = mask.resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = out.load()
    for y in range(size):
        for x in range(size):
            a = small.getpixel((x, y)) / 255.0
            if a <= 0.01:
                continue
            a = a ** alpha_curve
            v = int(170 + 85 * a)  # nucleo mais claro que a borda
            px[x, y] = (v, v, v, int(255 * a))
    return out


def _noise(size, seed, octaves=3):
    rnd = random.Random(seed)
    base = [[rnd.random() for _ in range(size + 1)] for _ in range(size + 1)]

    def sample(x, y, cell):
        gx, gy = x / cell, y / cell
        x0, y0 = int(gx), int(gy)
        fx, fy = gx - x0, gy - y0

        def g(i, j):
            return base[(y0 + j) % size][(x0 + i) % size]
        top = g(0, 0) * (1 - fx) + g(1, 0) * fx
        bot = g(0, 1) * (1 - fx) + g(1, 1) * fx
        return top * (1 - fy) + bot * fy

    out = [[0.0] * size for _ in range(size)]
    for y in range(size):
        for x in range(size):
            v, amp, cell, tot = 0.0, 1.0, size / 2, 0.0
            for _ in range(octaves):
                v += sample(x, y, max(1.0, cell)) * amp
                tot += amp
                amp *= 0.5
                cell /= 2
            out[y][x] = v / tot
    return out


def fx_soft(step, size=16):
    m = _canvas(size)
    d = ImageDraw.Draw(m)
    c = size * SS / 2
    r = c * (0.95 - step * 0.15)
    for i in range(24, 0, -1):  # gradiente radial
        rr = r * i / 24
        d.ellipse((c - rr, c - rr, c + rr, c + rr), fill=int(255 * (1 - i / 24) ** 1.6))
    return _finish(m, size)


def fx_streak(step, size=16):
    m = _canvas(size)
    d = ImageDraw.Draw(m)
    c = size * SS / 2
    length = c * (0.95 - step * 0.2)
    for i in range(12, 0, -1):
        w = c * 0.22 * i / 12
        d.ellipse((c - w, c - length * i / 12 - w, c + w, c + length * i / 12 + w), fill=int(255 * (1 - i / 12) ** 1.3))
    return _finish(m, size)


def fx_smoke(step, size=32, seed=7):
    n = _noise(size, seed + step * 13)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            r = math.hypot(x - c, y - c) / (size / 2)
            nv = n[y][x]
            fall = max(0.0, 1.0 - r * (1.0 + step * 0.12) - (0.5 - nv) * 0.9)
            v = min(1.0, fall * 1.6) * (0.35 + 1.1 * max(0.0, nv - 0.25)) * (1.0 - step * 0.18)
            a = max(0, min(255, int(255 * v)))
            if a > 3:
                out.putpixel((x, y), (215, 215, 215, a))
    return out


def fx_shape(kind, step, size=16, seed=0):
    """Familias dos elementos novos, 4 quadros (0 a 3) que encolhem e se desfazem."""
    m = _canvas(size)
    d = ImageDraw.Draw(m)
    S = size * SS
    c = S / 2
    k = 1.0 - step * 0.17
    rnd = random.Random(seed * 31 + step)
    if kind == "earth":  # lasca de pedra angulosa
        pts = []
        for i in range(5):
            a = i / 5 * math.tau + rnd.uniform(-0.45, 0.45) + step * 0.7
            rr = c * 0.85 * k * rnd.uniform(0.45, 1.0)
            pts.append((c + math.cos(a) * rr, c + math.sin(a) * rr))
        d.polygon(pts, fill=150)
        d.polygon([pts[0], pts[1], pts[2], (c, c)], fill=225)
    elif kind == "water":  # gota com brilho
        r = c * 0.55 * k
        d.ellipse((c - r, c - r * 0.6, c + r, c + r * 1.4), fill=170)
        d.polygon([(c - r * 0.7, c), (c + r * 0.7, c), (c, c - r * 1.6)], fill=170)
        d.ellipse((c - r * 0.45, c - r * 0.2, c - r * 0.05, c + r * 0.25), fill=255)
    elif kind == "light":  # brilho de 4 pontas
        for ang, ln in ((0, 0.95), (math.pi / 2, 0.95), (math.pi / 4, 0.5), (3 * math.pi / 4, 0.5)):
            dx, dy = math.cos(ang) * c * ln * k, math.sin(ang) * c * ln * k
            d.line([(c - dx, c - dy), (c + dx, c + dy)], fill=230, width=int(SS * 1.5))
        r = c * 0.25 * k
        d.ellipse((c - r, c - r, c + r, c + r), fill=255)
    elif kind == "shadow":  # fiapo de fumaca escura
        for i in range(5):
            a = rnd.uniform(0, math.tau)
            rr = c * rnd.uniform(0.15, 0.45) * k
            ox, oy = math.cos(a) * c * 0.35, math.sin(a) * c * 0.35
            d.ellipse((c + ox - rr, c + oy - rr, c + ox + rr, c + oy + rr), fill=int(150 + 60 * rnd.random()))
    elif kind == "blood":  # gota redonda com cauda
        r = c * 0.45 * k
        d.ellipse((c - r, c - r + r * 0.6, c + r, c + r + r * 0.6), fill=220)
        d.polygon([(c - r * 0.5, c + r * 0.4), (c + r * 0.5, c + r * 0.4), (c + r * 0.1, c - r * 1.4)], fill=220)
    elif kind == "time":  # anel com marcas de relogio e um ponteiro que gira
        r = c * 0.78 * k
        d.ellipse((c - r, c - r, c + r, c + r), outline=230, width=int(SS * 1.2))
        for i in range(12):
            a = i / 12 * math.tau
            r1, r2 = r * (0.62 if i % 3 == 0 else 0.78), r * 0.92
            d.line([(c + math.cos(a) * r1, c + math.sin(a) * r1), (c + math.cos(a) * r2, c + math.sin(a) * r2)], fill=230, width=SS)
        a = step * 0.9
        d.line([(c, c), (c + math.cos(a) * r * 0.7, c + math.sin(a) * r * 0.7)], fill=255, width=int(SS * 1.4))
    elif kind == "nature":  # folha girando
        r = c * 0.8 * k
        ang = step * 0.6
        pts = [(i / 23, math.sin(i / 23 * math.pi) * r * 0.45) for i in range(24)]
        poly = [(c + (t - 0.5) * 2 * r, c - w) for t, w in pts] + [(c + (t - 0.5) * 2 * r, c + w) for t, w in reversed(pts)]
        rot = [(c + (x - c) * math.cos(ang) - (y - c) * math.sin(ang), c + (x - c) * math.sin(ang) + (y - c) * math.cos(ang)) for x, y in poly]
        d.polygon(rot, fill=200)
        d.line([rot[0], rot[23]], fill=255, width=SS)
    elif kind == "sound":  # arcos concentricos
        for i in range(3):
            r = c * (0.35 + 0.25 * i) * (0.8 + step * 0.12)
            d.arc((c - r, c - r, c + r, c + r), -50, 50, fill=int(255 - 50 * i), width=int(SS * 1.3))
            d.arc((c - r, c - r, c + r, c + r), 130, 230, fill=int(255 - 50 * i), width=int(SS * 1.3))
    return _finish(m, size)


def fx_decal(kind, size=32, seed=3):
    m = _canvas(size)
    d = ImageDraw.Draw(m)
    S = size * SS
    c = S / 2
    rnd = random.Random(seed + sum(ord(ch) for ch in kind))
    if kind == "rune":
        r = c * 0.85
        d.ellipse((c - r, c - r, c + r, c + r), outline=230, width=int(SS * 1.2))
        r2 = c * 0.62
        d.ellipse((c - r2, c - r2, c + r2, c + r2), outline=170, width=SS)
        for i in range(6):
            a = i / 6 * math.tau
            p1 = (c + math.cos(a) * r2, c + math.sin(a) * r2)
            p2 = (c + math.cos(a + math.tau / 3) * r2, c + math.sin(a + math.tau / 3) * r2)
            d.line([p1, p2], fill=200, width=SS)
    elif kind in ("scorch", "stain", "splash", "scar"):
        n = _noise(size, rnd.randint(0, 999))
        for y in range(size):
            for x in range(size):
                r = math.hypot(x - size / 2 + 0.5, y - size / 2 + 0.5) / (size / 2)
                edge = 0.75 + 0.35 * n[y][x]
                if kind == "scar":
                    v = max(0.0, 1.0 - abs(r - 0.6) * 5) * (0.6 + 0.6 * n[y][x])
                elif kind == "splash":  # poca central de borda ondulada (as gotas em volta vem depois)
                    ang = math.atan2(y - size / 2 + 0.5, x - size / 2 + 0.5)
                    rim = 0.42 + 0.08 * math.sin(ang * 7) + 0.06 * (n[y][x] - 0.5)
                    v = 1.0 if r < rim else 0.0
                elif kind == "stain":  # respingo: miolo com raios irregulares para fora
                    ang = math.atan2(y - size / 2 + 0.5, x - size / 2 + 0.5)
                    spikes = 0.38 + 0.32 * max(0.0, math.sin(ang * 9 + 1.3)) ** 3 + 0.1 * (n[y][x] - 0.5)
                    v = 1.0 if r < spikes else 0.0
                else:  # chamuscado: mancha escura com buracos e borda roida
                    v = max(0.0, (edge - r * 1.05) * 3.0) * (0.35 + 1.3 * max(0.0, n[y][x] - 0.28))
                d.rectangle((x * SS, y * SS, x * SS + SS - 1, y * SS + SS - 1), fill=max(0, min(255, int(230 * min(1.0, v)))))
    if kind == "splash":  # gotas soltas em volta da poca
        for i in range(9):
            a = rnd.uniform(0, math.tau)
            dist = c * rnd.uniform(0.62, 0.9)
            rr = c * rnd.uniform(0.04, 0.09)
            px, py = c + math.cos(a) * dist, c + math.sin(a) * dist
            d.ellipse((px - rr, py - rr, px + rr, py + rr), fill=230)
    if kind == "rime":
        for i in range(6):
            a = i / 6 * math.tau + 0.2
            ex, ey = c + math.cos(a) * c * 0.9, c + math.sin(a) * c * 0.9
            d.line([(c, c), (ex, ey)], fill=230, width=int(SS * 1.2))
            for j in (0.4, 0.65):
                bx, by = c + math.cos(a) * c * 0.9 * j, c + math.sin(a) * c * 0.9 * j
                for sgn in (-1, 1):
                    b = a + sgn * 0.7
                    d.line([(bx, by), (bx + math.cos(b) * c * 0.22, by + math.sin(b) * c * 0.22)], fill=200, width=SS)
    elif kind == "crack":
        for i in range(7):
            a = rnd.uniform(0, math.tau)
            x, y = c, c
            for _ in range(5):
                a += rnd.uniform(-0.6, 0.6)
                nx, ny = x + math.cos(a) * c * 0.2, y + math.sin(a) * c * 0.2
                d.line([(x, y), (nx, ny)], fill=220, width=int(SS * 1.1))
                x, y = nx, ny
    elif kind == "bloom":
        for i in range(8):
            a = i / 8 * math.tau
            px, py = c + math.cos(a) * c * 0.5, c + math.sin(a) * c * 0.5
            r = c * 0.2
            d.ellipse((px - r, py - r, px + r, py + r), fill=200)
        r = c * 0.18
        d.ellipse((c - r, c - r, c + r, c + r), fill=255)
    return _finish(m, size, alpha_curve=0.9)


NEW_FAMILIES = ["earth", "water", "light", "shadow", "blood", "time", "nature", "sound"]
DECALS = ["rune", "scorch", "rime", "crack", "splash", "stain", "scar", "bloom"]


def element_fx_assets():
    """Sprites e particles/fx_*.json das particulas de elemento (ver com.arcane.fx.FxFamilies)."""
    for fam, old in OLD_FAMILIES.items():  # reaproveitam os sprites antigos (ja coloridos)
        write_json(f"particles/fx_{fam}.json", {"textures": [f"arcane:{old}_{i}" for i in range(4)]})
    gens = {"soft": fx_soft, "streak": fx_streak, "smoke": fx_smoke}
    for fam, fn in gens.items():
        frames = []
        for i in range(4):
            save(fn(i), f"particle/fx_{fam}_{i}.png")
            frames.append(f"arcane:fx_{fam}_{i}")
        write_json(f"particles/fx_{fam}.json", {"textures": frames})
    for n, fam in enumerate(NEW_FAMILIES):
        frames = []
        for i in range(4):
            save(fx_shape(fam, i, seed=n), f"particle/fx_{fam}_{i}.png")
            frames.append(f"arcane:fx_{fam}_{i}")
        write_json(f"particles/fx_{fam}.json", {"textures": frames})
    for dec in DECALS:
        save(fx_decal(dec), f"particle/fx_decal_{dec}.png")
        write_json(f"particles/fx_decal_{dec}.json", {"textures": [f"arcane:fx_decal_{dec}"]})


def generate():
    names = particle_assets()
    element_fx_assets()
    grimoire_assets()
    workbench_assets()
    crystal_animated()
    save(workbench_gui(), "gui/arcane_workbench.png")
    save(grimoire_gui(), "gui/grimoire.png")
    save(mana_bar(), "gui/mana_bar.png")
    item_assets()
    logo()
    return names


if __name__ == "__main__":
    import sys as _sys
    if "--element-fx" in _sys.argv:  # so as particulas de elemento, sem regerar o resto da arte
        element_fx_assets()
    else:
        generate()
