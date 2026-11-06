# Design do chat × estado do mod

Este documento cruza **cada decisão do chat de design** com o que existe no código. "Feito" significa implementado **e testado** (a coluna de testes diz onde). Valores marcados como provisórios estão na config do servidor.

Pilar: *magia é conhecimento; conhecimento gera poder; poder gera reputação; reputação gera medo.* Sem XP, sem árvore de talentos, sem classes, sem magias prontas.

## Feito

| Decisão | Como está | Testes |
|---------|-----------|--------|
| Combinação quase livre | O validador só exige: Forma no início, sem repetir o que não pode, ordem coerente, glifos identificados | `InscriptionCompilerOrderTest`, `GrimoireStateTest` |
| **A ordem importa** | O compilador processa na ordem: `Projétil→Dano→Fogo` ≠ `Projétil→Fogo→Dano`. Modificadores valem para o efeito anterior (ou o próximo, se ainda não houve) | `CompilerSemanticsTest` |
| Significado único por glifo | 64 glifos, cada um uma ideia; sem exceções por contexto | `everyGlyphHasAHandler` |
| Categorias Forma/Efeito/Filtro/Modificador/Condição | Todas, com 64 glifos (lista em [GLIFOS.md](GLIFOS.md)) | GameTests de cada glifo |
| **Agrupamento** ("Grupo … Fim do Grupo, AOE") | `Grupo` / `Fim do Grupo`: um modificador depois afeta todos os efeitos do grupo | `aModifierAfterEndGroup…` |
| Efeitos que reagem ao mundo | Fogo acende TNT/fogueiras e derrete gelo e neve; Gelo congela água e transforma lava; Gravidade derruba areia/cascalho/bigorna; Corte limpa folhas e tosquia; Raio vanilla carrega creepers | `fireLightsTnt…`, `freezeTurns…`, `gravityDropsSand` |
| Combo marcar → afetar marcados | `Marcar` + filtros `Marcados`/`Não Marcados`; `Gravidade` soma quando repetida (a "mini singularidade") | `markedAndUnmarked…`, `gravitySlams…AndStacks` |
| Condições / magias reativas | `Ao Sofrer Dano` arma a magia em você (1 disparo, 60 s, máx. 3); também Mana Baixa, Parado, Noite, Agachado, Na Água, Em Perigo, Ao Atingir, Ao Bater | `onHurtArmsTheSpell…` |
| Formas variadas | Eu, Toque, Projétil, Raio, Aura, Onda (cone), Pilar, Barreira (blocos temporários) | testes de cada forma |
| **Até 10 glifos, custo exponencial** | Máx. 10 (`maxGlyphsPerSpell`). Depois dos 3 primeiros, cada glifo ×1,3 (`costGrowthPerGlyph`, provisório). 10 glifos ≈ 6× a soma simples. A prévia da bancada usa a mesma conta do servidor | `costGrowsExponentially…`, `clientCostPreview…` |
| Bancada × campo | Na bancada 100% do poder; fora, 30% e +75% de mana | pré-existente |
| Grimório guarda magias ilimitadas vezes | Magias salvas se conjuram quantas vezes quiser; a capacidade são as **páginas** (8 no início; +2 por Página Arcana) | `pagesSetTheSpellCapacity…` |
| **Grimório sempre cai e leva tudo** | Ao morrer, cai o item com magias, descobertas, diário e páginas; não expira, resiste a fogo e lava, brilha; o jogador fica com um provisório em branco | `ownerRecoveringTheGrimoire…` |
| Penalidade de morte | Metade da mana até recuperar o grimório ou por 3 dias de jogo; depois o provisório vira o seu | lógica no `GrimoireLifecycleHandler` |
| Outro jogador fica com suas magias | Shift + direito no grimório alheio **estuda**: copia as fórmulas, mas só *suspeita* dos glifos | `studyingSomeoneElses…` |
| **Compartilhar magias** | **Copiar** escreve a magia num pergaminho (1 papel + 1 tinta); quem recebe lança ou estuda (Shift + direito) | `studyingTheSameSpellTwice…` |
| Nome da magia é do jogador | A bancada pede o nome; salvar com o mesmo nome substitui (editar) | — |
| Descoberta por experimentação / observação / pesquisa | Ver magos conjurar → *suspeitar*; experimentar → entender; notas e estudo → identificar; diário registra | `SyncTest`, GameTests |
| NPCs usam o mesmo sistema | Os magos conjuram composições reais pelo mesmo `CastingService` (hoje 6 elementos fixos) | `everyMageElementCastsAValidSpell` |
| Glifos raros e custosos, não únicos | Nível 1–4 por glifo; copiáveis entre grimórios | `everyGlyphHasATierAndAFamily` |
| **Dungeons por nível e especialidade** | 19 estruturas vanilla com nível (raridade máxima) e famílias favorecidas (Sangue, Fogo, Vida, Conhecimento, Vazio…) | `LootTablesTest`, `glyphLootIsInjected…` |
| Zonas de mana e Mana Poisoning | Cristais (×2 a ×32 perto), envenenamento com náusea e dano; máscara e manto protegem | `crystalZoneDetectedAtAnyOffset` |
| Selo geométrico (formas encaixadas) | Na aba Magias do Grimório: cada glifo é um anel que envolve o anterior (triângulo, círculo, quadrado, losango, pentágono), rotação própria por glifo | `SealGeometryTest` + verificado no jogo |
| Grimório flutuante, não segurado | Livro que flutua ao lado, com páginas virando | verificado no jogo |
| Estágios do grimório | Calculados pelas páginas (7 estágios) e mostrados na tela | `pagesSurviveSaveAndLoad` |

