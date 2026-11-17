package com.arcane.client;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Opcoes do jogador (cliente): modo de uso do grimorio e acessibilidade. Ficam em config/arcane-client.toml.
 * As teclas (conjurar, trocar, grimorio, roda, conjunto) sao configuraveis em Controles, categoria Arcane.
 */
public final class ClientPrefs {
  /** Como o jogador escolhe a magia: roda radial (segurar uma tecla), conjuntos (tecla troca o conjunto, B a magia) ou teclas 1 a 9. */
  public enum UseMode { RADIAL, LOADOUT, CASTER }

  public static final ForgeConfigSpec SPEC;
  public static final ForgeConfigSpec.EnumValue<UseMode> USE_MODE;
  public static final ForgeConfigSpec.BooleanValue LATCH_KEYS;
  public static final ForgeConfigSpec.BooleanValue COLORBLIND;
  public static final ForgeConfigSpec.BooleanValue REDUCE_FLASHES;

  static {
    ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
    USE_MODE = b.comment("How you pick spells: RADIAL (hold a key), LOADOUT (sets of up to 6 spells) or CASTER (number keys 1-9)")
        .defineEnum("useMode", UseMode.RADIAL);
    LATCH_KEYS = b.comment("Accessibility: press once to start and once to finish, instead of holding keys (radial wheel, charge and hold casting)")
        .define("toggleInsteadOfHold", false);
    COLORBLIND = b.comment("Accessibility: colour-blind friendly palette (blue/orange/yellow) plus letters on glyph categories").define("colorblind", false);
    REDUCE_FLASHES = b.comment("Accessibility: weaker and shorter screen flashes when casting").define("reduceFlashes", false);
    SPEC = b.build();
  }

  private ClientPrefs() {}

  public static UseMode mode() { return USE_MODE.get(); }

  public static boolean latch() { return LATCH_KEYS.get(); }

  public static boolean colorblind() { return COLORBLIND.get(); }

  public static boolean reduceFlashes() { return REDUCE_FLASHES.get(); }

  public static void cycleMode() {
    UseMode[] all = UseMode.values();
    USE_MODE.set(all[(mode().ordinal() + 1) % all.length]);
    SPEC.save();
  }

  public static void flip(ForgeConfigSpec.BooleanValue value) {
    value.set(!value.get());
    SPEC.save();
  }
}
