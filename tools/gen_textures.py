"""Gera todas as texturas do mod Arcane em pixel art procedural.

Uso:  python tools/gen_textures.py
Requer: Pillow (pip install pillow)

As texturas sao placeholders de qualidade: coerentes entre si (paleta roxa/ciano
arcana) e no formato exato que o Minecraft espera. Um artista pode substituir
qualquer PNG mantendo o mesmo caminho e tamanho.
"""
from __future__ import annotations

import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

import sys
sys.path.insert(0, str(Path(__file__).resolve().parent))
from glyph_table import G  # noqa: E402  (tabela unica dos glifos)
import gen_mage_skins  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "arcane" / "textures"
RES = ROOT / "src" / "main" / "resources"

# ---------------------------------------------------------------- paleta
T = (0, 0, 0, 0)
PAL = {
    ".": T,
    "k": (20, 12, 30, 255),      # contorno quase preto
    "d": (45, 25, 70, 255),      # roxo escuro
    "p": (95, 50, 150, 255),     # roxo
    "P": (150, 90, 210, 255),    # roxo claro
    "L": (205, 165, 245, 255),   # lilas
    "c": (40, 150, 190, 255),    # ciano escuro
    "C": (90, 220, 240, 255),    # ciano
    "W": (225, 250, 255, 255),   # brilho
    "g": (150, 110, 30, 255),    # ouro escuro
    "G": (230, 190, 70, 255),    # ouro
    "w": (110, 70, 40, 255),     # madeira
    "b": (70, 42, 25, 255),      # madeira escura
    "s": (205, 215, 225, 255),   # vidro / prata
    "S": (150, 160, 175, 255),   # prata escura
    "n": (20, 30, 80, 255),      # tinta azul escura
    "N": (40, 60, 150, 255),     # tinta azul
    "r": (110, 10, 20, 255),     # sangue escuro
    "R": (190, 30, 40, 255),     # sangue
    "i": (200, 230, 255, 90),    # tinta invisivel (translucida)
    "I": (235, 250, 255, 150),
    "y": (225, 205, 160, 255),   # pergaminho
    "Y": (245, 232, 195, 255),
    "o": (170, 140, 95, 255),    # pergaminho sombra
    "t": (40, 90, 95, 255),      # tecido manto escuro
    "T": (70, 140, 140, 255),    # tecido manto
    "u": (120, 190, 180, 255),   # tecido claro
}


def sprite(rows: list[str], pal: dict | None = None) -> Image.Image:
    pal = {**PAL, **(pal or {})}
    h = len(rows)
    w = max(len(r) for r in rows)
    img = Image.new("RGBA", (w, h), T)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            img.putpixel((x, y), pal[ch])
    return img


def save(img: Image.Image, rel: str) -> None:
    path = ASSETS / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("  ", path.relative_to(ROOT))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


