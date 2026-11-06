"""Gera docs/GLIFOS.md a partir de tools/glyph_table.py (a documentacao nunca desatualiza).

Uso:  python tools/gen_glyph_docs.py
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from glyph_table import G, CATEGORY_ORDER  # noqa: E402

OUT = Path(__file__).resolve().parents[1] / "docs" / "GLIFOS.md"

TITLES = {
    "form": ("Formas", "Como o feitiço chega ao alvo. **Todo feitiço começa por uma Forma** (a única exceção são Condições e Ativadores). Só pode haver uma."),
    "filter": ("Filtros", "Escolhem quem é afetado. Valem para os efeitos do trecho em que aparecem e para os trechos seguintes."),
    "effect": ("Efeitos", "O que acontece com o alvo. A mana de um efeito cresce com o seu parâmetro principal (o dobro do dano custa o dobro)."),
    "modifier": ("Modificadores", "Alteram o glifo vizinho. Funcionam antes ou depois do efeito que modificam."),
    "condition": ("Condições", "Abrem um trecho novo do feitiço que só acontece se a condição for verdadeira."),
    "activator": ("Ativadores", "Disparam algo armazenado."),
}


def fmt(v):
    return str(int(v)) if v == int(v) else str(v)


def main():
    lines = [
        "# Glifos do Arcane",
        "",
        "Gerado por `tools/gen_glyph_docs.py` a partir de `tools/glyph_table.py`. **Não edite à mão.**",
        "",
        f"São **{len(G)} glifos** em 6 categorias. Parâmetros aparecem como `padrão (mín–máx)`; o servidor sempre limita os valores a esse intervalo.",
        "",
        "## Como compor",
        "",
        "1. A **Forma** vem primeiro: define de onde o feitiço sai.",
        "2. **Filtros** escolhem quem sofre. **Efeitos** fazem o trabalho. **Modificadores** ajustam o vizinho.",
        "3. Uma **Condição** (ou o modificador **Atraso**) abre um trecho novo: o que vem depois só roda se ela passar, ou depois do tempo.",
        "4. A **ordem importa**: `Projétil → Dano → Fogo` não é `Projétil → Fogo → Dano`.",
        "5. Na Bancada, passe o mouse sobre uma linha e use a **roda do mouse** para ajustar o parâmetro principal (Shift: x5).",
        "",
        "### Exemplos que funcionam (testados em mundo real)",
        "",
        "| Magia | Composição | O que faz |",
        "|-------|-----------|-----------|",
        "| Bola de Fogo | Projétil → Dano → Fogo | Orbe que fere e incendeia |",
        "| Armadilha de pedra | (Pena) Eu → Dano, num bloco | Fere quem pisa no bloco |",
        "| Lança de Gelo | Projétil → Dano → Lentidão | Fere e deixa o alvo lento |",
        "| Fuga | Eu → Salto → Velocidade | Teleporta e corre |",
        "| Rajada | Aura → Empurrão → Dano | Afasta e fere tudo em volta |",
        "| Caçador de mortos | Aura → Mortos-vivos → Mais Próximo → Dano | Só o morto-vivo mais próximo |",
        "| Luz de emergência | Eu → Em Perigo → Escudo | Escudo só quando a vida está baixa |",
        "| Mina | Raio → Escavar | Quebra o bloco que você mira (se for mole) |",
        "| Fogueira à distância | Projétil → Ao Bater → Incendiar | Acende fogo onde o orbe bate |",
        "| Bomba-relógio | Raio → Atraso → Explosão | Explode 2 s depois |",
        "",
    ]
    for cat in CATEGORY_ORDER:
        title, intro = TITLES[cat]
        items = [(gid, g) for gid, g in G.items() if g["cat"] == cat]
        lines += [f"## {title} ({len(items)})", "", intro, "",
                  "| Glifo | Id | Mana | Parâmetros | O que faz |", "|-------|----|------|------------|-----------|"]
        for gid, g in items:
            params = ", ".join(f"`{k}` {fmt(d)} ({fmt(lo)}–{fmt(hi)})" for k, (d, lo, hi, _) in g["params"].items()) or "—"
            name, desc = g["pt"]
            lines.append(f"| **{name}** | `arcane:{gid}` | {g['cost']} | {params} | {desc} |")
        lines.append("")
    lines += [
        "## Limites de segurança",
        "",
        "- **Explosão** nunca destrói blocos. No máximo 3 explosões, raio máximo 8.",
        "- **Escavar** só quebra blocos de dureza até o parâmetro `power` (máx. 5, antes da obsidiana), no máximo 4 por conjuração, e **nunca** blocos com inventário (baús, fornalhas) nem inquebráveis.",
        "- **Escavar** e **Incendiar** só agem em blocos se quem conjura é jogador ou se a regra `mobGriefing` estiver ligada (os magos NPC respeitam a regra).",
        "- **Trovão**: no máximo 3 raios por conjuração. **Cuidado:** o raio pode acertar você se mirar perto.",
        "- Potencial do cristal (×2 a ×32) amplifica dano, cura, duração e força, mas **não** alcance, raio nem distância.",
        "- Durações de efeitos de poção têm teto (ex.: Lentidão 60 s, Veneno 30 s).",
        "",
        "## Como se descobre um glifo",
        "",
        "Os 3 primeiros (Eu, Projétil, Dano) você já conhece. Os demais:",
        "",
        "1. **Ver em ação:** quando um mago conjura perto de você (até 24 blocos), você passa a *suspeitar* dos glifos da magia dele.",
        "2. **Experimentar:** glifos *suspeitos* aparecem na Bancada com `?`. Salvar uma composição com eles não grava a magia, mas aumenta seu entendimento: Suspeito → Parcial → Identificado.",
        "3. **Notas de glifo:** estudar uma nota (botão direito) identifica o glifo na hora. Elas vêm de baús (masmorras, minas, templos, fortalezas, cidades antigas) e dos magos mortos (uma nota de um glifo da magia dele).",
        "",
        "Os glifos mais baratos são mais comuns nos baús; Trovão e Explosão (custo 6) são raros.",
        "",
    ]
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print("  ", OUT)


if __name__ == "__main__":
    main()
