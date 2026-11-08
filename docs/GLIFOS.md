# Glifos do Arcane

Gerado por `tools/gen_glyph_docs.py` a partir de `tools/glyph_table.py`. **Não edite à mão.**

São **85 glifos** em 6 categorias. Parâmetros aparecem como `padrão (mín–máx)`; o servidor sempre limita os valores a esse intervalo.

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
| Onda de gravidade | Onda → Marcar → Marcados → Gravidade | A onda só marca; depois só os marcados são esmagados |
| Singularidade | Raio → Gravidade → Gravidade → Gravidade | Três gravidades somam: o alvo é esmagado com força |
| Contra-ataque | Eu → Ao Sofrer Dano → Salto | Fica armada (60 s): ao levar dano, você salta |
| Ponte de gelo | Eu → Congelar | Água em volta de você vira gelo |
| Muralha | Barreira | Parede de energia 3x3 que some sozinha |
| Grupo | Raio → Grupo → Dano → Fogo → Fim do Grupo → Potência | A Potência vale para Dano **e** Fogo |

## Formas (10)

Como o feitiço chega ao alvo. **Todo feitiço começa por uma Forma** (a única exceção são Condições e Ativadores). Só pode haver uma.

| Glifo | Id | Mana | Nível | Especialidade | Parâmetros | O que faz |
|-------|----|------|-------|---------------|------------|-----------|
| **Eu** | `arcane:self` | 1 | 1 | Terra | — | Forma: o feitiço acontece em você. |
| **Toque** | `arcane:touch` | 2 | 1 | Terra | `range` 3 (1–6) | Forma: afeta o que estiver na sua mira, bem perto (3 blocos). |
| **Projétil** | `arcane:projectile` | 3 | 1 | Terra | `speed` 1.5 (0.5–3) | Forma: dispara um orbe que aplica o feitiço onde acertar (criatura ou bloco). |
| **Raio** | `arcane:ray` | 3 | 2 | Tempestade | `range` 24 (4–48) | Forma: um feixe instantâneo que acerta a primeira criatura ou bloco na mira. |
| **Aura** | `arcane:aura` | 4 | 3 | Força | `radius` 5 (2–12) | Forma: afeta todas as criaturas ao seu redor (menos você). |
| **Onda** | `arcane:wave` | 3 | 2 | Força | `range` 8 (2–16) | Forma: uma onda em cone a sua frente, afetando todas as criaturas que ela alcança. |
| **Pilar** | `arcane:pillar` | 4 | 3 | Tempestade | `range` 24 (4–48), `radius` 2 (1–6) | Forma: uma coluna vertical no ponto que você mira (criaturas e o bloco atingido). |
| **Barreira** | `arcane:barrier` | 4 | 3 | Terra | `duration` 200 (40–600), `radius` 3 (2–12) | Forma: ergue uma parede de energia a sua frente (3x3 com raio 3; Area e raio ampliam). Com Domo, Esfera ou Anel vira uma casca em volta do centro. Os glifos seguintes atingem os blocos e quem estiver dentro ou encostado. |
| **Invocar Lobos** | `arcane:summon_wolves` | 4 | 2 | Terra | `count` 3 (2–6), `duration` 1200 (200–6000) | Forma de RITUAL (menor): invoca lobos domesticados que lutam por você por um tempo. Os lobos são os alvos do que vem depois. |
| **Invocar Golem** | `arcane:summon_golem` | 6 | 3 | Terra | `duration` 2400 (400–12000) | Forma de RITUAL (médio): ergue um golem de ferro que protege você por um tempo. O golem é o alvo do que vem depois. |

## Filtros (13)

Escolhem quem é afetado. Valem para os efeitos do trecho em que aparecem e para os trechos seguintes.

