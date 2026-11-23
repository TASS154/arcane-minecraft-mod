"""Textura do selo da Hidra: o cristal de mana recolorido em carmesim e preto, com runas douradas pulsando.

Uso: python tools/gen_seal_texture.py  (le mana_crystal*.png e escreve hydra_seal*.png na mesma pasta)
"""
from pathlib import Path
from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/arcane/textures/block"


def ramp(l):
    """Luminancia (0..1) -> cor: preto-violeta nas sombras, carmesim no meio, ouro claro no brilho."""
    stops = [(0.0, (14, 4, 10)), (0.35, (88, 8, 22)), (0.7, (200, 30, 40)), (0.92, (255, 170, 70)), (1.0, (255, 240, 190))]
    for (a, ca), (b, cb) in zip(stops, stops[1:]):
        if l <= b:
            t = (l - a) / (b - a)
            return tuple(int(ca[i] + (cb[i] - ca[i]) * t) for i in range(3))
    return stops[-1][1]


def recolor(src, dst, rune_rows=False):
    im = Image.open(TEX / src).convert("RGBA")
    out = Image.new("RGBA", im.size)
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = im.getpixel((x, y))
            l = (0.3 * r + 0.59 * g + 0.11 * b) / 255.0
            c = ramp(min(1.0, l * 1.05))
            # runas: tracos dourados em cada quadro da animacao (16x16 por quadro), deslocados de quadro para quadro
            if rune_rows and a > 0:
                frame, fy = divmod(y, 16)
                if (x + fy * 3 + frame * 5) % 9 == 0 and 3 <= fy <= 13:
                    c = (255, 200, 90)
            out.putpixel((x, y), (*c, a))
    out.save(TEX / dst)


if __name__ == "__main__":
    recolor("mana_crystal.png", "hydra_seal.png", rune_rows=True)
    recolor("mana_crystal_base.png", "hydra_seal_base.png")
    (TEX / "hydra_seal.png.mcmeta").write_text((TEX / "mana_crystal.png.mcmeta").read_text())
    print("ok")
