# Arcane: estado atual, roteiro de testes e status das funcionalidades

Data: 6 nov 2026. Tudo aqui foi conferido no código e, onde indicado, em teste automático (73 testes unitários e 60 GameTests passando) ou em captura do jogo real.

---

## 1. O que existe no mod

### Núcleo do sistema de magia
- **81 glifos** em 6 categorias: 1 ativador, 11 condições, 13 filtros, 22 modificadores, 24 efeitos e 10 formas. Cada glifo tem tier (1 a 4), custo de mana, ícone 8x8 e nome em PT e EN. A lista completa está em `docs/GLIFOS.md`.
- **A ordem dos glifos importa.** O compilador monta um plano de execução a partir da sequência: forma, filtros, condições, efeitos, modificadores, grupos (`grupo` / `fim de grupo`), repetição e atraso entre segmentos.
- **Até 10 glifos por magia**, com custo de mana crescendo de forma exponencial (fator 1,3; os 3 primeiros são grátis de multiplicador).
- **Formas:** projétil, raio, aura, onda, pilar, barreira, invocação, entre outras.
- **Efeitos:** dano, cura, fogo, gelo, raio, explosão, teletransporte curto, escavar, marcar, gravidade, atrair, empurrar, clima, dobra de espaço, poções (veneno, lentidão, velocidade etc.).
- **Magias reativas** (armadas): disparam quando algo acontece (pisar, abrir baú, ser atingido).
- **Pena Arcana + tinta** gravam a magia em blocos, itens e baús. Tinta de sangue custa vida; tinta invisível não solta faíscas.

### Bancada e grimório
- **Bancada Arcana** com GUI própria: monte a sequência de glifos, dê nome, salve. Bancada = 100% de poder; em campo = 30%.
- **Grimório** (item e entidade): abas Magias, Glifos e Diário. Guarda magias salvas, glifos conhecidos e o registro de descobertas.
- **O grimório sempre cai ao morrer, com todo o conteúdo**, e só o dono o recupera. Sem ele há penalidade de mana por 3 dias ou até recuperá-lo.
- **Estágios do grimório** (por número de páginas): a capa muda do 1 ao 4; páginas arcanas aumentam a capacidade.
- **Mana**: barra no HUD, regeneração, zonas de mana (cristais), mana poisoning, armaduras que reduzem o envenenamento.

### Rituais
- **Altar Arcano**, **Giz Arcano** e **Bússola Arcana** (desenha o círculo gastando tinta).
- Círculos de raio 4, 8 e 16: poder ×1,5, ×3 e ×10, área ×1,5, ×2,5 e ×4. Mínimo de 85% de completude.
- Glifos que só funcionam em ritual, componentes físicos consumidos no altar, glifo Ativar, traços que acendem depois do ritual.

### Mundo e criaturas
- **Mago NPC** com 18 variantes (6 elementos × 3 estilos), mesmo motor de magia do jogador, spawn natural raro, ovo gerador, dropa nota de glifo.
- **Cristal de Mana** menor e maior, gerado no mundo, com partículas e animação.
- Descoberta de glifos: ver um mago conjurar, experimentar, achar nota de glifo, comando.

### Arte e animação (esta rodada)
- Partículas por elemento (fogo, gelo, raio, veneno, força, vazio, vida), cada uma com movimento próprio.
- **Raio que cai** (animação de 6 quadros) e **onda de choque** (6 quadros).
- **Projétil animado** (4 quadros) e **variante de tempestade** com arcos em zigue-zague, tingido pela cor do elemento.
- **Mago**: brilho animado nos olhos, mãos e peito na cor do elemento; braços sobem ao conjurar; balanço leve parado.
- **Grimório flutuante**: capa com brilho que varre e runas que pulsam (8 quadros por estágio), cresce e solta faíscas nos estágios altos.
- **Feixe 3D do Raio** (forma Raio): barra retangular sólida em 3D saindo da altura da mão (núcleo branco e casca na cor do elemento) que surge inteira e encolhe até sumir em 14 ticks. Um círculo arcano (anel girando e anel tracejado) abre no ponto de origem, que muda a cada disparo (sorteado em volta do conjurador), e o feixe sempre termina onde o jogador mira. Partículas do elemento saem da barra, do círculo e do impacto. Visto no jogo (laranja para fogo, amarelo para raio).
- **Grimório em 3D**: além do livro aberto, páginas soltas orbitam em volta (4 no estágio 1, até 10 no 4), com um anel de runas luminoso que gira; do estágio 3 em diante há um segundo anel inclinado. Vistas no jogo. Em primeira pessoa só o livro aparece, para as páginas não taparem a tela.
- Bancada com modelo 3D, altar com modelo, cristal animado, traços acesos, GUIs redesenhadas.

