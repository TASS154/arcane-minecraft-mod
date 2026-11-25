package com.arcane.restore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * O diario do Restaurar de uma dimensao, salvo com o mundo ({@code data/arcane_restore_journal.dat}). Antes ficava so em
 * memoria e sumia ao fechar o servidor. Guarda no maximo o teto da config (as mais antigas saem primeiro).
 */
public final class JournalData extends SavedData {
  static final String NAME = "arcane_restore_journal";

  final JournalLog<BlockJournal.Entry> log = new JournalLog<>();

  public static JournalData of(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(tag -> load(level, tag), JournalData::new, NAME);
  }

  public static JournalData load(ServerLevel level, CompoundTag tag) {
    JournalData d = new JournalData();
    var blocks = level.holderLookup(Registries.BLOCK);
    ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
    for (int i = 0; i < list.size(); i++) {
      CompoundTag e = list.getCompound(i);
      BlockJournal.Entry entry = new BlockJournal.Entry(BlockPos.of(e.getLong("p")),
          NbtUtils.readBlockState(blocks, e.getCompound("b")), NbtUtils.readBlockState(blocks, e.getCompound("a")),
          e.hasUUID("w") ? e.getUUID("w") : null);
      d.log.add(entry, e.getLong("t"), Integer.MAX_VALUE, Long.MAX_VALUE);
    }
    return d;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag list = new ListTag();
    for (var s : log.oldestFirst()) {
      CompoundTag e = new CompoundTag();
      e.putLong("p", s.value().pos().asLong());
      e.put("b", NbtUtils.writeBlockState(s.value().before()));
      e.put("a", NbtUtils.writeBlockState(s.value().after()));
      e.putLong("t", s.tick());
      if (s.value().author() != null) e.putUUID("w", s.value().author());
      list.add(e);
    }
    tag.put("entries", list);
    return tag;
  }
}
