package com.arcane.ritual;

import com.arcane.inscription.Inscription;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public class RitualSite {
  public enum State { DORMANT, ARMED, COOLDOWN }
  public enum BuildMode { AUTO, MANUAL }

  private final UUID id;
  private final Inscription inscription;
  private final BlockPos origin;
  private final AABB bounds;
  private double geometryScore;
  private double scaleFactor;
  private State state;
  private final BuildMode buildMode;

  public RitualSite(UUID id, Inscription inscription, BlockPos origin, AABB bounds,
                    double geometryScore, double scaleFactor, State state, BuildMode buildMode) {
    this.id = id;
    this.inscription = inscription;
    this.origin = origin;
    this.bounds = bounds;
    this.geometryScore = geometryScore;
    this.scaleFactor = scaleFactor;
    this.state = state;
    this.buildMode = buildMode;
  }

  public UUID id() { return id; }
  public Inscription inscription() { return inscription; }
  public BlockPos origin() { return origin; }
  public AABB bounds() { return bounds; }
  public double geometryScore() { return geometryScore; }
  public double scaleFactor() { return scaleFactor; }
  public State state() { return state; }
  public BuildMode buildMode() { return buildMode; }
  public void setState(State state) { this.state = state; }
  public void setGeometryScore(double score) { this.geometryScore = score; }

  public CompoundTag save(CompoundTag tag) {
    tag.putUUID("id", id);
    tag.putLong("origin", origin.asLong());
    tag.putDouble("geometry", geometryScore);
    tag.putDouble("scale", scaleFactor);
    tag.putString("state", state.name());
    tag.putString("mode", buildMode.name());
    Inscription.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, inscription)
        .result().ifPresent(encoded -> tag.put("inscription", encoded));
    return tag;
  }

  public static RitualSite load(CompoundTag tag) {
    UUID id = tag.getUUID("id");
    BlockPos origin = BlockPos.of(tag.getLong("origin"));
    Inscription inscription = Inscription.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("inscription"))
        .result().orElse(Inscription.testSelfDamage());
    int size = 16;
    AABB bounds = new AABB(origin).inflate(size);
    return new RitualSite(id, inscription, origin, bounds,
        tag.getDouble("geometry"), tag.getDouble("scale"),
        State.valueOf(tag.getString("state")), BuildMode.valueOf(tag.getString("mode")));
  }
}
