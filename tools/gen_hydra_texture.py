"""Textura da Hidra arcana (256x128) e a camada que brilha (hydra_eyes.png).

- Escamas de obsidiana em losangos de 4x3, com borda escura, reflexo roxo no alto de cada escama e variacao de
  tom em manchas grandes (ruido suave), para nao parecer um bloco liso.
- Veias de runas: linhas longas que se ramificam por toda a textura (qualquer face do modelo pega veias), com
  pequenos glifos ao longo delas. Na textura base ficam ciano escuro; na camada emissiva, claras (o renderer tinge
  pela cor do glifo e faz pulsar).
- Osso (u 224-255, v 0-47): dentes, chifres, garras, espinhos, com veios.
- Olhos (u 224-239, v 96-103).

    python tools/gen_hydra_texture.py
"""
from pathlib import Path
import math
import random
from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/arcane/textures/entity/hydra"
W, H = 256, 128
BONE = (224, 0, 256, 48)
EYES = (224, 96, 240, 104)


def inside(x, y, box):
    return box[0] <= x < box[2] and box[1] <= y < box[3]


def smooth_noise(rnd, cells=8):
    grid = [[rnd.random() for _ in range(cells + 1)] for _ in range(cells + 1)]

    def f(x, y):
        gx, gy = x / W * cells, y / H * cells
        x0, y0 = int(gx), int(gy)
        tx, ty = gx - x0, gy - y0
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = grid[y0][x0] * (1 - tx) + grid[y0][x0 + 1] * tx
        b = grid[y0 + 1][x0] * (1 - tx) + grid[y0 + 1][x0 + 1] * tx
        return a * (1 - ty) + b * ty
    return f


def main():
    rnd = random.Random(2024)
    noise = smooth_noise(rnd, 10)
    base = Image.new("RGBA", (W, H))
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    # escamas: losangos 4 de largura x 3 de altura, fileiras deslocadas
    for y in range(H):
        for x in range(W):
            row = y // 3
            u = (x + (row % 2) * 2) % 4
            v = y % 3
            n = noise(x, y)
            shade = 0.75 + 0.5 * n
            if u == 0 or v == 2:
                c = (12, 8, 18)                     # junta entre escamas
            elif v == 0 and u in (1, 2):
                c = (78, 54, 108)                   # reflexo no alto da escama
            elif u == 3:
                c = (26, 18, 36)                    # lado na sombra
            else:
                c = (40, 28, 56)                    # miolo
            j = rnd.randint(-4, 4)
            base.putpixel((x, y), tuple(max(0, min(255, int(ch * shade) + j)) for ch in c) + (255,))
    # veias: caminhos longos que se ramificam, com glifos de 3x3 ao longo
    def vein(x, y, steps, depth):
        dirs = ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1))
        d = rnd.choice(dirs)
        for s in range(steps):
            if inside(x, y, BONE) or inside(x, y, EYES):
                return
            bright = 0.65 + 0.35 * rnd.random()
            base.putpixel((x, y), (30, int(140 * bright), int(160 * bright), 255))
            glow.putpixel((x, y), (90, int(235 * bright), 255, 255))
            if s % 14 == 7 and rnd.random() < 0.5:  # glifo: um pequeno simbolo em volta da veia
                for gx, gy in rnd.choice((((1, 0), (1, 1), (0, 1)), ((-1, -1), (1, -1), (0, 1)), ((1, 1), (-1, 1), (-1, -1)))):
                    px, py = (x + gx) % W, (y + gy) % H
                    if not inside(px, py, BONE) and not inside(px, py, EYES):
                        base.putpixel((px, py), (40, 170, 190, 255))
                        glow.putpixel((px, py), (130, 245, 255, 230))
            if depth < 2 and rnd.random() < 0.035:
                vein(x, y, steps // 2, depth + 1)   # ramo
            if rnd.random() < 0.07:  # rachadura: muda pouco de rumo, quase sempre para um vizinho do rumo atual
                i = dirs.index(d)
                d = dirs[(i + rnd.choice((4, 5, 6, 7, 0, 1))) % len(dirs)] if rnd.random() < 0.3 else rnd.choice(dirs)
            x, y = (x + d[0]) % W, (y + d[1]) % H

    for _ in range(60):
        vein(rnd.randrange(W), rnd.randrange(H), rnd.randint(30, 80), 0)
    # osso: marfim com veios e base mais escura
    for y in range(BONE[1], BONE[3]):
        for x in range(BONE[0], BONE[2]):
            n = rnd.randint(-10, 8)
            grain = int(10 * math.sin((x * 0.9 + y * 0.3)))
            c = (214 + n + grain, 204 + n + grain, 176 + n + grain)
            if rnd.random() < 0.05:
                c = (140, 128, 104)
            base.putpixel((x, y), tuple(max(0, min(255, ch)) for ch in c) + (255,))
    # olhos
    for y in range(EYES[1], EYES[3]):
        for x in range(EYES[0], EYES[2]):
            base.putpixel((x, y), (20, 60, 70, 255))
            core = (y - EYES[1]) in (3, 4)
            glow.putpixel((x, y), (240, 255, 255, 255) if core else (110, 245, 255, 255))
    OUT.mkdir(parents=True, exist_ok=True)
    base.save(OUT / "hydra.png")
    glow.save(OUT / "hydra_eyes.png")
    lit = sum(1 for x in range(W) for y in range(H) if glow.getpixel((x, y))[3] > 0)
    print("ok", lit, "pixels de veia")


if __name__ == "__main__":
    main()
