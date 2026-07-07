package com.arcane.client;

import com.arcane.ArcaneMod;
import com.arcane.client.screen.ArcaneWorkbenchScreen;
import com.arcane.entity.GrimoireEntity;
import com.arcane.menu.ArcaneWorkbenchMenu;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.C2SCastRequestPacket;
import com.arcane.registry.ModEntities;
import com.arcane.registry.ModMenuTypes;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
  public static KeyMapping CAST_KEY;

  @SubscribeEvent
  public static void onClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> {
      MenuScreens.register(ModMenuTypes.ARCANE_WORKBENCH.get(), ArcaneWorkbenchScreen::new);
      EntityRenderers.register(ModEntities.GRIMOIRE.get(), NoopRenderer::new);
      EntityRenderers.register(ModEntities.ARCANE_PROJECTILE.get(),
          ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.0f, true));
      EntityRenderers.register(ModEntities.NPC_MAGE.get(),
          ctx -> new net.minecraft.client.renderer.entity.ZombieRenderer<>(ctx));
    });
  }

  @SubscribeEvent
  public static void registerKeys(RegisterKeyMappingsEvent event) {
    CAST_KEY = new KeyMapping("key.arcane.cast", GLFW.GLFW_KEY_V, "key.categories.arcane");
    event.register(CAST_KEY);
  }

  public static void tryCast() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.player.connection == null) return;
    com.arcane.grimoire.GrimoireAttachments.get(mc.player).flatMap(g -> g.activeInscriptionId())
        .ifPresent(id -> ModNetwork.CHANNEL.sendToServer(new C2SCastRequestPacket(id, false)));
  }
}
