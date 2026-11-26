# Proposta: forma Chuva (26 nov 2026)

**Só desenho. Nada disto está implementado.** Pedido de Everton (prompt 3, B5): o efeito Chuva sai e nasce a forma
Chuva. O efeito já saiu (alias `rain` → Molhar, ver `docs/AUDITORIA_GLIFOS.md`, "Mapa de aliases"). Esta proposta espera
aprovação; cada ponto em aberto tem uma recomendação marcada.

## Verbo

**"Eu faço o céu cair aqui."** A Chuva é a única forma que entrega o efeito **de cima para baixo, ao longo do tempo,
num lugar**, e só em quem está debaixo do céu. As vizinhas dela fazem outra coisa:

| Forma | Diferença |
|---|---|
| Aura + Zona | Volume em volta de você, pulsa em quem está dentro, com ou sem teto. A Chuva fica onde você mirou e o teto protege. |
| Coluna + Zona | Um eixo estreito do chão ao céu. A Chuva é uma área larga e rasa (o chão inteiro do círculo). |
| Projétil | Uma carga, um acerto. A Chuva é contínua e não acerta "uma vez". |

## Id

`rainfall` (nome "Chuva" / "Rain"). **Não** reusar `rain`: esse id é alias do efeito antigo, e uma magia salva com o
efeito Chuva viraria a forma no meio da inscrição (forma depois de efeito = inválida). A regra de reação em massa já
aponta para `rainfall` (`interactions/default.json`, gatilho `form_element` da Nuvem de Vapor).

## Entrega de alvos

- **Onde:** o ponto mirado (mesma mira do Pilar: primeira criatura ou bloco na linha, até o alcance), assentado no chão.
  Sem chão debaixo do ponto (mira no céu vazio): falha antes de cobrar mana, como a Coluna.
- **Área:** círculo de raio `radius` (padrão 5, de 3 a 10) em volta do ponto. A nuvem fica a `cloud_height` blocos acima
  do chão (fixo 8, ou até o teto mais baixo se houver um acima do centro; ver "teto parcial").
- **Quem é atingido em cada gota (tick):** toda criatura viva dentro do círculo e **sem teto** (ver abaixo), **inclusive
  você** se estiver debaixo dela (a Chuva não sabe quem a chamou; use Exceto Eu para se poupar). Os filtros seguintes
  (Hostis, Exceto Eu, Mais Próximo...) valem normalmente sobre essa lista.
- **Blocos:** os blocos do chão exposto ao céu dentro do círculo (a superfície molhada). Teto = não recebe.
- **Pontos:** cada gota que cai no chão vira um ponto de impacto, no máximo `drops_per_tick` (proposta: 3) por tick,
  sorteados no círculo. É por eles que Chuva → Fogo acende o chão aqui e ali, sem incendiar tudo de uma vez.

## O que é "sem teto"

Uma criatura está sem teto se **o céu chega até a cabeça dela**: do topo da caixa dela até a altura da nuvem não há
nenhum bloco que bloqueie movimento (`COLLIDER`, o mesmo teste de mira das outras formas). Folhas contam como teto (
elas bloqueiam movimento). Vidro conta como teto. Água não conta (a Chuva cai na água). Proposta de implementação: um
`clip` vertical por criatura por tick, só para quem está no círculo (barato: poucas criaturas).

Para blocos: o bloco está sem teto se o de cima dele está livre até a nuvem.

**Por que não `canSeeSky`?** Porque `canSeeSky` olha até o limite do mundo e erra em cavernas abertas e debaixo de
uma Barreira mágica. Com a altura da nuvem, uma Barreira por cima de alguém protege da Chuva, que é o que um jogador
espera.

## Teto parcial

Cada criatura é testada sozinha: debaixo de uma árvore no meio da chuva, quem está sob as folhas fica seco e quem está
na clareira molha. Não existe "meio teto". Uma criatura grande (caixa larga) conta como sem teto se o **centro** do topo
dela está livre (proposta simples; alternativa: qualquer um dos 4 cantos livres, mais generosa).

Se houver um teto acima do centro **mais baixo** que a nuvem (a Chuva conjurada dentro de um salão), a nuvem desce até
1 bloco abaixo desse teto e a chuva cai dentro do salão. Recomendação: **sim** (a nuvem desce), porque conjurar dentro de
casa e não ver nada parece defeito. Opção contrária: dentro de casa a Chuva falha "o céu está tapado".

## Tick e duração

- Duração embutida: `seconds` (padrão 6, de 3 a 15). Sem glifo Zona, a Chuva já é uma zona curta.
- Intervalo entre gotas: `interval` fixo de **1 s** para efeitos de dano e de estado (Fogo, Congelar, Veneno, Dano,
  Molhar...). Um efeito instantâneo forte não pode cair a cada tick.
- Proposta de números (provisórios, iriam para config e para `docs/PERGUNTAS.md`): 6 s, 1 pulso por segundo, raio 5.

## Zona e Segurar

- **Chuva + Zona:** a Zona estende a duração (`seconds` da Zona substitui o da Chuva) e os eventos de zona valem (Ao
  Entrar na Zona = quem entra debaixo da chuva). Não multiplica o intervalo: continua 1 pulso por segundo.
