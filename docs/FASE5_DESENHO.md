# Fase 5: documento de desenho (sistemas novos)

Estado: **proposta, nada implementado.** Aguarda aprovação antes de qualquer código. Os três sistemas são independentes entre si e podem ser aprovados, adiados ou cortados separadamente.

Custo em "unidades": P ≈ 0,5 dia, M ≈ 1 dia, G ≈ 2 a 3 dias de trabalho, contando código, GameTests, docs e ajustes (estimativa minha, sem margem para surpresas de cliente).

---

## 1. Restaurar (recompor blocos destruídos recentemente)

**Decisões fechadas com Everton:** glifo **raro** (tier 3); **qualquer jogador** pode restaurar estrago de qualquer autor; **sem recarga**; explosões e afins **não dropam item por padrão** (config para ligar; ligada, os blocos daquela conjuração não entram no diário); **Dig fora do diário** e sempre dropa; **Cortar entra** no diário e não dropa; baús e fornalhas podem ser destruídos e soltam o conteúdo, o Restaurar devolve só o bloco vazio; diário só em memória na v1.

### O que é
Glifo de efeito que reconstrói, dentro de um raio, os blocos que **magias** destruíram há pouco, a partir de um snapshot guardado no momento da destruição.

### Desenho
- **Registro.** Um `BlockJournal` por dimensão guarda `(pos, BlockState, tempo, autor)`. Entram blocos destruídos por Explosão Destrutiva, Cortar, Dano em blocos frágeis, Congelar/Derreter, Fogo/Ignição e barreiras. **Escavar não entra.** Cada ponto de escrita chama `BlockJournal.record(level, pos, oldState, cause)` **antes** de alterar o bloco.
- **Sem loot.** Os efeitos que entram no diário destroem sem soltar item (`destroyBlock(pos, false)`). Config `blastDropsLoot` (padrão `false`): se ligada, esses efeitos dropam e **não registram** no diário. O conteúdo de blocos com inventário cai normalmente (são itens que já existiam).
- **Limites duros.** Teto de entradas por dimensão (4.096, as mais antigas saem primeiro), janela de validade (120 s), teto de blocos por conjuração (256), raio máximo 12 (escala com a magnitude).
- **Restaurar.** Percorre o diário no raio, do mais antigo para o mais novo, em lotes de 32 por tick, e recoloca o bloco só se a posição **ainda estiver no estado pós-magia** (ar, fluido ou resíduo). Se alguém construiu ali, a entrada é descartada. Só o estado simples do bloco volta; bloco com inventário volta vazio, sem NBT.
- **Custo e recarga.** Mana = custo base + valor pequeno por bloco efetivamente restaurado; se nada for restaurado, não cobra. **Sem recarga.** Efeito visual: partículas subindo.
- **Persistência.** Só em memória na v1 (some ao reiniciar).
- **Config.** `restoreEnabled`, `restoreWindowSeconds`, `restoreMaxBlocksPerCast`, `restoreJournalCap`, `blastDropsLoot`.

### Riscos
| Risco | Gravidade | Mitigação |
|---|---|---|
| Loot de explosão virar exploit | média | padrão sem loot; com loot ligado, fora do diário |
| Sobrescrever construção de terceiros | alta | só restaura se o estado atual for o "pós-magia"; mesmo caminho `mayAlterWorld` dos outros efeitos |
| Sem recarga, uso como "pincel" ou spam | baixa | o custo em mana e o teto por conjuração limitam; reavaliar em jogo |
| Memória/tick em explosões grandes | média | tetos por dimensão e por conjuração; lotes de 32 por tick |
| Mudança de gameplay (explosão sem loot) | média | aprovada por Everton; configurável |

### Custo: **M** (sem persistência, sem NBT, sem rastreio de drops).
### Testes planejados
GameTests: registro em cada ponto de escrita (e Dig **não** registra); restauração exata; recusa com construção por cima; teto por conjuração; expiração da janela; sem drops por padrão e com `blastDropsLoot` fora do diário; baú volta vazio; qualquer jogador restaura; sem recarga. JUnit: política de teto/expiração do diário.

---

## 2. Coro (magia que exige dois ou mais jogadores)

### O que é
Uma inscrição marcada como **Coro** só funciona se N jogadores (N ≥ 2) a conjurarem juntos, e ganha força por participante.

