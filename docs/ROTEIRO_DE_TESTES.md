# Roteiro de testes em jogo (Fases 0 a 5), versão detalhada

Quase tudo abaixo passou em teste automático (JUnit e GameTests), mas **quase nada foi visto num cliente real**. Cada item diz como montar, o que fazer, o que esperar e o que conta como falha. Marque `[x]` ao passar e anote o que estranhar.

**Sobre os números:** os valores entre parênteses são os padrões do glifo (você pode mudá-los com a roda do mouse na Bancada). Valores de balanceamento são **provisórios**: se algo parecer forte ou fraco demais, anote em vez de tratar como bug.

---

## 0. Preparação

### 0.1 Entrar no jogo
- [ ] O cliente já foi aberto (`gradlew runClient`). Crie um mundo **Superplano**, modo **Criativo**, com cheats ligados.
- [ ] Abra o menu `Mods` e confirme que o **Arcane** está carregado.

### 0.2 Comandos de apoio (cole no chat)
```
/arcane know all
/arcane mana max @s 1000
/arcane mana set @s 1000
/gamerule doDaylightCycle false
/gamerule doMobSpawning false
/time set day
/weather clear
/difficulty normal
/give @s arcane:arcane_workbench
/give @s arcane:arcane_pen
/give @s arcane:common_ink
/give @s arcane:mana_crystal
/give @s minecraft:iron_sword
/give @s minecraft:shulker_box
```
- Criativo não leva dano de magia: para testar cura, roubo de vida, Medo em jogadores etc., use `/gamemode survival` e `/damage @s 10` para se ferir.
- Mobs de teste (parados, sem IA, não atrapalham): `/summon pig ~ ~ ~5 {NoAI:1b}`, `/summon zombie ~ ~ ~6 {NoAI:1b}`.
- Mobs com IA (para Medo, Sono, Provocar, Chamariz, Reanimar): `/summon zombie ~ ~ ~8` (de dia queima: use `/time set midnight` ou `{Silent:0b}` com sombra; se queimar, ponha um capacete).
- Limpar mobs: `/kill @e[type=!player,type=!item]`.
- Log do jogo: pasta `run/logs/latest.log` do projeto (procure `ERROR` e `Exception` ao fim de cada bloco).

### 0.3 Como montar uma magia (Bancada)
1. Coloque a **Bancada Arcana** e clique com o botão direito.
2. Clique nos glifos da esquerda na ordem desejada. **A forma vem primeiro** (`Eu`, `Projétil`, `Raio`, `Aura`, `Onda`, `Pilar`, `Barreira`, `Toque`), depois filtros e efeitos, e os modificadores logo **depois** do efeito que modificam.
3. Digite um nome no campo do topo e clique em **Salvar**. Ela vira a magia ativa.
4. **V** conjura, **B** troca de magia, **G** abre o Grimório, **R** abre a roda.
5. Para ajustar um número (duração, raio...), passe o mouse na linha da composição e use a **roda do mouse** (Shift: passo 5 vezes maior).
6. Tetos: no máximo 10 glifos por magia (config `maxGlyphsPerSpell`) e o custo cresce a cada glifo extra. Com mana 1000 isso não atrapalha.
- [ ] A Bancada abre, mostra os 143 glifos conhecidos e **Salvar** grava sem erro.
- [ ] Nomes e descrições aparecem em português; troque o idioma para Inglês e confira (sem chaves cruas tipo `glyph.arcane.xyz`).

### 0.4 Magias-base para reaproveitar (salve estas primeiro)
| Nome | Composição |
|---|---|
| `Faísca` | Projétil, Dano |
| `Mira` | Raio, Dano |
| `Mira Sono` | Raio, Sono |
| `Eu Cura` | Eu, Cura |

---

## 1. Fase 0: cristal, cicatrizes, zonas, convidados

