"""Tabela unica dos glifos do mod Arcane.

Fonte de: data/arcane/glyphs/*.json, nomes/descricoes nos arquivos de idioma e icones
(textura 16x16 com simbolo 8x8). Edite aqui e rode:  python tools/gen_glyphs.py

Campos:
  cat      form | effect | filter | modifier | condition | activator
  cost     mana base
  params   {nome: (padrao, minimo, maximo, tipo)}  (o servidor limita os valores a este intervalo)
  magnitude  parametros que o potencial ambiental (cristal) e a precisao amplificam
  scope    escopo do modificador (ver ModifierScope)
  comp     composicao (so formas: max 1, posicao inicial)
  pt / en  (nome, descricao)
  icon     8 linhas de 8 caracteres; '#' acende o pixel
"""

G = {}


def glyph(gid, cat, cost, pt, en, icon, params=None, magnitude=None, scope=None, comp=None, tier=1, ritual=0, components=None):
    G[gid] = dict(cat=cat, cost=cost, pt=pt, en=en, icon=icon, params=params or {}, magnitude=magnitude or [],
                  scope=scope, comp=comp, tier=tier, ritual=ritual, components=components or [])


FORM_COMP = {"max_per_inscription": 1, "position": "start"}

# ------------------------------------------------------------------ FORMAS
glyph("self", "form", 1,
      ("Eu", "Forma: o feitiço acontece em você."),
      ("Self", "Form: the spell happens on you."),
      ["..####..", ".#....#.", "#..##..#", "#.#..#.#", "#.#..#.#", "#..##..#", ".#....#.", "..####.."], comp=FORM_COMP)
glyph("touch", "form", 2,
      ("Toque", "Forma: afeta o que estiver na sua mira, bem perto (3 blocos)."),
      ("Touch", "Form: affects whatever you aim at, up close (3 blocks)."),
      ["..#.#...", "..#.#.#.", "..#.#.#.", "#.#####.", "#######.", ".######.", "..####..", "..####.."],
      params={"range": (3.0, 1.0, 6.0, "float")}, comp=FORM_COMP)
glyph("projectile", "form", 3,
      ("Projétil", "Forma: dispara um orbe que aplica o feitiço onde acertar (criatura ou bloco)."),
      ("Projectile", "Form: fires an orb that applies the spell where it lands (creature or block)."),
      ["........", "....#...", "....##..", "########", "########", "....##..", "....#...", "........"],
      params={"speed": (1.5, 0.5, 3.0, "float")}, magnitude=["speed"], comp=FORM_COMP)
glyph("ray", "form", 3,
      ("Raio", "Forma: um feixe instantâneo que acerta a primeira criatura ou bloco na mira."),
      ("Ray", "Form: an instant beam that hits the first creature or block you aim at."),
      ["......##", "....###.", "...###..", "..###...", ".###....", "###.....", "##......", "#......."],
      params={"range": (24.0, 4.0, 48.0, "float")}, comp=FORM_COMP)
glyph("aura", "form", 4,
      ("Aura", "Forma: afeta todas as criaturas ao seu redor (menos você)."),
      ("Aura", "Form: affects every creature around you (except you)."),
      ["#..##..#", ".#.##.#.", "..####..", "########", "########", "..####..", ".#.##.#.", "#..##..#"],
      params={"radius": (5.0, 2.0, 12.0, "float")}, comp=FORM_COMP)

# ------------------------------------------------------------------ EFEITOS
glyph("damage", "effect", 2,
      ("Dano", "Efeito: fere o alvo. A criatura atingida passa a perseguir você."),
      ("Damage", "Effect: hurts the target. The creature will turn on you."),
      ["#......#", ".#....#.", "..#..#..", "...##...", "...##...", "..#..#..", ".#....#.", "#......#"],
      params={"amount": (4.0, 1.0, 20.0, "float")}, magnitude=["amount"])
glyph("fire", "effect", 2,
      ("Fogo", "Efeito: incendeia criaturas. Em blocos, nada acontece (use Incendiar)."),
      ("Fire", "Effect: sets creatures ablaze. Does nothing to blocks (use Ignite)."),
      ["...#....", "...##...", "..###.#.", ".#####..", ".######.", "########", ".######.", "..####.."],
      params={"duration": (60.0, 20.0, 200.0, "int")}, magnitude=["duration"])
glyph("push", "effect", 2,
      ("Empurrão", "Efeito: arremessa o alvo para longe de você. Em si mesmo, é um impulso para a frente."),
      ("Push", "Effect: knocks the target away from you. On yourself, it is a forward dash."),
      ["#...#...", ".#...#..", "..#...#.", "...#...#", "...#...#", "..#...#.", ".#...#..", "#...#..."],
      params={"strength": (1.0, 0.3, 3.0, "float")}, magnitude=["strength"])
