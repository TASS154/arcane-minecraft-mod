package com.arcane.registry;

import com.arcane.ArcaneMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
  public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ArcaneMod.MODID);

  /** Faisca arcana: projeteis, cristais, escrita e conjuracao. */
  public static final RegistryObject<SimpleParticleType> ARCANE_SPARK = reg("arcane_spark");
  /** Particulas por elemento (ver SpellElement). */
  public static final RegistryObject<SimpleParticleType> ARCANE_EMBER = reg("arcane_ember");
  public static final RegistryObject<SimpleParticleType> ARCANE_FROST = reg("arcane_frost");
  public static final RegistryObject<SimpleParticleType> ARCANE_BOLT = reg("arcane_bolt");
  public static final RegistryObject<SimpleParticleType> ARCANE_VENOM = reg("arcane_venom");
  public static final RegistryObject<SimpleParticleType> ARCANE_FORCE = reg("arcane_force");
  public static final RegistryObject<SimpleParticleType> ARCANE_VOID = reg("arcane_void");
  public static final RegistryObject<SimpleParticleType> ARCANE_LIFE = reg("arcane_life");

  private ModParticles() {}

  private static RegistryObject<SimpleParticleType> reg(String name) {
    return PARTICLES.register(name, () -> new SimpleParticleType(false));
  }

  public static void register(IEventBus bus) {
    PARTICLES.register(bus);
  }
}
