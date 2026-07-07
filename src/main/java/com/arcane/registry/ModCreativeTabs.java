package com.arcane.registry;

import com.arcane.ArcaneMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcaneMod.MODID);

    public static final RegistryObject<CreativeModeTab> ARCANE = TABS.register("arcane", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.arcane"))
            .icon(() -> new ItemStack(ModItems.ARCANE_PEN.get()))
            .displayItems((params, output) -> {
                output.accept(ModItems.ARCANE_WORKBENCH.get());
                output.accept(ModItems.MANA_CRYSTAL.get());
                output.accept(ModItems.ARCANE_PEN.get());
                output.accept(ModItems.COMMON_INK.get());
                output.accept(ModItems.BLOOD_INK.get());
                output.accept(ModItems.INVISIBLE_INK.get());
                output.accept(ModItems.GLYPH_NOTE.get());
                output.accept(ModItems.ARCANE_MASK.get());
                output.accept(ModItems.INSULATING_CLOAK.get());
                output.accept(ModItems.GLYPH_PROJECTOR.get());
                output.accept(ModItems.SYMMETRY_MIRROR.get());
            }).build());

    private ModCreativeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