glyph("heal", "effect", 3,
      ("Cura", "Efeito: recupera vida do alvo."),
      ("Heal", "Effect: restores the target's health."),
      ["...##...", "...##...", "...##...", "########", "########", "...##...", "...##...", "...##..."],
      params={"amount": (4.0, 1.0, 20.0, "float")}, magnitude=["amount"])
glyph("slow", "effect", 2,
      ("Lentidão", "Efeito: o alvo anda devagar por um tempo."),
      ("Slow", "Effect: the target moves slowly for a while."),
      ["#..##..#", ".#.##.#.", "..####..", "###..###", "###..###", "..####..", ".#.##.#.", "#..##..#"],
      params={"duration": (100.0, 20.0, 600.0, "int"), "level": (1.0, 0.0, 3.0, "int")}, magnitude=["duration"])
glyph("poison", "effect", 2,
      ("Veneno", "Efeito: envenena o alvo, ferindo-o aos poucos (não mata)."),
      ("Poison", "Effect: poisons the target, wearing it down (never kills)."),
      ["...##...", "..####..", ".######.", ".#.##.#.", ".######.", "..#..#..", "..####..", "...##..."],
      params={"duration": (100.0, 20.0, 400.0, "int"), "level": (0.0, 0.0, 2.0, "int")}, magnitude=["duration"])
glyph("speed", "effect", 2,
      ("Velocidade", "Efeito: o alvo corre mais rápido por um tempo."),
      ("Speed", "Effect: the target runs faster for a while."),
      ["#.......", "##......", "###..#..", "####.##.", "#####.##", "######.#", "..####.#", "....##.#"],
      params={"duration": (200.0, 20.0, 1200.0, "int"), "level": (1.0, 0.0, 3.0, "int")}, magnitude=["duration"])
glyph("levitate", "effect", 2,
      ("Levitação", "Efeito: o alvo flutua para cima por poucos segundos."),
      ("Levitate", "Effect: the target floats upward for a few seconds."),
      ["...##...", "..####..", ".######.", "...##...", "...##...", "........", ".######.", "........"],
      params={"duration": (40.0, 10.0, 200.0, "int")}, magnitude=["duration"])
glyph("shield", "effect", 3,
      ("Escudo", "Efeito: concede corações de absorção ao alvo."),
      ("Shield", "Effect: grants absorption hearts to the target."),
      [".######.", "########", "##.##.##", "##.##.##", "##.##.##", ".######.", "..####..", "...##..."],
      params={"duration": (600.0, 100.0, 2400.0, "int"), "level": (1.0, 0.0, 4.0, "int")}, magnitude=["duration"])
glyph("thunder", "effect", 6,
      ("Trovão", "Efeito: um raio cai sobre o alvo. Cuidado: o fogo se espalha."),
      ("Thunder", "Effect: a lightning bolt strikes the target. Beware of the fire."),
      ["...###..", "..###...", ".###....", "#######.", "...###..", "..###...", ".##.....", "##......"])
glyph("explode", "effect", 6,
      ("Explosão", "Efeito: explode no alvo sem destruir blocos. Fere quem estiver perto, inclusive você."),
      ("Explosion", "Effect: explodes at the target without breaking blocks. Hurts everyone nearby, you included."),
      ["#..#..#.", "..#.#...", ".#.#.#.#", "#.###.#.", "..###...", "#.###.#.", ".#.#.#.#", "#..#..#."],
      params={"radius": (2.0, 1.0, 6.0, "float")})
glyph("blink", "effect", 3,
      ("Salto", "Efeito: você é teleportado para o ponto que está mirando."),
      ("Blink", "Effect: you teleport to the spot you are aiming at."),
      ["#.#.....", ".#.#....", "..#.#...", "...#.#..", "...#.#..", "..#.#...", ".#.#....", "#.#....."],
      params={"distance": (8.0, 2.0, 24.0, "float")})
glyph("ignite", "effect", 2,
      ("Incendiar", "Efeito: acende fogo no bloco alvo, se houver espaço livre acima."),
      ("Ignite", "Effect: lights a fire on the target block, if there is free space above."),
      ["...#....", "..##....", ".###.#..", ".####.#.", ".#####.#", "########", "########", "########"])