| Glifo | Id | Mana | Nível | Especialidade | Parâmetros | O que faz |
|-------|----|------|-------|---------------|------------|-----------|
| **Hostis** | `arcane:hostile` | 1 | 1 | Sangue | — | Filtro: só afeta criaturas hostis. |
| **Somente Eu** | `arcane:self_filter` | 0 | 1 | Vazio | — | Filtro: restringe o efeito a você. |
| **Blocos** | `arcane:blocks` | 1 | 1 | Terra | — | Filtro: mira blocos em vez de criaturas. |
| **Jogadores** | `arcane:players` | 1 | 2 | Vida | — | Filtro: só afeta jogadores. |
| **Animais** | `arcane:animals` | 1 | 2 | Vida | — | Filtro: só afeta animais. |
| **Mortos-vivos** | `arcane:undead` | 1 | 2 | Sangue | — | Filtro: só afeta mortos-vivos (zumbis, esqueletos...). |
| **Exceto Eu** | `arcane:not_self` | 0 | 1 | Vazio | — | Filtro: tira você do alvo. |
| **Mais Próximo** | `arcane:nearest` | 1 | 2 | Conhecimento | — | Filtro: só a criatura mais próxima de você. |
| **Ferido** | `arcane:wounded` | 1 | 2 | Sangue | — | Filtro: só criaturas com menos da metade da vida. |
| **Marcados** | `arcane:marked` | 1 | 1 | Conhecimento | — | Filtro: só criaturas que foram Marcadas. |
| **Não Marcados** | `arcane:unmarked` | 1 | 1 | Conhecimento | — | Filtro: só criaturas que NÃO foram Marcadas. |
| **Aliados** | `arcane:allies` | 1 | 2 | Vida | — | Filtro: jogadores, animais domesticados, aldeões e golens de ferro. |
| **Madeira** | `arcane:wood` | 1 | 1 | Terra | — | Filtro: só blocos de madeira (troncos e tábuas). |

## Efeitos (25)

O que acontece com o alvo. A mana de um efeito cresce com o seu parâmetro principal (o dobro do dano custa o dobro).

