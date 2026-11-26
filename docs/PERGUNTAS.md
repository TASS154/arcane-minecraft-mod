
## Dúvidas registradas na Fase 0
- **Zonas restauradas:** voltam só quando o conjurador (jogador online ou criatura carregada) existe; se o prazo passou com o mundo fechado, somem sem disparar "Quando a zona acabar". Preferi isso a disparar efeitos fora de contexto. Confirmar.
- **Ao Entrar na Zona:** quem já estava dentro na hora de conjurar não conta como "entrou". Confirmar.
- **Convidados:** a lista é por jogador (não por inscrição) e vale também para animais domesticados. "Dono/Lista" de uma inscrição em bloco usa o conjurador de quem dispara, não de quem escreveu. Confirmar se deve guardar o autor na inscrição.
- **Cristal:** explosões têm teto absoluto (40 e 36 blocos) mesmo com x32, por segurança do servidor.

## Dúvidas registradas na Fase 1
- **Modo de conjuração "na bancada":** escolhi o Grimório (aba Magias) para não mudar o pacote de salvar da Bancada. Se for essencial na Bancada, é uma adição pequena. Confirmar.
- **Carregar:** custo = 1,5x o custo da magia, gasto aos poucos durante 3 s; a magia sai gratuita e até 3x mais forte. Se soltar antes, gasta só o que carregou e sai proporcionalmente mais fraca. Valores provisórios (`CastChannels`).
- **Segurar:** repete a cada 0,5 s pagando o custo cheio; para sem mana. Sem recarga especial.
- **Teclas 1-9** no modo Conjurador consomem as teclas da hotbar (a roda do mouse ainda troca a hotbar).
- **"Usadas recentemente"** usa a última conjuração (por qualquer modo), guardada em tempo de jogo.

## Dúvidas registradas na Fase 2
- **Glifo novo ou Flutuar?** Reaproveitei Flutuar (já era voo, "puxado pela cabeça"). Se quiser um glifo separado "Voo", é só renomear/duplicar com a tabela; o comportamento fica igual.
- **Custo:** 0,25 mana por tick por bloco/tick de velocidade (ex.: velocidade 1 = 5 mana/s; velocidade 3 = 15 mana/s). Provisório, em `flightManaPerTick`.
- **Queda segura:** uma queda, até 60 s depois do voo. Se quiser dano normal ao cair de grandes alturas quando faltar mana (punição), é uma linha.

## Dúvidas registradas na Fase 3
- **Chuva vs. Clima:** Clima já existe como ritual global. Criei Chuva como efeito local (molha, apaga fogo, rega). Se a intenção era "chuva de verdade no mundo", isso já é o Clima.
- **Investida vs. Empurrão em si mesmo:** o Empurrão em si mesmo já dá impulso para a frente. Investida difere por mirar o alvo, ter alcance (3 a 16) e ferir quem está no caminho. Se achar redundante, posso remover.
- **Planar vs. Flutuar:** Planar é queda lenta passiva (poção); Flutuar continua sendo o voo.
- **Inverter:** par Cegar/Invisibilidade e Medo/Provocar foram escolha minha; Levitação/Gravidade mapeia duração para força (usa o padrão da força). Confirmar a lista de pares.
- **Sono:** acordar por qualquer dano remove Lentidão/Fraqueza do alvo, inclusive as de outras fontes. Valores provisórios (duração máx. 30 s).
- **Ao Cair:** só dispara com queda de 3 blocos ou mais (o limite é fixo, não tem parâmetro).
- **Chuva ou Tempestade:** lê o clima do mundo (não exige estar sob o céu). Segurando Item X: só a mão principal.
- **Molhar na regra do Fogo:** o Fogo em alvo molhado ou na chuva "nem pega" (vira vapor), por regra JSON; mudei o dano do Raio com alvo molhado de +0,3 para +0,5.

