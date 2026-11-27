> ## CONGELAMENTO DE EDIÇÃO (pedido de Everton, 26 nov)
> **Nenhuma edição no jogo Arcane até sábado (28/11). Liberada apenas domingo (29/11).** Até lá vale só design e
> arquitetura. Nada abaixo foi aplicado ao código. Nenhuma sessão deve editar este projeto antes de domingo, mesmo
> que Everton autorize depois. As sessões `prompt-2` e `prompt-3` estão ociosas e não devem ser reativadas para editar.

# Perguntas e respostas de Everton (26 nov 2026)

Rodada de decisões depois dos 3 prompts de 25 e 26 nov. Cada item tem a pergunta (como foi feita) e a resposta de Everton.
Resposta **em branco** = Everton pediu explicação ou o arquivo antes de responder. **Nada disto foi aplicado ao jogo
ainda** (pedido dele: "NÃO EDITE NADA DO JOGO AINDA").

## 1. Travando trabalho

**1. Números do Corte.** (A) dano 6, +4% por ponto de armadura, custo 4; (B) dano 3, +5%, custo 3; (C) dano 6, ignora
metade da armadura, custo 4. Ligar o "alvo único" junto. (`DECISIONS.md` nº 3)
> **Resposta:** B, e o dano escala com a vida restante do alvo.

**2. Forma Chuva.** Aprovar o desenho e os números (raio 5, 6 s, 1 pulso/s)? Teto parcial pelo centro do topo ou por
qualquer canto? Dentro de casa: a nuvem desce até o teto ou falha? (`DECISIONS.md` nº 4, `PROPOSTA_FORMA_CHUVA.md`)
> **Resposta:** segue a recomendação (aprovar; centro do topo; dentro de casa a nuvem desce).

**3. Hidra com afinidades.** (a) nada; (b) fixa por estágio; (c) por cabeça viva. (`DECISIONS.md` nº 2)
> **Resposta:** C. E só se pode começar a enfrentá-la depois de enfrentar o primeiro Guardião: ao matá-lo, uma onda
> varre a partir dele, como uma onda de choque, com um texto ("O arcano do mundo foi desbalanceado"), e então os selos,
> que antes não podiam ser destruídos, agora podem.

**4. Assinatura da magia.** Vem da contagem dos elementos mais usados (como está), do ID do jogador, ou só depois do
sistema de estilo aprovado? (`DECISIONS.md` nº 1)
> **Resposta:**
> _(Everton pediu: "primeiramente, me explica o que seria o estilo")_

**5. Sprints de presença visual.** Autorizar o sprint P0 da auditoria gráfica? (`AUDITORIA_GRAFICA.md`)
> **Resposta:** autorizo.

## 2. Balanço e servidor

**6. Explosão** rende 10 a 13x a mediana de dano por mana. Custo cresce com o raio, ou o dano cai? (`BALANCO_AUTO.md`)
> **Resposta:** meio a meio (o custo sobe e o dano cai).

**7. Modificadores passam do teto da bancada** (Potência, Duração, Área, Alcance, Sobrecarga). Limitar a máximo × 2?
> **Resposta:** não. Isso é, de certa forma, desatualizado: por exemplo, colocar uma Potência aumenta MUITO o custo da
> magia, assim como Área.

**8. Custo pelo valor final** (depois dos modificadores, como já é com a área)?
> **Resposta:** sim.

**9. Enfraquecer** encarece em vez de baratear e não afeta a Explosão.
> **Resposta:** baratear, e criar outro glifo para raio (assim como tem um para área e outro para potência).

**10. Crateras sem teto: calcular aos poucos** (mesmo resultado no mundo)?
> **Resposta:** depende de como for afetar visualmente.

**11. Teto de raio 64 também no "Sem limites"?**
> **Resposta:** não. Sem limites é sem limites; cabe ao dono do servidor saber o que quer e o seu custo.

**12. Diário do Restaurar:** subir o teto de 4.096 para 32.768, ou aceitar que crateras grandes não voltam inteiras?
> **Resposta:** sim, mas não tem uma maneira de fazer isso de forma mais simples/barata? Por exemplo, guardar a geração do
> mapa original e fazer o Restaurar surgir a partir dali; assim não teria tanto limite, e o diário ficaria para o que o
> jogador construiu.

**13. Uma cratera grande por vez por dimensão** (as outras esperam na fila)?
> **Resposta:**
> _(Everton perguntou: "como assim?")_

## 3. Números e tabelas provisórios (Fases H a N)

**14. Tabela de afinidades dos mobs** (`data/arcane/affinities/`).
> **Resposta:**
> _(Everton pediu: "me mostre")_

**15. Números provisórios de H a N** (~40 linhas em `PERGUNTAS.md`, "Números provisórios"): aprovar em bloco ou revisar.
> **Resposta:**
> _(Everton pediu o caminho do arquivo)_

