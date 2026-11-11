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

    /** Flutuar: voo suave guiado pelo olhar (o movimento roda no cliente; a amplificacao guarda a velocidade x10). */
    public static final RegistryObject<MobEffect> FLOAT_FLIGHT = EFFECTS.register("float_flight", FloatFlight::new);

    private ModEffects() {}

    public static void register(IEventBus bus) { EFFECTS.register(bus); }

    private static final class Marker extends MobEffect {
        Marker(int color) { super(MobEffectCategory.BENEFICIAL, color); }
    }

    /**
     * Enquanto dura: o jogador pode "voar" (permissao de voo, para o servidor nao expulsar por flutuar), nao sofre queda e
     * criaturas sem controle ficam sem gravidade. Ao acabar, devolve exatamente o que tinha.
     */
    public static final class FloatFlight extends MobEffect {
        private static final String GRANTED = "arcane_float_mayfly";

        FloatFlight() { super(MobEffectCategory.BENEFICIAL, 0xBFA8FF); }

        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) { return true; }

        @Override
        public void applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amplifier) {
            entity.fallDistance = 0.0f;
            if (entity.level().isClientSide) return;
            if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
                var abilities = player.getAbilities();
                if (!abilities.mayfly) {
                    abilities.mayfly = true;
                    player.getPersistentData().putBoolean(GRANTED, true);
                    player.onUpdateAbilities();
                }
            } else {
                entity.setNoGravity(true);
            }
        }

        @Override
        public void removeAttributeModifiers(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap map, int amplifier) {
            super.removeAttributeModifiers(entity, map, amplifier);
            if (entity.level().isClientSide) return;
            if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
                if (player.getPersistentData().getBoolean(GRANTED)) {
                    player.getPersistentData().remove(GRANTED);
                    if (!player.isCreative() && !player.isSpectator()) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                    }
                }
            } else {
                entity.setNoGravity(false);
            }
        }
    }
}
