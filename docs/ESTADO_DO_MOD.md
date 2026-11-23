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

### Foco, desempenho e polimento (18 nov 2026)
- **Foco (`focus`, novo glifo de forma, tier 2, 144 glifos no total; testado por GameTest, NÃO em jogo):** entrega só a criatura viva sob a mira (padrão 24 blocos, de 4 a 48, não atravessa paredes). Sem criatura na mira a conjuração falha **antes de cobrar mana** (`message.arcane.focus.no_target`), para jogadores e criaturas. Feito para efeitos como Atrair, Marcar e Elo. Testes: `focusDeliversOnlyTheLivingCreatureUnderAimAndFailsWithoutSpendingMana` e `focusDoesNotSeeThroughWallsOrPickBlocks`. Não vale em rituais nem inscrições do mundo.
- **Desempenho do Restaurar (medido em GameTest):** com o diário cheio (4096) escolher os blocos leva bem menos de 100 ms e cada tick de recolocação bem menos de 25 ms (`restoreWithAFullJournalStaysCheapPerTick`). Não medido: FPS do cliente com os efeitos novos, nem TPS num servidor real.
- **Flake corrigido:** os testes do Restaurar às vezes falhavam porque o `ConfigValue.set` do teste de loot gravava o arquivo de config e o Forge o recarregava do disco (corrida). Agora o teste usa `BlockJournal.overrideLootForTests`, sem mexer no arquivo. 188 GameTests passam.
- **Servidor dedicado: ainda NÃO testado.** Só consegui a checagem estática: nenhuma classe do cliente (`net.minecraft.client`) é referenciada fora de `client/` e dos pacotes com `@OnlyIn`/`DistExecutor`. Subir de verdade (`gradlew runServer`) exige aceitar o EULA da Mojang (`eula.txt`), que não aceito por você.
- **Estilo de magia único por jogador:** só o desenho, em `docs/ESTILO_UNICO_DESENHO.md`, com 5 perguntas.

### Servidor dedicado, coluna e voo estilo Frieren (18 nov 2026)
- **Servidor dedicado (testado, EULA aceito por Everton):** subi `runServer` numa cópia isolada do projeto (porta 25599, offline). Resultado: o servidor **iniciou ("Done 23,7 s")**, carregou **144 glifos** e não houve nenhum erro de classe do cliente. Os avisos de config ("Incorrect key ... corrected from null") são normais na criação do arquivo. **Não testado:** entrar com um cliente no servidor (rede, voo sem "moved too quickly", menu do Bolso, Restaurar com latência). Os GameTests já rodam em ambiente de servidor dedicado.
- **Coluna (Pilar), GameTest, visual NÃO visto:** a região da coluna agora sobe até o limite de construção do mundo (antes 10 blocos), assentada no chão. **Não se manifesta no ar**: sem chão firme até 5 blocos abaixo do ponto mirado, falha com `message.arcane.pillar.needs_ground` **antes de cobrar mana** (rituais ficam de fora). No cliente a luz sobe até o céu e **some com a altura** (sem ponta visível); a prévia do Carregar também, e não aparece se mirar o ar. Testes: `pillarReachesTheSkyAndStrikesCreaturesFarAbove` e `pillarDoesNotManifestInMidAirAndCostsNothing`. A casca de blocos do formato Parede/Coluna continua limitada a 10 de altura.
- **Voo estilo Frieren (NÃO visto em jogo):** a cabeça fica sempre em pé, olhando adiante (cancela a inclinação do corpo). **Só ir para a frente deita o corpo** (até ~78 graus, mais no sprint); subir e descer **não deitam**; lados e para trás mantêm em pé (leve rolagem ou recuo) e mexem tronco, pernas e braços. O sinal da rolagem lateral e a intensidade dos membros foram escolhidos só pela matemática: **podem estar invertidos**, confirme em F5.