**16. Boneco de Treino:** receita (fardo de feno + 4 gravetos), 40 HP, corações (❤, ½, "♥×N" acima de 10).
> **Resposta:** aprovado.

**17. Modificadores de Projétil:** Perseguir, Ricochete e Arco no tier 2; Fragmentar no tier 3; cacos e Ricochete ao
bater numa criatura.
> **Resposta:** aprovado.

**18. Nuvem e afinidade:** a nuvem conta por efeito ("mesma conjuração"); o teto de afinidade só limita o bônus; o
elemento usado é o do `effects.json` (Dano e Explosão contam como arcano).
> **Resposta:**
> _(Everton: "não entendi")_

## 4. Escolhas do prompt 3

**19. Mais Próximo "logo após a forma"** ignora qualquer modificador no meio e inclui aliados e bichos.
> **Resposta:** o Mais Próximo logo após uma forma entrega aquela forma na entidade mais próxima de acordo com o filtro.
> Se o filtro vier antes, ela entrega apenas para o mais próximo que concordar com o filtro; se o filtro vier depois,
> entrega no mais próximo e só depois verifica se cabe. O filtro basicamente diz "tudo depois daqui só acontece se
> entrar nesse filtro".

**20. Toque sem alvo continua gastando mana** (só o Raio falha no vazio).
> **Resposta:** Toque sem alvo não gasta e não dispara, assim como Foco sem alvo.

**21. Privilégio do Toque:** ao tocar uma criatura, alcança também o bloco logo atrás dela.
> **Resposta:** não vejo necessidade. Em vez disso, faça dela uma magia sem rastro, que barateia um pouco a magia e
> amplifica um pouco o efeito (×1,3).

**22. Raio vazio com efeitos que não dependem de alvo** (Salto, Bolso, Clima) não falha.
> **Resposta:** aprovado.

**23. Atraso → Após Segundos:** magias antigas ficam 1 de mana mais baratas, e um Grupo aberto antes do Atraso deixa de
ser alcançado depois dele.
> **Resposta:**
> _(Everton pediu: "me explique melhor")_

**24. Carinho → Cura com 4 HP** (e Ferido removido, efeito Chuva → Molhar nas magias salvas).
> **Resposta:** mantenha o Carinho, mas fica só com o efeito dos corações.

## 5. Propostas de sistema

**25. Tags de estado** (`PROPOSTA_TAGS_DE_ESTADO.md`): implementar? Com ou sem reações novas? Quais estados sobrevivem
à morte?
> **Resposta:**
> _(Everton pediu o arquivo para ler)_

**26. Estilo único** (`ESTILO_UNICO_DESENHO.md`), 5 perguntas: uso ou pontos; muda ou é permanente; limite total;
lista de pares; vale para magos NPC.
> **Resposta:**
> _(Everton pediu o arquivo antes de responder)_

## 6. Escolhas antigas nunca confirmadas

**27. Zonas e convidados:** zona que vence com o mundo fechado some sem disparar "Quando a Zona Acabar"; "Ao Entrar na
Zona" não conta quem já estava dentro; lista de convidados por jogador.
> **Resposta:** como assim a zona pode vencer com o jogo fechado? Se o mundo está fechado, o tempo congela. Ao entrar na
> zona, realmente não conta quem já está dentro. Lista de convidados é por jogador. A zona também deve respeitar o
> formato: pode ser em coluna, domo etc.
> _(a primeira parte ainda espera a explicação)_

**28. Modo de conjuração só no Grimório**, não na bancada.
> **Resposta:** exatamente.

**29. Voo:** Flutuar serve como voo (sem glifo "Voo" separado), com queda segura por 60 s.
> **Resposta:**
> _(Everton: "como assim?")_

**30. Investida × Empurrão em si mesmo:** manter os dois?
> **Resposta:** mantém. Faça que o Empurrão possa ser dado para qualquer lado (funciona como dash); se o jogador estiver
> pressionando para frente ou nada, funciona como hoje (para onde mira). Investida com alcance menor e dá dano.

**31. Eletrificar e Condutor** num glifo só (`mode`), ou dois glifos?
> **Resposta:**
> _(Everton pediu: "me diga a diferença de cada")_

**32. Reanimar** (criatura recém-morta, não jogador), **Encantar** (temporário), **Sobrecarga** (custa 1 coração).
> **Resposta:** Reanimar pode trazer um jogador de volta 1 vez, pelo custo da metade da vida restante do conjurador, e
> precisa de uma Potência para funcionar. Sobrecarga aprovada.
> _(Encantar: Everton perguntou "o que Encantar faz?")_

**33. Vincular:** a frase nova do prompt 3 está certa?
> **Resposta:**
> _(Everton: "qual?")_