## Dúvidas registradas na Fase 4
- **Não duplicados:** Telecinese já existe; Persistente é igual a Duração (dobra o tempo); Runa de Chão é Zona + Ao Entrar na Zona. Se quiser uma Runa com visual próprio, posso criar só a casca.
- **Eletrificar/Condutor:** um glifo só, com `mode` 0 (arcos de dano) ou 1 (condutor de raios). Se preferir dois glifos, é só dividir a tabela.
- **Reanimar Aliado:** interpretei como "erguer uma criatura que morreu há pouco" (até 2 min, não jogador nem chefe, máx. 2), com metade da vida e IA de apoio. Se queria reviver um aliado/jogador, é outra mecânica.
- **Encantar:** é um encanto temporário de golpes (tag no item), não um encantamento permanente do jogo; só corpo a corpo.
- **Vincular:** o significado não estava claro; escolhi "o efeito de poção acaba se o conjurador morrer ou se afastar". Confirmar.
- **Sobrecarga vs. Potência:** Potência (+50%) já existia; Sobrecarga dobra a força mas cobra 1 coração de dano mágico por efeito (nunca letal). Valores provisórios.
- **Furtivo e Falso Rastro:** só atuam no resíduo de mundo (cicatrizes) e no alarde da conjuração; não tornam a conjuração invisível a quem está olhando.
- **Ativar por Sinal:** vale em inscrições no mundo; dispara na subida do sinal e usa a criatura mais perto (6 blocos) como origem. Sem criatura por perto, nada acontece.
- **Silêncio:** quem está silenciado não consegue nem se dissipar; só o tempo (ou outro meio futuro) resolve. Dissipar em outros não tira Silêncio.
- **Escalada e Gancho no jogador:** dependem de velocidade/entrada aplicadas no servidor; podem precisar de ajuste fino no cliente.

## Revisão de 18 nov 2026 (padrões adotados)
- **Regulador de área:** Ctrl+scroll (ou scroll simples no modo Conjurador), só em magias com área. Mude se preferir outra tecla.
- **Constantes do custo:** zona até 60x, área ^1,25 com teto 12x, invocação por tamanho e quantidade. Ajuste em `SpellLimits` se ainda parecer barato.
- **Regras do mundo na tela de Opções:** só editáveis em mundo local.
- **Ritual sem forma:** vira Aura centrada no altar.
- **Golem:** sem opção de quantidade (era ignorada no custo).

## Segunda revisão de 18 nov 2026 (padrões adotados)
- **Valor das oferendas:** tabela em `OfferingValue` (provisória). Cada oferenda consome 1 unidade; slot repetido conta uma vez. Cada item é uma unidade, não a pilha inteira.
- **Lendário:** vem só do valor das oferendas (> 300%), sem exigir círculo maior nem glifo de nível 4; basta um círculo válido. Se achar forte demais, volte a exigir círculo maior.
- **Teto 1500%:** `ritualMaxPercent` (100 a 100000).
- **Cratera:** esfera de até raio 24 (mais que isso a cratera para em 24); vanilla continua para raios menores que 2,5.
- **Onda (runas):** o círculo é só uma aproximação do leque.
- **Segurar:** só Pilar fica fixo; feixe e aura seguem o conjurador.

## Terceira revisão de 18 nov 2026 (padrões adotados)
- **Itens únicos:** cabeça de dragão, ovo de dragão e elitro. Estrela do Nether NÃO é única (renovável). Quebram o teto por inteiro, não só sobem ele.
- **"Sem limite" = 0** nos tetos de ritual, cristal e carga.
- **Raio fixo das áreas:** um único interruptor (`areaHardCaps`) em vez de um por forma.

## Dúvidas registradas na Fase 5
- **Bolso, 13 espaços:** "metade de um baú pequeno" virou 13 (27 / 2 arredondado para baixo). O menu mostra 2 fileiras (18) e os 5 últimos ficam travados. Se preferir 14 ou 9, é só mudar `pocketSlots`.
- **Coro, "ao mesmo tempo":** interpretei como uma janela de 3 s (60 ticks) a partir de quem começa, com o círculo de raio 8 ao redor dele. Os outros precisam ter a mesma magia equipada. Dispara no fim da janela, para contar todo mundo. Tudo em config (`chorusRadius`, `chorusWindowTicks`, `chorusMaxParticipants`).
- **Coro e rituais/inscrições do mundo:** não valem (só conjuração direta de jogadores). Magias carregadas e NPCs também não.
- **Restaurar:** os mesmos blocos nunca voltam duas vezes; o autor da destruição não é guardado; sem recarga (o custo em mana limita). Se virar "pincel", reavaliar uma recarga curta.
- **Explosões sem loot:** vale para Explosão Destrutiva e Cadeia, Cortar e Dano em blocos frágeis. Congelar e Fogo (gelo/neve) nunca soltaram item.
- **Raridade:** Bolso, Restaurar e Coro ficaram no tier 3 (raro). Confirmar se o Bolso e o Coro devem ser mais raros ou mais comuns.

