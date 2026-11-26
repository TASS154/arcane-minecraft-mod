# Auditoria gráfica do Arcane (presença visual e sonora)

Data: 25 nov 2026. Padrão de qualidade: `.claude/skills/arcane-presence/SKILL.md`. Auditoria somente de leitura (nenhum código alterado).

Base: HEAD `7e8c462`, `build/reports/vanilla_fx.txt` (240 usos de `ParticleTypes`/`SoundEvents` em 40 arquivos fora da fachada `Fx`), folhas de contato de `run/screenshots/` (25 nov 14:48: `contact_sheet.png`, `contact_pairs.png`, `autotest_sheet_*.png`) e leitura dos arquivos citados. Nada aqui foi ouvido por humano: as notas de som são por leitura de código e de `tools/gen_sounds.py`.

Legenda das tags: GOLD, OK, VANILLA_CRUTCH, BOX_MASH, MISSING, REGRESSION_RISK. Notas de 1 a 5 contra a skill.

## 1. Resumo executivo

1. A geometria procedural é o ponto forte: `OriginalFxRenderer`, `BeamRenderer`, `RuneCircle`, `ShapeFxRenderer` e `ChannelFx` desenham malha própria, com fases, e respeitam "reduzir clarões".
2. O elo fraco é o som e o "resto do mundo": só os 16 elementos (via `Fx.sound`, 102 oggs) têm som próprio. Hidra, originais, invocados, rituais e quase todos os 114 efeitos usam `SoundEvents` vanilla.
3. 240 usos vanilla fora da fachada; `Fx.*` é chamado em só 10 arquivos de produção. A regra "nada de `ParticleTypes`/`SoundEvents` nos caminhos quentes" está longe de ser cumprida; o teste só relata.
4. A Hidra tem o melhor modelo do mod (9 pescoços de 13 gomos que afinam, cauda de 12 gomos, patas digitigradas, cabeças variantes, animação procedural). Os poderes dela, porém, são quase todos partícula e som vanilla (sopro, varrida, tempestade, meteoro, cura, cataclismo).
5. Zero som próprio para a Hidra: `ENDER_DRAGON_GROWL` aparece em pelo menos 11 pontos e `WITHER_SPAWN` em 5, em `HydraEntity`, `HydraPowers`, `HydraPhases`, `HydraLairs` e `SealedChambers`.
6. Os originais (teto GOLD visual) têm som vanilla em todos os 7: `BELL_RESONATE`, `WARDEN_SONIC_BOOM`, `GENERIC_EXPLODE`, `WITHER_SPAWN`, `TOTEM_USE`, `SHULKER_SHOOT`, `BEACON_POWER_SELECT` (`OriginalGlyphs.java`).
7. Os invocados (lobo e golem) são modelos válidos, mas com pernas e cauda sem taper e um halo que provavelmente não orbita (gira cubo fino no próprio eixo). Nascimento e fim sem FX próprio.
8. A folha de contato mostra o problema da identidade: projétil, aura, onda e raio são a mesma geometria com tinta; a diferença entre os 16 elementos fica quase toda na partícula. A coluna (pilar) não aparece em nenhuma das 16 linhas da folha, só o círculo e as partículas.
9. Risco de regressão a priorizar: `ParticleTypes.FLASH` enviado do servidor e do cliente (7 pontos) não passa por "reduzir clarões"; só o overlay e o `OriginalFxRenderer` respeitam a opção.
10. Recomendação: 3 sprints só de presença (sons próprios da Hidra e dos originais; poderes da Hidra e invocados em geometria; troca dos pontos quentes dos handlers e ampliação da folha de contato). Detalhe na seção 7.

## 2. Scorecard por área

| Área | Nota média | Tags dominantes |
|---|---|---|
| Geometria de FX (Original, Shape, Beam, RuneCircle, Channel) | 4,3 | GOLD, OK |
| Partículas de elemento (`fx/*`, `client/particle/*`, jsons) | 3,6 | OK |
| Projétil, rastro, impacto | 3,7 | OK |
| Hidra: modelo e animação | 4,5 | GOLD |
| Hidra: textura | 3,0 | OK |
| Hidra: poderes, fases, morte | 2,0 | VANILLA_CRUTCH, REGRESSION_RISK |
| Hidra: som | 1,0 | VANILLA_CRUTCH, MISSING |
| Invocados (lobo, golem) | 2,8 | BOX_MASH leve, MISSING |
| Magos NPC, ilusão, petrificar, invisibilidade | 3,6 | OK, GOLD (invisibilidade) |
| Grimório, selos, decals, bússola | 3,7 | OK |
| Handlers de efeito (114 glifos) | 1,9 | VANILLA_CRUTCH |
| Sons de elemento (16) | 4,0 | OK (não ouvido) |
| Sons fora de elemento | 1,5 | VANILLA_CRUTCH, MISSING |
| Voo (FloatFlight) | 3,0 | OK (movimento), VANILLA_CRUTCH (FX) |
| HUD, tema, câmera, animação do corpo | 4,0 | OK |
| Tooling de evidência (DevAutoTest, folha) | 3,0 | MISSING (cobertura) |

