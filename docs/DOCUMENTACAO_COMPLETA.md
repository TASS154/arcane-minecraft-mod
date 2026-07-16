# Arcane — Documentação Completa do Sistema

Documentação de design e funcionamento do mod **Arcane** (Minecraft 1.20.1, Forge/NeoForge).  
Descreve o sistema como deve funcionar para o jogador. Onde o MVP ainda não cobre o design, há uma nota `[MVP]` ou `[Planejado]`.

---

## Visão Geral

**Arcane** é um sistema de magia modular baseado em **glifos**. Não há feitiços prontos para desbloquear. O jogador descobre conceitos (glifos), combina-os em **inscrições** e conjura o resultado.

O mesmo pipeline executa magia de:

- jogadores  
- NPCs magos  
- itens e scrolls  
- blocos e baús  
- rituais no mundo  

Um arquimago não se define só pelo que lança com a mão: ele transforma o **ambiente** em extensão do grimório — armadilhas, portões, fazendas, muralhas e circuitos mágicos.

| Campo | Valor |
|-------|--------|
| Nome | Arcane |
| Mod ID | `arcane` |
| Versão atual | `0.1.0` (MVP) |
| Minecraft | 1.20.1 |
| Loader | Forge / NeoForge |

---

## Filosofia do mod

> Magia é conhecimento.  
> Não existem feitiços prontos.  
> Existem glifos que representam conceitos.  
> O jogador cria seus próprios feitiços.

Princípios:

1. **Nunca desbloquear “Fireball”.** Aprender `Projétil` + `Fogo` (e o que mais fizer sentido).
2. **Ordem importa.** A sequência de glifos muda o resultado.
3. **Criatividade recompensada.** Combinações emergem das regras, não de exceções hardcoded.
4. **Um pipeline para tudo.** Mesmas regras para jogador, NPC, item, bloco e ritual.
5. **Sem magias exclusivas de NPC.** NPCs usam o mesmo sistema.
6. **Progressão sem XP nem árvore de talentos.** Progresso = descoberta, pesquisa, grimório, componentes.
7. **Mundo como grimório.** Itens, blocos e regiões podem carregar magia.
8. **Ambiente amplifica.** Cristais de mana multiplicam potência; proximidade traz risco.

---

## Objetivos

- Permitir construir praticamente qualquer magia por composição de glifos.
- Fazer o jogador **descobrir** conhecimento sem depender de wiki.
- Escala de poder ligada ao **mundo** (cristais, rituais, precisão), não só ao inventário.
- Transformar bases, dungeons e multiplayer em circuitos mágicos jogáveis.
- Extensível por datapacks (novos glifos, tintas, canetas, cristais) sem reescrever o core.

---

## Loop principal de gameplay

```
Explorar / observar / experimentar
        ↓
Descobrir glifos (notas, ruínas, NPCs, análise)
        ↓
Registrar no grimório + pesquisar
        ↓
Compor inscrição na Bancada Arcana
        ↓
Conjurar (tecla V) ou aplicar no mundo (pena + tinta)
        ↓
Amplificar com cristais / rituais (risco de Mana Poisoning)
        ↓
Evoluir grimório, componentes e território
```

No dia a dia:

1. Abrir a **Bancada Arcana**, montar glifos conhecidos, salvar no grimório.  
2. Conjurar a inscrição ativa.  
3. Perto de **cristais**, ganhar potência absurda — ou sofrer saturação.  
4. Usar **pena + tinta** para escrever em blocos, baús, papéis, itens.  
5. Construir **rituais** grandes para magias de escala.  
6. Morrer → grimório cai → recuperar ou ficar com metade da mana.

---

## Lore do Sistema de Magia

### O que é mana

Existe **um único tipo de mana**: energia arcana que o mago canaliza e regenera naturalmente.

Não há “mana de fogo” vs “mana de gelo”. Elementos e comportamentos vêm dos **glifos**, não de pools separados.

Regiões do mundo concentram mana ambiente. No centro dessas regiões há formações raras — **cristais de mana** — que saturam a realidade ao redor. Magias lançadas ali não são “outro feitiço”: são a **mesma** inscrição, amplificada pelo ambiente.

### O que são glifos

Glifos são **conceitos** inscritos: Forma, Efeito, Filtro, Modificador, Condição, Ativador.