### Revisão da UI do Grimório, categoria Ritual e Domínio (18 nov 2026)
Pedidos de Everton. Mecânica nova coberta por GameTest (191 passam + JUnit); **interface NÃO vista em jogo real**.
- **Grimório (G) enxuto (implementado, não visto em jogo):** sumiram os botões Ativar, Fechar (Esc fecha), Favorita, os dois ✔ (nome e pasta) e a linha C1–C4. Agora: **estrela ☆/★ ao lado de cada magia da lista** (clique = favorita); **clicar na magia seleciona e ativa**; nome e pasta salvam com **Enter**; modo de conjuração é um botão de **uma letra** (T/C/S/A, dica com o nome) e os conjuntos são chips **1–4** (verde = a magia está no conjunto); apagar virou **✖** (um clique pede confirmação, mostra "?"). A busca perdeu o texto longo ("Buscar...").
- **Categoria Ritual (exibição):** glifos que exigem círculo/componentes (Invocar Lobos, Invocar Golem, Teleporte/Warp, Clima) aparecem agora como categoria **Ritual** (cor rosa) no Grimório, Bancada e lista, em vez de "Efeito". Só a **exibição** mudou: a gramática continua tratando-os como efeitos (ordem de glifos intacta).
- **Domínio (novo glifo `domain`, modificador, tier 3, GameTest `domainKeepsEffectsInsideUntilTheShellBreaks`):** use **Barreira + Domo (ou Esfera) + Domínio + um efeito** (ex.: Dano, Cura, Lentidão). Fecha a casca; tudo dentro recebe os efeitos a cada intervalo (0,5–5 s, padrão 1) por até 60 s (padrão 30). A casca **não se refaz** e **não some sozinha** enquanto o Domínio dura; quando perde ~10% dos blocos (alguém quebra) ou o tempo acaba, o Domínio termina, a casca restante se desfaz e dispara "Quando a zona acabar". Conta como Zona (máx. 2 por conjurador). Sem Zona/Domínio, Barreira + Domo + efeito aplica uma vez (ou use o glifo Zona para repetir, mas a barreira então some com a duração dela).
- Pendências: sem visual próprio do Domínio (só a casca de barreira e partículas), sem som de quebra.

### Quatro ciclos: sons, visuais, mobs e feeling (18 nov 2026)
Mecânica de mobs coberta por GameTest (193 passam + JUnit). **Sons, visuais e tremor de câmera: implementados, NÃO ouvidos/vistos em jogo.**
- **Sons novos (sintetizados por `tools/gen_sounds.py`, provisórios, trocáveis por .ogg de mesmo nome):** `charge_start` (runas abrindo ao começar a carregar), `charge_release` (disparo da carga; volume e tom variam com a potência), `domain_open`, `domain_break`, `flight_boost` (voo rápido, no lugar do trovão de tridente), `golem_slam`, `golem_hum`. Todos com legenda pt/en.
- **Visuais:** o Domínio abre com coluna de runas + círculo, solta faíscas pela casca a cada pulso e estilhaça com faíscas elétricas e impacto ao quebrar; disparar uma carga solta uma onda (aura) que cresce com a potência.
- **Mobs com ação própria (testado):** **Golem Rúnico** — cada golpe é um impacto em área (70% do dano em inimigos ao redor do alvo, com empurrão para cima, som e partículas), levanta uma **runa de guarda** (Resistência III por 5 s, recarga 15 s) quando abaixo de 60% de vida lutando, e zumbe parado. **Lobo Espectral** — **salta para as costas** do alvo a 6–20 blocos (recarga 5 s), a mordida **entorpece** (Lentidão II, 3 s) e não sofre dano de queda. Testes: `runicGolemStrikeSplashesNearbyEnemies`, `spectralWolfPouncesBehindADistantTarget`. Pendente: ouvir/ver o golem e o lobo em ação contra mobs reais (a IA vanilla escolhe os alvos).
- **Feeling (cliente, não visto):** `CameraShake` — tremor de câmera por "trauma" em impactos, abertura de ritual e disparo de carga forte, mais um tremor leve e crescente enquanto você carrega; some com atenuação por distância e é desligado pela opção "reduzir clarões".

