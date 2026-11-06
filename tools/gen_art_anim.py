"""Animacoes em pixel art, geradas por codigo (sem dependencia de .mcmeta, o mod escolhe o quadro por tempo).

- Grimorio: 8 quadros por estagio, um brilho diagonal varre a capa e as runas pulsam.
- Mago: brilho de olhos e maos por elemento (6 quadros), usado como camada luminosa.
- Projetil: orbe com 4 quadros e variante de tempestade (arco de raio em zigue-zague).
- Particulas grandes: raio que cai (bolt_strike) e onda de choque (shock), 6 quadros cada.
"""
from __future__ import annotations

import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/arcane"
TEX = ASSETS / "textures"

# mesma ordem de MageElement: fire, frost, storm, venom, blast, gale
MAGE_COLORS = [(255, 140, 40), (150, 215, 255), (255, 235, 90), (120, 235, 70), (255, 80, 60), (190, 235, 220)]


def save(img, rel):
    p = TEX / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


def write_json(rel, obj):
    p = ASSETS / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def clamp(v):
    return max(0, min(255, int(v)))


# ---------------------------------------------------------------- grimorio
def grimoire_frames():
    for stage in (1, 2, 3, 4):
        base = Image.open(TEX / f"entity/grimoire_s{stage}.png").convert("RGBA")
        w, h = base.size
        for k in range(8):
            img = base.copy()
            px = img.load()
            sweep = (k / 8.0) * (w + h + 12) - 6
            for y in range(h):
                for x in range(w):
                    r, g, b, a = px[x, y]
                    if a == 0:
                        continue
                    lum = (r + g + b) / 3
                    d = abs((x + y) - sweep)
                    boost = 0.0
                    if d < 3:
                        boost = (3 - d) / 3 * 0.55
                    # runas e detalhes claros pulsam
                    if lum > 120:
                        boost += 0.25 * (0.5 + 0.5 * math.sin(k / 8.0 * 2 * math.pi + x * 0.4))
                    px[x, y] = (clamp(r + (255 - r) * boost), clamp(g + (255 - g) * boost * 0.9),
                                clamp(b + (255 - b) * boost), a)
            save(img, f"entity/grimoire_s{stage}_f{k}.png")


# ---------------------------------------------------------------- mago
def mage_glow():
    for e, col in enumerate(MAGE_COLORS):
        for k in range(6):
            img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
            px = img.load()
            pulse = 0.55 + 0.45 * math.sin(k / 6.0 * 2 * math.pi)
            c = tuple(clamp(v * (0.7 + 0.3 * pulse)) for v in col)
            core = tuple(clamp(v + (255 - v) * 0.6 * pulse) for v in col)
            # olhos (frente da cabeca: u 8..15, v 8..15)
            for x in (9, 10, 13, 14):
                px[x, 12] = core + (255,)
            for x in (8, 11, 12, 15):
                px[x, 12] = c + (int(110 * pulse),)
            # maos: ponta dos bracos (frente do braco direito u 44..47 e esquerdo u 36..39, v 28..31)
            for (x0, x1) in ((44, 47), (36, 39)):
                for x in range(x0, x1 + 1):
                    px[x, 31] = core + (255,)
                    px[x, 30] = c + (int(180 * pulse),)
                    px[x, 29] = c + (int(70 * pulse),)
            # runa que pulsa no peito (frente do torso u 20..27, v 20..31)
            for (x, y) in ((23, 23), (24, 23), (23, 24), (24, 24)):
                px[x, y] = core + (int(150 + 100 * pulse),)
            for (x, y) in ((22, 23), (25, 23), (23, 22), (24, 22), (22, 24), (25, 24), (23, 25), (24, 25)):
                px[x, y] = c + (int(90 * pulse),)
            save(img, f"entity/npc_mage/glow_{e}_{k}.png")


