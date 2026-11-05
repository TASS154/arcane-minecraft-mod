"""Sintetiza os efeitos sonoros do mod Arcane em OGG Vorbis (mono, 44.1 kHz).

Uso:  python tools/gen_sounds.py
Requer: numpy, soundfile (pip install numpy soundfile)

Sao sons sinteticos provisorios, feitos para dar feedback audivel a cada acao.
Um sound designer pode trocar qualquer .ogg mantendo o mesmo nome.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
import soundfile as sf

SR = 44100
OUT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "arcane" / "sounds"
rng = np.random.default_rng(1234)


def t(sec):
    return np.linspace(0, sec, int(SR * sec), endpoint=False)


def env(n, attack=0.01, release=0.3):
    e = np.ones(n)
    a = max(1, int(SR * attack))
    r = max(1, int(SR * release))
    e[:a] = np.linspace(0, 1, a)
    e[-r:] *= np.linspace(1, 0, r) ** 2
    return e


def lowpass(x, alpha):
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += alpha * (v - acc)
        y[i] = acc
    return y


def reverb(x, delays=(0.031, 0.047, 0.071, 0.113), decay=0.45):
    out = np.concatenate([x, np.zeros(int(SR * 0.6))])
    for d in delays:
        k = int(SR * d)
        g = decay
        for rep in range(1, 6):
            s = k * rep
            if s >= len(out):
                break
            out[s:s + len(x)] += x[: len(out) - s] * g
            g *= decay
    return out


def norm(x, peak=0.8):
    m = np.max(np.abs(x))
    return x if m == 0 else x / m * peak


def spell_cast():
    tt = t(0.7)
    sweep = np.sin(2 * np.pi * (300 * tt + 900 * tt ** 2))
    shimmer = 0.4 * np.sin(2 * np.pi * 1800 * tt) * np.sin(2 * np.pi * 7 * tt)
    whisper = lowpass(rng.normal(0, 1, len(tt)), 0.15) * 0.6
    return reverb((sweep * 0.6 + shimmer + whisper) * env(len(tt), 0.02, 0.45))


def spell_hit():
    tt = t(0.35)
    thump = np.sin(2 * np.pi * 110 * tt * np.exp(-tt * 6)) * np.exp(-tt * 10)
    crack = rng.normal(0, 1, len(tt)) * np.exp(-tt * 30)
    sparkle = 0.3 * np.sin(2 * np.pi * 2400 * tt) * np.exp(-tt * 12)
    return reverb(thump + crack * 0.5 + sparkle, decay=0.3)


def workbench_save():
    tt = t(0.4)
    stamp = lowpass(rng.normal(0, 1, len(tt)), 0.3) * np.exp(-tt * 25)
    tone = 0.5 * (np.sin(2 * np.pi * 523 * tt) + np.sin(2 * np.pi * 784 * tt)) * env(len(tt), 0.05, 0.3) * (tt > 0.05)
    return reverb(stamp + tone * 0.6, decay=0.25)


def glyph_learned():
    tt = t(1.4)
    out = np.zeros_like(tt)
    for i, f in enumerate([523.25, 659.25, 783.99, 1046.5]):
        start = i * 0.12
        mask = tt >= start
        local = tt - start
        out += mask * np.sin(2 * np.pi * f * local) * np.exp(-local * 2.2) * 0.5
        out += mask * np.sin(2 * np.pi * f * 2.01 * local) * np.exp(-local * 3) * 0.15
    return reverb(out * env(len(tt), 0.005, 0.5))


def pen_stroke():
    tt = t(0.3)
    scratch = rng.normal(0, 1, len(tt))
    scratch = scratch - lowpass(scratch, 0.2)  # passa-altas
    mod = 0.6 + 0.4 * np.sin(2 * np.pi * 18 * tt)
    return scratch * mod * env(len(tt), 0.02, 0.12) * 0.7


def ritual_activate():
    tt = t(2.2)
    drone = sum(np.sin(2 * np.pi * f * tt) / (i + 1) for i, f in enumerate([55, 82.5, 110, 165]))
    rise = 0.3 * np.sin(2 * np.pi * (220 * tt + 120 * tt ** 2)) * (tt / tt[-1])
    rumble = lowpass(rng.normal(0, 1, len(tt)), 0.02) * 3
    return reverb((drone + rise + rumble) * env(len(tt), 0.4, 0.9), decay=0.55)


def mana_poison():
    tt = t(1.0)
    buzz = np.sign(np.sin(2 * np.pi * 61 * tt)) * 0.3 + np.sin(2 * np.pi * 67 * tt) * 0.4
    warble = np.sin(2 * np.pi * (900 + 200 * np.sin(2 * np.pi * 5 * tt)) * tt) * 0.15
    return lowpass(buzz + warble, 0.25) * env(len(tt), 0.1, 0.4)


def grimoire_summon():
    tt = t(0.9)
    flutter = lowpass(rng.normal(0, 1, len(tt)), 0.35) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 14 * tt)))
    chime = 0.5 * np.sin(2 * np.pi * 880 * tt) * np.exp(-tt * 4)
    return reverb((flutter * 0.5 + chime) * env(len(tt), 0.02, 0.4))


SOUNDS = {
    "spell_cast": spell_cast,
    "spell_hit": spell_hit,
    "workbench_save": workbench_save,
    "glyph_learned": glyph_learned,
    "pen_stroke": pen_stroke,
    "ritual_activate": ritual_activate,
    "mana_poison": mana_poison,
    "grimoire_summon": grimoire_summon,
}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, fn in SOUNDS.items():
        data = norm(fn().astype(np.float64)).astype(np.float32)
        path = OUT / f"{name}.ogg"
        sf.write(path, data, SR, format="OGG", subtype="VORBIS")
        print(f"   {path.name}  ({len(data) / SR:.2f}s)")


if __name__ == "__main__":
    main()
