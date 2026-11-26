"""Previa da voz da magia (Fase L): grava em tools/out/ um WAV por inscricao de exemplo, para ouvir sem abrir o jogo.

Uso:  python tools/preview_voice.py                       (exemplos)
      python tools/preview_voice.py projectile damage fire  (uma inscricao sua; elemento: --element fire)
Requer: numpy, soundfile. A regra e a mesma de com.arcane.fx.SpellVoice (FNV-1a do id, pentatonica, oitava pela
categoria, 8 notas no maximo); as amostras saem da mesma receita de tools/gen_sounds.py (voice_sample).
NAO ouvido em jogo: o jogo toca a amostra .ogg com o tom do Minecraft (que muda a velocidade, como aqui).
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import soundfile as sf

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_sounds as gs  # noqa: E402
from glyph_table import G  # noqa: E402

OUT = Path(__file__).resolve().parent / "out"
PENTA = [0, 2, 4, 7, 9]
STEP_TICKS = 3
MAX_NOTES = 8
FAMILY = {"fire": "fire", "blood": "fire", "frost": "frost", "water": "frost", "storm": "storm", "sound": "storm",
          "venom": "life", "life": "life", "nature": "life", "force": "earth", "earth": "earth", "void": "void",
          "shadow": "void"}


def fnv1a(s: str) -> int:
    h = 0x811C9DC5
    for b in s.encode("utf-8"):
        h ^= b
        h = (h * 0x01000193) & 0xFFFFFFFF
    return h - (1 << 32) if h >= (1 << 31) else h  # como int do Java


def kind(gid):
    cat = G.get(gid, {}).get("cat", "effect")
    return {"form": 0, "modifier": 2}.get(cat, 1)


def compose(ids, big=False, power=1.0):
    idx = list(range(len(ids)))
    if len(ids) > MAX_NOTES:
        middle = sorted(idx[1:-1], key=lambda i: (-G.get(ids[i], {}).get("cost", 1), i))[:MAX_NOTES - 2]
        idx = sorted([0, len(ids) - 1] + middle)
    base = max(0.35, min(1.0, 0.55 + 0.15 * power))
    notes = []
    for step, i in enumerate(idx):
        k = kind(ids[i])
        vol = base * (1.0 if k == 1 else 0.9 if k == 0 else 0.7)
        notes.append([fnv1a(ids[i]) % 5, k, step * STEP_TICKS, min(1.0, vol), False])
    if big and notes:
        last, first = notes[-1], notes[0]
        notes.append([min(4, (last[0] + 3) % 5), last[1], last[2] + STEP_TICKS, last[3], False])
        notes.append([first[0], min(2, first[1] + 1), last[2] + STEP_TICKS, first[3], True])
    return notes


def pitch(degree, octave):
    s = PENTA[degree] + 12 * (octave - 1)
    while s > 12:
        s -= 12
    while s < -12:
        s += 12
    return max(0.5, min(2.0, 2 ** (s / 12)))


def resample(x, rate):
    """Muda o tom como o Minecraft: toca mais rapido (agudo) ou mais devagar (grave)."""
    n = int(len(x) / rate)
    return np.interp(np.arange(n) * rate, np.arange(len(x)), x)


def render(ids, element="arcane", big=False):
    family = FAMILY.get(element, "arcane")
    short = gs.voice_sample(family, False)
    long = gs.voice_sample(family, True)
    notes = compose(ids, big)
    total = int(gs.SR * (notes[-1][2] / 20 + 3.5)) if notes else gs.SR
    mix = np.zeros(total)
    for degree, octave, delay, vol, tail in notes:
        y = resample(long if tail else short, pitch(degree, octave)) * vol
        at = int(gs.SR * delay / 20)
        end = min(total, at + len(y))
        mix[at:end] += y[:end - at]
    return gs.norm(mix, 0.8).astype(np.float32), notes


EXAMPLES = {
    "faisca": (["projectile", "damage"], "arcane", False),
    "bola_de_fogo": (["projectile", "damage", "fire", "power"], "fire", False),
    "bola_de_fogo_invertida": (["projectile", "fire", "damage", "power"], "fire", False),
    "gelo_em_area": (["aura", "freeze", "slow", "area"], "frost", False),
    "tempestade_ritual": (["aura", "thunder", "wet", "power", "area", "lengthen"], "storm", True),
    "vazio_longo": (["ray", "hostile", "drain_mana", "blind", "weaken", "curse", "power", "after_seconds", "silence", "swift"], "void", False),
}


def main(argv):
    OUT.mkdir(parents=True, exist_ok=True)
    if argv:
        element = "arcane"
        if "--element" in argv:
            i = argv.index("--element")
            element = argv[i + 1]
            argv = argv[:i] + argv[i + 2:]
        jobs = {"sua_magia": (argv, element, "--big" in argv)}
        jobs["sua_magia"] = ([a for a in argv if a != "--big"], element, "--big" in argv)
    else:
        jobs = EXAMPLES
    for name, (ids, element, big) in jobs.items():
        audio, notes = render(ids, element, big)
        path = OUT / f"voice_{name}.wav"
        sf.write(path, audio, gs.SR)
        print(f"   {path.name}: {' '.join(ids)}  ->  " + ", ".join(f"g{d}o{o}+{t}t" for d, o, t, _, _ in notes))


if __name__ == "__main__":
    main(sys.argv[1:])