## Revisão de toggle, área, custo e rituais (18 nov 2026)
Cobertos por GameTest (198 passam): toggle sobrevive a outra magia, regulador de área x dano, modelo de custo, ritual sem forma e o pacote do altar. **Telas, scroll, camada de pedra, raios do Salto/Atravessar e anel de área: implementados, NÃO vistos em jogo.**
- **Toggle:** só desliga ao apertar de novo ou quando a mana acaba; conjurar outra magia não o desliga (`CastChannels.TOGGLES`). Testado.
- **Segurar (Hold):** agora *mantém* a magia: efeitos limitados ao próximo pulso e a casca de Barreira/Domo é renovada enquanto segura e desfeita ao soltar. Não testado em jogo (só o comportamento antigo de repetir tem teste).
- **Carga amplia a área:** potência xN também multiplica a área (área e dano iguais). Testado.
- **Regulador de área:** Ctrl+scroll (ou scroll simples no modo Conjurador) com uma magia que tenha área: sobe a área e reduz o dano na proporção inversa (passos de 1,25x, de -4 a +8). Só vale para magias com área. Testado a lógica; o scroll em si não.
- **Rádio:** scroll com o radial aberto troca de conjunto; o botão "conjunto ativo" saiu do menu do Grimório.
- **Cristal x áreas:** o limite do cristal (`crystalAreaCap`) já valia na conta; Explosão e Rajada agora mostram um anel no raio real e uma mensagem na barra de ação (as partículas vanilla da explosão não crescem). Nos mundos de teste o cap já é 32.
- **Config na aba Mods:** o botão Config abre as Opções do Arcane (preferências + regras do mundo, estas só em mundo local).
- **Custo:** zona/domínio cobram por pulso e duração (até 60x), área maior cobra ~ razão^1,25 (teto 12x), invocações cobram por tamanho e quantidade. Golem não tem mais "quantidade".
- **Domínio além do domo:** o raio do efeito agora é limitado ao da casca.
- **Altar:** clicar no altar abre uma tela própria (não usa a magia ativa nem o Grimório): desenhe o ritual, veja a prévia (círculo, nível, mana, oferendas) e Realizar/Guardar. Ritual dispensa forma (vira Aura no altar). Testado pelo pacote; a tela não foi vista.
- **Efeitos novos:** Salto e Atravessar soltam raios elétricos; Petrificar dá o efeito marcador e o cliente desenha o modelo em pedra (`PetrifyLayer`). Não visto.

## Segunda revisão: cratera redonda, altar com oferendas, runas e Segurar (18 nov 2026)
Cobertos por GameTest (202 passam): cratera esférica, valor das oferendas e teto, ritual com oferendas pelo pacote, pilar mantido fixo e raio real. **Telas e visuais: implementados, NÃO vistos em jogo.**
- **Explosão redonda (testado a geometria):** a cratera das explosões destrutivas de magia deixou de ser a do vanilla (grade de raios, quadrada em raios grandes) e virou uma esfera (`RoundCrater`, raio até 24, borda levemente irregular, blocos duros cedem menos, obsidiana e rocha-mãe ficam). Vale para Explosão Destrutiva e Cadeia. A Explosão comum não quebra blocos, então não muda. Não visto em jogo.
- **GUI do altar refeita (não vista):** duas abas, **Ritual** (glifos, categoria com setas, números) e **Oferendas**, com o relatório numa coluna própria à direita (texto quebrado na largura), sem nada sobreposto.
- **Oferendas (testado a lógica):** até 3 itens do inventário, qualquer item, 1 unidade de cada. Cada item vale uma porcentagem (`OfferingValue`: precioso por tabela, o resto pela raridade, encantamentos somam). O valor soma ao do círculo (menor 150%, médio 300%, maior 1000%). Se as oferendas sozinhas passam de **300%** o ritual é **lendário** e o total **dobra** (700% vira 1400%). Limite **1500%** em `ritualMaxPercent` (config). O valor total vira o poder do ritual. A Estrela do Nether sozinha já é lendária (310%). Substitui o botão antigo da Estrela. Magia guardada no altar lembra o valor das oferendas.
- **Runas da carga = área real (não visto):** o servidor manda o raio real (carga, cristal e regulador contados) e as runas de Aura, Onda e Pilar têm esse tamanho. Onda: círculo de meio alcance, a meio caminho (aproximação do leque).
- **Segurar (não visto):** o pilar mantido fica fixo onde nasceu (mirar em outro lugar não o move; testado). Aura, Onda e Pilar mantidos não reiniciam a animação a cada pulso: o desenho abre uma vez, fica de pé e apaga ao soltar; o som toca só na primeira. As runas do canal não pulsam mais.