glyph("dig", "effect", 3,
      ("Escavar", "Efeito: quebra o bloco alvo se for mole (terra, areia, madeira, pedra comum). Não quebra baús."),
      ("Dig", "Effect: breaks the target block if it is soft (dirt, sand, wood, plain stone). Never breaks chests."),
      ["..#####.", ".#....#.", "......##", "....#.##", "...#..#.", "..#.....", ".#......", "#......."],
      params={"power": (2.0, 0.5, 5.0, "float")})

# ------------------------------------------------------------------ FILTROS
glyph("hostile", "filter", 1,
      ("Hostis", "Filtro: só afeta criaturas hostis."),
      ("Hostile", "Filter: only affects hostile creatures."),
      ["#......#", "##....##", ".######.", ".#.##.#.", ".######.", "..#..#..", "..####..", "........"])
glyph("self_filter", "filter", 0,
      ("Somente Eu", "Filtro: restringe o efeito a você."),
      ("Only Me", "Filter: restricts the effect to you."),
      [".######.", "#......#", "#..##..#", "#.####.#", "#.####.#", "#..##..#", "#......#", ".######."])
glyph("blocks", "filter", 1,
      ("Blocos", "Filtro: mira blocos em vez de criaturas."),
      ("Blocks", "Filter: targets blocks instead of creatures."),
      ["########", "#..#...#", "#..#...#", "########", "#...#..#", "#...#..#", "########", "........"])
glyph("players", "filter", 1,
      ("Jogadores", "Filtro: só afeta jogadores."),
      ("Players", "Filter: only affects players."),
      ["..####..", "..####..", "..####..", ".######.", "#.####.#", "..#..#..", "..#..#..", "..#..#.."])
glyph("animals", "filter", 1,
      ("Animais", "Filtro: só afeta animais."),
      ("Animals", "Filter: only affects animals."),
      ["#.#..#.#", "#.#..#.#", "..####..", ".######.", "########", "########", ".#....#.", ".#....#."])
glyph("undead", "filter", 1,
      ("Mortos-vivos", "Filtro: só afeta mortos-vivos (zumbis, esqueletos...)."),
      ("Undead", "Filter: only affects the undead (zombies, skeletons...)."),
      [".######.", "########", "#.####.#", "#.#..#.#", "########", "..####..", "..#..#..", "..####.."])
glyph("not_self", "filter", 0,
      ("Exceto Eu", "Filtro: tira você do alvo."),
      ("Not Me", "Filter: removes you from the targets."),
      [".######.", "#......#", "#.....##", "#....#.#", "#...#..#", "#..#...#", "##.....#", ".######."])
glyph("nearest", "filter", 1,
      ("Mais Próximo", "Filtro: só a criatura mais próxima de você."),
      ("Nearest", "Filter: only the creature closest to you."),
      ["...##...", "...##...", "...##...", "###..###", "###..###", "...##...", "...##...", "...##..."])
glyph("wounded", "filter", 1,
      ("Ferido", "Filtro: só criaturas com menos da metade da vida."),
      ("Wounded", "Filter: only creatures below half health."),
      [".##..##.", "####.###", "###.####", "####.###", ".###.##.", "..####..", "...##...", "........"])

# ------------------------------------------------------------------ MODIFICADORES
glyph("area", "modifier", 2,
      ("Área", "Modificador: dobra o raio da forma e do efeito anteriores (Aura, Explosão)."),
      ("Area", "Modifier: doubles the radius of the form and effect (Aura, Explosion)."),
      ["#..##..#", "........", "..####..", "#.#..#.#", "#.#..#.#", "..####..", "........", "#..##..#"],
      scope={"target": "all", "kind": "effect", "mutate": {"radius": {"op": "multiply", "value": 2.0}}})
glyph("more_range", "modifier", 1,
      ("Alcance", "Modificador: aumenta em 50% o alcance da forma (Toque, Raio, Salto)."),
      ("Range", "Modifier: increases the form's range by 50% (Touch, Ray, Blink)."),
      ["........", "#...#...", ".#...#..", "..#...#.", "..#...#.", ".#...#..", "#...#...", "........"],
      scope={"target": "all", "kind": "form", "mutate": {"range": {"op": "multiply", "value": 1.5},
                                                         "distance": {"op": "multiply", "value": 1.5}}})
glyph("delay", "modifier", 1,
      ("Atraso", "Modificador: o que vem depois só acontece alguns segundos mais tarde."),
      ("Delay", "Modifier: what follows only happens a few seconds later."),
      ["########", ".#....#.", "..#..#..", "...##...", "...##...", "..#..#..", ".#.##.#.", "########"],
      params={"seconds": (2.0, 0.5, 30.0, "float")},
      scope={"target": "next_segment", "kind": "segment", "wrap": True, "delay_param": "seconds"})
