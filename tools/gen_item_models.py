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
    ("blue_d", (20, 30, 80, 255)),
    ("blood", (190, 30, 40, 255)),
    ("blood_d", (110, 10, 20, 255)),
    ("ghost", (235, 250, 255, 150)),
    ("ghost_d", (200, 230, 255, 110)),
    ("glass", (215, 235, 255, 255)),
    ("sheen", (255, 255, 255, 255)),
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
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "gui": {"rotation": gui_rot, "translation": [0, 0, 0], "scale": [gui_scale] * 3},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
        },
        "elements": elements,
    }


def pen():
    """Caneta de pena: ponta, anel de ouro, empunhadura, corpo, gema e pluma; inclinada 45 graus."""
    r = 45
    return model([
        box((7.4, 0, 7.4), (8.6, 2.2, 8.6), "dark", r),      # ponta
        box((6.8, 2.2, 6.8), (9.2, 3.4, 9.2), "gold", r),    # anel
        box((6.6, 3.4, 6.6), (9.4, 6.4, 9.4), "lilac", r),   # empunhadura
        box((6.2, 6.4, 6.2), (9.8, 12.0, 9.8), "violet", r),  # corpo
        box((6.0, 6.4, 6.0), (10.0, 7.0, 10.0), "violet_d", r),
        box((6.6, 12.0, 6.6), (9.4, 13.4, 9.4), "gold", r),  # tampa
        box((7.2, 13.4, 7.2), (8.8, 14.4, 8.8), "lilac", r),  # gema
        box((7.85, 11.0, 5.4), (8.15, 18.0, 10.6), "white", r),  # pluma
        box((7.85, 14.0, 4.6), (8.15, 17.0, 5.4), "lilac", r),
    ], [25, -40, 0], 0.85)


def bottle(light, dark):
    return model([
        box((4.5, 0.0, 4.5), (11.5, 7.5, 11.5), dark),        # corpo
        box((4.5, 5.0, 4.5), (11.5, 7.5, 11.5), light),       # nivel claro
        box((4.0, 0.0, 4.0), (12.0, 0.8, 12.0), "dark"),      # base
        box((6.0, 7.5, 6.0), (10.0, 9.5, 10.0), "glass"),     # gargalo
        box((5.6, 9.0, 5.6), (10.4, 9.9, 10.4), "gold"),      # aro
        box((6.4, 9.9, 6.4), (9.6, 11.6, 9.6), "cork"),       # rolha
        box((5.0, 1.5, 4.4), (6.2, 6.5, 4.6), "sheen"),       # brilho
    ], [25, -40, 0], 1.0)


def main():
    palette()
    out = ASSETS / "models/item"
    out.mkdir(parents=True, exist_ok=True)
    models = {
        "arcane_pen": pen(),
        "common_ink": bottle("blue", "blue_d"),
        "blood_ink": bottle("blood", "blood_d"),
        "invisible_ink": bottle("ghost", "ghost_d"),
    }
    for name, m in models.items():
        (out / f"{name}.json").write_text(json.dumps(m, indent=2) + "\n", encoding="utf-8")
        print("  ", f"models/item/{name}.json")


if __name__ == "__main__":
    main()
