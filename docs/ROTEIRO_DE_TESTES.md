# Roteiro de testes em jogo (Fases 0 a 5)

Tudo abaixo passou nos testes automáticos, mas **quase nada foi visto em um cliente real**. Este roteiro serve para fechar essa lacuna. Marque `[x]` ao passar e anote o que estranhar no fim de cada bloco.

## 0. Preparação
- [ ] Mundo novo em **Criativo** (ou Sobrevivência com cheats), Superplano facilita ver blocos e círculos.
- [ ] `/arcane know all` (aprende os 143 glifos). Teclas padrão: **V** conjura, **B** troca a magia ativa, **G** abre o Grimório, **R** roda de magias.
- [ ] `/arcane mana set @s 1000` e `/arcane mana max @s 1000` para não esbarrar em mana.
- [ ] Ligue o cheat de tempo/clima se precisar: `/weather clear`, `/gamerule doDaylightCycle false`.
- [ ] Para montar feitiços: Bancada Arcana (ou caneta + tinta). Fórmula de todo feitiço: **forma** primeiro (`Eu`, `Projétil`, `Raio`...), depois efeitos e modificadores.

## 1. Fase 0: cristal, cicatrizes, zonas, convidados
- [ ] **Cristal de mana:** coloque um e chegue perto. Surgem corações azuis (potência > 1)? O veneno de mana machuca mais quanto mais perto? Longe do cristal tudo volta ao normal?
- [ ] **Magia aleatória perto do cristal:** com magias no grimório, perto do centro uma magia sua dispara sozinha de vez em quando?
- [ ] **Cicatrizes:** conjure um Raio com Dano em um bloco. Aparece um resíduo no mundo? Feche e reabra o mundo: ele continua?
- [ ] **Zona:** `Eu` + `Zona` + um efeito (ex.: Cura). Saia e volte: a zona continua? Feche/reabra o mundo com a zona ativa.
- [ ] **Ao Entrar / Ao Sair da Zona:** dispara uma vez em cada transição, não a cada tick?
- [ ] **Forasteiros e convidados:** `/arcane guest add <jogador>`, `list`, `remove`. A magia com filtro Forasteiros poupa você e seus convidados?

## 2. Fase 1: usabilidade do Grimório (a interface toda é inédita)
- [ ] **Grimório (G):** abre? A página mostra busca, filtros, favoritos e pastas sem botões sobrepostos? (a textura é 256x200, o layout é o maior suspeito.)
- [ ] Busque um glifo, favorite uma magia, crie uma pasta e mova uma magia para ela.
- [ ] **Modos de conjuração** (no Grimório, por magia): toque, carregar, segurar, alternar. Teste cada um com V: carregar deve mostrar a barra e dar mais força quanto mais tempo; segurar repete enquanto segura e para ao soltar.
- [ ] **Roda radial (R):** mostra as magias; escolher troca a ativa? Troca de conjunto (até 4 conjuntos de 6) funciona?
- [ ] **Barra de magias** na tela (SpellBarOverlay) e as **opções** (`ArcaneOptionsScreen`): daltonismo, reduzir flashes.
- [ ] Feche e reabra o mundo: favoritas, pastas, conjuntos e modos persistem?

## 3. Fase 2: voo
- [ ] Magia `Eu` + `Flutuar` (com `Planar` para planagem). Em modo alternar, ligue e desligue.
- [ ] Custo de mana por tick drena? Acaba a mana: o voo para e você pousa sem dano na 1ª queda?
- [ ] A inclinação do corpo ao planar parece natural? A velocidade chega a ser rápida demais (kick "moved too quickly" em servidor dedicado)?
- [ ] **Servidor dedicado com cliente conectado** (nunca testado): suba um servidor e entre por outro cliente para ver se o voo não expulsa você.

## 4. Fase 3: glifos de custo baixo (20)
Para cada um, monte `Eu` ou `Projétil` + o glifo e observe:
- [ ] **Chuva** molha/apaga fogo/rega plantação (não confundir com o ritual Clima). **Molhar**: depois de molhado, Raio fere mais, Gelo congela mais e Fogo não pega.
- [ ] **Investida** (distância e sensação), **Planar**, **Provocar**, **Medo**, **Sono** (em um mob com IA real: ele foge/dorme de verdade?), **Tremor**.
- [ ] **Roubar Vida / Transferir Vida / Ler Item** (o texto sai legível? Ler item de criatura e item no chão).
- [ ] **Inverter** antes de Dano, Fogo, Lentidão: o efeito inverte (Dano vira Cura, Fogo vira Congelar...)?
- [ ] **Gatilhos:** Ao Matar, Ao Cair (queda de 3+ blocos), Ao Pular armam a magia e disparam **uma vez**?
- [ ] **Condições:** Vida do Alvo abaixo de X, Bioma/Dimensão, Segurando Item, Chuva.

