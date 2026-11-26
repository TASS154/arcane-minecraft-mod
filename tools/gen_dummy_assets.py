"""Texturas do Boneco de Treino Arcano (Fase H): corpo de lona com costuras, alvo rúnico no peito e o item.

Uso:  python tools/gen_dummy_assets.py
Requer: Pillow. Placeholder: NÃO visto em jogo.
"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/arcane"
rnd = random.Random(20261125)

CANVAS = [(176, 146, 98), (163, 134, 88), (188, 158, 108), (150, 122, 80)]
STITCH = (92, 64, 40)
RUNE = (120, 220, 255)
RING = (178, 34, 52)


def canvas_fill(img, x0, y0, w, h):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            c = CANVAS[(x * 3 + y * 7 + rnd.randint(0, 1)) % len(CANVAS)]
            img.putpixel((x, y), c + (255,))


def entity_texture():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    # cabeca (0,0 32x16), corpo (16,16 24x16), braco (40,16 16x16), perna (0,16 16x16)
    canvas_fill(img, 0, 0, 32, 16)
    canvas_fill(img, 16, 16, 24, 16)
    canvas_fill(img, 40, 16, 16, 16)
    canvas_fill(img, 0, 16, 16, 16)
    # rosto (frente da cabeca: 8..16, 8..16): olhos em X costurados e boca reta
    for (cx, cy) in ((10, 11), (13, 11)):
        for d in (-1, 0, 1):
            img.putpixel((cx + d, cy + d), STITCH + (255,))
            img.putpixel((cx + d, cy - d), STITCH + (255,))
    for x in range(10, 15):
        img.putpixel((x, 14), STITCH + (255,))
    # costura vertical do corpo (frente: 20..28, 20..32) e alvo runico no peito
    for y in range(20, 32, 2):
        img.putpixel((24, y), STITCH + (255,))
    cx, cy = 23.5, 25.5
    for y in range(20, 32):
        for x in range(20, 28):
            r = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if 3.2 <= r < 4.0:
                img.putpixel((x, y), RING + (255,))
            elif r < 1.2:
                img.putpixel((x, y), RUNE + (255,))
    # faixas nas costuras dos membros
    for x in range(0, 16):
        img.putpixel((x, 26), STITCH + (255,))
    for x in range(40, 56):
        img.putpixel((x, 24), STITCH + (255,))
    out = ROOT / "textures/entity/training_dummy.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print("  ", out)


def item_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # poste
    for y in range(9, 16):
        img.putpixel((7, y), (110, 78, 46, 255))
        img.putpixel((8, y), (96, 66, 38, 255))
    # braco
    for x in range(3, 13):
        img.putpixel((x, 7), (163, 134, 88, 255))
    # tronco e cabeca de lona
    for y in range(4, 11):
        for x in range(5, 11):
            img.putpixel((x, y), CANVAS[(x + y) % 4] + (255,))
    for y in range(0, 4):
        for x in range(6, 10):
            img.putpixel((x, y), CANVAS[(x * 2 + y) % 4] + (255,))
    # alvo
    for (x, y) in ((7, 6), (8, 6), (7, 8), (8, 8), (6, 7), (9, 7)):
        img.putpixel((x, y), RING + (255,))
    img.putpixel((7, 7), RUNE + (255,))
    img.putpixel((8, 7), RUNE + (255,))
    out = ROOT / "textures/item/training_dummy.png"
    img.save(out)
    print("  ", out)
    model = ROOT / "models/item/training_dummy.json"
    model.write_text('{\n  "parent": "minecraft:item/generated",\n  "textures": {\n    "layer0": "arcane:item/training_dummy"\n  }\n}\n',
                     encoding="utf-8")
    print("  ", model)


if __name__ == "__main__":
    entity_texture()
    item_texture()
