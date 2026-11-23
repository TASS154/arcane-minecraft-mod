"""Modelos 3D do Cetro: 8 pontas x 6 hastes. Cada combinacao e um modelo; o modelo base escolhe pelo predicado.

Rode depois de gen_item_models.py (usa a mesma paleta):  python tools/gen_scepter_models.py
"""
import json
from gen_item_models import ASSETS, box, model, palette

HEADS = ["amethyst", "diamond", "emerald", "redstone", "lapis", "quartz", "ender", "star"]
SHAFTS = ["wood", "gold", "iron", "bone", "blaze", "obsidian"]
SHAFT_COLOR = {"wood": ("wood", "gold"), "gold": ("gold", "gold_d"), "iron": ("steel", "grey_d"),
               "bone": ("bone", "grey"), "blaze": ("blaze", "orange"), "obsidian": ("obsidian", "violet_d")}


def scepter(head, shaft):
    body, trim = SHAFT_COLOR[shaft]
    r = -45
    els = [
        box((7.4, 0.0, 7.4), (8.6, 1.0, 8.6), trim, r),       # pomo de baixo
        box((7.55, 1.0, 7.55), (8.45, 11.0, 8.45), body, r),  # haste
        box((7.3, 4.0, 7.3), (8.7, 4.8, 8.7), trim, r),       # anel da empunhadura
        box((7.0, 11.0, 7.0), (9.0, 11.8, 9.0), trim, r),     # base da coroa
        box((6.6, 11.8, 7.6), (7.2, 14.2, 8.4), trim, r),     # garra esquerda
        box((8.8, 11.8, 7.6), (9.4, 14.2, 8.4), trim, r),     # garra direita
        box((7.6, 11.8, 6.6), (8.4, 14.2, 7.2), trim, r),     # garra de tras
        box((7.6, 11.8, 8.8), (8.4, 14.2, 9.4), trim, r),     # garra da frente
        box((7.1, 12.0, 7.1), (8.9, 14.6, 8.9), head, r),     # gema
        box((7.6, 14.6, 7.6), (8.4, 15.6, 8.4), head, r),     # ponta da gema
    ]
    if head == "star":
        els.append(box((6.4, 13.0, 7.8), (9.6, 13.6, 8.2), head, r))
    return model(els, [30, 225, 0], 1.0)


def main():
    palette()
    out = ASSETS / "models/item"
    overrides = []
    for hi, h in enumerate(HEADS):
        for si, s in enumerate(SHAFTS):
            name = f"scepter_{h}_{s}"
            (out / f"{name}.json").write_text(json.dumps(scepter(h, s), indent=2) + "\n", encoding="utf-8")
            overrides.append({"predicate": {"arcane:head": hi, "arcane:shaft": si}, "model": f"arcane:item/{name}"})
    base = scepter(HEADS[0], SHAFTS[0])
    base["overrides"] = overrides
    (out / "scepter.json").write_text(json.dumps(base, indent=2) + "\n", encoding="utf-8")
    print(len(overrides), "modelos de cetro")


if __name__ == "__main__":
    main()