## 3. Tabela completa de itens

Ação: P0 (quebra a fantasia "isto é Arcane, não Minecraft com tint"), P1, P2, ou "manter".

### 3.1 Render do cliente

| Caminho | O que é | Nota | Tag | Evidência | Ação |
|---|---|---|---|---|---|
| `src/main/java/com/arcane/client/render/OriginalFxRenderer.java` | 13 FX dos originais e da Hidra: cúpula com meridianos, relógio, singularidade, esfera que vira vazio, disco de eclipse, mandala, aviso, estilhaço | 5 (visual) | GOLD | Funções `timeStop`, `rewind`, `spaceCollapse`, `annihilate`, `eclipse`, `genesis`, `gravity`, `warn`, `devastate`, `breakout`, `awaken`, `roar`, `sealBreak`, `shockwave`; `flash()` respeita `ClientPrefs.reduceFlashes()` (linha 101); partículas vanilla só como apoio (linhas 147-182). Limite de 40 FX simultâneos (linha 96). `FX_REWIND` não emite partícula nem tem som. | Manter. P1: dar som (ver `OriginalGlyphs`). P2: `debugQuads` para o escuro, conferir em shaders. |
| `.../client/render/ShapeFxRenderer.java` | Aura (parede e anéis), onda (arco), pilar (coluna do céu, coluna glitch da Gravidade), impacto (octaedro e estilhaços), ritual, holograma | 4 | OK | Geometria própria em `draw()`, `skyColumn`, `glitchColumn`, `drawRitual`. Mas o mesmo desenho serve para os 16 elementos, trocando só cor; só FORCE (glitch) e STORM/FIRE/LIFE (estilhaços) variam. Folha de contato: coluna do pilar ausente nas 16 linhas. | P0: confirmar se o pilar sumiu por regressão ou por tempo de captura. P1: variação de forma por elemento. |
| `.../client/render/BeamRenderer.java` | Raio: barra com 4 camadas, núcleo branco, círculo de runas na origem, encolhe | 4 | GOLD | `draw()`: halo, camadas e `RuneCircle`; partículas por elemento ao longo da barra. Folha: raio legível em todas as 16 linhas. Falta flash de impacto em geometria (só 2 partículas, linha 82-86 de `emit`). | Manter. P2: faixa de impacto no fim do feixe. |
| `.../client/render/RuneCircle.java` | Círculo de runas (dois anéis, 12 runas com padrões, hexagrama) | 4 | OK | `draw()`; padrão decidido por índice, parecido entre usos. | Manter. |
| `.../client/render/ChannelFx.java` | Runas de canalização que crescem com a carga; coluna prévia; shake sustentado | 4 | OK | Geometria em `render()`; `END_ROD` de apoio (linha 74); fases carga e segura distintas. | Manter. |
| `.../client/render/CompassDrawFx.java` | Frasco e pena voando até o altar e riscando o chão | 3 | VANILLA_CRUTCH | Rastro da tinta é `ParticleTypes.ENCHANT` (linhas 122-123), sem linha de luz própria; coreografia boa. | P1: fita de luz ou partícula própria na ponta. |
| `.../client/render/ArcaneProjectileRenderer.java` | Orbe billboard de 4 quadros e rastro em fita de duas camadas (cor e secundária) | 3 | OK | Rastro usa `element().color()` e `secondary()`; orbe é a mesma estrela branca de 4 pontas para 15 dos 16 elementos (só STORM tem sprite próprio). Folha `contact_sheet.png` coluna projectile. | P1: sprite ou silhueta por família (chama, cristal, gota...). |
| `.../client/render/HydraRenderer.java` | Modelo, animação e camada emissiva da Hidra | 4,5 | GOLD | `NECKS=9, SEG=13, TAIL=12`; gomos com `w = max(3.4f, w-0.25f)` e `tw*0.9` (taper); patas de 3 partes com garras; `headKind` (cabeças grandes, crista, cega); dorso muda com o glifo; `feetOnGround`; gestos ociosos; olhar com atraso por gomo. Correntes da selada e octaedros de brasa (linhas 69-98) são OK. | Manter. P2: limpar mapas estáticos `SMOOTH_Y`/`AIM` (só limpam acima de 32). |
| `.../client/render/HydraRenderer.java` (camada `Veins`) | Veias e olhos emissivos tingidos pelo glifo | 3 | OK | Redesenha o modelo inteiro com `hydra_eyes.png` (linha 140): veias brilham no corpo todo, não só em veias/olhos. Skill pede emissivo só em veia, olho, runa. | P1: máscara por região. |
| `.../client/render/SummonRenderers.java` | Renderers do lobo e do golem com camada `GlowLayer` | 3 | OK | Textura e brilho por tier (3 níveis). Sem estado de nascimento, morte ou ataque no render. | P1 |
| `.../client/model/SummonModels.java` | Animação do lobo e do golem | 2,5 | BOX_MASH | Lobo: cauda só balança (`tail.xRot/zRot`), sem respiração nem orelhas. Golem: `halo.yRot = age*0.08` em cubo 1x6x1 posicionado em x=-4/0/4 gira no próprio eixo, quase não muda a silhueta (linha ~108). | P0: halo orbitar de verdade (pivô no centro). |
| `.../client/model/SummonLayers.java` (gerado por `tools/gen_summon_models.py`) | Geometria do lobo (focinho, juba, esporões) e do golem (pelve < tronco < peito, punhos, cristais) | 3 | BOX_MASH leve | Lobo: pernas `3x6x3` e cauda `3x8x3`+`2x3x2` sem taper de cadeia; golem: braços `7x20x7` retos. Há juba, brow, cristais e assimetria nula (espelhado). Generator não impõe regras de taper ou assimetria. | P1: pernas e cauda em 3 gomos que afinam; uma placa ou chifre assimétrico. |
| `.../client/render/NPCMageRenderer.java` | Mago NPC: 18 skins, brilho animado de 6 quadros por elemento, braços erguidos ao conjurar | 3,5 | OK | `GlowLayer` com `RenderType.eyes`; sem geometria própria (é o modelo do jogador). | Manter. |
| `.../client/render/IllusionRenderer.java` | Ilusão com a skin do dono | 3 | OK | Sem translúcido nem distorção; o FX da entidade é vanilla (ver 3.3). | P2 |
| `.../client/render/PetrifyLayer.java` | Redesenha o modelo com `minecraft:textures/block/stone.png` | 3 | OK | Textura de pedra vanilla como identidade; escurecimento leve com seno. Sem rachadura nem som. | P2 |
| `.../client/render/InvisibilityRender.java` | Distorção translúcida proporcional ao movimento | 4 | GOLD | Decisão de design de Everton; `Faded` consumer; tremor por movimento. | Manter. Atenção ao crash "Pose stack not empty" já corrigido. |
| `.../client/render/GrimoireRenderer.java` | Livro vanilla `BookModel` com capa animada de 8 quadros, órbita de páginas e anéis, pena | 3,5 | OK | Capas `grimoire_s1..s4_f0..f7`; anéis luminosos em `renderOrbit`; pena é o item `Items.FEATHER` (renderOnDesk). | P2: pena própria. |
| `.../client/render/InscriptionDecals.java` | Selos de glifos na superfície | 4 | OK | Quad com ícone do glifo, pulso. | Manter. |
| `.../client/SealRenderer.java`, `.../client/SealGeometry.java` | Selo geométrico por categoria na GUI (pixel a pixel) | 3 | OK | `g.fill` de 1 px por ponto; forma por categoria. | P2 |

