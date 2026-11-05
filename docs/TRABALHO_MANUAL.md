# Arcane — Trabalho manual pendente

Este documento lista tudo que **não pode ser resolvido só com código** e precisa de decisão humana, arte, design ou testes no jogo.

Use como checklist de produção. Itens marcados com **(P0)** são os mais urgentes para o mod parecer “jogável” e não “protótipo de programador”.

---

## Estado em 4 nov 2026: o que já foi feito

Tudo abaixo está no repositório. As artes e sons são **provisórios de qualidade**, gerados por `tools/gen_textures.py` e `tools/gen_sounds.py`; um artista pode trocar qualquer arquivo mantendo nome e tamanho.

| Área | Feito |
|------|-------|
| Texturas de bloco | Bancada (topo/lado/base), Cristal de Mana (cristal + base) |
| Texturas de item | Pena, 3 tintas, Nota de Glifo, Máscara, Manto, Projetor, Espelho |
| Modelos JSON | Todos os itens; bancada `cube_bottom_top`; cristal com modelo 3D de pontas (não é mais cubo), com hitbox ajustada |
| GUI | Fundo da Bancada (240×222), 16 ícones de glifo + ícone "desconhecido", barra de mana |
| Bancada | Lista só glifos conhecidos, ordem editável (↑ ↓ ✕), custo, prévia de validade, Conjurar da bancada, troca de magia ativa |
| HUD | Barra de mana, magia ativa, aviso sem grimório, zona de mana, aviso de Mana Poisoning |
| Grimório | Visível: livro flutuante que gira e folheia (modelo de livro vanilla + textura própria) |
| Projétil | Orbe brilhante com rastro de partículas e explosão de faíscas no impacto |
| Mago NPC | Skin própria (manto, capuz, olhos ciano) e ovo gerador |
| Armadura | Máscara (cabeça) e Manto (peito) agora são equipáveis; antes eram itens comuns e a proteção contra Mana Poisoning nunca funcionava |
| Partículas | `arcane:arcane_spark` (4 quadros): projétil, cristal, conjuração, escrita com pena |
| Sons | 8 sons em `sounds.json` com legendas: conjurar, impacto, salvar, aprender glifo, pena, ritual, Mana Poisoning, grimório |
| Worldgen | Cristais de mana nas cavernas do Overworld (Y -56 a 24, raro) |
| Loot | Notas de glifo e tintas injetadas em 7 baús vanilla; drops dos blocos; tags de ferramenta |
| Receitas | Bancada, Pena, 3 tintas, Nota, Máscara, Manto, Projetor, Espelho |
| Textos | `pt_br` e `en_us` completos: lore de itens, nomes e descrições de glifos, categorias, níveis de conhecimento, HUD, legendas |
| Ícone do mod | `arcane_logo.png` no `mods.toml` |

### Correções de código feitas junto

- A tecla **V** usava o grimório do lado do cliente, que nunca era sincronizado, e enviava um id de inscrição que não existia no servidor. Agora o servidor sincroniza mana, glifos e magias (`ArcaneSync` → `ClientArcaneData`).
- A precisão de bancada era confiada ao cliente. Agora o servidor só a concede se a Bancada estiver aberta.
- O pacote de salvar aceitava qualquer id e qualquer tamanho. Agora limita a 24 glifos, valida antes de salvar e informa o motivo da recusa. Ids inexistentes não entram mais no grimório.
- Falhas de conjuração (mana insuficiente etc.) agora aparecem na barra de ação.
- O projétil some depois de 10 s se não acertar nada (antes ficava no mundo para sempre).

### Decisões que tomei e precisam da sua aprovação

- **Composição inválida não é salva**, mas continua contando como experimento de descoberta. Antes salvava e falhava só na hora de conjurar.
- **Receitas e loot**: ingredientes e pesos escolhidos por mim (ametista como base arcana). Ajuste em `tools/gen_data.py`.
- **Drop do Cristal**: sem Toque de Seda dá 2 a 4 fragmentos de ametista; com Toque de Seda dá o cristal.
- **Armadura**: defesa baixa de propósito (1 na máscara, 2 no manto). O valor delas é a proteção contra Mana Poisoning.
- **Nomes dos glifos** são descritivos (Eu, Projétil, Dano...). Os nomes de fantasia continuam sendo decisão de design.
- **Tecla B** troca a magia ativa.

### Ainda pendente

