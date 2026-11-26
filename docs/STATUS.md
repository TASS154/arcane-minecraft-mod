# Arcane: status atual

Atualizado em 25 nov 2026. Fonte curta do estado do mod. O histórico de cada rodada está em `docs/CHANGELOG_DEV.md`; a
lista de glifos, em `docs/GLIFOS.md` (gerada). O que precisa de olho, ouvido ou mão humana está em
`docs/PENDENTE_HUMANO.md`.

## Números

- **207 glifos** (fonte: `tools/glyph_table.py`): 11 formas, 16 filtros, 30 condições, 114 efeitos, 35 modificadores e
  1 ativador. Tiers 1 a 4, mais 7 glifos originais de tier 5 (só ao derrotar uma Hidra).
- Testes: JUnit (`src/test/java`) e cerca de 272 GameTests (`src/main/java/com/arcane/gametest`).
  Comando: `.\gradlew.bat test runGameTestServer --offline` com JDK 17.
- Até 10 glifos por magia; custo cresce com fator 1,3 a partir do 4º glifo.

## O que existe (mecânica coberta por testes)

- **Motor de magia:** a ordem dos glifos importa (forma, filtros, condições, efeitos, modificadores, grupos, repetição e
  atrasos que se somam). Alvos tipados (criaturas, blocos, pontos vazios), prévia na bancada, zonas, barramento de eventos
  (`SpellEvents`), tabela de interações elementais em `data/arcane/interactions/*.json`.
- **Conjuração:** modos tocar, carregar, segurar e alternar; roda radial (R), conjuntos 1 a 4, regulador de área;
  magias armadas (Ao Sofrer Dano, Ao Morrer, Após N Acertos, Ao Dormir etc.); contramagia; Coro (vários jogadores).
- **Bancada e grimório:** grimório cai ao morrer (só o dono recupera, com penalidade de mana); pousado na bancada, a pena
  escreve 1 s por glifo; outro mago pode levá-lo e estudá-lo (1 glifo por dia de jogo). Estágios 1 a 7 do grimório.
- **Mana:** base, bônus por glifos e poções, cristais (zonas ×N), envenenamento por mana, nível contra controle.
- **Rituais:** altar com oferendas, giz, Bússola Arcana (desenho animado), círculos de raio 4, 8 e 16, lendário pelo valor
  das oferendas. O glifo **Ativar** dispara um ritual armazenado num altar a até 48 blocos.
- **Desenhos em objetos:** Pena Arcana + tintas (comum, sangue, invisível), canetas de bloco, Caneta de Fênix e de
  Criatura, solvente; gatilhos por pisar, abrir, proximidade, sinal de redstone; armadura e escudo.
- **Mundo:** magos NPC (18 variantes, personalidades), cristais de mana, Ruínas Arcanas, covis de Hidra com masmorra
  procedural, Hidras de 14 estágios (seladas, fases por cabeça, fúria, morte por glifo), Golem Rúnico e Lobo Espectral.
- **Restaurar** (diário de blocos em memória, com teto), Bolso Arcano, Cetro/cajado (6 magias, 48 aparências).
- **Regras do mundo:** `/arcane rules` com perfis Seguro, Épico e Sem limites; tetos de área, ritual e cristal em config.
- **Servidor dedicado:** subiu sem erro de classe do cliente (18 nov).

## NÃO visto em jogo por um humano (resumo; detalhe em PENDENTE_HUMANO.md)

Estes pontos foram vistos só pelo roteiro automático ou por nenhum teste visual. Everton jogou cerca de 2 h em 24 nov,
mas ainda não confirmou cada item.

- Interface: roda R com teclado real, grimório enxuto, GUI do altar, tela de regras e perfis, tooltip da prévia, Bolso.
- Hidra: modelo novo (patas digitigradas, pescoço e cauda longos) andando no terreno; luta real nos estágios altos;
  decapitação; mortes por glifo; covil e masmorra em jogo normal.
- Efeitos: onda, pilar, impacto, flash de conjuração, holograma do Projetor, coluna roxa da gravidade, Petrificar,
  correntes do Selamento, títulos do Batizar, sinais de mana vazando, invisibilidade (distorção).
- Sensação: voo estilo Frieren e níveis de velocidade, planar, Investida, Gancho, Atravessar, tremor de câmera.
- Som: todos os sons são sintetizados (`tools/gen_sounds.py`) e nenhum foi aprovado por ouvido humano.

## Testes instáveis e problemas conhecidos

