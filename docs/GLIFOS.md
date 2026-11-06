# Glifos do Arcane

Gerado por `tools/gen_glyph_docs.py` a partir de `tools/glyph_table.py`. **Não edite à mão.**

São **42 glifos** em 6 categorias. Parâmetros aparecem como `padrão (mín–máx)`; o servidor sempre limita os valores a esse intervalo.

## Como compor

1. A **Forma** vem primeiro: define de onde o feitiço sai.
2. **Filtros** escolhem quem sofre. **Efeitos** fazem o trabalho. **Modificadores** ajustam o vizinho.
3. Uma **Condição** (ou o modificador **Atraso**) abre um trecho novo: o que vem depois só roda se ela passar, ou depois do tempo.
4. A **ordem importa**: `Projétil → Dano → Fogo` não é `Projétil → Fogo → Dano`.
5. Na Bancada, passe o mouse sobre uma linha e use a **roda do mouse** para ajustar o parâmetro principal (Shift: x5).

### Exemplos que funcionam (testados em mundo real)

| Magia | Composição | O que faz |
|-------|-----------|-----------|
| Bola de Fogo | Projétil → Dano → Fogo | Orbe que fere e incendeia |
| Armadilha de pedra | (Pena) Eu → Dano, num bloco | Fere quem pisa no bloco |
| Lança de Gelo | Projétil → Dano → Lentidão | Fere e deixa o alvo lento |
| Fuga | Eu → Salto → Velocidade | Teleporta e corre |
| Rajada | Aura → Empurrão → Dano | Afasta e fere tudo em volta |
| Caçador de mortos | Aura → Mortos-vivos → Mais Próximo → Dano | Só o morto-vivo mais próximo |
| Luz de emergência | Eu → Em Perigo → Escudo | Escudo só quando a vida está baixa |
| Mina | Raio → Escavar | Quebra o bloco que você mira (se for mole) |
| Fogueira à distância | Projétil → Ao Bater → Incendiar | Acende fogo onde o orbe bate |
| Bomba-relógio | Raio → Atraso → Explosão | Explode 2 s depois |

## Formas (5)

Como o feitiço chega ao alvo. **Todo feitiço começa por uma Forma** (a única exceção são Condições e Ativadores). Só pode haver uma.

| Glifo | Id | Mana | Parâmetros | O que faz |
|-------|----|------|------------|-----------|
| **Eu** | `arcane:self` | 1 | — | Forma: o feitiço acontece em você. |
| **Toque** | `arcane:touch` | 2 | `range` 3 (1–6) | Forma: afeta o que estiver na sua mira, bem perto (3 blocos). |
| **Projétil** | `arcane:projectile` | 3 | `speed` 1.5 (0.5–3) | Forma: dispara um orbe que aplica o feitiço onde acertar (criatura ou bloco). |
| **Raio** | `arcane:ray` | 3 | `range` 24 (4–48) | Forma: um feixe instantâneo que acerta a primeira criatura ou bloco na mira. |
| **Aura** | `arcane:aura` | 4 | `radius` 5 (2–12) | Forma: afeta todas as criaturas ao seu redor (menos você). |

## Filtros (9)

Escolhem quem é afetado. Valem para os efeitos do trecho em que aparecem e para os trechos seguintes.

| Glifo | Id | Mana | Parâmetros | O que faz |
|-------|----|------|------------|-----------|
| **Hostis** | `arcane:hostile` | 1 | — | Filtro: só afeta criaturas hostis. |
| **Somente Eu** | `arcane:self_filter` | 0 | — | Filtro: restringe o efeito a você. |
| **Blocos** | `arcane:blocks` | 1 | — | Filtro: mira blocos em vez de criaturas. |
| **Jogadores** | `arcane:players` | 1 | — | Filtro: só afeta jogadores. |
| **Animais** | `arcane:animals` | 1 | — | Filtro: só afeta animais. |
| **Mortos-vivos** | `arcane:undead` | 1 | — | Filtro: só afeta mortos-vivos (zumbis, esqueletos...). |
| **Exceto Eu** | `arcane:not_self` | 0 | — | Filtro: tira você do alvo. |
| **Mais Próximo** | `arcane:nearest` | 1 | — | Filtro: só a criatura mais próxima de você. |
| **Ferido** | `arcane:wounded` | 1 | — | Filtro: só criaturas com menos da metade da vida. |

## Efeitos (14)

O que acontece com o alvo. A mana de um efeito cresce com o seu parâmetro principal (o dobro do dano custa o dobro).

