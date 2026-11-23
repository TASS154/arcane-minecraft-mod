"""Textura da Hidra (128x128): escamas claras em tons de cinza (o renderizador tinge pela cor do estagio), ventre
mais claro, chifres de osso e a camada de olhos que brilham.

    python tools/gen_hydra_texture.py
"""
from pathlib import Path
import random
from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/arcane/textures/entity/hydra"


def scales(img, box, base, dark, light, seed):
    rnd = random.Random(seed)
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            # escamas em losango: linhas alternadas deslocadas, borda escura, miolo com brilho
            row = (y - y0) // 2
            u = (x - x0 + (row % 2) * 2) % 4
            v = (y - y0) % 2
            c = base
            if u == 0 or (v == 1 and u == 3):
                c = dark
            elif u == 2 and v == 0:
                c = light
            n = rnd.randint(-8, 8)
            img.putpixel((x, y), tuple(max(0, min(255, ch + n)) for ch in c) + (255,))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    base, dark, light = (170, 170, 165), (95, 95, 92), (215, 215, 205)
    scales(img, (0, 0, 128, 128), base, dark, light, 7)
    # ventre do corpo (face de baixo do corpo: u 40..56, v 0..24) e dos pescocos mais claro e liso
    for (x0, y0, x1, y1) in [(40, 0, 56, 24), (98, 0, 102, 10), (96, 16, 99, 25)]:
        for y in range(y0, y1):
            for x in range(x0, x1):
                k = 230 - ((y - y0) % 3) * 12
                img.putpixel((x, y), (k, k - 6, k - 18, 255))
    # chifres (osso) e dentes na mandibula
    for y in range(30, 34):
        for x in range(112, 118):
            img.putpixel((x, y), (236, 226, 200, 255))
    for x in range(91, 96):
        img.putpixel((x, 53), (245, 240, 230, 255))
    # boca escura na frente da cabeca (linha de baixo da face norte)
    for x in range(92, 98):
        img.putpixel((x, 42), (40, 10, 14, 255))
    # olhos: face norte da cabeca = u 92..98, v 38..43
    for (x, y) in [(93, 39), (96, 39)]:
        img.putpixel((x, y), (255, 210, 60, 255))
    img.save(OUT / "hydra.png")
    eyes = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    for (x, y) in [(93, 39), (96, 39)]:
        eyes.putpixel((x, y), (255, 230, 90, 255))
    eyes.save(OUT / "hydra_eyes.png")
    print("hydra.png e hydra_eyes.png")


if __name__ == "__main__":
    main()