glyph("power", "modifier", 2,
      ("Potência", "Modificador: +50% de força no efeito anterior (dano, cura, empurrão, nível)."),
      ("Power", "Modifier: +50% strength on the previous effect (damage, heal, push, level)."),
      ["...##...", "...##...", "#.####.#", ".######.", "########", ".######.", "#.####.#", "...##..."],
      scope={"target": "previous_effect", "kind": "effect",
             "mutate": {"amount": {"op": "multiply", "value": 1.5}, "strength": {"op": "multiply", "value": 1.5},
                        "level": {"op": "add", "value": 1.0}, "power": {"op": "multiply", "value": 1.5}}})
glyph("lengthen", "modifier", 1,
      ("Duração", "Modificador: dobra o tempo do efeito anterior (fogo, lentidão, veneno...)."),
      ("Duration", "Modifier: doubles the previous effect's time (fire, slow, poison...)."),
      ["..#..#..", ".##..##.", "########", "########", ".##..##.", "..#..#..", "........", "........"],
      scope={"target": "previous_effect", "kind": "effect", "mutate": {"duration": {"op": "multiply", "value": 2.0}}})
glyph("swift", "modifier", 1,
      ("Velocidade do Projétil", "Modificador: o projétil voa 50% mais rápido."),
      ("Swift", "Modifier: the projectile flies 50% faster."),
      ["#.#.#...", "##.##...", "###.###.", "####.###", "###.###.", "##.##...", "#.#.#...", "........"],
      scope={"target": "delivery", "kind": "form", "mutate": {"speed": {"op": "multiply", "value": 1.5}}})

# ------------------------------------------------------------------ CONDICOES
glyph("on_hit_entity", "condition", 0,
      ("Ao Atingir", "Condição: o que vem depois só acontece ao atingir uma criatura."),
      ("On Hit", "Condition: what follows only happens when a creature is hit."),
      ["...#....", "..#.#...", "#..#..#.", ".#...#..", "..#.#...", ".#...#..", "#..#..#.", "...#...."])
glyph("on_hit_block", "condition", 0,
      ("Ao Bater", "Condição: o que vem depois só acontece ao atingir um bloco."),
      ("On Impact", "Condition: what follows only happens when a block is hit."),
      ["########", "#......#", "#.#..#.#", "#..##..#", "#..##..#", "#.#..#.#", "#......#", "########"])
glyph("after_seconds", "condition", 0,
      ("Após Segundos", "Condição: o que vem depois só acontece após alguns segundos."),
      ("After Seconds", "Condition: what follows only happens after a few seconds."),
      ["..####..", ".#..#.#.", "#...#..#", "#...#..#", "#....#.#", "#.....##", ".#....#.", "..####.."],
      params={"seconds": (2.0, 0.5, 30.0, "float")})
glyph("low_health", "condition", 0,
      ("Em Perigo", "Condição: o que vem depois só acontece se você estiver com menos da metade da vida."),
      ("In Danger", "Condition: what follows only happens if you are below half health."),
      [".##..##.", "########", "########", "########", ".######.", "..####..", "...##...", "........"])
glyph("sneaking", "condition", 0,
      ("Agachado", "Condição: o que vem depois só acontece se você estiver agachado."),
      ("Sneaking", "Condition: what follows only happens while you are sneaking."),
      ["..##....", ".####...", "..##....", ".######.", "#.####.#", "..####..", ".##..##.", "##....##"])
glyph("night", "condition", 0,
      ("Noite", "Condição: o que vem depois só acontece à noite."),
      ("Night", "Condition: what follows only happens at night."),
      ["..####..", ".##.....", "##......", "##......", "##......", ".##.....", "..####..", "........"])
glyph("in_water", "condition", 0,
      ("Na Água", "Condição: o que vem depois só acontece se você estiver na água."),
      ("In Water", "Condition: what follows only happens while you are in water."),
      ["........", "........", ".##..##.", "#..##..#", "........", ".##..##.", "#..##..#", "........"])

# ------------------------------------------------------------------ ATIVADOR
glyph("activate", "activator", 1,
      ("Ativar", "Ativador: dispara um ritual armazenado (rituais ainda não estão disponíveis)."),
      ("Activate", "Activator: triggers a stored ritual (rituals are not available yet)."),
      ["...##...", ".#.##.#.", "#..##..#", "#..##..#", "#......#", "#......#", ".#....#.", "..####.."])

