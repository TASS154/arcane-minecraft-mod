"""Modelos 3D dos itens de desenho (caneta e frascos de tinta), gerados por codigo.

Uso: python tools/gen_item_models.py
Cada modelo usa uma paleta (item/draw_palette.png, quadrados de 4x4 px) e caixas com UV apontando para a cor.
Rodar depois de gen_textures.py / gen_art_*.py (nao toca nas texturas deles).
"""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/arcane"

# nome -> cor (RGBA). A ordem define o quadrado na paleta (4 por linha).
COLORS = [
    ("dark", (20, 12, 30, 255)),
    ("gold", (230, 190, 70, 255)),
    ("gold_d", (150, 110, 30, 255)),
    ("violet", (150, 90, 210, 255)),
    ("violet_d", (95, 50, 150, 255)),
    ("lilac", (205, 165, 245, 255)),
    ("white", (240, 235, 255, 255)),
    ("cork", (150, 105, 60, 255)),
    ("blue", (40, 60, 150, 255)),
    ("blood", (190, 30, 40, 255)),
    ("blood_d", (110, 10, 20, 255)),
    ("ghost", (235, 250, 255, 150)),
    ("ghost_d", (200, 230, 255, 110)),
    ("glass", (60, 75, 120, 255)),
    ("ink_dark", (14, 18, 44, 255)),
    ("cork_l", (196, 152, 96, 255)),
    ("brown", (110, 75, 45, 255)),
    ("brown_d", (72, 48, 28, 255)),
    ("green", (80, 170, 70, 255)),
    ("grey", (140, 140, 150, 255)),
    ("grey_d", (88, 88, 100, 255)),
    ("silver", (205, 210, 222, 255)),
    ("ice", (170, 225, 255, 255)),
    ("ice_d", (110, 180, 235, 255)),
    ("oak", (190, 150, 90, 255)),
    ("oak_d", (125, 90, 50, 255)),
    ("blue_gem", (60, 140, 255, 255)),
    ("red", (200, 50, 45, 255)),
    ("red_d", (130, 25, 25, 255)),
    ("orange", (235, 150, 60, 255)),
    ("steel", (170, 180, 190, 255)),
    ("teal", (60, 200, 190, 255)),
    ("teal_d", (30, 120, 130, 255)),
]
INDEX = {name: i for i, (name, _) in enumerate(COLORS)}


def palette() -> None:
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for i, (_, c) in enumerate(COLORS):
        x, y = (i % 8) * 4, (i // 8) * 4
        for dx in range(4):
            for dy in range(4):
                img.putpixel((x + dx, y + dy), c)
    path = ASSETS / "textures/item/draw_palette.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def box(frm, to, color, rot=None):
    i = INDEX[color]
    u, v = (i % 8) * 2 + 0.5, (i // 8) * 2 + 0.5  # paleta 32x32 em coordenadas 0-16: cada cor ocupa 2x2
    face = {"uv": [u, v, u + 1, v + 1], "texture": "#0"}
    el = {"from": list(frm), "to": list(to), "faces": {f: dict(face) for f in ("north", "south", "east", "west", "up", "down")}}
    if rot is not None:
        el["rotation"] = {"angle": rot, "axis": "z", "origin": [8, 8, 8]}
    return el


def model(elements, gui_rot, gui_scale):
    return {
        "textures": {"0": "arcane:item/draw_palette", "particle": "arcane:item/draw_palette"},
        "gui_light": "side",
        "display": {
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.55, 0.55, 0.55]},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, 0.5], "scale": [0.55, 0.55, 0.55]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.5, 0.5, 0.5]},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.5, 0.5, 0.5]},
            "gui": {"rotation": gui_rot, "translation": [0, 0, 0], "scale": [gui_scale] * 3},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
        },
        "elements": elements,
    }