### 1.1 Cristal de mana
- **Montagem:** coloque um **Cristal de Mana** no chão. Fique em survival.
- **Fazer:** ande em direção a ele, anotando o que o HUD mostra (à direita da hotbar).
- **Esperado:**
  - [ ] Aparece **"Zona de mana ×N"** (N entre 2 e 32) e o número cresce ao chegar mais perto.
  - [ ] **"Mana Poisoning!"** aparece e a barra de mana fica **azul** quando a exposição já fere.
  - [ ] Dano de veneno de mana cresce com a zona; na zona 4+ vêm Fraqueza e Lentidão; na zona 5 o Definhar.
  - [ ] **Corações azuis** (potência > 1) enquanto está na zona.
  - [ ] Longe do cristal, tudo volta ao normal e os corações azuis somem.
- **Falha se:** nada do HUD aparece, ou o efeito não some ao sair.

### 1.2 Potência aumentada pelo cristal
- **Montagem:** magia `Aura, Empurrão` (raio 5).
- **Fazer:** conjure longe do cristal e depois dentro da zona, com porcos parados em círculo.
- **Esperado:**
  - [ ] Dentro da zona o raio/efeito é maior (até ×32 de área, limitado por `crystalAreaCap`; explosões têm teto absoluto de 40 blocos).
- **Falha se:** o efeito é igual dentro e fora.

### 1.3 Magia aleatória perto do cristal
- **Fazer:** fique parado na zona mais interna (zona 5) por ~1 minuto com algumas magias no grimório.
- **Esperado:**
  - [ ] De vez em quando **uma magia do seu grimório dispara sozinha**, de graça (até 30% por segundo no centro).
- **Falha se:** nunca dispara, ou dispara toda hora no limite da zona.

### 1.4 Cicatrizes (resíduos no mundo)
- **Montagem:** `Raio, Dano` e depois `Raio, Fogo` num bloco.
- **Fazer:** conjure e olhe o ponto de impacto. Feche o mundo e abra de novo.
- **Esperado:**
  - [ ] Resíduo visível no impacto (partículas/marca) por um tempo (padrão 20 min, `scarLifetimeTicks`).
  - [ ] Depois de salvar e sair, o resíduo ainda existe ao voltar.
- **Falha se:** o resíduo some ao reentrar (a cicatriz deveria ser salva).

### 1.5 Zona
- **Montagem:** `Eu, Zona (seconds 20), Cura`.
- **Fazer:** em survival, `/damage @s 10`, conjure e fique dentro do círculo.
- **Esperado:**
  - [ ] Você cura **a cada segundo** (intervalo 1) enquanto está na zona.
  - [ ] Ao sair do círculo você para de curar; ao voltar, volta a curar.
  - [ ] Ao fim de 20 s a zona acaba.
  - [ ] **Reinício do mundo:** conjure, saia do mundo com a zona ativa, volte: a zona continua enquanto você está presente.
- **Falha se:** não cura, cura fora, ou sobrevive ao fim do tempo.

### 1.6 Ao Entrar / Ao Sair da Zona
- **Montagem A:** `Eu, Zona (seconds 30), Ao Entrar na Zona, Velocidade`.
- **Montagem B:** `Eu, Zona (seconds 30), Ao Sair da Zona, Velocidade`.
- **Fazer:** conjure A, entre e saia do círculo várias vezes.
- **Esperado:**
  - [ ] Velocidade é aplicada **uma vez a cada entrada** (A) ou **uma vez a cada saída** (B), não a cada tick.
- **Falha se:** repete o efeito continuamente.

### 1.7 Convidados e Forasteiros
- **Montagem:** `Aura, Forasteiros, Dano` e porcos/zumbis ao redor.
- **Fazer:** `/arcane guest add <outro jogador>`, `/arcane guest list`, depois `/arcane guest remove <jogador>`.
- **Esperado:**
  - [ ] Mobs ao redor são feridos; **você e seus convidados** não.
  - [ ] Os animais domesticados seus também são poupados.
  - [ ] `list` mostra a contagem e os nomes; `remove` tira o jogador da lista (e persiste ao reentrar no mundo).
- **Falha se:** o dono é ferido ou o convidado é ferido.

---

## 2. Fase 1: usabilidade do Grimório (interface inédita, maior risco visual)