## Terceira revisão: tetos configuráveis, itens únicos e regras do mundo (18 nov 2026)
Lógica coberta por GameTest (203 passam). **Telas: implementadas, NÃO vistas em jogo.**
- **Todo teto é configurável e removível (testado o do ritual):** `ritualMaxPercent` (0 = sem limite), `crystalAreaCap` (0 = sem teto), `chargeMaxScale` (0 = sem teto) e `areaHardCaps` (desligado = sem raio máximo fixo nas formas e efeitos de área; antes eram 24 a 64 blocos fixos no código).
- **Itens únicos (testado):** cabeça de dragão, ovo de dragão e elitro (lista `uniqueOfferings`, configurável) como oferenda quebram o teto do ritual, se `uniqueOfferingsBreakCap` estiver ligado. A magia guardada no altar lembra isso. Cabeça de dragão passou a valer 250%.
- **Cristal e área:** os efeitos de área com teto fixo (Controle, Baixo custo, Utilidade) agora sobem o teto junto com o multiplicador do cristal. Raio e Toque medem alcance, não área, e não mudam.
- **Altar:** fechar a tela guarda o rascunho (glifos, números, oferendas, aba) por altar até sair do mundo.
- **Regras do mundo (testada a lógica; tela não vista):** saíram do grimório e da tela de configurações do mod (o menu Mods só existe fora do mundo, onde a config do mundo não está carregada). Agora são abertas dentro do mundo por **`/arcane rules`**, só para administradores (operador nível 2 ou dono do mundo local). O servidor revalida cada clique, aplica na hora e **salva sozinho** em `serverconfig/arcane-server.toml` (não há botão Salvar). Terreno, quebra de blocos, interações, cicatrizes, raio fixo, tetos (cristal, carga, ritual; "sem limite" é o último passo) e itens únicos.

## Quarta revisão: Raio sem alcance e Pilar no alto (18 nov 2026)
Cobertos por GameTest (206 passam). Não visto em jogo.
- **Raio quase infinito:** o alcance máximo do glifo (48) passou a valer "sem limite" e usa `rayRange` da config (padrão 512, até 4096). Abaixo de 48 o alcance escolhido continua valendo. O Toque segue curto.
- **Pilar no alto:** olhar para baixo de uma torre alta acha o chão (o alcance do Pilar também usa `rayRange`), e mirar numa criatura no céu faz a coluna descer ao chão logo abaixo dela e acertá-la. Olhar para o céu vazio continua sem coluna.
- **Teste:** o mundo de teste tem terreno natural acima da estrutura, então o teste de altura abre uma coluna de ar antes.

## Próxima fase (pedido de 18 nov 2026, ainda NÃO iniciada): desenhos em objetos
- Desenhar em comidas, armas, itens e outros objetos, com **canetas específicas por tipo** (comida, arma, item...) e **canetas específicas por bloco** (terra, pedra, obsidiana...). Modelos 3D das canetas.
- **Animação do compasso:** ao usar o compasso, a tinta e a caneta flutuam do inventário/mão; a caneta molha na tinta e só então começa a desenhar (não é instantâneo).
- A definir antes de codar: quais tipos de caneta existem, o que cada desenho faz em cada tipo de objeto, custo/receita, e como os modelos 3D serão feitos (sem editor externo: geração por código, como as outras peças).