# ---------------------------------------------------------------- itens
ITEMS = {
    "arcane_pen": [
        "..............LW",
        ".............LPW",
        "............LPPL",
        "...........LPPP.",
        "..........LPPpL.",
        ".........LPPpP..",
        "........LPPpP...",
        ".......LPPpP....",
        "......LPPpP.....",
        ".....LPPpP......",
        ".....PPpP.......",
        "....GgpP........",
        "...GgG..........",
        "..gGg...........",
        ".CG.............",
        "WC..............",
    ],
    "glyph_note": [
        "................",
        "..kkkkkkkkkkk...",
        "..kYYYYYYYYYyk..",
        "..kYyyyyyyyYyk..",
        "..kYyyPPPyyYyk..",
        "..kYyPyyyPyYyk..",
        "..kYyPyCyPyYyk..",
        "..kYyPyyyPyYyk..",
        "..kYyyPPPyyYyk..",
        "..kYyyyPyyyYyk..",
        "..kYyoyyyoyYyk..",
        "..kYyyoooyyYyk..",
        "..kYYYYYYYYYok..",
        "..kooooooooook..",
        "...kkkkkkkkkk...",
        "................",
    ],
    "arcane_mask": [
        "................",
        "................",
        "...kkkkkkkkkk...",
        "..kddddddddddk..",
        ".kdpppppppppppk.",
        ".kpPPppppppPPpk.",
        ".kpkCCkppkCCkpk.",
        ".kpkWCkppkWCkpk.",
        ".kppkkpPPpkkppk.",
        "..kpppPGGPpppk..",
        "..kdppPGGPppdk..",
        "...kdppPPppdk...",
        "....kddppddk....",
        ".....kkddkk.....",
        ".......kk.......",
        "................",
    ],
    "insulating_cloak": [
        "................",
        ".....kkkkkk.....",
        "....kuTGGTuk....",
        "...kTTkGGkTTk...",
        "..kTTTtkktTTTk..",
        "..kTTTttttTTTk..",
        ".kTTTTttttTTTTk.",
        ".kTuTTttttTTuTk.",
        ".kTTTTttttTTTTk.",
        ".kTTTttCCttTTTk.",
        "kTTTTttCCttTTTTk",
        "kTuTTttttttTTuTk",
        "kTTTTttttttTTTTk",
        "kttTTtttttttTTtk",
        ".kkttkkkkkkttkk.",
        "...kk......kk...",
    ],
    "glyph_projector": [
        "................",
        "......kkkk......",
        ".....kCWWCk.....",
        "....kCCWWCCk....",
        "....kcCCCCck....",
        ".....kcCCck.....",
        "....kGkcckGk....",
        "...kGgSSSSgGk...",
        "...kgSssssSgk...",
        "...kgSsPPsSgk...",
        "...kgSsPPsSgk...",
        "...kgSssssSgk...",
        "...kGgSSSSgGk...",
        "....kbwwwwbk....",
        "...kbwwwwwwbk...",
        "...kkkkkkkkkk...",
    ],
    "symmetry_mirror": [
        "................",
        ".....kkkkkk.....",
        "....kGGGGGGk....",
        "...kGsWWWssGk...",
        "...kGsWsssCGk...",
        "...kGWssssCGk...",
        "...kGsssCCsGk...",
        "...kGssCCssGk...",
        "...kGsCCsssGk...",
        "....kGGGGGGk....",
        ".....kkGGkk.....",
        ".......wb.......",
        ".......wb.......",
        ".......wb.......",
        "......kGGk......",
        ".......kk.......",
    ],
}

BOTTLE = [
    "................",
    "......kkkk......",
    "......kwwk......",
    "......kbbk......",
    ".....kssssk.....",
    "......kssk......",
    ".....ks11sk.....",
    "....ks1111sk....",
    "...ks122221sk...",
    "...ks122221sk...",
    "...ks122221sk...",
    "...ks222222sk...",
    "...ks222222sk...",
    "....ks2222sk....",
    ".....kkkkkk.....",
    "................",
]


def bottle(light, dark):
    rows = [r.replace("1", "#").replace("2", "%") for r in BOTTLE]
    return sprite(rows, {"#": light, "%": dark})


# ---------------------------------------------------------------- blocos
def noise_fill(img, base, var, seed):
    rnd = random.Random(seed)
    w, h = img.size
    for y in range(h):
        for x in range(w):
            f = 1.0 + rnd.uniform(-var, var)
            img.putpixel((x, y), shade(base, f))


def workbench_top():
    img = Image.new("RGBA", (16, 16))
    noise_fill(img, PAL["w"], 0.08, 1)
    d = ImageDraw.Draw(img)
    for y in (0, 15):
        d.line([(0, y), (15, y)], fill=PAL["b"])
    for x in (0, 15):
        d.line([(x, 0), (x, 15)], fill=PAL["b"])
    # pano roxo de escriba
    d.rectangle([2, 2, 13, 13], fill=PAL["d"])
    d.rectangle([3, 3, 12, 12], fill=PAL["p"])
    # circulo de glifo
    for a in range(0, 360, 12):
        x = 7.5 + 3.6 * math.cos(math.radians(a))
        y = 7.5 + 3.6 * math.sin(math.radians(a))
        img.putpixel((round(x), round(y)), PAL["C"])
    for px in [(7, 5), (8, 5), (5, 7), (5, 8), (10, 7), (10, 8), (7, 10), (8, 10)]:
        img.putpixel(px, PAL["L"])
    img.putpixel((7, 7), PAL["W"]); img.putpixel((8, 8), PAL["W"])
    img.putpixel((8, 7), PAL["C"]); img.putpixel((7, 8), PAL["C"])
    return img