Um glifo não é um feitiço. É uma peça. Sem Forma, a magia não existe. Sem Efeito (em muitos casos), nada acontece. Filtros escolhem alvos; modificadores alteram o que veio antes; condições atrasam ou gatilham segmentos; ativadores disparam rituais armazenados.

### Origem dos grimórios

O grimório é o **repositório vivo** do conhecimento do mago: pesquisas, magias, descobertas, experimentos.

Não se segura na mão. **Flutua** ao lado do portador. Cresce com o jogador — de um tomo discreto a um volume colossal de páginas flutuantes (visão próxima ao grimório de Julius Novachrono: enorme, páginas infinitas, sem capa).

Cada mago tem um **grimório principal**. É identidade e biblioteca. Pode cair na morte, ser recuperado, (no design) copiado ou roubado. Sem ele, o mago fica enfraquecido — a mana máxima cai pela metade.

> **Pergunta aberta:** há uma origem cosmogônica específica (quem criou o primeiro grimório / glifo)? O design atual trata o grimório como artefato pessoal que emerge com o mago, sem mito fundador escrito. Se quiser lore canônica (deuses, civilização antiga, etc.), diga e eu encaixo nesta seção.

### Cristais de mana

Formações naturais **extremamente raras**. Não são só minério: são **eventos mundiais** — pontos de interesse que atraem dungeons, facções, torres e experimentos abandonados.

A potência máxima existe só enquanto o cristal está **conectado ao ambiente**. Extraído, perde a amplificação absurda e vira material para equipamentos, tintas e catalisadores.

### Mana Poisoning

Saturação arcana por exposição prolongada a zonas densas de mana. O corpo absorve mais energia do que consegue canalizar.

Manifestações típicas: corações azulados, náusea, distorção visual, sons alterados, dano contínuo, instabilidade de conjuração, redução temporária de mana máxima. Severidade sobe com **zona** e **tempo de exposição**.

Proteção: máscaras, mantos, armaduras arcanas, poções, rituais de ancoragem, acessórios.

---

## Progressão do Jogador

Não há XP de magia nem árvore de talentos.

### Descoberta

Glifos não vêm “desbloqueados no nível 5”. Surgem por:

- exploração e dungeons  
- livros e muralhas antigas  
- experimentação  
- observação e análise de magias (jogador/NPC)  
- grimórios e notas encontrados  

Níveis de conhecimento (do desconhecido ao domínio):

`UNKNOWN` → `SUSPECTED` → `PARTIAL` → `IDENTIFIED` → `MASTERED`

Só com conhecimento suficiente a inscrição pode ser usada com segurança na conjuração.

`[MVP]` Análise graduada e notas existem em esqueleto; o fluxo completo de descoberta ainda está incompleto.

### Pesquisa

O grimório registra experimentos e progresso de pesquisa. Tópicos avançam até conclusão; o resultado alimenta conhecimento e pistas — sem skill tree.

`[MVP]` Serviço de pesquisa em memória; sem UI completa de diário.

### Grimório

Centro do sistema:

- guarda pesquisas, magias, descobertas e log de experimentos  
- flutua ao lado do jogador  
- evolve visualmente com o tempo  

### Bancada

**Bancada Arcana** — local de composição precisa:

| Local | Poder | Custo de mana | Persistência |
|-------|-------|---------------|--------------|
| Na bancada | 100% (precisão máxima) | Normal | Magia registrada de forma estável |
| Fora da bancada (campo) | ~30% do poder | Custo maior (~1,75×) | Desenhos imprecisos |

`[MVP]` GUI com botões fixos de teste; a versão final lista só glifos conhecidos, com reordenação e preview de custo.

### Evolução visual

Estágios planejados do grimório flutuante (1 → 5):

1. Tomo pequeno e discreto  
2. Páginas mais evidentes  
3. Volume maior, aura leve  
4. Páginas semi-soltas  
5. Forma “Julius”: páginas infinitas flutuando, sem capa  

`[MVP]` Campo `visualStage` existe; progressão automática e modelo 3D ainda não.

---

## Sistema de Glifos

Toda magia é uma **lista ordenada** de instâncias de glifo (inscrição).

### Forma

Define **como** a magia existe no mundo. Sem Forma, não há conjuração válida.

Exemplos de design: Toque, Projétil, Onda, Self, Raio (beam), Barreira, Pilar, Cone, Aura, Armadilha, Invocação.