### 3.2 Partículas, modelos, anim, HUD, câmera

| Caminho | O que é | Nota | Tag | Evidência | Ação |
|---|---|---|---|---|---|
| `.../client/particle/ElementFxParticle.java` | Partícula de elemento com papéis BODY/TRAIL/IMPACT/DECAL/REACTION, aditiva, decal no bloco | 4 | OK | Movimento vem do json; `alpha *= 0.7` aditivo (sem clarão). | Manter. |
| `.../client/particle/ArcaneSparkParticle.java` | Partícula legada de 4 quadros | 3 | OK | Perfis `Profile.SPARK/EMBER/...`; ainda usada por cristais e escrita. | P2 |
| `src/main/resources/data/arcane/elements/*.json` | Movimento de cada elemento | 3 | OK | Os 8 novos diferem só em `gravity`, `spiral`, `jitter` e `additive`: earth 0,04, blood 0,045, water 0,035 (todos "cai"); light e sound são aditivos parados. Skill pede movimentos próprios (subir, nucleação, zigue-zague, sucção). | P1 |
| `src/main/java/com/arcane/fx/Fx.java` (fachada) | `cast`, `impact`, `burst`, `ring`, `shellSparks`, `decal`, `reaction`, `sound` com orçamento | 4 | OK | Bom desenho (som por potência, secundária no rastro). Usado em só 10 arquivos. | Manter e estender. |
| `src/main/java/com/arcane/client/anim/PlayerClips.java`, `PlayerAnimClient.java` | 6 clipes de corpo (cast direita/esquerda, finish, self, area, ritual) | 4 | OK | Keyframes encadeados; 6 clipes para 207 glifos. | P2: clipes por forma. |
| `.../client/FloatFlightClient.java` | Voo estilo Frieren: inclinação, onda de choque, rastro, FOV | 3 | VANILLA_CRUTCH | Corpo/inclinação excelentes (linhas 195-247). Onda de choque: `END_ROD`/`CLOUD`/`GLOW`, `FLASH` (linhas 150-154) e `FIREWORK_ROCKET_LARGE_BLAST` (155); rastro `DustParticleOptions` + `END_ROD` + `CLOUD` (169-171). `FLASH` sem checar `reduceFlashes`. | P0: trocar FX por geometria/`ModParticles` e respeitar a opção. |
| `.../client/CameraShake.java` | Trauma com decaimento quadrático | 4 | OK | Respeita `reduceFlashes` (linhas 26, 33). | Manter. |
| `.../client/hud/CastFlashOverlay.java` | Vinheta curta na conjuração | 4 | OK | Desliga com `reduceFlashes`. | Manter. |
| `.../client/hud/ManaHudOverlay.java`, `SpellBarOverlay.java`, `.../client/ArcaneTheme.java` | HUD de mana e tema luminoso | 4 | OK | Painel de vidro, barra com brilho, alerta de mana baixa. | Manter. |
| `.../client/DevContactSheet.java`, `DevAutoTest.java`, `tools/dev/contact_sheet.py` | Roteiro e folha elemento x forma | 3 | MISSING (cobertura) | Folha cobre 16 elementos x 5 formas e 4 pares; não cobre originais, Hidra, invocados, impactos nem sons. | P0 (ver sprint 1). |

