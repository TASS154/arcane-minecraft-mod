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
]
INDEX = {name: i for i, (name, _) in enumerate(COLORS)}


def palette() -> None:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i, (_, c) in enumerate(COLORS):
        x, y = (i % 4) * 4, (i // 4) * 4
        for dx in range(4):
            for dy in range(4):
                img.putpixel((x + dx, y + dy), c)
    path = ASSETS / "textures/item/draw_palette.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def box(frm, to, color, rot=None):
    i = INDEX[color]
    u, v = (i % 4) * 4 + 1, (i // 4) * 4 + 1
    face = {"uv": [u, v, u + 2, v + 2], "texture": "#0"}
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
