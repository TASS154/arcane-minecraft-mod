package com.arcane.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TargetList {
    private final List<Entity> entities;
    private final List<BlockTarget> blocks;

    public TargetList(List<Entity> entities, List<BlockTarget> blocks) {
        this.entities = List.copyOf(entities);
        this.blocks = List.copyOf(blocks);
    }

    public static TargetList empty() {
        return new TargetList(List.of(), List.of());
    }

    public static TargetList ofEntities(Entity... entities) {
        return new TargetList(List.of(entities), List.of());
    }

    public static TargetList merge(TargetList a, TargetList b) {
        List<Entity> entities = new ArrayList<>(a.entities);
        entities.addAll(b.entities);
        List<BlockTarget> blocks = new ArrayList<>(a.blocks);
        blocks.addAll(b.blocks);
        return new TargetList(entities, blocks);
    }

    public List<Entity> entities() { return entities; }
    public List<BlockTarget> blocks() { return blocks; }
    public boolean isEmpty() { return entities.isEmpty() && blocks.isEmpty(); }

    public record BlockTarget(BlockPos pos, BlockState state) {}
}