### 3.3 Servidor: partículas e sons vanilla (fora da fachada)

| Caminho | O que é | Nota | Tag | Evidência | Ação |
|---|---|---|---|---|---|
| `src/main/java/com/arcane/entity/HydraEntity.java` | Rugidos, passos, escudo, despertar, golpes | 1,5 | VANILLA_CRUTCH | 30 usos: `WITHER_SPAWN` 338, `ENDER_DRAGON_GROWL` 339/445/639/696/889, `RAVAGER_ROAR/STUNNED/AMBIENT` 444-458, `WARDEN_DIG/STEP` 880/1029, `EXPLOSION_EMITTER` 803, `EVOKER_FANGS_ATTACK` 546, `CHAIN_HIT` 681. Passos e rugido de outros mobs. | P0: família `arcane:hydra.*`. |
| `.../entity/HydraPowers.java` | Sopro, varrida, rugido, tempestade, meteoro, cura, devastar, cataclismo, original | 2 | VANILLA_CRUTCH | BREATH: `FLAME`/`DRAGON_BREATH` + `ENDER_DRAGON_GROWL` (70, 160-180); SWEEP: `SWEEP_ATTACK`+`EXPLOSION`+`GENERIC_EXPLODE` sem geometria (81-83); STORM: `LIGHTNING_BOLT` vanilla; METEOR: `LargeFireball` vanilla (122); HEAL: `HEART`+`BEACON_POWER_SELECT` (127-128); CATACLYSM: anel de `FLAME`/`SOUL_FIRE_FLAME`+`EXPLOSION_EMITTER` (160-165). Só ROAR, DEVASTATE e ORIGINAL usam `sendShape`. | P0: geometria própria para sopro, varrida e cura; P1 para tempestade e meteoro. |
| `.../entity/HydraPhases.java` | Fase por cabeça | 1,5 | VANILLA_CRUTCH | `ENCHANT`, `EXPLOSION_EMITTER`, `WITHER_BREAK_BLOCK`, `ENDER_DRAGON_GROWL`, `WITHER_SPAWN` (75-86). | P1 |
| `.../entity/HydraDeath.java` | Morte por glifo: desfaz em runas | 3 | REGRESSION_RISK | Usa `sendShape ORIGINAL` (linha 65, bom), mas `FLASH` do servidor em 106 e 128 (ignora `reduceFlashes`), `GENERIC_EXPLODE`, `END_PORTAL_SPAWN`. | P0: trocar `FLASH` pelo flash do `OriginalFxRenderer`; som próprio. |
| `.../world/HydraLairs.java`, `.../world/SealedChambers.java`, `.../block/HydraSealBlock.java` | Covil, câmaras, selo | 2 | VANILLA_CRUTCH | `ENDER_DRAGON_GROWL`, `AMETHYST_BLOCK_BREAK`, `CHAIN_BREAK`, `UI_TOAST_CHALLENGE_COMPLETE`, `WARDEN_EMERGE`, `EXPLOSION`; `SOUL_FIRE_FLAME`, `CRIMSON_SPORE`. | P1 |
| `.../execution/handlers/OriginalGlyphs.java` | Os 7 originais | 2 (som) | VANILLA_CRUTCH | Visual com `ORIGINAL` (GOLD), som vanilla: `BELL_RESONATE` 87, `BEACON_POWER_SELECT` 120, `WARDEN_SONIC_BOOM` 141, `GENERIC_EXPLODE` 169, `WITHER_SPAWN` 197, `TOTEM_USE` 225, `SHULKER_SHOOT` 244; partículas `HEART`/`TOTEM_OF_UNDYING`/`END_ROD` de apoio. | P0: 7 sons próprios (um por original). |
| `.../execution/handlers/LifeEffects.java` | Efeitos de vida/utilidade | 1,5 | VANILLA_CRUTCH | 30 usos: `FIREWORK`+`FLASH` e dois sons de fogos (428-432), `ANVIL_USE` 99, `BUNDLE_INSERT` 110, `ITEM_PICKUP` 156, `WITCH_CELEBRATE` 478, `NOTE_BLOCK_HARP` 524, `TOTEM_OF_UNDYING` 238. | P1 |
| `.../execution/handlers/LoreEffects.java` | Efeitos "lore" | 1,5 | VANILLA_CRUTCH | `FLASH` 98, `ENDER_EYE_DEATH` 100, `PORTAL_TRIGGER`, `ENDERMAN_TELEPORT`, `CHAIN_PLACE/BREAK`, `IRON_DOOR_CLOSE`. | P1 |
| `.../execution/handlers/LowCostEffects.java` | Chuva, rajada, trovão, terremoto | 2 | VANILLA_CRUTCH | `RAIN`+`CLOUD`+`WEATHER_RAIN` 76-78, `PLAYER_ATTACK_SWEEP` 144, `LIGHTNING_BOLT_THUNDER` 215, `DAMAGE_INDICATOR`, `HEART`; já usa `Fx.ring`/`burst` em 217-218 (bom). | P1 |
| `.../execution/handlers/ControlEffects.java`, `UtilityEffects.java`, `Phase5Effects.java`, `LightEffectHandler.java`, `GrowEffectHandler.java`, `HealEffectHandler.java` | Controle, utilidade, luz, cura | 2 | VANILLA_CRUTCH | `ENCHANT`, `ENCHANTED_HIT`, `SOUL`, `SNEEZE`, `ITEM_SLIME`, `PORTAL`, `REVERSE_PORTAL`, `END_ROD`, `GLOW`, `HAPPY_VILLAGER`, `STONE_PLACE`. | P1/P2 |
| `.../execution/Phase4Service.java`, `Telekinesis.java`, `WorldMarks.java`, `ChorusService.java`, `Pierce.java`, `Wetness.java`, `CreatureLevels.java` | Serviços de fase 4, telecinese, marcas | 2 | VANILLA_CRUTCH | `ENCHANT`/`ENCHANTED_HIT`/`END_ROD` (Phase4Service 204-436); `REVERSE_PORTAL`+`BEACON_ACTIVATE/DEACTIVATE` (Telekinesis); `FLASH`+`TOTEM_USE`+`BEACON_POWER_SELECT` (WorldMarks 119-122); `CRIT` (Pierce 46); `FALLING_WATER`/`SPLASH` (Wetness). | P1 |
| `.../execution/CraterQueue.java` | Cratera da Aniquilação | 4 | OK | `sendShape ORIGINAL` (100) mais `BLOCK`/`EXPLOSION` de apoio. | Manter. |
| `.../entity/ArcaneGolemEntity.java`, `SpectralWolfEntity.java` | Efeitos dos invocados | 2,5 | VANILLA_CRUTCH | Golem: `POOF`+`END_ROD`+`GLOW` (51-64), mas `GOLEM_SLAM` e `GOLEM_HUM` próprios. Lobo: `REVERSE_PORTAL`+`ENDERMAN_TELEPORT` vanilla (67-70); sem som próprio. | P1 |
| `.../entity/IllusionEntity.java` | Ilusão | 2 | VANILLA_CRUTCH | Assinatura por `FLAME`/`SNOWFLAKE`/`ELECTRIC_SPARK`/`WITCH` (111-114), `POOF`, `ILLUSIONER_MIRROR_MOVE`. | P1 |
| `.../grimoire/GrimoireDesk.java`, `.../inscribing/*`, `.../ritual/RitualLeaks.java`, `.../restore/BlockJournal.java`, `.../item/SealedCreatureItem.java` | Bancada, inscrição, vazamento de ritual | 2,5 | VANILLA_CRUTCH | `BOOK_PAGE_TURN`, `ENCHANT`, `SMOKE`, `SOUL_FIRE_FLAME`+`BEACON_AMBIENT`, `AMETHYST_*`, `FLASH` em `SealedCreatureItem` 41. | P2 |
| `.../entity/ArcaneProjectileEntity.java` | Impacto do projétil | 3,5 | OK | Toca `SPELL_HIT` (genérico) e `Fx.impact` (som do elemento) no mesmo ponto (linhas 119-128): som duplo. Expiração visível não confirmada. | P1: tocar só o do elemento; confirmar expire. |

