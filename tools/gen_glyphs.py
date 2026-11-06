"""Gera data/arcane/glyphs/*.json e as chaves glyph.arcane.* dos idiomas a partir de tools/glyph_table.py.

Uso:  python tools/gen_glyphs.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from glyph_table import G  # noqa: E402

ROOT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources"
GLYPH_DIR = ROOT / "data" / "arcane" / "glyphs"
LANG_DIR = ROOT / "assets" / "arcane" / "lang"


def definition(gid, g):
    d = {"category": g["cat"], "behavior": {"handler": f"arcane:{gid}"}}
    if g["magnitude"]:
        d["behavior"]["magnitude_keys"] = g["magnitude"]
    if g["comp"]:
        d["composition"] = g["comp"]
    if g["params"]:
        d["parameters"] = {k: {"type": t, "default": df, "min": lo, "max": hi}
                           for k, (df, lo, hi, t) in g["params"].items()}
    if g["scope"]:
        d["modifier_scope"] = g["scope"]
    d["cost"] = {"mana": g["cost"]}
    if g["tier"] != 1:
        d["tier"] = g["tier"]
    return d


def main():
    GLYPH_DIR.mkdir(parents=True, exist_ok=True)
    wanted = set()
    for gid, g in G.items():
        wanted.add(f"{gid}.json")
        (GLYPH_DIR / f"{gid}.json").write_text(json.dumps(definition(gid, g), indent=2, ensure_ascii=False) + "\n",
                                               encoding="utf-8")
    for stale in GLYPH_DIR.glob("*.json"):
        if stale.name not in wanted:
            stale.unlink()
            print("  removido", stale.name)

    for lang, idx in (("pt_br", "pt"), ("en_us", "en")):
        path = LANG_DIR / f"{lang}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        for key in [k for k in data if k.startswith("glyph.arcane.")]:
            del data[key]
        for gid, g in G.items():
            name, desc = g[idx]
            data[f"glyph.arcane.{gid}"] = name
            data[f"glyph.arcane.{gid}.desc"] = desc
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{len(G)} glifos gerados")


if __name__ == "__main__":
    main()