---

## 2. O que você pode testar e como

Pré-requisito: Minecraft com Forge 1.20.1 e `arcane-0.1.0.jar` em `mods` (já instalado). Em um mundo criativo.

| # | O que testar | Como |
|---|--------------|------|
| 1 | Aprender tudo | `/arcane know all` (ou `/arcane know arcane:fire` para um glifo) |
| 2 | Itens | Aba criativa do mod: bancada, grimório, penas, tintas, páginas, altar, giz, bússola, máscara, manto, ovo do mago |
| 3 | Grimório flutuando | Entre no mundo e aperte **F5**: o livro aberto flutua e gira ao seu lado |
| 4 | Abrir o grimório | Tecla **G** ou clique direito no item. Veja as abas Magias, Glifos, Diário |
| 5 | Montar uma magia | Coloque a Bancada, clique direito, clique nos glifos na ordem (ex.: Projétil, Dano, Fogo), dê nome, salve |
| 6 | Conjurar | Tecla **V** conjura a magia ativa, **B** troca a ativa. Observe a mana no HUD |
| 7 | Elementos visuais | Conjure Projétil+Dano+Fogo (laranja), +Gelo (azul), +Raio (amarelo, com raio caindo), +Veneno (verde) |
| 8 | Raio animado | Magia Raio/Projétil + Trovão: veja a animação do raio caindo no alvo |
| 9 | Choque | Projétil + Explodir: anel de choque no ponto da explosão |
| 10 | Mago | Use o ovo gerador; fique a distância e veja os braços subindo e o brilho colorido ao conjurar |
| 11 | Cristal e mana | Perto do Cristal de Mana a magia fica mais forte (HUD mostra "Mana zone xN"); muito tempo perto dá Mana Poisoning |
| 12 | Penalidade de morte | Morra: o grimório cai com tudo; pegue de volta e a magia volta |
| 13 | Pena | Pena na mão principal, tinta na outra, clique direito num bloco: grava a magia ativa; pisar dispara |
| 14 | Ritual | Coloque o Altar, trace o círculo com o Giz ou a Bússola, clique direito no altar com a magia ativa. Os traços acendem em ciano |
| 15 | Testes automáticos | `./gradlew test runGameTestServer` |

---

## 3. Status das funcionalidades

| Funcionalidade | Estado | Observação |
|----------------|--------|-----------|
| Grimório flutua ao lado do jogador | **Implementada e vista no jogo** | Captura F5 confirmou o livro aberto ao lado do jogador |
| Grimório com capa animada | Implementada | Compila e roda sem erro; o brilho é discreto e difícil de ver em captura parada |
| Grimório muda por estágio (1 a 4) | Implementada | Só a capa e o tamanho mudam; páginas em órbita e o estágio "só páginas / Julius" não existem |
| Grimório 3D (páginas e anéis em órbita) | Implementada e vista no jogo | Páginas orbitando e anéis de runas |
| Feixe 3D do Raio | Implementada e vista no jogo | Só visual; o dano continua no servidor |
| Estágio visual atualiza na hora ao usar Página Arcana | **Parcial** | A entidade só troca de estágio quando é recriada (relogar ou recuperar o grimório) |
| Equipar/guardar o grimório flutuante | Implementada (GameTest não cobre) | Agachado + botão direito com o grimório vazio mostra ou guarda; a escolha é salva no jogador. Slot de equipamento próprio não existe |
| Glifos de vários tipos | Implementada | 6 categorias, 81 glifos |
| Salvar magias no grimório | Implementada | Testada por GameTest e captura |
| Bancada com GUI | Implementada | Captura confirmou |
| Rituais (círculos, altar, componentes) | Implementada | GameTests de arena; captura confirmou traços acesos |
| Rituais lendários e círculos maiores que 16 | **Não implementados** | Dependem da sua decisão de regra (o Weather já é ritual lendário) |
| Juramentos (Quietude, Sangue, Luz) | Implementados e testados por GameTest | Dão +60/+70/+50% de força; a restrição e o custo são checados no servidor |
| Glifo Ao Morrer | Implementado e testado por GameTest | Arma a magia como Ao Sofrer Dano; dispara uma vez antes da morte |
| Assinatura arcana e cicatrizes (resíduos) | Implementadas e testadas por GameTest | Magias de custo 5+ deixam resíduo brilhante; só ficam em memória (somem ao fechar o servidor); configuráveis |
| Análise de resíduo | Implementada | Projetor de Glifos agachado: mostra elemento e assinatura e avança o conhecimento de um glifo. Não testada no jogo |
| Interações de elemento (chuva, pântano, gelo apaga fogo) | Implementadas, GameTest de gelo/fogo | Configuráveis; as de chuva/pântano não foram vistas no jogo |
| Efeitos visuais por forma (aura, onda, pilar, impacto, ritual, holograma) e flash de conjuração | Implementados; aura, ritual e feixe vistos no jogo | Onda, pilar, impacto, flash, holograma e livro-guia não foram vistos |
| Espelho de Simetria e Projetor de Glifos | Implementados; espelho testado por GameTest | Projetor (holograma) não testado no jogo |
| Livro-guia na primeira entrada | Implementado | Não testado no jogo |
| Estágios 5 a 7 do grimório | Implementados no código | Livro some a partir do 6; 7 ganha terceiro anel; não vistos no jogo |
| Aba de hipóteses/pesquisa (interface) | **Não implementada** | Só a análise de resíduos |
| Árvore de ramos paralelos | **Não implementada** | |
| Masmorras, estruturas próprias e mago chefe | **Não implementados** | Só o spawn de cristais e magos |
| Outras condições (Quando outro glifo terminar, Ao quebrar a barreira) | **Não implementadas** | |
| Qualidade da mana, Envenenamento por Mana completo, teste multijogador | **Não implementados** | |
| Animação do raio | Implementada e vista no jogo | Partícula pixel art de 6 quadros |
| Animação dos magos | Implementada e vista no jogo | Brilho animado visto; braços sobem pela metade na captura (pode precisar de ajuste fino) |
| Animação do projétil | Implementada | Código roda; não vi em captura parada |
| Animação de choque | Implementada | Vista em captura anterior com cor errada do selo; precisa de nova checagem |
| 18 skins de mago | Implementada | Geradas por código (não da internet, por licença) |