### 3.4 Assets e tools

| Caminho | O que é | Nota | Tag | Evidência | Ação |
|---|---|---|---|---|---|
| `src/main/resources/assets/arcane/sounds.json` e `sounds/` | 15 sons base e 34 eventos de elemento (16x2 + vapor + estilhaço), 102 oggs de elemento | 3,5 | OK | Cobre elementos; zero eventos de Hidra, originais, lobo, petrificar, invisibilidade, selos. | P0: novos eventos. |
| `tools/gen_sounds.py` | Síntese em 3 camadas (ataque, corpo, cauda) | 3,5 | OK | Receita por elemento e 3 variações. Nada ouvido por humano (STATUS). | P1: modo `--hydra`, `--originals`. |
| `src/main/resources/assets/arcane/textures/entity/hydra/hydra.png`, `tools/gen_hydra_texture.py` | Textura de obsidiana com veias | 3 | OK | Imagem lida: malha de linhas ciano por toda a UV (qualquer face pega veia) sem leitura anatômica de pescoço/cabeça/ventre. | P1: veias por região. |
| `.../textures/entity/summon/*`, `tools/gen_summon_models.py` | 3 níveis de lobo e golem | 3 | OK | Texturas e glow gerados do mesmo modelo (sem desencontro de UV). | Manter; ver BOX_MASH em 3.1. |
| `.../textures/entity/npc_mage/*`, `tools/gen_mage_skins.py` | 18 skins e 36 glows | 3,5 | OK | | Manter. |
| `.../textures/particle/*`, `particles/*.json`, `tools/gen_art_vfx.py` (`--element-fx`) | 19 famílias e 8 decals | 3 | OK | Não revisado pixel a pixel nesta rodada. | P2 |
| `tools/gen_scepter_models.py`, `gen_item_models.py` e `assets/arcane/models/item/*` | Cetros (48 aparências) e itens | 3,5 | OK | Não revisado em jogo. | P2 |
| `tools/gen_art_anim.py`, `gen_art_ritual.py`, `gen_seal_texture.py` | Capas animadas, ritual, selo | 3,5 | OK | | Manter. |
| `build/reports/vanilla_fx.txt` | Relatório do `VanillaFxUsageTest` | n/a | REGRESSION_RISK | Só relata (240 usos); não falha. | P1: teto por arquivo (baseline) para não crescer. |