# ================================================================== DESIGN v2 (chat de design): novos glifos
glyph("wave", "form", 3,
      ("Onda", "Forma: uma onda em cone a sua frente, afetando todas as criaturas que ela alcança."),
      ("Wave", "Form: a cone-shaped wave in front of you, affecting every creature it reaches."),
      ["..#..#..", ".#....#.", "#..##..#", "........", "#..##..#", ".#....#.", "..#..#..", "........"],
      params={"range": (8.0, 2.0, 16.0, "float")}, comp=FORM_COMP)
glyph("pillar", "form", 4,
      ("Pilar", "Forma: uma coluna vertical no ponto que você mira (criaturas e o bloco atingido)."),
      ("Pillar", "Form: a vertical column at the point you aim at (creatures and the block hit)."),
      ["..####..", "..#..#..", "..#..#..", "..#..#..", "..#..#..", "..#..#..", "..#..#..", ".######."],
      params={"range": (24.0, 4.0, 48.0, "float"), "radius": (2.0, 1.0, 6.0, "float")}, comp=FORM_COMP)
glyph("barrier", "form", 4,
      ("Barreira", "Forma: ergue uma parede de 3x3 blocos de energia a sua frente, que some sozinha."),
      ("Barrier", "Form: raises a 3x3 wall of energy in front of you that fades on its own."),
      ["#.#.#.#.", "########", "#.#.#.#.", "########", "#.#.#.#.", "########", "#.#.#.#.", "########"],
      params={"duration": (200.0, 40.0, 600.0, "int")}, magnitude=["duration"], comp=FORM_COMP)

glyph("mark", "effect", 1,
      ("Marcar", "Efeito: marca o alvo (ele brilha). Os Marcados podem ser escolhidos depois pelo filtro Marcados."),
      ("Mark", "Effect: marks the target (it glows). The Marked can be picked later with the Marked filter."),
      ["...##...", "..#..#..", ".#.##.#.", "#.#..#.#", "#.#..#.#", ".#.##.#.", "..#..#..", "...##..."],
      params={"duration": (200.0, 20.0, 1200.0, "int")}, magnitude=["duration"])
glyph("gravity", "effect", 3,
      ("Gravidade", "Efeito: o alvo fica pesado e é puxado para baixo (cai com força). Derruba areia, cascalho e bigornas. Vários Gravidade somam."),
      ("Gravity", "Effect: the target gets heavy and is slammed down (hard landing). Drops sand, gravel and anvils. Several Gravity glyphs add up."),
      ["...##...", "...##...", "...##...", "...##...", "#######.", ".#####..", "..###...", "...#...."],
      params={"strength": (1.0, 0.3, 4.0, "float")}, magnitude=["strength"])
glyph("attract", "effect", 2,
      ("Atrair", "Efeito: puxa o alvo na sua direção."),
      ("Attract", "Effect: pulls the target toward you."),
      ["#......#", ".#....#.", "..#..#..", "...##...", "..####..", "..#..#..", ".#....#.", "#......#"],
      params={"strength": (1.0, 0.3, 3.0, "float")}, magnitude=["strength"])
glyph("blind", "effect", 2,
      ("Cegar", "Efeito: o alvo fica cego por um tempo."),
      ("Blind", "Effect: the target is blinded for a while."),
      ["........", ".######.", "#......#", "#..##..#", "#..##..#", "#......#", ".######.", "#......#"],
      params={"duration": (100.0, 20.0, 400.0, "int")}, magnitude=["duration"])
glyph("wither", "effect", 3,
      ("Definhar", "Efeito: o alvo definha (dano contínuo do Wither)."),
      ("Wither", "Effect: the target withers (continuous Wither damage)."),
      ["..####..", ".######.", "##.##.##", "########", ".######.", "..#..#..", "..####..", "...##..."],
      params={"duration": (80.0, 20.0, 300.0, "int"), "level": (0.0, 0.0, 2.0, "int")}, magnitude=["duration"])
glyph("invisible", "effect", 3,
      ("Invisibilidade", "Efeito: o alvo fica invisível por um tempo."),
      ("Invisibility", "Effect: the target turns invisible for a while."),
      ["..####..", ".#....#.", "#.#..#.#", "#......#", "#.#..#.#", "#..##..#", ".#....#.", "..####.."],
      params={"duration": (600.0, 100.0, 2400.0, "int")}, magnitude=["duration"])
glyph("cut", "effect", 2,
      ("Corte", "Efeito: dano físico (a armadura reduz). Tosquia ovelhas e, em blocos, corta folhas na hora."),
      ("Cut", "Effect: physical damage (armor reduces it). Shears sheep and, on blocks, instantly cuts leaves."),
      ["#.......", ".#......", "..#..#..", "...##...", "..##....", ".##.....", "##......", "#......."],
      params={"amount": (3.0, 1.0, 16.0, "float")}, magnitude=["amount"])