---

### Alvos tipados e prévia (modularização, passo 2)
- `TargetList` agora tem criaturas, blocos e **pontos vazios**. O Raio que erra e o centro do Pilar entregam um ponto; Fogo, Acender, Cura, Poções, Empurrar, Congelar, Raio e Explosão sabem agir nele (ver tabela em COMO_INSTALAR_E_USAR.md).
- `EffectReach` guarda o que cada forma entrega e cada efeito alcança; `SpellPreview` gera a descrição e os avisos, e os pacotes `C2SPreviewPacket`/`S2CPreviewPacket` (protocolo 8) levam isso à bancada.
- Testado: 81 GameTests (4 novos) e testes unitários passando. Não verificado no cliente: a aparência do tooltip da prévia.

### Zonas e barramento de eventos (modularização, passo 3)
- Glifos novos: **Zona** e **Quando a Zona Acabar** (83 glifos). `Zones` repete a forma do mesmo lugar (contexto congelado via `ExecutionContext.freeze`), com limites por conjurador e global; `SpellEvents` é o barramento de eventos por chave.
- Testado: 85 GameTests (4 novos: repetição, evento de fim, limite de 2 zonas, invocação + avisos da prévia) e testes unitários. Não testado em jogo.
- Falta: condições "Ao quebrar a barreira" e "Ao entrar na zona" (basta emitir a chave); `ArmedSpells` (Ao sofrer dano / Ao morrer) ainda não passa pelo barramento; zonas não persistem entre sessões.

### Tabela de interações, matriz e proteção (modularização, passo 4)
- `InteractionRules` lê `data/arcane/interactions/*.json` (regras: elemento + condições + dano/ação); `ElementInteractions` só aplica a força da config. Condições novas são uma entrada em `InteractionRules.CONDITIONS`.
- Custo único por tamanho (`SpellLimits.multiplier`) e por duração (`zoneMultiplier`), espelhado no cliente.
- Config `worldAlteringSpells` e `blockBreakingSpells` (`Targeting.mayAlterWorld` / `mayBreakBlocks`).
- Testado: 87 GameTests, incluindo a matriz (a cada rodada centenas de combinações) e a tabela carregada sem problemas; testes unitários da tabela e do custo. A matriz só garante que nada quebra: não prova que toda combinação tem um efeito visível (para isso a prévia avisa as inúteis).
- Os testes do Raio e da matriz rodam em lotes isolados: efeitos de área de testes vizinhos arremessavam a vaca do teste do Raio.

## 4. Bug corrigido nesta rodada
Um grimório flutuante sem dono (sobra de teste ou dado antigo) derrubava o login com `NullPointerException`. Agora o filtro verifica o dono e grimórios sem dono se removem sozinhos após 2 segundos.

## 5. Limitações conhecidas
- As animações são trocas de quadro por tempo (código), não `.mcmeta`; isso funciona em entidades e partículas, mas não em blocos.
- O braço do mago sobe com interpolação de 14 ticks; se quiser mais teatral, aumente `14` em `NPCMageEntity.startCastAnimation`.
- As capturas de verificação foram feitas num mundo de teste com muitos mobs; pode haver ruído visual.