| Glifo | Id | Mana | Parâmetros | O que faz |
|-------|----|------|------------|-----------|
| **Dano** | `arcane:damage` | 2 | `amount` 4 (1–20) | Efeito: fere o alvo. A criatura atingida passa a perseguir você. |
| **Fogo** | `arcane:fire` | 2 | `duration` 60 (20–200) | Efeito: incendeia criaturas. Em blocos, nada acontece (use Incendiar). |
| **Empurrão** | `arcane:push` | 2 | `strength` 1 (0.3–3) | Efeito: arremessa o alvo para longe de você. Em si mesmo, é um impulso para a frente. |
| **Cura** | `arcane:heal` | 3 | `amount` 4 (1–20) | Efeito: recupera vida do alvo. |
| **Lentidão** | `arcane:slow` | 2 | `duration` 100 (20–600), `level` 1 (0–3) | Efeito: o alvo anda devagar por um tempo. |
| **Veneno** | `arcane:poison` | 2 | `duration` 100 (20–400), `level` 0 (0–2) | Efeito: envenena o alvo, ferindo-o aos poucos (não mata). |
| **Velocidade** | `arcane:speed` | 2 | `duration` 200 (20–1200), `level` 1 (0–3) | Efeito: o alvo corre mais rápido por um tempo. |
| **Levitação** | `arcane:levitate` | 2 | `duration` 40 (10–200) | Efeito: o alvo flutua para cima por poucos segundos. |
| **Escudo** | `arcane:shield` | 3 | `duration` 600 (100–2400), `level` 1 (0–4) | Efeito: concede corações de absorção ao alvo. |
| **Trovão** | `arcane:thunder` | 6 | — | Efeito: um raio cai sobre o alvo. Cuidado: o fogo se espalha. |
| **Explosão** | `arcane:explode` | 6 | `radius` 2 (1–6) | Efeito: explode no alvo sem destruir blocos. Fere quem estiver perto, inclusive você. |
| **Salto** | `arcane:blink` | 3 | `distance` 8 (2–24) | Efeito: você é teleportado para o ponto que está mirando. |
| **Incendiar** | `arcane:ignite` | 2 | — | Efeito: acende fogo no bloco alvo, se houver espaço livre acima. |
| **Escavar** | `arcane:dig` | 3 | `power` 2 (0.5–5) | Efeito: quebra o bloco alvo se for mole (terra, areia, madeira, pedra comum). Não quebra baús. |

## Modificadores (6)

Alteram o glifo vizinho. Funcionam antes ou depois do efeito que modificam.

| Glifo | Id | Mana | Parâmetros | O que faz |
|-------|----|------|------------|-----------|
| **Área** | `arcane:area` | 2 | — | Modificador: dobra o raio da forma e do efeito anteriores (Aura, Explosão). |
| **Alcance** | `arcane:more_range` | 1 | — | Modificador: aumenta em 50% o alcance da forma (Toque, Raio, Salto). |
| **Atraso** | `arcane:delay` | 1 | `seconds` 2 (0.5–30) | Modificador: o que vem depois só acontece alguns segundos mais tarde. |
| **Potência** | `arcane:power` | 2 | — | Modificador: +50% de força no efeito anterior (dano, cura, empurrão, nível). |
| **Duração** | `arcane:lengthen` | 1 | — | Modificador: dobra o tempo do efeito anterior (fogo, lentidão, veneno...). |
| **Velocidade do Projétil** | `arcane:swift` | 1 | — | Modificador: o projétil voa 50% mais rápido. |

## Condições (7)

Abrem um trecho novo do feitiço que só acontece se a condição for verdadeira.

| Glifo | Id | Mana | Parâmetros | O que faz |
|-------|----|------|------------|-----------|
| **Ao Atingir** | `arcane:on_hit_entity` | 0 | — | Condição: o que vem depois só acontece ao atingir uma criatura. |
| **Ao Bater** | `arcane:on_hit_block` | 0 | — | Condição: o que vem depois só acontece ao atingir um bloco. |
| **Após Segundos** | `arcane:after_seconds` | 0 | `seconds` 2 (0.5–30) | Condição: o que vem depois só acontece após alguns segundos. |
| **Em Perigo** | `arcane:low_health` | 0 | — | Condição: o que vem depois só acontece se você estiver com menos da metade da vida. |
| **Agachado** | `arcane:sneaking` | 0 | — | Condição: o que vem depois só acontece se você estiver agachado. |
| **Noite** | `arcane:night` | 0 | — | Condição: o que vem depois só acontece à noite. |
| **Na Água** | `arcane:in_water` | 0 | — | Condição: o que vem depois só acontece se você estiver na água. |

## Ativadores (1)

Disparam algo armazenado.

| Glifo | Id | Mana | Parâmetros | O que faz |
|-------|----|------|------------|-----------|
| **Ativar** | `arcane:activate` | 1 | — | Ativador: dispara um ritual armazenado (rituais ainda não estão disponíveis). |

## Limites de segurança

- **Explosão** nunca destrói blocos. No máximo 3 explosões, raio máximo 8.
- **Escavar** só quebra blocos de dureza até o parâmetro `power` (máx. 5, antes da obsidiana), no máximo 4 por conjuração, e **nunca** blocos com inventário (baús, fornalhas) nem inquebráveis.
- **Escavar** e **Incendiar** só agem em blocos se quem conjura é jogador ou se a regra `mobGriefing` estiver ligada (os magos NPC respeitam a regra).
- **Trovão**: no máximo 3 raios por conjuração. **Cuidado:** o raio pode acertar você se mirar perto.
- Potencial do cristal (×2 a ×32) amplifica dano, cura, duração e força, mas **não** alcance, raio nem distância.
- Durações de efeitos de poção têm teto (ex.: Lentidão 60 s, Veneno 30 s).

## Como se descobre um glifo

Os 3 primeiros (Eu, Projétil, Dano) você já conhece. Os demais:

1. **Ver em ação:** quando um mago conjura perto de você (até 24 blocos), você passa a *suspeitar* dos glifos da magia dele.
2. **Experimentar:** glifos *suspeitos* aparecem na Bancada com `?`. Salvar uma composição com eles não grava a magia, mas aumenta seu entendimento: Suspeito → Parcial → Identificado.
3. **Notas de glifo:** estudar uma nota (botão direito) identifica o glifo na hora. Elas vêm de baús (masmorras, minas, templos, fortalezas, cidades antigas) e dos magos mortos (uma nota de um glifo da magia dele).

Os glifos mais baratos são mais comuns nos baús; Trovão e Explosão (custo 6) são raros.
