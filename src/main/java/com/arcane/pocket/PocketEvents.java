package com.arcane.pocket;

import com.arcane.ArcaneMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class PocketEvents {
  private PocketEvents() {}

  /** O Bolso permanece na morte: copia a etiqueta para o novo corpo e descarta o contêiner em cache do antigo. */
  @SubscribeEvent
  public static void clone(PlayerEvent.Clone event) {
    copyBetween(event.getOriginal(), event.getEntity());
  }

  /** Copia o conteudo do Bolso de um corpo para outro e descarta o contêiner em cache. */
  public static void copyBetween(Player from, Player to) {
    CompoundTag old = from.getPersistentData();
    if (old.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
      CompoundTag persisted = old.getCompound(Player.PERSISTED_NBT_TAG);
      if (persisted.contains(ArcanePocket.KEY, Tag.TAG_LIST)) {
        CompoundTag data = to.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        data.getCompound(Player.PERSISTED_NBT_TAG).put(ArcanePocket.KEY, persisted.get(ArcanePocket.KEY).copy());
      }
    }
    ArcanePocket.forget(to.getUUID());
  }

  @SubscribeEvent
  public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { ArcanePocket.forget(event.getEntity().getUUID()); }

  @SubscribeEvent
  public static void stopped(ServerStoppedEvent event) { ArcanePocket.clearCache(); }
}