def workbench_side():
    img = Image.new("RGBA", (16, 16))
    noise_fill(img, PAL["w"], 0.1, 2)
    d = ImageDraw.Draw(img)
    # tampo
    d.rectangle([0, 0, 15, 2], fill=PAL["b"])
    d.line([(0, 1), (15, 1)], fill=PAL["w"])
    # pano pendendo
    d.rectangle([2, 3, 13, 6], fill=PAL["p"])
    for x in range(2, 14, 2):
        img.putpixel((x, 7), PAL["p"])
    for x in (4, 8, 11):
        img.putpixel((x, 4), PAL["C"])
    # gaveta e pernas
    d.rectangle([3, 9, 12, 12], fill=PAL["b"])
    d.rectangle([4, 10, 11, 11], fill=PAL["w"])
    img.putpixel((7, 10), PAL["G"]); img.putpixel((8, 10), PAL["G"])
    d.rectangle([0, 3, 1, 15], fill=PAL["b"])
    d.rectangle([14, 3, 15, 15], fill=PAL["b"])
    return img


def workbench_bottom():
    img = Image.new("RGBA", (16, 16))
    noise_fill(img, PAL["b"], 0.1, 3)
    return img


def mana_crystal():
    img = Image.new("RGBA", (16, 16))
    rnd = random.Random(7)
    for y in range(16):
        for x in range(16):
            # faixas diagonais de faceta
            band = (x + y) % 6
            base = [PAL["c"], PAL["C"], PAL["C"], PAL["c"], (30, 110, 160, 255), PAL["c"]][band]
            img.putpixel((x, y), shade(base, 1 + rnd.uniform(-0.06, 0.06)))
    for x, y in [(3, 2), (4, 3), (10, 5), (11, 6), (6, 11), (13, 12), (2, 9)]:
        img.putpixel((x, y), PAL["W"])
    for x, y in [(8, 8), (7, 8), (8, 7), (12, 2), (2, 13)]:
        img.putpixel((x, y), PAL["L"])
    return img


def mana_crystal_base():
    img = Image.new("RGBA", (16, 16))
    noise_fill(img, (70, 70, 85, 255), 0.12, 9)
    rnd = random.Random(10)
    for _ in range(10):
        img.putpixel((rnd.randrange(16), rnd.randrange(16)), PAL["c"])
    return img


# ---------------------------------------------------------------- GUI
def panel(d, box, fill, light, dark, border=PAL["k"]):
    x0, y0, x1, y1 = box
    d.rectangle(box, fill=fill)
    d.rectangle(box, outline=border)
    d.line([(x0 + 1, y0 + 1), (x1 - 1, y0 + 1)], fill=light)
    d.line([(x0 + 1, y0 + 1), (x0 + 1, y1 - 1)], fill=light)
    d.line([(x0 + 1, y1 - 1), (x1 - 1, y1 - 1)], fill=dark)
    d.line([(x1 - 1, y0 + 1), (x1 - 1, y1 - 1)], fill=dark)


def inset(d, box, fill=(18, 10, 28, 255)):
    x0, y0, x1, y1 = box
    d.rectangle(box, fill=fill)
    d.line([(x0, y0), (x1, y0)], fill=(10, 5, 15, 255))
    d.line([(x0, y0), (x0, y1)], fill=(10, 5, 15, 255))
    d.line([(x0, y1), (x1, y1)], fill=(120, 80, 170, 255))
    d.line([(x1, y0), (x1, y1)], fill=(120, 80, 170, 255))


# Layout da Bancada (deve bater com ArcaneWorkbenchScreen.java)
WB_W, WB_H = 240, 222