### Decisões do Everton (19 nov 2026, design fechado, nada construído)
- **Efeito:** o glifo no objeto dispara a magia no uso natural dele: comida com Cura cura ao comer; espada com Detonar explode ao atacar. Um modelo só para todos os objetos.
- **Canetas (onde desenhar):** ar (caneta de fênix, cara), blocos por material, poções, redstone, criaturas (só por loot), armas, comidas, itens. 90% craftáveis e também achadas em loot.
- **Blocos por material:** terra/cascalho/areia = um grupo; pedra/tijolos/minérios e afins de pedra = outro; obsidiana, blocos de metal/diamante, bigorna = outro; neve, etc. Agrupamento configurável.
- **Desenho em bloco:** adapta o sistema atual (armadilha por condição, ex.: ao pisar). Cada caneta de bloco também pode dar propriedades. Magia **persistente** (não se desfaz ao ativar) exige um glifo modificador próprio: Persistência.
- **Capacidade:** bloco comum = 1 desenho por face (a face em que se desenha importa). Item pequeno (diamante) = 1 glifo; item grande (espada) = 2.
- **Tinta = propriedade, caneta = onde se pode desenhar.** Tinta comum dura 10 dias in-game, sangue 20, invisível 7. Nova **tinta permanente** (um pouco mais cara) nunca se desgasta; misturada a outra, a torna permanente (ex.: invisível permanente). Apagar: pó solvente no objeto/bloco. Não se sobrescreve. Cada glifo a mais deixa o objeto instável: pode quebrar a cada uso e a potência fica imprevisível.
- **Custo:** a tinta fica no inventário; cada glifo gasta uma quantia; cada pote tem quantidade finita.
- **Animação do compasso:** só visual (tinta e caneta flutuam, a caneta molha e desenha), duração proporcional ao tamanho do círculo, não interrompível, exceto no altar.
- **Modelos:** cada caneta com forma própria, método livre, acabamento polido.
- **Quem desenha:** qualquer objeto, inclusive de outros jogadores e criaturas (especial). Desenho visível a todos; tinta invisível esconde até do autor.
- **Corte jogável inicial:** caneta de arma, caneta de bloco (pedra) e animação do compasso. Tetos e tipos configuráveis.

## Coluna da gravidade (glitch roxo)

- Implementado, não testado no jogo: quando a magia tem elemento FORCE (Gravidade e afins: empurrar, atrair, levitar, flutuar, telecinese) e forma Coluna, `ShapeFxRenderer.glitchColumn` desenha a coluna semitransparente e arroxeada, com fatias que tremem de lado, piscam e têm buracos; o padrão desliza para baixo. Só efeito visual (cliente); a mecânica não mudou. Conferir no jogo: `pillar` + `gravity`.

### Fase de desenhos em objetos, entrega 1 (19 nov 2026)
- **Respostas da segunda rodada** (fechadas): o desenho se gasta ao usar, salvo com o glifo Persistência; a instabilidade vale só acima do limite de glifos do objeto (limite 2: o 3º já arrisca) e quebrar destrói o objeto; a caneta de ar cria um círculo flutuante que dispara a magia, com duração pela tinta e **opção de sem limite**; poção dispara ao beber, redstone ao sinal; desenho em criatura dispara (consentimento depende de honra); pó solvente craftável (açúcar + bolsa de tinta de lula, barato), apaga tudo de uma vez, inclusive tinta permanente; mistura de tinta na mesa de trabalho comum; glifo de bloco dispara por proximidade ou toque; grupos de bloco em JSON.
- **Compasso animado (verificado por GameTest, visual NÃO visto em jogo):** o desenho do círculo não é mais instantâneo. `CompassDrawing` põe os traços aos poucos, em varredura (`RitualService.sweepOrder`), com a duração `compassTicksPerBlock` × raio (padrão 12, mínimo 20 ticks; 0 = instantâneo). A tinta só é gasta ao terminar, não interrompe, e sair do jogo conclui o círculo na hora. O cliente (`CompassDrawFx`) mostra frasco e caneta flutuando da mão até o altar, a caneta mergulhando na tinta e depois passando exatamente pelo traço que o servidor está pondo.
- **Modelos 3D:** caneta e os três frascos de tinta passaram a ser modelos em caixas, gerados por `tools/gen_item_models.py` (paleta `draw_palette.png`). **Não vistos em jogo.**
- **Entrega 1 aprovada em jogo pelo Everton (19 nov 2026)** depois de refazer os modelos (caneta fina, frasco quadrado com rolha) e a animação (caminho contínuo: anéis e depois hastes; o traço só aparece depois da caneta). O caminho está em `CompassPath`, compartilhado por servidor e cliente.