## Pendente (por tamanho e dependência de decisão)

Ordenado do que mais valeria fazer a seguir.

1. **Rituais: implementados (primeira versão).** Altar Arcano, Giz e Bússola Arcana, círculos de raio 4/8/16 (×1,5 / ×3 / ×10), glifos exclusivos de ritual e componentes físicos, glifo Ativar, traços que acendem. Ainda em aberto: regra do nível Lendário, listas finais de componentes e círculos maiores que 16. Ver "Rituais" em `docs/COMO_INSTALAR_E_USAR.md`.
2. **Juramentos (binding vows).** Aceitar uma limitação em troca de poder (parado +150%, só à noite +80%, um alvo +200%, metade da vida +500%, sem mana por 10 min +700%). Cabe bem como uma nova categoria de modificador com custo e restrição checada no servidor.
3. **Assinatura arcana e cicatrizes no mundo.** Cada mago "desenha diferente": a assinatura sairia do estilo (ordem, parâmetros, rotação do selo), permitindo rastrear quem lançou. Magias fortes deixam marcas por dias (grama queimada, pedras flutuando, árvores envelhecidas, neve).
4. **Análise ativa.** Hoje só se *suspeita* ao ver ou estudar. Falta analisar uma magia já lançada (resíduo no mundo, vítima) e a aba de pesquisa do grimório com hipóteses e comparações ("A B C / A B D / A B E").
5. **Estrutura em árvore.** Hoje a magia é uma lista ordenada com grupos. Ramos paralelos reais (um projétil que causa dano de fogo **e** marca ao mesmo tempo) exigem uma segunda dimensão na bancada.
6. **Visual do grimório por estágio** (livro → páginas em órbita → "Julius"), a **Aprimoração por ritual** (livro no centro + estrela de 6 pontas com 6 componentes), e o grimório **equipável** de verdade (slot próprio).
7. **Conteúdo de mundo.** Estruturas próprias (templo de sangue arruinado, catedral subterrânea, laboratório), livros e murais com a "língua perdida", glifos exclusivos por dungeon, NPCs que ensinam, vendem, copiam, duelam e roubam grimórios, e reação ao grimório famoso.
8. **Mais condições** (Ao Morrer, Quando outro glifo terminar, Ao quebrar a barreira), **mais interações** (Raio energiza para-raios e Redstone, cozinhar itens), e **qualidade da mana** pelo ambiente (instável, pura, corrompida, sagrada).
9. **Visuais únicos por tipo de dano** (um projétil de corte chega diferente de um de fogo).
10. **Mana Poisoning completo:** visão distorcida, mutações temporárias, magia saindo errada, explosões espontâneas.

## Decisões que tomei e preciso que você confirme

- **Custo exponencial:** ×1,3 por glifo depois dos 3 primeiros. Provisório; o ritmo certo só se descobre jogando.
- **8 páginas iniciais** e 2 por Página Arcana. Páginas só em baús de estruturas de nível 2+; faltam as formas de crescimento por ritual.
- **Penalidade de 3 dias:** entendi como "dura até recuperar o grimório **ou** por 3 dias de jogo, o que vier primeiro". Se quiser que dure até recuperar (sem teto), é uma linha.
- **Se o grimório for destruído** (só o vazio o destrói), o jogador perde o conteúdo e fica com o provisório. Ou prefere um backup parcial?
- **Estudar não consome** o grimório alheio (dá para estudar de novo; não duplica). O pergaminho **é consumido** ao estudar.
- **A magia "Faísca"** (Projétil→Dano) já vem no grimório novo, para o jogador conseguir conjurar logo. O design diz "nunca nomeie magias": quer que o grimório comece vazio?
- **Raio, Pilar, Onda, Barreira** respeitam `mobGriefing` quando quem conjura é um mago NPC.

## Perguntas para destravar os rituais

1. O Compasso Arcano desenha sozinho (consumindo giz/tinta/sangue) ou só mostra o holograma e você desenha?
2. O nível do ritual (150% / 300% / 1000%) vem do **tamanho** do círculo, dos **componentes** ou de ambos?
3. Rituais lendários "quebram regras": lista fechada (teleportar vila, invocar dragão, alterar clima) ou qualquer magia com ritual lendário?
4. O ritual é **dormente** até algo disparar (glifo Ativar) ou dispara ao fechar o círculo?
5. Cada ritual precisa de **estrutura específica** (altar, 4 pilares) ou o círculo basta?
6. Juramentos: lista fixa de condições, ou o jogador as monta com os mesmos glifos (Condição → vira juramento)?
