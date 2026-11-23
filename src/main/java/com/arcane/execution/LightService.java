package com.arcane.execution;

import com.arcane.ArcaneMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Luzes da magia Luz: um bloco de luz invisivel que se apaga sozinho quando o tempo acaba. As luzes ficam salvas no
 * mundo, entao sair e voltar nao deixa luz eterna para tras. Verifica so uma vez por segundo.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class LightService extends SavedData {
  private static final String NAME = "arcane_lights";
  private final Map<Long, Long> expiry = new HashMap<>();

  public static LightService get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(LightService::load, LightService::new, NAME);
  }

  private static LightService load(CompoundTag tag) {
    LightService s = new LightService();
    for (Tag t : tag.getList("lights", Tag.TAG_COMPOUND)) {
      CompoundTag c = (CompoundTag) t;
      s.expiry.put(c.getLong("p"), c.getLong("t"));
    }
    return s;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag list = new ListTag();
    expiry.forEach((p, t) -> {
      CompoundTag c = new CompoundTag();
      c.putLong("p", p);
      c.putLong("t", t);
      list.add(c);
    });
    tag.put("lights", list);
    return tag;
  }

  /** Acende uma luz (nivel 1-15) por {@code ticks} (0 = para sempre). Falso se o lugar nao esta livre. */
  public static boolean place(ServerLevel level, BlockPos pos, int brightness, int ticks) {
    var st = level.getBlockState(pos);
    if (!st.isAir() && !st.is(Blocks.LIGHT)) return false;
    level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Math.max(1, Math.min(15, brightness))), 3);
    LightService s = get(level);
    if (ticks > 0) s.expiry.put(pos.asLong(), level.getGameTime() + ticks);
    else s.expiry.remove(pos.asLong());
    s.setDirty();
    return true;
  }

  public static void clearAll(ServerLevel level) {
    get(level).expiry.clear();
    get(level).setDirty();
  }

  public static int pending(ServerLevel level) {
    return get(level).expiry.size();
  }

  @SubscribeEvent
  public static void onTick(TickEvent.LevelTickEvent event) {
    if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.getGameTime() % 20 != 0) return;
    LightService s = get(level);
    if (s.expiry.isEmpty()) return;
    long now = level.getGameTime();
    var it = s.expiry.entrySet().iterator();
    while (it.hasNext()) {
      var e = it.next();
      if (e.getValue() > now) continue;
      BlockPos pos = BlockPos.of(e.getKey());
      if (!level.isLoaded(pos)) continue;
      if (level.getBlockState(pos).is(Blocks.LIGHT)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      it.remove();
      s.setDirty();
    }
  }
}