**34. Bolso** 13 espaços; **Bolso e Coro** tier 3; **Coro** janela de 3 s, raio 8.
> **Resposta:** aprovado.

**35. Lendário** pelo valor das oferendas (> 300%), sem exigir círculo maior.
> **Resposta:** exige no mínimo círculo de raio 16.

**36. Grimório:** penalidade "até recuperar ou 3 dias de jogo"; destruído sem backup; começa com "Faísca" ou vazio?
> **Resposta:** o livro começa com a Faísca; penalidade aprovada. Grimórios com dono não podem ser destruídos de maneira
> nenhuma e nem somem (despawn): em vez de ser item dropado como os outros, ele é sempre um item/entidade 3D e, quando
> dropado, vira uma entidade parada. `/kill` não pode matá-lo.

**37. Custo exponencial** ×1,3 por glifo depois do 3º; 8 páginas iniciais.
> **Resposta:** aprovado.

**38. Decisões de 6 nov** (`TRABALHO_MANUAL.md`): receitas e loot; drop do Cristal; defesa da máscara e do manto;
tecla B; inscrição por pisar de graça; a Pena escreve em qualquer bloco; composição inválida conta como experimento.
> **Resposta:**
> _(Everton pediu: "me explique melhor")_

**39. Brecha de descoberta:** o cliente consegue "aprender" qualquer glifo mandando experimentos.
> **Resposta:**
> _(Everton pediu: "explique melhor")_

**40. Hidra pegando fogo fica vermelha o tempo todo.** Incomoda?
> **Resposta:** para mim, não.

---

## Rodada 2 (26 nov, tarde): respostas de Everton

Legenda: **Aprovado** = pode ir para implementação (domingo). **Dúvida** = falta uma resposta antes. **Explicado** = pedido de explicação, já respondido aqui.

### 1. Corte: aprovado B, com escala pela vida restante
Corte = **B** (dano 3, +5% por ponto de armadura, custo 3), alvo único, sem área e sem cadeia. Acréscimo de Everton: o dano **escala com a vida restante do alvo**. **Aprovado.** O número da escala fica provisório na implementação.

### 2. Forma Chuva: aprovado (recomendação)
Aprovada a forma Chuva com raio 5, 6 s e 1 pulso/s. O teto parcial conta pelo **centro do topo**. Dentro de casa, a nuvem **desce até o teto**. **Aprovado.**

### 3. Hidra: aprovado opção C, com portão do Guardião
- **Afinidade por cabeça viva** (opção C). Aprovado.
- **Portão:** só dá para enfrentar a Hidra depois de vencer o **primeiro Guardião de Ruína**.
- **Onda ao matar o Guardião:** uma onda de choque sai dele, com o texto "O arcano do mundo foi desbalanceado".
- **Selos:** antes indestrutíveis; depois dessa onda, **podem ser destruídos**.

Isso muda o comportamento da Hidra (que antes não podia ser ferida nos selos). Está aprovado por Everton, então entra em `DECISIONS.md` como decisão aprovada, a aplicar no domingo.

### 4. Estilo de magia (explicado e aprovado em princípio)

**O que é.** Cada jogador acumula afinidade com cada elemento conforme **usa** magias desse elemento. O estilo é formado pelos elementos em que ele está mais forte. Não é uma barra, nem uma tela, nem uma conquista: o jogador não é avisado de nada. O efeito aparece só no que ele conjura.

**Regras aprovadas (respostas às 5 perguntas de `ESTILO_UNICO_DESENHO.md`):**
- **Afinidade por uso** (Q1): sobe ao conjurar magias daquele elemento.
- **Não cai nunca** (Q2): se chegou a 100, fica 100. O jogador não é penalizado por testar coisas novas.
- **Só o efeito muda** quando outro elemento passa do atual. A afinidade em si nunca cai.
- **Parâmetros separados**, e **cada elemento vai a 100 sozinho** (Q3): não há limite total.
- **Sinergia própria para cada combinação** de dois elementos (Q4). Com 16 elementos são 120 pares, e cada um precisa de um efeito diferente.
- **Vale para magos NPC** (Q5), que recebem o estilo fixo quando nascem (no spawn).
- **Implícito:** nenhuma mensagem, barra ou aviso. Para evitar farm, a evolução é silenciosa.
- **Visual:** o grimório do jogador **brilha levemente** com as cores dos dois elementos dominantes, como em Black Clover.

**Dúvida 4a (grande, vai para `DECISIONS.md`):** escrever à mão os 120 efeitos de sinergia, ou criar uma **regra geradora** (família de efeito × par de elementos) com tabela escrita só para exceções? Recomendo a regra geradora: é mais fácil de equilibrar e cabe em config. Falta sua escolha.

