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

## 4. Bug corrigido nesta rodada
Um grimório flutuante sem dono (sobra de teste ou dado antigo) derrubava o login com `NullPointerException`. Agora o filtro verifica o dono e grimórios sem dono se removem sozinhos após 2 segundos.

## 5. Limitações conhecidas
- As animações são trocas de quadro por tempo (código), não `.mcmeta`; isso funciona em entidades e partículas, mas não em blocos.
- O braço do mago sobe com interpolação de 14 ticks; se quiser mais teatral, aumente `14` em `NPCMageEntity.startCastAnimation`.
- As capturas de verificação foram feitas num mundo de teste com muitos mobs; pode haver ruído visual.
