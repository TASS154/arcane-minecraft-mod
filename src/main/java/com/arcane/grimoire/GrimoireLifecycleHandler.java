package com.arcane.grimoire;

import com.arcane.execution.ManaState;
import com.arcane.entity.GrimoireEntity;
import com.arcane.registry.ModEntities;
import com.arcane.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class GrimoireLifecycleHandler {
  private static final long DESTROY_DEBUFF_TICKS = 72000L;

  @SubscribeEvent
  public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      spawnGrimoire(player);
      GrimoireAttachments.updateManaMultiplier(player);
      com.arcane.network.ArcaneSync.sync(player, true);
    }
  }

  @SubscribeEvent
  public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      com.arcane.network.ArcaneSync.forget(player);
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
          // Passados 3 dias de jogo sem recuperar o grimorio, o provisorio passa a ser o seu e a penalidade acaba.
          long until = grimoire.grimoireDestroyedUntilTick();
          if (until > 0 && serverPlayer.level().getGameTime() >= until) {
            grimoire.setGrimoirePresent(true);
            grimoire.setGrimoireDestroyedUntilTick(0L);
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.arcane.grimoire.adopted"), false);
            spawnGrimoire(serverPlayer);
          }
          GrimoireAttachments.updateManaMultiplier(serverPlayer);
        }
      });
      if (serverPlayer.tickCount % 5 == 0) {
        com.arcane.network.ArcaneSync.sync(serverPlayer, false);
      }
    }
  }

  @SubscribeEvent
  public void onDeath(LivingDeathEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    GrimoireAttachments.get(player).ifPresent(grimoire -> {
      // O grimorio SEMPRE cai, levando todo o conteudo. Fica no chao sem expirar e a prova de fogo e lava
      // (so o vazio o destroi); quem fica com o jogador e um grimorio provisorio, em branco.
      CompoundTag snapshot = new CompoundTag();
      grimoire.save(snapshot);
      ItemEntity entity = player.drop(com.arcane.item.GrimoireItem.createFor(player, snapshot), false);
      if (entity != null) {
        entity.setUnlimitedLifetime();
        entity.setInvulnerable(true);
        entity.setGlowingTag(true);
      }
      CompoundTag blank = new CompoundTag();
      GrimoireState.createDefault().save(blank);
      grimoire.load(blank);
      grimoire.setGrimoirePresent(false);
      grimoire.setGrimoireDestroyedUntilTick(player.level().getGameTime() + DESTROY_DEBUFF_TICKS);
      player.level().getEntitiesOfClass(GrimoireEntity.class, player.getBoundingBox().inflate(16))
          .forEach(GrimoireEntity::discard);
    });
    GrimoireAttachments.updateManaMultiplier(player);
  }

  /** O dono pega o proprio grimorio do chao: o conteudo volta para ele e o item e consumido. */
  @SubscribeEvent
  public void onItemPickup(EntityItemPickupEvent event) {
    ItemStack stack = event.getItem().getItem();
    if (!(event.getEntity() instanceof ServerPlayer player) || !com.arcane.item.GrimoireItem.isOwnedBy(stack, player.getUUID())) return;
    CompoundTag contents = com.arcane.item.GrimoireItem.contents(stack);
    if (contents == null) return;
    GrimoireAttachments.get(player).ifPresent(grimoire -> {
      GrimoireTransfer.restore(grimoire, contents);
      grimoire.setGrimoirePresent(true);
      grimoire.setGrimoireDestroyedUntilTick(0L);
    });
    event.getItem().discard();
    event.setCanceled(true);
    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.arcane.grimoire.restored"), false);
    GrimoireAttachments.updateManaMultiplier(player);
    spawnGrimoire(player);
    com.arcane.network.ArcaneSync.sync(player, true);
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
    level.playSound(null, player.blockPosition(), com.arcane.registry.ModSounds.GRIMOIRE_SUMMON.get(),
        net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.0f);
  }
}