### Imunidade do conjurador, Explosão Destrutiva e Cadeia (esta rodada)
- Imunidade: `SpellPipeline` tira o conjurador dos alvos de efeitos nocivos (exceto forma Eu); `SpellImmunity` poupa o dono de explosão, relâmpago e nuvem nociva (remoção do efeito um tick depois, pois o efeito só entra após o evento).
- Novos glifos: `blast` (efeito) e `chain` (modificador), total 85. Testados por GameTest (95 passando) e JUnit; não vistos ainda num cliente real.

### Rodada: potência pela escrita, caneta, círculo à mão, invocados, livro suave
- `PrecisionSnapshot.of(inscription)`: potência vem de `metadata.precision` (Bancada 1.0; campo 0.3), sem sobretaxa de mana. Pergaminho via caneta (30%); inscrição em bloco via interface de desenho (`ArcaneWorkbenchMenu` em modo caneta, `C2SSaveInscriptionPacket.penInscribe`).
- Círculo à mão: `RitualTemplate.HAND_RADII` (até 64), `RitualService.handInkCost`; avaliação de círculos >16 só confere os 4 pontos cardeais antes (não carrega chunks longe).
- Altar mantém a magia guardada e cobra mana por disparo.
- Visual: `RuneCircle` (círculo de runas), colunas descendo do céu (`ShapeFxRenderer`), livro com suavização no cliente (`GrimoireRenderer.follow`, dono sincronizado em `GrimoireEntity`).
- Invocados: `SpectralWolfEntity`, `ArcaneGolemEntity` (+ `SummonTier`), modelos em `SummonLayers` (gerado) e `SummonModels`, renderizadores em `SummonRenderers`.
- 99 GameTests + JUnit passando. NÃO verificado em tela real: aparência dos modelos/texturas, suavidade do livro, círculo de runas.

### Mana: base editável, glifos, poções
- `ManaState` (base + `glyphBonus` + `potionBonus`, `regenMultiplier`), `ManaBonuses.refresh` (a cada segundo e ao logar), efeitos `ModEffects.MANA_SURGE/MANA_VESSEL`, item `ManaPotionItem`, comando `/arcane mana`. GameTest `maxManaGrowsWithGlyphsPotionsAndTheCommandBase` (100 GameTests + 93 unitários passando). Não testado: beber a poção no jogo, ícones/texturas na tela.

### Rodada: grimório, escrita, ritual, voo, telecinese e câmaras
- **Escrita:** `ArcaneWorkbenchMenu` tem modos (BENCH 100%, BLOCK/ITEM com seletor de tinta, FIELD 30% via `C2SOpenWritePacket`); `saveToGrimoire` sempre cria magia nova (`uniqueName`). `C2SSaveInscriptionPacket` leva a tinta escolhida; `C2SRenameInscriptionPacket`; `ClientStatus` (linha de mensagens nas telas, via `ClientChatReceivedEvent.System`); `GrimoireState.removeInscription` seleciona a seguinte.
- **Alcance:** `ExecutionContext.areaFactor()` (Area x raiz do cristal, teto 3x), `coverRadius`, `setRitual(radius, center)`; `AreaCover.tile`; `Region.exposedBlocks`; `SpellLimits.parameterFactor`.
- **Ritual:** `CircleReport.symmetry/quality`, `RitualService.symmetry`, `drawPerfectCircle`, `PerfectCompassItem`. `RitualBonus` ganhou raio e centro; o cristal é lido no altar.
- **Glifos:** `FloatEffectHandler` + `ModEffects.FLOAT_FLIGHT` (voo no cliente em `FloatFlightClient`, inclinação no `RenderPlayerEvent`); `Telekinesis` + `TelekinesisEffectHandler` (usa `FallingBlockEntity` sem gravidade; tick em `TickDispatcher`).
- **Livro:** `GrimoireEntity.FLIP_UNTIL`/`flip()`, níveis 1/2/3+ no `GrimoireRenderer`, páginas infinitas acima da cabeça; `GrimoireItem.useContents` e `onItemPickup` voltam a invocar o livro. Desenhos na superfície: `InscriptionAnchor.face`, `S2CInscriptionDecalsPacket`, `InscriptionDecals`.
- **Câmaras:** `SealedChambers` (geração em `ChunkEvent.Load` com `isNewChunk`, criatura selada, soltura ao quebrar o altar), config `sealedChamberChance`, comando `/arcane chamber`.
- **Testes:** novos GameTests (`ritualAreaSpellsCoverTheWholeCircle`, `ritualExplosionsTileTheWholeCircle`, `symmetryAndPatternShapeRitualPower`, `auraReachesBlocksNotOnlyCreatures`, `telekinesisHoldsCreaturesAndBlockGroups`, `floatGlyphGrantsTheFlightEffect`, `sealedChamberHoldsACreatureUntilTheAltarBreaks`, `inscriptionRemembersTheSurfaceItWasDrawnOn`) e JUnit (`ParameterCostCurveTest`, `deletingTheActiveSpellSelectsTheOneThatCameAfter`).
- **NÃO verificado em tela real:** os menus novos (seletor de tinta, campo de renomear, linha de mensagens), a inclinação do voo, o halo das páginas infinitas, os desenhos nas superfícies, a aparência das câmaras e a geração natural (só a construção direta e o comando foram testados).