## Auditoria de glifos (25 nov 2026), detalhes em `docs/AUDITORIA_GLIFOS.md`
- **Modificadores passam do teto da bancada:** Potência, Duração, Área, Alcance e Sobrecarga multiplicam sem limite (Dano 20 com 2 Potências = 45; com Sobrecarga, 90). Devem respeitar o teto? Proposta: limitar ao máximo do parâmetro × 2.
- **Custo pelo valor antes do modificador:** o efeito é cobrado pelo valor escrito; o modificador soma um custo fixo. Só a área é recobrada pelo valor final. Recobrar tudo pelo valor final? (Isso faz Enfraquecer baratear, como a descrição dele promete; hoje ele encarece.)
- **Inverter ignora o conhecimento do glifo oposto** (Dano + Inverter cura sem conhecer Cura; Fogo vira Congelar, tier 1 → 3). Proposta: a validação exige conhecer o oposto.
- **Par de Inverter de Congelar:** hoje Fogo; a descrição de Aquecer diz "o inverso de Congelar". Qual deve ser?

## Carga do servidor (25 nov 2026), detalhes em `docs/CARGA.md`
- **Cratera sem teto trava o servidor** 5,7 s (raio 64) e 13,5 s (raio 128) num tick, no cálculo da lista. Posso calcular aos poucos (mesmo resultado no mundo)? Quer teto de 64 também no "Sem limites"?
- **Restaurar e crateras grandes:** o diário guarda 4.096 blocos; subir o teto padrão (32.768, ~3 MB salvos) ou aceitar que crateras grandes não voltam inteiras?

## Balanço automático (25 nov 2026), detalhes em `docs/BALANCO_AUTO.md`
- **Explosão:** 10 a 13 de dano por mana em área (mediana ~0,8). O custo deve crescer com o raio, ou o dano dela cair?
- **Enfraquecer** não afeta a Explosão (que não tem `amount`). Incluir `radius` (ou o dano da explosão) no que ele divide?

## Números provisórios (adendo de 25 nov: Fases H a N)

Todo número novo de balanceamento entra aqui, com o lugar onde mora (config ou JSON), o motivo e como medir no Boneco
de Treino (`/arcane dummy report` dá os valores exatos em HP). Nenhum número já existente foi mudado.

