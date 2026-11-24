"""Textura da Hidra arcana (256x128): escamas de obsidiana com veias de runas ciano, osso (dentes, chifres, garras,
espinhos) e olhos. Gera tambem a camada emissiva (hydra_eyes.png): os olhos e as veias brilham no escuro.

Regioes (ver HydraRenderer.HydraModel): tudo e escama de obsidiana, menos osso em u 224-255 / v 0-47 e olhos em
u 224-239 / v 96-103.

    python tools/gen_hydra_texture.py
"""
from pathlib import Path
import random
from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/arcane/textures/entity/hydra"
W, H = 256, 128
BONE = (224, 0, 256, 48)
EYES = (224, 96, 240, 104)


def inside(x, y, box):
    return box[0] <= x < box[2] and box[1] <= y < box[3]


def main():
    rnd = random.Random(1337)
    base = Image.new("RGBA", (W, H))
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    # escamas de obsidiana: losangos com borda escura e um brilho roxo no miolo
    for y in range(H):
        for x in range(W):
            row = y // 2
            u = (x + (row % 2) * 2) % 4
            v = y % 2
            c = (34, 24, 46)
            if u == 0 or (v == 1 and u == 3):
                c = (16, 10, 24)
            elif u == 2 and v == 0:
                c = (66, 46, 92)
            n = rnd.randint(-5, 5)
            base.putpixel((x, y), tuple(max(0, min(255, ch + n)) for ch in c) + (255,))
    # veias de runas: caminhos em angulo reto com pequenos "glifos" no meio, em ciano
    for _ in range(70):
        x, y = rnd.randrange(W), rnd.randrange(H)
        for step in range(rnd.randint(8, 22)):
            if inside(x, y, BONE) or inside(x, y, EYES):
                break
            bright = 0.7 + 0.3 * rnd.random()
            base.putpixel((x, y), (40, int(150 * bright), int(170 * bright), 255))
            glow.putpixel((x, y), (60, int(230 * bright), 255, 255))
            if step % 6 == 3:  # um pequeno glifo: cruz de 3 pixels
                for dx, dy in ((1, 0), (-1, 0), (0, 1)):
                    px, py = (x + dx) % W, (y + dy) % H
                    if not inside(px, py, BONE):
                        base.putpixel((px, py), (40, 150, 170, 255))
                        glow.putpixel((px, py), (60, 230, 255, 200))
            d = rnd.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
            x, y = (x + d[0]) % W, (y + d[1]) % H
    # osso: branco-amarelado com rachaduras
    for y in range(BONE[1], BONE[3]):
        for x in range(BONE[0], BONE[2]):
            n = rnd.randint(-12, 8)
            c = (222 + n, 214 + n, 188 + n)
            if rnd.random() < 0.06:
                c = (150, 140, 120)
            base.putpixel((x, y), c + (255,))
    # olhos: escuros no couro, ciano-branco brilhante na camada emissiva
    for y in range(EYES[1], EYES[3]):
        for x in range(EYES[0], EYES[2]):
            base.putpixel((x, y), (20, 60, 70, 255))
            core = (y - EYES[1]) in (3, 4)
            glow.putpixel((x, y), (230, 255, 255, 255) if core else (90, 240, 255, 255))
    OUT.mkdir(parents=True, exist_ok=True)
    base.save(OUT / "hydra.png")
    glow.save(OUT / "hydra_eyes.png")
    print("ok")


if __name__ == "__main__":
    main()