`[MVP]` Implementados: `self`, `touch`, `projectile`.

### Efeito

Define **o que acontece**. Processados **na ordem** em que aparecem.

Exemplos: Dano, Corte, Fogo, Gelo, Gravidade, Flutuar, Teleporte, Cura, Marcar, Explosão, Empurrão, Veneno…

```
Projétil → Dano → Fogo   ≠   Projétil → Fogo → Dano
```

`[MVP]` `damage`, `fire`, `push`, `heal`.

### Filtro

Restringe **quem/o quê** é afetado: Hostis, Aliados, Players, Self, Marcados, Mortos-vivos, Bosses, Blocos, Água, Madeira…

`[MVP]` `hostile`, `self_filter`, `blocks`.

### Modificadores

Alteram glifos anteriores / o segmento: Área, Mais duração, Delay, Ricochete, Fragmentar, Inverter, Multiplicar, Mais velocidade, Mais alcance, Ignorar armadura…

`[MVP]` `area` (×2 raio), `more_range` (×1,5 alcance), `delay`.

### Condições

Definem **quando** um trecho executa: Ao atingir bloco/entidade, Após X segundos, Ao sofrer dano, Ao morrer, Enquanto parado/agachado, Quando mana &lt; X%…

Permitem armadilhas, reativos e cadeias.

`[MVP]` `on_hit_entity`; `after_seconds` (stub); ativador `activate`.

### Ordem de execução

1. A inscrição é lida **da esquerda para a direita** (ordem do jogador).  
2. Forma resolve a **entrega** (alvos / projétil / toque…).  
3. Em cada segmento: condição → filtros → efeitos (nessa ordem interna) → possíveis cadeias/ativação.  
4. Modificadores pendentes aplicam-se ao próximo efeito/entrega relevante.  
5. Condições podem **adiar** ou **dividir** segmentos.  
6. Projéteis podem **pausar** o pipeline até o impacto.

### Pipeline interno

```
Inscrição
  → Validação (forma primeiro, conhecimento, regras do datapack)
  → Compilação (CompiledExecutionPlan + custo base de mana)
  → Contexto (ambiente, precisão, origem, look, trigger)
  → SpellPipeline
       Form → Targets
       Segments (condition → filters → effects → chain)
  → Amplificação: valor × potência_ambiente × precisão × geometria × escala
```

Fórmula de custo de mana (cast):

```
manaCost = ceil(baseMana × precision.manaCostMultiplier × inkMultiplier)
```

Um único executor serve cast, NPC, item, bloco, scroll e ritual.

---

## Grimórios

### Criação

Cada jogador possui um grimório principal ligado à sua alma/estado. Ele aparece flutuando ao lado e acompanha o portador.

`[MVP]` Spawn no login/respawn se o grimório está “presente”; entidade ainda invisível (sem modelo).

### Evolução

Cresce com descobertas, volume de magias e estágio visual (ver Progressão). Não é “upgrade de item”: é identidade.

### Grimório principal

- Knowledge map (glifos e nível)  
- Spellbook (inscrições salvas)  
- Inscrição ativa (o que a tecla V lança)  
- Discovery log / pesquisas  
- Flags: presente, destruído até tick X, estágio visual  

Conhecimento inicial de teste no MVP: `self`, `projectile`, `damage` + amostra tipo fireball.

### Roubo

`[Planejado]` Jogadores e NPCs podem roubar grimórios. Sem grimório: metade da mana máxima. Impulsiona PvP, facções e risco social.

### Destruição

Se o grimório for **destruído** (não só dropado):

- efeito debilitante por **3 dias in-game** (72 000 ticks)  
- mana máxima reduzida enquanto o estado persistir  

`[MVP]` API de destruição existe; pouco ou nada no gameplay a chama ainda.

### Recuperação

Na morte, o grimório principal **cai** (representado como item recuperável / nota renomeada no MVP). Ao pegar de volta:

- presença restaurada  
- entidade flutuante respawna  
- mana máxima volta ao normal  

Enquanto não recuperar: **50% da mana máxima**.

---

## Sistema de Magias

### Construção

Montagem na Bancada Arcana: escolher glifos conhecidos, ordenar, validar composição.

### Salvamento

Salva no spellbook do grimório (fonte `WORKBENCH`, tinta padrão comum, precisão 1,0). Define inscrição ativa.

### Conjuração

