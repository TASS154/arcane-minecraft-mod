package com.arcane.execution;

import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.inscription.Inscription;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class ExecutionContext {
    private final ServerLevel level;
    private final LivingEntity caster;
    private final Inscription inscription;
    private final CompiledExecutionPlan plan;
    private final EnvironmentalSnapshot environment;
    private final PrecisionSnapshot precision;
    private final Vec3 origin;
    private final Vec3 look;
    private final TriggerEvent trigger;
    private TargetList resolvedTargets = TargetList.empty();
    private Optional<UUID> ritualSiteId = Optional.empty();
    private double geometryBonus = 1.0;
    private double scaleFactor = 1.0;

    public ExecutionContext(ServerLevel level, LivingEntity caster, Inscription inscription,
                            CompiledExecutionPlan plan, EnvironmentalSnapshot environment,
                            PrecisionSnapshot precision, TriggerEvent trigger) {
        this.level = level;
        this.caster = caster;
        this.inscription = inscription;
        this.plan = plan;
        this.environment = environment;
        this.precision = precision;
        this.trigger = trigger;
        this.origin = caster.getEyePosition();
        this.look = caster.getLookAngle();
    }

    public ServerLevel level() { return level; }
    public LivingEntity caster() { return caster; }
    public Inscription inscription() { return inscription; }
    public CompiledExecutionPlan plan() { return plan; }
    public EnvironmentalSnapshot environment() { return environment; }
    public PrecisionSnapshot precision() { return precision; }
    public Vec3 origin() { return origin; }
    public Vec3 look() { return look; }
    public TriggerEvent trigger() { return trigger; }
    public TargetList resolvedTargets() { return resolvedTargets; }
    public void setResolvedTargets(TargetList targets) { this.resolvedTargets = targets; }
    public Optional<UUID> ritualSiteId() { return ritualSiteId; }
    public void setRitualSiteId(UUID id) { this.ritualSiteId = Optional.ofNullable(id); }
    public double geometryBonus() { return geometryBonus; }
    public void setGeometryBonus(double geometryBonus) { this.geometryBonus = geometryBonus; }
    public double scaleFactor() { return scaleFactor; }
    public void setScaleFactor(double scaleFactor) { this.scaleFactor = scaleFactor; }

    public double amplify(double value, java.util.List<String> magnitudeKeys, String key) {
        if (!magnitudeKeys.isEmpty() && !magnitudeKeys.contains(key)) {
            return value;
        }
        return value * environment.potencyMultiplier() * precision.powerMultiplier()
                * geometryBonus * scaleFactor;
    }

    public record TriggerEvent(String type, Optional<net.minecraft.world.entity.Entity> entity,
                               Optional<net.minecraft.core.BlockPos> blockPos) {
        public static TriggerEvent cast() {
            return new TriggerEvent("cast", Optional.empty(), Optional.empty());
        }
    }
}
