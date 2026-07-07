package com.arcane.entity;

import com.arcane.execution.CastingService;
import com.arcane.execution.EnvironmentalSnapshot;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.PrecisionSnapshot;
import com.arcane.execution.SpellPipeline;
import com.arcane.execution.TargetList;
import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.inscription.Inscription;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

public class ArcaneProjectileEntity extends ThrowableProjectile {
  private Inscription inscription = Inscription.testProjectileDamageFire();
  private CompiledExecutionPlan plan;
  private PrecisionSnapshot precision = PrecisionSnapshot.WORKBENCH;
  private EnvironmentalSnapshot environment = EnvironmentalSnapshot.NEUTRAL;

  public ArcaneProjectileEntity(EntityType<? extends ArcaneProjectileEntity> type, Level level) {
    super(type, level);
  }

  public void setInscription(Inscription inscription) { this.inscription = inscription; }
  public void setPlan(CompiledExecutionPlan plan) { this.plan = plan; }
  public void setPrecision(PrecisionSnapshot precision) { this.precision = precision; }
  public void setEnvironment(EnvironmentalSnapshot environment) { this.environment = environment; }

  @Override
  protected void defineSynchedData() {}

  @Override
  protected void onHit(HitResult result) {
    super.onHit(result);
    if (!level().isClientSide && getOwner() instanceof LivingEntity caster && plan != null) {
      ExecutionContext context = new ExecutionContext(
          (net.minecraft.server.level.ServerLevel) level(),
          caster,
          inscription,
          plan,
          environment,
          precision,
          result instanceof EntityHitResult entityHit
              ? new ExecutionContext.TriggerEvent("hit_entity", java.util.Optional.ofNullable(entityHit.getEntity()), java.util.Optional.empty())
              : ExecutionContext.TriggerEvent.cast()
      );
      if (result instanceof EntityHitResult entityHit && entityHit.getEntity() != null) {
        context.setResolvedTargets(TargetList.ofEntities(entityHit.getEntity()));
      }
      SpellPipeline pipeline = new SpellPipeline();
      for (CompiledExecutionPlan.ExecutionSegment segment : plan.segments()) {
        pipeline.executeSegment(context, segment);
      }
      discard();
    }
  }

  @Override
  public Packet<ClientGamePacketListener> getAddEntityPacket() {
    return NetworkHooks.getEntitySpawningPacket(this);
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {}

  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {}
}