### 2.1 Abrir e navegar (G)
- [ ] O Grimório abre. A aba **Magias** mostra as magias salvas.
- [ ] **Busca:** digitar parte do nome filtra a lista na hora.
- [ ] **Filtros:** cada filtro muda a lista; limpar o filtro restaura.
- [ ] **Favoritar:** marcar uma estrela (ou botão equivalente); a favorita aparece no topo e persiste ao fechar e reabrir.
- [ ] **Pastas:** criar uma pasta, mover uma magia, renomear/apagar. A magia aparece só na pasta escolhida.
- [ ] **Layout:** nenhum botão **sobrepõe** outro, nenhum texto sai da moldura (a textura é 256×200; botões novos podem ter ficado sobrepostos).
- [ ] **Uso recente:** conjurar uma magia a move para o topo de "recentes".

### 2.2 Modos de conjuração (escolha o modo na página da magia)
Magia de teste: `Mira` (Raio, Dano) num porco parado.
- [ ] **Toque:** V uma vez = um disparo.
- [ ] **Carregar:** segure V; aparece uma **barra de carga**; solte para disparar. Carregar mais dá **mais dano** que carregar pouco (compare a vida que o porco perde).
- [ ] **Segurar:** segure V e a magia repete enquanto segura; **ao soltar, para na hora**.
- [ ] **Alternar:** V liga, V desliga (ótimo para voo, ver seção 3).
- [ ] Em modo Segurar/Carregar, a mana não vaza depois de soltar.

### 2.3 Roda (R), barra e conjuntos
- [ ] **R** abre a roda com as magias; clicar ou soltar escolhe a ativa e a barra de magias atualiza.
- [ ] **Conjuntos:** monte até 4 conjuntos de até 6 magias; trocar de conjunto muda a roda/barra.
- [ ] **Teclas 1-9** (barra de magias): acionam a magia da posição correspondente.
- [ ] **Barra de magias** (SpellBarOverlay): mostra a ativa destacada e atualiza ao trocar com B.

### 2.4 Opções (tela de opções do Arcane)
- [ ] Alternar: modo alternar em vez de segurar; paleta de daltonismo; reduzir flashes.
- [ ] **Reduzir flashes:** o clarão ao conjurar some ou enfraquece. (Raios e cutscenes de outros efeitos podem continuar piscando; é limitação conhecida.)
- [ ] Reiniciar o jogo: as opções persistem (são salvas no cliente).

### 2.5 Persistência
- [ ] Fechar e reabrir o mundo mantém **favoritas, pastas, conjuntos, modos e a magia ativa**.

---

## 3. Fase 2: voo (Flutuar e Planar)

- **Montagem:** `Eu, Flutuar (duration 200)`. Coloque a magia em modo **Alternar**.
- **Fazer e esperar:**
  - [ ] Conjurar: você **voa** (sobe com espaço, desce com agachar) e a mana cai **a cada tick** (veja pela barra).
  - [ ] Conjurar de novo (alternar): o voo **para** e você cai sem levar dano na **primeira** queda.
  - [ ] A **velocidade** (parâmetro `speed`, padrão 0,6) muda com a roda do mouse; no máximo 3 blocos/tick.
  - [ ] **Planagem:** ponha `glide` em 1 (roda do mouse na linha de Flutuar): o corpo **inclina** ao planar e a descida é lenta. Anote se a inclinação parece natural.
  - [ ] **Fim da mana:** `/arcane mana set @s 5` com o voo ligado: o voo termina sozinho, você pousa e leva dano **só na segunda** queda.
  - [ ] **Duração além do tempo:** em modo segurar/alternar o voo se sustenta além dos 10 s do parâmetro enquanto você mantém.
- **Servidor dedicado (nunca testado):** suba um servidor (`gradlew runServer`) e entre com um segundo cliente. Confira que voar **não expulsa** você (sem "Flying is not enabled" ou "moved too quickly").
- **Falha se:** você cai ao ligar o voo, o servidor expulsa, ou a mana não drena.

---