| Glifo | Id | Mana | Nível | Especialidade | Parâmetros | O que faz |
|-------|----|------|-------|---------------|------------|-----------|
| **Dano** | `arcane:damage` | 2 | 1 | Sangue | `amount` 4 (1–20) | Efeito: fere o alvo. A criatura atingida passa a perseguir você. |
| **Fogo** | `arcane:fire` | 2 | 1 | Fogo | `duration` 60 (20–200) | Efeito: incendeia criaturas. Em blocos, nada acontece (use Incendiar). |
| **Empurrão** | `arcane:push` | 2 | 1 | Força | `strength` 1 (0.3–3) | Efeito: arremessa o alvo para longe de você. Em si mesmo, é um impulso para a frente. |
| **Cura** | `arcane:heal` | 3 | 1 | Vida | `amount` 4 (1–20) | Efeito: recupera vida do alvo. |
| **Lentidão** | `arcane:slow` | 2 | 2 | Gelo | `duration` 100 (20–600), `level` 1 (0–3) | Efeito: o alvo anda devagar por um tempo. |
| **Veneno** | `arcane:poison` | 2 | 2 | Sangue | `duration` 100 (20–400), `level` 0 (0–2) | Efeito: envenena o alvo, ferindo-o aos poucos (não mata). |
| **Velocidade** | `arcane:speed` | 2 | 2 | Vida | `duration` 200 (20–1200), `level` 1 (0–3) | Efeito: o alvo corre mais rápido por um tempo. |
| **Levitação** | `arcane:levitate` | 2 | 3 | Força | `duration` 40 (10–200) | Efeito: o alvo flutua para cima por poucos segundos. |
| **Escudo** | `arcane:shield` | 3 | 2 | Gelo, Vida | `duration` 600 (100–2400), `level` 1 (0–4) | Efeito: concede corações de absorção ao alvo. |
| **Trovão** | `arcane:thunder` | 6 | 4 | Tempestade | — | Efeito: um raio cai sobre o alvo. Cuidado: o fogo se espalha. |
| **Explosão** | `arcane:explode` | 6 | 3 | Fogo | `radius` 2 (1–6) | Efeito: explode no alvo sem destruir blocos. Fere quem estiver perto, inclusive você. |
| **Salto** | `arcane:blink` | 3 | 3 | Vazio | `distance` 8 (2–24) | Efeito: você é teleportado para o ponto que está mirando. |
| **Incendiar** | `arcane:ignite` | 2 | 1 | Fogo | — | Efeito: acende fogo no bloco alvo, se houver espaço livre acima. |
| **Escavar** | `arcane:dig` | 3 | 2 | Terra | `power` 2 (0.5–5) | Efeito: quebra o bloco alvo se for mole (terra, areia, madeira, pedra comum). Não quebra baús. |
| **Marcar** | `arcane:mark` | 1 | 2 | Conhecimento | `duration` 200 (20–1200) | Efeito: marca o alvo (ele brilha). Os Marcados podem ser escolhidos depois pelo filtro Marcados. |
| **Gravidade** | `arcane:gravity` | 3 | 4 | Força | `strength` 1 (0.3–4) | Efeito: o alvo fica pesado e é puxado para baixo (cai com força). Derruba areia, cascalho e bigornas. Vários Gravidade somam. |
| **Atrair** | `arcane:attract` | 2 | 2 | Força | `strength` 1 (0.3–3) | Efeito: puxa o alvo na sua direção. |
| **Cegar** | `arcane:blind` | 2 | 2 | Vazio | `duration` 100 (20–400) | Efeito: o alvo fica cego por um tempo. |
| **Definhar** | `arcane:wither` | 3 | 3 | Sangue | `duration` 80 (20–300), `level` 0 (0–2) | Efeito: o alvo definha (dano contínuo do Wither). |
| **Invisibilidade** | `arcane:invisible` | 3 | 4 | Vazio | `duration` 600 (100–2400) | Efeito: o alvo fica invisível por um tempo. |
| **Corte** | `arcane:cut` | 2 | 1 | Sangue | `amount` 3 (1–16) | Efeito: dano físico (a armadura reduz). Tosquia ovelhas e, em blocos, corta folhas na hora. |
| **Congelar** | `arcane:freeze` | 3 | 3 | Gelo | `duration` 100 (20–400), `radius` 1 (0.5–5) | Efeito: congela criaturas (lentidão e frio) e, nos blocos, transforma água em gelo e lava em obsidiana. |
| **Dobrar** | `arcane:warp` | 8 | 4 | Vazio | `range` 32 (8–64) | Efeito de RITUAL (maior): leva todas as criaturas alvo para o ponto que você mira (até 48). Com Aura ou Pilar e um círculo grande, desloca uma região inteira. |
| **Clima** | `arcane:weather` | 10 | 4 | Vazio | `mode` 1 (0–2) | Efeito de RITUAL (lendário): muda o clima do mundo (0 limpo, 1 chuva, 2 tempestade). Exige a oferenda de uma Estrela do Nether no círculo maior. |
| **Explosão Destrutiva** | `arcane:blast` | 8 | 4 | Fogo | `radius` 3 (1–6) | Efeito: explode no alvo DESTRUINDO blocos (como TNT, com queda de itens) e ferindo quem está perto. Você é imune. Raio 1–6. Respeita as configs do servidor contra destruição de blocos. |

## Modificadores (24)

Alteram o glifo vizinho. Funcionam antes ou depois do efeito que modificam.

