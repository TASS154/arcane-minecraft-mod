# Arcane: status atual

Atualizado em 26 nov 2026. Fonte curta do estado do mod. O histórico de cada rodada está em `docs/CHANGELOG_DEV.md`; a
lista de glifos, em `docs/GLIFOS.md` (gerada). O que precisa de olho, ouvido ou mão humana está em
`docs/PENDENTE_HUMANO.md`.

## Números

- **205 glifos** (fonte: `tools/glyph_table.py`): 11 formas, 15 filtros, 30 condições, 111 efeitos, 37 modificadores e
  1 ativador (26 nov: saíram Planar, Atraso, Ferido, Inverter, o efeito Chuva e Carinho, com aliases). Tiers 1 a 4, mais 7 glifos originais de tier 5 (só ao derrotar uma Hidra).
- Testes: JUnit (`src/test/java`) e 306 GameTests obrigatórios (`src/main/java/com/arcane/gametest`).
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

Prompt 3 de 26 nov (formas, Mais Próximo, poda de glifos, contrato de presença):

- **Feito, 26 nov.** Mapa de aliases com caso "dropado" (`GlyphAliases`, `RemovedGlyphMigrationTest`); cortes de
  Planar, Atraso, Ferido, Inverter (sem tocar em Inversão da Gravidade), efeito Chuva e Carinho, com loot, idioma,
  ícones, handlers e testes; Vincular com uma frase e GameTest de distância. **Mais Próximo** logo depois da forma vira
  entrega (`Seek`): Raio, Toque e Foco miram a criatura mais perto, o Pilar assenta sob ela, o Projétil sai na direção
  dela, Aura e Onda ficam só com ela; depois de outro filtro, continua filtro. Prévia diz qual dos dois. **Raio** não
  entrega ponto vazio e falha sem mana no ar. **Projétil** detona no fim da vida. **Coluna** pega todas as criaturas do
  fuste e o terreno da coluna. **Toque** +25% (config) e alcança o bloco atrás da criatura. Falhas de forma antes da
  mana num lugar só (`CastingService.formFailure`). Presença: feixe que se apaga no ar e estouro do orbe; NÃO vistos.
  306/306.
- **Parado esperando Everton:** números do Corte (`docs/DECISIONS.md` item 3) e forma Chuva (item 4,
  `docs/PROPOSTA_FORMA_CHUVA.md`).

Adendo de 25 nov (prompt 2): fases H a N, nesta ordem. Números novos: `docs/PERGUNTAS.md`, "Números provisórios".

- **H (Boneco de Treino Arcano), 25 nov: feita.** Entidade `training_dummy` (nunca morre, volta ao máximo após 5 s sem
  dano), letreiro em corações (barra, último golpe, média por segundo, tags de estado, mana da última conjuração),
  armadura e perfil de afinidade por clique, `/arcane dummy report` em HP exato. `DamageMeter` é o mesmo medidor da
  simulação de balanço. NÃO visto em jogo. 272/272.
- **I (afinidades elementais), 25 nov: feita.** `data/arcane/affinities/*.json` (6 perfis conservadores, provisórios),
  datapack sobrescreve por id, JSON inválido só gera aviso. O dano dentro de um efeito usa o elemento de gameplay dele
  (`effects.json`), uma vez por golpe, com teto `affinityMaxTotal` junto das reações. Retorno: anel maior e tom agudo na
  fraqueza, tom abafado na resistência (NÃO visto nem ouvido). Hidra sem perfil (inalterada). 276/276.
- **J (modificadores de Projétil), 25–26 nov: feita.** Glifos novos `homing` (Perseguir), `ricochet` (Ricochete),
  `fragment` (Fragmentar) e `arc` (Arco), pela `glyph_table.py`, com loot no padrão de tier. Só valem na forma Projétil
  (a prévia avisa nas outras). Perseguir curva com teto por tick, no cone à frente, e respeita o filtro do trecho; sem
  filtro, nunca você, aliados ou bichos. Ricochete conta cada quique como um acerto e perde força. Fragmentar parte
  em cacos fracos que nunca fragmentam. Arco cai no ponto mirado e Perseguir só curva na descida. Os quatro juntos
  degradam até caber em `projectileMaxTriggers` (8), sem erro. Rastros próprios (espiral, faísca, cacos) NÃO vistos. 283/283.
