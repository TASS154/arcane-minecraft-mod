# Glifos do Arcane

Gerado por `tools/gen_glyph_docs.py` a partir de `tools/glyph_table.py`. **Não edite à mão.**

São **147 glifos** em 6 categorias. Parâmetros aparecem como `padrão (mín–máx)`; o servidor sempre limita os valores a esse intervalo.

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

## Formas (11)

Como o feitiço chega ao alvo. **Todo feitiço começa por uma Forma** (a única exceção são Condições e Ativadores). Só pode haver uma.

| Glifo | Id | Mana | Nível | Especialidade | Parâmetros | O que faz |
|-------|----|------|-------|---------------|------------|-----------|
| **Eu** | `arcane:self` | 1 | 1 | Terra | — | Forma: o feitiço acontece em você. |
| **Toque** | `arcane:touch` | 2 | 1 | Terra | `range` 3 (1–6) | Forma: afeta o que estiver na sua mira, bem perto (3 blocos). |
| **Projétil** | `arcane:projectile` | 3 | 1 | Terra | `speed` 1.5 (0.5–3) | Forma: dispara um orbe que aplica o feitiço onde acertar (criatura ou bloco). |
| **Raio** | `arcane:ray` | 3 | 2 | Tempestade | `range` 48 (4–48) | Forma: um feixe instantâneo que acerta a primeira criatura ou bloco na mira, a qualquer distância (reduza o alcance para limitar). |
| **Aura** | `arcane:aura` | 4 | 3 | Força | `radius` 5 (2–12) | Forma: afeta todas as criaturas ao seu redor (menos você). |
| **Onda** | `arcane:wave` | 3 | 2 | Força | `range` 8 (2–16) | Forma: uma onda em cone a sua frente, afetando todas as criaturas que ela alcança. |
| **Pilar** | `arcane:pillar` | 4 | 3 | Tempestade | `range` 24 (4–48), `radius` 2 (1–6) | Forma: uma coluna vertical no ponto que você mira (criaturas e o bloco atingido). |
| **Barreira** | `arcane:barrier` | 4 | 3 | Terra | `duration` 200 (40–600), `radius` 3 (2–12) | Forma: ergue uma parede de energia a sua frente (3x3 com raio 3; Area e raio ampliam). Com Domo, Esfera ou Anel vira uma casca em volta do centro. Os glifos seguintes atingem os blocos e quem estiver dentro ou encostado. |
| **Invocar Lobos** | `arcane:summon_wolves` | 4 | 2 | Terra | `count` 3 (1–6), `size` 1 (1–3), `duration` 1200 (200–6000) | Forma de RITUAL (menor): invoca Lobos Espectrais domesticados que lutam por você por um tempo. Número (count) e tamanho (size 1–3) são escolha sua: maiores têm outro modelo (Alfa, Fenrir) e mais vida e dano. Os lobos são os alvos do que vem depois. |
| **Invocar Golem** | `arcane:summon_golem` | 6 | 3 | Terra | `size` 1 (1–3), `duration` 2400 (400–12000) | Forma de RITUAL (médio): ergue um Golem Rúnico que protege você por um tempo. Um golem só, e você escolhe o tamanho (size 1–3: Guardião, Colosso, Titã, cada um com modelo próprio): quanto maior e mais duradouro, mais mana. O golem é o alvo do que vem depois. |
| **Foco** | `arcane:focus` | 2 | 2 | Arcano | `range` 24 (4–48) | Forma: entrega só a criatura viva sob a sua mira (até 24 blocos, não atravessa paredes). Não toca blocos nem pontos vazios e não faz efeito próprio: serve para Atrair, Marcar, Elo e outros que só fazem sentido com uma criatura. Sem criatura na mira, a magia falha e não gasta mana. |

## Filtros (14)

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
| **Forasteiros** | `arcane:outsiders` | 2 | 3 | Arcano | — | Filtro: afeta só quem NÃO é você nem está na sua lista de convidados (comando /arcane guest add|remove|list). Animais domesticados e invocações de você ou dos seus convidados também ficam de fora. Útil em inscrições e zonas que protegem a sua casa. |

