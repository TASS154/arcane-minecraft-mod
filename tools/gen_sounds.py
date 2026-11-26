"""Sintetiza os efeitos sonoros do mod Arcane em OGG Vorbis (mono, 44.1 kHz).

Uso:  python tools/gen_sounds.py              (todos)
      python tools/gen_sounds.py --elements   (so os de elemento e reacao: assets/arcane/sounds/element)
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


def charge_start():
    """Runas se abrindo: arpejo curto subindo, com brilho."""
    tt = t(0.6)
    out = np.zeros_like(tt)
    for i, f in enumerate([392.0, 523.25, 659.25, 880.0]):
        local = tt - i * 0.07
        m = local >= 0
        out += m * np.sin(2 * np.pi * f * np.where(m, local, 0)) * np.exp(-np.where(m, local, 0) * 5) * 0.4
    air = lowpass(rng.normal(0, 1, len(tt)), 0.1) * np.linspace(0.1, 0.5, len(tt))
    return reverb((out + air * 0.4) * env(len(tt), 0.01, 0.3), decay=0.35)


def charge_release():
    """Descarga: estalo grave + varredura descendente e cauda brilhante."""
    tt = t(0.8)
    boom = np.sin(2 * np.pi * 70 * tt * np.exp(-tt * 3)) * np.exp(-tt * 7)
    sweep = np.sin(2 * np.pi * (1800 * tt - 1500 * tt ** 2)) * np.exp(-tt * 5) * 0.5
    crack = rng.normal(0, 1, len(tt)) * np.exp(-tt * 40) * 0.6
    sparkle = 0.25 * np.sin(2 * np.pi * 3100 * tt) * np.exp(-tt * 9) * np.sin(2 * np.pi * 21 * tt)
    return reverb(boom + sweep + crack + sparkle, decay=0.4)


def domain_open():
    """Dominio se fechando: batida grave, coro de harmonicos e vidro subindo."""
    tt = t(2.4)
    thump = np.sin(2 * np.pi * 45 * tt * np.exp(-tt * 1.2)) * np.exp(-tt * 3) * 1.2
    choir = sum(np.sin(2 * np.pi * f * tt + i) / (i + 1) for i, f in enumerate([110, 165, 220, 330, 440])) * 0.25
    choir = choir * env(len(tt), 0.8, 1.2)
    glass = np.sin(2 * np.pi * (600 * tt + 700 * tt ** 2)) * (tt / tt[-1]) * 0.25 * np.exp(-((tt - 2.0) ** 2) * 3)
    return reverb(thump + choir + glass, decay=0.55)


def domain_break():
    """Vidro enorme se estilhacando: estalos em cascata e graves."""
    tt = t(1.6)
    out = np.zeros_like(tt)
    for k in range(40):
        start = rng.random() ** 2 * 0.9
        f = rng.uniform(1800, 5200)
        local = tt - start
        m = local >= 0
        out += m * np.sin(2 * np.pi * f * np.where(m, local, 0)) * np.exp(-np.where(m, local, 0) * rng.uniform(25, 60)) * rng.uniform(0.1, 0.4)
    crack = rng.normal(0, 1, len(tt)) * np.exp(-tt * 18) * 0.7
    low = np.sin(2 * np.pi * 60 * tt) * np.exp(-tt * 6) * 0.9
    return reverb(out + crack + low, decay=0.4)


def flight_boost():
    """Arranque do voo rapido: vento filtrado subindo e estouro."""
    tt = t(0.9)
    wind = rng.normal(0, 1, len(tt))
    wind = wind - lowpass(wind, 0.04)
    wind = wind * np.sin(np.pi * np.clip(tt / 0.9, 0, 1)) ** 1.5
    pop = np.sin(2 * np.pi * 140 * tt * np.exp(-tt * 8)) * np.exp(-tt * 12)
    tone = 0.2 * np.sin(2 * np.pi * (500 * tt + 900 * tt ** 2)) * np.sin(np.pi * np.clip(tt / 0.9, 0, 1))
    return (wind * 0.7 + pop + tone) * env(len(tt), 0.005, 0.3)


def golem_slam():
    """Punho de pedra arcana: impacto pesado com ressonancia de cristal."""
    tt = t(1.0)
    thud = np.sin(2 * np.pi * 55 * tt * np.exp(-tt * 2)) * np.exp(-tt * 6)
    rock = lowpass(rng.normal(0, 1, len(tt)), 0.25) * np.exp(-tt * 14)
    ring = 0.3 * (np.sin(2 * np.pi * 740 * tt) + np.sin(2 * np.pi * 1110 * tt)) * np.exp(-tt * 5)
    return reverb(thud * 1.1 + rock * 0.7 + ring, decay=0.35)


def golem_hum():
    """Zumbido grave do golem arcano: bordao com batimento."""
    tt = t(1.5)
    base = np.sin(2 * np.pi * 82 * tt) + 0.8 * np.sin(2 * np.pi * 85 * tt) + 0.3 * np.sin(2 * np.pi * 246 * tt)
    pulse = 0.7 + 0.3 * np.sin(2 * np.pi * 3 * tt)
    return reverb(base * pulse * env(len(tt), 0.2, 0.6) * 0.5, decay=0.3)


SOUNDS = {
    "spell_cast": spell_cast,
    "spell_hit": spell_hit,
    "workbench_save": workbench_save,
    "glyph_learned": glyph_learned,
    "pen_stroke": pen_stroke,
    "ritual_activate": ritual_activate,
    "mana_poison": mana_poison,
    "grimoire_summon": grimoire_summon,
    "charge_start": charge_start,
    "charge_release": charge_release,
    "domain_open": domain_open,
    "domain_break": domain_break,
    "flight_boost": flight_boost,
    "golem_slam": golem_slam,
    "golem_hum": golem_hum,
}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, fn in SOUNDS.items():
        data = norm(fn().astype(np.float64)).astype(np.float32)
        path = OUT / f"{name}.ogg"
        sf.write(path, data, SR, format="OGG", subtype="VORBIS")
        print(f"   {path.name}  ({len(data) / SR:.2f}s)")


# ================================================================== sons por elemento (Fx), em 3 camadas
# Cada som = ATAQUE (o transiente: estalo, sopro, golpe) + CORPO (a textura do elemento) + CAUDA (como some: reverb,
# eco, chiado). Conjuracao e impacto usam a mesma receita com tempos e pesos diferentes. 3 variacoes por som (semente e
# afinacao levemente diferentes); o jogo ainda varia tom e volume pela potencia (com.arcane.fx.Fx.sound).

def _noise(n, r):
    return r.normal(0, 1, n)


def _bandnoise(n, r, lo_alpha, hi_alpha):
    """Ruido com banda aproximada: passa-baixa (lo_alpha) menos passa-baixa mais grave (hi_alpha)."""
    x = _noise(n, r)
    return lowpass(x, lo_alpha) - lowpass(x, hi_alpha)


def _sweep(tt, f0, f1):
    k = (f1 - f0) / max(tt[-1], 1e-6)
    return np.sin(2 * np.pi * (f0 * tt + 0.5 * k * tt ** 2))


def _attack(kind, n, r, tt):
    if kind == "crack":
        return _noise(n, r) * np.exp(-tt * 70)
    if kind == "thump":
        return np.sin(2 * np.pi * 70 * tt * np.exp(-tt * 8)) * np.exp(-tt * 18)
    if kind == "puff":
        return lowpass(_noise(n, r), 0.08) * np.exp(-tt * 25) * 2.0
    if kind == "chime":
        return np.sin(2 * np.pi * 1320 * tt) * np.exp(-tt * 30)
    if kind == "zap":
        return np.sign(np.sin(2 * np.pi * 900 * tt)) * np.exp(-tt * 60) * 0.6
    if kind == "drop":
        return np.sin(2 * np.pi * (900 - 2600 * np.minimum(tt, 0.08)) * tt) * np.exp(-tt * 35)
    return np.zeros(n)


def _body(kind, n, r, tt, base):
    if kind == "roar":       # fogo: rugido de ruido grave com crepitar
        crackle = (r.random(n) > 0.995) * r.normal(0, 0.8, n)
        return lowpass(_noise(n, r), 0.05) * 1.6 + lowpass(crackle, 0.6)
    if kind == "crystal":    # gelo: parciais inarmonicos agudos
        return sum(np.sin(2 * np.pi * base * m * tt) / (i + 1) for i, m in enumerate((1.0, 2.76, 5.40, 8.93)))
    if kind == "buzz":       # tempestade: zumbido eletrico com estalos
        return np.sign(np.sin(2 * np.pi * base * tt)) * 0.3 * (0.6 + 0.4 * np.sin(2 * np.pi * 31 * tt)) + (r.random(n) > 0.99) * r.normal(0, 0.4, n)
    if kind == "bubble":     # veneno: borbulhas
        out = np.zeros(n)
        for _ in range(14):
            s = r.integers(0, max(1, n - 2000))
            ln = 1800
            f = r.uniform(250, 700)
            seg = np.sin(2 * np.pi * np.linspace(f, f * 1.8, ln) * np.arange(ln) / SR) * np.exp(-np.arange(ln) / SR * 40)
            out[s:s + ln] += seg[: n - s]
        return out
    if kind == "whoosh":     # forca: sopro com varredura
        return _bandnoise(n, r, 0.35, 0.04) * (0.4 + 0.6 * np.sin(np.pi * tt / tt[-1]))
    if kind == "hollow":     # vazio: grave oco que desce
        return _sweep(tt, base, base * 0.5) * 0.8 + lowpass(_noise(n, r), 0.02) * 0.6
    if kind == "bell":       # vida/luz/tempo: sino suave
        return sum(np.sin(2 * np.pi * base * m * tt) * np.exp(-tt * (2 + i)) / (i + 1) for i, m in enumerate((1.0, 2.0, 3.01, 4.2)))
    if kind == "rumble":     # terra: rumor grave e pedrisco
        grit = (r.random(n) > 0.985) * r.normal(0, 1.2, n)
        return lowpass(_noise(n, r), 0.012) * 3.0 + lowpass(grit, 0.08) * 0.5
    if kind == "splash":     # agua: chuá de ruido com gotas
        drops = sum(np.sin(2 * np.pi * r.uniform(600, 1400) * tt) * np.exp(-np.maximum(tt - r.uniform(0.02, 0.3), 0) * 40) * (tt > 0.02) for _ in range(5)) * 0.2
        return _bandnoise(n, r, 0.4, 0.06) * np.exp(-tt * 6) + drops
    if kind == "shimmer":    # luz: brilho agudo com batimento
        return (np.sin(2 * np.pi * base * tt) + np.sin(2 * np.pi * base * 1.007 * tt)) * 0.5 * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * tt))
    if kind == "whisper":    # sombra: sussurro filtrado
        return _bandnoise(n, r, 0.25, 0.12) * (0.5 + 0.5 * np.sin(2 * np.pi * 3 * tt)) * 1.4
    if kind == "pulse":      # sangue: batida dupla abafada
        beat = np.zeros(n)
        for t0 in (0.0, 0.18):
            k = tt - t0
            beat += np.sin(2 * np.pi * 55 * np.maximum(k, 0)) * np.exp(-np.maximum(k, 0) * 14) * (k >= 0)
        return beat * 1.5 + lowpass(_noise(n, r), 0.03) * 0.3
    if kind == "tick":       # tempo: tique-taque e sino
        ticks = sum(np.exp(-np.maximum(tt - t0, 0) * 300) * (tt >= t0) * np.sin(2 * np.pi * 2400 * tt) for t0 in np.arange(0, tt[-1], 0.12))
        return ticks * 0.7 + np.sin(2 * np.pi * base * tt) * np.exp(-tt * 3) * 0.5
    if kind == "rustle":     # natureza: farfalhar de folhas
        return _bandnoise(n, r, 0.5, 0.15) * (0.3 + 0.7 * np.abs(np.sin(2 * np.pi * 7 * tt))) * 1.2
    if kind == "tone":       # som: acorde puro
        return sum(np.sin(2 * np.pi * base * m * tt) / (i + 1) for i, m in enumerate((1.0, 1.25, 1.5, 2.0)))
    return _noise(n, r) * 0.3   # arcano: sopro neutro


def _tail(kind, x):
    if kind == "reverb":
        return reverb(x, decay=0.42)
    if kind == "long":
        return reverb(x, delays=(0.05, 0.083, 0.121, 0.17), decay=0.55)
    if kind == "dry":
        return np.concatenate([x, np.zeros(int(SR * 0.05))])
    if kind == "echo":
        out = np.concatenate([x, np.zeros(int(SR * 0.5))])
        k = int(SR * 0.16)
        out[k:k + len(x)] += x * 0.35
        out[2 * k:2 * k + len(x)] += x * 0.12
        return out
    return x


# elemento: (ataque, corpo, cauda, frequencia base, peso do ataque no impacto)
ELEMENT_RECIPES = {
    "arcane": ("chime", "whoosh", "reverb", 660, 0.6),
    "fire":   ("puff", "roar", "reverb", 120, 0.9),
    "frost":  ("crack", "crystal", "long", 1800, 0.8),
    "storm":  ("zap", "buzz", "echo", 110, 1.0),
    "venom":  ("drop", "bubble", "dry", 400, 0.5),
    "force":  ("thump", "whoosh", "reverb", 80, 1.0),
    "void":   ("thump", "hollow", "long", 160, 0.7),
    "life":   ("chime", "bell", "long", 520, 0.4),
    "earth":  ("thump", "rumble", "dry", 60, 1.0),
    "water":  ("drop", "splash", "reverb", 800, 0.7),
    "light":  ("chime", "shimmer", "long", 1560, 0.5),
    "shadow": ("puff", "whisper", "long", 200, 0.4),
    "blood":  ("thump", "pulse", "dry", 55, 0.8),
    "time":   ("chime", "tick", "echo", 880, 0.5),
    "nature": ("puff", "rustle", "reverb", 300, 0.4),
    "sound":  ("chime", "tone", "echo", 440, 0.6),
}


def element_sound(element, role, variant):
    atk, body, tail, base, atk_w = ELEMENT_RECIPES[element]
    import zlib
    r = np.random.default_rng(zlib.crc32(f"{element}/{role}".encode()) + variant * 7919)  # determinista
    detune = (0.94, 1.0, 1.06)[variant]
    dur = 0.55 if role == "cast" else 0.4
    tt = t(dur)
    n = len(tt)
    a = _attack(atk, n, r, tt)
    a = a / (np.max(np.abs(a)) + 1e-9) * (atk_w if role == "impact" else 0.35)  # camadas normalizadas: o corpo nao some
    b = _body(body, n, r, tt, base * detune)
    b = b / (np.sqrt(np.mean(b ** 2)) + 1e-9) * 0.32
    shape = env(n, attack=0.05 if role == "cast" else 0.004, release=dur * 0.6)
    if role == "cast":  # a conjuracao cresce; o impacto comeca no maximo
        shape *= np.minimum(1.0, tt / (dur * 0.4) + 0.2)
    mix = a + b * shape * 0.7
    mix = np.tanh(mix * 1.8) / 1.8  # saturacao leve: estalos isolados nao dominam o volume
    return _tail(tail, mix)


def reaction_sound(kind, variant):
    r = np.random.default_rng(4242 + variant * 17 + {"steam": 0, "shatter": 1000, "vapor": 2000}[kind])
    tt = t({"steam": 0.6, "shatter": 0.35, "vapor": 1.6}[kind])
    n = len(tt)
    if kind == "vapor":  # Nuvem de Vapor (Fase K): chiado longo e largo, sobe devagar, ondula e some (NAO ouvido)
        x = _bandnoise(n, r, 0.55, 0.12) * 0.8 + _bandnoise(n, r, 0.85, 0.5) * 0.35
        swell = np.sin(np.pi * np.clip(tt / tt[-1], 0, 1)) ** 0.7
        wobble = 0.8 + 0.2 * np.sin(2 * np.pi * (1.3 + variant * 0.4) * tt)
        return reverb(x * swell * wobble * env(n, 0.25, 0.6), decay=0.45)
    if kind == "steam":  # chiado: ruido agudo que sobe e some
        x = _bandnoise(n, r, 0.7, 0.3) * env(n, 0.02, 0.4) * (0.6 + 0.4 * tt / tt[-1])
        return reverb(x * 1.4, decay=0.3)
    # estilhaco: varios estalos agudos de vidro
    out = np.zeros(n)
    for _ in range(9):
        s = r.integers(0, n // 2)
        f = r.uniform(2500, 6000)
        ln = n - s
        k = np.arange(ln) / SR
        out[s:] += np.sin(2 * np.pi * f * k) * np.exp(-k * r.uniform(30, 70)) * r.uniform(0.4, 1.0)
    out += _noise(n, r) * np.exp(-tt * 50) * 0.8
    return reverb(out, decay=0.35)


def element_sounds_main():
    import json
    sounds_json = OUT.parent / "sounds.json"
    data = json.loads(sounds_json.read_text(encoding="utf-8"))
    edir = OUT / "element"
    edir.mkdir(parents=True, exist_ok=True)
    for element in ELEMENT_RECIPES:
        for role in ("cast", "impact"):
            names = []
            for v in range(3):
                x = norm(element_sound(element, role, v).astype(np.float64), peak=0.75).astype(np.float32)
                name = f"{element}_{role}_{v + 1}"
                sf.write(edir / f"{name}.ogg", x, SR, format="OGG", subtype="VORBIS")
                names.append(f"arcane:element/{name}")
            data[f"element.{element}.{role}"] = {"subtitle": f"subtitles.arcane.element.{role}", "sounds": names}
    for kind in ONLY_REACTIONS or ("steam", "shatter", "vapor"):
        names = []
        for v in range(3):
            x = norm(reaction_sound(kind, v).astype(np.float64), peak=0.7).astype(np.float32)
            name = f"reaction_{kind}_{v + 1}"
            sf.write(edir / f"{name}.ogg", x, SR, format="OGG", subtype="VORBIS")
            names.append(f"arcane:element/{name}")
        data[f"reaction.{kind}"] = {"subtitle": f"subtitles.arcane.reaction.{kind}", "sounds": names}
    sounds_json.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"   {len(ELEMENT_RECIPES) * 6 + 6} sons de elemento em {edir}")


ONLY_REACTIONS = ()


def reactions_only_main(kinds):
    """So os sons de reacao pedidos (ex.: --reaction vapor), sem regravar os outros arquivos."""
    import json
    sounds_json = OUT.parent / "sounds.json"
    data = json.loads(sounds_json.read_text(encoding="utf-8"))
    edir = OUT / "element"
    edir.mkdir(parents=True, exist_ok=True)
    for kind in kinds:
        names = []
        for v in range(3):
            x = norm(reaction_sound(kind, v).astype(np.float64), peak=0.7).astype(np.float32)
            name = f"reaction_{kind}_{v + 1}"
            sf.write(edir / f"{name}.ogg", x, SR, format="OGG", subtype="VORBIS")
            names.append(f"arcane:element/{name}")
            print(f"   {name}.ogg  ({len(x) / SR:.2f}s)")
        data[f"reaction.{kind}"] = {"subtitle": f"subtitles.arcane.reaction.{kind}", "sounds": names}
    sounds_json.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


# ================================================================== voz da magia (Fase L)
# Uma amostra sustentada por familia, em Do4 (~261,6 Hz): o jogo muda o tom de 0,5 a 2,0 para tocar cada nota.
# "_long" e a mesma com cauda longa (rituais e magias grandes). NAO ouvido em jogo.
VOICE_BASE_HZ = 261.63
VOICE_RECIPES = {
    # (harmonicos (multiplo, peso), vibrato Hz, ruido de sopro, brilho do ataque)
    "arcane": ([(1, 1.0), (2, 0.35), (3, 0.18), (5, 0.08)], 5.0, 0.02, 0.6),   # sino suave
    "fire": ([(1, 1.0), (2, 0.6), (3, 0.45), (4, 0.3), (5, 0.2)], 6.0, 0.05, 0.4),  # quente, quase serra
    "frost": ([(1, 1.0), (2.76, 0.4), (5.4, 0.2)], 3.0, 0.01, 0.9),            # vidro (parciais inarmonicos)
    "storm": ([(1, 1.0), (3, 0.5), (5, 0.35), (7, 0.2)], 9.0, 0.04, 0.7),      # zumbido de onda quadrada
    "life": ([(1, 1.0), (2, 0.25)], 4.5, 0.06, 0.2),                           # flauta
    "earth": ([(0.5, 0.5), (1, 1.0), (2, 0.4)], 2.0, 0.03, 0.3),               # grave e encorpado
    "void": ([(1, 1.0), (1.5, 0.3), (2.01, 0.25)], 1.5, 0.08, 0.1),            # escuro, batimento lento
}


def voice_sample(family, long=False):
    harm, vib, breath, bright = VOICE_RECIPES[family]
    dur = 3.2 if long else 1.3
    tt = t(dur)
    n = len(tt)
    r = np.random.default_rng(777 + list(VOICE_RECIPES).index(family))
    phase = 2 * np.pi * VOICE_BASE_HZ * (tt + 0.0025 * np.sin(2 * np.pi * vib * tt) / (2 * np.pi * vib))
    x = np.zeros(n)
    for mult, w in harm:
        x += w * np.sin(phase * mult) * np.exp(-tt * (0.6 + mult * 0.4))
    x += lowpass(r.normal(0, 1, n), 0.2) * breath
    x += np.sin(phase * 4) * np.exp(-tt * 30) * bright * 0.3
    e = env(n, 0.02, 1.8 if long else 0.6)
    out = x * e
    return reverb(out, decay=0.55 if long else 0.35)


def voice_main():
    import json
    sounds_json = OUT.parent / "sounds.json"
    data = json.loads(sounds_json.read_text(encoding="utf-8"))
    vdir = OUT / "voice"
    vdir.mkdir(parents=True, exist_ok=True)
    for family in VOICE_RECIPES:
        for long in (False, True):
            x = norm(voice_sample(family, long).astype(np.float64), peak=0.7).astype(np.float32)
            name = f"{family}_long" if long else family
            sf.write(vdir / f"{name}.ogg", x, SR, format="OGG", subtype="VORBIS")
            data[f"voice.{family}" + (".long" if long else "")] = {"subtitle": "subtitles.arcane.voice", "sounds": [f"arcane:voice/{name}"]}
            print(f"   voice/{name}.ogg  ({len(x) / SR:.2f}s)")
    sounds_json.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    import sys as _sys
    if "--voice" in _sys.argv:  # so as amostras da voz da magia
        voice_main()
    elif "--reaction" in _sys.argv:  # so um som de reacao (ex.: --reaction vapor)
        reactions_only_main(_sys.argv[_sys.argv.index("--reaction") + 1:])
    elif "--elements" in _sys.argv:  # so os sons de elemento e das reacoes
        element_sounds_main()
    else:
        main()
        element_sounds_main()
