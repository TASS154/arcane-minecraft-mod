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
  /** Animacoes grandes: raio que cai e onda de choque (6 quadros cada). */
  public static final RegistryObject<SimpleParticleType> ARCANE_BOLT_STRIKE = reg("arcane_bolt_strike");
  public static final RegistryObject<SimpleParticleType> ARCANE_SHOCK = reg("arcane_shock");

  /**
   * Particulas de elemento ({@link com.arcane.fx.Fx}): um tipo por familia de sprites ({@code fx_<familia>}) e por decal
   * ({@code fx_decal_<nome>}). O elemento, o papel e a cor vao nas opcoes; o movimento vem do json do elemento.
   */
  public static final java.util.Map<String, RegistryObject<ParticleType<com.arcane.fx.ElementFxOptions>>> FX = new java.util.LinkedHashMap<>();

  static {
    for (String family : com.arcane.fx.FxFamilies.SPRITES) FX.put(family, fx("fx_" + family));
    for (String decal : com.arcane.fx.FxFamilies.DECALS) FX.put("decal_" + decal, fx("fx_decal_" + decal));
  }

  private ModParticles() {}

  private static RegistryObject<SimpleParticleType> reg(String name) {
    return PARTICLES.register(name, () -> new SimpleParticleType(false));
  }

  @SuppressWarnings("deprecation")
  private static RegistryObject<ParticleType<com.arcane.fx.ElementFxOptions>> fx(String name) {
    return PARTICLES.register(name, () -> new ParticleType<com.arcane.fx.ElementFxOptions>(false, com.arcane.fx.ElementFxOptions.DESERIALIZER) {
      @Override
      public com.mojang.serialization.Codec<com.arcane.fx.ElementFxOptions> codec() {
        return com.arcane.fx.ElementFxOptions.codec(this);
      }
    });
  }

  /** Tipo da familia (familia desconhecida: a faisca). */
  public static ParticleType<com.arcane.fx.ElementFxOptions> fx(String family, boolean decal) {
    var ro = FX.get(decal ? "decal_" + family : family);
    if (ro == null) ro = FX.get(decal ? "decal_rune" : "spark");
    return ro.get();
  }

  public static void register(IEventBus bus) {
    PARTICLES.register(bus);
  }
}
