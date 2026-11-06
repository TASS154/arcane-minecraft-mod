"""Gera 18 skins do Mago NPC (64x64, layout classico de jogador) em textures/entity/npc_mage/mage_NN.png.

variante v:  elemento = v % 6   (fogo, gelo, tempestade, veneno, explosao, vento)
             estilo   = v // 6  (0 capuz + barba curta, 1 chapeu + barba longa, 2 capa + circlete)
             pele/cabelo variam por v.

Skins sao 100% procedurais (sem arquivos de terceiros, sem problema de licenca).
Uso: python tools/gen_mage_skins.py
"""
from __future__ import annotations

import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/arcane/textures/entity/npc_mage"
VARIANTS = 18
T = (0, 0, 0, 0)

ELEMENTS = [
    # nome, tecido, tecido escuro, detalhe, brilho
    ("fire", (150, 38, 28), (92, 22, 16), (240, 172, 56), (255, 150, 40)),
    ("frost", (52, 102, 162), (30, 62, 112), (205, 236, 255), (120, 222, 255)),
    ("storm", (84, 72, 152), (50, 42, 102), (255, 232, 92), (255, 242, 120)),
    ("venom", (56, 122, 62), (30, 76, 42), (192, 232, 92), (150, 255, 84)),
    ("blast", (132, 52, 152), (82, 26, 102), (242, 172, 242), (255, 122, 255)),
    ("gale", (62, 66, 80), (34, 36, 48), (170, 172, 196), (180, 140, 255)),
]
SKIN = [(226, 188, 154), (200, 152, 112), (152, 106, 74), (112, 74, 52)]
HAIR = [(226, 226, 232), (160, 160, 168), (92, 62, 42), (204, 154, 72), (60, 44, 34), (176, 84, 52)]
BOOT = (46, 30, 24, 255)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def faces(ox, oy, w, h, d):
    return {
        "top": (ox + d, oy, w, d), "bottom": (ox + d + w, oy, w, d),
        "right": (ox, oy + d, d, h), "front": (ox + d, oy + d, w, h),
        "left": (ox + d + w, oy + d, d, h), "back": (ox + 2 * d + w, oy + d, w, h),
    }


class Painter:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), T)
        self.rnd = random.Random(seed)

    def px(self, x, y, color):
        if 0 <= x < 64 and 0 <= y < 64 and color is not None:
            self.img.putpixel((x, y), color)

    def face(self, box, name, fn, noise=0.0):
        x0, y0, w, h = faces(*box)[name]
        for y in range(h):
            for x in range(w):
                c = fn(x, y)
                if c is None:
                    continue
                if noise and c[3] == 255:
                    c = shade(c, 1 + self.rnd.uniform(-noise, noise))
                self.px(x0 + x, y0 + y, c)

    def all_faces(self, box, fn, noise=0.0, shades=None):
        shades = shades or {"top": 1.12, "bottom": 0.7, "right": 0.88, "front": 1.0, "left": 0.88, "back": 0.92}
        for name in faces(*box):
            s = shades[name]
            self.face(box, name, lambda x, y, n=name, s=s: _mul(fn(n, x, y), s), noise)


def _mul(c, s):
    return None if c is None else (shade(c, s) if s != 1.0 else c)