Tecla **V** (padrão) → pedido ao servidor → validação → gasto de mana → pipeline.

NPCs e triggers de mundo usam o mesmo caminho com contextos diferentes.

### Consumo

- Principalmente **mana**.  
- Tinta de sangue pode consumir **vida** na escrita no mundo.  
- Construção automática de ritual: 100% da mana atual + dobro de tinta (design).

### Mana

| Atributo | Valor de referência (MVP) |
|----------|---------------------------|
| Pool máxima | 100 |
| Regen | 0,05 / tick (~1 / s) |
| Sem grimório | ×0,5 na mana máxima |
| Um tipo só | Sim |

### Potência

Não é um “nível do feitiço”. É produto de:

- multiplicador ambiental (cristais)  
- precisão (bancada vs campo)  
- bônus de geometria (rituais manuais)  
- fator de escala do ritual  

Exemplo de design: explosão de raio 8 na Zona 5 (×32) → raio efetivo 256. O jogador não aprende “Explosão Gigante”; usa o **ambiente**.

---

## Sistema de Rituais

Rituais **não são outro tipo de magia**. São a mesma inscrição em **escala** — glifos gigantes no mundo, armazenados até ativação.

Podem:

- aumentar magia em até ~**300%** (design; geometria perfeita até **+150%** no avaliador atual)  
- exigir componentes físicos  
- habilitar efeitos que só fazem sentido em escala (invocações gigantes, teleporte de regiões, muralhas, clima)

### Glifos gigantes

Tamanhos-alvo: **16×16**, **32×32**, **64×64** (config `maxRitualSize`, padrão 64).

### Construção manual

Traço a traço (ou bloco a bloco), respeitando simetria, distância, alinhamento e fechamento. Avaliada ao finalizar.

### Construção automática

HUD com magia, tamanho, orientação e posição. A pena desenha sozinha.

Custo (design): **100% da mana atual** + **dobro de tinta**. **Sem** bônus de precisão geométrica.

### Avaliação de precisão

Nota composta (pesos atuais no código):

| Critério | Peso |
|----------|------|
| Simetria | 0,30 |
| Conectividade | 0,25 |
| Fechamento | 0,25 |
| Precisão | 0,20 |

Score × 1,5, teto **1,5** (+150% de potência).

### Ativação

O ritual fica **armazenado** (dormante/armado). Só dispara com glifo **Ativar**, em qualquer magia ou trigger:

- Toque → Ativar  
- Projétil → Ativar  
- Delay → Ativar  
- Ao sofrer dano / abrir baú / morrer → Ativar  

Casos de uso: minas, armadilhas, portões, puzzles, chefes, eventos multiplayer.

`[MVP]` Modelo `RitualSite` + ativação por pipeline; falta UX de construção no mundo.

---

## Componentes

### Canetas

Canetas **não** são progressão linear (“melhor pena”). São **especializações**. O mago monta um estojo por aventura.

Cada caneta define: superfícies, objetos, precisão, velocidade, consumo de tinta, propriedades especiais.

#### Todas as categorias — superfície

| Caneta | Superfícies / papel |
|--------|---------------------|
| Pena de Giz | Grama, terra, areia |
| Pena de Pedra | Pedras |
| Pena de Obsidiana | Basalto, ardósia, obsidiana |
| Pena do Nether | Blocos do Nether |
| Pena do End | Blocos do End |
| Pena Abissal | Água / superfícies aquáticas |
| Pena de Fênix | Ar (glifos suspensos) |

#### Todas as categorias — objetos

| Caneta | Alvos |
|--------|--------|
| Pena do Ferreiro | Armas, ferramentas, armaduras |
| Pena do Alquimista | Comidas, poções, bebidas, ingredientes |
| Pena do Caçador | Arcos, bestas, flechas, lanças |
| Pena do Demolidor | TNT, explosivos, cristais |
| Pena do Escriba | Livros, papéis, mapas, placas, banners |
| Pena Arcana | Grimórios, cajados, catalisadores, orbes |

`[MVP]` Uma `arcane_pen` genérica (bloco/grimório). Demais canetas: design + datapack futuro.

#### Materiais / Objetos / Superfícies

Qualquer bloco ou item pode ser candidato, **limitado pela caneta**. O mundo vira circuito: o que a caneta aceita, a magia pode habitar.

#### Progressão

