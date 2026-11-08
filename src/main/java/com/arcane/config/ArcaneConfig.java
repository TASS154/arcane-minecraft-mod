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
  public static final ForgeConfigSpec.BooleanValue ELEMENT_INTERACTIONS;
  public static final ForgeConfigSpec.DoubleValue ELEMENT_INTERACTION_STRENGTH;
  public static final ForgeConfigSpec.IntValue MAGE_CAST_INTERVAL_TICKS;
  public static final ForgeConfigSpec.IntValue GRIMOIRE_LOSS_DAYS;
  public static final ForgeConfigSpec.BooleanValue SCARS_ENABLED;
  public static final ForgeConfigSpec.IntValue SCAR_LIFETIME_TICKS;
  public static final ForgeConfigSpec.BooleanValue WORLD_ALTERING_SPELLS;
  public static final ForgeConfigSpec.BooleanValue BLOCK_BREAKING_SPELLS;

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
    ELEMENT_INTERACTIONS = builder.comment("Elements interact with the environment (storm stronger in rain, fire weaker when wet...)")
        .define("elementInteractions", true);
    ELEMENT_INTERACTION_STRENGTH = builder.comment("Scale of the element interaction bonuses (0 = none, 1 = default)")
        .defineInRange("elementInteractionStrength", 1.0, 0.0, 3.0);
    MAGE_CAST_INTERVAL_TICKS = builder.comment("Ticks between NPC mage casts").defineInRange("mageCastIntervalTicks", 80, 10, 1200);
    GRIMOIRE_LOSS_DAYS = builder.comment("Days of reduced mana after losing the grimoire (PROVISIONAL)").defineInRange("grimoireLossDays", 3, 0, 30);
    SCARS_ENABLED = builder.comment("Powerful spells leave temporary scars in the world").define("scarsEnabled", true);
    SCAR_LIFETIME_TICKS = builder.comment("How long a world scar lasts, in ticks (PROVISIONAL)").defineInRange("scarLifetimeTicks", 24000, 200, 1728000);
    WORLD_ALTERING_SPELLS = builder.comment("Spells may place or change blocks (barriers, fire, ice, growth...). Turn off on servers that want no terrain changes")
        .define("worldAlteringSpells", true);
    BLOCK_BREAKING_SPELLS = builder.comment("Spells may destroy blocks (Dig, Cut, Damage on fragile blocks). Turn off to protect builds")
        .define("blockBreakingSpells", true);
    SPEC = builder.build();
  }
}