glyph("freeze", "effect", 3,
      ("Congelar", "Efeito: congela criaturas (lentidão e frio) e, nos blocos, transforma água em gelo e lava em obsidiana."),
      ("Freeze", "Effect: freezes creatures (slowness and cold) and, on blocks, turns water to ice and lava to obsidian."),
      ["...##...", "#..##..#", ".#.##.#.", "..####..", "########", "..####..", ".#.##.#.", "#..##..#"],
      params={"duration": (100.0, 20.0, 400.0, "int"), "radius": (1.0, 0.5, 5.0, "float")}, magnitude=["duration"])

glyph("marked", "filter", 1,
      ("Marcados", "Filtro: só criaturas que foram Marcadas."),
      ("Marked", "Filter: only creatures that have been Marked."),
      ["...##...", "..#..#..", ".#.##.#.", "#.####.#", "#.####.#", ".#.##.#.", "..#..#..", "...##..."])
glyph("unmarked", "filter", 1,
      ("Não Marcados", "Filtro: só criaturas que NÃO foram Marcadas."),
      ("Unmarked", "Filter: only creatures that have NOT been Marked."),
      ["...##...", "..#..#..", ".#....#.", "#......#", "#......#", ".#....#.", "..#..#..", "...##..."])
glyph("allies", "filter", 1,
      ("Aliados", "Filtro: jogadores, animais domesticados, aldeões e golens de ferro."),
      ("Allies", "Filter: players, tamed animals, villagers and iron golems."),
      ["..####..", ".#....#.", "#..##..#", "#.####.#", "#..##..#", "#......#", ".#....#.", "..####.."])
glyph("wood", "filter", 1,
      ("Madeira", "Filtro: só blocos de madeira (troncos e tábuas)."),
      ("Wood", "Filter: only wooden blocks (logs and planks)."),
      ["########", "#.####.#", "##....##", "#.####.#", "##....##", "#.####.#", "##....##", "########"])

glyph("group", "modifier", 0,
      ("Grupo", "Modificador: abre um grupo. Um modificador depois de Fim do Grupo afeta todos os efeitos do grupo."),
      ("Group", "Modifier: opens a group. A modifier after End Group affects every effect in the group."),
      ["###.....", "#.......", "#.......", "#.......", "#.......", "#.......", "#.......", "###....."],
      scope={"target": "group_start", "kind": "structure"})
glyph("end_group", "modifier", 0,
      ("Fim do Grupo", "Modificador: fecha o grupo aberto por Grupo."),
      ("End Group", "Modifier: closes the group opened by Group."),
      [".....###", ".......#", ".......#", ".......#", ".......#", ".......#", ".......#", ".....###"],
      scope={"target": "group_end", "kind": "structure"})
glyph("weaken", "modifier", 1,
      ("Enfraquecer", "Modificador: metade da força do efeito anterior (dano, cura, empurrão, nível). Barateia a magia."),
      ("Weaken", "Modifier: half the strength of the previous effect (damage, heal, push, level). Makes the spell cheaper."),
      ["........", "........", "...##...", "..####..", ".######.", "...##...", "...##...", "........"],
      scope={"target": "previous_effect", "kind": "effect",
             "mutate": {"amount": {"op": "multiply", "value": 0.5}, "strength": {"op": "multiply", "value": 0.5},
                        "power": {"op": "multiply", "value": 0.5}}})
glyph("repeat", "modifier", 2,
      ("Repetição", "Modificador: repete o efeito anterior (no total, o número de vezes do parâmetro). Cada repetição custa o efeito de novo."),
      ("Repeat", "Modifier: repeats the previous effect (the parameter is the total number of times). Each repeat costs the effect again."),
      ["..####..", ".#....#.", "#.....##", "#....###", "#......#", "#.#....#", ".#....#.", "..####.."],
      params={"times": (2.0, 2.0, 4.0, "int")},
      scope={"target": "repeat", "kind": "effect", "repeat_param": "times"})

glyph("on_hurt", "condition", 0,
      ("Ao Sofrer Dano", "Condição: arma a magia em você: o que vem depois só acontece quando você levar dano (uma vez, por até 60 s)."),
      ("When Hurt", "Condition: arms the spell on you: what follows only happens when you take damage (once, for up to 60 s)."),
      ["...##...", "..#..#..", ".#.##.#.", "#..##..#", "#..##..#", ".#....#.", "..#..#..", "...##..."])