## 4. Fase 3: glifos de custo baixo (20)

Use mobs de teste com `NoAI:1b` salvo onde indicado.

### 4.1 Molhar, Chuva, Estrondo
- **Chuva** (`Raio, Chuva`): mire num ponto com um porco pegando fogo (`/summon pig ~ ~ ~5 {NoAI:1b,Fire:200s}`).
  - [ ] Cai chuva local (partículas), o fogo do porco **apaga**. Uma plantação de terra arada **seca** fica **hidratada** (mais escura).
  - [ ] O clima do mundo **não muda**.
- **Molhar** (`Raio, Molhar`): num porco.
  - [ ] O porco fica "molhado" e o fogo apaga.
  - [ ] Compare: `Raio, Fogo` num porco **molhado** (some em vapor, não pega fogo) e num **seco** (pega fogo).
  - [ ] Interação com Trovão (`Raio, Trovão`) e Congelar: molhado leva um pouco mais de dano e congela mais rápido.
- **Estrondo** (`Raio, Estrondo`):
  - [ ] Fere de leve (~2), **empurra** quem está no raio 3 e deixa zonzo por ~1,5 s.
  - [ ] **Nenhum bloco** quebra e **nenhum fogo** se forma.

### 4.2 Movimento e posição
- **Investida** (`Eu, Investida`, dist. 8, dano 3): mire num zumbi a 6 blocos.
  - [ ] Você avança em linha reta, **fere** quem está no caminho (~3) e para junto da parede se houver.
  - [ ] Diferença para Salto: você **anda** no caminho, não teletransporta.
- **Planar** (`Eu, Planar`), em survival: `/tp @s ~ ~40 ~` e conjure antes de cair.
  - [ ] Queda **lenta** e **sem dano** na aterrissagem. Passado o tempo (10 s) a queda volta ao normal.

### 4.3 Controle de mobs (precisa de IA real)
Use `/time set midnight` e `/summon zombie ~ ~ ~8` (com capacete se queimar).
- **Provocar** (`Eu, Provocar`):
  - [ ] Os hostis num raio passam a **perseguir você**, mesmo se perseguiam outro alvo. Não afeta outros jogadores.
- **Medo** (`Mira Medo`: Raio, Medo, 5 s):
  - [ ] O zumbi **foge de você** por ~5 s e **não escolhe você** como alvo; depois volta.
  - [ ] Em jogador: ele fica **Fraco** e com as mãos pesadas.
- **Sono** (`Raio, Sono`, 5 s):
  - [ ] O zumbi fica **parado e fraco**. **Qualquer dano acorda**. (Compare com Petrificar, que não acorda.)
- **Inverter + Medo**: `Raio, Medo, Inverter` vira **Provocar** no alvo.

### 4.4 Vida
Em survival: `/damage @s 10`.
- **Roubo de Vida** (`Raio, Roubo de Vida`) em um zumbi:
  - [ ] Você recupera ~60% do dano que realmente causou (dano 3: ~1,8). Em `Eu, Roubo de Vida` **não funciona**.
- **Transferir Vida** (`Raio, Transferir Vida`) num porco ferido:
  - [ ] O porco ganha vida (até 4) e você perde o mesmo, **nunca abaixo de 1 coração**.
- **Inverter** em Roubo de Vida vira Transferir e vice-versa.

### 4.5 Ler Item
- Zumbi com item: `/summon zombie ~ ~ ~5 {NoAI:1b,HandItems:[{id:"minecraft:iron_sword",Count:1b,tag:{Damage:50,Enchantments:[{id:"minecraft:sharpness",lvl:3s}]}},{}]}`
- [ ] `Raio, Ler Item`: o chat mostra **nome, quantidade, durabilidade e encantamentos**.
- [ ] Também funciona num **item solto** no chão e informa "nada para ler" se mirar no vazio.

