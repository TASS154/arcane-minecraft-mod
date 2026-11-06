package com.arcane.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ArcaneConfig {
  public static final ForgeConfigSpec SPEC;
  public static final ForgeConfigSpec.DoubleValue MAX_AMBIENT_POTENCY;
  public static final ForgeConfigSpec.IntValue MAX_RITUAL_SIZE;
  public static final ForgeConfigSpec.IntValue MAX_GLYPHS;
  public static final ForgeConfigSpec.DoubleValue COST_GROWTH;
  public static final ForgeConfigSpec.IntValue COST_FREE_GLYPHS;
  public static final ForgeConfigSpec.DoubleValue RITUAL_POWER_MINOR;
  public static final ForgeConfigSpec.DoubleValue RITUAL_POWER_MEDIUM;
  public static final ForgeConfigSpec.DoubleValue RITUAL_POWER_MAJOR;
  public static final ForgeConfigSpec.DoubleValue RITUAL_MIN_COMPLETENESS;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    MAX_AMBIENT_POTENCY = builder.comment("Maximum ambient potency multiplier").defineInRange("maxAmbientPotency", 32.0, 1.0, 64.0);
    MAX_RITUAL_SIZE = builder.comment("Maximum ritual size in blocks").defineInRange("maxRitualSize", 64, 8, 128);
    MAX_GLYPHS = builder.comment("Maximum glyphs in one spell").defineInRange("maxGlyphsPerSpell", 10, 3, 24);
    COST_GROWTH = builder.comment("Mana cost multiplier per glyph beyond the free ones (exponential growth). PROVISIONAL balance value")
        .defineInRange("costGrowthPerGlyph", 1.3, 1.0, 3.0);
    COST_FREE_GLYPHS = builder.comment("Glyphs that do not add to the exponential surcharge")
        .defineInRange("costFreeGlyphs", 3, 0, 24);
    RITUAL_POWER_MINOR = builder.comment("Power multiplier of a minor ritual (radius 4 circle). PROVISIONAL balance value")
        .defineInRange("ritualPowerMinor", 1.5, 1.0, 100.0);
    RITUAL_POWER_MEDIUM = builder.comment("Power multiplier of a medium ritual (radius 8). PROVISIONAL")
        .defineInRange("ritualPowerMedium", 3.0, 1.0, 100.0);
    RITUAL_POWER_MAJOR = builder.comment("Power multiplier of a major ritual (radius 16). PROVISIONAL")
        .defineInRange("ritualPowerMajor", 10.0, 1.0, 100.0);
    RITUAL_MIN_COMPLETENESS = builder.comment("Fraction of the circle's traces that must be drawn for a ritual to work")
        .defineInRange("ritualMinCompleteness", 0.85, 0.3, 1.0);
    SPEC = builder.build();
  }
}