- Função do **Projetor de Glifos** e do **Espelho de Simetria** (depende do design de rituais).
- Estágios visuais do grimório (a entidade não sincroniza o estágio com o cliente).
- Arte final de artista, sons gravados, modelo 3D próprio da bancada.
- Spawn natural do Mago NPC, estruturas, livros de lore, balanceamento e playtest (seções 5 a 9).
- Brecha de descoberta: o cliente pode mandar qualquer glifo existente em "experimentos" para aprendê-lo após 3 salvamentos. Corrigir exige decidir a regra de descoberta.

---

## 1. Arte visual — texturas

Hoje **não existe a pasta** `assets/arcane/textures/`. Quase tudo usa textura vanilla emprestada ou aparece sem modelo próprio.

### Blocos **(P0)**

| Asset | Caminho sugerido | Tamanho | Notas |
|-------|------------------|---------|-------|
| Bancada Arcana | `textures/block/arcane_workbench.png` | 16×16 (ou conjunto para modelo 3D) | Mesa de escriba mágica; hoje usa crying obsidian |
| Cristal de Mana (grande) | `textures/block/mana_crystal.png` | 16×16 + emissivo opcional | Deve parecer fonte de energia; hoje usa ametista |
| Traços de glifo no chão | `textures/block/glyph_trace_*.png` | 16×16 | Para rituais e escrita arcana no mundo |

### Itens **(P0)**

| Item | Caminho sugerido | Situação atual |
|------|------------------|----------------|
| Pena Arcana | `textures/item/arcane_pen.png` | Pena vanilla |
| Tinta Comum | `textures/item/common_ink.png` | Saco de tinta vanilla |
| Tinta de Sangue | `textures/item/blood_ink.png` | Sem textura própria |
| Tinta Invisível | `textures/item/invisible_ink.png` | Sem textura própria |
| Nota de Glifo | `textures/item/glyph_note.png` | Livro vanilla |
| Máscara Arcana | `textures/item/arcane_mask.png` | **Sem modelo JSON** — item roxo/preto |
| Manto Isolante | `textures/item/insulating_cloak.png` | **Sem modelo JSON** |
| Projetor de Glifos | `textures/item/glyph_projector.png` | **Sem modelo JSON** |
| Espelho de Simetria | `textures/item/symmetry_mirror.png` | **Sem modelo JSON** |

### GUI **(P0)**

| Asset | Caminho sugerido | Notas |
|-------|------------------|-------|
| Fundo da Bancada Arcana | `textures/gui/arcane_workbench.png` | 256×256 ou múltiplo de 256; hoje é retângulo roxo no código |
| Ícones de glifos | `textures/gui/glyphs/<nome>.png` | Um ícone por glifo para a bancada mostrar glifos conhecidos |
| Barra de mana | `textures/gui/mana_bar.png` | HUD ainda não existe |

### Grimório **(P1)**

| Asset | Notas |
|-------|-------|
| Modelo do grimório flutuante | Hoje a entidade usa `NoopRenderer` — **invisível** no jogo |
| Estágios visuais 1–5 | Livro pequeno → páginas flutuantes (visão do Julius Novachrono no design original) |
| Texturas de página / capa | Para cada estágio de progressão |

### Partículas e efeitos **(P1)**

| Efeito | Caminho sugerido |
|--------|------------------|
| Projétil arcano | `textures/particle/arcane_spark.png` |
| Zona de mana / cristal | Partículas ambiente perto do cristal |
| Mana Poisoning | Partículas azuis, distorção (pode precisar shader — trabalho avançado) |
| Escrita de glifo | Partículas ao desenhar com a pena |

### Ícone do mod **(P2)**

| Asset | Caminho |
|-------|---------|
| Logo no mod list | `textures/gui/mod_icon.png` (opcional, 128×128) |

---

## 2. Modelos 3D e blockstates

Textura sozinha não basta — é preciso atualizar os JSONs em `assets/arcane/models/`.

### A fazer manualmente

- [ ] **Bancada Arcana** — trocar `cube_all` por modelo de mesa (JSON blockbench ou multipart)
- [ ] **Cristal de Mana** — modelo de cristal (não cubo inteiro); animação de rotação opcional
- [ ] **Criar `models/item/`** para: `blood_ink`, `invisible_ink`, `arcane_mask`, `insulating_cloak`, `glyph_projector`, `symmetry_mirror`
- [ ] **Grimório** — model + animação para entidade flutuante
- [ ] **NPC Mago** — skin ou modelo custom (hoje usa renderer de zumbi)