| Glifo | Id | Mana | Nível | Especialidade | Parâmetros | O que faz |
|-------|----|------|-------|---------------|------------|-----------|
| **Área** | `arcane:area` | 2 | 1 | Força | — | Modificador: dobra o raio da forma e do efeito anteriores (Aura, Explosão). |
| **Alcance** | `arcane:more_range` | 1 | 1 | Terra | — | Modificador: aumenta em 50% o alcance da forma (Toque, Raio, Salto). |
| **Atraso** | `arcane:delay` | 1 | 2 | Conhecimento | `seconds` 2 (0.5–30) | Modificador: o que vem depois só acontece alguns segundos mais tarde. |
| **Potência** | `arcane:power` | 2 | 1 | Força | — | Modificador: +50% de força no efeito anterior (dano, cura, empurrão, nível). |
| **Duração** | `arcane:lengthen` | 1 | 2 | Conhecimento | — | Modificador: dobra o tempo do efeito anterior (fogo, lentidão, veneno...). |
| **Velocidade do Projétil** | `arcane:swift` | 1 | 2 | Terra | — | Modificador: o projétil voa 50% mais rápido. |
| **Grupo** | `arcane:group` | 0 | 1 | Conhecimento | — | Modificador: abre um grupo. Um modificador depois de Fim do Grupo afeta todos os efeitos do grupo. |
| **Fim do Grupo** | `arcane:end_group` | 0 | 1 | Conhecimento | — | Modificador: fecha o grupo aberto por Grupo. |
| **Enfraquecer** | `arcane:weaken` | 1 | 2 | Conhecimento | — | Modificador: metade da força do efeito anterior (dano, cura, empurrão, nível). Barateia a magia. |
| **Repetição** | `arcane:repeat` | 2 | 3 | Força | `times` 2 (2–4) | Modificador: repete o efeito anterior (no total, o número de vezes do parâmetro). Cada repetição custa o efeito de novo. |
| **Juramento da Quietude** | `arcane:vow_stillness` | 1 | 1 | Conhecimento | `pledge` 2 (1–5) | Juramento personalizável (Promessa 1 a 5): bônus = 28% x promessa^0,85 (promessa 2 = +50%). Exige ficar parado no chão por (promessa-1) x 1 s antes de conjurar. |
| **Juramento de Sangue** | `arcane:vow_blood` | 1 | 3 | Vazio | `pledge` 2 (1–5) | Juramento personalizável (Promessa 1 a 5): bônus = 30% x promessa^0,85 (promessa 2 = +54%). Custa 2 x promessa de vida e recusa se sobrar pouco. |
| **Juramento da Luz** | `arcane:vow_daylight` | 1 | 1 | Conhecimento | `pledge` 2 (1–5) | Juramento personalizável (Promessa 1 a 5): bônus = 25% x promessa^0,85 (promessa 2 = +45%). 1 dia; 2 céu aberto; 3 sem chuva; 4 manhã a tarde; 5 perto do meio-dia. |
| **Juramento Personalizado** | `arcane:vow_oath` | 1 | 3 | Vazio | `kind` 1 (1–5), `seconds` 15 (3–600) | Você escolhe o castigo e quanto tempo ele dura, DEPOIS de conjurar. Tipo: 1 imobilidade, 2 silêncio (nenhuma outra magia), 3 mana selada, 4 fragilidade (+50% de dano recebido), 5 cegueira. Bônus = k x ln(1 + segundos/5), com k 0,30 / 0,22 / 0,35 / 0,25 / 0,20 e teto de +150%. Ex.: imobilidade por 30 s = +58%. |
| **Domo** | `arcane:shape_dome` | 2 | 2 | Força | — | Formato: a forma vira uma meia-esfera (casca de blocos na Barreira; so quem esta dentro na Aura). Use Area para aumentar o raio. |
| **Esfera** | `arcane:shape_sphere` | 3 | 3 | Força | — | Formato: a forma vira uma esfera completa em volta do centro. |
| **Anel** | `arcane:shape_ring` | 2 | 2 | Força | — | Formato: a forma vira um anel no chao em volta do centro (coluna de 2 blocos de altura). |
| **Cone** | `arcane:shape_cone` | 2 | 2 | Arcano | — | Formato: a forma vira um cone de ~100 graus a sua frente (a Onda ja e assim). Na Barreira, uma tampa curva. |
| **Coluna** | `arcane:shape_column` | 2 | 3 | Arcano | — | Formato: a forma vira uma coluna vertical no ponto mirado (o Pilar ja e assim). Na Barreira, um cilindro oco. |
| **Parede** | `arcane:shape_wall` | 2 | 2 | Arcano | — | Formato: a forma vira uma parede perpendicular ao seu olhar, a 3 blocos a frente (a Barreira ja e assim). |
| **Eu (origem)** | `arcane:origin_self` | 1 | 1 | Terra | — | Origem: o centro da forma passa a ser voce, mesmo em formas que normalmente miram (Pilar) ou ficam a frente (parede). |
| **Mira** | `arcane:origin_aim` | 1 | 1 | Terra | — | Origem: o centro da forma passa a ser o ponto para onde voce olha (ate 32 blocos), em vez de voce. |
| **Zona** | `arcane:zone` | 3 | 3 | Conhecimento | `seconds` 10 (2–60), `interval` 1 (0.5–5) | Torna a forma persistente: ela se repete do mesmo lugar a cada intervalo, durante o tempo escolhido, reaplicando os efeitos em quem está dentro (Aura + Fogo vira um campo em chamas; Barreira vira parede contínua; Projétil vira torre). Parâmetros: segundos (até 60) e intervalo (mín. 0,5 s). No máximo 2 zonas suas ao mesmo tempo. Invocações ignoram a Zona. |
| **Cadeia** | `arcane:chain` | 3 | 3 | Força | — | Modificador: o efeito anterior se propaga de criatura em criatura. Explosão e Explosão Destrutiva detonam de novo, menores, em quem estava perto (até 3 saltos, 8 extras); Dano salta em arco para a mais próxima, com 60% do dano a cada salto. Outros efeitos ignoram a Cadeia (a prévia avisa). |