Não há “Pena Nv.5”. Progressão = **coletar especializações** e combinar com tintas certas. Ferramentas auxiliares de ritual (design): Projetor de Glifos, Espelho de Simetria.

---

## Tintas

Tintas alteram **comportamento**, não só cor.

### Todos os tipos

| Tinta | Propriedade principal | Status |
|-------|----------------------|--------|
| **Comum** | Neutra (mult. mana 1,0) | MVP |
| **Sangue** | Mais poder / menos custo de mana (0,85); consome vida (2 HP na escrita) | MVP |
| **Invisível** | Visível ao desenhar; some ao fechar; só detectável com meios especiais; mult. 1,1 | MVP parcial |
| **Prismática** | Reduz custo de mana (0,75) | Planejado |
| **Permanente** | Não some com o tempo | Planejado |
| **Efêmera** | Some após tempo | Planejado |
| **Viva** | Corrige pequenas imperfeições | Planejado |
| **Fantasma** | Quase indetectável mesmo com visão arcana | Planejado |
| **Do Núcleo** | Derivada de cristais; enorme energia; rituais potentes; risco de falha/explosão/poisoning; mult. ~0,5 | Planejado |

### Fabricação

`[Planejado]` Receitas ainda não definidas no datapack. Núcleo exige fragmentos/processamento de cristal.

### Propriedades e aplicações

- Multiplicador de custo de mana no cast/escrita  
- Custos secundários (vida, estabilidade)  
- Persistência e visibilidade no mundo  
- Estabilidade de rituais (Núcleo)  

Aplicações: bancada, escrita em blocos, scrolls, rituais automáticos/manuais, itens.

---

## Cristais de Mana

### Zonas (cristal maior)

| Zona | Raio (ref.) | Multiplicador | Risco |
|------|-------------|---------------|--------|
| 1 — Campo Externo | ≤ 32 | ×2 | Seguro; regen↑; partículas leves |
| 2 — Campo Concentrado | ≤ 24 | ×4 | Seguro; ambiente alterado |
| 3 — Contaminação | ≤ 16 | ×8 | Início do Mana Poisoning |
| 4 — Saturação | ≤ 10 | ×16 | Dano, instabilidade, mana máx. ↓ |
| 5 — Núcleo | ≤ 5 | ×32 | Quase letal sem preparação; rituais lendários |

### Cristais menores

Duas zonas: ×1,5 e ×2. Para bases pequenas, oficinas e iniciantes.

### Multiplicadores

Aplicados como **modificador ambiental global** no pipeline. A magia não “sabe” que veio de um cristal — só recebe potência, estabilidade e qualidade da mana local. Futuras fontes (altares, tempestades arcanas) encaixam no mesmo gancho.

### Mana Poisoning (números MVP)

- Ativo a partir da zona 3: `poison = (zona - 2) × 0,15`  
- Confusão periódica; dano mágico se exposição &gt; 0,2  
- Proteção atual: Máscara Arcana **+0,3**, Manto Isolante **+0,4** (teto **0,9**)

### Extração

Minerar/remover a formação: perde o campo de amplificação absurda. O material serve para craft avançado.

`[Planejado]` Worldgen, extração e quebra de campo.

### Fragmentos

`[Planejado]` Fragmentos / núcleos processados → tinta do núcleo, catalisadores, equipamentos. Detalhes de craft e raridade ainda abertos.

### Equipamentos

Design: armadura arcana, máscara, manto, anéis (absorver/converter mana, reduzir intoxicação), rituais de ancoragem.

`[MVP]` Máscara e manto (mitigação de poisoning).

---

## Aplicação em Objetos

Pequenos glifos escritos em objetos (caneta adequada). O objeto passa a executar magia no trigger certo.

| Alvo | Exemplo de comportamento |
|------|--------------------------|
| **Armas** | Ao acertar → Teleporte / Dano / Fogo |
| **Armaduras** | Ao sofrer dano → Escudo / Empurrão / Ativar |
| **Comidas** | Ao comer → Cura ou Veneno |
| **TNT** | Ao colocar → Fogo (detona de imediato) |
| **Flechas** | Ao acertar → Levitar / área |
| **Pérolas do End** | Ao arremessar → Multiplicar |
| **Blocos** | Inscrição no mundo; triggers por toque/proximidade |
| **Baús** | Ao abrir → cast / ativar ritual |
| **Portas** | Ao aproximar → Abrir |
| **Máquinas** | Automatizar (redstone-like via glifos): fazendas, portões, circuitos |