## Efeitos (65)

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
| **Flutuar** | `arcane:float` | 5 | 4 | Força | `duration` 200 (40–1200), `speed` 0.6 (0.3–3), `glide` 0 (0–1) | Efeito: o alvo paira e voa com suavidade na direção do olhar, como se fosse erguido pela cabeça (a cabeça vai primeiro, as pernas seguem). W avança, Espaço sobe, Shift desce, Ctrl acelera; sem comandos, ele paira parado. Velocidade de 0,3 a 3 blocos por tick, duração em ticks e planagem (0 a 1: sem comandos, o corpo plana adiante como numa elytra e cai devagar em vez de parar). Custa mana a cada tick enquanto voa (mais em velocidade alta) e acaba sem mana; a primeira queda depois do voo não causa dano. No Grimório, o modo Segurar ou Alternar mantém o voo enquanto você canaliza. |
| **Telecinese** | `arcane:telekinesis` | 6 | 4 | Força | `duration` 100 (20–400), `distance` 6 (2–16), `sphere` 0 (0–6), `same_type` 0 (0–1) | Efeito: segura criaturas e blocos com a mente e os leva pairando diante da mira. O grupo de blocos é definido ao conjurar: um bloco; uma esfera (Esfera 1–6) ao redor do bloco mirado; ou, com Mesmo tipo, os blocos conectados do mesmo material (até 120), opcionalmente limitados pela esfera. Ao soltar, tudo cai com o embalo. Distância 2–16, duração até 20 s. |
| **Chuva** | `arcane:rain` | 2 | 2 | Gelo, Tempestade | `radius` 4 (1.5–10), `duration` 400 (40–1200) | Efeito: uma chuva local cai sobre o alvo: molha quem está na área (raio 1,5–10), apaga fogo e rega plantações. Não muda o clima do mundo (isso é o ritual Clima). |
| **Investida** | `arcane:charge` | 3 | 2 | Força | `distance` 8 (3–16), `damage` 3 (0–12) | Efeito: você avança em linha reta contra o alvo (ou para onde olha), até 16 blocos, ferindo quem estiver no caminho. Diferente de Empurrão em si mesmo, que só dá um impulso, e de Salto, que teleporta. |
| **Planar** | `arcane:glide` | 2 | 2 | Força | `duration` 200 (20–1200) | Efeito: o alvo cai devagar por um tempo (queda lenta), sem dano de queda. É a versão passiva; para voar de verdade, use Flutuar. |
| **Provocar** | `arcane:taunt` | 1 | 1 | Força | — | Efeito: criaturas hostis passam a perseguir você, largando outro alvo e acordando do Sono ou do Medo. Não afeta jogadores. |
| **Medo** | `arcane:fear` | 2 | 2 | Vazio | `duration` 100 (20–400) | Efeito: a criatura larga o alvo e foge de você; enquanto dura, não escolhe você como alvo (1 a 20 s). Jogadores ficam fracos e com as mãos pesadas. |
| **Sono** | `arcane:sleep` | 3 | 3 | Vazio | `duration` 100 (20–600) | Efeito: a criatura larga o alvo e adormece, imóvel e fraca, por até 30 s. Qualquer dano a acorda. |
| **Estrondo** | `arcane:rumble` | 3 | 2 | Tempestade, Força | `radius` 3 (1–8), `damage` 2 (0–12) | Efeito: um trovão seco no alvo: fere de leve, empurra para fora (raio 1–8) e deixa as criaturas zonzas por 1,5 s. Não quebra blocos nem causa fogo. |
| **Escuridão** | `arcane:darkness` | 2 | 2 | Vazio | `duration` 200 (20–600) | Efeito: o alvo fica envolto em escuridão pulsante (efeito Escuridão do jogo): a visão some aos poucos e volta. Mais suave que Cegar. |
| **Molhar** | `arcane:wet` | 1 | 1 | Gelo, Tempestade | `duration` 200 (40–1200) | Efeito: deixa a criatura molhada por até 60 s e apaga o fogo dela. Molhado: Trovão e Raio ferem mais, Gelo congela mais rápido e mais forte, e Fogo nem pega (vira vapor). As regras estão em data/arcane/interactions. |
| **Roubo de Vida** | `arcane:lifesteal` | 3 | 2 | Sangue | `amount` 3 (1–16), `ratio` 0.6 (0.1–1) | Efeito: fere o alvo e devolve a você 60% do dano que realmente causou. Não funciona em si mesmo. Invertido, vira Transferir Vida. |
| **Transferir Vida** | `arcane:life_transfer` | 2 | 2 | Sangue | `amount` 4 (1–16) | Efeito: você passa vida ao alvo (1–16), sem nunca cair abaixo de 1 coração. Invertido, vira Roubo de Vida. |
| **Ler Item** | `arcane:read_item` | 1 | 1 | Conhecimento | — | Efeito: mostra no chat o item que a criatura segura (ou o item solto no chão): nome, quantidade, durabilidade e encantamentos. Só informa; não muda nada. |
| **Satélites** | `arcane:satellites` | 4 | 3 | Força | `count` 2 (1–4), `duration` 200 (40–600), `damage` 2 (0.5–8) | Efeito: orbes giram ao redor do alvo (1–4, 2 blocos de raio) ferindo quem encostar neles, menos o dono. Duração até 30 s. |
| **Elo** | `arcane:link` | 3 | 3 | Vazio | `duration` 300 (40–1200) | Efeito: liga as duas primeiras criaturas alvo (ou você e o alvo): metade do dano que uma sofre passa para a outra, por até 60 s. |
| **Rastro** | `arcane:trail` | 3 | 3 | Força | `duration` 200 (40–600), `damage` 1.5 (0.5–6) | Efeito: o alvo deixa para trás um rastro de brasas (até 24 pontos, 6 s cada) que fere e atrasa quem pisar, menos o dono. |
| **Gancho** | `arcane:hook` | 2 | 2 | Força | `strength` 1.2 (0.5–3) | Efeito: você é puxado em linha reta até o alvo (criatura, bloco ou ponto), como um cabo preso nele; a queda é anulada. Não puxa o alvo (isso é Atrair). |
| **Escalar** | `arcane:climb` | 2 | 2 | Força | `duration` 200 (40–1200) | Efeito: por um tempo, quem avança contra uma parede sobe por ela (jogadores: segurando para frente ou pulo; agache para parar). Anula a queda. |
| **Atravessar** | `arcane:phase` | 4 | 4 | Vazio | `thickness` 4 (1–6) | Efeito: você atravessa uma parede de até 6 blocos na direção em que olha e sai do outro lado, onde houver espaço para você. Sem espaço, nada acontece. |
| **Retorno** | `arcane:recall` | 3 | 3 | Vazio | — | Efeito: o primeiro uso marca o lugar onde você está (10 min); o segundo leva você de volta, se estiver na mesma dimensão. Cada uso gasta a mana da magia. |
| **Encantar** | `arcane:imbue` | 3 | 3 | Força | `duration` 1200 (200–6000), `kind` 1 (1–4), `hits` 6 (1–10) | Efeito: a arma na mão do alvo ganha por um tempo (até 5 min) e por poucos golpes (1–10) +2 de dano e um efeito: kind 1 fogo, 2 gelo, 3 raio (+3), 4 veneno. Só vale golpe corpo a corpo. |
| **Petrificar** | `arcane:petrify` | 4 | 4 | Gelo | `duration` 60 (20–300) | Efeito: o alvo vira pedra por até 15 s: imóvel, sem escolher alvos e com metade do dano recebido. O dano não quebra o efeito (diferente do Sono). |
| **Ácido** | `arcane:acid` | 3 | 3 | Sangue | `amount` 3 (1–12), `corrode` 8 (0–60) | Efeito: fere ignorando a armadura e corrói as peças de armadura do alvo (até 60 pontos de durabilidade cada, nunca as quebra). |
| **Eletrificar** | `arcane:electrify` | 3 | 3 | Tempestade | `duration` 200 (40–600), `mode` 0 (0–1) | Efeito: mode 0 (Eletrificar): o dano que o alvo sofre salta 35% para até 2 criaturas num raio de 4. mode 1 (Condutor): raios que caírem a 24 blocos caem no alvo. Dura até 30 s. |
| **Contágio** | `arcane:contagion` | 3 | 3 | Sangue | `radius` 4 (1.5–8) | Efeito: os efeitos nocivos do alvo (e o fogo) passam para as criaturas num raio de 1,5–8, com 60% da duração. Sem efeito nocivo no alvo, nada acontece. |
| **Reanimar Aliado** | `arcane:reanimate` | 6 | 4 | Sangue | `duration` 600 (100–2400) | Efeito: ergue, perto do alvo, uma criatura que morreu há pouco (até 2 min, não jogador nem chefe), com metade da vida, que luta por você: nunca ataca você nem outros aliados seus e vai atrás de quem agrediu você ou de quem você agrediu. Máximo de 2 por vez e duração de até 2 min. |
| **Sentir** | `arcane:sense` | 1 | 1 | Conhecimento | `radius` 16 (4–32) | Efeito: só você percebe as criaturas vivas num raio de 4–32 blocos, mesmo atrás de paredes ou invisíveis (partículas só para você) e vê quantas são e quantas hostis. |
| **Visão Arcana** | `arcane:arcane_sight` | 2 | 2 | Conhecimento | `duration` 400 (100–1200), `radius` 24 (8–32) | Efeito: visão noturna por um tempo e mostra, só para você, resíduos de magia e inscrições num raio de até 32 blocos, com a contagem. |
| **Emitir Sinal** | `arcane:emit_signal` | 1 | 1 | Tempestade | `duration` 40 (10–200) | Efeito: acende por instantes (0,5 a 10 s) um bloco de Sinal Arcano no ar onde o alvo está, que emite redstone 15 e some sozinho. Só ocupa espaço vazio; máximo 4 por conjuração. |
| **Dissipar** | `arcane:dispel` | 3 | 3 | Conhecimento | — | Efeito: nos outros, tira efeitos benéficos e estados mágicos (Refletir, Eletrificar, Elo, Molhado, Marca); em você, tira os nocivos (Maldição, Silêncio, Petrificar, Medo, Sono) e o fogo. |
| **Refletir** | `arcane:reflect` | 3 | 3 | Força | `duration` 100 (20–400) | Efeito: por até 20 s, projéteis que acertam o alvo voltam para quem os lançou e 40% do dano de perto volta ao agressor. Vale para você também. |
| **Silêncio** | `arcane:silence` | 3 | 3 | Vazio | `duration` 100 (20–600) | Efeito: o alvo não consegue conjurar por até 30 s (jogadores e magos). Não afeta golpes comuns. |
| **Drenar Mana** | `arcane:drain_mana` | 2 | 2 | Vazio | `amount` 20 (5–100) | Efeito: tira até 5–100 de mana de jogadores alvo e passa para você. Não afeta criaturas sem mana. |
| **Troca** | `arcane:swap` | 3 | 3 | Vazio | — | Efeito: você e a primeira criatura alvo trocam de lugar (mesma dimensão). |
| **Revelar** | `arcane:reveal` | 2 | 2 | Conhecimento | `radius` 12 (3–24) | Efeito: tira a invisibilidade e faz brilhar por 5 s as criaturas num raio de 3–24 blocos, e conta quantas estavam escondidas. |
| **Chamariz** | `arcane:decoy` | 2 | 2 | Vazio | `duration` 100 (20–300) | Efeito: uma ilusão no ponto mirado atrai criaturas num raio de 20 blocos e elas deixam de te escolher como alvo por até 15 s. |
| **Maldição** | `arcane:curse` | 3 | 3 | Sangue | `duration` 300 (60–2400) | Efeito: por até 2 min o alvo leva 25% a mais de dano e não recupera vida. Dissipar em si mesmo tira. |
| **Bolso Arcano** | `arcane:pocket` | 2 | 3 | Conhecimento | — | Efeito: abre o seu Bolso Arcano, um espaço de 13 itens só seu (como um Baú do Fim), que permanece quando você morre. Não abre durante combate recente nem aceita shulkers, o grimório ou páginas. |
| **Restaurar** | `arcane:restore` | 4 | 3 | Terra | `radius` 10 (2–16) | Efeito: recompõe, num raio de 2–16 blocos (padrão 10), aos poucos, os blocos que magias destruíram nos últimos 2 minutos (explosões, Cortar, Dano, gelo e fogo), onde ninguém construiu depois. Não devolve itens: explosões não soltam loot (configurável). Cobra um pouco de mana por bloco que volta. |

