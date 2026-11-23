package com.arcane.inscribing;

import com.arcane.inscription.Inscription;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class WorldInscriptionIndex extends SavedData {
  private static final String DATA_NAME = "arcane_inscriptions";
  /** Um desenho por face do bloco. */
  private final Map<BlockPos, java.util.EnumMap<net.minecraft.core.Direction, InscriptionAnchor>> anchors = new HashMap<>();
  private final Map<Long, Set<BlockPos>> byChunk = new HashMap<>();

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
        Inscription.CODEC.parse(NbtOps.INSTANCE, anchorTag.get("inscription"))
            .result().ifPresent(inscription -> {
              net.minecraft.core.Direction face = anchorTag.contains("face")
                  ? net.minecraft.core.Direction.from3DDataValue(anchorTag.getInt("face")) : net.minecraft.core.Direction.UP;
              index.anchors.computeIfAbsent(pos, k -> new java.util.EnumMap<>(net.minecraft.core.Direction.class)).put(face,
                  new InscriptionAnchor(pos, inscription, anchorTag.getString("trigger"), new ResourceLocation(anchorTag.getString("ink")), face));
              index.indexChunk(pos);
            });
      }
    }
    return index;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag list = new ListTag();
    anchors.forEach((pos, faces) -> faces.forEach((face, anchor) -> {
      CompoundTag anchorTag = new CompoundTag();
      anchorTag.putLong("pos", pos.asLong());
      anchorTag.putString("trigger", anchor.triggerType());
      anchorTag.putString("ink", anchor.ink().toString());
      anchorTag.putInt("face", face.get3DDataValue());
      Inscription.CODEC.encodeStart(NbtOps.INSTANCE, anchor.inscription())
          .result().ifPresent(encoded -> anchorTag.put("inscription", encoded));
      list.add(anchorTag);
    }));
    tag.put("anchors", list);
    return tag;
  }

  /** Qualquer desenho do bloco (o primeiro, em ordem de face). */
  public Optional<InscriptionAnchor> get(BlockPos pos) {
    return at(pos).stream().findFirst();
  }

  public Optional<InscriptionAnchor> get(BlockPos pos, net.minecraft.core.Direction face) {
    var faces = anchors.get(pos);
    return faces == null ? Optional.empty() : Optional.ofNullable(faces.get(face));
  }

  /** Todos os desenhos do bloco (ate um por face). */
  public java.util.List<InscriptionAnchor> at(BlockPos pos) {
    var faces = anchors.get(pos);
    return faces == null ? java.util.List.of() : new java.util.ArrayList<>(faces.values());
  }

  /** Poe o desenho; substitui o que ja houver na mesma face. */
  public void put(InscriptionAnchor anchor) {
    BlockPos pos = anchor.pos();
    anchors.computeIfAbsent(pos, k -> new java.util.EnumMap<>(net.minecraft.core.Direction.class)).put(anchor.face(), anchor);
    indexChunk(pos);
    setDirty();
  }

  public Optional<InscriptionAnchor> remove(BlockPos pos, net.minecraft.core.Direction face) {
    var faces = anchors.get(pos);
    InscriptionAnchor removed = faces == null ? null : faces.remove(face);
    if (removed != null) {
      if (faces.isEmpty()) forget(pos);
      setDirty();
    }
    return Optional.ofNullable(removed);
  }

  /** Remove todos os desenhos do bloco. Devolve o primeiro removido (se houve algum). */
  public Optional<InscriptionAnchor> remove(BlockPos pos) {
    var faces = anchors.remove(pos);
    if (faces == null || faces.isEmpty()) return Optional.empty();
    forget(pos);
    setDirty();
    return Optional.of(faces.values().iterator().next());
  }

  private void forget(BlockPos pos) {
    anchors.remove(pos);
    Set<BlockPos> chunk = byChunk.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
    if (chunk != null) chunk.remove(pos);
  }

  public java.util.Collection<InscriptionAnchor> all() {
    java.util.List<InscriptionAnchor> list = new java.util.ArrayList<>();
    for (var faces : anchors.values()) list.addAll(faces.values());
    return list;
  }

  /** Apaga todos os desenhos deste mundo (so para os testes automaticos). */
  public void clearAll() {
    anchors.clear();
    byChunk.clear();
    setDirty();
  }

  public boolean isEmpty() {
    return anchors.isEmpty();
  }

  public Set<BlockPos> getInChunk(int chunkX, int chunkZ) {
    Set<BlockPos> positions = byChunk.get(ChunkPos.asLong(chunkX, chunkZ));
    return positions == null ? Set.of() : Collections.unmodifiableSet(positions);
  }

  private void indexChunk(BlockPos pos) {
    byChunk.computeIfAbsent(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), key -> new HashSet<>()).add(pos);
  }
}
