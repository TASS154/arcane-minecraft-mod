package com.arcane.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ArcaneConfig {
  public static final ForgeConfigSpec SPEC;
  public static final ForgeConfigSpec.DoubleValue MAX_AMBIENT_POTENCY;
  public static final ForgeConfigSpec.IntValue MAX_RITUAL_SIZE;
  public static final ForgeConfigSpec.IntValue MAX_GLYPHS;
  public static final ForgeConfigSpec.DoubleValue COST_GROWTH;
  public static final ForgeConfigSpec.IntValue COST_FREE_GLYPHS;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    MAX_AMBIENT_POTENCY = builder.comment("Maximum ambient potency multiplier").defineInRange("maxAmbientPotency", 32.0, 1.0, 64.0);
    MAX_RITUAL_SIZE = builder.comment("Maximum ritual size in blocks").defineInRange("maxRitualSize", 64, 8, 128);
    MAX_GLYPHS = builder.comment("Maximum glyphs in one spell").defineInRange("maxGlyphsPerSpell", 10, 3, 24);
    COST_GROWTH = builder.comment("Mana cost multiplier per glyph beyond the free ones (exponential growth). PROVISIONAL balance value")
        .defineInRange("costGrowthPerGlyph", 1.3, 1.0, 3.0);
    COST_FREE_GLYPHS = builder.comment("Glyphs that do not add to the exponential surcharge")
        .defineInRange("costFreeGlyphs", 3, 0, 24);
    SPEC = builder.build();
  }
}