def pen():
    """Caneta fina: ponta, anel de ouro, empunhadura, corpo, faixa e gema. Inclinada -45 graus (ponta embaixo a esquerda)."""
    r = -45
    return model([
        box((7.7, 0, 7.7), (8.3, 2.0, 8.3), "dark", r),       # ponta
        box((7.3, 2.0, 7.3), (8.7, 2.8, 8.7), "gold", r),     # anel
        box((7.4, 2.8, 7.4), (8.6, 5.5, 8.6), "lilac", r),    # empunhadura
        box((7.3, 5.5, 7.3), (8.7, 12.0, 8.7), "violet", r),  # corpo
        box((7.2, 12.0, 7.2), (8.8, 12.8, 8.8), "gold", r),   # faixa
        box((7.5, 12.8, 7.5), (8.5, 13.8, 8.5), "lilac", r),  # gema
    ], [25, -40, 0], 1.0)


def block_pen(kind):
    """Canetas de bloco: mesma familia da Pena Arcana, cada uma com forma propria. Inclinadas -45 graus."""
    r = -45
    if kind == "earth":    # corpo grosso de terra, empunhadura verde, gema redonda (cubo grande)
        els = [box((7.6, 0, 7.6), (8.4, 1.6, 8.4), "dark", r), box((7.0, 1.6, 7.0), (9.0, 4.6, 9.0), "green", r),
               box((6.8, 4.6, 6.8), (9.2, 11.0, 9.2), "brown", r), box((6.6, 11.0, 6.6), (9.4, 11.8, 9.4), "brown_d", r),
               box((7.0, 11.8, 7.0), (9.0, 13.8, 9.0), "green", r)]
    elif kind == "stone":  # corpo cinza facetado, pomo quadrado largo
        els = [box((7.6, 0, 7.6), (8.4, 2.0, 8.4), "dark", r), box((7.2, 2.0, 7.2), (8.8, 3.0, 8.8), "grey_d", r),
               box((7.3, 3.0, 7.3), (8.7, 11.0, 8.7), "grey", r), box((6.4, 11.0, 6.4), (9.6, 13.4, 9.6), "grey_d", r),
               box((6.9, 13.4, 6.9), (9.1, 13.9, 9.1), "grey", r)]
    elif kind == "metal":  # fina e longa, ponta comprida, dois aneis e gema azul
        els = [box((7.7, 0, 7.7), (8.3, 3.4, 8.3), "dark", r), box((7.2, 3.4, 7.2), (8.8, 4.0, 8.8), "silver", r),
               box((7.35, 4.0, 7.35), (8.65, 12.0, 8.65), "silver", r), box((7.2, 12.0, 7.2), (8.8, 12.6, 8.8), "gold", r),
               box((7.2, 7.6, 7.2), (8.8, 8.2, 8.8), "gold", r), box((7.4, 12.6, 7.4), (8.6, 14.0, 8.6), "blue_gem", r)]
    elif kind == "snow":   # corpo de gelo e um cristal comprido no topo
        els = [box((7.6, 0, 7.6), (8.4, 2.0, 8.4), "ice_d", r), box((7.2, 2.0, 7.2), (8.8, 3.0, 8.8), "white", r),
               box((7.3, 3.0, 7.3), (8.7, 10.5, 8.7), "ice", r), box((7.1, 10.5, 7.1), (8.9, 11.2, 8.9), "white", r),
               box((7.5, 11.2, 7.5), (8.5, 12.8, 8.5), "ice_d", r), box((7.75, 12.8, 7.75), (8.25, 15.0, 8.25), "white", r)]
    elif kind == "phoenix":   # corpo vermelho-alaranjado, penacho de chama dourado no topo
        els = [box((7.6, 0, 7.6), (8.4, 2.0, 8.4), "dark", r), box((7.0, 2.0, 7.0), (9.0, 3.4, 9.0), "gold", r),
               box((7.2, 3.4, 7.2), (8.8, 10.5, 8.8), "red", r), box((7.0, 6.0, 7.0), (9.0, 6.8, 9.0), "orange", r),
               box((7.0, 10.5, 7.0), (9.0, 11.5, 9.0), "gold", r), box((7.3, 11.5, 7.3), (8.7, 13.5, 8.7), "orange", r),
               box((7.6, 13.5, 7.6), (8.4, 15.2, 8.4), "gold", r), box((8.4, 12.0, 7.7), (9.8, 14.0, 8.3), "red", r)]
    elif kind == "creature":  # corpo roxo escuro e uma caveirinha branca no topo
        els = [box((7.6, 0, 7.6), (8.4, 2.0, 8.4), "dark", r), box((7.0, 2.0, 7.0), (9.0, 3.0, 9.0), "silver", r),
               box((7.2, 3.0, 7.2), (8.8, 10.5, 8.8), "violet_d", r), box((7.0, 10.5, 7.0), (9.0, 11.2, 9.0), "silver", r),
               box((6.6, 11.2, 6.6), (9.4, 14.0, 9.4), "white", r), box((6.8, 12.6, 9.4), (7.6, 13.4, 9.5), "dark", r),
               box((8.4, 12.6, 9.4), (9.2, 13.4, 9.5), "dark", r)]
    elif kind == "redstone":  # corpo vermelho, pino de redstone no topo e contatos dourados
        els = [box((7.6, 0, 7.6), (8.4, 2.0, 8.4), "dark", r), box((7.0, 2.0, 7.0), (9.0, 3.2, 9.0), "gold", r),
               box((7.2, 3.2, 7.2), (8.8, 11.5, 8.8), "red", r), box((7.0, 6.0, 7.0), (9.0, 6.8, 9.0), "red_d", r),
               box((7.0, 11.5, 7.0), (9.0, 12.3, 9.0), "gold", r), box((7.4, 12.3, 7.4), (8.6, 14.3, 8.6), "red_d", r)]
    elif kind == "food":      # corpo laranja, empunhadura de pao e uma baga no topo
        els = [box((7.6, 0, 7.6), (8.4, 1.8, 8.4), "dark", r), box((7.0, 1.8, 7.0), (9.0, 5.0, 9.0), "cork_l", r),
               box((7.2, 5.0, 7.2), (8.8, 11.5, 8.8), "orange", r), box((7.0, 11.5, 7.0), (9.0, 12.2, 9.0), "gold", r),
               box((7.0, 12.2, 7.0), (9.0, 14.0, 9.0), "red", r), box((7.6, 14.0, 7.6), (8.4, 14.8, 8.4), "green", r)]
    elif kind == "weapon":    # corpo de aco, guarda larga e pomo de ouro
        els = [box((7.7, 0, 7.7), (8.3, 2.6, 8.3), "dark", r), box((7.3, 2.6, 7.3), (8.7, 10.5, 8.7), "steel", r),
               box((5.8, 10.5, 7.0), (10.2, 11.3, 9.0), "gold", r), box((7.2, 11.3, 7.2), (8.8, 13.0, 8.8), "grey_d", r),
               box((7.0, 13.0, 7.0), (9.0, 14.0, 9.0), "gold", r)]
    elif kind == "potion":    # corpo verde-azulado, bulbo de vidro no topo
        els = [box((7.6, 0, 7.6), (8.4, 2.0, 8.4), "dark", r), box((7.2, 2.0, 7.2), (8.8, 4.5, 8.8), "teal_d", r),
               box((7.3, 4.5, 7.3), (8.7, 10.0, 8.7), "teal", r), box((7.0, 10.0, 7.0), (9.0, 10.8, 9.0), "silver", r),
               box((6.4, 10.8, 6.4), (9.6, 13.6, 9.6), "glass", r), box((6.8, 11.2, 6.8), (9.2, 12.8, 9.2), "teal", r)]
    else:                  # madeira: corpo de carvalho, empunhadura escura e uma folha
        els = [box((7.6, 0, 7.6), (8.4, 1.8, 8.4), "dark", r), box((7.2, 1.8, 7.2), (8.8, 5.0, 8.8), "oak_d", r),
               box((7.3, 5.0, 7.3), (8.7, 12.0, 8.7), "oak", r), box((7.2, 12.0, 7.2), (8.8, 12.7, 8.8), "oak_d", r),
               box((7.6, 12.7, 7.6), (8.4, 13.3, 8.4), "green", r), box((8.4, 12.7, 7.7), (10.2, 13.1, 8.3), "green", r)]
    return model(els, [25, -40, 0], 1.0)