### 4.6 Inverter (tabela de pares)
Monte `Eu, X, Inverter` para cada par e confirme o oposto:
- [ ] Dano → Cura (você ganha vida).
- [ ] Empurrão → Atrair.
- [ ] Lentidão → Velocidade.
- [ ] Fogo → Congelar.
- [ ] Levitação → Gravidade.
- [ ] Cegar → Invisibilidade.
- [ ] Efeito sem par (ex.: Marcar) **ignora** o Inverter sem erro.

### 4.7 Gatilhos (armam a magia; disparam uma vez; vencem em 60 s)
- **Ao Matar:** `Eu, Ao Matar, Cura`. Conjure e mate um mob com a espada.
  - [ ] Cura **uma vez** quando mata; matar de novo não cura (precisa rearmar). Sem matar em 60 s expira.
- **Ao Cair:** `Eu, Ao Cair, Estrondo`. Em survival, caia 5 blocos.
  - [ ] Dispara ao pousar. Cair só 2 blocos **não** dispara.
- **Ao Pular:** `Eu, Ao Pular, Velocidade`.
  - [ ] Ao pular você recebe Velocidade **uma vez**.

### 4.8 Condições de estado
- **Vida do Alvo Abaixo de X** (`Raio, Vida do Alvo Abaixo de X (30), Dano`): num porco com vida cheia **nada**; ferido a menos de 30% (3 de 10): dispara.
- **Bioma ou Dimensão** (`Eu, Bioma ou Dimensão (kind 5 Frio), Cura`): só funciona em bioma nevado; (kind 1 Superfície) vale no Overworld; (kind 2 Nether) não funciona no Overworld. Teste `/tp` para dois biomas.
- **Segurando Item X** (`Eu, Segurando Item X (kind 1 espada), Velocidade`): só com a espada na mão principal; mão vazia não dispara.
- **Chuva ou Tempestade** (`Eu, Chuva ou Tempestade, Cura`): `/weather clear` não dispara; `/weather rain` dispara; com `storm` 1 só com `/weather thunder`.
- **Escuridão** (`Eu, Escuridão`): efeito Escuridão do jogo em você, mais suave que Cegar.

---

## 5. Fase 4: glifos médios (30)

### 5.1 Movimento e utilidade
- **Gancho** (`Raio, Gancho`, força 1,2): mire num bloco distante e num mob.
  - [ ] Você é **puxado em linha reta** até o alvo; **sem dano de queda**. Não puxa o alvo (isso é Atrair).
- **Escalar** (`Eu, Escalar` 10 s): encoste numa parede alta e segure W ou pule.
  - [ ] Você **sobe** pela parede; **agachar** para; sem dano de queda. A sensação é a maior incerteza: anote lentidão ou tremor.
- **Atravessar** (`Eu, Atravessar` espessura 4): olhe para uma parede de 3 blocos.
  - [ ] Você sai do outro lado. Parede de 7 blocos ou sem espaço do outro lado: **nada acontece**.
- **Retorno** (`Eu, Retorno`): conjure uma vez (marca o lugar) e ande 30 blocos; conjure de novo.
  - [ ] Volta ao ponto marcado (mesma dimensão). Em outra dimensão **não volta**. A marca expira em 10 min.
- **Troca** (`Raio, Troca`): num mob.
  - [ ] Você e o mob **trocam de lugar**.
- **Sentir** (`Eu, Sentir`): com mobs atrás de uma parede.
  - [ ] Partículas **só para você** nas criaturas vivas e o chat diz quantas são e quantas hostis.
- **Visão Arcana** (`Eu, Visão Arcana`): perto de uma cicatriz e de uma inscrição.
  - [ ] Visão noturna e destaque de resíduos e inscrições (só para você) com contagem.
- **Revelar** (`Eu, Revelar`): com um mob invisível (`/effect give @e[type=zombie] invisibility 60`).
  - [ ] A invisibilidade some, ele **brilha por 5 s** e o chat conta os escondidos.
- **Emitir Sinal** (`Raio, Emitir Sinal`): junto a uma lâmpada de redstone ou pistão.
  - [ ] Surge um bloco de **Sinal Arcano** no ar que emite **redstone 15** e **some sozinho** (0,5 a 10 s); no máximo 4 por conjuração; só em espaço vazio. O visual deve ser o do "traço aceso".