| Teste ou problema | Situação |
|---|---|
| Foco entrega a criatura na mira | **Causa achada (25 nov):** a busca de entidades só vê seções já acessíveis; o teste conjurava no mesmo tick do spawn. Agora espera a criatura aparecer na busca. |
| Foco não vê através de parede | Mesmo defeito, mas passava sem testar nada (falso positivo). Corrigido junto. |
| Desenho por proximidade | **Causa achada (25 nov):** jogadores falsos de testes antigos ficavam na área (o GameTest não limpa jogadores). `TestPlayers`. |
| Golpe de cauda da Hidra | Provavelmente a mesma causa (mira o jogador remanescente); não falhou mais. |
| Masmorra do covil com 1 sala | **Corrigido (25 nov):** salas sem lugar eram puladas; `LairDungeonPlanTest`. |
| Pilar olhando de muito alto, Parar o Tempo | Não falharam nas rodadas de 25 nov; causa provável a mesma, não provada. |
| `/kill` não matou a Hidra do estágio 7 | **Explicado (25 nov):** morre na hora; a barra ficava nos 5,5 s da morte em runas. Barra some na morte. |
| Crash "Pose stack not empty" | **Corrigido (25 nov):** voar com Flutuar estando invisível pelo glifo empurrava a pose duas vezes e desfazia uma. |
| Servidor "Can't keep up" | **Medido (25 nov):** cratera de raio 64/128 trava 5,7/13,5 s no cálculo da lista (só sem os tetos). Ver `docs/CARGA.md`. |

## Limitações registradas

- O diário do Restaurar é salvo com o mundo e guarda o autor (25 nov), mas o teto de 4.096 não cobre crateras grandes.
- Roubar o grimório da bancada só funciona com o dono online.
- `ArmedSpells` ainda não passa pelo barramento `SpellEvents`.
- "Propriedades de bloco" das canetas de bloco: design não definido.
- Caçadores sem magia: outro módulo, adiado por Everton.
- As interações elementais usam `SpellElement.mechanic` (mapa antigo de 23 efeitos); os 16 elementos visuais vêm de
  `data/arcane/elements/effects.json`.

## Build e CI

- Toolchain Java 17 (`build.gradle`). O workflow `.github/workflows/build.yml` instala JDK 21 só para rodar o Gradle;
  o toolchain 17 é baixado pelo Gradle. O CI roda `./gradlew build`, que não executa os GameTests.

## Próximas fases (pedido de 25 nov)

A higiene; B auditoria de glifos e aliases; C visual dos elementos; D robustez de servidor; E dividir arquivos grandes;
F balanço por simulação; G proposta de tags de estado. Andamento por fase no fim deste arquivo.

## Andamento das fases

Adendo de 25 nov (prompt 2): fases H a N, nesta ordem. Números novos: `docs/PERGUNTAS.md`, "Números provisórios".

- **H (Boneco de Treino Arcano), 25 nov: feita.** Entidade `training_dummy` (nunca morre, volta ao máximo após 5 s sem
  dano), letreiro em corações (barra, último golpe, média por segundo, tags de estado, mana da última conjuração),
  armadura e perfil de afinidade por clique, `/arcane dummy report` em HP exato. `DamageMeter` é o mesmo medidor da
  simulação de balanço. NÃO visto em jogo. 272/272.

- **E (manutenção), 25 nov: feita.** GameTests divididos por área (`GameTestSupport` + 8 classes); poderes, fases e
  morte da Hidra em `HydraPowers`, `HydraPhases`, `HydraDeath`. 267/267.

- **F (balanço por simulação), 25 nov: feita.** `BalanceSimulationGameTests` (roda só com flag); relatório em
  `docs/BALANCO_AUTO.md`: Explosão é o grande outlier; nenhum laço. Nada alterado. 267/267.

- **G (proposta), 25 nov: feita.** `docs/PROPOSTA_TAGS_DE_ESTADO.md` (tags de estado e reações num sistema só, com
  migração em 5 passos). Nada implementado.

- **D (robustez), 25 nov: feita.** Carga medida e limites propostos em `docs/CARGA.md`; diário do Restaurar salvo com
  autor; orçamento de reentrância; API `ProtectionProvider`; causa dos testes instáveis (jogadores de testes antigos) e
  do `/kill` na Hidra. 266/266.

- **C (visual dos elementos), 25 nov: feita.** Elementos em json, fachada `Fx`, sprites e sons novos, composição,
  reações, orçamento linear no raio. Visto só pelo roteiro (`docs/img/folha_elementos.jpg`); NÃO visto nem ouvido por
  humano. 261/261.

- **B (auditoria de glifos), 25 nov: B1 a B3 feitas; B4 aguarda aprovação.** Relatório em `docs/AUDITORIA_GLIFOS.md`
  (marque `[x]`). Inverter ignora o conhecimento do glifo oposto (GameTests opcionais `GlyphAuditGameTests` falham de
  propósito até a correção ser aprovada). Aliases de glifo prontos (`GlyphAliases`, `GlyphAliasesTest`). 259/259.

- **A (higiene), 25 nov: feita.** CLAUDE.md, este STATUS, log movido para CHANGELOG_DEV, documento antigo arquivado,
  descrição do Ativar corrigida, CI consertado (o `gradlew` estava sem permissão de execução: as 19 últimas execuções
  falharam com "Permission denied" antes de compilar). Crash do voo invisível e Foco instável corrigidos. 257/257.