def workbench_gui():
    img = Image.new("RGBA", (256, 256), T)
    d = ImageDraw.Draw(img)
    panel(d, (0, 0, WB_W - 1, WB_H - 1), (52, 30, 78, 255), (110, 70, 160, 255), (28, 15, 42, 255))
    # moldura dourada interna
    d.rectangle((3, 3, WB_W - 4, WB_H - 4), outline=PAL["g"])
    for cx, cy in [(3, 3), (WB_W - 4, 3), (3, WB_H - 4), (WB_W - 4, WB_H - 4)]:
        d.rectangle((cx - 1, cy - 1, cx + 1, cy + 1), fill=PAL["G"])
    # area: glifos conhecidos (esquerda)
    inset(d, (8, 30, 113, 163))
    # area: composicao (direita)
    inset(d, (120, 30, 231, 135))
    # area: custo / validacao
    inset(d, (120, 140, 231, 163))
    # padrao de runas sutil nos insets
    rnd = random.Random(42)
    for box in [(9, 31, 112, 162), (121, 31, 230, 134)]:
        for _ in range(40):
            x = rnd.randint(box[0], box[2]); y = rnd.randint(box[1], box[3])
            img.putpixel((x, y), (35, 22, 52, 255))
    # linha separadora do seletor de magia ativa
    d.line([(8, 192), (WB_W - 9, 192)], fill=PAL["g"])
    # slot de glifo: normal 0,230 / hover 20,230 / selecionado 40,230 (18x18)
    for i, (f, o) in enumerate([((40, 25, 60, 255), (100, 70, 140, 255)),
                                 ((70, 45, 105, 255), PAL["C"]),
                                 ((60, 35, 90, 255), PAL["G"])]):
        x = i * 20
        d.rectangle((x, 230, x + 17, 247), fill=f, outline=o)
    return img


def mana_bar():
    img = Image.new("RGBA", (128, 32), T)
    d = ImageDraw.Draw(img)
    # moldura vazia 0,0 (102x10)
    d.rectangle((0, 0, 101, 9), fill=(15, 10, 25, 200), outline=PAL["k"])
    d.line([(1, 1), (100, 1)], fill=(40, 30, 60, 220))
    for x in range(10, 100, 10):
        d.line([(x, 2), (x, 7)], fill=(30, 22, 45, 220))
    # preenchimento 0,10 (100x8) gradiente ciano->lilas
    for x in range(100):
        t = x / 99
        c1, c2 = PAL["c"], PAL["P"]
        col = tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3)) + (255,)
        for y in range(8):
            f = 1.25 if y < 2 else (0.8 if y > 5 else 1.0)
            img.putpixel((x, 10 + y), shade(col, f))
    # preenchimento envenenado 0,20 (100x8) azul eletrico
    for x in range(100):
        for y in range(8):
            f = 1.3 if (x + y) % 7 == 0 else (0.85 if y > 5 else 1.0)
            img.putpixel((x, 20 + y), shade((60, 110, 255, 255), f))
    # icone de gota de mana 104,0 (9x9)
    gem = sprite([
        "....k....",
        "...kCk...",
        "..kCWCk..",
        ".kCCWCck.",
        "kCCCCCcck",
        "kcCCCCcck",
        ".kcCCcck.",
        "..kccck..",
        "...kkk...",
    ])
    img.paste(gem, (104, 0), gem)
    return img


# Icones de glifo: moldura colorida por categoria + simbolo 8x8
CATEGORY_COLORS = {
    "form": ((40, 150, 190, 255), (90, 220, 240, 255)),
    "effect": ((170, 50, 40, 255), (250, 140, 70, 255)),
    "filter": ((60, 130, 50, 255), (140, 220, 110, 255)),
    "modifier": ((150, 110, 30, 255), (240, 200, 80, 255)),
    "condition": ((95, 50, 150, 255), (205, 165, 245, 255)),
    "activator": ((150, 160, 175, 255), (240, 245, 255, 255)),
}