## 4. Top 15 P0

1. Sons próprios da Hidra (rugido, passo, mordida, sopro, despertar, morte, quebra de selo): substituir ~30 `SoundEvents` em `HydraEntity`, `HydraPowers`, `HydraPhases`, `HydraDeath`, `HydraLairs`.
2. Sons próprios dos 7 originais em `OriginalGlyphs.java` (hoje `BELL_RESONATE`, `WARDEN_SONIC_BOOM`, `GENERIC_EXPLODE`, `WITHER_SPAWN`, `TOTEM_USE`, `SHULKER_SHOOT`, `BEACON_POWER_SELECT`).
3. Sopro da Hidra (`HydraPowers.breathTick`): cone de geometria (fita ou casca) com núcleo e fases; deixar `FLAME`/`DRAGON_BREATH` como apoio.
4. Varrida da Hidra (SWEEP): arco de geometria no chão (reaproveitar a onda do `ShapeFxRenderer`) em vez de `SWEEP_ATTACK`+`EXPLOSION`.
5. Cura da Hidra (HEAL): mandala ou anéis ascendentes em vez de `HEART`+`BEACON`.
6. `FLASH` e "reduzir clarões": `FloatFlightClient` 154, `HydraDeath` 106/128, `LifeEffects` 430, `LoreEffects` 98, `WorldMarks` 120, `SealedCreatureItem` 41. Trocar pelo flash do `OriginalFxRenderer` (que respeita a opção).
7. Onda de choque do voo (`FloatFlightClient` 150-155): geometria própria de anéis e som próprio no lugar de `END_ROD`/`CLOUD`/`GLOW`/`FLASH`/`FIREWORK_ROCKET_LARGE_BLAST`.
8. Pilar: confirmar na folha de contato por que a coluna de luz não aparece em nenhum elemento (`contact_sheet.png`, coluna pilar); se for regressão, corrigir; se for tempo de captura, ajustar `DevContactSheet`.
9. Halo do golem (`SummonModels.GolemModel`): orbitar de verdade (pivô central) e acender com o brilho.
10. Cadeias dos invocados (`gen_summon_models.py`): pernas e cauda do lobo em gomos que afinam; braços do golem em 2 gomos com junta.
11. Nascimento e fim dos invocados: FX próprio (círculo de runas, desfazer em partículas do elemento) no lugar de `REVERSE_PORTAL`/`POOF`.
12. Projétil: silhueta por família de elemento (hoje a mesma estrela branca, só STORM tem sprite próprio) em `ArcaneProjectileRenderer`.
13. Som duplo no impacto (`SPELL_HIT` + `Fx.impact`): ficar só com o do elemento.
14. Ampliar a folha de contato: originais (7), Hidra (poderes e morte), invocados, impacto e fases de Domínio/Zona; sem isso nada acima é verificável.
15. Teto no `VanillaFxUsageTest`: falhar se o total subir acima de um baseline por arquivo (hoje só gera relatório).

