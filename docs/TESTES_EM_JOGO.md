# Testes em jogo: observações de magias e combinações

Rodada de 8 nov 2026, num servidor real do Forge 1.20.1 (`runGameTestServer`), com **126 conjurações** de glifos combinados. Cada uma foi feita num cenário montado do zero (chão de pedra 15x15, conjurador porco olhando para +Z). Esperei o efeito acontecer (30 ticks; 70 a 110 nas zonas e no atraso) e anotei o que mudou: vida, fogo, efeitos, movimento, blocos e entidades novas. A bateria **não afirma nada**; só registra. Nenhuma conjuração falhou nem lançou exceção.

Como repetir: `JAVA_TOOL_OPTIONS=-Darcane.observe=true` e `./gradlew runGameTestServer --offline`; o relatório sai em `run/run/observacoes.md`. Código: `ArcaneObservations.java`.

**Cenas:** `mobs` = vaca a 4 blocos à frente, ovelha ao lado (3 blocos), husk (hostil) a 3 blocos atrás, todos com 60% da vida. `glass`, `ice`, `tnt`, `grass`, `sand`, `stone`, `water` = um bloco desse tipo a 4 blocos à frente. `open` = nada na mira. `openmob` = só uma vaca perto do fim do raio de alcance 8. `sky` = vaca a 3 blocos (usada nas barreiras).

## O que se confirmou funcionando

- **Efeitos em criatura:** dano, fogo, cura, veneno, lentidão, empurrar, atrair, marcar (brilho), raio, explosão, congelar, gravidade (dano de queda), corte, cegueira, definhar, levitar, velocidade, escudo e invisibilidade fazem o esperado no Raio, na Aura, na Onda, no Projétil e no Pilar.
- **Filtros:** `hostile` só pegou o husk, `animals` só vaca e ovelha, `nearest` só o mais próximo, `marked` não pegou ninguém (ninguém marcado).
- **Formatos:** domo e esfera acertam todos ao redor, cone só o que está à frente, coluna e anel acertam o que o geometria prevê. Em Barreira: parede 9 blocos, domo 57, esfera 57, anel 32, cone 13, coluna oca 112.
- **Alvos de bloco:** Dano quebra vidro (e não pedra), Escavar quebra pedra, Fogo/Acender acendem TNT, Fogo derrete gelo, Congelar vira água em gelo, Gravidade derruba areia, Cura faz grama crescer, Veneno deixa nuvem, Acender põe fogo em cima.
- **Ponto vazio (Raio sem alvo, alcance 8):** Fogo, Acender e Raio acendem ou caem no chão; Veneno deixa nuvem; Empurrar, Dano, Atrair, Cura e Explosão agem na criatura perto do ponto; Explosão sem criatura não faz nada (por desenho, sem dano em blocos).
- **Zonas:** Aura + Zona repete (dano de 1 em 1 s causou 1 a 2 de dano acumulado; com intervalo 0,5 s o dobro), Zona + Fogo manteve tudo queimando, Zona + Cura curou até o máximo (husk 12→20), Zona + Empurrar empurrou, e `Quando a Zona Acabar + Explosão` matou os três no fim. Barreira + Zona e Projétil + Zona também repetem.
- **Juramentos:** Sangue custou 4 de vida ao conjurador e a vaca morreu (bônus de dano); Quietude e Juramento Personalizado também aumentaram o dano (vaca morreu / vida 6→0).
- **Atraso:** `Atraso` aplicou o dano depois de 110 ticks (não aos 30).
- **Combinações mistas:** `Dano + Fogo + Veneno` na mesma vaca aplicou dano, fogo e veneno; `Fogo + Cura` queimou e curou (6→8).

## Achados e pontos de atenção