- **Chamariz** (`Raio, Chamariz`) com zumbis perseguindo você:
  - [ ] Os mobs até 20 blocos **vão para o chamariz** e **deixam de te escolher** por até 15 s.

### 5.2 Combate e controle
- **Satélites** (`Eu, Satélites` 2 orbes): ao redor de você; chegue um mob perto.
  - [ ] Orbes giram e ferem quem encosta, **nunca o dono**.
- **Rastro** (`Eu, Rastro`): ande em linha reta com um mob seguindo.
  - [ ] Deixa brasas (até 24 pontos, 6 s cada) que **ferem e atrasam** quem pisa, nunca o dono.
- **Elo** (`Raio, Elo`) em um zumbi e depois em outro: dê dano em um.
  - [ ] Metade do dano **passa** para o outro por até 60 s. (Também vale `Eu, Elo` com um alvo: você e ele.)
- **Encantar** (`Eu, Encantar kind 1`): com espada na mão.
  - [ ] +2 de dano e fogo por **até 6 golpes** (ou 5 min); só corpo a corpo. kind 2 gelo, 3 raio, 4 veneno.
- **Petrificar** (`Raio, Petrificar`): num zumbi.
  - [ ] Fica imóvel, sem escolher alvos e leva **metade** do dano; **dano não quebra** (diferente do Sono). Dura até 15 s.
- **Ácido** (`Raio, Ácido`) em mob de armadura (`/summon zombie ~ ~ ~5 {NoAI:1b,ArmorItems:[{},{},{id:"minecraft:iron_chestplate",Count:1b},{}]}`):
  - [ ] Ignora armadura e **corrói** as peças (nunca quebra).
- **Eletrificar/Condutor** (`Raio, Eletrificar` mode 0): com 3 zumbis juntos.
  - [ ] O dano no alvo **salta 35%** para até 2 criaturas num raio de 4. Mode 1 (Condutor): `/summon lightning_bolt` perto: o raio **cai no alvo**.
- **Contágio** (`Raio, Veneno` em zumbi A; depois `Raio, Contágio` em A com zumbi B ao lado):
  - [ ] O Veneno passa para B (60% da duração). Sem efeito nocivo no alvo, **nada acontece**.
- **Maldição** (`Raio, Maldição`): no zumbi. 
  - [ ] Leva **25% a mais** de dano e **não cura** por até 2 min; `Eu, Dissipar` em si mesmo tira a maldição.
- **Refletir** (`Eu, Refletir`): com um esqueleto atirando em você.
  - [ ] Flechas **voltam** ao atirador e 40% do dano de perto volta ao agressor.
- **Silêncio** (`Raio, Silêncio`): em um mago NPC (ovo `arcane:npc_mage_spawn_egg`) ou outro jogador.
  - [ ] O alvo **não consegue conjurar** por até 30 s. Golpes comuns continuam. Quem está silenciado **não consegue nem se dissipar**.
- **Dissipar** (`Raio, Dissipar`): em um mob com efeitos benéficos ou Refletir.
  - [ ] Tira os efeitos; em você (`Eu, Dissipar`) tira Maldição, Silêncio, Petrificar, Medo, Sono e fogo.
- **Drenar Mana** (`Raio, Drenar Mana`): em outro jogador.
  - [ ] Tira até 20 de mana dele e passa para você. Mobs: nada.
- **Vincular** (`Raio, Lentidão, Vincular`): em um zumbi.
  - [ ] A Lentidão **acaba** se você morrer ou se afastar mais de 40 blocos. Efeitos que não são de poção ignoram o Vincular.
- **Reanimar Aliado** (`Raio, Reanimar Aliado`): perto de um mob que acabou de morrer (até 2 min).
  - [ ] Surge a criatura com **metade da vida** e luta por você; nunca ataca você nem seus aliados. Máximo 2 por vez. Não funciona em jogador nem chefe. Teste com IA real (nos testes automáticos os mobs não tinham IA).