## Modificadores (32)

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
| **Domínio** | `arcane:domain` | 4 | 3 | Arcano | `seconds` 30 (5–60), `interval` 1 (0.5–5) | Modificador de Barreira: fecha um domo e o mantém. Tudo que está dentro recebe os efeitos da magia continuamente (a cada intervalo) até o domo quebrar: ele acaba quando perde cerca de 10% dos blocos da casca, ou quando passa o tempo. A casca não se refaz e não some sozinha enquanto o Domínio dura; quebrar um pedaço dela o encerra (e dispara "Quando a zona acabar"). Use com Barreira + Domo (ou Esfera) + um efeito (Cura, Dano, Lentidão...). Parâmetros: segundos (até 60) e intervalo (mín. 0,5 s). Conta como Zona (mesmo limite de 2 por conjurador). |
| **Cadeia** | `arcane:chain` | 3 | 3 | Força | — | Modificador: o efeito anterior se propaga de criatura em criatura. Explosão e Explosão Destrutiva detonam de novo, menores, em quem estava perto (até 3 saltos, 8 extras); Dano salta em arco para a mais próxima, com 60% do dano a cada salto. Outros efeitos ignoram a Cadeia (a prévia avisa). |
| **Inverter** | `arcane:invert` | 1 | 2 | Vazio | — | Modificador: troca o efeito anterior pelo seu oposto, com a mesma magnitude. Pares: Dano e Cura, Empurrão e Atrair, Lentidão e Velocidade, Fogo e Congelar, Levitação e Gravidade, Roubo e Transferir Vida, Cegar e Invisibilidade, Medo e Provocar. Efeito sem par ignora o Inverter. |
| **Persistência** | `arcane:persistence` | 3 | 3 | Conhecimento | — | Modificador: o desenho não se desfaz ao disparar. Sem ele, uma inscrição no mundo (armadilha, runa) some depois de disparar uma vez. |
| **Furtivo** | `arcane:stealth` | 2 | 2 | Vazio | — | Modificador: a magia deixa menos rastro: o resíduo no mundo dura 1/4 do tempo e a conjuração não faz som nem partículas. Não esconde o efeito em si. |
| **Falso Rastro** | `arcane:false_trace` | 2 | 2 | Vazio | — | Modificador: o resíduo no mundo leva uma assinatura falsa (que não é a sua) e a magia deixa um segundo resíduo isca a até 10 blocos. Quem analisa o resíduo é enganado. |
| **Sobrecarga** | `arcane:overcharge` | 3 | 3 | Sangue | — | Modificador: dobra a força do efeito anterior (dano, cura, empurrão, raio, potência) e 50% de duração, mas você sofre 1 coração de dano mágico a cada efeito sobrecarregado (nunca fatal: só se você tiver mais de 1,5 coração). Mais forte que Potência, com preço. |
| **Vincular** | `arcane:bind` | 2 | 2 | Vazio | — | Modificador: prende o efeito anterior a você: se for um efeito de poção (Lentidão, Veneno, Cegar, Velocidade...) em outra criatura, ele acaba quando você morrer ou se afastar mais de 40 blocos. Efeitos de outros tipos ignoram o Vincular. |
| **Coro** | `arcane:chorus` | 2 | 3 | Vida | `min` 2 (2–4) | Modificador: a magia só dispara se 2–4 jogadores a conjurarem juntos, dentro do círculo que aparece no chão ao redor de quem começa (raio 8, 3 s para os outros). Todos precisam ter a mesma magia. A força sobe 35% por participante extra (máx. x2,4) e a mana é dividida entre eles. Sem gente suficiente, nada acontece e ninguém paga. |