### Cristal e Fase 0 (nova branch `feat/glifos-fase0`)
- **Cristal:** `ExecutionContext.areaFactor()` agora usa o multiplicador cheio da zona (x2 a x32, teto `crystalAreaCap`); explosões têm teto absoluto (40 / 36 blocos) e Pilar/Congelar sobem o teto de raio com cristal. Mana Poisoning ficou mais forte (`(zona-2)*0,3`, dano x4, Fraqueza/Lentidão na zona 4+, Wither na 5; `crystalPoisonDamage`). Corações azuis no cliente enquanto a potência > 1. A cada segundo sorteia-se uma % até o teto da zona (`crystalWildCastMaxPercent`, padrão 30% na zona 5) e uma magia aleatória do grimório é lançada de graça (`ManaPoisoningService.wildCast`).
- **Fase 0:** cicatrizes (`WorldScars.ScarStore`, `arcane_scars`) e zonas (`Zones.Store`, `arcane_zones`) são salvas no mundo; zonas voltam quando o conjurador está presente e expiram se acabaram com o mundo fechado. Novos glifos (90 no total): **Ao Entrar na Zona** (`on_zone_enter`), **Ao Sair da Zona** (`on_zone_exit`), filtro **Forasteiros** (`outsiders`, com `GuestLists` salva e `/arcane guest add|remove|list`).
- **Testes (GameTests, passando):** `scarsSurviveASaveAndReload`, `zonesAreSavedAndComeBackAfterARestart`, `zoneEnterAndExitConditionsFireOnlyOnTransitions`, `outsidersFilterSparesTheOwnerGuestsAndTheirPets` + checagens de cristal em `crystalZoneDetectedAtAnyOffset`. Total: 112 GameTests e os JUnit.
- **NÃO verificado em jogo real:** reinício de verdade do servidor (só simulado por `reloadFromStore`), corações azuis, a sensação das magias aleatórias e o dano do veneno, o comando `/arcane guest` digitado, os ícones novos.

### Fase 1: usabilidade do grimório (branch `feat/glifos-fase0`)
- **Servidor:** `SpellPrefs` (modo, favorita, pasta, uso recente e 4 conjuntos de até 6, salvos no grimório e sincronizados), `CastChannels` (toque, carregar, segurar, alternar), `C2SGrimoireOrgPacket`, `C2SCastRequestPacket` com "soltou", `CastingService.cast(..., scale, free)`.
- **Cliente:** `ClientPrefs` (config do cliente), `ClientCasting` (teclas e modos), `RadialScreen`, `ArcaneOptionsScreen`, `SpellBarOverlay`, `GrimoireScreen` por página com busca, filtros, favoritos, pastas, modo e conjuntos.
- **Testes passando:** GameTests `holdModeRepeatsWhileHeldAndStopsOnRelease`, `chargeModeCastsStrongerTheLongerItCharges`, `toggleModeStartsAndStopsTheFlight`, `loadoutsFavoritesAndFoldersAreValidatedAndSynced` (com `ServerPlayer` real e canal embutido); JUnit `SpellPrefsTest`.
- **NÃO verificado em tela real:** toda a interface (roda radial, página do grimório com busca/filtros/botões, opções, faixa do conjunto, barra de carga), as teclas R/1-9/B/V soltas, o modo "alternar em vez de segurar", a paleta de daltonismo e o layout dos botões novos na textura 256x200 (pode haver sobreposição).
- **Fora desta entrega:** o modo de conjuração escolhido **na Bancada** (hoje escolhe-se no Grimório); "Reduzir flashes" só cobre o clarão de conjuração, não raios/cutscenes de outros efeitos; cores do HUD de mana não têm variante de daltonismo.

