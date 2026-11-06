package com.arcane.entity;

import com.arcane.execution.EnvironmentalSnapshot;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.PrecisionSnapshot;
import com.arcane.execution.SpellPipeline;
import com.arcane.execution.TargetList;
import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.inscription.Inscription;
import com.arcane.execution.SpellElement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
  private static final EntityDataAccessor<Integer> ELEMENT =
      SynchedEntityData.defineId(ArcaneProjectileEntity.class, EntityDataSerializers.INT);
  private CompiledExecutionPlan plan;
  private PrecisionSnapshot precision = PrecisionSnapshot.WORKBENCH;
  private EnvironmentalSnapshot environment = EnvironmentalSnapshot.NEUTRAL;
  private double geometryBonus = 1.0;
  private double areaScale = 1.0;
  private int ritualLevel = 0;

  public ArcaneProjectileEntity(EntityType<? extends ArcaneProjectileEntity> type, Level level) {
    super(type, level);
  }

  public void setInscription(Inscription inscription) { this.inscription = inscription; }
  public void setPlan(CompiledExecutionPlan plan) {
    this.plan = plan;
    if (plan != null) this.entityData.set(ELEMENT, SpellElement.of(plan).ordinal());
  }

  /** Elemento visual (cor e particulas); sincronizado para o cliente. */
  public SpellElement element() { return SpellElement.byOrdinal(this.entityData.get(ELEMENT)); }
  public void setPrecision(PrecisionSnapshot precision) { this.precision = precision; }
  public void setEnvironment(EnvironmentalSnapshot environment) { this.environment = environment; }

  /** Poder, area e nivel do ritual em que foi lancado: valem quando o projetil acertar. */
  public void setRitualBonus(double geometryBonus, double areaScale, int ritualLevel) {
    this.geometryBonus = geometryBonus;
    this.areaScale = areaScale;
    this.ritualLevel = ritualLevel;
  }

  @Override
  protected void defineSynchedData() {
    this.entityData.define(ELEMENT, 0);
  }

  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide) {
      for (int i = 0; i < 2; i++) {
        level().addParticle(element().particle(),
            getX() + (random.nextDouble() - 0.5) * 0.2, getY() + (random.nextDouble() - 0.5) * 0.2,
            getZ() + (random.nextDouble() - 0.5) * 0.2, 0.0, 0.0, 0.0);
      }
    } else if (tickCount > 200) {
      discard();
    }
  }

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
              : result instanceof net.minecraft.world.phys.BlockHitResult blockHit2
                  ? new ExecutionContext.TriggerEvent("hit_block", java.util.Optional.empty(), java.util.Optional.of(blockHit2.getBlockPos()))
                  : ExecutionContext.TriggerEvent.cast()
      );
      context.setGeometryBonus(geometryBonus);
      context.setAreaScale(areaScale);
      context.setRitualLevel(ritualLevel);
      if (result instanceof EntityHitResult entityHit && entityHit.getEntity() != null) {
        context.setResolvedTargets(TargetList.ofEntities(entityHit.getEntity()));
      } else if (result instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
        net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
        context.setResolvedTargets(new TargetList(java.util.List.of(),
            java.util.List.of(new TargetList.BlockTarget(pos, level().getBlockState(pos)))));
      }
      var server = (net.minecraft.server.level.ServerLevel) level();
      server.playSound(null, getX(), getY(), getZ(), com.arcane.registry.ModSounds.SPELL_HIT.get(),
          net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 1.0f);
      server.sendParticles(element().particle(), getX(), getY(), getZ(),
          26, 0.3, 0.3, 0.3, 0.1);
      if (element() == SpellElement.STORM) {
        server.sendParticles(com.arcane.registry.ModParticles.ARCANE_BOLT_STRIKE.get(), getX(), getY() + 1.6, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
      } else if (element() == SpellElement.FORCE) {
        server.sendParticles(com.arcane.registry.ModParticles.ARCANE_SHOCK.get(), getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.0);
      }
      SpellPipeline.INSTANCE.runSegments(context);
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