### 10. Recalcular cratera em segundo plano: aprovado
Se não muda o visual, a cratera pode ser calculada aos poucos. **Aprovado.**

### 12. Diário do Restaurar: não aumentar para 32k; usar regeneração
Conta feita:
- Uma esfera de raio 16 tem cerca de **17.200 blocos** (4/3 · π · 16³).
- Duas explosões desse tamanho somam **cerca de 34.300 blocos**, mais que os **32.768** do diário de 32k.
- Ou seja, o diário de 32k **não cobre** "várias explosões de raio 16 em lugares diferentes". Falha no seu segundo critério.

**Alternativa proposta (híbrido):**
- **Terreno natural** volta regenerando o mundo a partir da seed, sem limite e quase sem custo de armazenamento. Só guarda a lista de regiões alteradas.
- **Blocos que jogadores ou magias construíram** continuam no diário, que pode ficar no tamanho atual (4.096).

Isso é mais barato que o diário de 32k e não tem limite de explosões. **Dúvida 12a:** confirmar o híbrido. Ressalva para a implementação: estruturas do mundo (vilas, ruínas) também são regeneradas pela geração, então é preciso confirmar que voltam iguais.

### 13. Uma cratera por vez: não
Não quer fila. Motivo que deu: "dá uma sensação de injustiça (por que a dele abriu e a minha não?)". Todas as crateras abrem ao mesmo tempo. **Decidido: não.**

### 14. Tabela de afinidades: aprovado
Aprovada como está (ver resumo na rodada anterior). **Aprovado.**

### 15. Números provisórios de H a N: aprovados por delegação
Você não conseguiu abrir o arquivo e pediu que eu avaliasse junto com o resto do projeto. **Minha avaliação: aprovar em bloco**, com duas ressalvas:
- **Limiar "Chuva + Fogo = 1"** (nuvem de vapor): não dispara até a forma Chuva existir. Só ganha efeito depois.
- **Números do Guardião** (60 HP, janela ×1,5): dependem do portão da Hidra (item 3). Podem ser ajustados depois de medir no Boneco.

Os demais números (Boneco 40 HP, Perseguir, Ricochete, Fragmentar, Arco, Nuvem, voz, maestria) estão dentro de faixas razoáveis e são medidos depois no Boneco. **Aprovado por delegação.** Os valores ficam em `docs/PERGUNTAS.md`, seção "Números provisórios".

### 18. Nuvem e afinidade: aprovado
**Aprovado.**

### 23. Atraso → Após Segundos: a Potência ainda alcança o grupo
A Potência continua alcançando o grupo. Como passa a afetar mais de um glifo, **o custo dela também sobe**. Drop de mana: **aprovado**.

**Dúvida 23a:** quanto custa a Potência por glifo afetado? Ex.: +1 de mana por glifo, ou ×1,5 por glifo? Falta o número.

### 24. Carinho: mantém, só o efeito de corações
Carinho continua existindo e fica **só com o efeito de corações** (cura de 4 HP em animais). **Aprovado.**

### 25. Tags de estado: aprovado, exceto um item
Aprovado o sistema de tags de estado (`PROPOSTA_TAGS_DE_ESTADO.md`). Everton não gostou do **item 4**, com a justificativa "não quero prender o jogador".

**Dúvida 25a:** eu entendi que o item 4 é **Sincronização** (passo 4 da migração: Petrificar, medo e sono desenhados pelo cliente via tag). Mas "prender" pode se referir a Petrificar (que imobiliza). Confirmar qual era o item. Enquanto isso, o item 4 **não entra** na implementação.

### 27. Zona que vence com o mundo fechado: ela ainda deve disparar
Quando a zona termina com o mundo fechado (ou o conjurador ausente), o efeito **"Quando a Zona Acabar" ainda dispara**. Hoje ela só some. **Aprovado** como mudança. Ao entrar na zona, quem já estava dentro não conta (como está). A zona pode ter formato de coluna, domo etc. (como está).

### 29. Flutuar como voo: "Isso é bom"
Flutuar serve de voo, sem glifo "Voo" separado. **Aprovado.**

### 31. Eletrificar e Condutor: dois glifos separados
Everton quer **dois glifos diferentes**, não um com `mode`:
- **Eletrificar:** o dano que o alvo leva salta 35% para até 2 criaturas num raio de 4 (como está).
- **Condutor:** raios que caem a até 24 blocos do alvo caem nele. **Não deve afetar só criaturas**: também atinge blocos e jogadores.

**Dúvida 31a:** qual é o **alvo** do Condutor? O raio cai no próprio alvo (como agora), ou pode ser atraído para um bloco ou um jogador? Falta definir a regra exata.