Ferramenta recomendada: [Blockbench](https://www.blockbench.net/) → exportar para Java Block/Item.

---

## 3. Áudio

Nenhum som customizado existe. Tudo usa áudio vanilla ou silêncio.

### Sons necessários **(P1)**

| Ação | Arquivo sugerido | Referência de sensação |
|------|------------------|------------------------|
| Conjurar magia | `sounds/spell_cast.ogg` | Sussurro + energia |
| Salvar inscrição na bancada | `sounds/workbench_save.ogg` | Carimbo / papel |
| Aprender glifo | `sounds/glyph_learned.ogg` | Revelação / harmônico |
| Escrever com pena | `sounds/pen_stroke.ogg` | Tinta no papel ou pedra |
| Ativar ritual | `sounds/ritual_activate.ogg` | Grave, reverb |
| Mana Poisoning | `sounds/mana_poison.ogg` | Zumbido desconfortável |
| Projétil impacto | `sounds/spell_hit.ogg` | Impacto mágico |

Depois de criar os `.ogg`, registrar em `assets/arcane/sounds.json` e chamar no código (trabalho de código, mas **gravar/escolher** o som é humano).

---

## 4. Interface e experiência do jogador

### Bancada Arcana **(P0)**

Hoje a GUI tem apenas 4 botões fixos (`+ Self`, `+ Projectile`, etc.). Falta:

- [ ] Listar **apenas glifos que o jogador conhece** (não botões hardcoded)
- [ ] Arrastar/reordenar glifos (a ordem muda o resultado)
- [ ] Feedback visual de composição inválida (vermelho, tooltip com motivo)
- [ ] Preview do custo de mana antes de salvar
- [ ] Botão “Conjurar da bancada” para testar sem sair da mesa

### HUD **(P1)**

- [ ] Barra de mana na tela
- [ ] Indicador de Mana Poisoning (corações azuis no design doc)
- [ ] Nome da inscrição ativa (qual magia a tecla **V** vai lançar)

### Grimório como interface **(P2)**

- [ ] Tela do grimório para ler descobertas, pesquisas e inscrições salvas
- [ ] Analisar magias de outros jogadores / NPCs pela interface

---

## 5. Game design — magia e balanceamento

O sistema funciona, mas os **números** foram escolhidos para teste, não para diversão final.

### Revisar manualmente **(P0)**

- [ ] Custo de mana de cada glifo em `data/arcane/glyphs/*.json`
- [ ] Dano base, duração de fogo, força de empurrão
- [ ] Regeneração de mana (`ManaState.regenPerTick`)
- [ ] Penalidade sem grimório (50% mana máx — confirmar se é suficiente)
- [ ] Multiplicadores de zona do cristal (×2 … ×32 — testar se ×32 não quebra o jogo)
- [ ] Curva de Mana Poisoning (dano, náusea, tempo de exposição)

### Criar conteúdo de glifos **(P1)**

Hoje existem 15 glifos MVP. O design original prevê dezenas/centenas. Para cada glifo novo, um humano deve decidir:

- [ ] Nome e fantasia (o que o jogador “descobre”)
- [ ] Categoria e ordem de combinação
- [ ] Custo, tier, famílias
- [ ] Se precisa de ritual ou componente físico
- [ ] Testar combinações emergentes (não criar feitiços prontos no código)

### Tintas e canetas **(P1)**

Datapacks existem parcialmente. Falta design de:

- [ ] Pena de Giz, Pena de Pedra, Pena do Ferreiro, Pena do Alquimista, etc. (todas do documento de design)
- [ ] Tinta Prismática, Permanente, Efêmera, Viva, Fantasma, do Núcleo
- [ ] Regras de balanceamento (tinta de sangue: quanto vida? quanto poder?)

---

## 6. Mundo, estruturas e progressão

### Worldgen **(P1)**

- [ ] Cristais de mana gerando naturalmente no mundo (hoje só existe bloco colocável no criativo)
- [ ] Cristais menores em cavernas
- [ ] Biomas/áreas com alta concentração de mana
- [ ] Estruturas: templos, bibliotecas, laboratórios, catedrais (cada uma com glifos temáticos)

### Dungeons e loot **(P1)**

- [ ] Integrar loot tables do mod em baús vanilla (dungeon, stronghold, etc.)
- [ ] Criar estruturas com **muralhas de glifos** que o jogador analisa
- [ ] Livros de pesquisa com pistas (texto escrito por humano, não gerado)

### NPCs **(P2)**

- [ ] Modelo e skin do Mago NPC
- [ ] Diálogos de ensino / venda de pesquisa
- [ ] Comportamento de IA mais inteligente (quando conjurar, fugir, etc.)
- [ ] Facções disputando cristais (design de missões)

---

## 7. Textos, lore e localização

### Português **(P0)**

`pt_br.json` cobre o básico. Falta:

- [ ] Descrições de item (`item.arcane.*.desc`) — lore em cada item
- [ ] Textos de descoberta graduais (“você sente calor…”, “parece um projétil…”)
- [ ] Mensagens de erro da bancada mais amigáveis
- [ ] Nomes fantasia dos glifos (não só `arcane:fire`)

### Inglês **(P2)**

- [ ] Completar `en_us.json` no mesmo nível do português

### Livros e documentos no jogo **(P1)**

- [ ] Escrever conteúdo de livros encontráveis
- [ ] Pistas de experimentação para o Research Journal

---

## 8. Rituais e construção no mundo

Sistema programado, mas a **experiência** precisa de trabalho humano:

- [ ] Desenhar templates de rituais 16×16, 32×32, 64×64 (como devem parecer)
- [ ] Definir quais rituais valem a pena construir manualmente vs. automático
- [ ] Testar geometria: o que é “simetria boa” na prática?
- [ ] Criar puzzles de dungeon usando `Ativar` + rituais armazenados
- [ ] Partículas de preview do Projetor de Glifos

---

## 9. Testes que só um humano faz

### Gameplay **(P0)**

- [ ] Montar 5–10 inscrições na bancada e conjurar — é divertido?
- [ ] A ordem dos glifos muda o resultado de forma **compreensível**?
- [ ] Morrer, perder grimório, recuperar — a penalidade é justa?
- [ ] Perto do cristal: poder demais? Mana Poisoning assusta mas é justo?

### Multiplayer **(P1)**

- [ ] Dois jogadores no servidor dedicado
- [ ] Copiar grimório / roubar grimório de NPC
- [ ] Lag com rituais grandes

### Compatibilidade **(P2)**

- [ ] Testar com outros mods populares (JEI, Curios, etc.)
- [ ] Testar em servidor com plugins (se aplicável)

---

## 10. Publicação e distribuição

### Antes de lançar publicamente **(P1)**

- [ ] Escolher licença do mod (hoje: “All Rights Reserved” no `gradle.properties`)
- [ ] Página no CurseForge / Modrinth com screenshots **reais** (precisa de arte pronta)
- [ ] Trailer ou GIFs de gameplay
- [ ] Resolver SSL/build em máquinas de CI (GitHub Actions já existe em `.github/workflows/build.yml` — validar se compila)
- [ ] Ícone e banner da página de download

### Documentação para jogadores **(P2)**

- [ ] Wiki leve ou vídeo tutorial (o mod evita wiki obrigatória por design — mas ajuda no lançamento)
- [ ] Changelog por versão

---

## 11. Resumo por prioridade

### P0 — Faça primeiro (mod “apresentável”)

1. Texturas: bancada, cristal, pena, tintas, nota de glifo
2. GUI da bancada (arte + listar glifos conhecidos)
3. Modelos JSON dos itens que faltam
4. Grimório visível (modelo mínimo)
5. Balanceamento básico de mana e custos
6. Textos pt_br com lore nos itens
7. Playtest de 1–2 horas anotando o que confunde

### P1 — Segunda leva (mod “completo”)

1. Sons
2. Partículas
3. Worldgen de cristais
4. Dungeons e loot integrado
5. HUD de mana
6. Mais glifos e tintas do design original
7. Rituais com arte e puzzles

### P2 — Polimento e lançamento

1. Grimório com estágios visuais épicos
2. NPCs com diálogo e facções
3. Modrinth/CurseForge
4. Tradução EN completa
5. Compatibilidade com outros mods

---

## 12. Onde colocar cada coisa (referência rápida)

```
src/main/resources/assets/arcane/
├── textures/
│   ├── block/          ← texturas de bloco
│   ├── item/           ← texturas de item
│   ├── gui/            ← interfaces
│   ├── entity/         ← grimório, NPC, projétil
│   └── particle/       ← partículas
├── models/             ← JSONs que apontam para as texturas
├── sounds/             ← arquivos .ogg (criar pasta)
├── sounds.json         ← registro de sons (criar arquivo)
└── lang/
    ├── pt_br.json      ← nomes e descrições
    └── en_us.json

src/main/resources/data/arcane/
├── glyphs/             ← balanceamento e novos glifos (JSON)
├── loot_tables/        ← loot em estruturas
└── worldgen/           ← geração de cristais (criar quando pronto)
```

---

## 13. O que **não** precisa ser feito manualmente

Estes já estão no código e seguem funcionando sem arte:

- Pipeline de execução de magias
- Compilador de inscrições
- Sistema de conhecimento / descoberta (lógica)
- Rede multiplayer básica
- Comandos de debug `/arcane know`
- Salvamento de grimório e mana

O trabalho manual **embeleza, equilibra e torna compreensível** — a engrenagem já existe.