1. **A magia machuca o próprio conjurador em alguns casos** (por desenho, mas pode surpreender): Pilar com origem Eu + Fogo queimou quem conjurou (10→7); Pilar com origem Eu + Veneno envenenou o conjurador com a própria nuvem (10→8); Barreira com Dano (coluna ou cone, centrada em você) feriu o conjurador (10→6), porque a Barreira alcança quem está dentro, inclusive ele. **Decisão para você:** manter, ou o conjurador ficar imune às próprias nuvens e à própria barreira?
2. **Raio perto demais atinge o conjurador:** com o alvo a 3 blocos, o relâmpago (raio de 3 blocos do Trovão) transformou o porco conjurador em piglin zumbificado. É o comportamento do relâmpago de vanilla.
3. **Gelo + Fogo cria muita água:** derreter um bloco de gelo virou fonte de água que se espalhou por 83 blocos na arena. Também é vanilla (gelo derretido vira fonte), mas numa construção pode alagar.
4. **Casos que "não fazem nada" e são coerentes:** Toque a 4 blocos (alcance 3), Pilar com origem Eu e a vaca a 4 (raio 2), Cura em quem tem vida cheia, Cortar/Explosão/Congelar/Empurrar em blocos sem criatura. A prévia da bancada avisa as combinações realmente inúteis; estas dependem da distância e não são avisadas.
5. **Filtro `wounded` (Feridos) não pegou a vaca a 60% de vida:** o limite parece ser menor que 60%. Não verifiquei o valor exato.
6. **Anel de Onda (alcance 8) não pegou ninguém:** o anel é uma faixa de 8±1,5 blocos; os alvos estavam a 3 e 4. Correto, mas pouco intuitivo.
7. **Limites do teste:** foi num servidor de testes, sem cliente: **não vi** partículas, sons, animações do personagem, o tooltip da prévia nem o aspecto das zonas e barreiras na tela. O conjurador é um porco (sem mana nem grimório), então custos de mana, ritual e juramentos de jogador não foram exercidos de verdade. O "nada observado" também só vê o que a bateria mede (não vê, por exemplo, partículas).

## Tabela completa