## Condições (12)

Abrem um trecho novo do feitiço que só acontece se a condição for verdadeira.

| Glifo | Id | Mana | Nível | Especialidade | Parâmetros | O que faz |
|-------|----|------|-------|---------------|------------|-----------|
| **Ao Atingir** | `arcane:on_hit_entity` | 0 | 1 | Conhecimento | — | Condição: o que vem depois só acontece ao atingir uma criatura. |
| **Ao Bater** | `arcane:on_hit_block` | 0 | 2 | Conhecimento | — | Condição: o que vem depois só acontece ao atingir um bloco. |
| **Após Segundos** | `arcane:after_seconds` | 0 | 2 | Conhecimento | `seconds` 2 (0.5–30) | Condição: o que vem depois só acontece após alguns segundos. |
| **Em Perigo** | `arcane:low_health` | 0 | 3 | Vazio | — | Condição: o que vem depois só acontece se você estiver com menos da metade da vida. |
| **Agachado** | `arcane:sneaking` | 0 | 2 | Conhecimento | — | Condição: o que vem depois só acontece se você estiver agachado. |
| **Noite** | `arcane:night` | 0 | 2 | Conhecimento | — | Condição: o que vem depois só acontece à noite. |
| **Na Água** | `arcane:in_water` | 0 | 2 | Conhecimento | — | Condição: o que vem depois só acontece se você estiver na água. |
| **Ao Sofrer Dano** | `arcane:on_hurt` | 0 | 3 | Vazio | — | Condição: arma a magia em você: o que vem depois só acontece quando você levar dano (uma vez, por até 60 s). |
| **Mana Baixa** | `arcane:low_mana` | 0 | 2 | Conhecimento | — | Condição: o que vem depois só acontece se sua mana estiver abaixo de 20%. |
| **Parado** | `arcane:standing_still` | 0 | 2 | Conhecimento | — | Condição: o que vem depois só acontece se você estiver parado no chão. |
| **Ao Morrer** | `arcane:on_death` | 0 | 3 | Vazio | — | Condição: arma a magia em você: o que vem depois só acontece quando você morrer (uma vez, por até 60 s). |
| **Quando a Zona Acabar** | `arcane:on_zone_end` | 0 | 3 | Conhecimento | — | Condição: o que vem depois só acontece quando a zona desta magia terminar (uma vez). Sem o glifo Zona, nunca acontece. |

## Ativadores (1)

Disparam algo armazenado.

| Glifo | Id | Mana | Nível | Especialidade | Parâmetros | O que faz |
|-------|----|------|-------|---------------|------------|-----------|
| **Ativar** | `arcane:activate` | 1 | 3 | Vazio | — | Ativador: dispara um ritual armazenado (rituais ainda não estão disponíveis). |

## Limites de segurança