def make(v: int) -> Image.Image:
    el = v % 6
    style = v // 6
    _, robe, dark, trim, glow = ELEMENTS[el]
    skin = SKIN[(v * 5 + 1) % 4] + (255,)
    skin_d = shade(skin, 0.82)
    hair = HAIR[(v * 7 + 2) % 6] + (255,)
    p = Painter(1000 + v)
    glow = glow + (255,)
    robe, dark, trim = robe + (255,), dark + (255,), trim + (255,)

    # ---------------------------------------------------------------- cabeca
    head = (0, 0, 8, 8, 8)

    def head_fn(name, x, y):
        if name == "front":
            return None
        if style == 2 and (y <= 3 or name in ("back",) and y <= 6):
            return hair
        if name == "top" and style == 2:
            return hair
        return skin

    p.all_faces(head, head_fn, noise=0.03)

    def front(x, y):
        c = skin
        if style == 2 and y <= 1:
            c = hair
        if style == 2 and y == 2 and x in (0, 7):
            c = hair
        if y == 3 and x in (1, 2, 5, 6):          # sobrancelhas
            c = hair
        if y == 4:                                  # olhos: branco fora, brilho do elemento dentro
            if x in (1, 6):
                c = (236, 236, 240, 255)
            elif x in (2, 5):
                c = glow
        if y == 5 and x in (3, 4):                  # nariz
            c = skin_d
        if style in (0, 1) and y >= 5:              # barba
            if not (y == 6 and x in (3, 4)):
                c = hair
            if y == 5 and x in (3, 4):
                c = hair
        elif y == 6 and x in (3, 4):                # boca
            c = shade(skin, 0.66)
        return c

    p.face(head, "front", front, noise=0.03)

    # ---------------------------------------------------------------- camada da cabeca (capuz / chapeu / circlete)
    hat = (32, 0, 8, 8, 8)
    if style == 0:  # capuz
        def hood(name, x, y):
            if name == "front":
                if 1 <= x <= 6 and y >= 2:
                    return trim if (x in (1, 6) or y == 2) and False else None
                return robe if y >= 2 else dark
            return robe if name != "top" else dark
        p.all_faces(hat, hood, noise=0.04)
        # friso do capuz ao redor do rosto
        for x in range(1, 7):
            p.px(40 + x, 8 + 2, trim)
        for y in range(2, 8):
            p.px(40 + 1, 8 + y, trim if y == 2 else None)
    elif style == 1:  # chapeu de mago (cobre a metade de cima da cabeca)
        def wiz(name, x, y):
            if name == "bottom":
                return None
            if y <= 2 or name == "top":
                return dark if name == "top" else robe
            if y == 3:
                return trim
            return None
        p.all_faces(hat, wiz, noise=0.04)
        p.px(40 + 3, 8 + 3, glow)
        p.px(40 + 4, 8 + 3, glow)
    else:  # circlete + gema
        def circ(name, x, y):
            if name in ("top", "bottom"):
                return None
            return trim if y == 2 else None
        p.all_faces(hat, circ)
        p.px(40 + 3, 8 + 2, glow)
        p.px(40 + 4, 8 + 2, glow)

    # ---------------------------------------------------------------- corpo
    body = (16, 16, 8, 12, 4)

    def body_fn(name, x, y):
        w = 8 if name in ("front", "back", "top", "bottom") else 4
        if name == "front":
            if x in (2, 5):
                return trim
            if 3 <= x <= 4:
                return dark
            if y in (7, 8):
                return dark
            return robe
        if name == "back":
            if (x, y) in {(3, 3), (4, 3), (2, 4), (5, 4), (2, 5), (5, 5), (3, 6), (4, 6)}:
                return glow
            return robe
        return robe

    p.all_faces(body, body_fn, noise=0.04)
    p.px(20 + 3, 20 + 7, glow)   # gema do cinto
    p.px(20 + 4, 20 + 7, glow)

    jacket = (16, 32, 8, 12, 4)

    def jacket_fn(name, x, y):
        if name in ("top", "bottom"):
            return None
        if style in (0, 1):  # tunica longa: so a metade de baixo
            if y < 6:
                if style == 1 and name == "front" and y <= 3 and 2 <= x <= 5:
                    return hair  # barba longa sobre o peito
                return None
            if y == 11:
                return trim
            if name == "front" and x in (3, 4) and y >= 8:
                return None
            return robe if (x + y) % 5 else dark
        # estilo 2: capa nas costas e gola
        if name == "back":
            return dark if y > 0 else trim
        if name == "front":
            return trim if y == 0 and x in (1, 2, 5, 6) else None
        if name in ("left", "right") and y <= 8:
            return dark if x >= 2 else None
        return None

    p.all_faces(jacket, jacket_fn, noise=0.04)

    # ---------------------------------------------------------------- bracos
    for arm_box, sleeve_box in (((40, 16, 4, 12, 4), (40, 32, 4, 12, 4)), ((32, 48, 4, 12, 4), (48, 48, 4, 12, 4))):
        def arm_fn(name, x, y):
            if name in ("top",):
                return robe
            if name == "bottom":
                return skin
            if y <= 7:
                return robe
            if y == 8:
                return trim
            return skin
        p.all_faces(arm_box, arm_fn, noise=0.04)

        def sleeve_fn(name, x, y):
            if name in ("top", "bottom"):
                return None
            if 2 <= y <= 8:
                return dark if y == 8 else robe
            return None
        p.all_faces(sleeve_box, sleeve_fn, noise=0.04)

    # ---------------------------------------------------------------- pernas
    for leg_box, pants_box in (((0, 16, 4, 12, 4), (0, 32, 4, 12, 4)), ((16, 48, 4, 12, 4), (0, 48, 4, 12, 4))):
        def leg_fn(name, x, y):
            if name == "top":
                return dark
            if name == "bottom":
                return (30, 20, 16, 255)
            return dark if y <= 8 else (BOOT if y <= 10 else (30, 20, 16, 255))
        p.all_faces(leg_box, leg_fn, noise=0.04)

        def pants_fn(name, x, y):
            if name in ("top", "bottom") or style == 2:
                return None
            if y <= 9:
                return robe if y < 9 else trim
            return None
        p.all_faces(pants_box, pants_fn, noise=0.04)

    return p.img


def generate(out_dir: Path = OUT):
    out_dir.mkdir(parents=True, exist_ok=True)
    for v in range(VARIANTS):
        img = make(v)
        path = out_dir / f"mage_{v:02d}.png"
        img.save(path)
        print("  ", path.name, ELEMENTS[v % 6][0], "estilo", v // 6)


if __name__ == "__main__":
    generate()
