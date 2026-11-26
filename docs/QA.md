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
