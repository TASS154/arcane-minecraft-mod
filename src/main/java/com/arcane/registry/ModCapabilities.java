package com.arcane.registry;

import com.arcane.ArcaneMod;
import com.arcane.execution.ManaState;
import com.arcane.grimoire.GrimoireState;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ModCapabilities {
    public static final Capability<ManaState> MANA = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<GrimoireState> GRIMOIRE = CapabilityManager.get(new CapabilityToken<>() {});

    private ModCapabilities() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ModCapabilities::registerCapabilities);
    }

    private static void registerCapabilities(net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent event) {
        event.register(ManaState.class);
        event.register(GrimoireState.class);
    }

    @Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
    public static class Events {
        @SubscribeEvent
        public static void attach(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
            if (event.getObject() instanceof net.minecraft.world.entity.player.Player) {
                event.addCapability(new ResourceLocation(ArcaneMod.MODID, "mana"), new PlayerDataProvider());
                event.addCapability(new ResourceLocation(ArcaneMod.MODID, "grimoire"), new GrimoireDataProvider());
            }
        }

        @SubscribeEvent
        public static void clone(PlayerEvent.Clone event) {
            event.getOriginal().getCapability(MANA).ifPresent(oldMana ->
                    event.getEntity().getCapability(MANA).ifPresent(newMana -> {
                        CompoundTag tag = new CompoundTag();
                        oldMana.save(tag);
                        newMana.load(tag);
                    }));
            event.getOriginal().getCapability(GRIMOIRE).ifPresent(oldGrimoire ->
                    event.getEntity().getCapability(GRIMOIRE).ifPresent(newGrimoire -> {
                        CompoundTag tag = new CompoundTag();
                        oldGrimoire.save(tag);
                        newGrimoire.load(tag);
                    }));
        }

        @SubscribeEvent
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            event.getEntity().getCapability(GRIMOIRE).ifPresent(grimoire -> {
                if (grimoire.spellbook().isEmpty()) {
                    GrimoireState defaults = GrimoireState.createDefault();
                    grimoire.load(defaultsToTag(defaults));
                }
            });
        }

        private static CompoundTag defaultsToTag(GrimoireState defaults) {
            CompoundTag tag = new CompoundTag();
            defaults.save(tag);
            return tag;
        }
    }

    public static class PlayerDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final ManaState mana = new ManaState();
        private final LazyOptional<ManaState> optional = LazyOptional.of(() -> mana);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == MANA ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            mana.save(tag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            mana.load(tag);
        }
    }

    public static class GrimoireDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final GrimoireState grimoire = new GrimoireState();
        private final LazyOptional<GrimoireState> optional = LazyOptional.of(() -> grimoire);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == GRIMOIRE ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            grimoire.save(tag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            grimoire.load(tag);
        }
    }
}
