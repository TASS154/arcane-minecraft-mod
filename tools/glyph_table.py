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
      ("Barreira", "Forma: ergue uma parede de energia a sua frente (3x3 com raio 3; Area e raio ampliam). Com Domo, Esfera ou Anel vira uma casca em volta do centro. Os glifos seguintes atingem os blocos e quem estiver dentro ou encostado."),
      ("Barrier", "Form: raises a wall of energy in front of you (3x3 at radius 3; Area and radius enlarge it). With Dome, Sphere or Ring it becomes a shell around the center. Following glyphs hit the blocks and anyone inside or touching."),
      ["#.#.#.#.", "########", "#.#.#.#.", "########", "#.#.#.#.", "########", "#.#.#.#.", "########"],
      params={"duration": (200.0, 40.0, 600.0, "int"), "radius": (3.0, 2.0, 12.0, "float")}, magnitude=["duration"], comp=FORM_COMP)

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
      ("Invocar Lobos", "Forma de RITUAL (menor): invoca Lobos Espectrais domesticados que lutam por você por um tempo. Número (count) e tamanho (size 1–3) são escolha sua: maiores têm outro modelo (Alfa, Fenrir) e mais vida e dano. Os lobos são os alvos do que vem depois."),
      ("Summon Wolves", "RITUAL form (minor): summons tamed Spectral Wolves that fight for you for a while. You pick the number (count) and size (1-3): bigger ones use another model (Alpha, Fenrir) with more health and damage. The wolves are the targets of what follows."),
      ["#.#..#.#", "###..###", ".##..##.", "..####..", ".######.", "..#..#..", "..####..", "...##..."],
      params={"count": (3.0, 1.0, 6.0, "int"), "size": (1.0, 1.0, 3.0, "int"), "duration": (1200.0, 200.0, 6000.0, "int")}, comp=FORM_COMP,
      tier=2, ritual=1, components=[("minecraft:bone", 6)])