glyph("low_mana", "condition", 0,
      ("Mana Baixa", "Condição: o que vem depois só acontece se sua mana estiver abaixo de 20%."),
      ("Low Mana", "Condition: what follows only happens if your mana is below 20%."),
      ["...##...", "..####..", ".######.", ".######.", ".######.", "..####..", "...##...", "........"])
glyph("standing_still", "condition", 0,
      ("Parado", "Condição: o que vem depois só acontece se você estiver parado no chão."),
      ("Standing Still", "Condition: what follows only happens while you stand still on the ground."),
      ["..####..", "..####..", "..####..", ".######.", ".######.", "..#..#..", "..#..#..", ".##..##."])

# ================================================================== RITUAIS: glifos que SO existem como ritual
# ritual: 1 menor, 2 medio, 3 maior, 4 lendario (nivel do circulo desenhado). components: itens consumidos ao conjurar.
glyph("summon_wolves", "form", 4,
      ("Invocar Lobos", "Forma de RITUAL (menor): invoca lobos domesticados que lutam por você por um tempo. Os lobos são os alvos do que vem depois."),
      ("Summon Wolves", "RITUAL form (minor): summons tamed wolves that fight for you for a while. The wolves are the targets of what follows."),
      ["#.#..#.#", "###..###", ".##..##.", "..####..", ".######.", "..#..#..", "..####..", "...##..."],
      params={"count": (3.0, 2.0, 6.0, "int"), "duration": (1200.0, 200.0, 6000.0, "int")}, comp=FORM_COMP,
      tier=2, ritual=1, components=[("minecraft:bone", 6)])
glyph("summon_golem", "form", 6,
      ("Invocar Golem", "Forma de RITUAL (médio): ergue um golem de ferro que protege você por um tempo. O golem é o alvo do que vem depois."),
      ("Summon Golem", "RITUAL form (medium): raises an iron golem that guards you for a while. The golem is the target of what follows."),
      ["..####..", ".######.", "##.##.##", "########", "..####..", ".##..##.", ".##..##.", ".##..##."],
      params={"duration": (2400.0, 400.0, 12000.0, "int")}, comp=FORM_COMP,
      tier=3, ritual=2, components=[("minecraft:iron_block", 2), ("minecraft:carved_pumpkin", 1)])
glyph("warp", "effect", 8,
      ("Dobrar", "Efeito de RITUAL (maior): leva todas as criaturas alvo para o ponto que você mira (até 48). Com Aura ou Pilar e um círculo grande, desloca uma região inteira."),
      ("Warp", "RITUAL effect (major): moves every targeted creature to the point you aim at (up to 48). With Aura or Pillar and a big circle, it displaces a whole region."),
      ["#......#", ".#....#.", "..#..#..", "...##...", "...##...", "..#..#..", ".#....#.", "#......#"],
      params={"range": (32.0, 8.0, 64.0, "float")}, tier=4, ritual=3,
      components=[("minecraft:ender_pearl", 4), ("minecraft:crying_obsidian", 1)])
glyph("weather", "effect", 10,
      ("Clima", "Efeito de RITUAL (lendário): muda o clima do mundo (0 limpo, 1 chuva, 2 tempestade). Exige a oferenda de uma Estrela do Nether no círculo maior."),
      ("Weather", "RITUAL effect (legendary): changes the world's weather (0 clear, 1 rain, 2 storm). Requires a Nether Star offering in the major circle."),
      ["...##...", ".######.", "########", "########", ".######.", "...##...", "..#..#..", ".#....#."],
      params={"mode": (1.0, 0.0, 2.0, "int")}, tier=4, ritual=4,
      components=[("minecraft:lightning_rod", 1)])


# ================================================================== JURAMENTOS: poder em troca de uma restricao (checada no servidor)
VOW_MUT = lambda m: {"target": "all", "kind": "effect",
                     "mutate": {"amount": {"op": "vow", "value": 1.0}, "strength": {"op": "vow", "value": 1.0},
                                "power": {"op": "vow", "value": 1.0}}}
VOW_PARAMS = {"pledge": (2.0, 1.0, 5.0, "int")}
glyph("vow_stillness", "modifier", 1,
      ("Juramento da Quietude", "Juramento personalizável (Promessa 1 a 5): bônus = 28% x promessa^0,85 (promessa 2 = +50%). Exige ficar parado no chão por (promessa-1) x 1 s antes de conjurar."),
      ("Vow of Stillness", "Customizable vow (Pledge 1-5): bonus = 28% x pledge^0.85 (pledge 2 = +50%). Requires standing still on the ground for (pledge-1) x 1 s before casting."),
      ["...##...", "...##...", "..####..", "..####..", "...##...", "...##...", "..####..", "..####.."],
      params=VOW_PARAMS, scope=VOW_MUT(1.6))
