package com.arcane.inscribing;

import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionMetadata;
import com.arcane.inscription.InscriptionSourceKind;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class WorldInscriptionIndex extends SavedData {
  private static final String DATA_NAME = "arcane_inscriptions";
  private final Map<BlockPos, InscriptionAnchor> anchors = new HashMap<>();

  public static WorldInscriptionIndex get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(WorldInscriptionIndex::load, WorldInscriptionIndex::new, DATA_NAME);
  }

  public WorldInscriptionIndex() {}

  public static WorldInscriptionIndex load(CompoundTag tag) {
    WorldInscriptionIndex index = new WorldInscriptionIndex();
    if (tag.contains("anchors", Tag.TAG_LIST)) {
      ListTag list = tag.getList("anchors", Tag.TAG_COMPOUND);
      for (Tag element : list) {
        CompoundTag anchorTag = (CompoundTag) element;
        BlockPos pos = BlockPos.of(anchorTag.getLong("pos"));
        Inscription.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, anchorTag.get("inscription"))
            .result().ifPresent(inscription -> index.anchors.put(pos,
                new InscriptionAnchor(pos, inscription, anchorTag.getString("trigger"),
                    new ResourceLocation(anchorTag.getString("ink")))));
      }
    }
    return index;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag list = new ListTag();
    anchors.forEach((pos, anchor) -> {
      CompoundTag anchorTag = new CompoundTag();
      anchorTag.putLong("pos", pos.asLong());
      anchorTag.putString("trigger", anchor.triggerType());
      anchorTag.putString("ink", anchor.ink().toString());
      Inscription.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, anchor.inscription())
          .result().ifPresent(encoded -> anchorTag.put("inscription", encoded));
      list.add(anchorTag);
    });
    tag.put("anchors", list);
    return tag;
  }

  public Optional<InscriptionAnchor> get(BlockPos pos) {
    return Optional.ofNullable(anchors.get(pos));
  }

  public void put(InscriptionAnchor anchor) {
    anchors.put(anchor.pos(), anchor);
    setDirty();
  }
}
