# Auditoria de glifos (25 nov 2026)

Relatório da Fase B. **Nada foi cortado nem fundido ainda.** Para aprovar, troque `[ ]` por `[x]` na proposta que quiser
(ou escreva outra ao lado). Só o que estiver marcado será aplicado, com alias para o id antigo (grimórios e inscrições
salvos continuam válidos; ver "Aliases" no fim). Custos e tiers abaixo vêm de `tools/glyph_table.py`; o comportamento,
do código.

Como ler: **diferença real** é o que o código faz de diferente (não o que a descrição diz).

---

## 1. Pares que você pediu

### 1.1 Pena (`feather`) × Planar (`glide`): duplicata exata

| | Pena | Planar |
|---|---|---|
| Handler | `PotionEffectHandler(SLOW_FALLING, 24000)` | `PotionEffectHandler(SLOW_FALLING, 1200)` |
| Duração | padrão 600, de 20 a 24.000 ticks | padrão 200, de 20 a 1.200 |
| Custo / tier | 1 / 1 | 2 / 2 |

**Diferença real:** nenhuma no efeito. Pena é mais barata, mais comum e dura até 20× mais. Planar não tem motivo para
existir: quem o acha já tem algo pior que a Pena.

- [ ] **A (recomendada):** cortar Planar; `glide` vira alias de `feather` (a duração escrita é mantida).
- [ ] B: manter os dois e diferenciar Planar (por exemplo, planar de verdade: avança enquanto cai). Isso é mecânica
  nova; preciso do design.

### 1.2 Atraso (`delay`, modificador) × Após Segundos (`after_seconds`, condição): duplicata funcional

Os dois compilam igual em `InscriptionCompiler`: fecham o trecho e abrem um novo com `delayTicks` somado (os atrasos em
sequência se acumulam nos dois). O handler da condição (`AfterSecondsCondition`) sempre devolve `true`.

**Diferença real:** (1) custo: Atraso 1 de mana, Após Segundos 0; (2) uma condição encerra o efeito de `Grupo` (um
modificador depois dela não alcança o grupo anterior); o Atraso não. (3) A Bancada os mostra em abas diferentes.

- [ ] **A (recomendada):** cortar Atraso; `delay` vira alias de `after_seconds` (mesmo parâmetro `seconds`). Magias
  antigas com Atraso ficam 1 de mana mais baratas e um Grupo aberto antes do atraso deixa de ser alcançado depois dele
  (caso raro).
- [ ] B: cortar Após Segundos (alias para `delay`). Magias antigas ficam 1 de mana mais caras.

### 1.3 Ferido (`wounded`, filtro) × Vida do Alvo Abaixo de X (`target_health`, condição)

| | Ferido | Vida do Alvo |
|---|---|---|
| Papel | **filtra**: só criaturas abaixo de 50% recebem os efeitos | **portão**: se ALGUM alvo estiver abaixo de X%, o trecho roda em TODOS |
| Limite | fixo 50% | X de 5 a 95 (padrão 30) |
| Custo / tier | 1 / 2 | 0 / 2 |

**Diferença real:** semânticas diferentes (escolher alvos × decidir se roda). Não são duplicatas. Parente: Em Perigo
(`low_health`) olha a vida de **quem conjura** (fixo 50%).

- [ ] **A (recomendada):** manter os três. Opcional: dar a Ferido o parâmetro `percent` (padrão 50, então nada muda em
  magias existentes).
- [ ] B: fundir Em Perigo e Vida do Alvo numa condição só com `kind` (0 conjurador, 1 alvo). Mexe na regra de quem é
  olhado; só com aprovação.

### 1.4 Fogo (`fire`) × Incendiar (`ignite`): sobreposição parcial

| Alvo | Fogo | Incendiar |
|---|---|---|
| Criatura | põe fogo (duração), vira vapor se molhada | nada |
| Bloco | acende TNT, fogueira e vela; derrete gelo e neve. **Não** põe fogo em cima de bloco comum | põe fogo **em cima** do bloco |
| Ponto vazio | chama no chão | chama no chão (igual) |

Custo 2 / tier 1 nos dois. **A descrição do Fogo está errada** ("Em blocos, nada acontece"): ele age em blocos especiais.

- [ ] **A (recomendada):** fundir: Fogo passa a pôr fogo em cima de bloco comum também (o que Incendiar faz) e
  `ignite` vira alias de `fire`. Isso muda o Fogo em blocos (passa a incendiar o mundo onde hoje não incendeia): Fogo
  em Aura ou Onda sobre o chão vai espalhar chamas. Respeita `mayAlterWorld`.