### 32. Reanimar e Encantar: aprovados
- **Reanimar:** traz um **jogador** de volta **1 vez**, pelo custo de **metade da vida restante do conjurador**. Precisa de uma Potência para funcionar. **Aprovado.**
- **Encantar:** **aprovado como temporário** (até 5 min, por 1 a 10 golpes).
- **Sobrecarga:** aprovada (custa 1 coração).

### 33. Diferença entre Vincular e Elo (explicado)
- **Elo** (`link`): liga **duas criaturas** (ou você e uma criatura). Metade do dano que uma sofre é transferido para a outra, por até 60 s. É um efeito que **divide dano** entre criaturas.
- **Vincular** (`bind`): é um **modificador**. Ele prende à sua presença um **efeito de poção** que você pôs em outra criatura (Lentidão, Veneno, Cegar, Velocidade). O efeito acaba se você morrer, sair do mundo ou se afastar mais de 40 blocos. Não serve para efeitos que não são de poção.

Resumo: **Elo divide dano; Vincular amarra um efeito de poção à sua presença.** Não se combinam por padrão.

### 35. Lendário: exige círculo de raio mínimo 16: aprovado
Ritual lendário exige **no mínimo** um círculo de raio **16**. **Aprovado.**

### 36. Grimório: aprovado, com regras novas
- **Começa com a Faísca.** Aprovado.
- **Penalidade de mana** (até recuperar ou 3 dias). Aprovado.
- **Grimório com dono não pode ser destruído de jeito nenhum.** Nem despawna.
- **Forma física:** é sempre um item/entidade 3D. Quando é largado, vira uma **entidade parada** no chão, não um item comum.
- **`/kill` não pode matá-lo.**

**Dúvida 36a:** "de qualquer maneira" inclui **o vazio**? Hoje "só o vazio destrói" o grimório. Falta confirmar se o vazio continua podendo destruir o grimório com dono, ou se esse caso também fica proibido.

### 37. Custo exponencial e 8 páginas: aprovado.

### 38. Cristais e fragmentos: aprovado em princípio, com dúvidas
Everton descreveu um sistema novo de fragmentos de cristal:
- **Cristais têm chance de dropar um fragmento.**
- **Fragmento no chão** (perto de jogadores) ou em **entidades que não são containers** (ex.: moldura de item) concede **1,5× de regeneração de mana e de poder**.
- **Fragmento no próprio inventário** concede **2×**.
- **Exposição prolongada** pode dar **efeitos adversos** (quais, ainda não definido).
- **Cristal obtido com Toque de Seda:** no máximo **16×** (antes 32× naturais), e **concede mana poisoning** conforme a zona. Em resumo, **perde uma zona**.

**Dúvidas 38a (grandes, vão para `DECISIONS.md`):**
- Qual a **chance** de drop do fragmento?
- Qual o raio de "perto de jogadores"?
- Quais **entidades não-container** contam (moldura de item? suporte de armadura?).
- Quais são os **efeitos adversos** e a partir de quanto tempo de exposição?
- "Perde uma zona": a zona do cristal de Toque de Seda é **uma a menos** do que a do cristal comum?

### 39. Glifos desconhecidos: fontes de descoberta
Everton descreveu quando um glifo **unknown** pode aparecer:
- **Armadilha sem tinta invisível** pode revelar um glifo desconhecido.
- **Mago conjurando** pode revelar um glifo desconhecido.
- **Efeitos como veneno** só podem ser "unknown" se **você for atingido** por eles.
- **Efeitos visuais** como Petrificar, quando **atingem outro no seu campo de visão**, podem revelar o glifo.

**Bug reportado:** Petrificar **não está transformando em pedra visualmente**. Vai para a lista de itens a ver em jogo.

**Dúvida 39a:** qual é o **conjunto exato de glifos** que uma armadilha ou um mago pode revelar? Quer que eu escreva a tabela "efeito × fonte de descoberta"?

### 40. Hidra pegando fogo fica vermelha: "para mim, não"
Não incomoda. **Aprovado.**

---

## Pendências: o que falta para aprovar ou responder

**Decisões grandes (vão para `docs/DECISIONS.md`):**
1. **Sinergias do estilo (item 4a):** os 120 pares escritos à mão, ou regra geradora com exceções (recomendo a regra).
2. **Híbrido do Restaurar (item 12a):** confirmar a regeneração do terreno natural + diário só dos blocos construídos.
3. **Cristais (item 38a):** chance de drop; raio; entidades que contam; efeitos adversos; zona do Toque de Seda.
4. **Fontes de descoberta (item 39a):** tabela "efeito × fonte".

**Dúvidas pequenas (uma linha basta):**
5. **Item 25a:** qual é o "item 4" das tags que você não quer (sincronização ou Petrificar?).
6. **Item 23a:** custo da Potência por glifo afetado (+1 ou ×1,5?).
7. **Item 31a:** alvo do Condutor (o raio cai no alvo, ou pode ser atraído a bloco ou jogador?).
8. **Item 36a:** o vazio ainda destrói grimório com dono?

