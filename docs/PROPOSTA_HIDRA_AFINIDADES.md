# Proposta: a Hidra com afinidades e reações

Estado: **só proposta, nada implementado na Hidra.** A Hidra continua sem perfil de afinidade e sem nenhum
comportamento novo. A única mudança no código dela (Fase N) foi passar o selo e o momento de fraqueza pelo
`PhaseGate` genérico, com o mesmo efeito de antes (a suíte inteira continua verde).

## O que já existe e pode ser usado

- **Afinidades** (Fase I): `data/arcane/affinities/*.json`, um multiplicador por elemento de gameplay do efeito, com o
  teto `affinityMaxTotal` (2,0) junto das reações. A Hidra não está em nenhum perfil (teste
  `entitiesWithoutProfileAreUnchanged`).
- **Reações** (Fase K): cada reação tem id, e o `CastScope` sabe em quem ela aconteceu e quem conjurou.
- **PhaseGate** (Fase N): casca que só cai por um estado ou uma reação, seguida de janela de vulnerabilidade. O
  Guardião de Ruína já usa um quebra-cabeça de dois passos (Molhar, depois Fogo).

## Três caminhos

### (a) Nada muda
A Hidra continua neutra a todos os elementos. Zero risco de balanço. O jogador não aprende nada de elementos com ela.

### (b) Afinidade fixa por estágio
Um perfil por faixa de estágio (por exemplo, 1 a 5 fraca a gelo; 6 a 10 imune a fogo, o que já é verdade pelo
`fireImmune()` do estágio 6; 11 a 14 resistente a tudo menos vazio). Simples de ler e de testar.
- Risco: estágio alto com resistência ampla vira esponja de dano e alonga uma luta que já é longa.
- Risco: um elemento "certo" por estágio vira receita fixa (sempre a mesma magia).

### (c) Afinidade por cabeça viva (recomendada para depois)
Cada cabeça tem um elemento (sorteado no nascimento, salvo no NBT e mostrado pela cor da cabeça). Enquanto a cabeça
vive, a Hidra resiste àquele elemento (×0,75) e é fraca ao oposto (×1,25). Quando uma cabeça cai pelos limiares de
vida, a resistência dela some. Uma reação no corpo (Vapor, Estilhaço) abre a mesma janela de fraqueza que a magia
grande já abre hoje (×2), pelo `PhaseGate`, com tempo de recarga.
- Ganho: a luta muda com o tempo, e cortar cabeças tem sentido elemental.
- Risco: com 7 cabeças, a soma pode virar "resiste a tudo". **Mitigação:** a afinidade vale só para as 2 cabeças mais
  próximas do jogador que acertou, e o multiplicador final passa pelo teto de 2,0 já existente.
- Risco: reação abrindo fraqueza pode encurtar muito a luta com Chuva + Tempestade. **Mitigação:** recarga de 20 s
  e só reações que exigem dois passos (preparar o alvo e depois acertar).
- Risco técnico: a Hidra usa `ProjectileUtil` e muitas partes; a afinidade pelo `AffinityContext` já pega o dano
  dentro dos efeitos, então não precisa tocar no `hurt` dela além do `PhaseGate`.

## Como medir antes de decidir

1. Jogar contra o Guardião de Ruína (Fase N): o quebra-cabeça de dois passos é entendido sem ler a dica?
2. No Boneco de Treino, perfil novo "hydra_test" (só Boneco, `dummy_only`) com os números de (c) e medir o dano por
   segundo de 3 magias comuns com e sem a fraqueza.
3. Só então ligar em uma Hidra de estágio baixo, por config (padrão desligado).

## Recomendação

(c), em duas etapas, e **só com aprovação**: primeiro a cor e o elemento das cabeças só visuais (sem efeito), para ver
se a leitura funciona; depois os multiplicadores. Registrado em `docs/DECISIONS.md`, item 2.