- [ ] B: fundir com `kind` (0 criaturas e blocos especiais, como hoje; 1 incendiar blocos). `ignite` vira alias com
  `kind = 1`. Não muda nenhuma magia existente.
- [ ] C: manter os dois e só corrigir a descrição do Fogo.

### 1.5 Dano (`damage`) × Corte (`cut`): manter

| | Dano | Corte |
|---|---|---|
| Fonte de dano | `arcane:spell` (respeita armadura desde 23 nov) | ataque do jogador/mob (`playerAttack`/`mobAttack`) |
| Extras | a criatura persegue você; Cadeia e Perfurar se aplicam | tosquia ovelhas em vez de ferir; corta folhas, teias e plantas (até 6 blocos) |
| Valores | 4 (1–20) | 3 (1–16) |

**Diferença real:** fonte de dano (Corte conta como golpe físico: proteções, espinhos e resistências reagem diferente)
e as utilidades. A frase no código do Corte ("diferente do Dano, que é mágico e ignora armadura") está desatualizada:
o Dano respeita armadura desde 23 nov.

- [ ] **A (recomendada):** manter os dois; corrigir o comentário.
- [ ] B: fundir Corte em Dano com `kind` (0 arcano, 1 físico). Perde-se o nome "Corte" na bancada.

### 1.6 Velocidade do Projétil (`swift`) × parâmetro `speed` do Projétil

`swift` multiplica `speed` da forma por 1,5 (não limitado ao máximo 3,0 da bancada: 3,0 × 1,5 = 4,5). Custa 1 fixo. Subir
o parâmetro na bancada de 1,5 para 2,25 custa mais (o `speed` é chave de magnitude da forma, mas formas não escalam
custo por parâmetro: só efeitos escalam, ver `GlyphDefinition.manaCost`). Então hoje **o parâmetro `speed` é grátis** e
o `swift` cobra 1 para fazer o mesmo e passar do teto.

- [ ] **A (recomendada):** cortar `swift`. Migração especial (não é só alias): ao ler uma magia antiga, remove o
  `swift` e multiplica o `speed` do Projétil por 1,5, limitado a 3,0. Preciso implementar esse caso no `GlyphAliases`.
- [ ] B: manter `swift` como o único jeito de passar do teto 3,0 (documentar).

### 1.7 Modificadores multiplicativos × parâmetros da bancada

O que o código faz (`ModifierApplicator`, `InscriptionCompiler`):

- Os modificadores **multiplicam sem limite**: não respeitam o máximo do parâmetro na bancada.
- O custo do efeito é calculado com o valor **antes** do modificador; o modificador cobra o custo fixo dele. Só a área
  (Aura, Onda, Pilar, Barreira) é recobrada depois dos modificadores (`areaCostOf`).

| Modificador | Faz | Custo | Mesma coisa pelo parâmetro custa |
|---|---|---|---|
| Potência | ×1,5 em amount/strength/power, +1 level | 2 | Dano 4→6: +1,2 de mana (curva `parameterFactor`) |
| Enfraquecer | ×0,5 | 1 | baixar o parâmetro **reduz** o custo; Enfraquecer **soma** 1 |
| Duração | ×2 em duration | 1 | Fogo 60→120: +2,5 de mana |
| Área | ×2 em radius | 2 | a área é recobrada pela área final nos dois casos |
| Alcance | ×1,5 em range/distance | 1 | grátis (forma) |
| Sobrecarga | ×2 força, ×1,5 raio e duração, 1 coração por efeito | 3 | — |

Achados:
1. **Passar do teto:** Dano no máximo (20) com 2 Potências = 45 de dano, pagando o custo de 20. Com Sobrecarga, 90.
   É o único jeito de passar dos tetos da bancada; não sei se é intencional.
2. **Enfraquecer não barateia** como a descrição diz ("Barateia a magia"): soma 1 de mana e mais um glifo. Para gastar
   menos, baixar o parâmetro é melhor.
3. Duração é mais barata que o parâmetro; Potência é mais cara.

Não alterei nada (é balanceamento). Perguntas registradas em `docs/PERGUNTAS.md`:
- [ ] Os modificadores devem respeitar o teto da bancada? (proposta: limitar ao máximo × 2)
- [ ] Os modificadores devem recobrar o efeito pelo valor final, como a área? (proposta: sim, e o modificador passa a
  custar 0 ou 1)