**Aprovados (serão aplicados no domingo):** Corte B com escala pela vida restante; Chuva com centro do topo e nuvem que desce; Hidra opção C com portão do Guardião e selos destrutíveis depois da onda; cratera sem fila; cratera de recálculo em segundo plano; Carinho só com corações; Flutuar como voo; Eletrificar e Condutor separados; Reanimar e Encantar temporário; Sobrecarga; Bolso e Coro; Lendário com raio 16; grimório inicial com Faísca e regras de dono; custo exponencial; Hidra vermelha; zona disparando mesmo com mundo fechado; tags de estado (menos o item 4).

**Para ver em jogo (não é decisão):** Petrificar não vira pedra visualmente; o letreiro do grimório brilhando nas duas cores; a regra de sinergia quando estiver pronta.

---

## Rodada 3 (27 nov): respostas e esboços

Continua valendo o congelamento: nada disto foi aplicado ao código. Só design.

### 1. Sinergias do estilo: prós e contras (ainda sem decisão)

Pares possíveis: 16 elementos → **120 pares** (sem contar o elemento consigo mesmo, que seria o bônus do dominante).

**A. Regra geradora** (família de efeito × par, com tabela só para exceções)
- Prós:
  - Cobre os 120 pares de uma vez, com coerência entre eles.
  - Equilibrar é mudar a regra em um lugar, não 120 entradas.
  - Adicionar um elemento novo gera automaticamente os pares novos.
  - Cabe em config/JSON, como o resto dos números provisórios.
- Contras:
  - Pares da mesma família ficam parecidos (dois elementos de "dano em área" soam iguais), o que briga com "cada um diferente".
  - Para a sinergia ser realmente única, a família e o par precisam se combinar de forma que nenhum par repita o mesmo texto e o mesmo efeito. Isso é verificável por teste, mas exige cuidado no desenho.
  - Erros de geração são menos óbvios que um JSON escrito à mão.

**B. Escrever os 120 à mão**
- Prós:
  - Cada combinação pode ter identidade própria, com a melhor ideia de cada par.
  - Fácil de explicar e de ler em qualquer sessão.
- Contras:
  - 120 entradas para escrever, revisar e testar. É muito trabalho antes de qualquer jogo real.
  - Ajustar um número de balanço exige mexer em muitas entradas.
  - Alto risco de um par ficar desequilibrado sem perceber.

**Recomendação: híbrido.** Regra geradora para os 120 pares (cada par ganha um efeito derivado da combinação de família com o par, com teste de que nenhum repete) e uma lista de **~20 exceções escritas à mão** para os pares de maior identidade (fogo + gelo, luz + sombra, vida + vazio...). Assim cada par é único e a manutenção continua pequena. Decisão ainda sua.

### 2. Restaurar: híbrido confirmado
Terreno natural volta pela geração do mundo a partir da seed, sem limite. Só os blocos construídos por jogadores ou magias ficam no diário, no tamanho atual (4.096). **Confirmado.** Entra em `DECISIONS.md` como decisão aprovada.

### 3. Esboço: cristais e fragmentos (para aprovar ou ajustar)

**Drop:**
- Cristal de mana tem uma **chance de soltar um fragmento** (valor provisório: 10%, em config).
- Toque de Seda não solta fragmento: solta o cristal inteiro, com teto de 16× (ver abaixo).

**Efeito do fragmento:**
- **No chão, perto de jogadores** (raio provisório: 8 blocos) ou **em entidade que não é container** (moldura de item): regeneração de mana ×1,5 e poder ×1,5 para quem está perto.
- **No inventário de quem carrega:** ×2.
- Container (baú, barril, shulker) não conta, para não virar estoque escondido.

**Exposição prolongada (efeito adverso, escalonado):**
- Depois de um tempo com o fragmento perto ou no inventário (provisório: 5 min), aplica **Fraqueza I**.
- Depois de 15 min, **mana poisoning na zona 1**.
- Acima disso, sobe um nível de zona a cada 10 min.
- Largar o fragmento ou se afastar zera o contador.

**Toque de Seda:** cristal com teto de **16×** (antes 32×) e **mana poisoning conforme a zona** menos uma (perde uma zona).

**Perguntas para fechar:**
- A chance de 10% e o raio de 8 blocos estão bons?
- Os tempos de exposição (5, 15 e 10 min) e os efeitos (Fraqueza, zona 1) estão bons?
- Quais entidades não-container contam além da moldura de item?

### 4. Esboço: fontes de descoberta de glifos