### Fase 2: Voo
- Reaproveitou o glifo Flutuar (regra "não duplicar"): `FlightService` (custo por tick, queda segura, codificação velocidade+planagem na amplificação), parâmetro `glide`, sustentação do voo em `CastChannels`, planagem em `FloatFlightClient`, config `flightManaPerTick`.
- **Testes passando:** GameTests `flightIsChanneledPaysManaEveryTickAndLandsSafely` (jogador real com canal embutido: custo por tick, sustenta além da duração, termina sem mana, devolve a permissão de voo, primeira queda sem dano e a segunda com), `flightSpeedAndGlideTravelInTheAmplifier`; JUnit `FlightServiceTest`. Total: 118 GameTests.
- **Servidor dedicado:** o movimento é do cliente e o servidor só concede `mayfly` durante o efeito (o que evita o kick "Flying is not enabled"); a velocidade máxima (3 blocos/tick) fica abaixo do limite de "moved too quickly". Os GameTests rodam num servidor sem cliente, mas **um servidor dedicado com cliente conectado NÃO foi testado**.
- **NÃO verificado em jogo real:** a sensação da planagem, a inclinação do corpo, o voo em rede (latência), o aviso de fim de mana.

### Fase 3: glifos de custo baixo (branch `feat/glifos-fase0`; 110 glifos no total)
- **Efeitos novos (implementados, testados só por GameTest, NÃO testados em jogo real):** Chuva (`rain`, chuva local: molha, apaga fogo, rega plantação; não é o ritual Clima), Investida (`charge`), Planar (`glide`, queda lenta), Provocar (`taunt`), Medo (`fear`), Sono (`sleep`), Estrondo (`rumble`), Escuridão (`darkness`), Molhar (`wet`), Roubo de Vida (`lifesteal`), Transferir Vida (`life_transfer`), Ler Item (`read_item`). Código em `execution/handlers/LowCostEffects.java`; Medo e Sono usam marcas lidas só por eventos (`MindStates`, sem custo por tick); Molhar usa `Wetness`.
- **Modificador Inverter (`invert`):** `Inversion` troca o efeito anterior pelo oposto na hora de executar (`SpellPipeline`). Pares: Dano/Cura, Empurrão/Atrair, Lentidão/Velocidade, Fogo/Congelar, Levitação/Gravidade, Roubo/Transferir Vida, Cegar/Invisibilidade, Medo/Provocar. Efeito sem par ignora o Inverter.
- **Molhar e as regras JSON:** "molhado" (glifo, água ou chuva) alimenta `data/arcane/interactions/default.json`: Raio/Trovão fere mais (+0,5), Gelo congela mais rápido e mais forte (`freeze_add`), Fogo nem pega e a criatura seca (ação nova `steam`). Trovão passou a somar esse dano a quem está no impacto.
- **Gatilhos novos (armam a magia, disparam uma vez):** Ao Matar (`on_kill`), Ao Cair (`on_fall`, queda de 3 blocos ou mais), Ao Pular (`on_jump`) em `ArmedSpells`.
- **Condições de estado novas:** Vida do Alvo Abaixo de X (`target_health`, `percent`), Bioma ou Dimensão (`region`, `kind` 1-8), Segurando Item X (`holding`, `kind` 0-7), Chuva ou Tempestade (`raining`, `storm`). Em `StateConditions`.
- **Testes passando:** 14 GameTests novos (total 132, todos passam), JUnit `Phase3GrammarTest` e extensão de `InteractionRulesTest`; contagem de glifos 110 nos testes de integridade.
- **NÃO verificado em jogo real:** a sensação e a distância da Investida, o visual das partículas, o comportamento de Medo/Sono em mobs com IA própria (nos testes os mobs não têm objetivos), o texto de Ler Item no chat, os ícones novos.
- **Fora desta fase (não feito):** sons próprios dos glifos novos (reaproveitei sons do jogo); Ler Item só lê criaturas e itens soltos, não baús.

