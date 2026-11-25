"""Monta a folha de contato elemento x forma a partir das fotos do roteiro de dev (DevContactSheet).

Uso:  python tools/dev/contact_sheet.py [pasta_das_fotos] [saida.png]
Padrao: run/screenshots -> run/screenshots/contact_sheet.png (e contact_pairs.png para a composicao).
Requer Pillow.
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image, ImageDraw

ELEMENTS = ["arcane", "fire", "frost", "storm", "venom", "force", "void", "life",
            "earth", "water", "light", "shadow", "blood", "time", "nature", "sound"]
FORMS = ["projectile", "ray", "aura", "wave", "pillar"]
CELL = (260, 170)


def crop(img: Image.Image) -> Image.Image:
    """Recorta o miolo da foto (onde ficam o suporte e a parede) na proporcao da celula."""
    w, h = img.size
    cw = int(w * 0.55)
    ch = int(cw * CELL[1] / CELL[0])
    x0 = (w - cw) // 2
    y0 = int(h * 0.18)
    y0 = max(0, min(h - ch, y0))
    return img.crop((x0, y0, x0 + cw, y0 + ch)).resize(CELL, Image.LANCZOS)


def build(rows, cols, name_of, src: Path, out: Path, label_w=90):
    sheet = Image.new("RGB", (label_w + CELL[0] * len(cols), 24 + CELL[1] * len(rows)), (18, 18, 24))
    d = ImageDraw.Draw(sheet)
    for j, c in enumerate(cols):
        d.text((label_w + j * CELL[0] + 6, 6), c, fill=(230, 230, 230))
    missing = 0
    for i, r in enumerate(rows):
        d.text((6, 24 + i * CELL[1] + CELL[1] // 2 - 6), r, fill=(230, 230, 230))
        for j, c in enumerate(cols):
            f = src / f"autotest_sheet_{name_of(r, c)}.png"
            x, y = label_w + j * CELL[0], 24 + i * CELL[1]
            if f.exists():
                sheet.paste(crop(Image.open(f).convert("RGB")), (x, y))
            else:
                missing += 1
                d.rectangle((x + 2, y + 2, x + CELL[0] - 3, y + CELL[1] - 3), outline=(120, 40, 40))
                d.text((x + 8, y + 8), "sem foto", fill=(200, 80, 80))
    sheet.save(out)
    print(f"   {out} ({len(rows)}x{len(cols)}, {missing} sem foto)")


def main():
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("run/screenshots")
    out = Path(sys.argv[2]) if len(sys.argv) > 2 else src / "contact_sheet.png"
    build(ELEMENTS, FORMS, lambda r, c: f"{r}_{c}", src, out)
    pairs = sorted(p.stem.replace("autotest_sheet_", "") for p in src.glob("autotest_sheet_pair_*.png"))
    if pairs:
        build(pairs, ["projectile"], lambda r, c: r, src, out.with_name("contact_pairs.png"), label_w=170)


if __name__ == "__main__":
    main()