| Fonte | O que pode revelar | Quando |
|---|---|---|
| Armadilha sem tinta invisível | glifos da magia da armadilha | ao disparar, para quem for atingido |
| Mago conjurando | glifos da magia que ele conjura | ao ser atingido ou ao ver a conjuração |
| Efeito de poção (veneno etc.) | glifo do efeito | só se você for atingido por ele |
| Efeito visual (ex.: Petrificar) | glifo do efeito | quando atinge outro dentro do seu campo de visão |

**Regra geral proposta:** a descoberta vai para quem **sofreu** ou **viu** o efeito, nunca para quem só estava por perto sem ver. Glifo descoberto assim entra como "identificado" no grimório, com o mesmo aviso discreto da aba Descobertas. Sem recompensa mecânica.

**Perguntas para fechar:**
- A regra "sofreu ou viu" está boa?
- Quer que eu escreva a tabela completa (glifo por glifo) ou só as famílias?

### 5. Item 4 dos estilos: elementos opostos penalizam um ao outro
Confirmado: **elementos opostos pesam um contra o outro**. Fogo alto reduz gelo, por exemplo. Isso faz o estilo ser uma escolha, não só acúmulo.

**Pergunta:** quais são os pares opostos? Sugestão: fogo × gelo, luz × sombra, vida × vazio, terra × tempestade (ar). O resto (água, força, sangue, tempo, natureza, som) fica sem oposto. Confirma ou ajusta?

### 6. Potência por glifo afetado: ×1,5
Confirmado: **cada glifo afetado pela Potência multiplica o custo dela por 1,5**. A Potência continua alcançando o grupo.

### 7. Condutor: atrai para onde o condutor foi colocado

**Regra:** o raio que cai dentro da área do Condutor é atraído para o **ponto onde ele foi colocado**, seja uma criatura, um bloco ou um jogador.

**Desfazer com Dispel:** o Dispel remove o Condutor. Everton quer que o Dispel **também desfaça escrituras** (inscrições no mundo), para dar aos jogadores alguma chance contra armadilhas em ruínas.

**Dúvida 7a:** não entendi a parte "para não substituir o pó". É sobre a tinta/pó arcano que a escritura usa, ou outra coisa? Preciso saber para desenhar a regra de que o Dispel apaga a escritura sem apagar o pó.

### 8. Vazio destruindo grimório com dono
Confirmado: **pode destruir o grimório com dono**, na teoria, mas a recuperação é **muito difícil** (praticamente não se recupera). Não é proibido, só muito difícil de reverter.

**Dúvida 8a:** "muito difícil" quer dizer que existe uma forma de recuperar, mesmo que cara e rara? Ou o grimório destruído pelo vazio se perde de vez? Isso muda a implementação.

---

## Pendências desta rodada

- **Decisão grande:** item 1 (regra geradora, híbrido recomendado: **respondido sem escolha ainda**).
- **Esboços para ajustar:** item 3 (cristais) e item 4 (fontes de descoberta).
- **Dúvidas pequenas:** 5 (pares opostos), 7a (o que é "pó"), 8a (recuperação do grimório pelo vazio).
- **Já confirmados:** 2 (híbrido do Restaurar), 6 (×1,5 por glifo afetado), 8 (vazio pode destruir, recuperação difícil).

---

## Rodada 4 (27 nov): decisões e esboço da planilha

Continua valendo o congelamento: nada disto foi aplicado ao código. Só design.

### Correção de uma rodada anterior: o item 4 rejeitado era dos estilos
Eu tinha entendido que o "item 4" que Everton não quis era o da proposta de **tags de estado** (sincronização). Não era.
O item rejeitado é o item 4 dos **estilos**: elementos opostos penalizam um ao outro. Ele **não gostou dessa penalidade**.
Logo, **as tags de estado ficam aprovadas inteiras**, incluindo a sincronização com o cliente.

### 1. Sinergias do estilo: escrita à mão (decidido)
Os 120 pares serão escritos **à mão**. Motivo dado por Everton: com exceções, a regra geradora produziria pares
"melhores" que outros. Consequência: cada par é escrito e revisado individualmente. É mais trabalho, mas nenhum par sai
favorecido por uma regra. **Decidido.** Entra em `DECISIONS.md` como decisão aprovada.

### 3. Cristais: chance de 20%, e o contador sobe e desce em ritmos diferentes (aprovado)
- **Chance de drop de fragmento: 20%** (aprovado).
- **O contador de exposição não reseta sozinho.** Ele **sobe** devagar e **desce 4× mais rápido**. Exemplo dado:
  se leva 10 min para subir até o efeito pleno, leva ~2,5 min para descer até zero.
- Os limiares continuam provisórios: Fraqueza I aos 5 min de subida; mana poisoning da zona 1 aos 15 min; subir uma
  zona a cada 10 min.
- Largar o fragmento ou se afastar **não zera** o contador: ele só desce no ritmo de descida.

