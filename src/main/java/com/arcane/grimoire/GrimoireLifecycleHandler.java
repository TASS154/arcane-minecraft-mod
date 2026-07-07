package com.arcane.grimoire;

import com.arcane.execution.ManaState;
import com.arcane.entity.GrimoireEntity;
import com.arcane.registry.ModEntities;
import com.arcane.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class GrimoireLifecycleHandler {
  private static final long DESTROY_DEBUFF_TICKS = 72000L;

  @SubscribeEvent
  public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      spawnGrimoire(player);
      GrimoireAttachments.updateManaMultiplier(player);
    }
  }

  @SubscribeEvent
  public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      GrimoireAttachments.updateManaMultiplier(player);
      if (GrimoireAttachments.get(player).map(GrimoireState::grimoirePresent).orElse(true)) {
        spawnGrimoire(player);
      }
    }
  }

  @SubscribeEvent
  public void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
    GrimoireAttachments.getMana(event.player).ifPresent(mana -> mana.tick());
    if (event.player instanceof ServerPlayer serverPlayer) {
      GrimoireAttachments.get(serverPlayer).ifPresent(grimoire -> {
        if (!grimoire.grimoirePresent()) {
          GrimoireAttachments.updateManaMultiplier(serverPlayer);
        }
      });
    }
  }

  @SubscribeEvent
  public void onDeath(LivingDeathEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    GrimoireAttachments.get(player).ifPresent(grimoire -> {
      grimoire.setGrimoirePresent(false);
      ItemStack drop = new ItemStack(ModItems.GLYPH_NOTE.get());
      drop.setHoverName(net.minecraft.network.chat.Component.literal("Grimório de " + player.getName().getString()));
      ItemEntity entity = player.drop(drop, false);
      if (entity != null) {
        entity.setExtendedLifetime();
        entity.setNoPickUpDelay();
      }
      player.level().getEntitiesOfClass(GrimoireEntity.class, player.getBoundingBox().inflate(16))
          .forEach(GrimoireEntity::discard);
    });
    GrimoireAttachments.updateManaMultiplier(player);
  }

  @SubscribeEvent
  public void onItemPickup(PlayerEvent.ItemPickupEvent event) {
    if (event.getStack().getHoverName().getString().startsWith("Grimório de ")) {
      GrimoireAttachments.get(event.getEntity()).ifPresent(grimoire -> {
        grimoire.setGrimoirePresent(true);
        grimoire.setGrimoireDestroyedUntilTick(0L);
      });
      if (event.getEntity() instanceof ServerPlayer player) {
        GrimoireAttachments.updateManaMultiplier(player);
        spawnGrimoire(player);
      }
    }
  }

  public static void markGrimoireDestroyed(ServerPlayer player, long currentTick) {
    GrimoireAttachments.get(player).ifPresent(grimoire -> {
      grimoire.setGrimoirePresent(false);
      grimoire.setGrimoireDestroyedUntilTick(currentTick + DESTROY_DEBUFF_TICKS);
    });
    GrimoireAttachments.updateManaMultiplier(player);
  }

  private static void spawnGrimoire(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    level.getEntitiesOfClass(GrimoireEntity.class, player.getBoundingBox().inflate(32))
        .stream().filter(g -> player.getUUID().equals(g.getPersistentData().getUUID("owner")))
        .forEach(GrimoireEntity::discard);

    int stage = GrimoireAttachments.get(player).map(GrimoireState::visualStage).orElse(1);
  if (!GrimoireAttachments.get(player).map(GrimoireState::grimoirePresent).orElse(false)) return;

    GrimoireEntity grimoire = new GrimoireEntity(ModEntities.GRIMOIRE.get(), level);
    grimoire.bind(player, stage);
    grimoire.getPersistentData().putUUID("owner", player.getUUID());
    level.addFreshEntity(grimoire);
  }
}