### Fase 4: glifos médios (branch `feat/glifos-fase0`; 140 glifos no total)
- **Efeitos de movimento e utilidade (implementados, testados só por GameTest, NÃO em jogo real):** Gancho (`hook`), Escalar (`climb`), Atravessar (`phase`), Retorno (`recall`), Troca (`swap`), Sentir (`sense`), Visão Arcana (`arcane_sight`), Emitir Sinal (`emit_signal`, bloco novo `arcane_signal`), Chamariz (`decoy`), Revelar (`reveal`). Código em `execution/handlers/UtilityEffects.java`.
- **Efeitos de combate e controle:** Satélites (`satellites`), Elo (`link`), Rastro (`trail`), Encantar (`imbue`), Petrificar (`petrify`), Ácido (`acid`), Eletrificar/Condutor (`electrify`, mode 0/1), Contágio (`contagion`), Reanimar Aliado (`reanimate`), Dissipar (`dispel`), Refletir (`reflect`), Silêncio (`silence`), Drenar Mana (`drain_mana`), Maldição (`curse`). Código em `ControlEffects.java`; o que dura fica em `Phase4Service` (mapas só com o que está ativo; sem efeito ativo, custo zero) e os estados em `Phase4State` (lidos só por eventos de dano, cura, projétil e raio).
- **Modificadores:** Furtivo (`stealth`: resíduo dura 1/4 e conjurar não faz som nem partículas), Falso Rastro (`false_trace`: assinatura falsa e um resíduo isca), Sobrecarga (`overcharge`: dobra a força, +50% duração, custa 1 coração por efeito), Vincular (`bind`: efeitos de poção acabam se o conjurador morre ou se afasta 40 blocos).
- **Condições e cooperação:** Após N Acertos (`after_hits`, arma como Ao Sofrer Dano), Ativar por Sinal (`on_signal`: em inscrição no mundo vira o gatilho "sinal de redstone", na subida do sinal, origem = criatura mais perto) e Emitir Sinal.
- **Já existiam (não duplicados):** Telecinese, Persistente (= Duração/`lengthen`), Runa de Chão (= Zona + Ao Entrar na Zona).
- **Testes passando:** 28 GameTests novos (total 160, todos passam na última execução; ver flake abaixo), JUnit `Phase4GrammarTest`; contagem de glifos 140 nos testes de integridade.
- **Flake registrado:** `satellitesAndTrailHurtThoseWhoTouchThem` falhou uma vez ("o dono não é ferido") e passou nas execuções seguintes; a mensagem agora mostra a vida. Não investigado a fundo.
- **NÃO verificado em jogo real:** a escalada (depende da entrada do jogador no servidor), a sensação do Gancho e do Atravessar, o visual das partículas, Chamariz e Reanimar com IA de mobs reais (nos testes os mobs não têm objetivos), o bloco Sinal Arcano em tela (reaproveita o modelo do traço aceso), Ativar por Sinal escrito pela mesa (a mudança do gatilho no pacote de salvar não foi exercitada por teste), ícones novos.
- **Fora desta fase:** Silêncio não afeta golpes normais; Drenar Mana só age em jogadores; Vincular só em efeitos de poção.

### Fase 5: Bolso Arcano, Restaurar e Coro (branch `feat/glifos-fase0`; 143 glifos no total)
Desenho aprovado por Everton em `docs/FASE5_DESENHO.md` (decisões fechadas no próprio documento). Os três são glifos raros (tier 3).
- **Bolso Arcano (`pocket`, implementado, testado só por GameTest, NÃO em jogo real):** espaço por jogador, como um Baú do Fim (só o dono abre o seu), **13 espaços** (metade de um baú pequeno; menu de 2 fileiras com os 5 restantes travados; `pocketSlots`). Fica em `PlayerPersisted.arcane_pocket` e **permanece na morte** (`PocketEvents.copyBetween` copia no clone). Cada alteração grava na hora, no servidor. Recusa shulkers, grimório, páginas e a lista `pocketBlockedItems`; não abre em combate recente (3 s). Admin: `/arcane pocket <jogador>` (abre), `... list`, `... clear`. As pilhas guardam o espaço de cada uma (o `fromTag` do vanilla as compacta, por isso há leitura própria).
- **Restaurar (`restore`, implementado, testado só por GameTest, NÃO em jogo real):** `BlockJournal` em memória por dimensão (teto 4.096, janela 120 s, até 256 blocos por conjuração, raio 2 a 12). Entram Explosão Destrutiva/Cadeia, Cortar, Dano em blocos frágeis, Congelar (água e lava) e Fogo (gelo e neve); **Escavar não entra** e continua soltando item. Só recoloca se a posição ainda está como a magia a deixou (nunca sobrescreve construção nova); desfaz da mais nova para a mais antiga. **Mudança de gameplay aprovada:** essas magias deixaram de soltar loot (`blastDropsLoot`, padrão `false`; ligado, soltam loot e os blocos ficam fora do diário). Baús e fornalhas soltam o conteúdo e voltam vazios. Mana: custo do glifo + `restoreManaPerBlock` (0,3) por bloco que de fato voltou; sem mana para todos, volta só o que ela paga. Sem recarga. Qualquer jogador restaura estrago de qualquer autor.
- **Coro (`chorus`, modificador, implementado, testado só por GameTest, NÃO em jogo real nem com dois clientes):** o primeiro a conjurar abre uma sessão (`ChorusService`) de 3 s com um círculo de raio 8 desenhado por partículas; quem estiver dentro, com a mesma magia (mesma `fingerprint`), e conjurar na janela entra. No fim, com `min` (2 a 4) participantes, dispara uma vez com o iniciador como conjurador, força `1 + 0,35 x (n-1)` (teto x2,4) e a mana dividida (arredondada para cima). Sem gente suficiente (ou quem fica sem mana), nada acontece e ninguém paga. Sem pacotes de rede novos. NPCs e magias carregadas (mana já paga) não formam Coro.
- **Testes passando:** 21 GameTests novos (total 181, todos passam em 2 execuções seguidas), JUnit `JournalLogTest` e `ChorusMathTest`; contagem de glifos 143 nos testes de integridade. Os GameTests da Fase 5 usam um batch por teste, porque os testes do mesmo batch rodam em paralelo e dividem o diário e as sessões.
- **Flake registrado:** `holdModeRepeatsWhileHeldAndStopsOnRelease` (Fase 1) falhou uma vez ("não repete depois de soltar") e passou nas 3 execuções seguintes; é sensível a tempo, não investigado.
- **NÃO verificado em jogo real:** o menu do Bolso, o visual do círculo e das partículas do Coro, a janela de 3 s com latência real, o Coro com dois clientes, o Restaurar numa explosão grande (desempenho e visual), os 5 espaços travados do Bolso na interface.
- **Fora desta fase / limitações:** diário do Restaurar só em memória (some ao reiniciar o servidor); o Restaurar recoloca todos os blocos de uma vez (no máximo 256 por padrão), sem lotes; blocos de bloco-entidade voltam vazios e sem NBT; autor da destruição não é guardado (qualquer um restaura); o Coro não vale em inscrições do mundo nem em rituais; magos NPC não participam.

