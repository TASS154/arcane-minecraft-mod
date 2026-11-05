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
  public static final RegistryObject<SimpleParticleType> ARCANE_SPARK = PARTICLES.register("arcane_spark",
      () -> new SimpleParticleType(false));

  private ModParticles() {}

  public static void register(IEventBus bus) {
    PARTICLES.register(bus);
  }
}