## 5. Fase 4: glifos médios (30)
Movimento e utilidade (todos inéditos com cliente real):
- [ ] **Gancho** (você é puxado até o alvo), **Escalar** (anda contra a parede e sobe; agache para parar), **Atravessar** (parede de até 6 blocos), **Retorno** (1º uso marca, 2º volta), **Troca** (troca de lugar com o alvo).
- [ ] **Sentir** e **Visão Arcana** (partículas só para você), **Revelar** (tira invisibilidade), **Emitir Sinal** (bloco de redstone que some sozinho).
- [ ] **Chamariz** (mobs reais mudam de alvo?) e **Reanimar** (mob morto há pouco volta como aliado com metade da vida).
Combate e controle:
- [ ] **Satélites** e **Rastro** (ferem quem encosta, nunca o dono), **Elo**, **Encantar**, **Petrificar**, **Ácido**, **Eletrificar/Condutor**, **Contágio**, **Maldição**, **Refletir** (projétil volta), **Silêncio** (alvo não conjura), **Dissipar**, **Drenar Mana** (só jogadores), **Vincular**, **Troca**.
- [ ] **Modificadores:** Furtivo (sem som nem partículas), Falso Rastro, Sobrecarga (dobra a força e cobra dano seu, nunca letal).
- [ ] **Após N Acertos** e **Ativar por Sinal** (em inscrição no mundo: sinal de redstone dispara na subida; o bloco de sinal tem visual correto?).

## 6. Fase 5 (novo)
**Bolso Arcano**
- [ ] `Eu` + `Bolso Arcano`: abre um menu de baú de 2 fileiras. Guarde itens, feche, reabra.
- [ ] Os **5 últimos espaços** estão travados (não aceitam item nem shift-click)?
- [ ] Recusa **shulker box, Grimório, páginas e bundle**.
- [ ] **Morra** (`/kill`) e reabra: os itens continuam?
- [ ] Saia do mundo e volte: persiste? Dois jogadores: cada um vê o seu?
- [ ] Leve dano de um mob e tente abrir logo em seguida (deve recusar por ~3 s).
- [ ] Admin: `/arcane pocket <jogador>`, `... list`, `... clear`.

**Restaurar**
- [ ] Em terra/pedra: `Eu` + `Explosão Destrutiva` (ou Projétil) e veja o buraco **sem itens no chão**.
- [ ] `Eu` + `Restaurar` (raio 2 a 12) perto do buraco: tudo volta? O efeito visual (partículas) aparece? A mana cai por bloco?
- [ ] Construa algo dentro do buraco e restaure: sua construção deve ser preservada.
- [ ] **Escavar** não é restaurado e continua soltando item.
- [ ] Explodir um baú com itens: itens caem, o baú volta **vazio**.
- [ ] Passados 2 minutos, o Restaurar não recompõe mais. Reinicie o mundo: o diário zera (esperado).
- [ ] Explosão **grande** (raio 8 com cristal): o servidor engasga ao restaurar centenas de blocos?
- [ ] Config `blastDropsLoot = true`: a explosão volta a dropar e o Restaurar não recompõe.

**Coro** (precisa de **2 clientes**: um servidor aberto à LAN ou um amigo)
- [ ] Os dois com a mesma magia `Eu` + `Cura` + `Coro` equipada.
- [ ] Jogador A conjura: surge um círculo de raio 8 no chão e uma mensagem. Nada acontece ainda.
- [ ] Jogador B conjura em até 3 s, dentro do círculo: no fim da janela a cura dispara, mais forte, e a mana dos dois cai pela metade do custo cada.
- [ ] Só um conjura: nada dispara, ninguém paga.
- [ ] B fora do círculo (ou conjurando tarde, depois dos 3 s): não entra.
- [ ] B com outra magia: não entra.
- [ ] A janela de 3 s é confortável com a sua latência? Ajuste `chorusWindowTicks`.

## 7. Geral
- [ ] Log do cliente/servidor sem erros ao longo da sessão.
- [ ] Desempenho: FPS e TPS estáveis com Satélites, Rastro, Chuva e Restaurar grande ao mesmo tempo.
- [ ] Ícones novos dos glifos legíveis no Grimório (muitos foram gerados por script).
- [ ] Idiomas: troque entre Português e Inglês e confira que não aparecem chaves cruas (`glyph.arcane...`).

## Como reportar
Para cada falha, anote: glifo/sistema, o que você montou, o que esperava e o que aconteceu (ou o trecho do log). Com isso eu reproduzo, escrevo o GameTest que falha e corrijo.
