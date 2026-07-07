package com.arcane.registry;

import com.arcane.ArcaneMod;
import com.arcane.menu.ArcaneWorkbenchMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ArcaneMod.MODID);

    public static final RegistryObject<MenuType<ArcaneWorkbenchMenu>> ARCANE_WORKBENCH = MENUS.register("arcane_workbench",
            () -> IForgeMenuType.create(ArcaneWorkbenchMenu::new));

    private ModMenuTypes() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