### Entrega 2: tintas, solvente e canetas de bloco (19 nov 2026)
Cobertas por GameTest (213 passam) e JUnit. **Nada disso visto em jogo ainda** (modelos, receitas, interface).
- **Regra da tinta (Everton):** tinta permanente PURA nunca se desgasta e **não** sai com pó solvente (o jogador precisa tomar cuidado). Tinta comum/sangue/invisível **misturada** com permanente (item novo na mesa de trabalho, ex.: "Tinta Invisível Permanente") nunca se desgasta, mas **sai** com pó solvente. Comum dura 10 dias do jogo, sangue 20, invisível 7 (config `inkDaysCommon/Blood/Invisible`, 0 = nunca).
- **Pó solvente** (açúcar + bolsa de tinta de lula, barato): botão direito num bloco apaga todos os desenhos dele de uma vez, menos os de tinta permanente pura. A pena não apaga mais (agachada só avisa).
- **Um desenho por face** do bloco (antes era um por bloco): `WorldInscriptionIndex` guarda por posição e face. Reescrever a mesma face troca o desenho.
- **Canetas de bloco por material:** terra, pedra, metal (blocos de metal e gema, obsidiana, bigorna), neve e madeira, cada uma com forma própria. A lista de blocos de cada grupo é uma tag de dados (`data/arcane/tags/blocks/pen_<grupo>.json`), editável por datapack. A Pena Arcana comum não escreve mais em blocos (só itens, papel e círculo a mão). Receitas: Pena Arcana + o material.
- **Glifos novos:** Persistência (o desenho não some ao disparar; sem ele, uma inscrição no mundo some depois de disparar uma vez) e Ativar por Proximidade (dispara quando uma criatura chega a 2,5 blocos). Agora são 147 glifos.
- **Ainda não feito:** propriedades de bloco (ex.: pedra que reflete dano), tipos de tinta que mudam o efeito além de custo e duração, caneta de ar (círculo flutuante), canetas de poção, redstone, criatura, arma, comida e item, tinta pelo "pote com quantidade" (hoje cada glifo gasta 1 frasco), animação da caneta de bloco. Isso é a entrega 3.