| Fase | Número | Valor | Onde | Motivo | Como medir no Boneco |
|---|---|---|---|---|---|
| H | Vida do Boneco | 40 HP (20 corações) | `ArcaneDummyEntity.MAX_HEALTH` | Barra legível de 10 corações (cada um = 4 HP); o Boneco nunca morre, então a vida só define a escala da barra | Bata até a barra esvaziar: ela para em 1 HP e o relatório soma o dano inteiro |
| H | Volta ao máximo | 5 s sem dano | `ArcaneDummyEntity.RESET_TICKS` (pedido do adendo) | Fecha a "sessão" de medida | Pare de bater; em 5 s a barra enche e a sessão fecha |
| I | Teto afinidade × reação | 2,0 | config `affinityMaxTotal` | Fraqueza + reação não passam do dobro | Perfil com fraqueza num alvo molhado (Tempestade): o relatório mostra no máximo 2x o dano escrito |
| I | Criaturas de fogo | imune a fogo; gelo e água ×1,5 | `affinities/fire_creatures.json` | Imunidade só no óbvio (blaze, ghast, magma cube, strider) | Perfil "Criaturas de fogo" no Boneco, conjure Congelar com dano e compare com "Neutro" |
| I | Criaturas do gelo | gelo ×0,5; fogo ×1,5 | `affinities/frost_creatures.json` | Stray, golem de neve, urso-polar | Idem com o perfil do gelo |
| I | Aquáticos | tempestade ×1,5; fogo ×0,75; água ×0,5 | `affinities/aquatic.json` | Peixes, afogados, guardiões | Idem |
| I | Mortos-vivos | luz ×1,5; sombra ×0,75; veneno ×0,5 | `affinities/undead.json` | Zumbis, esqueletos (tag), fantasma | Idem |
| I | Artrópodes | veneno ×0,75; fogo ×1,25 | `affinities/arthropods.json` | Aranhas, traça, abelha | Idem |
| I | Criaturas do End | vazio ×0,5; água ×1,5 | `affinities/end_creatures.json` | Enderman, shulker | Idem |
| H | Receita do Boneco | fardo de feno + 4 gravetos (" H ", "SSS", " S ") | `tools/gen_data.py` | Barata e simples, placeholder | (decisão de design) |
| J | Teto de disparos por projétil | 8 (acerto final + quiques + cacos) | config `projectileMaxTriggers` | Os quatro modificadores no máximo dariam 11; corta cacos primeiro, depois quiques | Projétil + os quatro no máximo + Dano: o relatório conta no máximo 8 golpes |
| J | Curva do Perseguir | 3° por tick por nível (1 a 3) | config `homingDegreesPerLevel` | Nível 3 fecha uma curva de 90° em 10 ticks: persegue, mas ainda dá para desviar | Boneco a 2 blocos fora da linha a 5 de distância: nível 1 erra, 3 acerta |
| J | Cone do Perseguir | 35° (meio ângulo), 16 blocos | config `homingConeDegrees`, `homingRange` | Só o que está à frente; não vira para trás | Boneco fora do cone: o orbe segue reto |
| J | Perda por quique | 25% do poder | config `ricochetPowerLoss` | 4 quiques somam 1 + 0,75 + 0,56 + 0,42 + 0,32 = 3,05 golpes de poder; quique fraco | Boneco atrás de uma parede, Ricochete 1: o golpe do quique marca 75% do direto |
| J | Poder dos cacos | soma até 1,2 do orbe; cada um no máximo 0,5 | config `fragmentTotalPower`, `fragmentMaxEach` | Fragmentar espalha, não multiplica: 20% a mais só se todos acertarem | Cinco Bonecos em leque: a soma do relatório fica abaixo de 1,2 do golpe direto |
| J | Cacos vivos por mundo | 64 | config `fragmentsPerWorld` | Proteção contra lag; o excesso não nasce | (desempenho) |
| J | Arco | gravidade 0,05 bloco/tick²; altura 1 a 12 (padrão 4); mira até 48 blocos | `ProjectileMods.ARC_GRAVITY`, glifo `arc` | O orbe cai no ponto mirado; a velocidade do glifo Projétil não vale no Arco | Mire num Boneco atrás de um muro de 3 blocos com altura 4 |
| J | Custo de mana dos glifos | Perseguir 2, Ricochete 2, Fragmentar 3, Arco 2 | `tools/glyph_table.py` | No padrão de Velocidade (1) e Potência (2) | Mana da última conjuração no letreiro do Boneco |
| K | Limiar da Nuvem: alvos molhados queimados | 3 no mesmo efeito | `interactions/default.json` (`mass_reactions`) | "Muitos" sem precisar de uma multidão | Três Bonecos molhados (Molhar) e Aura + Fogo: o relatório mostra "vapor_cloud" |
| K | Limiar da Nuvem: água no raio | 6 blocos de água a até 3 blocos do ponto | idem | Fogo jogado num lago, não numa poça | Projétil + Fogo na beira de um lago |
| K | Limiar da Nuvem: Chuva + Fogo | 1 (qualquer efeito de fogo na forma Chuva) | idem | Combinação óbvia, sempre dispara | Chuva + Fogo sobre os Bonecos |
| K | Nuvem: raio, duração | 4 blocos; 15 s (preso entre 10 e 20) | idem | Some antes de virar parede permanente | Cronometre do chiado até sumir |
| K | Nuvem: dano | 0,25 coração por segundo (0,5 HP) | idem | Leve: a nuvem atrapalha, não mata | Boneco dentro da nuvem: "Média" perto de 0,3 ❤/s |
| K | Nuvem: limites | 2 por chunk, 16 por mundo, raio máximo 8 na fusão | idem e `ReactionZones.MAX_RADIUS` | Fundir em vez de empilhar | (desempenho) |
| L | Magia grande (voz) | custo de mana ≥ 40, ou ritual | `CastingService.BIG_SPELL_MANA` | Só magias caras ganham quinta, oitava e cauda longa | (só som) |
| L | Ritmo da voz | 3 ticks entre notas; volume padrão 80% | `SpellVoice.STEP_TICKS`, config do cliente `spellVoiceVolume` | 8 notas em 1,2 s: curto o bastante para não cansar | `python tools/preview_voice.py` e ouça `tools/out/` |
| M | Maestria: tremida, falhas, cor | tremida até 0,18 bloco; falhas até 35% (nenhuma acima de 0,8); saturação 70% a 100%; desafinação até 3% | `fx/CastStyle` | Só aparência: maestria 1 é o visual de antes | Conjure com glifos Identificados e depois Dominados (só olhar) |
| M | Acabamento e acorde | a partir de maestria 0,85 | `CastStyle.polished` | Prêmio visual para quem domina a magia | (só olhar e ouvir) |
| M | Assinatura | no máximo 20% das partículas; matiz deslocado até 8% | `CastStyle.SIGNATURE_SHARE`, `MAX_HUE_SHIFT` | O elemento continua dominante | (só olhar) |
| N | Guardião: vida e ataque | 60 HP (30 corações), 5 de ataque, sem escala | `guardians/frost_warden.json` | Mini-chefe de ruína, não chefe de mundo | `/arcane guardian` e conte os golpes na janela |
| N | Guardião: janela | 8 s de dano ×1,5 depois do Vapor; depois a casca volta | idem | Recompensa quem preparou o alvo | Cronometre do estilhaço até o gelo voltar |
| N | Guardião: afinidade | gelo imune, fogo ×1,25, água ×0,5 | `affinities/ruin_guardian.json` | Lê como "criatura de gelo" | Perfil no Boneco não existe: meça no próprio guardião |
| N | Guardião: aparece | 50% dos Salões do Ritual (fixo pela posição) | `spawn_chance` no JSON | Nem toda ruína tem guarda | (exploração) |
| N | Guardião: recompensa | 2 a 4 tintas comuns, 1 a 3 gelo azul, 30% de nota de glifo até tier 3 | `reward` no JSON | Modesta; glifo original nunca | (loot) |

