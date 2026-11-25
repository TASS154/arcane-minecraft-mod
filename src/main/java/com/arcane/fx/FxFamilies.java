package com.arcane.fx;

import java.util.List;
import java.util.Set;

/**
 * Familias de sprites das particulas de elemento. Os sprites saem de {@code tools/gen_art_vfx.py}
 * ({@code element_fx_assets}); cada familia tem {@code particles/fx_<familia>.json} com os quadros.
 */
public final class FxFamilies {
  private FxFamilies() {}

  /** Sprites de particula. As 8 primeiras sao as antigas (ja coloridas); as outras sao em tons de cinza e levam a cor. */
  public static final List<String> SPRITES = List.of(
      "spark", "ember", "frost", "bolt", "venom", "force", "void", "life",
      "soft", "streak", "smoke",
      "earth", "water", "light", "shadow", "blood", "time", "nature", "sound");

  /** Marcas no bloco atingido. */
  public static final List<String> DECALS = List.of("rune", "scorch", "rime", "crack", "splash", "stain", "scar", "bloom");

  /** Sprites que ja vem coloridos (a cor do elemento nao e multiplicada por cima). */
  public static final Set<String> PRECOLORED = Set.of("spark", "ember", "frost", "bolt", "venom", "force", "void", "life");
}