## 5. Top 15 P1

1. Variação de forma por elemento em aura, onda e raio (hoje só cor e partícula); começar por FIRE, FROST, STORM, VOID, LIFE.
2. Movimento próprio dos 8 elementos novos (`data/arcane/elements/{earth,water,light,shadow,blood,time,nature,sound}.json` e `ElementFxParticle`): sucção, nucleação, ondas, mandala.
3. Textura da Hidra com veias por região e emissivo em máscara (`gen_hydra_texture.py`, camada `Veins`).
4. Tempestade e meteoro da Hidra com geometria e som próprios (hoje `LIGHTNING_BOLT` e `LargeFireball` vanilla).
5. Cataclismo: anel de fogo em geometria (hoje 72 pontos de `FLAME`+`SOUL_FIRE_FLAME`).
6. Fases por cabeça (`HydraPhases`): `ENCHANT`+`EXPLOSION_EMITTER` trocados por FX do `OriginalFxRenderer`.
7. `LifeEffects` (30 usos): começar pelo bloco de fogos (428-432) e pelos sons `ANVIL_USE`/`WITCH_CELEBRATE`/`NOTE_BLOCK_HARP`.
8. `LoreEffects`, `LowCostEffects`: trocar `PLAYER_ATTACK_SWEEP`, `LIGHTNING_BOLT_THUNDER`, `WEATHER_RAIN`, `ENDER_EYE_DEATH` por eventos do mod.
9. Ilusão: sair de `FLAME`/`SNOWFLAKE`/`ELECTRIC_SPARK`/`WITCH` como assinatura; usar `Fx` com o elemento do dono e render translúcido.
10. Bússola (`CompassDrawFx`): linha de luz na ponta da pena no lugar de `ENCHANT`.
11. `Telekinesis` e `WorldMarks`: sons e FX próprios.
12. Som do lobo (rosnado, passo, nascimento) e do golem (passo): `ModSounds` + `gen_sounds.py`.
13. Petrificar: rachaduras e som de pedra própria (hoje `stone.png` vanilla + `STONE_PLACE`).
14. Camada `Veins` da Hidra: não acender o corpo inteiro.
15. `gen_sounds.py --hydra --originals`, com envelope longo, graves e cauda de reverb; revisão humana de volume relativo.

## 6. O que NÃO mexer (GOLD/OK a preservar)

- `OriginalFxRenderer`: o teto. Só somar som e a partícula de apoio de `FX_REWIND`; não reescrever.
- `BeamRenderer`, `RuneCircle`, `ChannelFx`, `skyColumn` e `glitchColumn` do `ShapeFxRenderer`.
- `HydraRenderer.HydraModel`: geometria, taper, variantes de cabeça, `feetOnGround`, gestos ociosos, atraso por gomo. Qualquer mudança deve ser textura/emissivo, não mais cubos.
- `InvisibilityRender` (decisão de Everton) e a correção da pilha de pose em `FloatFlightClient`.
- `Fx` (fachada, orçamento, sons por potência), `ElementFxParticle`, `ElementFxOptions`, jsons de elemento (só estender).
- `CameraShake`, `CastFlashOverlay`, `ArcaneTheme`, `ManaHudOverlay`, `PlayerClips`/`PlayerAnimClient`.
- `GrimoireRenderer` (capas animadas e órbita), `InscriptionDecals`, trilha em fita de `ArcaneProjectileRenderer`.
- `CraterQueue` (usa `ORIGINAL` com apoio vanilla leve) e as tabelas de som de elemento (`ModSounds.ELEMENT`).

## 7. Plano de 3 sprints curtos (só presença)

**Sprint 1: som e verdade visual (P0).** Eventos `arcane:hydra.*` e `arcane:original.*` em `ModSounds`/`sounds.json`/`gen_sounds.py`; trocar os vanilla em `HydraEntity`, `HydraPowers`, `HydraPhases`, `HydraDeath`, `OriginalGlyphs`; flash do `OriginalFxRenderer` no lugar de `FLASH` (voo, morte, `LifeEffects`, `LoreEffects`, `WorldMarks`); folha de contato estendida; teto no `VanillaFxUsageTest`.

**Sprint 2: Hidra e invocados em geometria.** Sopro (cone), varrida (arco), cura (mandala) e cataclismo (anel) via `sendShape` novos ou `OriginalFxRenderer`; halo do golem, taper do lobo e do golem, nascimento e fim dos invocados; textura da Hidra com veias por região; som do lobo e do golem.

