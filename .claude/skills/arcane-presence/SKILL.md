---
name: arcane-presence
description: >-
  Padrão de presença visual/sonora do mod Arcane (Minecraft). Use ao criar ou
  revisar FX, partículas, sons, modelos de criatura/item, ShapeFx, projéteis,
  Hidra, invocados, originais, ou ao auditar se um efeito “parece vanilla”.
---

# Arcane Presence

## Quando usar
- Novo ou alterado: renderer, partícula, som, modelo (Hydra, summon, cetro, item 3D), ShapeFx, ChannelFx, projétil, originais.
- Review / auditoria gráfica.
- Qualquer efeito que hoje só chama `ParticleTypes.*` ou `SoundEvents.*`.

## Fonte da verdade visual
**Teto (GOLD):** `client/render/OriginalFxRenderer.java` — geometria procedural (cúpulas, meridianos, discos, colunas, vinhetas), fases claras, pouco spam de partícula vanilla.

**Bons vizinhos:** `BeamRenderer`, `RuneCircle`, partes geométricas de `ShapeFxRenderer` / `ChannelFx` quando desenham malha/anel, não só `addParticle`.

**Não usar como desculpa de qualidade:** GameTest verde, “compila”, “parece o ENCHANT do baú”.

## Verbo do efeito = presença
Todo FX de magia relevante tem fases:
1. **Windup** — aparece / carrega
2. **Sustain** — mantém identidade enquanto dura/canaliza
3. **Resolve** — impacto / sumiço / estilhaço

Tap pode comprimir; Hold/Zona/Domínio precisam sustain legível.

## Ban list (identidade)
Proibido como **assinatura** principal de um elemento/forma/efeito novo ou revisado:
- `ParticleTypes.CLOUD`, `ENCHANT`, `EXPLODE`, `FLASH`, `END_ROD`, `CRIT`, `SMOKE` como único look
- Novos mapeamentos de elemento → `SoundEvents.*` vanilla (a antiga `ElementSounds` foi removida em 25 nov; use `com.arcane.fx.Fx.sound` e os sons de `tools/gen_sounds.py --elements`)
- Modelo de criatura = pilha de cubos do mesmo tamanho sem taper, gap, assimetria ou emissivo

Permitido: partícula vanilla como **faísca de apoio** (≤30% do look) se o núcleo for geometria/`ModParticles`/som próprio.

## Obrigatório em FX novo/revisado
1. Geometria própria (anel, disco, fita, feixe, meridianos, glitch column, etc.) **ou** partícula de elemento (`com.arcane.fx.Fx`, `ModParticles.FX`) com movimento do elemento.
2. Som em `sounds.json` + asset via `tools/gen_sounds.py` (ou .ogg real com mesmo nome).
3. Cor do `SpellElement` primária; se a magia tiver 2 elementos, usar o segundo (`SpellElement.trailOf` / `secondaryColor`) no rastro/casca/runas.
4. Respeitar opção “reduzir clarões” (sem flash/vinheta agressiva).
5. Evidência: DevAutoTest / screenshot em `run/screenshots/` (folha de contato: `elementos` em `run/arcane_autotest.flag`) — senão descrever o que deveria aparecer em 3 fases.

## Modelos (criaturas / invocados / Hidra)
- Silhueta legível à distância (ler o que é com blur).
- Cadeias que **afinam** (pescoço, cauda, patas): gomos com escala decrescente.
- Negative space (vão entre mandíbula/chifres/dedos).
- Assimetria leve (uma crista, chifre, placa cega) > ornamento simétrico demais.
- Emissivo só em veias/olhos/runas — não o corpo inteiro.
- Tools: `gen_summon_models.py`, `gen_hydra_texture.py` — gerar com constraints acima, não “mais boxes”.
- Animação procedural > T-pose; pescoços/olhares com atraso por gomo quando houver mira.

## Formas — presença alinhada ao verbo
| Forma | Deve parecer |
|-------|----------------|
| Projétil | Carga com trajetória + rastro; impacto; **expire** visível se detonar no fim da vida |
| Raio | Feixe hit-scan; impacto em bloco/criatura; **sem** detonação no ar vazio |
| Foco | Lock em criatura (anel/runa no alvo), não feixe de terreno |
| Toque | Curto, contato, flash local |
| Coluna | Fuste do chão ao céu / multi-Y; não “Raio pintado de vertical” |
| Aura/Onda/Barreira | Volume/cone/casca legíveis; Domínio = abertura + pulso + quebra |

## Elementos
16 elementos com cor primária/secundária (`data/arcane/elements/<id>.json`). Os 8 novos não podem só recolorir partícula antiga para sempre — movimento próprio (subir, nucleação, zig-zag, sugção, etc.).

Bíblia mínima ao tocar um elemento:

| Elemento | Movimento | Som | Luz |
|----------|-----------|-----|-----|
| Fogo | Línguas + brasas orbitais | Crepitar / whoosh | Núcleo branco |
| Gelo | Nucleação / rachadura | Vidro seco | Azul frio |
| Tempestade | Zig-zag + afterimage | Estática curta | Flash 1 frame |
| Vazio | Sugção / silhueta oca | Muffled + click | Negro + borda |
| Força | Glitch / tear | Boom abafado + ring | Roxo |
| Vida | Mandala / pétalas | Chime | Dourado suave |

## Sons
- Sons de elemento passam por `Fx.sound` (eventos do mod); nada de remapear `SoundEvents` vanilla com pitch.
- Volume/pitch por **tamanho da magia** (custo/carga) quando tocar cast/impacto.
- Furtivo (`stealth`) continua sem som/partículas de conjuração.

## Checklist de PR / entrega visual
- [ ] 3 fases (ou justificativa de tap)
- [ ] Sem ban list como identidade
- [ ] Cor elemento (+ secundária se 2)
- [ ] Som próprio registrado
- [ ] Modelo: silhueta + taper + negative space (se aplicável)
- [ ] Reduzir clarões respeitado
- [ ] Evidência visual (screenshot/DevAutoTest) ou “só mecânica, presença pendente” explícito no PR/doc

## Anti-padrões a rejeitar
- “Adicionei partículas laranja = fogo pronto”
- “Remapeei FIRECHARGE com pitch”
- “Hidra maior = scale++ nos mesmos cubos”
- “Originais são especiais demais para o resto copiar a barra”
- Marcar presença feita porque o GameTest passou

## Saída ao auditar
Classificar: GOLD | OK | VANILLA_CRUTCH | BOX_MASH | MISSING | REGRESSION_RISK
Priorizar P0 = o que mais quebra a fantasia “isto é Arcane, não Minecraft com tint”.