UNKNOWN_SYMBOL = ["..####..", ".#....#.", "......#.", ".....#..", "....#...", "....#...", "........", "....#..."]


def glyph_icon(name, category):
    dark, light = CATEGORY_COLORS[category]
    img = Image.new("RGBA", (16, 16), T)
    d = ImageDraw.Draw(img)
    d.ellipse((0, 0, 15, 15), fill=(20, 12, 30, 230), outline=dark)
    d.ellipse((1, 1, 14, 14), outline=shade(dark, 0.7))
    sym = UNKNOWN_SYMBOL if name == "unknown" else G[name]["icon"]
    if sym:
        for y, row in enumerate(sym):
            for x, ch in enumerate(row):
                if ch == "#":
                    img.putpixel((4 + x, 4 + y), light)
    return img


# ---------------------------------------------------------------- entidades
def grimoire_book():
    """Textura 64x32 compativel com BookModel (layout do livro do encantamento)."""
    img = Image.new("RGBA", (64, 32), T)
    d = ImageDraw.Draw(img)
    cover, cover_d = (80, 30, 110, 255), (50, 18, 70, 255)
    # capas (0..28, 0..10)
    d.rectangle((0, 0, 27, 9), fill=cover)
    for x0 in (0, 16):
        d.rectangle((x0, 0, x0 + 11, 9), outline=PAL["G"])
        d.rectangle((x0 + 4, 3, x0 + 7, 6), fill=PAL["C"])
        img.putpixel((x0 + 5, 4), PAL["W"])
    d.rectangle((12, 0, 15, 9), fill=cover_d)
    # paginas (0..34, 10..19)
    d.rectangle((0, 10, 33, 18), fill=PAL["Y"])
    rnd = random.Random(5)
    for x in range(0, 34):
        for y in range(10, 19):
            if rnd.random() < 0.18:
                img.putpixel((x, y), PAL["y"])
    for x in range(1, 33, 3):
        img.putpixel((x, 13), PAL["P"])
        img.putpixel((x + 1, 15), PAL["c"])
    return img


def armor_layer():
    """Armadura arcana 64x32: mascara (cabeca) + manto (corpo/bracos)."""
    img = Image.new("RGBA", (64, 32), T)
    d = ImageDraw.Draw(img)
    # mascara sobre a cabeca (0..32, 0..16): so a frente e laterais
    d.rectangle((8, 8, 15, 15), fill=PAL["p"])
    img.putpixel((9, 11), PAL["C"]); img.putpixel((10, 11), PAL["W"])
    img.putpixel((13, 11), PAL["W"]); img.putpixel((14, 11), PAL["C"])
    d.line([(8, 8), (15, 8)], fill=PAL["d"])
    d.rectangle((11, 12, 12, 13), fill=PAL["G"])
    d.rectangle((0, 10, 7, 12), fill=PAL["d"]); d.rectangle((16, 10, 23, 12), fill=PAL["d"])
    d.rectangle((24, 10, 31, 12), fill=PAL["d"])
    # manto (corpo 16..40, 16..32 e bracos 40..56, 16..32)
    d.rectangle((16, 20, 39, 31), fill=PAL["T"])
    d.rectangle((20, 16, 35, 19), fill=PAL["t"])
    d.line([(20, 20), (27, 20)], fill=PAL["G"])
    img.putpixel((23, 23), PAL["C"]); img.putpixel((24, 23), PAL["C"])
    for x in range(16, 40, 4):
        d.line([(x, 28), (x, 31)], fill=PAL["t"])
    d.rectangle((40, 20, 55, 28), fill=PAL["T"])
    d.rectangle((44, 16, 51, 19), fill=PAL["t"])
    d.line([(40, 28), (55, 28)], fill=PAL["u"])
    return img


