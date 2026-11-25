# Fase 5: documento de desenho (sistemas novos)

Estado: **implementado** (aprovado por Everton): Bolso Arcano, Restaurar e Coro, nesta ordem. Ver "Fase 5" em `docs/CHANGELOG_DEV.md` para o que foi testado e o que não foi. Diferenças em relação ao desenho: o Restaurar recoloca tudo de uma vez (sem lotes de 32 por tick) e não guarda o autor; a cota do Coro é cobrada só no disparo. Os três sistemas são independentes entre si e podem ser aprovados, adiados ou cortados separadamente.

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

**Decisões fechadas com Everton:** todos os participantes ativam a magia **ao mesmo tempo**, **dentro do círculo**. Padrões que escolhi para o que ficou em aberto (registrados em `PERGUNTAS.md`): "ao mesmo tempo" = dentro de uma janela de 3 s; o círculo é uma área ao redor de quem inicia, de raio 8, desenhada no chão por partículas; todos precisam ter **a mesma magia equipada**.

### O que é
Uma inscrição marcada como **Coro** só funciona se N jogadores (N ≥ 2) a conjurarem juntos, e ganha força por participante.

### Desenho
- **Glifo.** Modificador `chorus` com parâmetro `min` (2 a 4). A inscrição que o contém passa a exigir um grupo.
- **Fluxo.** O primeiro a conjurar abre uma **sessão de coro** (`ChorusSession`: iniciador, centro, inscrição, prazo de 3 s, participantes) e o servidor desenha o círculo de raio 8 em volta dele. Quem estiver dentro do círculo, com a mesma magia equipada, e conjurar com a tecla normal até o prazo entra. Se, ao fim da janela, houver `min` participantes, a magia dispara **uma vez**, com o iniciador como conjurador. Se não, nada acontece e **ninguém gasta mana**.
- **Sem rede nova.** Não há convite nem UI de adesão: a conjuração normal (`C2SCastRequestPacket`) já basta e o círculo é só partículas do servidor. Um aviso de chat curto ("Coro de X: faltam N") sai pelo canal existente.
- **Custo.** Cada participante paga sua cota (custo total ÷ participantes, com piso), debitada no disparo; quem não tiver mana sai, e o coro falha se cair abaixo de `min`.
- **Força.** Magnitude `1 + 0,35 × (participantes − 1)`, teto x2,4, pelo mesmo caminho de `ExecutionContext.amplify`.
- **Posição.** Participante que saiu do círculo antes do disparo é descartado; a posição é conferida no servidor no fim da janela.
- **NPC.** Fora da v1.
- **Config.** `chorusEnabled`, `chorusRadius` (8), `chorusWindowTicks` (60), `chorusMaxParticipants`.

### Riscos
| Risco | Gravidade | Mitigação |
|---|---|---|
| Abuso por contas múltiplas | média | magia cooperativa por natureza; teto no multiplicador |
| Janela de 3 s apertada com latência alta | média | configurável; o tempo conta no servidor |
| Verificar em jogo exige 2 clientes | alta para teste manual | GameTests com jogadores simulados |
| Quem sai/desconecta no meio | baixa | a sessão expira sem cobrar mana |

### Custo: **M a G** (sem pacotes nem UI novos, caiu em relação ao desenho anterior).
### Testes planejados
GameTests: sessão abre, expira após 3 s sem disparar nem cobrar, dispara com N dentro do círculo, falha com N−1, jogador fora do círculo não conta, magia equipada diferente não conta, cota de mana, multiplicador com teto. JUnit: cálculo de cota e multiplicador.

---

## 3. Bolso Arcano (espaço de armazenamento do mago)

**Decisões fechadas com Everton:** funciona como **Baú do Fim** (por jogador, ninguém abre o de outro) e **permanece na morte**; capacidade = **metade de um baú pequeno**. Baú pequeno tem 27 espaços, então **13** (arredondado para baixo, configurável).

### O que é
Um espaço extra de itens ligado ao jogador, acessado por glifo, sem ocupar o inventário.

### Desenho
- **Armazenamento.** `anexo` por jogador (como o Grimório), com 13 espaços, serializado no NBT do jogador. Os anexos são copiados no clone de morte, então o conteúdo permanece.
- **Acesso.** Glifo `pocket` abre um menu de baú de 2 fileiras em que só os 13 primeiros espaços são utilizáveis (os 5 restantes ficam bloqueados). Custo de mana por abertura.
- **Regras contra abuso.** Lista de itens proibidos (shulker boxes, o próprio Grimório, itens com inscrição) e bloqueio em combate recente (3 s).
- **Admin.** `/arcane pocket <jogador> [limpar]` para recuperar itens.
- **Config.** `pocketEnabled`, `pocketSlots` (13), `pocketBlockedItems`.
- **Fora da v1.** Bolso por bloco, compartilhado, magias que sacam itens dele.

### Riscos
| Risco | Gravidade | Mitigação |
|---|---|---|
| Perda de itens por bug de serialização | alta | teste de salvar/recarregar |
| Duplicação (fechar na morte ou troca de dimensão) | alta | gravação no servidor ao fechar; cópia de morte testada |
| Aninhamento | média | lista de bloqueio |

### Custo: **M**.
### Testes planejados
GameTests: guardar e abrir, sobreviver a salvar/recarregar e ao clone de morte, bloqueio de itens proibidos, espaços 14+ inutilizáveis, bloqueio em combate, privacidade entre jogadores, comando de admin. JUnit: serialização do contêiner.

---

## Ordem e custo total sugeridos

1. **Bolso Arcano** (M): isolado e de menor risco; valida o padrão de menu/anexo.
2. **Restaurar** (M): escopo fechado nos pontos de escrita.
3. **Coro** (M a G): o único que depende de dois clientes para a verificação em jogo.

Total estimado: 3 a 5 dias de trabalho. Cada um fecha com commit próprio, docs regeradas e honestidade no `CHANGELOG_DEV.md`.

## Perguntas

Todas respondidas. Restam só os padrões que eu escolhi no Coro (janela de 3 s, raio 8, mesma magia equipada) e no Bolso (13 espaços), que Everton pode ajustar na revisão.
