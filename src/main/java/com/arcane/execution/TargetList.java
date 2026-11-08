package com.arcane.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TargetList {
    private final List<Entity> entities;
    private final List<BlockTarget> blocks;
    /** Pontos vazios no espaco (o raio que nao acertou nada, o centro de um pilar): efeitos podem agir ali. */
    private final List<Vec3> points;

    public TargetList(List<Entity> entities, List<BlockTarget> blocks) {
        this(entities, blocks, List.of());
    }

    public TargetList(List<Entity> entities, List<BlockTarget> blocks, List<Vec3> points) {
        this.entities = List.copyOf(entities);
        this.blocks = List.copyOf(blocks);
        this.points = List.copyOf(points);
    }

    public static TargetList ofPoint(Vec3 point) {
        return new TargetList(List.of(), List.of(), List.of(point));
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
        List<Vec3> points = new ArrayList<>(a.points);
        points.addAll(b.points);
        return new TargetList(entities, blocks, points);
    }

    public List<Entity> entities() { return entities; }
    public List<BlockTarget> blocks() { return blocks; }
    public List<Vec3> points() { return points; }
    public boolean isEmpty() { return entities.isEmpty() && blocks.isEmpty() && points.isEmpty(); }

    public record BlockTarget(BlockPos pos, BlockState state) {}
}