def arcane_spark():
    img = Image.new("RGBA", (8, 8), T)
    pts = {(3, 0): "L", (4, 0): "L", (3, 1): "C", (4, 1): "C", (0, 3): "L", (1, 3): "C", (6, 3): "C", (7, 3): "L",
           (0, 4): "L", (1, 4): "C", (6, 4): "C", (7, 4): "L", (3, 6): "C", (4, 6): "C", (3, 7): "L", (4, 7): "L",
           (2, 2): "C", (5, 2): "C", (2, 5): "C", (5, 5): "C"}
    for (x, y), c in pts.items():
        img.putpixel((x, y), PAL[c])
    for x in (2, 3, 4, 5):
        for y in (3, 4):
            img.putpixel((x, y), PAL["W"])
    for x, y in [(3, 2), (4, 2), (3, 5), (4, 5)]:
        img.putpixel((x, y), PAL["W"])
    return img


def spark_frames():
    """4 quadros de particula 8x8 que encolhem."""
    frames = []
    for i in range(4):
        img = Image.new("RGBA", (8, 8), T)
        r = 3.5 - i * 0.8
        for y in range(8):
            for x in range(8):
                dist = math.hypot(x - 3.5, y - 3.5)
                if dist <= r:
                    a = int(255 * (1 - dist / (r + 0.01)) ** 0.6)
                    col = PAL["W"] if dist < r * 0.4 else (PAL["C"] if i < 2 else PAL["L"])
                    img.putpixel((x, y), col[:3] + (max(a, 120),))
        frames.append(img)
    return frames


def projectile_tex():
    img = Image.new("RGBA", (16, 16), T)
    for y in range(16):
        for x in range(16):
            dist = math.hypot(x - 7.5, y - 7.5)
            if dist < 7.5:
                t = dist / 7.5
                if t < 0.3:
                    col = PAL["W"]
                elif t < 0.6:
                    col = PAL["C"]
                else:
                    col = PAL["P"]
                a = int(255 * (1 - t ** 2))
                img.putpixel((x, y), col[:3] + (a,))
    return img


def mod_logo():
    img = Image.new("RGBA", (128, 128), (25, 14, 38, 255))
    d = ImageDraw.Draw(img)
    for r, col in [(58, PAL["d"]), (52, PAL["p"]), (50, (25, 14, 38, 255))]:
        d.ellipse((64 - r, 64 - r, 64 + r, 64 + r), fill=col)
    for a in range(0, 360, 30):
        x = 64 + 44 * math.cos(math.radians(a)); y = 64 + 44 * math.sin(math.radians(a))
        d.rectangle((x - 2, y - 2, x + 2, y + 2), fill=PAL["G"])
    pts = [(64 + 34 * math.cos(math.radians(a - 90)), 64 + 34 * math.sin(math.radians(a - 90))) for a in range(0, 720, 144)]
    d.line(pts + [pts[0]], fill=PAL["C"], width=3)
    d.ellipse((56, 56, 72, 72), fill=PAL["W"])
    return img


def barrier_block():
    """Barreira arcana (16x16, translucida): vidro roxo com moldura e runas ciano."""
    img = Image.new("RGBA", (16, 16), (150, 90, 210, 70))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), outline=(205, 165, 245, 230))
    d.rectangle((1, 1, 14, 14), outline=(150, 90, 210, 150))
    rnd = random.Random(31)
    for _ in range(24):
        x, y = rnd.randint(2, 13), rnd.randint(2, 13)
        img.putpixel((x, y), (90, 220, 240, 170))
    for x in range(3, 13):
        img.putpixel((x, 8), (205, 165, 245, 120))
    for y in range(3, 13):
        img.putpixel((8, y), (205, 165, 245, 120))
    img.putpixel((8, 8), (225, 250, 255, 255))
    return img


def grimoire_item():
    """Item do Grimorio (16x16)."""
    return sprite([
        "................",
        "..kkkkkkkkkkk...",
        ".kdppppppppppk..",
        ".kdpGGGGGGGGpk..",
        ".kdpGppppppGpk..",
        ".kdpGpCCCCpGpk..",
        ".kdpGpCWWCpGpk..",
        ".kdpGpCWWCpGpk..",
        ".kdpGpCCCCpGpk..",
        ".kdpGppppppGpk..",
        ".kdpGGGGGGGGpk..",
        ".kdppppppppppk..",
        ".kdYYYYYYYYYyk..",
        ".kkYyyyyyyyyyk..",
        "..kkkkkkkkkkkk..",
        "................",
    ])