### Modificadores de Projétil (Fase J): dúvidas
- **Tier e família:** Perseguir, Ricochete e Arco no tier 2 (família força), Fragmentar no tier 3 (força e terra). Entram
  no loot das estruturas pelo padrão de tier do `gen_data.py`. Confirmar.
- **Cacos:** saem do ponto de acerto em leque, voam 1,5 s e não acertam a criatura que o orbe atingiu. Não herdam
  Perseguir, Ricochete nem Arco.
- **Ricochete em criatura:** não quica; o acerto em criatura é o final (e aí Fragmentar parte).

### Reações em massa e Diário (Fase K): dúvidas
- **"Mesma conjuração":** o limiar é contado por efeito de uma conjuração (Aura + Fogo em 3 molhados = 3 vapores). Dois
  efeitos de fogo na mesma magia não somam. Confirmar.
- **Zonas:** a Nuvem não entra na lista de zonas das magias (`Zones`), porque essas contam no limite de 2 zonas por
  conjurador e a Nuvem apagaria a zona do jogador. Usei um gerenciador próprio com o mesmo padrão de pulso
  (`ReactionZones`). Decisão pequena, fácil de mudar.
- **Diário:** salvo no mundo (`arcane_reaction_journal`), por jogador. As reações antigas ganharam ids e nomes
  (Chuva Carregada, Condução, Vapor, Congelamento Súbito, Estilhaço, Degelo, Chiado, Florada). Os nomes são meus.
- **Quem descobre:** só quem conjurou a magia que causou a reação. Reação sem conjuração (fogo que já estava queimando)
  não conta para ninguém.