- **Explosão** nunca destrói blocos. No máximo 3 explosões, raio máximo 8.
- **Escavar** só quebra blocos de dureza até o parâmetro `power` (máx. 5, antes da obsidiana), no máximo 4 por conjuração, e **nunca** blocos com inventário (baús, fornalhas) nem inquebráveis.
- **Escavar** e **Incendiar** só agem em blocos se quem conjura é jogador ou se a regra `mobGriefing` estiver ligada (os magos NPC respeitam a regra).
- **Trovão**: no máximo 3 raios por conjuração. **Cuidado:** o raio pode acertar você se mirar perto.
- Potencial do cristal (×2 a ×32) amplifica dano, cura, duração e força, mas **não** alcance, raio nem distância.
- Durações de efeitos de poção têm teto (ex.: Lentidão 60 s, Veneno 30 s).

## Limite e custo

- No máximo **10 glifos** por magia (configurável: `maxGlyphsPerSpell`).
- O custo de mana **cresce exponencialmente** com o tamanho: depois dos 3 primeiros glifos, cada glifo a mais multiplica o custo por 1,3 (`costGrowthPerGlyph`, valor provisório). Uma magia de 10 glifos custa cerca de 6x a soma simples.
- Preparação (Potência, Área, a bancada) aumenta o poder sem a sobretaxa de glifos extras.

## Reativas

`Ao Sofrer Dano` não roda ao conjurar: **arma** o trecho seguinte em você. Dispara uma vez quando você levar dano, ou expira em 60 s. No máximo 3 armadas. Funciona melhor com formas que não dependem de pontaria (Eu, Aura, Onda, Pilar).

## Nível e especialidade (dungeons)

Cada glifo tem um **nível** (1 comum a 4 rarissimo) e uma ou mais **especialidades**. Cada estrutura do mundo tem um nível (a raridade máxima dos glifos que guarda) e especialidades (as famílias favorecidas):

| Estrutura | Nível | Especialidades |
|-----------|-------|----------------|
| Calabouço (spawner) | 1 | Sangue, Terra |
| Mina abandonada | 1 | Terra, Força |
| Iglu | 1 | Gelo, Vida |
| Igreja de aldeia | 1 | Vida, Conhecimento |
| Naufrágio | 1 | Tempestade, Terra |
| Pirâmide do deserto | 2 | Fogo, Sangue |
| Templo da selva | 2 | Sangue, Vida |
| Fortaleza do Nether | 2 | Fogo, Sangue |
| Portal em ruínas | 2 | Fogo, Vazio |
| Posto de saqueadores | 2 | Sangue, Força |
| Ruína submersa grande | 2 | Gelo, Conhecimento |
| Biblioteca da fortaleza | 3 | Conhecimento, Vazio |
| Corredor da fortaleza | 3 | Conhecimento, Força |
| Cruzamento da fortaleza | 3 | Conhecimento, Terra |
| Mansão da floresta | 3 | Sangue, Conhecimento |
| Tesouro do bastião | 3 | Fogo, Força |
| Bastião | 3 | Fogo, Força |
| Cidade antiga | 4 | Vazio, Conhecimento, Força |
| Cidade do End (tesouro) | 4 | Vazio, Tempestade, Força |

Estruturas fracas só dão glifos comuns; só as mais fundas (Cidade Antiga, Cidade do End) guardam Gravidade, Trovão e Invisibilidade. Uma pirâmide do deserto dá mais Fogo e Sangue; uma igreja de aldeia, mais Vida.

## Como se descobre um glifo

Os 3 primeiros (Eu, Projétil, Dano) você já conhece. Os demais:

1. **Ver em ação:** quando um mago conjura perto de você (até 24 blocos), você passa a *suspeitar* dos glifos da magia dele.
2. **Experimentar:** glifos *suspeitos* aparecem na Bancada com `?`. Salvar uma composição com eles não grava a magia, mas aumenta seu entendimento: Suspeito → Parcial → Identificado.
3. **Estudar** o grimório ou o pergaminho de outro mago: você copia as fórmulas, mas só *suspeita* dos glifos. Para entender de verdade, carregue a magia na Bancada (**Editar**) e experimente.
4. **Notas de glifo:** estudar uma nota (botão direito) identifica o glifo na hora. Elas vêm de baús (masmorras, minas, templos, fortalezas, cidades antigas) e dos magos mortos (uma nota de um glifo da magia dele).

Os glifos mais baratos são mais comuns nos baús; Trovão e Explosão (custo 6) são raros.