**Sprint 3: elementos e handlers.** Silhueta de projétil por família; forma de aura/onda por elemento; movimento dos 8 elementos novos; troca dos pontos quentes de `LifeEffects`, `LoreEffects`, `LowCostEffects`, `Phase4Service`, `Telekinesis`, `WorldMarks` para `Fx`; ilusão e bússola.

Fora de escopo deste plano: balanceamento, IA da Hidra, glifos novos.

## 8. Prompt pronto para o Sprint 1 (P0)

```
Leia CLAUDE.md, .claude/skills/arcane-presence/SKILL.md e docs/AUDITORIA_GRAFICA.md. Sprint 1 (só presença, sem
mexer em balanceamento, IA ou glifos).

1) Sons próprios. Em tools/gen_sounds.py adicione --hydra e --originals (receita de 3 camadas, 3 variações) e gere:
   arcane:hydra.roar, hydra.step, hydra.bite, hydra.breath, hydra.awaken, hydra.death, hydra.seal_break;
   arcane:original.time_stop, rewind, space_collapse, annihilate, eclipse, genesis, gravity.
   Registre em registry/ModSounds.java e assets/arcane/sounds.json (com subtitle) e lang pt_br/en_us.
2) Troque os SoundEvents vanilla por esses eventos (via com.arcane.fx.Fx.sound(level, at, ResourceLocation, vol, pitch)) em:
   entity/HydraEntity.java, entity/HydraPowers.java, entity/HydraPhases.java, entity/HydraDeath.java,
   world/HydraLairs.java, execution/handlers/OriginalGlyphs.java. Volume e tom por tamanho (estágio/raio).
3) Flash e "reduzir clarões": substitua ParticleTypes.FLASH por um novo kind de flash no OriginalFxRenderer
   (que já respeita ClientPrefs.reduceFlashes) em client/FloatFlightClient.java (linha ~154), entity/HydraDeath.java
   (~106 e ~128), execution/handlers/LifeEffects.java (~430), LoreEffects.java (~98), execution/WorldMarks.java (~120),
   item/SealedCreatureItem.java (~41). Troque também FIREWORK_ROCKET_LARGE_BLAST do voo por ModSounds.FLIGHT_BOOST.
4) Folha de contato: estenda client/DevContactSheet.java e tools/dev/contact_sheet.py para uma segunda folha
   "originais_hidra" (7 originais + sopro, varrida, cura, cataclismo e morte da Hidra + lobo e golem tiers 0-2),
   disparada por "originais" em run/arcane_autotest.flag. Descubra e registre por que a coluna do pilar não aparece
   em run/screenshots/contact_sheet.png (coluna pilar).
5) VanillaFxUsageTest: adicione baseline por arquivo (a partir de build/reports/vanilla_fx.txt de hoje) e faça
   falhar se algum arquivo ou o total subir. Não toque em caminhos que já usam Fx.

Critérios de aceite (verificáveis):
- build/reports/vanilla_fx.txt: zero SoundEvents.ENDER_DRAGON_GROWL e zero WITHER_SPAWN em entity/Hydra*.java e
  world/HydraLairs.java; zero SoundEvents em execution/handlers/OriginalGlyphs.java; zero ParticleTypes.FLASH no repo.
- sounds.json declara os 14 eventos novos e cada um tem 3 .ogg em assets/arcane/sounds/ (o teste de sons existente passa).
- Com "reduzir clarões" ligado, nenhuma captura da folha "originais_hidra" tem quadro com flash de tela cheia.
- run/screenshots/ contém autotest_sheet_originais_hidra*.png e contact_originais.png com as 7 linhas de originais
  legíveis nas 3 fases (windup, sustain, resolve), e a causa do pilar sem coluna descrita em docs/STATUS.md.
- .\gradlew.bat test runGameTestServer --offline passa; nenhum GameTest existente alterado.
Marque som e visual como "NÃO visto/ouvido em jogo" em docs/PENDENTE_HUMANO.md. Um commit ao final.
```

## Notas de método e limites

- Primeira varredura foi profunda em `client/render`, `client/model`, `client/anim`, `client/hud`, `fx/*` e no relatório de usos vanilla. Segundo passe em Hidra (renderer, poderes, sons), invocados (layers, modelos, entidades), `ShapeFxRenderer` e sons (`ModSounds`, `sounds.json`, `Fx.sound`).
- Não auditado pixel a pixel: texturas de partícula, itens e cetros (`gen_art_vfx.py`, `gen_item_models.py`, `gen_scepter_models.py`); não ouvidos os oggs; GUIs (`client/screen/*`) fora do escopo gráfico de FX.
- A ausência da coluna do pilar na folha é um fato da imagem de 25 nov 14:48; a causa (regressão ou captura) não foi determinada.
