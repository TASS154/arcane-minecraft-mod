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
  private static long debuffTicks() { return 24000L * com.arcane.config.ArcaneConfig.GRIMOIRE_LOSS_DAYS.get(); }

  @SubscribeEvent
  public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      spawnGrimoire(player);
      // Criativo (testes): glifos novos, adicionados depois que o mundo foi criado, ja entram no grimorio.
      if (player.getAbilities().instabuild) {
        GrimoireAttachments.get(player).ifPresent(g -> com.arcane.glyph.GlyphRegistry.all().keySet().stream()
            .filter(id -> !g.knowledge().containsKey(id)).toList()
            .forEach(id -> com.arcane.discovery.DiscoveryService.learnMastered(player, id)));
      }
      GrimoireAttachments.updateManaMultiplier(player);
      com.arcane.network.ArcaneSync.sync(player, true);
      giveTutorialBook(player);
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
      grimoire.setGrimoireDestroyedUntilTick(player.level().getGameTime() + debuffTicks());
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
      grimoire.setGrimoireDestroyedUntilTick(currentTick + debuffTicks());
    });
    GrimoireAttachments.updateManaMultiplier(player);
  }

  /** Primeira entrada: um livro curto explica o basico (so uma vez por jogador). */
  private static void giveTutorialBook(ServerPlayer player) {
    var data = player.getPersistentData();
    var persisted = data.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
    if (persisted.getBoolean("arcane_tutorial_given")) return;
    persisted.putBoolean("arcane_tutorial_given", true);
    data.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);
    net.minecraft.world.item.ItemStack book = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);
    CompoundTag tag = book.getOrCreateTag();
    tag.putString("title", net.minecraft.network.chat.Component.translatable("tutorial.arcane.title").getString());
    tag.putString("author", "Arcane");
    net.minecraft.nbt.ListTag pages = new net.minecraft.nbt.ListTag();
    var texts = java.util.List.of(
        net.minecraft.network.chat.Component.translatable("tutorial.arcane.page1"),
        net.minecraft.network.chat.Component.translatable("tutorial.arcane.page2"),
        net.minecraft.network.chat.Component.translatable("tutorial.arcane.page3"),
        net.minecraft.network.chat.Component.translatable("tutorial.arcane.page4"),
        net.minecraft.network.chat.Component.translatable("tutorial.arcane.page5"),
        net.minecraft.network.chat.Component.translatable("tutorial.arcane.page6"));
    for (var text : texts) {
      pages.add(net.minecraft.nbt.StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(text)));
    }
    tag.put("pages", pages);
    if (!player.getInventory().add(book)) player.drop(book, false);
  }

  private static final String HIDDEN = "arcane_grimoire_hidden";

  private static boolean isHidden(ServerPlayer player) {
    return player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).getBoolean(HIDDEN);
  }

  /** "Equipar": mostra ou guarda o grimorio flutuante (o conteudo e as magias continuam disponiveis). */
  public static void toggleFloating(ServerPlayer player) {
    var data = player.getPersistentData();
    var persisted = data.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
    boolean hide = !persisted.getBoolean(HIDDEN);
    persisted.putBoolean(HIDDEN, hide);
    data.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);
    if (hide) {
      player.serverLevel().getEntitiesOfClass(GrimoireEntity.class, player.getBoundingBox().inflate(32))
          .stream().filter(g -> g.getPersistentData().hasUUID("owner") && player.getUUID().equals(g.getPersistentData().getUUID("owner")))
          .forEach(GrimoireEntity::discard);
    } else {
      spawnGrimoire(player);
    }
    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(hide ? "message.arcane.grimoire.hidden" : "message.arcane.grimoire.shown"), true);
  }

  /** Atualiza o estagio visual do grimorio flutuante ja existente (ex.: depois de usar uma Pagina Arcana). */
  public static void refreshGrimoire(ServerPlayer player) {
    int stage = GrimoireAttachments.get(player).map(GrimoireState::visualStage).orElse(1);
    player.serverLevel().getEntitiesOfClass(GrimoireEntity.class, player.getBoundingBox().inflate(32))
        .stream().filter(g -> g.getPersistentData().hasUUID("owner") && player.getUUID().equals(g.getPersistentData().getUUID("owner")))
        .forEach(g -> g.bind(player, stage));
  }

  private static void spawnGrimoire(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    level.getEntitiesOfClass(GrimoireEntity.class, player.getBoundingBox().inflate(32))
        .stream().filter(g -> g.getPersistentData().hasUUID("owner") && player.getUUID().equals(g.getPersistentData().getUUID("owner")))
        .forEach(GrimoireEntity::discard);

    int stage = GrimoireAttachments.get(player).map(GrimoireState::visualStage).orElse(1);
    if (isHidden(player)) return;
  if (!GrimoireAttachments.get(player).map(GrimoireState::grimoirePresent).orElse(false)) return;

    GrimoireEntity grimoire = new GrimoireEntity(ModEntities.GRIMOIRE.get(), level);
    grimoire.bind(player, stage);
    grimoire.getPersistentData().putUUID("owner", player.getUUID());
    level.addFreshEntity(grimoire);
    level.playSound(null, player.blockPosition(), com.arcane.registry.ModSounds.GRIMOIRE_SUMMON.get(),
        net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.0f);
  }
}