glyph("summon_golem", "form", 6,
      ("Invocar Golem", "Forma de RITUAL (médio): ergue Golens Rúnicos que protegem você por um tempo. Número (count 1–3) e tamanho (size 1–3: Guardião, Colosso, Titã, cada um com modelo próprio). O golem é o alvo do que vem depois."),
      ("Summon Golem", "RITUAL form (medium): raises Runic Golems that guard you for a while. You pick the number (count 1-3) and size (1-3: Guardian, Colossus, Titan, each with its own model). The golem is the target of what follows."),
      ["..####..", ".######.", "##.##.##", "########", "..####..", ".##..##.", ".##..##.", ".##..##."],
      params={"count": (1.0, 1.0, 3.0, "int"), "size": (1.0, 1.0, 3.0, "int"), "duration": (2400.0, 400.0, 12000.0, "int")}, comp=FORM_COMP,
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
glyph("vow_oath", "modifier", 1,
      ("Juramento Personalizado", "Você escolhe o castigo e quanto tempo ele dura, DEPOIS de conjurar. Tipo: 1 imobilidade, 2 silêncio (nenhuma outra magia), 3 mana selada, 4 fragilidade (+50% de dano recebido), 5 cegueira. Bônus = k x ln(1 + segundos/5), com k 0,30 / 0,22 / 0,35 / 0,25 / 0,20 e teto de +150%. Ex.: imobilidade por 30 s = +58%."),
      ("Custom Oath", "You choose the penalty and how long it lasts, AFTER casting. Kind: 1 immobile, 2 silence (no other spell), 3 sealed mana, 4 fragile (+50% damage taken), 5 blind. Bonus = k x ln(1 + seconds/5), with k 0.30 / 0.22 / 0.35 / 0.25 / 0.20, capped at +150%. E.g. immobile for 30 s = +58%."),
      ["..#..#..", ".######.", "#..##..#", "#.####.#", "#..##..#", ".######.", "..#..#..", "...##..."],
      params={"kind": (1.0, 1.0, 5.0, "int"), "seconds": (15.0, 3.0, 600.0, "float")}, scope=VOW_MUT(1.0))
glyph("on_death", "condition", 0,
      ("Ao Morrer", "Condição: arma a magia em você: o que vem depois só acontece quando você morrer (uma vez, por até 60 s)."),
      ("On Death", "Condition: arms the spell on you: what follows only happens when you die (once, for up to 60 s)."),
      ["..####..", ".######.", "##.##.##", "########", ".######.", "..#..#..", "..####..", "...##..."])


# ================================================================== FORMATOS e ORIGEM: valem para qualquer forma com area
def SHAPE_SCOPE(n):
    return {"target": "delivery", "kind": "form", "mutate": {"shape": {"op": "put", "value": float(n)}}}


glyph("shape_dome", "modifier", 2,
      ("Domo", "Formato: a forma vira uma meia-esfera (casca de blocos na Barreira; so quem esta dentro na Aura). Use Area para aumentar o raio."),
      ("Dome", "Shape: the form becomes a half-sphere (a block shell for Barrier; only those inside for Aura). Use Area to grow the radius."),
      ["..####..", ".#....#.", "#......#", "#......#", "#......#", "#......#", "########", "........"], scope=SHAPE_SCOPE(1))
glyph("shape_sphere", "modifier", 3,
      ("Esfera", "Formato: a forma vira uma esfera completa em volta do centro."),
      ("Sphere", "Shape: the form becomes a full sphere around the center."),
      ["..####..", ".#....#.", "#......#", "#......#", "#......#", "#......#", ".#....#.", "..####.."], scope=SHAPE_SCOPE(2))
glyph("shape_ring", "modifier", 2,
      ("Anel", "Formato: a forma vira um anel no chao em volta do centro (coluna de 2 blocos de altura)."),
      ("Ring", "Shape: the form becomes a ring on the ground around the center (2 blocks high)."),
      [".######.", "#......#", "#.####.#", "#.#..#.#", "#.#..#.#", "#.####.#", "#......#", ".######."], scope=SHAPE_SCOPE(3))
glyph("shape_cone", "modifier", 2,
      ("Cone", "Formato: a forma vira um cone de ~100 graus a sua frente (a Onda ja e assim). Na Barreira, uma tampa curva."),
      ("Cone", "Shape: the form becomes a ~100 degree cone in front of you (Wave already is). For Barrier, a curved cap."),
      ["...##...", "..####..", "..####..", ".######.", ".######.", "########", "########", "........"], scope=SHAPE_SCOPE(4))
glyph("shape_column", "modifier", 2,
      ("Coluna", "Formato: a forma vira uma coluna vertical no ponto mirado (o Pilar ja e assim). Na Barreira, um cilindro oco."),
      ("Column", "Shape: the form becomes a vertical column at the aimed point (Pillar already is). For Barrier, a hollow cylinder."),
      ["..####..", "..#..#..", "..#..#..", "..#..#..", "..#..#..", "..#..#..", "..#..#..", "..####.."], scope=SHAPE_SCOPE(5))
glyph("shape_wall", "modifier", 2,
      ("Parede", "Formato: a forma vira uma parede perpendicular ao seu olhar, a 3 blocos a frente (a Barreira ja e assim)."),
      ("Wall", "Shape: the form becomes a wall perpendicular to your gaze, 3 blocks ahead (Barrier already is)."),
      ["########", "#.#.#.#.", "########", "#.#.#.#.", "########", "#.#.#.#.", "########", "#.#.#.#."], scope=SHAPE_SCOPE(6))
glyph("origin_self", "modifier", 1,
      ("Eu (origem)", "Origem: o centro da forma passa a ser voce, mesmo em formas que normalmente miram (Pilar) ou ficam a frente (parede)."),
      ("Self (origin)", "Origin: the form's center becomes you, even for forms that normally aim (Pillar) or sit ahead (wall)."),
      ["..####..", ".#....#.", "#..##..#", "#.#..#.#", "#.#..#.#", "#..##..#", ".#....#.", "..####.."],
      scope={"target": "delivery", "kind": "form", "mutate": {"origin": {"op": "put", "value": -1.0}}})

glyph("origin_aim", "modifier", 1,
      ("Mira", "Origem: o centro da forma passa a ser o ponto para onde voce olha (ate 32 blocos), em vez de voce."),
      ("Aim", "Origin: the form's center becomes the point you look at (up to 32 blocks) instead of you."),
      ["...##...", "...##...", "..#..#..", "###..###", "###..###", "..#..#..", "...##...", "...##..."],
      scope={"target": "delivery", "kind": "form", "mutate": {"origin": {"op": "put", "value": 1.0}}})

glyph("zone", "modifier", 3,
      ("Zona", "Torna a forma persistente: ela se repete do mesmo lugar a cada intervalo, durante o tempo escolhido, reaplicando os efeitos em quem está dentro (Aura + Fogo vira um campo em chamas; Barreira vira parede contínua; Projétil vira torre). Parâmetros: segundos (até 60) e intervalo (mín. 0,5 s). No máximo 2 zonas suas ao mesmo tempo. Invocações ignoram a Zona."),
      ("Zone", "Makes the form persistent: it repeats from the same place every interval for the chosen time, reapplying effects to whoever is inside (Aura + Fire becomes a burning field; Barrier becomes a continuous wall; Projectile becomes a turret). Parameters: seconds (up to 60) and interval (min 0.5 s). At most 2 of your zones at once. Summons ignore Zone."),
      ["..####..", ".#....#.", "#.####.#", "#.#..#.#", "#.#..#.#", "#.####.#", ".#....#.", "..####.."],
      params={"seconds": (10.0, 2.0, 60.0, "float"), "interval": (1.0, 0.5, 5.0, "float")},
      scope={"target": "delivery", "kind": "form", "mutate": {"zone_seconds": {"op": "put", "value": 10.0, "param": "seconds"}, "zone_interval": {"op": "put", "value": 1.0, "param": "interval"}}})
glyph("on_zone_end", "condition", 0,
      ("Quando a Zona Acabar", "Condição: o que vem depois só acontece quando a zona desta magia terminar (uma vez). Sem o glifo Zona, nunca acontece."),
      ("When the Zone Ends", "Condition: what follows only happens when this spell's zone ends (once). Without the Zone glyph, it never happens."),
      ["..####..", ".#....#.", "#..##..#", "#.####.#", "#.####.#", "#..##..#", ".#....#.", "..####.."])

glyph("on_zone_enter", "condition", 0,
      ("Ao Entrar na Zona", "Condição: o que vem depois só acontece nas criaturas que ENTRAM na zona desta magia (uma vez para cada entrada, conferida a cada pulso). Quem já estava dentro ao conjurar não conta. Sem o glifo Zona, nunca acontece."),
      ("On Zone Enter", "Condition: what follows only happens to creatures that ENTER this spell's zone (once per entry, checked every pulse). Whoever was already inside when cast does not count. Without the Zone glyph, it never happens."),
      ["..####..", ".#....#.", "#.#....#", "#.####.#", "#.####.#", "#.#....#", ".#....#.", "..####.."])
glyph("on_zone_exit", "condition", 0,
      ("Ao Sair da Zona", "Condição: o que vem depois só acontece nas criaturas que SAEM da zona desta magia (uma vez para cada saída, conferida a cada pulso). Sem o glifo Zona, nunca acontece."),
      ("On Zone Exit", "Condition: what follows only happens to creatures that LEAVE this spell's zone (once per exit, checked every pulse). Without the Zone glyph, it never happens."),
      ["..####..", ".#....#.", "#....#.#", "#.####.#", "#.####.#", "#....#.#", ".#....#.", "..####.."])
glyph("outsiders", "filter", 2,
      ("Forasteiros", "Filtro: afeta só quem NÃO é você nem está na sua lista de convidados (comando /arcane guest add|remove|list). Animais domesticados e invocações de você ou dos seus convidados também ficam de fora. Útil em inscrições e zonas que protegem a sua casa."),
      ("Outsiders", "Filter: affects only those who are NOT you nor on your guest list (command /arcane guest add|remove|list). Tamed animals and summons of you or your guests are also left out. Handy for inscriptions and zones that guard your home."),
      ["..####..", ".#....#.", "#.#..#.#", "#..##..#", "#..##..#", "#.#..#.#", ".#....#.", "..####.."])

glyph("blast", "effect", 8,
      ("Explosão Destrutiva", "Efeito: explode no alvo DESTRUINDO blocos (como TNT, com queda de itens) e ferindo quem está perto. Você é imune. Raio 1–6. Respeita as configs do servidor contra destruição de blocos."),
      ("Destructive Blast", "Effect: explodes at the target DESTROYING blocks (like TNT, dropping items) and hurting nearby creatures. You are immune. Radius 1-6. Respects the server configs against block destruction."),
      ["#.#..#.#", ".#.##.#.", "#.####.#", "..####..", "..####..", "#.####.#", ".#.##.#.", "#.#..#.#"],
      params={"radius": (3.0, 1.0, 6.0, "float")})
glyph("chain", "modifier", 3,
      ("Cadeia", "Modificador: o efeito anterior se propaga de criatura em criatura. Explosão e Explosão Destrutiva detonam de novo, menores, em quem estava perto (até 3 saltos, 8 extras); Dano salta em arco para a mais próxima, com 60% do dano a cada salto. Outros efeitos ignoram a Cadeia (a prévia avisa)."),
      ("Chain", "Modifier: the previous effect spreads from creature to creature. Explosion and Destructive Blast detonate again, smaller, on nearby creatures (up to 3 hops, 8 extra); Damage arcs to the nearest creature at 60% per hop. Other effects ignore Chain (the preview warns)."),
      ["#......#", "##....##", ".##..##.", "..####..", "..####..", ".##..##.", "##....##", "#......#"],
      scope={"target": "previous_effect", "kind": "effect", "mutate": {"chain": {"op": "put", "value": 1.0}}})

glyph("float", "effect", 5,
      ("Flutuar", "Efeito: o alvo paira e voa com suavidade na direção do olhar, como se fosse erguido pela cabeça (a cabeça vai primeiro, as pernas seguem). W avança, Espaço sobe, Shift desce, Ctrl acelera; sem comandos, ele paira parado. Velocidade de 0,3 a 3 blocos por tick, duração em ticks e planagem (0 a 1: sem comandos, o corpo plana adiante como numa elytra e cai devagar em vez de parar). Custa mana a cada tick enquanto voa (mais em velocidade alta) e acaba sem mana; a primeira queda depois do voo não causa dano. No Grimório, o modo Segurar ou Alternar mantém o voo enquanto você canaliza."),
      ("Float", "Effect: the target hovers and glides smoothly where it looks, as if lifted by the head (the head leads, the legs follow). W moves, Space rises, Shift sinks, Ctrl speeds up; with no input it hovers in place. Speed from 0.3 to 3 blocks per tick, duration in ticks and glide (0 to 1: with no input the body glides forward like an elytra and sinks slowly instead of stopping). It costs mana every tick while flying (more at high speed) and ends when mana runs out; the first fall after the flight deals no damage. In the Grimoire, Hold or Toggle mode keeps the flight going while you channel."),
      ["...##...", "..####..", "...##...", "..#..#..", ".#.##.#.", "#..##..#", "..#..#..", ".#....#."],
      params={"duration": (200.0, 40.0, 1200.0, "int"), "speed": (0.6, 0.3, 3.0, "float"), "glide": (0.0, 0.0, 1.0, "float")}, magnitude=["duration"])
glyph("telekinesis", "effect", 6,
      ("Telecinese", "Efeito: segura criaturas e blocos com a mente e os leva pairando diante da mira. O grupo de blocos é definido ao conjurar: um bloco; uma esfera (Esfera 1–6) ao redor do bloco mirado; ou, com Mesmo tipo, os blocos conectados do mesmo material (até 120), opcionalmente limitados pela esfera. Ao soltar, tudo cai com o embalo. Distância 2–16, duração até 20 s."),
      ("Telekinesis", "Effect: holds creatures and blocks with your mind and carries them hovering in front of your aim. The block group is decided when you cast: one block; a sphere (Sphere 1-6) around the aimed block; or, with Same type, the connected blocks of the same material (up to 120), optionally limited by the sphere. When released, everything falls with its momentum. Distance 2-16, duration up to 20 s."),
      ["#..##..#", ".#.##.#.", "..####..", "#.####.#", "#.####.#", "..####..", ".#.##.#.", "#..##..#"],
      params={"duration": (100.0, 20.0, 400.0, "int"), "distance": (6.0, 2.0, 16.0, "float"), "sphere": (0.0, 0.0, 6.0, "int"), "same_type": (0.0, 0.0, 1.0, "int")}, magnitude=["duration"])


# ================================================================== FASE 3: glifos de custo baixo
glyph("rain", "effect", 2,
      ("Chuva", "Efeito: uma chuva local cai sobre o alvo: molha quem está na área (raio 1,5–10), apaga fogo e rega plantações. Não muda o clima do mundo (isso é o ritual Clima)."),
      ("Rain", "Effect: a local rain falls on the target: wets everyone in the area (radius 1.5-10), puts out fire and waters crops. It does not change the world weather (that is the Weather ritual)."),
      ["..####..", ".######.", "########", "........", ".#.#.#.#", "#.#.#.#.", ".#.#.#.#", "#.#.#.#."],
      params={"radius": (4.0, 1.5, 10.0, "float"), "duration": (400.0, 40.0, 1200.0, "int")}, magnitude=["duration"])
glyph("charge", "effect", 3,
      ("Investida", "Efeito: você avança em linha reta contra o alvo (ou para onde olha), até 16 blocos, ferindo quem estiver no caminho. Diferente de Empurrão em si mesmo, que só dá um impulso, e de Salto, que teleporta."),
      ("Charge", "Effect: you rush in a straight line at the target (or where you look), up to 16 blocks, hurting whoever is in the way. Unlike Push on yourself, which is only an impulse, and Blink, which teleports."),
      ["#.......", "##......", "###.##..", "########", "########", "###.##..", "##......", "#......."],
      params={"distance": (8.0, 3.0, 16.0, "float"), "damage": (3.0, 0.0, 12.0, "float")}, magnitude=["damage"])
glyph("glide", "effect", 2,
      ("Planar", "Efeito: o alvo cai devagar por um tempo (queda lenta), sem dano de queda. É a versão passiva; para voar de verdade, use Flutuar."),
      ("Glide", "Effect: the target falls slowly for a while (slow falling), with no fall damage. This is the passive version; to really fly, use Float."),
      ["...##...", "..####..", ".######.", "########", "#..##..#", "...##...", "..#..#..", ".#....#."],
      params={"duration": (200.0, 20.0, 1200.0, "int")}, magnitude=["duration"])
glyph("taunt", "effect", 1,
      ("Provocar", "Efeito: criaturas hostis passam a perseguir você, largando outro alvo e acordando do Sono ou do Medo. Não afeta jogadores."),
      ("Taunt", "Effect: hostile creatures start chasing you, dropping any other target and waking from Sleep or Fear. Does not affect players."),
      ["..#..#..", ".##..##.", "##....##", "#.#..#.#", "#......#", ".#.##.#.", "..#..#..", "...##..."])
glyph("fear", "effect", 2,
      ("Medo", "Efeito: a criatura larga o alvo e foge de você; enquanto dura, não escolhe você como alvo (1 a 20 s). Jogadores ficam fracos e com as mãos pesadas."),
      ("Fear", "Effect: the creature drops its target and flees from you; while it lasts it will not pick you as a target (1 to 20 s). Players become weak and heavy-handed."),
      ["..####..", ".#....#.", "#.#..#.#", "#......#", "#..##..#", ".#.##.#.", "..####..", "...##..."],
      params={"duration": (100.0, 20.0, 400.0, "int")}, magnitude=["duration"])
glyph("sleep", "effect", 3,
      ("Sono", "Efeito: a criatura larga o alvo e adormece, imóvel e fraca, por até 30 s. Qualquer dano a acorda."),
      ("Sleep", "Effect: the creature drops its target and falls asleep, still and weak, for up to 30 s. Any damage wakes it."),
      ["....###.", "......#.", ".....#..", "....###.", ".###....", "..#.....", ".#......", ".###...."],
      params={"duration": (100.0, 20.0, 600.0, "int")}, magnitude=["duration"])
glyph("rumble", "effect", 3,
      ("Estrondo", "Efeito: um trovão seco no alvo: fere de leve, empurra para fora (raio 1–8) e deixa as criaturas zonzas por 1,5 s. Não quebra blocos nem causa fogo."),
      ("Rumble", "Effect: a dry thunderclap on the target: hurts lightly, shoves outward (radius 1-8) and leaves creatures dazed for 1.5 s. Breaks no blocks and starts no fire."),
      ["#..##..#", ".#.##.#.", "..####..", "###..###", "###..###", "..####..", ".#.##.#.", "#..##..#"],
      params={"radius": (3.0, 1.0, 8.0, "float"), "damage": (2.0, 0.0, 12.0, "float")}, magnitude=["damage"])
glyph("darkness", "effect", 2,
      ("Escuridão", "Efeito: o alvo fica envolto em escuridão pulsante (efeito Escuridão do jogo): a visão some aos poucos e volta. Mais suave que Cegar."),
      ("Darkness", "Effect: the target is wrapped in pulsing darkness (the game's Darkness effect): vision fades and returns. Softer than Blind."),
      ["..####..", ".######.", "########", "########", "########", "########", ".######.", "..####.."],
      params={"duration": (200.0, 20.0, 600.0, "int")}, magnitude=["duration"])
glyph("wet", "effect", 1,
      ("Molhar", "Efeito: deixa a criatura molhada por até 60 s e apaga o fogo dela. Molhado: Trovão e Raio ferem mais, Gelo congela mais rápido e mais forte, e Fogo nem pega (vira vapor). As regras estão em data/arcane/interactions."),
      ("Soak", "Effect: leaves the creature wet for up to 60 s and puts out its fire. While wet: Thunder and storm spells hurt more, Frost freezes faster and harder, and Fire fizzles into steam. The rules live in data/arcane/interactions."),
      ["...##...", "...##...", "..####..", ".######.", "########", "########", ".######.", "..####.."],
      params={"duration": (200.0, 40.0, 1200.0, "int")}, magnitude=["duration"])
glyph("lifesteal", "effect", 3,
      ("Roubo de Vida", "Efeito: fere o alvo e devolve a você 60% do dano que realmente causou. Não funciona em si mesmo. Invertido, vira Transferir Vida."),
      ("Life Steal", "Effect: hurts the target and gives you back 60% of the damage actually dealt. Does not work on yourself. Inverted, it becomes Life Transfer."),
      ["..#..#..", ".######.", "########", "########", ".######.", "..####..", "...##...", "........"],
      params={"amount": (3.0, 1.0, 16.0, "float"), "ratio": (0.6, 0.1, 1.0, "float")}, magnitude=["amount"])
glyph("life_transfer", "effect", 2,
      ("Transferir Vida", "Efeito: você passa vida ao alvo (1–16), sem nunca cair abaixo de 1 coração. Invertido, vira Roubo de Vida."),
      ("Life Transfer", "Effect: you pass health to the target (1-16), never dropping below 1 heart. Inverted, it becomes Life Steal."),
      ["........", "..#..#..", ".######.", "########", ".######.", "..####..", "...##...", "........"],
      params={"amount": (4.0, 1.0, 16.0, "float")}, magnitude=["amount"])
glyph("read_item", "effect", 1,
      ("Ler Item", "Efeito: mostra no chat o item que a criatura segura (ou o item solto no chão): nome, quantidade, durabilidade e encantamentos. Só informa; não muda nada."),
      ("Read Item", "Effect: shows in chat the item the creature is holding (or the dropped item): name, count, durability and enchantments. Informational only; changes nothing."),
      ["..####..", ".#....#.", "#..##..#", "#.#..#.#", "#.#..#.#", "#..##..#", ".#....#.", "..####.."])
glyph("invert", "modifier", 1,
      ("Inverter", "Modificador: troca o efeito anterior pelo seu oposto, com a mesma magnitude. Pares: Dano e Cura, Empurrão e Atrair, Lentidão e Velocidade, Fogo e Congelar, Levitação e Gravidade, Roubo e Transferir Vida, Cegar e Invisibilidade, Medo e Provocar. Efeito sem par ignora o Inverter."),
      ("Invert", "Modifier: swaps the previous effect for its opposite, with the same magnitude. Pairs: Damage and Heal, Push and Attract, Slow and Speed, Fire and Freeze, Levitate and Gravity, Life Steal and Transfer, Blind and Invisibility, Fear and Taunt. An effect with no pair ignores Invert."),
      ["#......#", "##....##", "#.#..#.#", "#..##..#", "#..##..#", "#.#..#.#", "##....##", "#......#"],
      scope={"target": "previous_effect", "kind": "effect", "mutate": {"invert": {"op": "put", "value": 1.0}}})

glyph("on_kill", "condition", 0,
      ("Ao Matar", "Condição: arma a magia em você: o que vem depois só acontece quando você matar uma criatura (uma vez, por até 60 s). Com forma Eu, age em você."),
      ("On Kill", "Condition: arms the spell on you: what follows only happens when you kill a creature (once, for up to 60 s). With the Self form, it acts on you."),
      ["#......#", ".#....#.", "..#..#..", "...##...", "..####..", ".#.##.#.", "#..##..#", "...##..."])
glyph("on_fall", "condition", 0,
      ("Ao Cair", "Condição: arma a magia em você: o que vem depois só acontece quando você aterrissar depois de cair 3 blocos ou mais (uma vez, por até 60 s)."),
      ("On Fall", "Condition: arms the spell on you: what follows only happens when you land after falling 3 blocks or more (once, for up to 60 s)."),
      ["...##...", "...##...", "...##...", "..####..", ".######.", "........", "########", "........"])
glyph("on_jump", "condition", 0,
      ("Ao Pular", "Condição: arma a magia em você: o que vem depois só acontece quando você pular (uma vez, por até 60 s)."),
      ("On Jump", "Condition: arms the spell on you: what follows only happens when you jump (once, for up to 60 s)."),
      ["........", "...##...", "..####..", ".#.##.#.", "...##...", "...##...", "..#..#..", "########"])
glyph("target_health", "condition", 0,
      ("Vida do Alvo Abaixo de X", "Condição: o que vem depois só acontece se algum alvo criatura estiver com menos de X% da vida (X 5–95, padrão 30). Sem alvo criatura, não passa."),
      ("Target Health Below X", "Condition: what follows only happens if some creature target has less than X% health (X 5-95, default 30). With no creature target, it fails."),
      ["#.#..#.#", "########", "########", ".######.", "..####..", "...##...", "........", "###..###"],
      params={"percent": (30.0, 5.0, 95.0, "int")})
glyph("region", "condition", 0,
      ("Bioma ou Dimensão", "Condição: só acontece no lugar escolhido (kind): 1 Superfície, 2 Nether, 3 End, 4 Quente (deserto, mesa, Nether), 5 Frio (neve), 6 Floresta (floresta, selva, taiga), 7 Oceano, 8 Pântano."),
      ("Biome or Dimension", "Condition: only happens in the chosen place (kind): 1 Overworld, 2 Nether, 3 End, 4 Hot (desert, badlands, Nether), 5 Cold (snow), 6 Forest (forest, jungle, taiga), 7 Ocean, 8 Swamp."),
      ["..####..", ".######.", "##.##.##", "########", "..####..", ".#.##.#.", "#..##..#", "........"],
      params={"kind": (1.0, 1.0, 8.0, "int")})
glyph("holding", "condition", 0,
      ("Segurando Item X", "Condição: só acontece se o item na sua mão principal for do tipo escolhido (kind): 0 mão vazia, 1 espada ou machado, 2 ferramenta, 3 arco, besta ou tridente, 4 comida, 5 bloco, 6 poção, 7 qualquer item."),
      ("Holding Item X", "Condition: only happens if your main-hand item is of the chosen kind: 0 empty hand, 1 sword or axe, 2 tool, 3 bow, crossbow or trident, 4 food, 5 block, 6 potion, 7 any item."),
      ["...##...", "..####..", "...##...", "..####..", ".######.", ".#....#.", "#......#", "........"],
      params={"kind": (1.0, 0.0, 7.0, "int")})
glyph("raining", "condition", 0,
      ("Chuva ou Tempestade", "Condição: só acontece com chuva ou tempestade no mundo (storm 1: só tempestade)."),
      ("Rain or Storm", "Condition: only happens while it rains or storms in the world (storm 1: storms only)."),
      ["..####..", ".######.", "########", "........", ".#.#.#.#", "#.#.#.#.", ".#.#.#.#", "........"],
      params={"storm": (0.0, 0.0, 1.0, "int")})


# ================================================================== FASE 4: glifos medios
def _g(gid, cat, cost, pt, en, icon, **kw):
    glyph(gid, cat, cost, pt, en, icon, **kw)

_g("satellites", "effect", 4,
   ("Satélites", "Efeito: orbes giram ao redor do alvo (1–4, 2 blocos de raio) ferindo quem encostar neles, menos o dono. Duração até 30 s."),
   ("Satellites", "Effect: orbs circle the target (1-4, 2-block radius) hurting whoever touches them, except the owner. Lasts up to 30 s."),
   ["...##...", "#......#", "........", "##.##.##", "........", "#......#", "...##...", "........"],
   params={"count": (2.0, 1.0, 4.0, "int"), "duration": (200.0, 40.0, 600.0, "int"), "damage": (2.0, 0.5, 8.0, "float")}, magnitude=["damage"])
_g("link", "effect", 3,
   ("Elo", "Efeito: liga as duas primeiras criaturas alvo (ou você e o alvo): metade do dano que uma sofre passa para a outra, por até 60 s."),
   ("Link", "Effect: links the first two target creatures (or you and the target): half the damage one takes passes to the other, for up to 60 s."),
   [".##..##.", "#..##..#", "#.####.#", "#..##..#", ".##..##.", "........", "........", "........"],
   params={"duration": (300.0, 40.0, 1200.0, "int")}, magnitude=["duration"])
_g("trail", "effect", 3,
   ("Rastro", "Efeito: o alvo deixa para trás um rastro de brasas (até 24 pontos, 6 s cada) que fere e atrasa quem pisar, menos o dono."),
   ("Trail", "Effect: the target leaves a trail of embers behind (up to 24 points, 6 s each) that hurts and slows whoever steps on it, except the owner."),
   ["........", "......#.", ".....##.", "...###..", "..###...", ".##.....", "##......", "#......."],
   params={"duration": (200.0, 40.0, 600.0, "int"), "damage": (1.5, 0.5, 6.0, "float")}, magnitude=["damage"])
_g("hook", "effect", 2,
   ("Gancho", "Efeito: você é puxado em linha reta até o alvo (criatura, bloco ou ponto), como um cabo preso nele; a queda é anulada. Não puxa o alvo (isso é Atrair)."),
   ("Hook", "Effect: you are pulled in a straight line to the target (creature, block or point), like a cable anchored to it; fall damage is cancelled. It does not pull the target (that is Attract)."),
   ["....###.", "....#.#.", "....###.", "...#....", "..#.....", ".#......", "#.......", "#......."],
   params={"strength": (1.2, 0.5, 3.0, "float")}, magnitude=["strength"])
_g("climb", "effect", 2,
   ("Escalar", "Efeito: por um tempo, quem avança contra uma parede sobe por ela (jogadores: segurando para frente ou pulo; agache para parar). Anula a queda."),
   ("Climb", "Effect: for a while, whoever pushes against a wall climbs it (players: holding forward or jump; sneak to stop). Cancels falling."),
   ["#.......", "#..##...", "#.####..", "#..##...", "#..##...", "#.#..#..", "#.......", "#......."],
   params={"duration": (200.0, 40.0, 1200.0, "int")}, magnitude=["duration"])
_g("phase", "effect", 4,
   ("Atravessar", "Efeito: você atravessa uma parede de até 6 blocos na direção em que olha e sai do outro lado, onde houver espaço para você. Sem espaço, nada acontece."),
   ("Phase", "Effect: you pass through a wall up to 6 blocks thick in the direction you look and come out the other side, where there is room. With no room, nothing happens."),
   ["##....##", "##....##", "##.##.##", "##.##.##", "##.##.##", "##.##.##", "##....##", "##....##"],
   params={"thickness": (4.0, 1.0, 6.0, "int")})
_g("recall", "effect", 3,
   ("Retorno", "Efeito: o primeiro uso marca o lugar onde você está (10 min); o segundo leva você de volta, se estiver na mesma dimensão. Cada uso gasta a mana da magia."),
   ("Recall", "Effect: the first use marks where you stand (10 min); the second takes you back, if in the same dimension. Each use spends the spell's mana."),
   ["..####..", ".#....#.", "#..##..#", "#.#..#.#", "#..##.#.", ".#...##.", "..####..", "........"])
_g("imbue", "effect", 3,
   ("Encantar", "Efeito: a arma na mão do alvo ganha por um tempo (até 5 min) e por poucos golpes (1–10) +2 de dano e um efeito: kind 1 fogo, 2 gelo, 3 raio (+3), 4 veneno. Só vale golpe corpo a corpo."),
   ("Imbue", "Effect: the weapon in the target's hand gains for a while (up to 5 min) and for a few hits (1-10) +2 damage and an effect: kind 1 fire, 2 frost, 3 lightning (+3), 4 venom. Melee only."),
   ["....##..", "...###..", "..###...", ".###....", "###.#...", "##.###..", "#...#...", "........"],
   params={"duration": (1200.0, 200.0, 6000.0, "int"), "kind": (1.0, 1.0, 4.0, "int"), "hits": (6.0, 1.0, 10.0, "int")}, magnitude=["duration"])
_g("petrify", "effect", 4,
   ("Petrificar", "Efeito: o alvo vira pedra por até 15 s: imóvel, sem escolher alvos e com metade do dano recebido. O dano não quebra o efeito (diferente do Sono)."),
   ("Petrify", "Effect: the target turns to stone for up to 15 s: immobile, picking no targets and taking half damage. Damage does not break it (unlike Sleep)."),
   ["..####..", ".######.", "##.##.##", "########", "#.#..#.#", "########", ".######.", "..####.."],
   params={"duration": (60.0, 20.0, 300.0, "int")}, magnitude=["duration"])
_g("acid", "effect", 3,
   ("Ácido", "Efeito: fere ignorando a armadura e corrói as peças de armadura do alvo (até 60 pontos de durabilidade cada, nunca as quebra)."),
   ("Acid", "Effect: hurts ignoring armor and corrodes the target's armor pieces (up to 60 durability points each, never breaking them)."),
   ["...##...", "...##...", "..####..", ".######.", ".######.", ".######.", "..####..", "...##..."],
   params={"amount": (3.0, 1.0, 12.0, "float"), "corrode": (8.0, 0.0, 60.0, "int")}, magnitude=["amount"])
_g("electrify", "effect", 3,
   ("Eletrificar", "Efeito: mode 0 (Eletrificar): o dano que o alvo sofre salta 35% para até 2 criaturas num raio de 4. mode 1 (Condutor): raios que caírem a 24 blocos caem no alvo. Dura até 30 s."),
   ("Electrify", "Effect: mode 0 (Electrify): damage the target takes arcs 35% to up to 2 creatures within 4 blocks. mode 1 (Conductor): lightning striking within 24 blocks lands on the target instead. Lasts up to 30 s."),
   ["...###..", "..###...", ".###....", "#######.", "...###..", "..###...", ".##.....", "##......"],
   params={"duration": (200.0, 40.0, 600.0, "int"), "mode": (0.0, 0.0, 1.0, "int")}, magnitude=["duration"])
_g("contagion", "effect", 3,
   ("Contágio", "Efeito: os efeitos nocivos do alvo (e o fogo) passam para as criaturas num raio de 1,5–8, com 60% da duração. Sem efeito nocivo no alvo, nada acontece."),
   ("Contagion", "Effect: the target's harmful effects (and fire) spread to creatures within 1.5-8 blocks, at 60% duration. With no harmful effect on the target, nothing happens."),
   ["..#..#..", ".#.##.#.", "#.####.#", "..####..", "..####..", "#.####.#", ".#.##.#.", "..#..#.."],
   params={"radius": (4.0, 1.5, 8.0, "float")})
_g("reanimate", "effect", 6,
   ("Reanimar Aliado", "Efeito: ergue, perto do alvo, uma criatura que morreu há pouco (até 2 min, não jogador nem chefe), com metade da vida, que luta por você: nunca ataca você nem outros aliados seus e vai atrás de quem agrediu você ou de quem você agrediu. Máximo de 2 por vez e duração de até 2 min."),
   ("Reanimate Ally", "Effect: raises, near the target, a creature that died recently (up to 2 min ago, not a player or boss) at half health, fighting for you: it never attacks you or your other allies and goes after whoever hurt you or whoever you hurt. At most 2 at a time, lasting up to 2 min."),
   ["..####..", ".######.", "##.##.##", "########", "..####..", ".#.##.#.", "#......#", "#......#"],
   params={"duration": (600.0, 100.0, 2400.0, "int")}, magnitude=["duration"])
_g("sense", "effect", 1,
   ("Sentir", "Efeito: só você percebe as criaturas vivas num raio de 4–32 blocos, mesmo atrás de paredes ou invisíveis (partículas só para você) e vê quantas são e quantas hostis."),
   ("Sense", "Effect: only you perceive living creatures within 4-32 blocks, even behind walls or invisible (particles for you only), and see how many there are and how many hostile."),
   ["........", "..####..", ".#.##.#.", "#..##..#", "#..##..#", ".#.##.#.", "..####..", "........"],
   params={"radius": (16.0, 4.0, 32.0, "float")})
_g("arcane_sight", "effect", 2,
   ("Visão Arcana", "Efeito: visão noturna por um tempo e mostra, só para você, resíduos de magia e inscrições num raio de até 32 blocos, com a contagem."),
   ("Arcane Sight", "Effect: night vision for a while and shows, for you only, magic residues and inscriptions within up to 32 blocks, with the count."),
   ["..####..", ".#....#.", "#.#..#.#", "#.####.#", "#.####.#", "#.#..#.#", ".#....#.", "..####.."],
   params={"duration": (400.0, 100.0, 1200.0, "int"), "radius": (24.0, 8.0, 32.0, "float")}, magnitude=["duration"])
_g("emit_signal", "effect", 1,
   ("Emitir Sinal", "Efeito: acende por instantes (0,5 a 10 s) um bloco de Sinal Arcano no ar onde o alvo está, que emite redstone 15 e some sozinho. Só ocupa espaço vazio; máximo 4 por conjuração."),
   ("Emit Signal", "Effect: briefly lights (0.5 to 10 s) an Arcane Signal block in the air where the target is; it emits redstone 15 and vanishes on its own. Only fills empty space; at most 4 per cast."),
   ["...##...", "..#..#..", ".#.##.#.", "#.#..#.#", "#.#..#.#", ".#.##.#.", "..#..#..", "...##..."],
   params={"duration": (40.0, 10.0, 200.0, "int")})
_g("dispel", "effect", 3,
   ("Dissipar", "Efeito: nos outros, tira efeitos benéficos e estados mágicos (Refletir, Eletrificar, Elo, Molhado, Marca); em você, tira os nocivos (Maldição, Silêncio, Petrificar, Medo, Sono) e o fogo."),
   ("Dispel", "Effect: on others, removes beneficial effects and magic states (Reflect, Electrify, Link, Wet, Mark); on yourself, removes harmful ones (Curse, Silence, Petrify, Fear, Sleep) and fire."),
   ["#......#", ".#....#.", "..#..#..", "...##...", "...##...", "..#..#..", ".#....#.", "#......#"])
_g("reflect", "effect", 3,
   ("Refletir", "Efeito: por até 20 s, projéteis que acertam o alvo voltam para quem os lançou e 40% do dano de perto volta ao agressor. Vale para você também."),
   ("Reflect", "Effect: for up to 20 s, projectiles hitting the target fly back at their shooter and 40% of melee damage returns to the attacker. Works on you too."),
   ["...##...", "..####..", ".##..##.", "##.##.##", "##.##.##", ".##..##.", "..####..", "...##..."],
   params={"duration": (100.0, 20.0, 400.0, "int")}, magnitude=["duration"])
_g("silence", "effect", 3,
   ("Silêncio", "Efeito: o alvo não consegue conjurar por até 30 s (jogadores e magos). Não afeta golpes comuns."),
   ("Silence", "Effect: the target cannot cast for up to 30 s (players and mages). Does not affect ordinary attacks."),
   ["..####..", ".#....#.", "#.#..#.#", "#......#", "#.####.#", ".#....#.", "..####..", "#......#"],
   params={"duration": (100.0, 20.0, 600.0, "int")}, magnitude=["duration"])
_g("drain_mana", "effect", 2,
   ("Drenar Mana", "Efeito: tira até 5–100 de mana de jogadores alvo e passa para você. Não afeta criaturas sem mana."),
   ("Drain Mana", "Effect: takes 5-100 mana from target players and gives it to you. Does not affect creatures without mana."),
   ["..####..", ".######.", "#.####.#", "########", "########", ".######.", "..####..", "...##..."],
   params={"amount": (20.0, 5.0, 100.0, "float")}, magnitude=["amount"])
_g("swap", "effect", 3,
   ("Troca", "Efeito: você e a primeira criatura alvo trocam de lugar (mesma dimensão)."),
   ("Swap", "Effect: you and the first target creature trade places (same dimension)."),
   ["..###...", ".#...#..", "#.....#.", ".#...#..", "..#####.", "..#...#.", "...#.#..", "....#..."])
_g("reveal", "effect", 2,
   ("Revelar", "Efeito: tira a invisibilidade e faz brilhar por 5 s as criaturas num raio de 3–24 blocos, e conta quantas estavam escondidas."),
   ("Reveal", "Effect: removes invisibility and makes creatures within 3-24 blocks glow for 5 s, counting how many were hidden."),
   ["...##...", "#..##..#", ".#.##.#.", "..####..", "########", "..####..", ".#.##.#.", "#..##..#"],
   params={"radius": (12.0, 3.0, 24.0, "float")})
_g("decoy", "effect", 2,
   ("Chamariz", "Efeito: uma ilusão no ponto mirado atrai criaturas num raio de 20 blocos e elas deixam de te escolher como alvo por até 15 s."),
   ("Decoy", "Effect: an illusion at the aimed spot draws creatures within 20 blocks and they stop picking you as a target for up to 15 s."),
   ["..####..", ".#....#.", "#.#..#.#", "#......#", "#.####.#", ".#....#.", "..####..", "........"],
   params={"duration": (100.0, 20.0, 300.0, "int")}, magnitude=["duration"])
_g("curse", "effect", 3,
   ("Maldição", "Efeito: por até 2 min o alvo leva 25% a mais de dano e não recupera vida. Dissipar em si mesmo tira."),
   ("Curse", "Effect: for up to 2 min the target takes 25% more damage and cannot recover health. Dispel on yourself removes it."),
   ["#..##..#", ".#.##.#.", "..####..", "########", "#.####.#", "..#..#..", ".#.##.#.", "#..##..#"],
   params={"duration": (300.0, 60.0, 2400.0, "int")}, magnitude=["duration"])

_g("after_hits", "condition", 0,
   ("Após N Acertos", "Condição: arma a magia em você: o que vem depois só acontece depois de você acertar N golpes em outras criaturas (N 2–10, padrão 3; por até 60 s)."),
   ("After N Hits", "Condition: arms the spell on you: what follows only happens after you land N hits on other creatures (N 2-10, default 3; for up to 60 s)."),
   ["#.#.#.#.", "#.#.#.#.", "#.#.#.#.", "#.#.#.#.", "........", "########", "........", "........"],
   params={"n": (3.0, 2.0, 10.0, "int")})
_g("on_signal", "condition", 0,
   ("Ativar por Sinal", "Condição: em uma inscrição no mundo, troca o gatilho: ela dispara quando o bloco recebe sinal de redstone (na subida do sinal), tendo como origem a criatura mais perto (6 blocos). Fora de inscrições, não faz nada."),
   ("Activate on Signal", "Condition: in a world inscription, changes the trigger: it fires when the block receives a redstone signal (on the rising edge), using the nearest creature (6 blocks) as origin. Outside inscriptions it does nothing."),
   ["...##...", "...##...", "..####..", "...##...", "...##...", "...##...", "..####..", "...##..."])

_g("stealth", "modifier", 2,
   ("Furtivo", "Modificador: a magia deixa menos rastro: o resíduo no mundo dura 1/4 do tempo e a conjuração não faz som nem partículas. Não esconde o efeito em si."),
   ("Stealth", "Modifier: the spell leaves less trace: the world residue lasts 1/4 as long and casting makes no sound or particles. It does not hide the effect itself."),
   ["........", "..####..", ".#....#.", "#......#", "#.#..#.#", "#......#", ".#....#.", "........"],
   scope={"target": "flag", "kind": "effect", "mutate": {}})
_g("false_trace", "modifier", 2,
   ("Falso Rastro", "Modificador: o resíduo no mundo leva uma assinatura falsa (que não é a sua) e a magia deixa um segundo resíduo isca a até 10 blocos. Quem analisa o resíduo é enganado."),
   ("False Trace", "Modifier: the world residue carries a false signature (not yours) and the spell leaves a second decoy residue up to 10 blocks away. Whoever analyzes the residue is fooled."),
   ["#......#", ".#....#.", "..#..#..", "...##...", "...##...", "..#..#..", ".#....#.", "#......#"],
   scope={"target": "flag", "kind": "effect", "mutate": {}})
_g("overcharge", "modifier", 3,
   ("Sobrecarga", "Modificador: dobra a força do efeito anterior (dano, cura, empurrão, raio, potência) e 50% de duração, mas você sofre 1 coração de dano mágico a cada efeito sobrecarregado (nunca fatal: só se você tiver mais de 1,5 coração). Mais forte que Potência, com preço."),
   ("Overcharge", "Modifier: doubles the previous effect's strength (damage, heal, push, radius, power) and 50% more duration, but you take 1 heart of magic damage per overcharged effect (never fatal: only if you have more than 1.5 hearts). Stronger than Power, with a price."),
   ["...##...", "..####..", ".######.", "...##...", ".######.", "..####..", "...##...", "........"],
   scope={"target": "previous_effect", "kind": "effect",
          "mutate": {"amount": {"op": "multiply", "value": 2.0}, "strength": {"op": "multiply", "value": 2.0},
                     "power": {"op": "multiply", "value": 2.0}, "radius": {"op": "multiply", "value": 1.5},
                     "duration": {"op": "multiply", "value": 1.5}, "overcharge": {"op": "put", "value": 1.0}}})
_g("bind", "modifier", 2,
   ("Vincular", "Modificador: prende o efeito anterior a você: se for um efeito de poção (Lentidão, Veneno, Cegar, Velocidade...) em outra criatura, ele acaba quando você morrer ou se afastar mais de 40 blocos. Efeitos de outros tipos ignoram o Vincular."),
   ("Bind", "Modifier: ties the previous effect to you: if it is a potion-type effect (Slow, Poison, Blind, Speed...) on another creature, it ends when you die or move more than 40 blocks away. Other effect types ignore Bind."),
   ["..####..", ".#....#.", ".#....#.", "########", "########", ".#....#.", ".#....#.", "..####.."],
   scope={"target": "previous_effect", "kind": "effect", "mutate": {"bind": {"op": "put", "value": 1.0}}})

# ---------------------------------------------------------------- raridade (tier 1-4) e familias (especialidades de dungeon)
TIERS = {
    1: ["self", "projectile", "damage", "heal", "push", "fire", "hostile", "self_filter", "blocks", "area", "more_range",
        "on_hit_entity", "touch", "ignite", "cut", "marked", "unmarked", "not_self", "wood", "power", "group", "end_group",
        "taunt", "wet", "read_item", "sense", "emit_signal"],
    2: ["slow", "poison", "speed", "players", "animals", "undead", "nearest", "wounded", "allies", "delay", "lengthen",
        "weaken", "swift", "after_seconds", "on_hit_block", "sneaking", "night", "in_water", "mark", "blind", "attract",
        "ray", "dig", "shield", "shape_dome", "shape_ring", "shape_cone", "shape_wall", "standing_still", "low_mana", "wave",
        "rain", "charge", "glide", "fear", "rumble", "darkness", "lifesteal", "life_transfer", "invert", "on_jump",
        "target_health", "region", "holding", "raining",
        "hook", "climb", "arcane_sight", "reveal", "decoy", "drain_mana", "stealth", "false_trace", "bind", "on_signal"],
    3: ["levitate", "aura", "pillar", "barrier", "explode", "blink", "freeze", "wither", "repeat", "low_health", "on_hurt",
        "activate", "vow_blood", "on_death", "vow_oath", "shape_sphere", "shape_column", "zone", "on_zone_end", "on_zone_enter", "on_zone_exit", "outsiders", "chain", "sleep", "on_kill", "on_fall",
        "satellites", "link", "trail", "recall", "imbue", "acid", "electrify", "contagion", "dispel", "reflect", "silence", "swap",
        "curse", "overcharge", "after_hits"],
    4: ["thunder", "gravity", "invisible", "blast", "float", "telekinesis", "phase", "petrify", "reanimate"],
}
FAMILIES = {
    "fire": ["fire", "ignite", "explode", "blast"],
    "ice": ["slow", "freeze", "shield", "wet", "rain", "petrify"],
    "storm": ["thunder", "ray", "pillar", "rain", "rumble", "raining", "wet", "electrify", "on_signal", "emit_signal"],
    "blood": ["damage", "poison", "wither", "cut", "wounded", "hostile", "undead", "lifesteal", "life_transfer", "on_kill", "acid", "curse", "reanimate", "contagion", "overcharge", "after_hits"],
    "life": ["heal", "shield", "speed", "allies", "players", "animals"],
    "force": ["hook", "climb", "satellites", "imbue", "reflect", "trail", "charge", "glide", "taunt", "rumble", "on_fall", "on_jump", "float", "telekinesis", "chain", "shape_dome", "shape_sphere", "shape_ring", "push", "attract", "gravity", "levitate", "wave", "aura", "area", "power", "repeat"],
    "knowledge": ["mark", "marked", "unmarked", "nearest", "delay", "after_seconds", "on_hit_entity", "on_hit_block",
                  "group", "end_group", "lengthen", "weaken", "night", "in_water", "sneaking", "standing_still", "low_mana", "vow_stillness", "vow_daylight", "zone", "on_zone_end", "read_item", "target_health", "region", "holding", "sense", "arcane_sight", "reveal", "dispel"],
    "void": ["vow_oath", "vow_blood", "on_death", "warp", "weather", "blink", "invisible", "blind", "on_hurt", "low_health", "activate", "not_self", "self_filter", "fear", "sleep", "darkness", "invert", "phase", "recall", "swap", "link", "silence", "drain_mana", "decoy", "stealth", "false_trace", "bind"],
    "earth": ["summon_wolves", "summon_golem", "dig", "barrier", "blocks", "wood", "more_range", "swift", "origin_aim", "origin_self", "touch", "projectile", "self"],
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
