package com.arcane.client;

import com.arcane.ArcaneMod;
import com.arcane.client.hud.ManaHudOverlay;
import com.arcane.client.particle.ArcaneSparkParticle;
import com.arcane.client.render.ArcaneProjectileRenderer;
import com.arcane.client.render.GrimoireRenderer;
import com.arcane.client.render.NPCMageRenderer;
import com.arcane.client.screen.ArcaneWorkbenchScreen;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.C2SCastRequestPacket;
import com.arcane.network.packet.C2SSelectInscriptionPacket;
import com.arcane.registry.ModEntities;
import com.arcane.registry.ModMenuTypes;
import com.arcane.registry.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
  public static KeyMapping CAST_KEY;
  public static KeyMapping RADIAL_KEY;
  public static KeyMapping CYCLE_KEY;
  public static KeyMapping GRIMOIRE_KEY;

  /** Cetro: a gema (tinta 0) e o metal (tinta 1) sao pintados com as cores escolhidas na tela do cetro. */
  @SubscribeEvent
  public static void onItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
    event.register((stack, index) -> com.arcane.item.ScepterItem.tint(stack, index), com.arcane.registry.ModItems.SCEPTER.get());
  }

  @SubscribeEvent
  public static void onClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> MenuScreens.register(ModMenuTypes.ARCANE_WORKBENCH.get(), ArcaneWorkbenchScreen::new));
    // Cetro: a ponta e a haste escolhem o modelo (ver models/item/scepter.json).
    event.enqueueWork(() -> {
      net.minecraft.client.renderer.item.ItemProperties.register(com.arcane.registry.ModItems.MANA_COMPASS.get(), new net.minecraft.resources.ResourceLocation("arcane", "angle"),
          (stack, level, entity, seed) -> {
            var holder = entity != null ? entity : stack.getEntityRepresentation();
            if (holder == null) return 0f;
            var lvl = level != null ? level : holder.level();
            var target = com.arcane.item.ManaCompassItem.target(stack, lvl);
            if (target == null) return (float) ((lvl.getGameTime() % 160) / 160.0); // sem alvo: gira devagar
            double toTarget = Math.atan2(target.getZ() + 0.5 - holder.getZ(), target.getX() + 0.5 - holder.getX()) / (Math.PI * 2);
            double facing = (holder.getVisualRotationYInDegrees() / 360.0);
            return (float) net.minecraft.util.Mth.positiveModulo(0.5 - (facing - 0.25) + toTarget - 0.5 + 0.5, 1.0);
          });
      net.minecraft.client.renderer.item.ItemProperties.register(com.arcane.registry.ModItems.SCEPTER.get(), new net.minecraft.resources.ResourceLocation("arcane", "head"),
          (stack, level, entity, seed) -> com.arcane.item.ScepterItem.head(stack));
      net.minecraft.client.renderer.item.ItemProperties.register(com.arcane.registry.ModItems.SCEPTER.get(), new net.minecraft.resources.ResourceLocation("arcane", "shaft"),
          (stack, level, entity, seed) -> com.arcane.item.ScepterItem.shaft(stack));
    });
    // Botao "Config" da aba Mods: abre as opcoes do Arcane (preferencias e, num mundo local, as regras do mundo).
    net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
        () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new com.arcane.client.screen.ArcaneOptionsScreen(parent)));
  }

  @SubscribeEvent
  public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(ModEntities.GRIMOIRE.get(), GrimoireRenderer::new);
    event.registerEntityRenderer(ModEntities.ARCANE_PROJECTILE.get(), ArcaneProjectileRenderer::new);
    event.registerEntityRenderer(ModEntities.NPC_MAGE.get(), NPCMageRenderer::new);
    event.registerEntityRenderer(ModEntities.HYDRA.get(), com.arcane.client.render.HydraRenderer::new);
    event.registerEntityRenderer(ModEntities.ILLUSION.get(), com.arcane.client.render.IllusionRenderer::new);
    event.registerEntityRenderer(ModEntities.SPECTRAL_WOLF.get(), com.arcane.client.render.SummonRenderers.Wolf::new);
    event.registerEntityRenderer(ModEntities.TRAINING_DUMMY.get(), com.arcane.client.render.ArcaneDummyRenderer::new);
    event.registerEntityRenderer(ModEntities.RUIN_GUARDIAN.get(), com.arcane.client.render.RuinGuardianRenderer::new);
    event.registerEntityRenderer(ModEntities.ARCANE_GOLEM.get(), com.arcane.client.render.SummonRenderers.Golem::new);
  }

  /** Petrificar: toda criatura e a skin do jogador ganham a camada que desenha o modelo em pedra quando petrificadas. */
  @SubscribeEvent
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void addPetrifyLayers(EntityRenderersEvent.AddLayers event) {
    for (var type : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValues()) {
      try {
        // getRenderer devolve R extends LivingEntityRenderer: tipos sem renderer vivo (Noop) estouram o cast, entao pulamos.
        net.minecraft.client.renderer.entity.LivingEntityRenderer living = event.getRenderer((net.minecraft.world.entity.EntityType) type);
        if (living != null) living.addLayer(new com.arcane.client.render.PetrifyLayer(living));
      } catch (ClassCastException ignored) {
        // nao e uma criatura viva
      }
    }
    for (String skin : event.getSkins()) {
      var renderer = event.getSkin(skin);
      if (renderer != null) renderer.addLayer(new com.arcane.client.render.PetrifyLayer(renderer));
    }
  }

  @SubscribeEvent
  public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
    event.registerLayerDefinition(com.arcane.client.model.SummonModels.WOLF_LAYER, com.arcane.client.model.SummonLayers::wolf);
    event.registerLayerDefinition(com.arcane.client.render.HydraRenderer.LAYER, com.arcane.client.render.HydraRenderer.HydraModel::createLayer);
    event.registerLayerDefinition(com.arcane.client.model.SummonModels.GOLEM_LAYER, com.arcane.client.model.SummonLayers::golem);
  }

  @SubscribeEvent
  public static void registerParticles(RegisterParticleProvidersEvent event) {
    event.registerSpriteSet(ModParticles.ARCANE_SPARK.get(), s -> new ArcaneSparkParticle.Provider(s, ArcaneSparkParticle.Profile.SPARK));
    event.registerSpriteSet(ModParticles.ARCANE_EMBER.get(), s -> new ArcaneSparkParticle.Provider(s, ArcaneSparkParticle.Profile.EMBER));
    event.registerSpriteSet(ModParticles.ARCANE_FROST.get(), s -> new ArcaneSparkParticle.Provider(s, ArcaneSparkParticle.Profile.FROST));
    event.registerSpriteSet(ModParticles.ARCANE_BOLT.get(), s -> new ArcaneSparkParticle.Provider(s, ArcaneSparkParticle.Profile.BOLT));
    event.registerSpriteSet(ModParticles.ARCANE_VENOM.get(), s -> new ArcaneSparkParticle.Provider(s, ArcaneSparkParticle.Profile.VENOM));
    event.registerSpriteSet(ModParticles.ARCANE_FORCE.get(), s -> new ArcaneSparkParticle.Provider(s, ArcaneSparkParticle.Profile.FORCE));
    event.registerSpriteSet(ModParticles.ARCANE_VOID.get(), s -> new ArcaneSparkParticle.Provider(s, ArcaneSparkParticle.Profile.VOID));
    event.registerSpriteSet(ModParticles.ARCANE_BOLT_STRIKE.get(), sp -> new ArcaneSparkParticle.Provider(sp, ArcaneSparkParticle.Profile.STRIKE));
    event.registerSpriteSet(ModParticles.ARCANE_SHOCK.get(), sp -> new ArcaneSparkParticle.Provider(sp, ArcaneSparkParticle.Profile.SHOCK));
    event.registerSpriteSet(ModParticles.ARCANE_LIFE.get(), s -> new ArcaneSparkParticle.Provider(s, ArcaneSparkParticle.Profile.LIFE));
    ModParticles.FX.forEach((family, type) ->
        event.registerSpriteSet(type.get(), s -> new com.arcane.client.particle.ElementFxParticle.Provider(s, family)));
  }

  @SubscribeEvent
  public static void registerOverlays(RegisterGuiOverlaysEvent event) {
    event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "mana", new ManaHudOverlay());
    event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "spell_bar", new com.arcane.client.hud.SpellBarOverlay());
    event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "cast_flash", new com.arcane.client.hud.CastFlashOverlay());
    event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "vapor", new com.arcane.client.hud.VaporOverlay());
  }

  @SubscribeEvent
  public static void registerKeys(RegisterKeyMappingsEvent event) {
    CAST_KEY = new KeyMapping("key.arcane.cast", GLFW.GLFW_KEY_V, "key.categories.arcane");
    CYCLE_KEY = new KeyMapping("key.arcane.cycle", GLFW.GLFW_KEY_B, "key.categories.arcane");
    GRIMOIRE_KEY = new KeyMapping("key.arcane.grimoire", GLFW.GLFW_KEY_G, "key.categories.arcane");
    RADIAL_KEY = new KeyMapping("key.arcane.radial", GLFW.GLFW_KEY_R, "key.categories.arcane");
    event.register(CAST_KEY);
    event.register(CYCLE_KEY);
    event.register(GRIMOIRE_KEY);
    event.register(RADIAL_KEY);
  }

  /** Conjura a inscricao ativa. O id vem do servidor (ClientArcaneData); o servidor revalida tudo. */
  public static void tryCast(boolean fromWorkbench) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.getConnection() == null) return;
    ClientArcaneData.active().ifPresentOrElse(
        id -> ModNetwork.CHANNEL.sendToServer(new C2SCastRequestPacket(id, fromWorkbench)),
        () -> mc.player.displayClientMessage(Component.translatable("hud.arcane.no_spell").withStyle(ChatFormatting.GRAY), true));
  }

  /** Passa para a proxima inscricao salva no grimorio. */
  public static void cycleSpell(int direction) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.getConnection() == null) return;
    List<ClientArcaneData.SpellEntry> spells = ClientArcaneData.spells();
    if (ClientPrefs.mode() == ClientPrefs.UseMode.LOADOUT && !ClientArcaneData.activeLoadoutSpells().isEmpty()) spells = ClientArcaneData.activeLoadoutSpells();
    if (spells.isEmpty()) return;
    UUID current = ClientArcaneData.active().orElse(null);
    int index = 0;
    for (int i = 0; i < spells.size(); i++) {
      if (spells.get(i).id().equals(current)) index = i;
    }
    int next = Math.floorMod(index + direction, spells.size());
    ClientArcaneData.SpellEntry entry = spells.get(next);
    ModNetwork.CHANNEL.sendToServer(new C2SSelectInscriptionPacket(entry.id()));
    Component name = entry.name().isEmpty() ? Component.translatable("gui.arcane.unnamed") : Component.literal(entry.name());
    mc.player.displayClientMessage(Component.translatable("hud.arcane.selected", name).withStyle(ChatFormatting.LIGHT_PURPLE), true);
  }
}
