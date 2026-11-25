# Proposta: tags de estado e reações num sistema só (Fase G, 25 nov 2026)

Só proposta. **Nada foi implementado.** Escrita a partir do código de 25 nov.

## Hoje: quatro sistemas que fazem a mesma coisa

| Sistema | Onde | O que guarda | Como lê |
|---|---|---|---|
| `Wetness` | `execution/Wetness.java` | `arcane_wet_until` (tick de fim) no `getPersistentData()` da entidade | `isSoaked` |
| `MindStates` | `execution/MindStates.java` | medo, sono, distração: `*_until` + UUID de quem causou (`arcane_fear_of`, `arcane_distract_of`) | `isScared`, `isAsleep`, `isScaredOf` |
| `Phase4State` | `execution/Phase4State.java` | maldição, silêncio, reflexo, petrificar, carregado, elo, lacaio, retorno: `*_until` e alguns UUID/posições | `active(e, key)` |
| `InteractionRules` | `data/arcane/interactions/default.json` + `InteractionRules.CONDITIONS` | regras elemento × condição → dano extra, gelo extra, ação (`steam`, `extinguish`) | `matching(level, element, entity)` |

Mais chaves soltas fora deles: `arcane_marked_until` (Marks), `arcane_flight_safe_until`, `arcane_summon_until`,
`arcane_last_breath_until/_day`, e recargas como `arcane_sat_cd` e `arcane_trail_cd` (Phase4Service). São 19 chaves
`*_until` diferentes.

Problemas:
1. **Mesmo formato, quatro APIs.** Todas são "chave com tick de fim no NBT da entidade". Cada uma tem seu `set/active/clear`.
2. **Listas paralelas que desatualizam.** `Phase4State.NEGATIVE` e `MAGICAL` (o que Dissipar tira) precisam citar à mão
   as chaves de `MindStates`, `Wetness` e `Marks`. Estado novo esquecido nessas listas = Dissipar não tira.
3. **Reações não enxergam os estados.** As condições do JSON são fixas em código (`raining`, `wet`, `frozen`, `on_fire`,
   biomas). "Fogo em amaldiçoado", "Raio em quem está com elo" ou "Gelo em quem dorme" exigem código novo.
4. **Sem sincronização com o cliente.** O NBT persistente não vai ao cliente; cada estado visível inventa o seu pacote
   ou efeito de poção marcador (Petrificar usa um efeito marcador para o cliente desenhar a pedra).
5. **Sem prioridade nem conflito.** Molhado e em chamas coexistem até uma regra JSON resolver; sono e medo também.

## Proposta: `StateTags`

Um registro de tags de estado, definidas em dados, e uma única API.

### Definição (dados)

`data/arcane/states/<id>.json`:

```json
{
  "id": "arcane:wet",
  "max_ticks": 1200,
  "stack": "max",                 // max (fica o maior fim) | refresh | add
  "categories": ["magical"],      // o que Dissipar tira: "negative" em si mesmo, "magical" nos outros
  "source": false,                // guarda quem causou (medo de quem, elo com quem)
  "sync": true,                   // vai ao cliente (para desenhar)
  "removes": ["arcane:burning"],  // aplicar esta tira aquelas (molhar apaga fogo)
  "blocked_by": [],               // nao entra se a criatura tiver alguma destas
  "boss_immune": false
}
```

Estados do jogo base entram como tags "derivadas", só de leitura, com o mesmo nome no JSON de reações:
`arcane:burning` (`isOnFire`), `arcane:frozen` (`getTicksFrozen() > 0`), `arcane:in_rain`, `arcane:in_water`.
Biomas viram tags de lugar: `arcane:hot_biome`, `arcane:lush_biome`.

### API (código)

```java
StateTags.apply(entity, "arcane:wet", ticks, source);   // respeita stack, removes, blocked_by
StateTags.has(entity, "arcane:fear");
StateTags.source(entity, "arcane:fear");                // UUID ou null
StateTags.clear(entity, "arcane:wet");
StateTags.clearCategory(entity, "negative");            // Dissipar
```

Armazenamento: uma capability (`ArcaneStates`) com um mapa tag → (fim, fonte), salva com a entidade, em vez de chaves
soltas no `getPersistentData()`. As tags com `sync: true` vão num pacote só (`S2CStatesPacket`), enviado quando mudam,
para quem acompanha a entidade.

### Reações

O JSON de interações passa a falar em tags:

```json
{ "element": "fire", "when": ["arcane:wet"], "damage_add": -0.4, "after": [{"clear": "arcane:wet"}, {"reaction": "steam"}] }
{ "element": "fire", "when": ["arcane:frozen"], "damage_add": 0.25, "after": [{"reaction": "shatter"}] }
{ "element": "frost", "when": ["arcane:burning"], "after": [{"clear": "arcane:burning"}, {"reaction": "hiss"}] }
```

- `when` aceita qualquer tag registrada (inclusive as derivadas e as de lugar).
- `after` vira uma lista de ações pequenas: `clear`, `apply` (com ticks), `reaction` (o visual de `Fx.reaction`).
- O visual das reações (Fase C) fica ligado pelo nome; nada de visual no código da regra.
- Validação na carga (como hoje): tag, elemento ou ação desconhecidos viram aviso e a regra é ignorada.

## Migração (sem quebrar mundos salvos)

1. **Camada de compatibilidade.** Criar `StateTags` e a capability; `Wetness`, `MindStates` e `Phase4State` passam a
   delegar para ela, mantendo os métodos atuais. Na primeira leitura de uma entidade, as chaves antigas
   (`arcane_*_until`, `arcane_fear_of`...) são copiadas para a capability e apagadas do NBT. GameTest: entidade salva
   com as chaves antigas carrega com os mesmos estados.
2. **Dissipar por categoria.** Trocar `NEGATIVE` e `MAGICAL` por `clearCategory`. GameTest: cada estado de cada
   categoria sai, e nenhum outro.
3. **Reações por tag.** O leitor aceita o formato antigo (`"wet"`, `"frozen"`, `"after": "steam"`) traduzindo para o novo,
   então datapacks de terceiros continuam valendo. `default.json` passa para o novo formato. O teste de matriz e os
   testes de gelo/fogo continuam passando.
4. **Sincronização.** Petrificar, medo e sono desenhados pelo cliente via tag em vez de efeito marcador.
5. **Limpeza.** Depois de uma versão com a camada de compatibilidade, apagar os métodos antigos (ou deixá-los como
   atalhos).

Cada passo é um commit com GameTests, e nenhum muda números de balanço.

## Riscos e perguntas

- Capability nova exige cuidado com clonagem ao morrer (`PlayerEvent.Clone`): hoje o NBT persistente do jogador some na
  morte. Decidir quais tags sobrevivem à morte (proposta: nenhuma, como hoje).
- Pacote de sincronização: com muitas criaturas e estados, mandar só a diferença.
- Pergunta para Everton: as reações novas que isso permite (Raio em quem tem Elo, Gelo em quem dorme) entram, ou o
  sistema muda só por dentro? A proposta não cria nenhuma reação nova.