### Desenho
- **Glifo.** Modificador `chorus` com parâmetro `min` (2 a 4). A inscrição que o contém passa a exigir um grupo.
- **Fluxo.** O iniciador conjura normalmente; em vez de executar, o servidor abre uma **sessão de coro** (`ChorusSession`: id, iniciador, inscrição, prazo de 10 s, lista de participantes). Jogadores num raio (padrão 16) que tenham **a mesma magia equipada** no Grimório e apertem a tecla de conjurar dentro do prazo entram. Quando chega a `min`, a magia dispara **uma vez**, com o iniciador como conjurador.
- **Custo.** Cada participante paga sua cota de mana (custo total ÷ participantes, com piso), debitada no servidor no momento do disparo; se algum não tiver mana, ele sai da sessão e o coro falha se cair abaixo de `min`.
- **Força.** Multiplicador de magnitude `1 + 0,35 × (participantes − 1)`, com teto (x2,4 com 5 ou mais), pela mesma via de `ExecutionContext.amplify`, sem inventar outro caminho.
- **Rede.** Um pacote S2C avisa os jogadores próximos ("Coro de X: pressione para participar", barra de 10 s) e um C2S de adesão (o pressionar normal de conjurar reaproveita `C2SCastRequestPacket` com o id da sessão). Nada vem do cliente além da intenção; distância, mana e magia equipada são conferidos no servidor.
- **NPC.** Fora da v1 (magos NPC não participam).
- **Config.** `chorusEnabled`, `chorusRadius`, `chorusWindowSeconds`, `chorusMaxParticipants`.

### Riscos
| Risco | Gravidade | Mitigação |
|---|---|---|
| Abuso por contas múltiplas | média | é uma magia cooperativa por natureza; teto no multiplicador |
| Spam de convites | média | uma sessão por iniciador, recarga de 10 s, raio |
| Dessincronização de UI | média | a sessão é inteira no servidor; o cliente só mostra estado recebido |
| Impossível de testar com um jogador só | alta para o teste manual | GameTests com jogadores simulados (já temos o padrão `EmbeddedChannel`); teste manual exige 2 clientes |
| Quem sai/desconecta no meio | baixa | a sessão expira e devolve nada (a mana só sai no disparo) |

### Custo: **G** (UI de adesão e dois pacotes são a maior parte).
### Testes planejados
GameTests: sessão abre, sessão fecha por prazo, dispara com N, falha com N−1, cota de mana, raio, magia equipada diferente recusa, multiplicador com teto. JUnit: cálculo de cota e de multiplicador.

---

## 3. Bolso Arcano (espaço de armazenamento do mago)

### O que é
Um espaço extra de itens ligado ao jogador, acessado por glifo, sem ocupar o inventário.

### Desenho
- **Armazenamento.** Um `anexo` (attachment) por jogador, como o Grimório (`GrimoireAttachments`): contêiner de 9 a 27 espaços conforme nível, serializado no NBT do jogador. Sobrevive à morte? **Pergunta 2.**
- **Acesso.** Glifo `pocket`: ao conjurar, abre a tela do contêiner (`MenuProvider` de baú simples, sem GUI nova). Custo de mana por abertura, recarga curta.
- **Regras para evitar abuso.** Lista de itens proibidos (outros mods de armazenamento, shulker boxes, o próprio Grimório e itens com inscrição, para não aninhar), bloqueio para quem está em combate recente (3 s), e o conteúdo não é lido por nenhum filtro/efeito.
- **Multiplayer.** O contêiner é por jogador; ninguém abre o de outro. Comando de admin `/arcane pocket <jogador> [limpar]` para recuperar itens.
- **Config.** `pocketEnabled`, `pocketSlots`, `pocketBlockedItems`.
- **Escopo fora da v1.** Bolso por inscrição/bloco, Bolso compartilhado, magias que sacam itens do bolso.

### Riscos
| Risco | Gravidade | Mitigação |
|---|---|---|
| Perda de itens por bug de serialização | alta | teste de salvar/recarregar; cópia de segurança no NBT |
| Exploit de duplicação (fechar menu em tick de morte, troca de dimensão) | alta | só abre/fecha no servidor, gravação ao fechar, bloqueio durante morte |
| Aninhamento (bolso dentro do bolso, shulker) | média | lista de bloqueio |
| Item perdido na morte, conforme regra escolhida | média | decisão explícita (Pergunta 2) |
| Compatibilidade com mods de inventário | baixa | usar o caminho padrão `Container`/`MenuProvider` |

### Custo: **M**.
### Testes planejados
GameTests: guardar e abrir, sobreviver a salvar/recarregar, bloqueio de itens proibidos, bloqueio em combate, privacidade entre jogadores, comando de admin. JUnit: serialização do contêiner.

---

## Ordem e custo total sugeridos

1. **Bolso Arcano** (M): isolado e de menor risco; bom para validar o padrão de menu/anexo.
2. **Restaurar** (M): maior risco de exploit, mas de escopo fechado nos pontos de escrita.
3. **Coro** (G): o mais caro e o único que depende de dois clientes para a verificação em jogo.

Total estimado: 4 a 7 dias de trabalho. Cada um fecha com commit próprio, docs regeradas e honestidade no `ESTADO_DO_MOD.md`.

## Perguntas que precisam de resposta antes de começar

1. ~~Restaurar: glifo vs ritual~~ **Fechado:** glifo raro (tier 3).
2. **Bolso:** os itens ficam com o jogador na morte (recomendado, é um bolso mágico) ou caem como o inventário?
3. ~~Restaurar: diário~~ **Fechado:** só em memória na v1.
4. **Coro:** os participantes precisam ter a mesma magia equipada (recomendado, evita adesão acidental) ou basta estarem no raio?
5. **Todos:** posso cortar algum dos três ou mudar a ordem?