### Ajustes pós-Fase 5 e reforma visual (17 nov 2026, branch `feat/glifos-fase0`)
Pedidos de Everton depois do primeiro teste. Mecânica coberta por GameTest (185 passam); **visual e interface NÃO vistos em jogo real**.
- **Restaurar (testado por GameTest, não em jogo):** raio padrão 10, de 2 a 16 (`radius`); até 1024 blocos por conjuração (`restoreMaxBlocksPerCast`). Os blocos **voltam gradualmente** (`restoreBlocksPerTick`, 6 por tick), do mais novo para o mais antigo, com partículas e som; cada um é conferido de novo na hora de voltar (se alguém construiu ali no meio do caminho, fica). A mana é cobrada pelos blocos escolhidos no momento da conjuração. Teste novo: `restoreBringsBlocksBackGraduallyNotAllAtOnce`.
- **Carregar (testado por GameTest):** sem limite de tempo. A carga drena mana na mesma taxa de antes (1,5 x custo por 60 ticks) e **só para** quando a mana acaba ou a potência chega a `chargeMaxScale` (6); sem mana espera, não dispara sozinha. O servidor manda ao dono o estado (`S2CChannelPacket`) e o cliente desenha as **runas da base da forma** (feixe, pilar, aura, onda, eu), que crescem com a mana gasta. Teste novo: `chargeHasNoTimeLimitAndStopsOnlyWhenManaRunsOut`.
- **Segurar (testado por GameTest):** conjura **uma** vez (custo normal, animação, som) e mantém; o efeito é reaplicado a cada 10 ticks **em silêncio** (sem animação, som, clarão nem partículas de conjuração) e a mana drena aos poucos (`holdDrainPerSecond`, 0,6 x custo por segundo). Não é um efeito persistente "de verdade" (um objeto no mundo): é a mesma magia reaplicada a cada 10 ticks enquanto se segura, só que sem parecer uma nova conjuração. Teste novo: `holdCastsOnceThenOnlyDrainsSlowly`.
- **Voo, mana (testado por GameTest):** o servidor pulava a cobrança de mana de quem estava em **Criativo**, o que explica "não puxa mana"; agora cobra para todos (menos espectador), e o voo rápido (sprint) cobra 2,5x. Teste novo: `flightDrainsManaEvenInCreativeAndSprintCostsMore`. Cliente (**não visto em jogo**): sem animação de andar em voo, corpo alinhado ao olhar, e ao segurar sprint: onda de choque, FOV maior, rastro ciano.
- **Reforma visual ("arcano luminoso", NÃO vista em jogo):** `ArcaneTheme` (paleta e painéis), HUD de mana e barra de magias refeitos, roda com anéis, Grimório com textura escura (`tools/gen_art_vfx.py`) e textos reajustados para claro sobre escuro, círculo de runas de conjuração (`S2CShapeFxPacket.CAST`), halo no feixe e na aura, pose de canalização do jogador. A Bancada Arcana **não** foi mexida: já era escura. Pode haver texto ilegível ou sobreposição que só aparece na tela: ver seção 8 do `ROTEIRO_DE_TESTES.md`.
- **Fora do que foi feito:** os ícones dos 143 glifos e dos itens não foram refeitos; a animação do jogador ganhou só a pose de canalização e o voo (sem clipes novos de conjuração); as runas de carga só aparecem para o próprio jogador (outros jogadores não veem a carga).
- **Flake visto:** `emitSignalPowersAnAirBlockThenVanishes` ("sumiu sozinho") falhou uma vez numa execução e passou na seguinte, sem relação com estas mudanças; não investigado.