glyph("vow_blood", "modifier", 1,
      ("Juramento de Sangue", "Juramento personalizável (Promessa 1 a 5): bônus = 30% x promessa^0,85 (promessa 2 = +54%). Custa 2 x promessa de vida e recusa se sobrar pouco."),
      ("Vow of Blood", "Customizable vow (Pledge 1-5): bonus = 30% x pledge^0.85 (pledge 2 = +54%). Costs 2 x pledge health and refuses if too little is left."),
      ["...##...", "..####..", ".######.", ".######.", "..####..", "..####..", "...##...", "........"],
      params=VOW_PARAMS, scope=VOW_MUT(1.7))
glyph("vow_daylight", "modifier", 1,
      ("Juramento da Luz", "Juramento personalizável (Promessa 1 a 5): bônus = 25% x promessa^0,85 (promessa 2 = +45%). 1 dia; 2 céu aberto; 3 sem chuva; 4 manhã a tarde; 5 perto do meio-dia."),
      ("Vow of Daylight", "Customizable vow (Pledge 1-5): bonus = 25% x pledge^0.85 (pledge 2 = +45%). 1 day; 2 open sky; 3 no rain; 4 morning to afternoon; 5 near noon."),
      ["#..##..#", ".#.##.#.", "..####..", "########", "########", "..####..", ".#.##.#.", "#..##..#"],
      params=VOW_PARAMS, scope=VOW_MUT(1.5))
glyph("on_death", "condition", 0,
      ("Ao Morrer", "Condição: arma a magia em você: o que vem depois só acontece quando você morrer (uma vez, por até 60 s)."),
      ("On Death", "Condition: arms the spell on you: what follows only happens when you die (once, for up to 60 s)."),
      ["..####..", ".######.", "##.##.##", "########", ".######.", "..#..#..", "..####..", "...##..."])

# ---------------------------------------------------------------- raridade (tier 1-4) e familias (especialidades de dungeon)
TIERS = {
    1: ["self", "projectile", "damage", "heal", "push", "fire", "hostile", "self_filter", "blocks", "area", "more_range",
        "on_hit_entity", "touch", "ignite", "cut", "marked", "unmarked", "not_self", "wood", "power", "group", "end_group"],
    2: ["slow", "poison", "speed", "players", "animals", "undead", "nearest", "wounded", "allies", "delay", "lengthen",
        "weaken", "swift", "after_seconds", "on_hit_block", "sneaking", "night", "in_water", "mark", "blind", "attract",
        "ray", "dig", "shield", "standing_still", "low_mana", "wave"],
    3: ["levitate", "aura", "pillar", "barrier", "explode", "blink", "freeze", "wither", "repeat", "low_health", "on_hurt",
        "activate", "vow_blood", "on_death"],
    4: ["thunder", "gravity", "invisible"],
}
FAMILIES = {
    "fire": ["fire", "ignite", "explode"],
    "ice": ["slow", "freeze", "shield"],
    "storm": ["thunder", "ray", "pillar"],
    "blood": ["damage", "poison", "wither", "cut", "wounded", "hostile", "undead"],
    "life": ["heal", "shield", "speed", "allies", "players", "animals"],
    "force": ["push", "attract", "gravity", "levitate", "wave", "aura", "area", "power", "repeat"],
    "knowledge": ["mark", "marked", "unmarked", "nearest", "delay", "after_seconds", "on_hit_entity", "on_hit_block",
                  "group", "end_group", "lengthen", "weaken", "night", "in_water", "sneaking", "standing_still", "low_mana", "vow_stillness", "vow_daylight"],
    "void": ["vow_blood", "on_death", "warp", "weather", "blink", "invisible", "blind", "on_hurt", "low_health", "activate", "not_self", "self_filter"],
    "earth": ["summon_wolves", "summon_golem", "dig", "barrier", "blocks", "wood", "more_range", "swift", "touch", "projectile", "self"],
}
for _tier, _ids in TIERS.items():
    for _gid in _ids:
        G[_gid]["tier"] = _tier
for _fam, _ids in FAMILIES.items():
    for _gid in _ids:
        G[_gid].setdefault("families", []).append(_fam)
for _gid, _g in G.items():
    _g.setdefault("families", ["arcane"])

ORDER = list(G.keys())
CATEGORY_ORDER = ["form", "filter", "effect", "modifier", "condition", "activator"]

if __name__ == "__main__":
    for k, v in G.items():
        assert len(v["icon"]) == 8 and all(len(r) == 8 for r in v["icon"]), k
    print(len(G), "glifos")