- **K (reações em massa, Nuvem de Vapor e Diário), 26 nov: feita.** As regras de `interactions/*.json` ganharam `id`,
  `name_key` e `hint_key` (opcionais; o formato antigo continua valendo). `mass_reactions` é uma estrutura genérica de
  limiar para zona; a única hoje é a Nuvem de Vapor (3 molhados queimados, 6 blocos de água no raio ou Chuva + Fogo):
  apaga fogo, molha, fere 0,25 ❤/s e some em 15 s, funde com as vizinhas, teto por chunk e por mundo, respeita
  `worldAlteringSpells`. Véu na tela só para quem está dentro (mais fraco com "reduzir clarões") e chiado novo
  (`reaction_vapor_*.ogg`), NÃO vistos nem ouvidos. Diário de descobertas por jogador (salvo no mundo), aba
  "Descobertas" no Grimório com "???" e "X de Y", aviso discreto e o som de glifo aprendido mais agudo. 287/287 (o teste da nuvem com 3 molhados falhou em 2 de 4 rodadas antes de esperar a conjuração; passou nas 2 últimas: vigiar).
- **L (voz da magia), 26 nov: feita.** `SpellVoice.compose` é pura e determinística (FNV-1a do id escolhe o grau da
  pentatônica, a categoria escolhe a oitava, no máximo 8 notas guardando a primeira, a última e as mais caras, tom
  entre 0,5 e 2,0). O servidor compõe e manda as notas num campo compacto do pacote de efeito de conjuração; o cliente
  toca por uma fila por tick (`VoicePlayer`), com uma amostra sustentada por família (7, em `sounds/voice/`). Rituais e
  magias grandes ganham quinta, oitava e cauda longa. Uma voz por jogador a cada 0,5 s. Opções: ligar/desligar, volume,
  nota ao passar o mouse no grimório (desligada). A pena da bancada soa na nota do glifo. `/arcane voice debug` e
  `tools/preview_voice.py` (WAVs em `tools/out/`). NÃO ouvido. 289/289.
- **M (maestria e assinatura, só aparência), 26 nov: feita.** Maestria (0 a 1) = média dos estados de conhecimento
  dos glifos, pesada pelo custo. Baixa: partículas tremidas, cor lavada, rastro com falhas, voz desafinada; alta:
  anel de acabamento e acorde limpo; maestria 1 é o visual de antes. Assinatura: semente pelo UUID e pelos dois
  elementos mais conjurados (contagem só cosmética, `StyleTally`; ver `docs/DECISIONS.md` item 1), motivo anel,
  espiral ou pontos em até 20% das partículas, paleta de daltonismo própria, desligada pelo Cor, opções para a sua e a
  dos outros. Vão no campo compacto do pacote de efeito. Teste de regressão: maestria 0 e 1 dão o mesmo dano e a mesma
  mana. Folha de contato: palavra `maestria` em `run/arcane_autotest.flag` (18 fotos). NÃO visto. 291/291.
- **N (Guardião de Ruína e Hidra), 26 nov: feita.** `HydraEntity` sem comportamento novo: o selo e o momento de
  fraqueza (×2) passaram pelo `PhaseGate` genérico (casca que só cai por estado ou reação + janela de vulnerabilidade),
  com a suíte verde. Proposta das afinidades da Hidra em `docs/PROPOSTA_HIDRA_AFINIDADES.md` (esperando aprovação,
  `docs/DECISIONS.md` item 2). Protótipo do **Guardião de Ruína** (`ruin_guardian`): só no Salão do Ritual das Ruínas
  Arcanas (50%), 60 HP fixos, casca de gelo que absorve tudo e só racha com Vapor (Molhar, depois Fogo), janela de 8 s
  com dano ×1,5, afinidade de gelo, recompensa modesta sem glifo original, tudo em `data/arcane/guardians/` e
  `affinities/ruin_guardian.json`. `/arcane guardian` (op) invoca um para teste. NÃO visto em jogo. 296/296.

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
  (marque `[x]`). Em 26 nov o Inverter foi cortado e os GameTests opcionais do furo saíram junto. Aliases de glifo prontos (`GlyphAliases`, `GlyphAliasesTest`). 259/259.

- **A (higiene), 25 nov: feita.** CLAUDE.md, este STATUS, log movido para CHANGELOG_DEV, documento antigo arquivado,
  descrição do Ativar corrigida, CI consertado (o `gradlew` estava sem permissão de execução: as 19 últimas execuções
  falharam com "Permission denied" antes de compilar). Crash do voo invisível e Foco instável corrigidos. 257/257.