- [ ] Enfraquecer: corrigir a descrição ou fazê-lo baratear de fato (custo do efeito pelo valor final resolve)?

---

## 2. Outras sobreposições achadas

| Glifos | Situação | Proposta |
|---|---|---|
| Inverter(Congelar) = Fogo × **Aquecer** (`warm`) | A descrição de Aquecer diz "o inverso de Congelar", mas Inverter troca Congelar por Fogo | [ ] mudar o par de Inverter para Congelar ↔ Aquecer (Fogo ↔ Molhar?) · [ ] manter |
| Explosão (`explode`, t3) × Explosão Destrutiva (`blast`, t4) | Mesma explosão; a Destrutiva quebra blocos (cratera redonda) | [ ] manter (tier e regra de mundo diferentes) · [ ] fundir com `kind` |
| Olhos Noturnos (`night_eye`) × Visão Arcana (`arcane_sight`) | Visão Arcana já dá visão noturna e mostra resíduos | [ ] manter (Olhos é tier 2 barato) · [ ] cortar Olhos Noturnos (alias) |
| Em Perigo (`low_health`) × Vida do Alvo × Ferido | ver 1.3 | ver 1.3 |
| Secar (`dry`) × Aquecer × Molhar | Secar tira molhado e fogo; Aquecer tira frio e derrete gelo | manter (sem proposta) |
| Formatos Parede/Cone/Coluna × Barreira/Onda/Pilar | "já é assim" por padrão: o formato serve para as outras formas | manter (por desenho) |
| Empurrão em si × Investida | já registrado em PERGUNTAS (Fase 3) | aguardando |
| Levitação × Flutuar × Pena | sobem, voam, caem devagar: papéis diferentes | manter |

---

## 3. Inverter: conhecimento e tier (teste em `GlyphAuditGameTests`)

**Furo confirmado** pelos GameTests opcionais `inverterRequiresKnowingTheResultingGlyph` e
`validationOfInvertedSpellsChecksTheOpposite` (`required = false`: falham sem derrubar a suite).

- A validação (`InscriptionValidator`) só confere o conhecimento dos glifos **escritos**. Inverter troca o efeito na
  execução (`Inversion.resolve`, chamado em `SpellPipeline`). Quem conhece Dano + Inverter cura sem nunca ter
  aprendido Cura.
- **Tier não é checado em lugar nenhum do Java.** É só dado usado pelos geradores de loot. Então "respeitar o tier" se
  resume a respeitar o conhecimento. Mas o furo dá um atalho para glifos raros:

| Escrito (tier) | Vira (tier) | Sobe tier? |
|---|---|---|
| Dano (1) | Cura (1) | não |
| Empurrão (1) | Atrair (2) | sim |
| Lentidão (2) | Velocidade (2) | não |
| Fogo (1) | Congelar (3) | **sim, +2** |
| Levitação (3) | Gravidade (4) | sim |
| Roubo de Vida (2) | Transferir Vida (2) | não |
| Cegar (2) | Invisibilidade (4) | **sim, +2** |
| Medo (2) | Provocar (1) | não |

- Também: o custo pago é o do glifo escrito (Fogo 2 + Inverter 1 = 3, Congelar custa 3, então empata), e o oposto recebe
  os parâmetros do escrito sem checar os limites dele (Congelar sai sem `radius`, usa o padrão).

**Correção proposta (não feita):** na validação, para cada efeito seguido de Inverter que tenha par, exigir também o
conhecimento do oposto (mensagem "você não conhece Cura"). Uma linha no `InscriptionValidator` e os dois testes viram
obrigatórios.
- [ ] Aprovar a correção.

---

## 4. Aliases (feito)

`com.arcane.glyph.GlyphAliases`: mapa de id antigo → id atual, com parâmetros fixados opcionais (fusão com `kind`).
Passam por ele: o codec das inscrições (`GlyphInstance.CODEC`: grimório, inscrições no mundo, itens, altares), o
conhecimento do grimório (fica o maior nível entre id antigo e novo), notas de glifo, estudo na bancada e
`/arcane know`. Ao regravar, sai o id atual. Teste: `GlyphAliasesTest` (JUnit, 5 casos: inscrição salva, sem alias
quebra, fusão com `kind` sem sobrescrever o escrito, cadeia, conhecimento).

O mapa `APPROVED` está **vazio** até você marcar as propostas acima.
