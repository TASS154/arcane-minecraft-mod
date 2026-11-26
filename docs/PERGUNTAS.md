
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

### Modificadores de Projétil (Fase J): dúvidas
- **Tier e família:** Perseguir, Ricochete e Arco no tier 2 (família força), Fragmentar no tier 3 (força e terra). Entram
  no loot das estruturas pelo padrão de tier do `gen_data.py`. Confirmar.
- **Cacos:** saem do ponto de acerto em leque, voam 1,5 s e não acertam a criatura que o orbe atingiu. Não herdam
  Perseguir, Ricochete nem Arco.
- **Ricochete em criatura:** não quica; o acerto em criatura é o final (e aí Fragmentar parte).

### Afinidades (Fase I): dúvidas
- **Elemento do efeito:** usei o de `effects.json` (por glifo), como pede o adendo. Isso faz Dano e Explosão contarem
  como arcano e fogo; o Fogo (que só acende) não fere dentro do efeito, então a imunidade só pega o que fere na hora.
  Queimar e veneno que vêm depois não passam pela afinidade. Confirmar.
- **Teto:** só limita bônus (fraqueza); se a reação sozinha já passa do teto, a afinidade fica neutra e a reação fica
  como era (não mexi em número existente).