- **Chuva + Segurar (Hold):** a nuvem fica onde foi conjurada (não segue a mira, como o Pilar) e chove enquanto você
  segura e paga; ao soltar, para na hora. Alternar idem.
- **Chuva + Carregar:** mais raio, não mais dano por gota (a carga aumenta a área até ×1,5).

## Custo de mana

A Chuva entrega o efeito muitas vezes, então paga como uma zona: custo dos efeitos × número de pulsos, com o mesmo
multiplicador de zona (`SpellLimits.zoneMultiplier`, teto 60×), mais a área (`areaCostFactor` pelo raio). Exemplo com os
números propostos: Chuva (custo próprio 3) + Fogo (2) = (3 + 2 × 6 pulsos) × área 1 = 15. Isso fica perto de Aura +
Zona de 6 s com Fogo, que é o par mais parecido; a Chuva ganha a área no ponto mirado e perde quem está coberto.

## Ritual

Num ritual, a Chuva cobre o círculo inteiro (raio do círculo: 4, 8 ou 16) centrada no altar, com a nuvem a 12 blocos.
Lendário: a duração vai a 60 s. **Não** muda o clima do mundo (isso continua sendo o ritual Clima). Recomendação: ritual
de Chuva vale como "zona grande" para a Nuvem de Vapor e para as reações (Chuva Carregada).

## Interações e reações

- **Molhado:** toda gota molha quem está sem teto (como o efeito Chuva fazia), **mesmo que o efeito seguinte não seja
  de água**. É a identidade da forma e o que alimenta as reações: Chuva → Trovão fere mais (Chuva Carregada/Condução),
  Chuva → Congelar congela mais rápido.
  - Exceção: com Fogo, a gota não molha (seria vapor imediato e o Fogo nunca pegaria). Ver Nuvem de Vapor.
- **Nuvem de Vapor (Fase K):** o gatilho `form_element` (forma `rainfall` + efeito de fogo) já está no JSON e dispara a
  nuvem no centro da Chuva no primeiro pulso. Hoje ele não dispara (a forma não existe); os outros gatilhos (3 molhados
  queimados, 6 blocos de água) continuam iguais.
- **Apagar fogo e regar plantações** (o que o efeito Chuva fazia): a forma faz sozinha em cada pulso nos blocos sem teto,
  com o mesmo teto de 24 blocos por pulso, respeitando `worldAlteringSpells`. Exceto com Fogo.

## Exemplos

| Magia | O que acontece |
|---|---|
| `Chuva → Fogo` | Chove fogo por 6 s: a cada segundo, quem está sem teto pega fogo (duração do glifo Fogo) e até 3 pontos do chão acendem uma chama. Quem se abriga debaixo de uma árvore ou de uma Barreira não queima. No primeiro pulso, a Nuvem de Vapor nasce no centro se houver água (gatilho já existente). |
| `Chuva → Congelar` | Granizo: a cada segundo, quem está sem teto fica molhado e recebe Congelar (molhado congela mais rápido e mais forte, pela regra que já existe). A água exposta no círculo vira gelo aos poucos. |
| `Chuva → Veneno` | Chuva ácida: quem fica sem teto acumula Veneno a cada segundo (o glifo Veneno renova a duração; não empilha nível). Plantas não são regadas. |
| `Chuva → Exceto Eu → Dano` | Tempestade de agulhas em volta do ponto mirado, poupando você. |
| `Chuva → Hostis → Trovão` | Molha e eletrocuta só os hostis sem teto: a reação Chuva Carregada aumenta o dano. |

## Presença (skill arcane-presence)

- **Windup (0,5 s):** a nuvem se forma no alto: disco achatado de geometria própria (não `CLOUD` vanilla) na cor do
  elemento do efeito, crescendo do centro, com som de pressão baixando (`tools/gen_sounds.py`, família nova `rainfall`).
- **Sustain:** fitas verticais (linhas finas, não partículas de chuva vanilla) caindo da nuvem até o chão ou até o teto
  mais próximo de cada coluna, na cor do efeito (Fogo laranja com brasas, Congelar azul com cristal, Veneno verde); o
  segundo elemento (se houver) nas bordas da nuvem. Onde há teto, a fita para no teto e respinga: o jogador vê onde está
  protegido.
- **Resolve:** a nuvem se desfaz em anel e some, som de goteira final.
- Respeita "reduzir clarões" (sem relâmpago de tela no Trovão).

## O que fica parado esperando

- Aprovação deste desenho e dos números (raio 5, 6 s, 1 pulso/s, 3 gotas de ponto por tick, nuvem a 8 blocos).
- Decisão sobre "teto parcial" e "dentro de casa" (recomendo: cada criatura sozinha, centro do topo; dentro de casa a
  nuvem desce).
- Depois de aprovado: glifo `rainfall` pela `glyph_table.py`, `RainfallFormHandler` com zona própria, sons e GameTests
  (sem teto molha, teto protege, Zona estende, custo de zona, Nuvem de Vapor pelo gatilho existente).