### Entrega 3a: desenhos em objetos (19 nov 2026)
Cobertos por GameTest (217 passam). **Não visto em jogo** (interface, modelos, tooltip).
- **Desenho em comida, arma, poção e item:** cada objeto guarda uma lista de desenhos (cada um com a sua tinta e data). Disparam no uso natural: comida e poção ao consumir (quem consome conjura), arma a cada golpe direto, qualquer outro item ao usar (botão direito). O desenho se desfaz ao disparar, salvo com Persistência.
- **Capacidade:** item pequeno 1 desenho, grande (arma, ferramenta, armadura, escudo) 2. Passar do limite é permitido até `itemExtraDrawings` (padrão +2, 0 = não passa) e deixa o objeto **instável**: potência de 40% a 160% a cada uso e chance de quebrar (25% por desenho a mais, quebrar destrói o objeto).
- **Canetas:** Comida, Arma e Poção (escrevem só na sua classe, item na outra mão) e Redstone (blocos de redstone, lâmpadas, pistões, alavancas; é uma caneta de bloco). A Pena Arcana escreve em papel (pergaminho, como antes) e nos demais objetos. Receitas e modelos 3D próprios.
- **Tinta e solvente:** a tinta de cada desenho se desgasta como nos blocos; o pó solvente na mão principal com o item na outra apaga os desenhos (menos tinta permanente pura).
- **Limites conhecidos:** a arma só dispara em golpe corpo a corpo (arco, besta e tridente arremessado não); poção arremessada não dispara (só ao beber); armadura não tem gatilho próprio (usa o botão direito de item, que veste a peça, então na prática não dispara); o pote de tinta ainda gasta 1 frasco por desenho.
- **Entrega 3b (falta):** caneta de ar (círculo flutuante), desenho em criaturas, pote de tinta com quantidade, propriedades de bloco.

### Cratera sem teto escondido (22 nov 2026)
Cobertas por GameTest (219 passam). **Não visto em jogo com raio grande.**
- **Causa do buraco que não crescia:** `RoundCrater.MAX_RADIUS` fixo em 24 limitava a esfera de blocos removidos, em qualquer configuração. Os mobs morriam num raio maior porque o dano de explosão usa o raio real.
- **Agora:** com "Raio máximo fixo das áreas" ligado a cratera continua parando em 24. Desligado, vale `craterMaxRadius` (padrão 128, 0 = sem limite).
- **Fila de blocos** (`CraterQueue`): crateras grandes saem aos poucos (`craterBlocksPerTick`, padrão 10 mil por tick), em vez de travar o servidor. Cada bloco entra no diário do Restaurar. Raio 96 tem cerca de 3,7 milhões de blocos, então leva alguns segundos para abrir inteira.
- **Limites:** a varredura da esfera em si ainda roda de uma vez (raio 128 varre ~17 milhões de posições); o diário do Restaurar guarda um registro por bloco, então uma cratera enorme usa bastante memória. Com `blastDropsLoot` ligado o caminho do vanilla continua o antigo.

## Entrega 3b: potes com quantidade, Caneta de Fênix e Caneta de Criatura (2026-11-23)

- **Potes de tinta com quantidade**: cada pote tem `inkPotUnits` (24) unidades e cada glifo gasta `inkUnitsPerGlyph` (1). Pote usado pela metade guarda o resto (nbt `ink_units`); gasta-se antes os potes pela metade. Tooltip mostra `Tinta: x/24`. Sem tinta suficiente o desenho e recusado.
- **Caneta de Fênix** (craftavel: Pena + lagrima de ghast + 2 po de blaze + diamante): desenha no ar (bloco vazio na face clicada ou 3 blocos a frente), gatilho sempre por proximidade, dura o quanto a tinta durar; `airDrawingsNeverFade = true` na config tira o limite.
- **Caneta de Criatura** (so em baus): botao direito numa criatura (nao jogador) escreve nela; ate 2 desenhos; ela dispara a magia quando atacar; some depois de disparar, salvo Persistencia; tinta vence como nos outros.
- Correcao: desenhar em comida/pocao com o item na mao secundaria nao inicia mais o uso (consumia o item).
- Testes novos: unidades de tinta, criatura desenhada, caneta de ar. Passaram.
- **Honestidade**: a suite de GameTest tem 5 falhas (electrify, link, lifesteal, attract, swap) que tambem aparecem sem estas mudancas (em HEAD deram ate 9, variando). Suspeita: estado persistido em `run/world` (ex.: `arcane_zones.dat` com cristais de testes antigos); nao confirmado, nao apaguei o mundo. Falta testar a 3b no cliente (menus, modelos, renderizar o circulo de ar).
- Pendente: bloco com propriedades, e o item (c) magias longas com `after_seconds` (atrasos sao absolutos por segmento, nao acumulados).