def grimoire_gui():
    """Fundo da tela do Grimorio: capa de couro, duas paginas de pergaminho e lombada (256x200)."""
    img = Image.new("RGBA", (256, 256), T)
    d = ImageDraw.Draw(img)
    rnd = random.Random(77)
    # capa
    d.rectangle((0, 0, 255, 199), fill=(58, 30, 74, 255), outline=PAL["k"])
    for y in range(1, 199):
        for x in range(1, 255):
            if rnd.random() < 0.18:
                img.putpixel((x, y), shade((58, 30, 74, 255), 1 + rnd.uniform(-0.1, 0.1)))
    d.rectangle((3, 3, 252, 196), outline=PAL["g"])
    for cx, cy in [(3, 3), (252, 3), (3, 196), (252, 196)]:
        d.rectangle((cx - 1, cy - 1, cx + 1, cy + 1), fill=PAL["G"])
    # paginas
    for x0, x1 in ((12, 124), (132, 244)):
        d.rectangle((x0, 24, x1, 172), fill=(238, 224, 186, 255), outline=(120, 90, 50, 255))
        for y in range(25, 172):
            for x in range(x0 + 1, x1):
                if rnd.random() < 0.12:
                    img.putpixel((x, y), shade((238, 224, 186, 255), 1 + rnd.uniform(-0.05, 0.03)))
        for y in range(39, 172, 12):  # linhas de caderno, alinhadas as linhas da lista
            d.line([(x0 + 3, y), (x1 - 3, y)], fill=(216, 198, 156, 255))
    # lombada: sombra suave nas bordas internas das paginas
    for i in range(7):
        f = 0.92 - i * 0.012
        for y in range(25, 172):
            for x in (124 - i, 132 + i):
                if 12 < x < 244:
                    r, g, b, _ = img.getpixel((x, y))
                    img.putpixel((x, y), shade((r, g, b, 255), f))
    d.rectangle((125, 24, 131, 172), fill=(86, 52, 40, 255))
    d.line([(128, 24), (128, 172)], fill=(60, 34, 28, 255))
    return img


def main():
    print("Gerando texturas Arcane...")
    for name, rows in ITEMS.items():
        save(sprite(rows), f"item/{name}.png")
    save(bottle(PAL["N"], PAL["n"]), "item/common_ink.png")
    save(bottle(PAL["R"], PAL["r"]), "item/blood_ink.png")
    save(bottle(PAL["I"], PAL["i"]), "item/invisible_ink.png")

    save(workbench_top(), "block/arcane_workbench_top.png")
    save(workbench_side(), "block/arcane_workbench_side.png")
    save(workbench_bottom(), "block/arcane_workbench_bottom.png")
    save(mana_crystal(), "block/mana_crystal.png")
    save(mana_crystal_base(), "block/mana_crystal_base.png")
    save(barrier_block(), "block/arcane_barrier.png")

    save(workbench_gui(), "gui/arcane_workbench.png")
    save(mana_bar(), "gui/mana_bar.png")
    for name, g in G.items():
        save(glyph_icon(name, g["cat"]), f"gui/glyphs/{name}.png")
    save(glyph_icon("unknown", "activator"), "gui/glyphs/unknown.png")
    save(grimoire_gui(), "gui/grimoire.png")
    save(grimoire_item(), "item/grimoire.png")

    save(grimoire_book(), "entity/grimoire.png")
    gen_mage_skins.generate()
    old = ASSETS / "entity" / "npc_mage.png"
    if old.exists():
        old.unlink()
    save(projectile_tex(), "entity/arcane_projectile.png")
    save(armor_layer(), "models/armor/arcane_weave_layer_1.png")

    for i, f in enumerate(spark_frames()):
        save(f, f"particle/arcane_spark_{i}.png")

    logo = mod_logo()
    logo.save(RES / "arcane_logo.png")
    print("   src/main/resources/arcane_logo.png")


if __name__ == "__main__":
    main()
