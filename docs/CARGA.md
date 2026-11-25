# Carga do servidor (Fase D, 25 nov 2026)

Medido por `LoadGameTests.craterRadius64And128` (GameTest, servidor de testes nesta máquina) com os tetos de área
desligados (perfil "Sem limites": `areaHardCaps = false`, `craterMaxRadius = 0`). Com o perfil padrão a cratera para no
raio 24 e nada disto acontece. Para medir de novo, rode a suíte e procure `[carga]` na saída.

## Números

| | Raio 64 | Raio 128 |
|---|---|---|
| Blocos da cratera | 383.641 | 3.347.521 |
| Calcular a lista (`RoundCrater.blockLongs`) | **5,7 s num tick só** | **13,5 s num tick só** |
| Memória da lista (8 bytes por bloco) | 2,9 MB | 25,5 MB |
| Montar as cascas (`CraterQueue.submit`) | 144 ms | 561 ms |
| Remoção pela frente (40 ticks) | 129.260 blocos, pior tick 159 ms | 182.202 blocos, pior tick 19 ms |
| Tempo até terminar | ~7 s | ~37 s |
| Diário do Restaurar | cheio (4.096, o teto) | cheio (4.096) |

Partículas da mesma escala (ver Fase C, `FxGameTests`): anel de raio 128 = 256 partículas; casca de raio 128 = 388
faíscas em 0,42 ms.

## O que os números dizem

1. **O travamento é o cálculo da lista, não a remoção.** `blockLongs` percorre o cubo (2r+1)³ inteiro, lendo cada bloco
   (e gerando chunks), dentro de um tick. Raio 64 trava o servidor 5,7 s; raio 128, 13,5 s. Isto explica os "Can't keep
   up" de até 28 s vistos no jogo em 24 nov, se houve explosão grande.
2. A remoção já é boa: o orçamento `craterBlocksPerTick` segura o tick (o pior foi 159 ms, no começo do raio 64, com
   geração de chunk junto).
3. **O Restaurar não consegue desfazer crateras grandes:** o diário guarda 4.096 entradas por dimensão e a cratera de raio
   64 tem 383 mil blocos. Só as últimas 4.096 voltam.
4. Memória: aceitável até 128 (25 MB por cratera em andamento), mas duas ou três crateras de 128 ao mesmo tempo já pesam.

## Limites propostos (nada aplicado: são decisões de balanço e de servidor)

- [ ] **Calcular a cratera aos poucos:** gerar as cascas por camada em vários ticks (o mesmo orçamento por tick), em vez
  da lista inteira num tick. Mudança só de implementação; o resultado no mundo é o mesmo. Recomendo fazer.
- [ ] **Teto de raio mesmo no "Sem limites":** 64 por padrão (`craterMaxRadius = 64` no perfil), deixando 0 (sem limite)
  como escolha explícita do dono do servidor.
- [ ] **Diário do Restaurar:** subir o teto padrão para 32.768 (≈3 MB salvos) ou aceitar que crateras grandes não voltam
  inteiras e dizer isso na descrição do glifo.
- [ ] **Uma cratera grande por vez por dimensão:** as outras esperam na fila.

## Outras proteções feitas nesta fase

- Diário do Restaurar salvo com o mundo (`JournalData`), com teto, guardando quem destruiu (`Entry.author`; a cratera do
  jogador guarda o jogador; a da Hidra fica sem dono).
- Orçamento de reentrância (`SpellReentry`): profundidade máxima `spellMaxDepth` (8) e `spellMaxExecutionsPerTick` (512)
  por tick no servidor. Cada sistema já se protegia sozinho; este é a rede de segurança entre sistemas.
- `ProtectionProvider` (API): outros mods podem negar alterar ou quebrar blocos; consultado em `Targeting.mayAlterWorld`
  e `mayBreakBlocks`.
