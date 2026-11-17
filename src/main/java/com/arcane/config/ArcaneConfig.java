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
  public static final ForgeConfigSpec.DoubleValue SEALED_CHAMBER_CHANCE;
  public static final ForgeConfigSpec.DoubleValue CRYSTAL_AREA_CAP;
  public static final ForgeConfigSpec.DoubleValue FLIGHT_MANA_PER_TICK;
  public static final ForgeConfigSpec.DoubleValue CRYSTAL_POISON_DAMAGE;
  public static final ForgeConfigSpec.DoubleValue CRYSTAL_WILD_CAST_MAX;
  public static final ForgeConfigSpec.DoubleValue MANA_PER_GLYPH;
  public static final ForgeConfigSpec.DoubleValue MANA_SURGE_MULTIPLIER;
  public static final ForgeConfigSpec.DoubleValue MANA_VESSEL_BONUS;
  public static final ForgeConfigSpec.IntValue MANA_SURGE_TICKS;
  public static final ForgeConfigSpec.IntValue MANA_VESSEL_TICKS;
  public static final ForgeConfigSpec.BooleanValue SCARS_ENABLED;
  public static final ForgeConfigSpec.IntValue SCAR_LIFETIME_TICKS;
  public static final ForgeConfigSpec.BooleanValue WORLD_ALTERING_SPELLS;
  public static final ForgeConfigSpec.BooleanValue BLOCK_BREAKING_SPELLS;
  public static final ForgeConfigSpec.BooleanValue POCKET_ENABLED;
  public static final ForgeConfigSpec.IntValue POCKET_SLOTS;
  public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> POCKET_BLOCKED_ITEMS;
  public static final ForgeConfigSpec.BooleanValue RESTORE_ENABLED;
  public static final ForgeConfigSpec.IntValue RESTORE_WINDOW_SECONDS;
  public static final ForgeConfigSpec.IntValue RESTORE_MAX_BLOCKS;
  public static final ForgeConfigSpec.IntValue RESTORE_JOURNAL_CAP;
  public static final ForgeConfigSpec.BooleanValue BLAST_DROPS_LOOT;
  public static final ForgeConfigSpec.BooleanValue CHORUS_ENABLED;
  public static final ForgeConfigSpec.DoubleValue CHORUS_RADIUS;
  public static final ForgeConfigSpec.IntValue CHORUS_WINDOW_TICKS;
  public static final ForgeConfigSpec.IntValue CHORUS_MAX_PARTICIPANTS;

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
    SEALED_CHAMBER_CHANCE = builder.comment("Chance per newly generated Overworld chunk of an underground sealed-creature chamber (0 disables)").defineInRange("sealedChamberChance", 0.008, 0.0, 1.0);
    FLIGHT_MANA_PER_TICK = builder.comment("Mana drained every tick while the Float flight lasts, per block/tick of flight speed (0 = free flight) (PROVISIONAL)").defineInRange("flightManaPerTick", 0.25, 0.0, 10.0);
    CRYSTAL_AREA_CAP = builder.comment("Ceiling of the crystal's multiplier on spell radius/area (the zone multiplier goes up to 32)").defineInRange("crystalAreaCap", 32.0, 1.0, 32.0);
    CRYSTAL_POISON_DAMAGE = builder.comment("Mana Poisoning damage multiplier near crystals (1 = base)").defineInRange("crystalPoisonDamage", 1.0, 0.0, 20.0);
    CRYSTAL_WILD_CAST_MAX = builder.comment("Max chance (percent per second, at the innermost zone) of a random grimoire spell firing near a crystal; each second a percentage up to the zone limit is drawn (0 disables)").defineInRange("crystalWildCastMaxPercent", 30.0, 0.0, 100.0);
    MANA_PER_GLYPH = builder.comment("Extra max mana per discovered glyph (partial knowledge or better) (PROVISIONAL)").defineInRange("manaPerGlyph", 1.5, 0.0, 50.0);
    MANA_SURGE_MULTIPLIER = builder.comment("Mana regeneration multiplier while under the Mana Surge potion (PROVISIONAL)").defineInRange("manaSurgeMultiplier", 4.0, 1.0, 50.0);
    MANA_VESSEL_BONUS = builder.comment("Max mana bonus of the Mana Vessel potion, as a fraction of the base max (PROVISIONAL)").defineInRange("manaVesselBonus", 0.5, 0.0, 10.0);
    MANA_SURGE_TICKS = builder.comment("Duration of the Mana Surge potion, in ticks").defineInRange("manaSurgeTicks", 6000, 20, 1728000);
    MANA_VESSEL_TICKS = builder.comment("Duration of the Mana Vessel potion, in ticks").defineInRange("manaVesselTicks", 6000, 20, 1728000);
    SCARS_ENABLED = builder.comment("Powerful spells leave temporary scars in the world").define("scarsEnabled", true);
    SCAR_LIFETIME_TICKS = builder.comment("How long a world scar lasts, in ticks (PROVISIONAL)").defineInRange("scarLifetimeTicks", 24000, 200, 1728000);
    WORLD_ALTERING_SPELLS = builder.comment("Spells may place or change blocks (barriers, fire, ice, growth...). Turn off on servers that want no terrain changes")
        .define("worldAlteringSpells", true);
    BLOCK_BREAKING_SPELLS = builder.comment("Spells may destroy blocks (Dig, Cut, Damage on fragile blocks). Turn off to protect builds")
        .define("blockBreakingSpells", true);
    POCKET_ENABLED = builder.comment("Arcane Pocket: a per-player ender-chest-like storage opened by the Pocket glyph").define("pocketEnabled", true);
    POCKET_SLOTS = builder.comment("Usable slots of the Arcane Pocket (half of a small chest = 13; the menu shows 2 rows and the rest stay locked)").defineInRange("pocketSlots", 13, 1, 18);
    POCKET_BLOCKED_ITEMS = builder.comment("Item ids that cannot go in the Pocket (shulker boxes, the grimoire and pages are always blocked)")
        .defineList("pocketBlockedItems", java.util.List.of("minecraft:bundle"), o -> o instanceof String);
    RESTORE_ENABLED = builder.comment("Restore glyph: recompose blocks that spells destroyed recently").define("restoreEnabled", true);
    RESTORE_WINDOW_SECONDS = builder.comment("How long a destroyed block stays restorable (PROVISIONAL)").defineInRange("restoreWindowSeconds", 120, 5, 3600);
    RESTORE_MAX_BLOCKS = builder.comment("Max blocks one Restore cast recomposes (PROVISIONAL)").defineInRange("restoreMaxBlocksPerCast", 256, 1, 4096);
    RESTORE_JOURNAL_CAP = builder.comment("Max journal entries per dimension; the oldest are dropped first").defineInRange("restoreJournalCap", 4096, 64, 65536);
    BLAST_DROPS_LOOT = builder.comment("If true, blocks destroyed by explosions and other destructive spells drop items (Dig always does). Those blocks are then NOT recorded for Restore, to avoid a loot exploit")
        .define("blastDropsLoot", false);
    CHORUS_ENABLED = builder.comment("Chorus modifier: spells that need several players casting together").define("chorusEnabled", true);
    CHORUS_RADIUS = builder.comment("Radius (blocks) of the chorus circle around the first caster (PROVISIONAL)").defineInRange("chorusRadius", 8.0, 2.0, 32.0);
    CHORUS_WINDOW_TICKS = builder.comment("Ticks the others have to cast after the first caster opens the chorus (PROVISIONAL)").defineInRange("chorusWindowTicks", 60, 10, 400);
    CHORUS_MAX_PARTICIPANTS = builder.comment("Max players counted in one chorus").defineInRange("chorusMaxParticipants", 5, 2, 16);
    SPEC = builder.build();
  }
}