### 5.3 Modificadores e condições
- **Furtivo** (`Raio, Dano, Furtivo`): 
  - [ ] **Sem som nem partículas** de conjuração; o resíduo no mundo dura **1/4** do tempo.
- **Falso Rastro** (`Raio, Dano, Falso Rastro`): com **Visão Arcana**.
  - [ ] O resíduo mostra assinatura que não é a sua e surge um **segundo resíduo isca** a até 10 blocos.
- **Sobrecarga** (`Eu, Cura, Sobrecarga`): em survival.
  - [ ] A cura é **o dobro** e você sofre **1 coração** de dano mágico (nunca fatal: só se tiver mais de 1,5 coração).
- **Após N Acertos** (`Eu, Após N Acertos (3), Velocidade`): conjure e bata em um mob 3 vezes.
  - [ ] Dispara **depois do 3.º golpe**, uma vez.
- **Ativar por Sinal** (inscrição no mundo: caneta + tinta num bloco: `Eu` + `Ativar por Sinal` + efeito):
  - [ ] Ao ligar uma alavanca/redstone no bloco, a magia dispara **na subida do sinal**, tendo como origem a criatura mais próxima (6 blocos). Sem criatura por perto, nada acontece.

---

## 6. Fase 5: Bolso Arcano, Restaurar, Coro

### 6.1 Bolso Arcano (`Eu, Bolso Arcano`)
- [ ] **Abrir:** aparece um menu de baú de **2 fileiras** com o título "Bolso Arcano".
- [ ] **Espaços travados:** só os **13 primeiros** aceitam item. Os 5 últimos não aceitam clique nem Shift+clique. (Anote se os 5 travados **parecem** utilizáveis: não há marca visual.)
- [ ] **Guardar e reabrir:** guarde itens, feche, abra de novo: tudo está lá, no mesmo espaço.
- [ ] **Recusas:** `shulker_box`, o **Grimório**, uma **Página Arcana** e o `minecraft:bundle` são recusados.
- [ ] **Morte:** `/kill @s` e reabra o Bolso: os itens **continuam**, e não caem no chão.
- [ ] **Salvar e sair:** feche o mundo e abra: o conteúdo persiste.
- [ ] **Combate:** em survival, deixe um zumbi bater em você e tente abrir **logo depois**: recusa com mensagem; ~3 s depois abre.
- [ ] **Dois jogadores:** cada um vê **só o seu**.
- [ ] **Admin:** `/arcane pocket <jogador>` abre o do outro; `.. list` conta itens e espaços; `.. clear` esvazia. Sem op: o comando não aparece/é negado.
- [ ] **Config:** `pocketEnabled=false` bloqueia a abertura com mensagem; `pocketSlots` muda a capacidade (1 a 18).

### 6.2 Restaurar
**Preparar:** um terreno plano de terra e pedra, 9×9. Magias: `Explosão Destrutiva` = `Eu, Explosão Destrutiva (radius 3)` e `Restaurar` = `Eu, Restaurar (radius 8)`.
- [ ] **Explosão sem loot:** conjure a Explosão Destrutiva. **O buraco abre e nenhum item cai** (nem terra nem pedra).
- [ ] **Restaurar:** conjure `Restaurar` perto: todo o buraco volta, igual ao que era (terra volta terra). A mana cai pelo custo do glifo (4) **mais ~0,3 por bloco** que voltou. Partículas brancas sobem.
- [ ] **Construção nova é preservada:** abra um buraco, construa blocos de madeira dentro dele e restaure: **a madeira fica** e só as posições vazias voltam.
- [ ] **Escavar:** `Raio, Escavar` num bloco: o bloco **solta item** e o Restaurar **não o devolve**.
- [ ] **Corte:** `Raio, Corte` em teia/folhas: quebra **sem drop** e o Restaurar devolve.
- [ ] **Gelo e fogo:** `Raio, Congelar` em água vira gelo; Restaurar devolve a água. `Raio, Fogo` em gelo/neve derrete; Restaurar devolve.
- [ ] **Baú:** coloque um baú com itens e exploda: **os itens caem**, e o Restaurar devolve o baú **vazio**.
- [ ] **Raio:** blocos além do raio escolhido **não** voltam; raio máximo 12.
- [ ] **Tempo:** espere mais de 2 min (ou `restoreWindowSeconds` baixo) e tente: **nada** volta.
- [ ] **Reinício:** exploda, feche e abra o mundo, restaure: **nada** volta (o diário é só em memória; esperado).
- [ ] **Qualquer jogador:** outro jogador restaura o buraco que você abriu.
- [ ] **Sem mana:** com `/arcane mana set @s 6` e muitos blocos no diário: volta **parte** e a mana termina perto de zero.
- [ ] **Explosão grande:** `Explosão Destrutiva (radius 8)` em terreno denso, depois Restaurar (raio 12): anote se o servidor **engasga**.
- [ ] **Config `blastDropsLoot=true`:** a explosão **dropa** itens e o Restaurar **não** recompõe esses blocos.