`[MVP]` Escrita em blocos + baú ao abrir. Demais alvos: planejados.

Princípio: **todos** os itens do Minecraft são candidatos, limitados pela caneta.

---

## Scrolls

Não existe item “Scroll” exclusivo.

Qualquer **papel** com inscrição NBT vira pergaminho. Condições definem o gatilho:

- Ao ser dropado / arremessado / queimado / aberto / destruído  
- Clique direito (cast imediato)

`[MVP]` Papel + NBT + clique direito (precisão de campo). Outros gatilhos ainda stub.

---

## Exploração

### Dungeons

Cada dungeon tem **especialização** temática; dificuldade liga à raridade dos glifos.

| Tipo | Temas de glifos |
|------|-----------------|
| Templo arruinado | Sangue, sacrifício |
| Biblioteca | Conhecimento, ilusão |
| Laboratório | Gravidade, tempo |
| Catedral | Luz, cura |
| Fortaleza | Barreiras |

### Especializações

Loot, muralhas analisáveis, livros de pesquisa e puzzles com `Ativar` + rituais.

### Pesquisa

Notas e diários encontrados alimentam o grimório; experimentação fecha lacunas de conhecimento.

### NPCs

Magos no mundo usam o mesmo sistema (ver abaixo). Facções podem disputar cristais.

### Bibliotecas e ruínas

Fontes primárias de descoberta sem wiki: paredes com glifos, tomos parciais, experimentos abandonados perto de cristais.

`[Planejado]` Worldgen e estruturas.

---

## NPCs Magos

### Grimórios

Todo NPC mago tem grimório próprio. Ao ser derrotado, o grimório **sempre cai** (conhecimento/loot).

### IA

Compõe e conjura com o mesmo pipeline. `[MVP]` Loop simples: melee + cast periódico na inscrição ativa contra jogador próximo.

### Pesquisa

`[Planejado]` Podem avançar pesquisas e ensinar / vender resultados.

### Comércio

`[Planejado]` Venda de pesquisas, notas de glifo, componentes — não “feitiços prontos exclusivos”.

### Duelos

`[Planejado]` Confrontos mago vs mago com o mesmo sistema (cópia, análise, roubo de grimório).

---

## Objetivo final (fantasia do late game)

Após dezenas ou centenas de horas, um arquimago não é reconhecido só pelas magias que lança:

é reconhecido porque **alterou o mundo**. Entrar no território dele é sentir que qualquer bloco, objeto ou construção pode fazer parte de uma magia.

---

## Referência rápida — o que o MVP já faz

| Sistema | Jogável agora? |
|---------|----------------|
| Bancada + save + cast (V) | Sim (GUI limitada) |
| Mana + penalidade sem grimório | Sim |
| ~15 glifos + pipeline | Sim |
| Cristal ×2…×32 + poisoning básico | Sim (bloco criativo; sem worldgen) |
| Drop/recuperação de grimório | Sim |
| Baú com inscrição / papel scroll | Parcial |
| Ritual completo no mundo | Esqueleto |
| Canetas/tintas completas | Não |
| Aplicações em armas/comida/TNT… | Não |
| Dungeons / worldgen | Não |
| NPC comércio/duelo/roubo | Não |

Guias relacionados:

- [Como instalar e usar](COMO_INSTALAR_E_USAR.md)  
- [Arquitetura técnica](ARCHITECTURE.md)  
- [Trabalho manual pendente](TRABALHO_MANUAL.md)  

---

## Perguntas em aberto (para fechar o canônico)

1. **Origem dos grimórios / glifos** — há mito fundador, civilização antiga, ou deixamos misterioso de propósito?  
2. **Receitas** de todas as tintas e processamento de fragmentos de cristal?  
3. **Fragmentos** — quantos tipos (pó, fragmento, núcleo) e o que cada um crafta?  
4. **Máquinas** — lista prioritária de automações vanilla (funil, dispensador, farm de golen, etc.)?  
5. **Estágios 1–5 do grimório** — o que exatamente sobe o estágio (nº de glifos MASTERED, nº de magias, tempo, rituais)?  
6. **Roubo/cópia** — grimório copiado fica incompleto? perde páginas? custo?  
7. **Facções** ao redor de cristais — nomes e objetivos?