# ---------------------------------------------------------------- projetil
def orb(k, storm):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cx = cy = 7.5
    rnd = random.Random(100 + k)
    if not storm:
        r = 4.2 + 0.7 * math.sin(k / 4.0 * 2 * math.pi)
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, y - cy)
                if d < r * 0.5:
                    px[x, y] = (255, 255, 255, 255)
                elif d < r:
                    px[x, y] = (230, 245, 255, 230)
                elif d < r + 1.6:
                    px[x, y] = (200, 225, 255, 110)
        ang = k / 4.0 * math.pi / 2
        for i in range(4):
            a = ang + i * math.pi / 2
            for t in (5.5, 6.5, 7.2):
                x, y = int(round(cx + math.cos(a) * t)), int(round(cy + math.sin(a) * t))
                if 0 <= x < 16 and 0 <= y < 16:
                    px[x, y] = (255, 255, 255, 220)
    else:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, y - cy)
                if d < 2.6:
                    px[x, y] = (255, 255, 255, 255)
                elif d < 4.4:
                    px[x, y] = (230, 240, 255, 150)
        # arcos em zigue-zague para fora
        for i in range(3):
            a = rnd.uniform(0, 2 * math.pi)
            x, y = cx, cy
            for step in range(6):
                a += rnd.uniform(-0.9, 0.9)
                x += math.cos(a) * 1.4
                y += math.sin(a) * 1.4
                xi, yi = int(round(x)), int(round(y))
                if 0 <= xi < 16 and 0 <= yi < 16:
                    px[xi, yi] = (255, 255, 255, 255 - step * 25)
    return img


def projectiles():
    for k in range(4):
        save(orb(k, False), f"entity/arcane_projectile_f{k}.png")
        save(orb(k, True), f"entity/arcane_projectile_storm_f{k}.png")


# ---------------------------------------------------------------- particulas grandes
def bolt_strike(k, n=6):
    """Raio vertical 32x32: o arco acende de cima para baixo, brilha e se apaga."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(7)  # mesmo traco em todos os quadros: so a luz muda
    path = []
    x = 16.0
    for y in range(32):
        x += rnd.uniform(-1.6, 1.6)
        x = max(8.0, min(23.0, x))
        path.append((int(round(x)), y))
    reach = min(32, int(32 * (k + 1) / 3)) if k < 3 else 32
    fade = 1.0 if k < 4 else (0.6 if k == 4 else 0.3)
    for (x, y) in path[:reach]:
        for dx, a in ((-2, 50), (-1, 130), (0, 255), (1, 130), (2, 50)):
            xx = x + dx
            if 0 <= xx < 32:
                col = (255, 255, 255) if dx == 0 else (190, 215, 255) if abs(dx) == 1 else (120, 150, 255)
                px[xx, y] = col + (int(a * fade),)
    # ramificacoes
    if k >= 1:
        for (bx, by, d) in ((path[10][0], 10, 1), (path[20][0], 20, -1)):
            xx, yy = bx, by
            for s in range(7):
                xx += d
                yy += rnd.choice((0, 1))
                if 0 <= xx < 32 and 0 <= yy < 32:
                    px[xx, yy] = (200, 225, 255, int(220 * fade))
    # clarao no impacto
    if k in (2, 3, 4):
        r = 4 + k
        for y in range(32):
            for x in range(32):
                d = math.hypot(x - path[-1][0], y - 29)
                if d < r and y > 22:
                    a = int(180 * (1 - d / r) * fade)
                    if a > px[x, y][3]:
                        px[x, y] = (255, 255, 255, a)
    return img


def shock(k, n=6):
    """Anel de choque que se expande."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    r = 3 + k * 2.6
    fade = 1.0 - k / n * 0.8
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - 15.5, y - 15.5)
            if abs(d - r) < 1.0:
                px[x, y] = (255, 255, 255, int(255 * fade))
            elif abs(d - r) < 2.0:
                px[x, y] = (200, 215, 255, int(120 * fade))
            elif abs(d - (r - 3)) < 0.8 and k > 1:
                px[x, y] = (170, 190, 255, int(90 * fade))
    return img


def big_particles():
    for k in range(6):
        save(bolt_strike(k), f"particle/bolt_strike_{k}.png")
        save(shock(k), f"particle/shock_{k}.png")
    write_json("particles/arcane_bolt_strike.json", {"textures": [f"arcane:bolt_strike_{k}" for k in range(6)]})
    write_json("particles/arcane_shock.json", {"textures": [f"arcane:shock_{k}" for k in range(6)]})


def generate():
    grimoire_frames()
    mage_glow()
    projectiles()
    big_particles()


if __name__ == "__main__":
    generate()