### Afinidades (Fase I): dúvidas
- **Elemento do efeito:** usei o de `effects.json` (por glifo), como pede o adendo. Isso faz Dano e Explosão contarem
  como arcano e fogo; o Fogo (que só acende) não fere dentro do efeito, então a imunidade só pega o que fere na hora.
  Queimar e veneno que vêm depois não passam pela afinidade. Confirmar.
- **Teto:** só limita bônus (fraqueza); se a reação sozinha já passa do teto, a afinidade fica neutra e a reação fica
  como era (não mexi em número existente).

## Redesign de formas, Mais Próximo e poda de glifos (prompt 3, 26 nov 2026)

### Números provisórios novos

| Número | Valor | Onde | Motivo | Como medir no Boneco |
|---|---|---|---|---|
| Bônus de contato do Toque | ×1,25 nos efeitos (parâmetros de magnitude: dano, cura, duração...) | config `touchPowerBonus` (1 a 2) | O pedido diz 1,25 a 1,5; comecei pelo menor para o Toque não engolir o Raio a 3 blocos | Boneco a 2 blocos: `Toque → Dano 4` marca 5 HP no relatório; `Raio → Dano 4`, 4 HP |
| Alcance da busca do Mais Próximo como entrega | até 48 blocos (o alcance da forma, se menor); Projétil 32 | `Seek.MAX_RANGE`, `Seek.PROJECTILE_RANGE` | O Raio "sem limite" (512) e o Pilar não podem varrer o mundo atrás de criaturas | Boneco a 40 e a 50 blocos com `Raio → Mais Próximo → Dano`: só o de 40 |
| Blocos do fuste da Coluna | até 32 blocos acima da marca no chão | `PillarFormHandler.SHAFT_BLOCK_HEIGHT` | O fuste visual vai ao céu; varrer blocos até o limite do mundo custaria caro | (mundo) |
| Detonação de fim de vida | só com 32 blocos carregados em volta do orbe | `ArcaneProjectileEntity.EXPIRE_LOADED_RADIUS` | Não gerar chunks no meio do tick (segurança do servidor, não balanço) | (desempenho) |

### Decisões pequenas que tomei

- **Ordem do Mais Próximo:** "logo após a forma" ignora qualquer modificador (de entrega ou de efeito) no meio; um
  filtro, efeito, condição ou ativador antes dele o devolve ao papel de filtro. Formas que não miram (Eu, Barreira,
  Invocar) mantêm o filtro e a prévia avisa.
- **Mais Próximo como entrega inclui aliados e bichos** (como o filtro sempre fez). Suportes de armadura não contam.
  Para mirar só inimigos: `Raio → Hostis → Mais Próximo` (filtro) ou `Raio → Mais Próximo → Hostis` (entrega, depois
  filtro: se o mais perto for um aliado, nada acontece).
- **Falhas de forma antes da mana, num lugar só** (`CastingService.formFailure`): Foco sem criatura, Coluna sem chão,
  Raio que não pega criatura nem bloco, e Mais Próximo sem ninguém à vista. Vale para jogador e para criaturas que
  conjuram. Inscrições no mundo, zonas e magias armadas não passam por ali: nelas a forma só entrega vazio.
- **Raio vazio com efeitos que não dependem de alvo** (só Salto, Bolso, Clima...): não falha, como antes.
- **Toque sem alvo continua gastando mana** (só o Raio falha no vazio, como o pedido diz). Se quiser o mesmo padrão no
  Toque, é uma linha em `formFailure`.
- **Privilégio do Toque:** ao tocar uma criatura, a mão chega também ao bloco logo atrás dela na mesma linha (dentro dos
  3 blocos). Escolhi este entre os três sugeridos porque é o único que o Raio não pode copiar (ele para no primeiro).
- **Projétil:** o orbe que vence o tempo de vida detona no ponto onde está (efeitos de ponto agem ali); "Ao Atingir" e
  "Ao Bater" não disparam, porque não houve acerto. Os cacos do Fragmentar só somem.
- **Coluna:** os blocos entregues são o terreno exposto dentro do círculo, do chão até 32 blocos acima, de baixo para
  cima. Cavar quebra no máximo 4 por conjuração, como sempre: numa Coluna, os de baixo primeiro.
