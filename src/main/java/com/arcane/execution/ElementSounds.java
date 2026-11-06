package com.arcane.execution;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Camada sonora por elemento, sobre os sons proprios do mod (usa sons do jogo base, sem arquivos novos). */
public final class ElementSounds {
  private ElementSounds() {}

  private static SoundEvent cast(SpellElement e) {
    return switch (e) {
      case FIRE -> SoundEvents.FIRECHARGE_USE;
      case FROST -> SoundEvents.POWDER_SNOW_BREAK;
      case STORM -> SoundEvents.BEACON_POWER_SELECT;
      case VENOM -> SoundEvents.SLIME_SQUISH;
      case FORCE -> SoundEvents.ENDER_DRAGON_FLAP;
      case VOID -> SoundEvents.ENDERMAN_TELEPORT;
      case LIFE -> SoundEvents.AMETHYST_BLOCK_CHIME;
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
      default -> SoundEvents.AMETHYST_BLOCK_RESONATE;
    };
  }

  public static void playCast(ServerLevel level, Vec3 at, SpellElement e) {
    level.playSound(null, at.x, at.y, at.z, cast(e), SoundSource.PLAYERS, 0.5f, 0.9f + level.random.nextFloat() * 0.2f);
  }

  public static void playImpact(ServerLevel level, Vec3 at, SpellElement e) {
    float volume = e == SpellElement.FORCE || e == SpellElement.FIRE ? 0.35f : 0.6f;
    level.playSound(null, at.x, at.y, at.z, impact(e), SoundSource.PLAYERS, volume, 0.9f + level.random.nextFloat() * 0.2f);
  }
}
