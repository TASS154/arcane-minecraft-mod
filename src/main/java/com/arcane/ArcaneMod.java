package com.arcane;

import com.arcane.glyph.GlyphBootstrap;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.grimoire.GrimoireLifecycleHandler;
import com.arcane.inscribing.InscribingHandler;
import com.arcane.network.ModNetwork;
import com.arcane.registry.ModAttachments;
import com.arcane.registry.ModBlocks;
import com.arcane.registry.ModCreativeTabs;
import com.arcane.registry.ModEntities;
import com.arcane.registry.ModItems;
import com.arcane.registry.ModMenuTypes;
import com.arcane.registry.ModParticles;
import com.arcane.registry.ModSounds;
import com.arcane.config.ArcaneConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ArcaneMod.MODID)
public class ArcaneMod {
    public static final String MODID = "arcane";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ArcaneMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModEntities.register(modBus);
        ModMenuTypes.register(modBus);
        ModCreativeTabs.register(modBus);
        ModAttachments.register(modBus);
        ModSounds.register(modBus);
        ModParticles.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ArcaneConfig.SPEC);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::onConfig);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new GrimoireLifecycleHandler());
        MinecraftForge.EVENT_BUS.register(new InscribingHandler());
        MinecraftForge.EVENT_BUS.register(new com.arcane.world.ArcaneLootInjector());
    }

    private void onConfig(final net.minecraftforge.fml.event.config.ModConfigEvent event) {
        if (event.getConfig().getSpec() == ArcaneConfig.SPEC) {
            com.arcane.inscription.SpellLimits.set(ArcaneConfig.MAX_GLYPHS.get(), ArcaneConfig.COST_GROWTH.get(),
                    ArcaneConfig.COST_FREE_GLYPHS.get());
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
            GlyphBootstrap.registerDefaults();
            LOGGER.info("Arcane mod initialized — modular glyph magic system ready.");
        });
    }

    private void onAddReloadListeners(final AddReloadListenerEvent event) {
        event.addListener(GlyphRegistry.reloadListener());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Arcane server starting with {} glyphs loaded.", GlyphRegistry.size());
    }
}
