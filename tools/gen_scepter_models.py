"""Modelos 3D do Cetro (cajado grande): 10 pontas x 8 hastes = 80 modelos. A cor da gema e a do metal NAO entram no
modelo: as faces marcadas com tintindex 0 (gema) e 1 (metal) sao pintadas pelo jogo (ItemColors), entao cada modelo
vale para todas as cores.

Referencias de estilo (arquetipos de cajado de mago, nao copia de nenhum): lua crescente segurando um orbe (os
cajados de maga de animes de fantasia, como o da Frieren), gaiola com orbe, aro de monge, cachos de cristal, cajado de
druida em espiral, asas, estrela, tridente, aureola e o orbe classico.

O cajado vai de y = -7 a y = 23 (30 de altura, centro em 8, que e o eixo de giro das poses).
Rode depois de gen_item_models.py:  python tools/gen_scepter_models.py
"""
import json
import math

from gen_item_models import ASSETS, INDEX, palette

HEADS = ["crescent", "cage", "ring", "crystals", "crook", "wings", "star", "trident", "halo", "orb"]
SHAFTS = ["white", "wood", "twisted", "bone", "obsidian", "gilded", "crystal", "branch"]
GEM, METAL = 0, 1
C = 8.0  # eixo do cajado (x e z)


def el(frm, to, color, tint=None, rot=None, axis="z", origin=None):
    i = INDEX[color]
    u, v = (i % 8) * 2 + 0.5, (i // 8) * 2 + 0.5
    face = {"uv": [u, v, u + 1, v + 1], "texture": "#0"}
    if tint is not None:
        face["tintindex"] = tint
    e = {"from": [round(x, 3) for x in frm], "to": [round(x, 3) for x in to],
         "faces": {f: dict(face) for f in ("north", "south", "east", "west", "up", "down")}}
    if rot:
        o = origin or [(frm[k] + to[k]) / 2 for k in range(3)]
        e["rotation"] = {"angle": rot, "axis": axis, "origin": [round(x, 3) for x in o]}
    return e


def cube(cx, cy, cz, s, color, tint=None, rot=None, axis="z"):
    h = s / 2
    return el((cx - h, cy - h, cz - h), (cx + h, cy + h, cz + h), color, tint, rot, axis)


def metal(frm, to, rot=None, axis="z", origin=None):
    return el(frm, to, "tint_d", METAL, rot, axis, origin)


def gem(cx, cy, cz, s, rot=None, axis="z"):
    return cube(cx, cy, cz, s, "tint", GEM, rot, axis)


def arc(cx, cy, r, a0, a1, n, size, plane="xy", tint=METAL, color="tint_d"):
    """Arco feito de cubinhos (o formato de lua, aro, gancho). Angulos em graus; plano xy (de frente) ou zy (de lado)."""
    out = []
    for k in range(n):
        a = math.radians(a0 + (a1 - a0) * k / max(1, n - 1))
        x, y = cx + math.cos(a) * r, cy + math.sin(a) * r
        if plane == "xy":
            out.append(cube(x, y, C, size, color, tint))
        else:
            out.append(cube(C, y, x, size, color, tint))
    return out


def collar(y):
    """Encaixe de metal entre a haste e a ponta."""
    return [metal((6.9, y, 6.9), (9.1, y + 0.8, 9.1)), metal((7.3, y + 0.8, 7.3), (8.7, y + 1.6, 8.7))]


# ------------------------------------------------------------------ pontas (comecam em y ~ 13.5)
def head(kind):
    e = []
    if kind == "crescent":
        # lua crescente aberta em cima, segurando um orbe; um arco fino por dentro e um pingente
        e += collar(13.5)
        e += arc(C, 19.0, 4.2, 205, 515, 16, 1.15)
        e += arc(C, 19.0, 3.1, 220, 320, 6, 0.6)
        e.append(gem(C, 19.0, C, 2.8))
        e.append(gem(C, 19.0, C, 1.4, 45, "y"))
        e.append(metal((7.6, 15.0, 7.6), (8.4, 16.4, 8.4)))
        e.append(cube(C + 4.2 * math.cos(math.radians(155)), 19.0 + 4.2 * math.sin(math.radians(155)) - 1.4, C, 0.8, "tint", GEM))
    elif kind == "cage":
        e += collar(13.5)
        for dx, dz in ((-1.9, -1.9), (1.9, -1.9), (-1.9, 1.9), (1.9, 1.9)):
            e.append(metal((C + dx - 0.35, 15.0, C + dz - 0.35), (C + dx + 0.35, 20.5, C + dz + 0.35)))
        e.append(metal((6.0, 14.6, 6.0), (10.0, 15.2, 10.0)))
        e.append(metal((6.4, 20.5, 6.4), (9.6, 21.1, 9.6)))
        e.append(metal((7.3, 21.1, 7.3), (8.7, 22.0, 8.7)))
        e.append(metal((7.7, 22.0, 7.7), (8.3, 23.0, 8.3)))
        e.append(gem(C, 17.8, C, 3.0))
    elif kind == "ring":
        # aro de monge: argola grande com a gema no meio e argolinhas penduradas
        e += collar(13.5)
        e.append(metal((7.6, 15.0, 7.6), (8.4, 16.0, 8.4)))
        e += arc(C, 19.2, 4.4, 0, 345, 24, 0.9)
        e.append(gem(C, 19.2, C, 2.0, 45, "z"))
        e.append(metal((7.7, 16.0, 7.7), (8.3, 18.2, 8.3)))
        for side in (-1, 1):
            e += arc(C + side * 4.4, 17.6, 1.1, 0, 330, 8, 0.45)
    elif kind == "crystals":
        e.append(metal((6.4, 13.5, 6.4), (9.6, 14.6, 9.6)))
        e.append(metal((6.0, 14.6, 6.0), (10.0, 15.2, 10.0)))
        e.append(el((7.0, 15.0, 7.0), (9.0, 22.5, 9.0), "tint", GEM))
        e.append(el((7.4, 22.5, 7.4), (8.6, 23.2, 8.6), "tint", GEM))
        e.append(el((5.2, 14.8, 7.3), (6.8, 20.0, 8.7), "tint", GEM, 22.5, "z", [6.0, 15.0, 8.0]))
        e.append(el((9.2, 14.8, 7.3), (10.8, 19.0, 8.7), "tint", GEM, -22.5, "z", [10.0, 15.0, 8.0]))
        e.append(el((7.3, 14.8, 5.4), (8.7, 18.5, 6.8), "tint", GEM, -22.5, "x", [8.0, 15.0, 6.0]))
        e.append(el((7.3, 14.8, 9.2), (8.7, 18.0, 10.6), "tint", GEM, 22.5, "x", [8.0, 15.0, 10.0]))
    elif kind == "crook":
        # cajado de druida: a haste sobe e se enrola num gancho; a gema fica pendurada dentro da espiral
        e.append(el((7.4, 13.5, 7.4), (8.6, 18.5, 8.6), "oak_d"))
        e += arc(C + 2.6, 18.5, 2.6, 180, 0, 9, 1.2, color="oak_d", tint=None)
        e += arc(C + 2.6, 18.5, 2.6, 0, -120, 5, 1.0, color="oak_d", tint=None)
        e += arc(C + 2.6, 18.5, 1.3, -120, -300, 6, 0.7, color="oak", tint=None)
        e.append(metal((7.2, 14.5, 7.2), (8.8, 15.3, 8.8)))
        e.append(metal((C + 2.3, 16.2, 7.7), (C + 2.7, 17.2, 8.3)))
        e.append(gem(C + 2.5, 15.6, C, 1.5, 45, "y"))
        e.append(cube(C + 5.2, 18.0, C, 0.7, "green"))
        e.append(cube(C + 4.6, 20.6, C, 0.6, "green"))
    elif kind == "wings":
        e += collar(13.5)
        e.append(gem(C, 18.0, C, 2.4, 45, "y"))
        e.append(gem(C, 20.2, C, 1.2, 45, "y"))
        for side in (-1, 1):
            r = -22.5 * side
            for k in range(4):
                y0 = 15.3 + k * 1.4
                inner, outer = 1.6, 2.6 + k * 1.1
                x0, x1 = (C - outer, C - inner) if side < 0 else (C + inner, C + outer)
                e.append(el((x0, y0, 7.6), (x1, y0 + 1.1, 8.4), "tint_d", METAL, r, "z", [C + side * inner, y0, C]))
            e.append(el((C + side * 1.4 - 0.4, 14.8, 7.5), (C + side * 1.4 + 0.4, 21.0, 8.5), "tint_d", METAL))
    elif kind == "star":
        e += collar(13.5)
        e.append(metal((7.6, 15.0, 7.6), (8.4, 23.0, 8.4)))
        e.append(metal((3.6, 18.6, 7.6), (12.4, 19.4, 8.4)))
        e.append(metal((7.1, 16.5, 7.1), (8.9, 21.5, 8.9), 45, "y"))
        e.append(metal((5.5, 18.1, 7.3), (10.5, 19.9, 8.7), 45, "x"))
        e.append(gem(C, 19.0, C, 2.4, 45, "z"))
        for a in (45, 135, 225, 315):
            x, y = C + math.cos(math.radians(a)) * 2.8, 19.0 + math.sin(math.radians(a)) * 2.8
            e.append(cube(x, y, C, 0.7, "tint", GEM))
    elif kind == "trident":
        e += collar(13.5)
        e.append(metal((5.0, 15.0, 7.5), (11.0, 15.9, 8.5)))
        e.append(metal((7.5, 15.9, 7.5), (8.5, 22.0, 8.5)))
        e.append(metal((7.7, 22.0, 7.7), (8.3, 23.2, 8.3)))
        for side in (-1, 1):
            x = C + side * 2.6
            e.append(metal((x - 0.45, 15.9, 7.55), (x + 0.45, 20.0, 8.45)))
            e.append(metal((x - 0.3 + side * 0.4, 20.0, 7.7), (x + 0.3 + side * 0.4, 21.0, 8.3)))
        e.append(gem(C, 17.0, C, 1.8, 45, "y"))
        e.append(gem(C, 19.5, C, 1.2, 45, "y"))
    elif kind == "halo":
        e += collar(13.5)
        e.append(el((7.2, 15.2, 7.2), (8.8, 22.0, 8.8), "tint", GEM))
        e.append(el((7.6, 22.0, 7.6), (8.4, 23.0, 8.4), "tint", GEM))
        e += arc(C, 18.6, 3.4, 0, 345, 20, 0.7)
        e += arc(C, 18.6, 3.4, 0, 345, 20, 0.55, plane="zy")
        for a in (30, 150, 210, 330):
            x, y = C + math.cos(math.radians(a)) * 4.6, 18.6 + math.sin(math.radians(a)) * 4.6
            e.append(cube(x, y, C, 0.6, "tint", GEM))
    elif kind == "orb":
        e += collar(13.5)
        e.append(metal((6.5, 15.0, 6.5), (9.5, 15.8, 9.5)))
        for dx, dz in ((-1.8, 0), (1.8, 0), (0, -1.8), (0, 1.8)):
            e.append(metal((C + dx - 0.35, 15.0, C + dz - 0.35), (C + dx + 0.35, 18.2, C + dz + 0.35)))
        e.append(gem(C, 18.4, C, 3.6))
        e.append(gem(C, 18.4, C, 2.4, 45, "y"))
    return e


# ------------------------------------------------------------------ hastes (de y = -7 ate 13.5)
def shaft(kind):
    e = []
    lo, hi = -7.0, 13.5
    if kind == "wood":
        e.append(el((7.35, lo + 1, 7.35), (8.65, hi, 8.65), "wood"))
        e.append(metal((7.2, lo, 7.2), (8.8, lo + 1, 8.8)))
        for y in (1.5, 9.0):
            e.append(metal((7.15, y, 7.15), (8.85, y + 0.6, 8.85)))
        e.append(el((7.25, 2.1, 7.25), (8.75, 5.5, 8.75), "grip"))
    elif kind == "white":
        # haste branca com aneis de metal e empunhadura azul (estilo dos cajados de maga de anime)
        e.append(el((7.4, lo + 1.2, 7.4), (8.6, hi, 8.6), "white"))
        e.append(metal((7.2, lo, 7.2), (8.8, lo + 1.2, 8.8)))
        for y in (0.0, 6.4, 11.6):
            e.append(metal((7.2, y, 7.2), (8.8, y + 0.5, 8.8)))
            e.append(metal((7.3, y + 0.8, 7.3), (8.7, y + 1.1, 8.7)))
        e.append(el((7.3, 1.6, 7.3), (8.7, 5.6, 8.7), "blue"))
    elif kind == "twisted":
        n = 14
        step = (hi - lo) / n
        for k in range(n):
            off = 0.28 * math.sin(k * 1.1)
            offz = 0.28 * math.cos(k * 1.1)
            e.append(el((7.4 + off, lo + k * step, 7.4 + offz), (8.6 + off, lo + (k + 1) * step + 0.05, 8.6 + offz),
                        "oak" if k % 2 else "oak_d"))
        e.append(metal((7.2, 10.0, 7.2), (8.8, 10.6, 8.8)))
    elif kind == "bone":
        e.append(el((7.45, lo, 7.45), (8.55, hi, 8.55), "bone"))
        for y in (lo, -2.0, 3.0, 8.0, hi - 1.2):
            e.append(el((7.15, y, 7.15), (8.85, y + 1.0, 8.85), "bone"))
        e.append(el((7.3, 0.0, 7.3), (8.7, 2.6, 8.7), "grip"))
    elif kind == "obsidian":
        e.append(el((7.2, lo + 1.4, 7.2), (8.8, hi, 8.8), "obsidian"))
        e.append(metal((7.6, lo, 7.6), (8.4, lo + 1.4, 8.4)))
        e.append(metal((7.0, lo + 1.4, 7.0), (9.0, lo + 2.0, 9.0)))
        for y in (-2.5, 2.5, 7.5):
            e.append(el((7.1, y, 7.6), (8.9, y + 0.7, 8.4), "tint", GEM))
            e.append(el((7.6, y, 7.1), (8.4, y + 0.7, 8.9), "tint", GEM))
        e.append(metal((7.0, 11.8, 7.0), (9.0, hi, 9.0)))
    elif kind == "gilded":
        e.append(metal((7.4, lo + 1, 7.4), (8.6, hi, 8.6)))
        e.append(metal((7.1, lo, 7.1), (8.9, lo + 1, 8.9)))
        e.append(el((7.25, 1.0, 7.25), (8.75, 5.0, 8.75), "grip"))
        for y in (-3.0, 7.5, 10.5):
            e.append(el((7.25, y, 7.25), (8.75, y + 0.4, 8.75), "tint", GEM))
    elif kind == "crystal":
        n = 6
        step = (hi - lo) / n
        for k in range(n):
            y0 = lo + k * step
            e.append(el((7.45, y0 + 0.5, 7.45), (8.55, y0 + step, 8.55), "tint", GEM))
            e.append(metal((7.2, y0, 7.2), (8.8, y0 + 0.5, 8.8)))
    elif kind == "branch":
        n = 10
        step = (hi - lo) / n
        for k in range(n):
            off = 0.35 * math.sin(k * 2.3)
            w = 0.7 - 0.02 * k
            e.append(el((C - w + off, lo + k * step, C - w), (C + w + off, lo + (k + 1) * step + 0.05, C + w), "brown" if k % 3 else "brown_d"))
        e.append(el((8.4, 3.0, 7.7), (10.2, 3.6, 8.3), "brown", None, 45, "z", [8.4, 3.0, 8.0]))
        e.append(el((5.6, 8.0, 7.7), (7.6, 8.6, 8.3), "brown", None, -45, "z", [7.6, 8.0, 8.0]))
        e.append(cube(10.0, 4.8, C, 0.8, "green"))
        e.append(cube(5.8, 9.6, C, 0.8, "green"))
    return e


def display():
    return {
        "thirdperson_righthand": {"rotation": [90, 0, 0], "translation": [0, 3.5, 0.5], "scale": [0.85, 0.85, 0.85]},
        "thirdperson_lefthand": {"rotation": [90, 0, 0], "translation": [0, 3.5, 0.5], "scale": [0.85, 0.85, 0.85]},
        "firstperson_righthand": {"rotation": [0, -90, 8], "translation": [2.5, 2.0, 0.5], "scale": [0.55, 0.55, 0.55]},
        "firstperson_lefthand": {"rotation": [0, 90, -8], "translation": [2.5, 2.0, 0.5], "scale": [0.55, 0.55, 0.55]},
        "gui": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
        "fixed": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
        "head": {"rotation": [0, 0, 0], "translation": [0, 4, 0], "scale": [0.6, 0.6, 0.6]},
    }


def model(h, s):
    return {
        "textures": {"0": "arcane:item/draw_palette", "particle": "arcane:item/draw_palette"},
        "gui_light": "front",
        "display": display(),
        "elements": shaft(s) + head(h),
    }


def main():
    palette()
    out = ASSETS / "models/item"
    for old in out.glob("scepter_*.json"):
        old.unlink()
    overrides = []
    for hi, h in enumerate(HEADS):
        for si, s in enumerate(SHAFTS):
            name = f"scepter_{h}_{s}"
            m = model(h, s)
            for e in m["elements"]:
                for k in range(3):
                    assert -16 <= e["from"][k] <= 32 and -16 <= e["to"][k] <= 32, (name, e)
            (out / f"{name}.json").write_text(json.dumps(m, indent=1) + "\n", encoding="utf-8")
            overrides.append({"predicate": {"arcane:head": hi, "arcane:shaft": si}, "model": f"arcane:item/{name}"})
    base = model(HEADS[0], SHAFTS[0])
    base["overrides"] = overrides
    (out / "scepter.json").write_text(json.dumps(base, indent=1) + "\n", encoding="utf-8")
    print(len(overrides), "modelos de cetro")


if __name__ == "__main__":
    main()