## Condições (24)

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
| **Ao Entrar na Zona** | `arcane:on_zone_enter` | 0 | 3 | Arcano | — | Condição: o que vem depois só acontece nas criaturas que ENTRAM na zona desta magia (uma vez para cada entrada, conferida a cada pulso). Quem já estava dentro ao conjurar não conta. Sem o glifo Zona, nunca acontece. |
| **Ao Sair da Zona** | `arcane:on_zone_exit` | 0 | 3 | Arcano | — | Condição: o que vem depois só acontece nas criaturas que SAEM da zona desta magia (uma vez para cada saída, conferida a cada pulso). Sem o glifo Zona, nunca acontece. |
| **Ao Matar** | `arcane:on_kill` | 0 | 3 | Sangue | — | Condição: arma a magia em você: o que vem depois só acontece quando você matar uma criatura (uma vez, por até 60 s). Com forma Eu, age em você. |
| **Ao Cair** | `arcane:on_fall` | 0 | 3 | Força | — | Condição: arma a magia em você: o que vem depois só acontece quando você aterrissar depois de cair 3 blocos ou mais (uma vez, por até 60 s). |
| **Ao Pular** | `arcane:on_jump` | 0 | 2 | Força | — | Condição: arma a magia em você: o que vem depois só acontece quando você pular (uma vez, por até 60 s). |
| **Vida do Alvo Abaixo de X** | `arcane:target_health` | 0 | 2 | Conhecimento | `percent` 30 (5–95) | Condição: o que vem depois só acontece se algum alvo criatura estiver com menos de X% da vida (X 5–95, padrão 30). Sem alvo criatura, não passa. |
| **Bioma ou Dimensão** | `arcane:region` | 0 | 2 | Conhecimento | `kind` 1 (1–8) | Condição: só acontece no lugar escolhido (kind): 1 Superfície, 2 Nether, 3 End, 4 Quente (deserto, mesa, Nether), 5 Frio (neve), 6 Floresta (floresta, selva, taiga), 7 Oceano, 8 Pântano. |
| **Segurando Item X** | `arcane:holding` | 0 | 2 | Conhecimento | `kind` 1 (0–7) | Condição: só acontece se o item na sua mão principal for do tipo escolhido (kind): 0 mão vazia, 1 espada ou machado, 2 ferramenta, 3 arco, besta ou tridente, 4 comida, 5 bloco, 6 poção, 7 qualquer item. |
| **Chuva ou Tempestade** | `arcane:raining` | 0 | 2 | Tempestade | `storm` 0 (0–1) | Condição: só acontece com chuva ou tempestade no mundo (storm 1: só tempestade). |
| **Após N Acertos** | `arcane:after_hits` | 0 | 3 | Sangue | `n` 3 (2–10) | Condição: arma a magia em você: o que vem depois só acontece depois de você acertar N golpes em outras criaturas (N 2–10, padrão 3; por até 60 s). |
| **Ativar por Sinal** | `arcane:on_signal` | 0 | 2 | Tempestade | — | Condição: em uma inscrição no mundo, troca o gatilho: ela dispara quando o bloco recebe sinal de redstone (na subida do sinal), tendo como origem a criatura mais perto (6 blocos). Fora de inscrições, não faz nada. |
| **Ativar por Proximidade** | `arcane:on_near` | 0 | 2 | Conhecimento | — | Condição: em uma inscrição no mundo, troca o gatilho: ela dispara quando uma criatura chega a 2,5 blocos do bloco (a criatura mais perto é a origem). Fora de inscrições, não faz nada. |

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
