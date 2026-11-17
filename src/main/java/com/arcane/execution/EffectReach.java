package com.arcane.execution;

import java.util.Map;

/**
 * Quais tipos de alvo cada forma entrega e cada efeito sabe tratar (criatura, bloco, ponto vazio). E a tabela que a
 * previa da bancada usa para avisar de combinacoes que nao fazem nada. Mantida junto dos handlers: ao dar a um efeito
 * um comportamento novo (ex.: Dano em blocos), atualize o mapa.
 */
public final class EffectReach {
  public static final int ENTITY = 1, BLOCK = 2, POINT = 4, ALL = 7;

  private EffectReach() {}

  private static final Map<String, Integer> FORMS = Map.ofEntries(
      Map.entry("self", ENTITY), Map.entry("touch", ENTITY | BLOCK), Map.entry("projectile", ENTITY | BLOCK),
      Map.entry("ray", ALL), Map.entry("aura", ENTITY), Map.entry("wave", ENTITY),
      Map.entry("pillar", ALL), Map.entry("barrier", ENTITY | BLOCK),
      Map.entry("summon_wolves", ENTITY), Map.entry("summon_golem", ENTITY));

  private static final Map<String, Integer> EFFECTS = Map.ofEntries(
      Map.entry("damage", ENTITY | BLOCK), Map.entry("cut", ENTITY | BLOCK), Map.entry("fire", ALL),
      Map.entry("ignite", BLOCK | POINT), Map.entry("push", ALL), Map.entry("heal", ALL),
      Map.entry("slow", ALL), Map.entry("poison", ALL), Map.entry("speed", ALL), Map.entry("levitate", ALL),
      Map.entry("shield", ALL), Map.entry("blind", ALL), Map.entry("wither", ALL), Map.entry("invisible", ALL),
      Map.entry("thunder", ALL), Map.entry("explode", ALL), Map.entry("freeze", ALL),
      Map.entry("blast", ALL), Map.entry("dig", BLOCK), Map.entry("gravity", ENTITY | BLOCK), Map.entry("attract", ENTITY), Map.entry("mark", ENTITY),
      Map.entry("float", ENTITY), Map.entry("telekinesis", ENTITY | BLOCK),
      Map.entry("rain", ALL), Map.entry("charge", ALL), Map.entry("glide", ENTITY), Map.entry("taunt", ENTITY),
      Map.entry("fear", ENTITY), Map.entry("sleep", ENTITY), Map.entry("rumble", ALL), Map.entry("darkness", ALL),
      Map.entry("satellites", ENTITY), Map.entry("link", ENTITY), Map.entry("trail", ENTITY), Map.entry("hook", ALL),
      Map.entry("climb", ENTITY), Map.entry("phase", ALL), Map.entry("recall", ALL), Map.entry("imbue", ENTITY),
      Map.entry("petrify", ENTITY), Map.entry("acid", ENTITY), Map.entry("electrify", ENTITY), Map.entry("contagion", ENTITY),
      Map.entry("reanimate", ALL), Map.entry("sense", ALL), Map.entry("arcane_sight", ALL), Map.entry("emit_signal", ALL),
      Map.entry("dispel", ENTITY), Map.entry("reflect", ENTITY), Map.entry("silence", ENTITY), Map.entry("drain_mana", ENTITY),
      Map.entry("swap", ENTITY), Map.entry("reveal", ALL), Map.entry("decoy", ALL), Map.entry("curse", ENTITY),
      Map.entry("wet", ENTITY), Map.entry("lifesteal", ENTITY), Map.entry("life_transfer", ENTITY), Map.entry("read_item", ENTITY), Map.entry("pocket", ALL),
      // Nao dependem dos alvos: agem sobre o conjurador ou o mundo.
      Map.entry("blink", ALL), Map.entry("weather", ALL), Map.entry("warp", ALL));

  /** Efeitos nocivos: o conjurador nao os sofre vindos de formas que nao sejam a forma Eu. */
  private static final java.util.Set<String> HARMFUL = java.util.Set.of("damage", "cut", "fire", "slow", "poison", "blind",
      "wither", "freeze", "gravity", "push", "attract", "thunder", "blast", "explode", "telekinesis",
      "taunt", "fear", "sleep", "rumble", "darkness", "lifesteal",
      "petrify", "acid", "curse", "silence", "drain_mana", "swap", "contagion");

  /** Efeitos que sabem encadear (modificador Cadeia). */
  private static final java.util.Set<String> CHAINABLE = java.util.Set.of("damage", "explode", "blast");

  public static boolean harmful(String path) { return HARMFUL.contains(path); }
  public static boolean chainable(String path) { return CHAINABLE.contains(path); }

  /** Formas que usam formato e origem (via Region). */
  private static final java.util.Set<String> REGION_FORMS = java.util.Set.of("aura", "wave", "pillar", "barrier");

  /** Efeitos que nao dependem dos alvos (agem sobre o conjurador ou o mundo). */
  public static boolean independent(String path) {
    return path.equals("blink") || path.equals("weather") || path.equals("warp") || path.equals("recall") || path.equals("phase")
        || path.equals("sense") || path.equals("arcane_sight") || path.equals("pocket") || path.equals("restore");
  }
  public static int ofForm(String path) { return FORMS.getOrDefault(path, ALL); }
  public static int ofEffect(String path) { return EFFECTS.getOrDefault(path, ALL); }
  public static boolean usesRegion(String formPath) { return REGION_FORMS.contains(formPath); }

  /** O que sobra de alvos depois de um filtro: os de bloco ficam so com blocos; os demais so com criaturas. */
  public static int afterFilter(String filterPath, int mask) {
    return switch (filterPath) {
      case "blocks", "wood" -> mask & BLOCK;
      default -> mask & ENTITY;
    };
  }
}
