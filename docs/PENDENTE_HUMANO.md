# Pendente de olho, ouvido ou mão humana

Tudo que o código e os testes não conseguem confirmar. Ordem: prioridade (o que mais arrisca a experiência primeiro).
Cada item diz como ver. Marque `[x]` quando conferir e escreva o que achou ao lado. Comandos exigem cheats.

## P1: pode quebrar a sessão ou a sensação central

- [ ] **Voar invisível (crash corrigido em 25 nov):** dê-se Flutuar e Invisibilidade do glifo, voe em terceira pessoa (F5)
  por 1 minuto. Antes fechava o jogo com "Pose stack not empty". Confira também se o corpo ainda inclina ao voar.
- [ ] **Barra de chefe da Hidra some na morte (25 nov, NÃO visto):** `/arcane hydra 7`, depois `/kill @e[type=arcane:hydra]`:
  a barra deve sumir na hora e a Hidra se desfazer em runas por ~5 s.
- [ ] **Desempenho do servidor:** na sessão de 24 nov o log mostrou "Can't keep up" várias vezes (uma de 28 s). Anote o que
  estava acontecendo quando o jogo engasgar (Hidra grande? cratera? covil sendo gerado?).
- [ ] **Hidra nova andando:** `/arcane hydra 6`, em sobrevivência, deixe-a perseguir você em morro. Passada natural? As
  patas pisam no chão? O corpo sobe e desce suave?
- [ ] **Luta real contra Hidra grande** (estágio 9+): poderes legíveis? Dá para fugir da devastação? Decapitação aparece?
- [ ] **Roda R com teclado real:** andar, pular e correr com a roda aberta; escolher magia com o mouse.
- [ ] **Voo estilo Frieren:** níveis de velocidade pelo Ctrl, inclinação do corpo, aviso de fim de mana.

## P2: interface que ninguém viu

- [ ] Grimório (G) enxuto: estrela de favorita, clique ativa, Enter salva nome e pasta, botões T/C/S/A, chips 1–4, ✖.
- [ ] GUI do altar (abas Ritual e Oferendas, relatório à direita).
- [ ] `/arcane rules`: tela de regras e perfis Seguro, Épico, Sem limites.
- [ ] Tooltip da prévia na bancada (avisos de combinação inútil).
- [ ] Bolso Arcano (13 espaços, 5 travados).
- [ ] Livro-guia na primeira entrada; Projetor de Glifos (holograma); análise de resíduo.

## P3: efeitos visuais

- [ ] Formas: onda, pilar, impacto, flash de conjuração; coluna roxa (gravidade + Coluna); Petrificar (modelo em pedra).
- [ ] Correntes do Selamento, títulos do Batizar, sinais de mana vazando (círculo de raio 16 completo).
- [ ] Invisibilidade: distorção turva sem armadura nem itens.
- [ ] Estágios 5 a 7 do grimório (livro some no 6; terceiro anel no 7).
- [ ] Mortes da Hidra por glifo (runas, buraco negro do Eclipse, retroceder); covil e masmorra gerados em jogo normal.
- [ ] Hidra pegando fogo fica vermelha o tempo todo (o jogo pinta quem toma dano): decidir se incomoda.

## P3b: visual novo dos elementos (Fase C, 25 nov), NÃO visto em jogo por humano

Visto só nas fotos do roteiro automático (folha de contato em `run/screenshots/contact_sheet.png`; para refazer, escreva
`elementos` em `run/arcane_autotest.flag` e abra o cliente). Confira em jogo, de dia e de noite:
- [ ] Cada um dos 16 elementos tem cara própria (terra cai em lascas, água em gotas, luz pisca, sombra gira, sangue pinga,
  tempo gira como relógio, natureza solta folhas, som solta arcos).
- [ ] Composição: magia com dois elementos (Projétil + Fogo + Congelar) mostra o corpo do 1º e o rastro e o impacto do 2º.
- [ ] Marcas no bloco atingido (chamuscado, geada, rachadura, poça, mancha, cicatriz, flores, runa) somem em ~5 s.
- [ ] Reações: vapor (Fogo em alvo molhado), estilhaço (Fogo em alvo congelado), chiado (gelo apagando fogo).
- [ ] Brilho somado (luz, tempestade, tempo, som) não ofusca: "reduzir clarões".
- [ ] Qualidade: `fxQuality` (0,25 a 2) e `fxMaxParticlesPerCast` na config do servidor mudam a quantidade.

## P4: sons

- [ ] Todos os sons são sintetizados por `tools/gen_sounds.py`. Ouça conjuração e impacto de cada elemento e diga quais
  ficaram ruins.
- [ ] **Sons novos por elemento (25 nov, NÃO ouvidos por humano):** 16 elementos × conjuração e impacto × 3 variações, e
  vapor e estilhaço. Cada um em 3 camadas (ataque, corpo, cauda); a potência deixa mais alto e mais grave. Os de terra e
  sangue saíram os mais altos (graves); diga se incomodam. Substituíram os sons do jogo base que cada elemento usava.

## P5: sensação e multijogador

- [ ] Investida, Gancho, Atravessar, Planar: distância e sensação.
- [ ] Coro com dois clientes e latência real; voo em rede.
- [ ] Medo, Sono, Chamariz e Reanimar com mobs de IA real.