**Dúvida 3a (pequena):** o contador de 5 min para Fraqueza é de subida, então ao descer para zero some em ~75 s?
Confirmo que é isso.

### 4. Planilha de descoberta: o que revela o quê (esboço)

Cada efeito de glifo cai em uma das três colunas, conforme **quem é afetado** (e quem vê):

| Coluna | Como o glifo é descoberto | Exemplos (esboço) |
|---|---|---|
| **Só vendo** | Você só assiste ao efeito, sem ser atingido. Vale se o efeito cai dentro do seu campo de visão. | Petrificar (vira pedra), Luz, Invisibilidade vista de longe, Fogos Arcanos, Estrondo sobre outro |
| **Afetando os outros** | Você atinge outra criatura ou jogador com o efeito. | Dano, Cegar, Lentidão, Atrair, Empurrão, Congelar, Veneno aplicado num alvo |
| **Afetando você mesmo** | O efeito atinge você (como alvo de si mesmo, ou pelo que o mago/armadilha te joga). | Veneno (quando você é atingido), Fogo que te queima, Lentidão em você, Cura, Escudo em você |

**Regra geral:** a descoberta vai para **quem está sendo afetado ou vê** o efeito. Quem só estava por perto e não viu
não descobre. Glifo descoberto entra como "identificado" no grimório, com o aviso discreto da aba Descobertas.

**Observação:** Petrificar hoje não vira pedra visualmente (bug já registrado). Para a coluna "só vendo" funcionar,
o efeito precisa aparecer.

**Pergunta 4a:** quer a planilha completa (todos os 114 efeitos classificados nas três colunas) ou basta a lista de
exemplos acima para fechar a regra?

### 5. Pares opostos: rejeitado
Não haverá penalidade entre elementos opostos. **Rejeitado.** O estilo não penaliza os elementos que o jogador não usa.

### 7. Dispel não desfaz escrituras (decidido)
Hoje o **pó solvente** é o item que desfaz escrituras. Se o Dispel também desfizer escrituras, o solvente perde o
propósito. **Decidido:** o Dispel **não afeta escrituras**. Ele continua removendo efeitos, e o Condutor é removido pelo
Dispel (como combinado). Para dar chance ao jogador contra armadilhas de ruína, a saída é o próprio solvente, que
continua disponível.

**Pergunta 7a:** isso fecha o item? Se sim, a armadilha de ruína fica desarmável só com o solvente.

### 8. Como perder um grimório no vazio: cenários decentes (para escolher)

**Fato do jogo:** hoje, um item cai no vazio (abaixo do fundo do mundo) e some. Como o grimório com dono não pode
despawnar, ele precisa de uma regra própria aí. Três cenários que dão perda real **sem** ser injusta:

**A. Esfera de vazio com janela de recuperação (recomendado)**
- Um efeito de vazio (ex.: Buraco Negro do Eclipse, ou a armadilha de vazio de uma ruína) puxa o grimório para dentro
  da esfera.
- Se alguém **não tirar** o grimório de dentro dela em **até 60 s**, ele é "engolido" e vai para um ponto marcado no
  vazio.
- Para recuperar: o dono pode usar **Rastrear Posse** (já existe, aponta para o grimório perdido) e **Retorno** (já
  existe) para ir até o ponto. Recuperável, mas caro e arriscado.
- Perda definitiva só se o ponto for destruído (por exemplo, a esfera ser fechada por um Dispel do dono).

**B. Queda no vazio do mundo (sem efeito de glifo)**
- Se o grimório cai abaixo do mundo, ele **volta ao último ponto seguro do dono** (cama ou spawn).
- Não há perda. Isso garante que a regra "não despawna" nunca destrói o grimório por acidente.
- Vantagem: justo e sem surpresa. Desvantagem: não há risco real.

**C. Perda permanente pelo vazio (não recomendado)**
- O grimório some de vez se cair no vazio do mundo.
- Viola a regra de "não despawna", e o jogador perde tudo sem chance de recuperar. Recomendo não usar.

**Recomendação:** **A** para o vazio de glifo (perda com janela e recuperação), e **B** para a queda acidental. Assim
existe perda real e recuperável, e nenhum jogador perde o grimório por acidente.

**Pergunta 8a:** A (recomendado), B, C, ou combinação A+B? E a janela de 60 s está boa?

---

## Pendências desta rodada

- **Decidido:** sinergias à mão (1); 20% de chance e contador que sobe e desce em ritmos diferentes (3); Dispel não
  afeta escrituras (7); elementos opostos rejeitados (5); tags de estado aprovadas inteiras.
- **Esboço para fechar:** planilha de descoberta (4): completa ou só exemplos (4a).
- **Escolha:** cenário do vazio (8a).
- **Confirmar:** contador de Fraqueza descendo em ~75 s (3a); Dispel fechado e solvente como única forma (7a).