- **Aliases** (`GlyphAliases.APPROVED`, tabela em `docs/AUDITORIA_GLIFOS.md`): Planar → Pena; Atraso → Após Segundos;
  efeito Chuva → Molhar (perde o raio); Carinho → Cura com 4 de vida (Carinho só curava animais; a Cura vale para
  qualquer criatura); Ferido e Inverter saem da inscrição (dropados). Uma magia salva com `Aura → Ferido → Dano` passa a
  ferir todos na aura; uma com `Dano → Inverter` volta a ferir em vez de curar. Não achei substituto honesto para
  nenhum dos dois.
- **Atraso → Após Segundos:** a magia antiga fica 1 de mana mais barata, e um Grupo aberto antes do atraso deixa de ser
  alcançado por um modificador depois dele (a condição fecha o grupo). Caso raro, registrado na auditoria de 25 nov.
- **Vincular** (uma frase): prende à sua presença um efeito de poção que você pôs em outra criatura; ele acaba quando
  você morre, sai do mundo ou fica a mais de 40 blocos dela.

### Corte: proposta de números (B4). PARADO esperando aprovação

O desenho aprovado: dano caro, alvo único, sem Área nem Cadeia, bônus proporcional à armadura do alvo. **Nenhum número
foi mudado no código.** A regra que não pede número já vale: o Corte **não tem raio** (Área não age nele) e **não está
na lista do Cadeia** (`EffectReach.CHAINABLE` = Dano, Explosão, Explosão Destrutiva). O "alvo único" (numa Aura, só a
criatura mais perto de você leva o Corte) eu **não** liguei: muda o que magias salvas fazem hoje e é parte do mesmo
pacote; entra junto com os números.

Conta feita com a fórmula de armadura do Minecraft 1.20.1 (`CombatRules.getDamageAfterAbsorb`: armadura e resistência
da armadura; ferro 15/0, diamante 20/8, netherite 20/12). Não rodei o Boneco: os valores são exatos pela fórmula, e o
relatório do Boneco deve bater com eles (`/arcane dummy report`, clique para trocar a armadura).

Dano efetivo (HP) por conjuração:

| Magia | Mana | Sem armadura | Ferro | Diamante | Netherite |
|---|---|---|---|---|---|
| Dano 4 (hoje) | 2 | 4,00 | 1,92 | 0,96 | 0,93 |
| Corte 3 (hoje) | 2 | 3,00 | 1,38 | 0,69 | 0,67 |
| **Proposta A:** Corte 6, +4% por ponto de armadura do alvo, custo 4 | 4 | 6,00 | 5,68 | 3,33 | 3,09 |
| Proposta B: Corte 3, +5% por ponto, custo 3 | 3 | 3,00 | 2,65 | 1,56 | 1,49 |
| Proposta C: Corte 6, ignora metade da armadura, custo 4 | 4 | 6,00 | 4,92 | 3,96 | 3,89 |

- O bônus de A e B multiplica o dano **antes** da armadura: `dano × (1 + k × armadura)`, com armadura de 0 a 20 (teto
  ×1,8 em A). A armadura ainda reduz; o Corte só chega mais perto do dano escrito.
- C é mais forte que A contra diamante e netherite e quase igual contra ferro; é "perfurar", não "proporcional à
  armadura" como o desenho pede.
- **Recomendação: A.** Contra alvo sem armadura o Corte rende 1,5 HP por mana (o Dano rende 2): pior. Contra netherite,
  0,77 HP por mana (o Dano, 0,47): melhor. Isso dá ao Corte um nicho claro ("abre armadura") sem virar o novo Dano. Alvo
  único e sem Área impedem Aura + Corte de limpar uma sala.
- O padrão do parâmetro `amount` passaria de 3 para 6 (intervalo 1 a 16, igual), e o custo do glifo de 2 para 4. O `k`
  ficaria na config (`cutArmorBonusPerPoint`, provisório).
- Perguntas: (1) A, B, C ou outra? (2) Ligar o alvo único junto? (3) O bônus vale contra a armadura natural de mobs
  (zumbi tem 2)? Recomendo sim: é o mesmo atributo `ARMOR` que a armadura vestida soma.