def bottle(light, dark):
    """Frasco quadrado e baixo, gargalo de vidro e rolha larga. Caixas empilhadas, sem faces coplanares sobrepostas."""
    return model([
        box((4.0, 0.0, 4.0), (12.0, 5.0, 12.0), dark),         # corpo
        box((4.8, 5.0, 4.8), (11.2, 6.2, 11.2), light),        # ombro
        box((5.8, 6.2, 5.8), (10.2, 7.8, 10.2), "glass"),      # gargalo
        box((5.4, 7.8, 5.4), (10.6, 8.4, 10.6), light),        # aro
        box((6.0, 8.4, 6.0), (10.0, 9.6, 10.0), "cork"),       # rolha (base)
        box((5.2, 9.6, 5.2), (10.8, 10.8, 10.8), "cork_l"),    # rolha (topo)
    ], [25, -40, 0], 1.0)


def solvent_texture():
    """Po solvente: um monte de po claro num fundo transparente (sprite plano)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = [(7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7),
          (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8), (3, 9), (4, 9), (5, 9), (6, 9),
          (7, 9), (8, 9), (9, 9), (10, 9), (11, 9), (12, 9), (4, 10), (5, 10), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10), (11, 10)]
    for i, (x, y) in enumerate(px):
        shade = 225 - (y - 5) * 9 - (i * 7) % 18
        img.putpixel((x, y), (shade, shade, min(255, shade + 20), 255))
    for x, y in ((6, 9), (9, 8), (7, 7)):
        img.putpixel((x, y), (255, 255, 255, 255))
    path = ASSETS / "textures/item/solvent_powder.png"
    img.save(path)


def main():
    palette()
    solvent_texture()
    (ASSETS / "models/item").mkdir(parents=True, exist_ok=True)
    (ASSETS / "models/item/solvent_powder.json").write_text(json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": "arcane:item/solvent_powder"}}, indent=2) + "\n", encoding="utf-8")
    out =ASSETS / "models/item"
    out.mkdir(parents=True, exist_ok=True)
    models = {
        "arcane_pen": pen(),
        "phoenix_pen": block_pen("phoenix"),
        "creature_pen": block_pen("creature"),
        "redstone_pen": block_pen("redstone"),
        "food_pen": block_pen("food"),
        "weapon_pen": block_pen("weapon"),
        "potion_pen": block_pen("potion"),
        "earth_pen": block_pen("earth"),
        "stone_pen": block_pen("stone"),
        "metal_pen": block_pen("metal"),
        "snow_pen": block_pen("snow"),
        "wood_pen": block_pen("wood"),
        "common_ink": bottle("blue", "ink_dark"),
        "blood_ink": bottle("blood", "blood_d"),
        "invisible_ink": bottle("ghost", "ghost_d"),
        "permanent_ink": bottle("gold", "gold_d"),
        "mixed_common_ink": bottle("gold", "ink_dark"),
        "mixed_blood_ink": bottle("gold", "blood_d"),
        "mixed_invisible_ink": bottle("gold", "ghost_d"),
    }
    for name, m in models.items():
        (out / f"{name}.json").write_text(json.dumps(m, indent=2) + "\n", encoding="utf-8")
        print("  ", f"models/item/{name}.json")


if __name__ == "__main__":
    main()
