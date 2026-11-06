package com.arcane.execution;

import com.arcane.ArcaneMod;
import com.arcane.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Criaturas invocadas por ritual duram um tempo: o fim fica gravado na propria entidade (sobrevive a reinicio do
 * mundo) e um verificador a cada segundo as dispensa, com faiscas.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class Summons {
  private static final String KEY = "arcane_summon_until";
  private static final Map<ServerLevel, Set<UUID>> TRACKED = new WeakHashMap<>();

  private Summons() {}

  public static void mark(Entity entity, long untilGameTime) {
    entity.getPersistentData().putLong(KEY, untilGameTime);
    if (entity.level() instanceof ServerLevel level) {
      synchronized (TRACKED) {
        TRACKED.computeIfAbsent(level, l -> new HashSet<>()).add(entity.getUUID());
      }
    }
  }

  public static boolean isSummon(Entity entity) {
    return entity.getPersistentData().contains(KEY);
  }

  /** Invocacao carregada de um save: volta a ser acompanhada (ou some, se o tempo ja passou). */
  @SubscribeEvent
  public static void onJoin(EntityJoinLevelEvent event) {
    if (event.getLevel() instanceof ServerLevel level && isSummon(event.getEntity())) {
      if (event.getEntity().getPersistentData().getLong(KEY) <= level.getGameTime()) {
        event.setCanceled(true);
      } else {
        synchronized (TRACKED) {
          TRACKED.computeIfAbsent(level, l -> new HashSet<>()).add(event.getEntity().getUUID());
        }
      }
    }
  }

  @SubscribeEvent
  public static void onLevelTick(TickEvent.LevelTickEvent event) {
    if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.getGameTime() % 20 != 0) return;
    synchronized (TRACKED) {
      Set<UUID> ids = TRACKED.get(level);
      if (ids == null) return;
      Iterator<UUID> it = ids.iterator();
      while (it.hasNext()) {
        Entity e = level.getEntity(it.next());
        if (e == null || !e.isAlive()) {
          it.remove();
        } else if (e.getPersistentData().getLong(KEY) <= level.getGameTime()) {
          level.sendParticles(ModParticles.ARCANE_SPARK.get(), e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 14, 0.3, 0.4, 0.3, 0.05);
          e.discard();
          it.remove();
        }
      }
    }
  }
}
