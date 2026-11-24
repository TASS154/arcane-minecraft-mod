package com.arcane.execution;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Camada sonora por elemento, sobre os sons proprios do mod (usa sons do jogo base, sem arquivos novos). */
public final class ElementSounds {
  private ElementSounds() {}

  /** Tres variacoes de tom por som (grave, normal, agudo). */
  private static final float[] VARIANTS = {0.82f, 1.0f, 1.18f};

  private static SoundEvent cast(SpellElement e) {
    return switch (e) {
      case FIRE -> SoundEvents.FIRECHARGE_USE;
      case FROST -> SoundEvents.POWDER_SNOW_BREAK;
      case STORM -> SoundEvents.BEACON_POWER_SELECT;
      case VENOM -> SoundEvents.SLIME_SQUISH;
      case FORCE -> SoundEvents.ENDER_DRAGON_FLAP;
      case VOID -> SoundEvents.ENDERMAN_TELEPORT;
      case LIFE -> SoundEvents.AMETHYST_BLOCK_CHIME;
      case EARTH -> SoundEvents.ROOTED_DIRT_BREAK;
      case WATER -> SoundEvents.BUCKET_EMPTY;
      case LIGHT -> SoundEvents.BEACON_ACTIVATE;
      case SHADOW -> SoundEvents.SCULK_SHRIEKER_SHRIEK;
      case BLOOD -> SoundEvents.HONEY_BLOCK_SLIDE;
      case TIME -> SoundEvents.BELL_RESONATE;
      case NATURE -> SoundEvents.AZALEA_LEAVES_PLACE;
      case SOUND -> SoundEvents.NOTE_BLOCK_CHIME.value();
      default -> SoundEvents.ENCHANTMENT_TABLE_USE;
    };
  }

  private static SoundEvent impact(SpellElement e) {
    return switch (e) {
      case FIRE -> SoundEvents.GENERIC_EXPLODE;
      case FROST -> SoundEvents.GLASS_BREAK;
      case STORM -> SoundEvents.LIGHTNING_BOLT_IMPACT;
      case VENOM -> SoundEvents.SLIME_BLOCK_BREAK;
      case FORCE -> SoundEvents.WARDEN_SONIC_BOOM;
      case VOID -> SoundEvents.ENDERMAN_STARE;
      case LIFE -> SoundEvents.AMETHYST_CLUSTER_BREAK;
      case EARTH -> SoundEvents.STONE_BREAK;
      case WATER -> SoundEvents.GENERIC_SPLASH;
      case LIGHT -> SoundEvents.AMETHYST_BLOCK_BREAK;
      case SHADOW -> SoundEvents.SCULK_CLICKING;
      case BLOOD -> SoundEvents.SLIME_BLOCK_STEP;
      case TIME -> SoundEvents.BELL_BLOCK;
      case NATURE -> SoundEvents.CHERRY_LEAVES_BREAK;
      case SOUND -> SoundEvents.NOTE_BLOCK_BELL.value();
      default -> SoundEvents.AMETHYST_BLOCK_RESONATE;
    };
  }

  public static void playCast(ServerLevel level, Vec3 at, SpellElement e) {
    level.playSound(null, at.x, at.y, at.z, cast(e), SoundSource.PLAYERS, 0.5f, VARIANTS[level.random.nextInt(VARIANTS.length)] + level.random.nextFloat() * 0.05f);
  }

  public static void playImpact(ServerLevel level, Vec3 at, SpellElement e) {
    float volume = e == SpellElement.FORCE || e == SpellElement.FIRE ? 0.35f : 0.6f;
    level.playSound(null, at.x, at.y, at.z, impact(e), SoundSource.PLAYERS, volume, 0.9f + level.random.nextFloat() * 0.2f);
  }
}