| # | Cena | Magia | Resultado observado |
|---|---|---|---|
| 1 | mobs | `ray,damage` | cow#1 moveu 2.0 bl, vida 6->2 |
| 2 | mobs | `ray,fire` | cow#1 vida 6->4, pegando fogo |
| 3 | mobs | `ray,heal` | cow#1 vida 6->10 |
| 4 | mobs | `ray,poison` | cow#1 vida 6->4, efeito:poison |
| 5 | mobs | `ray,slow` | cow#1 efeito:slowness 2 |
| 6 | mobs | `ray,push` | cow#1 moveu 4.0 bl |
| 7 | mobs | `ray,attract` | cow#1 moveu 2.4 bl |
| 8 | mobs | `ray,mark` | cow#1 efeito:glowing |
| 9 | mobs | `ray,thunder` | cow#1 morreu; bloco ar->fire |
| 10 | mobs | `ray,explode` | cow#1 morreu; nova entidade item x2 |
| 11 | mobs | `ray,freeze` | cow#1 congelando, efeito:slowness 2 |
| 12 | mobs | `ray,gravity` | cow#1 vida 6->5 |
| 13 | mobs | `ray,cut` | cow#1 moveu 2.0 bl, vida 6->3 |
| 14 | mobs | `ray,blind` | cow#1 efeito:blindness |
| 15 | mobs | `ray,wither` | cow#1 vida 6->5, efeito:wither |
| 16 | mobs | `ray,levitate` | cow#1 moveu 1.2 bl, efeito:levitation |
| 17 | mobs | `ray,speed` | cow#1 efeito:speed 2 |
| 18 | mobs | `ray,shield` | cow#1 efeito:absorption 2 |
| 19 | mobs | `ray,invisible` | cow#1 efeito:invisibility |
| 20 | mobs | `aura,damage` | sheep#1 moveu 2.0 bl, vida 5->1; husk#1 moveu 2.0 bl, vida 12->8; cow#1 moveu 2.0 bl, vida 6->2 |
| 21 | mobs | `aura,push` | sheep#1 moveu 4.2 bl; husk#1 moveu 4.9 bl; cow#1 moveu 4.0 bl |
| 22 | mobs | `aura,attract` | sheep#1 moveu 2.4 bl; husk#1 moveu 2.2 bl; cow#1 moveu 2.4 bl |
| 23 | mobs | `aura,freeze` | sheep#1 congelando, efeito:slowness 2; husk#1 congelando, efeito:slowness 2; cow#1 congelando, efeito:slowness 2 |
| 24 | mobs | `aura,gravity` | sheep#1 vida 5->4; husk#1 vida 12->11; cow#1 vida 6->5 |
| 25 | mobs | `aura,poison` | sheep#1 vida 5->3, efeito:poison; cow#1 vida 6->4, efeito:poison |
| 26 | mobs | `aura,heal` | sheep#1 vida 5->8; husk#1 vida 12->16; cow#1 vida 6->10 |
| 27 | mobs | `aura,fire` | sheep#1 vida 5->3, pegando fogo; husk#1 vida 12->10, pegando fogo; cow#1 vida 6->4, pegando fogo |
| 28 | mobs | `wave,damage` | cow#1 moveu 2.0 bl, vida 6->2 |
| 29 | mobs | `wave,push` | cow#1 moveu 4.0 bl |
| 30 | mobs | `wave,shape_sphere,damage` | sheep#1 moveu 2.0 bl, vida 5->1; husk#1 moveu 2.0 bl, vida 12->8; cow#1 moveu 2.0 bl, vida 6->2 |
| 31 | mobs | `aura,shape_cone,damage` | cow#1 moveu 2.0 bl, vida 6->2 |
| 32 | mobs | `aura,shape_column,damage` | cow#1 moveu 2.0 bl, vida 6->2 |
| 33 | mobs | `aura,hostile,damage` | husk#1 moveu 2.0 bl, vida 12->8 |
| 34 | mobs | `aura,animals,damage` | sheep#1 moveu 2.0 bl, vida 5->1; cow#1 moveu 2.0 bl, vida 6->2 |
| 35 | mobs | `aura,nearest,damage` | husk#1 moveu 2.0 bl, vida 12->8 |
| 36 | mobs | `aura,area,damage` | sheep#1 moveu 2.0 bl, vida 5->1; husk#1 moveu 2.0 bl, vida 12->8; cow#1 moveu 2.0 bl, vida 6->2 |
| 37 | mobs | `aura,power,damage` | sheep#1 morreu; husk#1 moveu 2.0 bl, vida 12->6; cow#1 morreu; nova entidade item x3 |
| 38 | mobs | `ray,power,damage` | cow#1 morreu; nova entidade item |
| 39 | mobs | `ray,weaken,damage` | cow#1 moveu 2.0 bl, vida 6->4 |
| 40 | mobs | `ray,damage,damage` | cow#1 moveu 2.0 bl, vida 6->2 |
| 41 | mobs | `projectile,damage` | cow#1 moveu 2.0 bl, vida 6->2 |
| 42 | mobs | `projectile,explode` | cow#1 morreu; nova entidade item x2 |
| 43 | mobs | `projectile,fire` | cow#1 vida 6->4, pegando fogo |
| 44 | mobs | `touch,damage` | nada observado |
| 45 | mobs | `pillar,damage` | cow#1 moveu 2.0 bl, vida 6->2 |
| 46 | mobs | `pillar,thunder` | cow#1 morreu; bloco ar->fire |
| 47 | mobs | `pillar,origin_self,damage` | nada observado |
| 48 | mobs | `pillar,origin_aim,fire` | cow#1 vida 6->2, pegando fogo; bloco ar->fire |
| 49 | mobs | `self,speed` | conjurador efeito:speed 2 |
| 50 | mobs | `self,shield` | conjurador efeito:absorption 2 |
| 51 | mobs | `self,blink` | conjurador moveu 8.0 bl |
| 52 | mobs | `self,levitate` | conjurador moveu 1.2 bl, efeito:levitation |
| 53 | mobs | `self,heal` | nada observado |
| 54 | mobs | `aura,zone@seconds=2,damage@amount=1` | sheep#1 moveu 2.0 bl, vida 5->4; husk#1 moveu 2.0 bl, vida 12->11; cow#1 moveu 2.0 bl, vida 6->5 |
| 55 | mobs | `aura,zone@seconds=2,fire` | sheep#1 vida 5->1, pegando fogo; husk#1 vida 12->8, pegando fogo; cow#1 vida 6->2, pegando fogo |
| 56 | mobs | `aura,zone@seconds=2,on_zone_end,explode` | sheep#1 morreu; husk#1 morreu; cow#1 morreu; nova entidade item x4 |
| 57 | mobs | `aura,zone@seconds=2,heal` | sheep#1 vida 5->8; husk#1 vida 12->20; cow#1 vida 6->10 |
| 58 | mobs | `projectile,zone@seconds=2,damage@amount=1` | cow#1 moveu 2.0 bl, vida 6->5 |
| 59 | glass | `ray,damage` | bloco glass->ar |
| 60 | glass | `ray,cut` | nada observado |
| 61 | glass | `ray,explode` | nada observado |
| 62 | ice | `ray,fire` | bloco ar->water x83; bloco ice->water |
| 63 | ice | `ray,freeze` | nada observado |
| 64 | tnt | `ray,fire` | bloco tnt->ar; nova entidade tnt |
| 65 | tnt | `ray,ignite` | bloco ar->fire |
| 66 | grass | `ray,heal` | bloco ar->grass |
| 67 | grass | `ray,ignite` | bloco ar->fire |
| 68 | grass | `ray,poison` | nova entidade area_effect_cloud |
| 69 | grass | `ray,push` | nada observado |
| 70 | sand | `ray,gravity` | falling block#1 sumiu; bloco ar->sand |
| 71 | stone | `ray,dig` | bloco stone->ar; nova entidade item |
| 72 | stone | `ray,explode` | nada observado |
| 73 | stone | `ray,ignite` | bloco ar->fire |
| 74 | stone | `ray,damage` | nada observado |
| 75 | stone | `ray,freeze` | nada observado |
| 76 | water | `ray,freeze` | bloco water->ice |
| 77 | open | `ray@range=8,fire` | bloco ar->fire |
| 78 | open | `ray@range=8,ignite` | bloco ar->fire |
| 79 | open | `ray@range=8,explode` | nada observado |
| 80 | open | `ray@range=8,thunder` | bloco ar->fire |
| 81 | open | `ray@range=8,poison` | nova entidade area_effect_cloud |
| 82 | open | `ray@range=8,freeze` | nada observado |
| 83 | open | `ray@range=8,heal` | nada observado |
| 84 | open | `ray@range=8,damage` | nada observado |
| 85 | open | `ray,fire` | nada observado |
| 86 | openmob | `ray@range=8,push` | cow#1 moveu 1.1 bl |
| 87 | openmob | `ray@range=8,poison` | cow#1 vida 6->4, efeito:poison |
| 88 | openmob | `ray@range=8,damage` | cow#1 moveu 1.1 bl, vida 6->2 |
| 89 | openmob | `ray@range=8,attract` | cow#1 moveu 2.4 bl |
| 90 | openmob | `ray@range=8,heal` | cow#1 vida 6->10 |
| 91 | openmob | `ray@range=8,explode` | cow#1 morreu; nova entidade item x2 |
| 92 | open | `pillar@range=8,fire` | bloco ar->fire |
| 93 | open | `pillar@range=8,thunder` | bloco ar->fire |
| 94 | openmob | `pillar@range=8,damage` | cow#1 moveu 1.1 bl, vida 6->2 |
| 95 | openmob | `pillar@range=8,push` | cow#1 moveu 1.1 bl |
| 96 | open | `pillar@range=8,origin_self,fire` | conjurador vida 10->7, pegando fogo; bloco ar->fire |
| 97 | open | `pillar,origin_self,poison` | conjurador vida 10->8, efeito:poison; nova entidade area_effect_cloud x2 |
| 98 | mobs | `ray,damage,fire` | cow#1 moveu 2.0 bl, vida 6->1, pegando fogo |
| 99 | mobs | `ray,fire,heal` | cow#1 vida 6->8, pegando fogo |
| 100 | mobs | `ray,damage,fire,poison` | cow#1 moveu 2.0 bl, vida 6->1, pegando fogo, efeito:poison |
| 101 | mobs | `aura,attract,damage` | sheep#1 vida 5->1; husk#1 vida 12->8; cow#1 vida 6->2 |
| 102 | mobs | `aura,push,damage` | sheep#1 moveu 5.0 bl, vida 5->1; husk#1 moveu 5.0 bl, vida 12->8; cow#1 moveu 4.6 bl, vida 6->2 |
| 103 | mobs | `aura,freeze,damage` | sheep#1 moveu 2.0 bl, vida 5->1, congelando, efeito:slowness 2; husk#1 moveu 2.0 bl, vida 12->8, congelando, efeito:slowness 2; cow#1 moveu 2.0 bl, vida 6->2, congelando, efeito:slowness 2 |
| 104 | mobs | `aura,hostile,fire` | husk#1 vida 12->10, pegando fogo |
| 105 | mobs | `aura,wounded,heal` | nada observado |
| 106 | mobs | `aura,marked,damage` | nada observado |
| 107 | mobs | `aura,origin_aim,damage` | cow#1 moveu 2.0 bl, vida 6->2 |
| 108 | mobs | `wave,shape_ring,damage` | nada observado |
| 109 | mobs | `wave,shape_dome,damage` | sheep#1 moveu 2.0 bl, vida 5->1; husk#1 moveu 2.0 bl, vida 12->8; cow#1 moveu 2.0 bl, vida 6->2 |
| 110 | mobs | `ray,damage,vow_blood` | conjurador vida 10->6; cow#1 morreu; nova entidade item |
| 111 | mobs | `ray,damage,vow_stillness` | cow#1 morreu; nova entidade item x2 |
| 112 | mobs | `ray,damage,vow_oath` | cow#1 moveu 2.0 bl, vida 6->0 |
| 113 | mobs | `ray,delay,damage` | cow#1 moveu 2.0 bl, vida 6->2 |
| 114 | mobs | `aura,zone@seconds=2,poison` | sheep#1 vida 5->2, efeito:poison; cow#1 vida 6->3, efeito:poison |
| 115 | mobs | `aura,zone@seconds=2,push` | sheep#1 moveu 4.2 bl; husk#1 moveu 4.9 bl; cow#1 moveu 4.0 bl |
| 116 | mobs | `aura,zone@seconds=2&interval=0.5,damage@amount=1` | sheep#1 moveu 2.0 bl, vida 5->4; husk#1 moveu 4.4 bl, vida 12->10; cow#1 moveu 2.0 bl, vida 6->5 |
| 117 | mobs | `aura,shape_dome,zone@seconds=2,damage@amount=1` | sheep#1 moveu 2.0 bl, vida 5->4; husk#1 moveu 2.0 bl, vida 12->11; cow#1 moveu 2.0 bl, vida 6->5 |
| 118 | sky | `barrier,damage` | cow#1 moveu 1.6 bl, vida 6->2; bloco ar->arcane_barrier x9 |
| 119 | sky | `barrier,shape_dome,heal` | cow#1 vida 6->10; bloco ar->arcane_barrier x57 |
| 120 | sky | `barrier,shape_ring,damage` | cow#1 moveu 2.0 bl, vida 6->2; bloco ar->arcane_barrier x32 |
| 121 | sky | `barrier,shape_column,origin_self,damage` | conjurador moveu 1.9 bl, vida 10->6; cow#1 moveu 1.6 bl, vida 6->2; bloco ar->arcane_barrier x112 |
| 122 | sky | `barrier,shape_sphere,origin_self,heal` | cow#1 vida 6->10; bloco ar->arcane_barrier x57 |
| 123 | sky | `barrier,shape_cone,damage` | conjurador moveu 1.8 bl, vida 10->6; cow#1 moveu 2.0 bl, vida 6->2; bloco ar->arcane_barrier x13 |
| 124 | sky | `barrier,zone@seconds=2,damage` | cow#1 moveu 1.6 bl, vida 6->2; bloco ar->arcane_barrier x9 |
