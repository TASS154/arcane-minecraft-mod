package com.arcane.registry;

import com.arcane.ArcaneMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Efeitos das pocoes de mana. Sao so marcadores: ManaBonuses le se estao ativos e aplica os numeros. */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ArcaneMod.MODID);

    /** Regeneracao de mana acelerada. */
    public static final RegistryObject<MobEffect> MANA_SURGE = EFFECTS.register("mana_surge", () -> new Marker(0x4FD8FF));
    /** Mana maxima aumentada. */
    public static final RegistryObject<MobEffect> MANA_VESSEL = EFFECTS.register("mana_vessel", () -> new Marker(0xA070FF));

    private ModEffects() {}

    public static void register(IEventBus bus) { EFFECTS.register(bus); }

    private static final class Marker extends MobEffect {
        Marker(int color) { super(MobEffectCategory.BENEFICIAL, color); }
    }
}