### 6.3 Coro (precisa de **2 clientes**; use o mundo aberto para a rede local ou um segundo launcher)
- **Magia (nos dois jogadores):** `Eu, Cura, Coro (min 2)` com o **mesmo nome e composição** (mesma magia equipada nos dois). Os dois em survival e feridos (`/damage @s 10`).
- [ ] **Abrir:** o jogador A conjura: aparece uma **mensagem** e um **círculo de partículas** de raio 8 no chão. Nada acontece ainda e a mana de A **não** cai.
- [ ] **Entrar:** B (dentro do círculo) conjura em até **3 s**: B recebe "você entrou no Coro (2 de 2)" e A vê a contagem.
- [ ] **Disparo:** ao fim da janela a magia dispara **uma vez** em A (a Cura é ~35% maior que a de uma conjuração normal, 4 → ~5,4) e as **duas manas caem pela metade do custo** cada.
- [ ] **Só um:** A conjura e B não: **nada acontece**, e a mana de A **não** cai ("o Coro não se completou").
- [ ] **Fora do círculo:** B a mais de 8 blocos: abre o **próprio** Coro, não entra no de A.
- [ ] **Tarde demais:** B conjura **depois** dos 3 s: não entra.
- [ ] **Magia diferente:** B com outra magia de Coro (valores diferentes): não entra.
- [ ] **Sem mana:** B gasta a mana antes do disparo: o Coro **falha**, ninguém paga.
- [ ] **Três ou mais:** `min 3` com 3 jogadores: força ×1,7. Com 5, ×2,4 (teto).
- [ ] **Latência:** a janela de 3 s é confortável? Anote o valor ideal (`chorusWindowTicks`).
- [ ] **Quem não entra:** NPC/mago não conjura Coro; carregar a magia (modo Carregar) é recusado ("não pode ser carregada").

---

## 7. Geral

- [ ] **Erros no log:** ao fim da sessão, `run/logs/latest.log` sem `Exception` nem `ERROR` ligados ao Arcane.
- [ ] **Desempenho:** com Satélites, Rastro, Chuva, Estrondo e Restaurar grande ligados ao mesmo tempo, o jogo mantém FPS e TPS (`/tick query` ou F3).
- [ ] **Ícones:** ao rolar o Grimório, os ícones novos dos 83 glifos das Fases 3 a 5 são **legíveis** e distintos entre si.
- [ ] **Loot:** `/loot` ou exploração de estruturas (cidade antiga, fortaleza) rendem **notas de glifo** novas.
- [ ] **Mago NPC:** o Mago NPC ainda conjura normalmente, e os glifos novos não quebram seu comportamento.
- [ ] **Morte do jogador:** o grimório cai, o provisório aparece e o Bolso **permanece**.
- [ ] **Servidor dedicado:** subir `gradlew runServer`, entrar com cliente, usar uma magia de cada fase.

## Como reportar uma falha
Anote: **ID do item** (por exemplo `4.3 Medo`), a **composição exata** da magia, o que esperava, o que aconteceu e, se houver, o trecho do log. Com isso eu reproduzo, escrevo o GameTest que falha e corrijo.
