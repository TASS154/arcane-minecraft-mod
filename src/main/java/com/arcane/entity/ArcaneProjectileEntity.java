package com.arcane.entity;

import com.arcane.execution.EnvironmentalSnapshot;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.PrecisionSnapshot;
import com.arcane.execution.ProjectileMods;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class ArcaneProjectileEntity extends ThrowableProjectile {
  private Inscription inscription = Inscription.testProjectileDamageFire();
  private static final EntityDataAccessor<Integer> ELEMENT =
      SynchedEntityData.defineId(ArcaneProjectileEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> TRAIL =
      SynchedEntityData.defineId(ArcaneProjectileEntity.class, EntityDataSerializers.INT);
  /** Bits de {@link ProjectileMods} (o cliente desenha o rastro de cada modificador). */
  private static final EntityDataAccessor<Integer> MODS =
      SynchedEntityData.defineId(ArcaneProjectileEntity.class, EntityDataSerializers.INT);
  private CompiledExecutionPlan plan;
  private PrecisionSnapshot precision = PrecisionSnapshot.WORKBENCH;
  private EnvironmentalSnapshot environment = EnvironmentalSnapshot.NEUTRAL;
  private double geometryBonus = 1.0;
  private double areaScale = 1.0;
  private int ritualLevel = 0;

  // ---- modificadores de projetil (Fase J)
  private ProjectileMods.Mods mods = ProjectileMods.Mods.NONE;
  private ProjectileMods.Limits limits = ProjectileMods.Limits.DEFAULT;
  private int bounces;
  private int triggers;
  private long lastBounceTick = Long.MIN_VALUE;
  private boolean shard;
  private double powerScale = 1.0;
  private UUID ignored;
  private int maxLife = 200;
  private Entity homingTarget;
  /** Fragmentos vivos por mundo (teto {@code fragmentsPerWorld}). */
  private static final Map<Level, Integer> SHARDS = new WeakHashMap<>();

  public ArcaneProjectileEntity(EntityType<? extends ArcaneProjectileEntity> type, Level level) {
    super(type, level);
  }

  public void setInscription(Inscription inscription) { this.inscription = inscription; }
  public void setPlan(CompiledExecutionPlan plan) {
    this.plan = plan;
    if (plan != null) {
      this.entityData.set(ELEMENT, SpellElement.of(plan).ordinal());
      this.entityData.set(TRAIL, SpellElement.trailOf(plan).ordinal());
    }
  }

  /** Elemento visual (cor e particulas); sincronizado para o cliente. */
  public SpellElement element() { return SpellElement.byOrdinal(this.entityData.get(ELEMENT)); }

  /** Elemento do rastro e do impacto (o segundo da magia; sem ele, o mesmo do corpo). */
  public SpellElement trailElement() { return SpellElement.byOrdinal(this.entityData.get(TRAIL)); }
  public void setPrecision(PrecisionSnapshot precision) { this.precision = precision; }
  public void setEnvironment(EnvironmentalSnapshot environment) { this.environment = environment; }

  /** Poder, area e nivel do ritual em que foi lancado: valem quando o projetil acertar. */
  public void setRitualBonus(double geometryBonus, double areaScale, int ritualLevel) {
    this.geometryBonus = geometryBonus;
    this.areaScale = areaScale;
    this.ritualLevel = ritualLevel;
  }

  /** Modificadores ja dentro do orcamento (quem conjura degrada antes, {@link ProjectileMods#budgeted}). */
  public void setMods(ProjectileMods.Mods mods, ProjectileMods.Limits limits) {
    this.mods = mods;
    this.limits = limits;
    this.entityData.set(MODS, mods.bits() | (shard ? ProjectileMods.BIT_SHARD : 0));
  }

  public ProjectileMods.Mods mods() { return mods; }

  public int modBits() { return this.entityData.get(MODS); }

  public int bounces() { return bounces; }

  /** Quantas vezes este projetil ja rodou os trechos da magia (acertos e quiques). */
  public int triggers() { return triggers; }

  public boolean isShard() { return shard; }

  public double powerScale() { return powerScale; }

  public static int shardsAlive(Level level) { return SHARDS.getOrDefault(level, 0); }

  @Override
  protected void defineSynchedData() {
    this.entityData.define(ELEMENT, 0);
    this.entityData.define(TRAIL, 0);
    this.entityData.define(MODS, 0);
  }

  /** O projetil de magia voa em linha reta: a gravidade nao o puxa para baixo (o Arco aplica a dele no tick). */
  @Override
  protected float getGravity() { return 0.0f; }

  @Override
  protected boolean canHitEntity(Entity e) {
    if (ignored != null && ignored.equals(e.getUUID())) return false;
    if (shard && e instanceof ArcaneProjectileEntity) return false;
    return super.canHitEntity(e);
  }

  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide) {
      // corpo (elemento 1) em volta do orbe e rastro (elemento 2) ficando para tras
      var body = com.arcane.fx.Fx.options(element(), com.arcane.fx.Fx.Role.BODY, 1f, -1);
      var trail = com.arcane.fx.Fx.options(trailElement(), com.arcane.fx.Fx.Role.TRAIL, 1f, -1);
      var back = getDeltaMovement().scale(-0.5);
      level().addParticle(body, getX() + (random.nextDouble() - 0.5) * 0.2, getY() + (random.nextDouble() - 0.5) * 0.2,
          getZ() + (random.nextDouble() - 0.5) * 0.2, 0.0, 0.0, 0.0);
      for (int i = 0; i < 2; i++) {
        double t = random.nextDouble();
        level().addParticle(trail, getX() + back.x * t, getY() + back.y * t, getZ() + back.z * t, 0.0, 0.0, 0.0);
      }
      modTrails();
    } else if (!isRemoved()) {
      steerServer();
      if (tickCount > maxLife) expire();
    }
  }

  /** Tempo de vida do orbe em ticks (10 s; os cacos do Fragmentar, 1,5 s). */
  public int maxLife() { return maxLife; }

  /**
   * Fim da vida (prompt de 26 nov, A2): o orbe que nao acertou nada DETONA no ponto onde esta, em vez de sumir sem efeito.
   * Os efeitos recebem esse ponto como alvo (efeitos de ponto agem ali; os que pedem criatura ou bloco nao tem o que
   * pegar). Trechos com "Ao Atingir" ou "Ao Bater" nao disparam: nao houve acerto. Os cacos do Fragmentar so somem.
   */
  void expire() {
    if (shard || plan == null || !(getOwner() instanceof LivingEntity caster) || !(level() instanceof ServerLevel server)) {
      discard();
      return;
    }
    // so detona com o entorno carregado: um efeito de area na borda do mundo carregado geraria chunks no meio do tick
    var at = blockPosition();
    if (!server.hasChunksAt(at.offset(-EXPIRE_LOADED_RADIUS, 0, -EXPIRE_LOADED_RADIUS), at.offset(EXPIRE_LOADED_RADIUS, 0, EXPIRE_LOADED_RADIUS))) {
      discard();
      return;
    }
    long started = System.nanoTime();
    ExecutionContext context = context(new ExecutionContext.TriggerEvent("expire", java.util.Optional.empty(), java.util.Optional.empty()));
    context.setResolvedTargets(TargetList.ofPoint(position()));
    float fxPower = (float) Math.max(0.3, Math.min(1.0, powerScale));
    // presenca: o orbe estoura no ar (anel de impacto virado para cima, decal do elemento, som de acerto mais grave)
    server.playSound(null, getX(), getY(), getZ(), com.arcane.registry.ModSounds.SPELL_HIT.get(),
        net.minecraft.sounds.SoundSource.PLAYERS, 0.8f * fxPower, 0.75f);
    com.arcane.network.ModNetwork.sendShape(server, com.arcane.network.packet.S2CShapeFxPacket.IMPACT, position(),
        new net.minecraft.world.phys.Vec3(0, 1, 0), fxPower, trailElement().color(), trailElement().ordinal(), 12);
    com.arcane.fx.Fx.impact(server, caster, position(), null, element(), trailElement(), fxPower);
    com.arcane.execution.WorldScars.leave(context, position());
    triggers++;
    expired = true;
    SpellPipeline.INSTANCE.runSegments(context);
    discard();
    long ms = (System.nanoTime() - started) / 1_000_000L;
    if (ms > 200) com.mojang.logging.LogUtils.getLogger().warn("Arcane: detonacao de fim de vida do projetil levou {} ms em {} ({})", ms, at, inscription);
  }

  /** Raio em blocos que precisa estar carregado em volta do orbe para ele detonar no fim da vida. */
  public static final int EXPIRE_LOADED_RADIUS = 32;

  /** Se o orbe detonou por fim de vida (para testes). */
  public boolean expired() { return expired; }

  private boolean expired;

  /** Rastro proprio de cada modificador (NAO visto em jogo): espiral (Perseguir), faisca (Ricochete), cacos (Fragmentar). */
  private void modTrails() {
    int bits = modBits();
    if (bits == 0) return;
    Vec3 v = getDeltaMovement();
    if ((bits & ProjectileMods.BIT_HOMING) != 0 && v.lengthSqr() > 1e-6) {
      // espiral fina em volta da trajetoria: le-se a curva
      Vec3 side = v.cross(new Vec3(0, 1, 0));
      if (side.lengthSqr() < 1e-6) side = new Vec3(1, 0, 0);
      side = side.normalize();
      Vec3 up = side.cross(v).normalize();
      double a = tickCount * 0.9;
      Vec3 off = side.scale(Math.cos(a) * 0.25).add(up.scale(Math.sin(a) * 0.25));
      level().addParticle(com.arcane.fx.Fx.options(element(), com.arcane.fx.Fx.Role.TRAIL, 0.6f, -1),
          getX() + off.x, getY() + off.y, getZ() + off.z, 0, 0, 0);
    }
    if ((bits & ProjectileMods.BIT_RICOCHET) != 0 && tickCount % 3 == 0) {
      level().addParticle(com.arcane.fx.Fx.options(trailElement(), com.arcane.fx.Fx.Role.IMPACT, 0.35f, 0xFFF2B0),
          getX(), getY(), getZ(), (random.nextDouble() - 0.5) * 0.1, 0.05, (random.nextDouble() - 0.5) * 0.1);
    }
    if ((bits & (ProjectileMods.BIT_FRAGMENT | ProjectileMods.BIT_SHARD)) != 0 && tickCount % 2 == 0) {
      float scale = (bits & ProjectileMods.BIT_SHARD) != 0 ? 0.3f : 0.45f;
      level().addParticle(com.arcane.fx.Fx.options(element(), com.arcane.fx.Fx.Role.TRAIL, scale, -1),
          getX() + (random.nextDouble() - 0.5) * 0.4, getY() + (random.nextDouble() - 0.5) * 0.4,
          getZ() + (random.nextDouble() - 0.5) * 0.4, 0, 0, 0);
    }
  }

  /** Servidor: gravidade do Arco (sem o arrasto do vanilla) e curva do Perseguir (no Arco, so na descida). */
  private void steerServer() {
    if (mods.arc() <= 0 && mods.homing() <= 0) return;
    Vec3 v = getDeltaMovement();
    if (mods.arc() > 0) {
      double drag = isInWater() ? 0.8 : 0.99;
      v = new Vec3(v.x / drag, v.y / drag - ProjectileMods.ARC_GRAVITY, v.z / drag);
    }
    if (mods.homing() > 0 && (mods.arc() <= 0 || v.y < 0)) {
      if (tickCount % 4 == 0 || homingTarget == null || !homingTarget.isAlive()) homingTarget = findHomingTarget(v);
      if (homingTarget != null) {
        Vec3 aim = homingTarget.position().add(0, homingTarget.getBbHeight() * 0.5, 0).subtract(position());
        v = ProjectileMods.steer(v, aim, ProjectileMods.turnPerTick(mods.homing(), limits.homingDegreesPerLevel()));
      }
    }
    setDeltaMovement(v);
  }

  /**
   * Alvo do Perseguir: o mais perto no cone a frente que passa pelos filtros do primeiro trecho da magia. Sem filtro,
   * nunca o conjurador, aliados (lista de convidados), bichos de estimacao nem invocacoes do mesmo dono. O conjurador
   * nunca e perseguido.
   */
  public Entity findHomingTarget(Vec3 dir) {
    if (!(level() instanceof ServerLevel server) || !(getOwner() instanceof LivingEntity caster) || plan == null) return null;
    double range = limits.homingRange();
    Vec3 from = position();
    List<Entity> found = new ArrayList<>(server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range),
        e -> e.isAlive() && e != caster && !e.isSpectator() && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand)
            && ProjectileMods.inCone(from, dir, e.position().add(0, e.getBbHeight() * 0.5, 0), limits.homingConeDegrees(), range)));
    if (found.isEmpty()) return null;
    var segment = plan.segments().isEmpty() ? null : plan.segments().get(0);
    if (segment != null && !segment.filters().isEmpty()) {
      ExecutionContext context = context(ExecutionContext.TriggerEvent.cast());
      TargetList current = new TargetList(found, List.of(), List.of());
      for (var filter : segment.filters()) {
        var resolver = com.arcane.glyph.GlyphHandlerRegistry.getFilter(filter.handler()).orElse(null);
        if (resolver != null) current = resolver.filter(context, current);
      }
      found = new ArrayList<>(current.entities());
      found.remove(caster);
    } else {
      var guests = com.arcane.execution.GuestLists.get(server.getServer());
      found.removeIf(e -> guests.isFriendly(caster.getUUID(), e) || com.arcane.execution.Phase4Service.sameTeam(caster, e));
    }
    return found.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(this))).orElse(null);
  }

  private ExecutionContext context(ExecutionContext.TriggerEvent trigger) {
    ExecutionContext context = new ExecutionContext((ServerLevel) level(), (LivingEntity) getOwner(),
        inscription, plan, environment, precision, trigger);
    context.setGeometryBonus(geometryBonus * powerScale);
    context.setAreaScale(areaScale);
    context.setRitualLevel(ritualLevel);
    return context;
  }

  @Override
  public void remove(Entity.RemovalReason reason) {
    if (shard && !level().isClientSide && !isRemoved()) SHARDS.merge(level(), -1, (a, b) -> Math.max(0, a + b));
    super.remove(reason);
  }

  @Override
  protected void onHit(HitResult result) {
    // Ricochete: no maximo um acerto de bloco por tick (o Ao Bater dispara uma vez por quique)
    if (!level().isClientSide && result instanceof BlockHitResult && lastBounceTick == level().getGameTime()) return;
    super.onHit(result);
    if (!level().isClientSide && getOwner() instanceof LivingEntity caster && plan != null) {
      ServerLevel server = (ServerLevel) level();
      boolean bounce = result instanceof BlockHitResult && bounces < mods.ricochet();
      ExecutionContext context = context(result instanceof EntityHitResult entityHit
          ? new ExecutionContext.TriggerEvent("hit_entity", java.util.Optional.ofNullable(entityHit.getEntity()), java.util.Optional.empty())
          : result instanceof BlockHitResult blockHit2
              ? new ExecutionContext.TriggerEvent("hit_block", java.util.Optional.empty(), java.util.Optional.of(blockHit2.getBlockPos()))
              : ExecutionContext.TriggerEvent.cast());
      if (result instanceof EntityHitResult entityHit && entityHit.getEntity() != null) {
        context.setResolvedTargets(TargetList.ofEntities(entityHit.getEntity()));
      } else if (result instanceof BlockHitResult blockHit) {
        net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
        context.setResolvedTargets(new TargetList(List.of(),
            List.of(new TargetList.BlockTarget(pos, level().getBlockState(pos)))));
      }
      float fxPower = (float) Math.max(0.3, Math.min(1.0, powerScale));
      server.playSound(null, getX(), getY(), getZ(), com.arcane.registry.ModSounds.SPELL_HIT.get(),
          net.minecraft.sounds.SoundSource.PLAYERS, 0.9f * fxPower, bounce ? 1.3f : 1.0f);
      com.arcane.network.ModNetwork.sendShape(server, com.arcane.network.packet.S2CShapeFxPacket.IMPACT, position(),
          getDeltaMovement().scale(-1).normalize(), fxPower, trailElement().color(), trailElement().ordinal(), shard ? 5 : 12);
      net.minecraft.core.Direction face = result instanceof BlockHitResult bh ? bh.getDirection() : null;
      com.arcane.fx.Fx.impact(server, caster, face != null ? result.getLocation() : position(), face, element(), trailElement(), fxPower); // decal no ponto exato
      if (!shard) com.arcane.execution.WorldScars.leave(context, position());
      if (!shard && element() == SpellElement.STORM) {
        server.sendParticles(com.arcane.registry.ModParticles.ARCANE_BOLT_STRIKE.get(), getX(), getY() + 1.6, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
      } else if (!shard && element() == SpellElement.FORCE) {
        server.sendParticles(com.arcane.registry.ModParticles.ARCANE_SHOCK.get(), getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.0);
      }
      triggers++;
      SpellPipeline.INSTANCE.runSegments(context);
      if (bounce && result instanceof BlockHitResult bh) {
        bounceOff(server, caster, bh);
        return;
      }
      if (mods.fragment() > 0 && !shard) split(server, caster, result);
      discard();
    }
  }

  /** Ricochete: reflete na face, perde poder, faiscas (NAO visto em jogo). */
  private void bounceOff(ServerLevel server, LivingEntity caster, BlockHitResult hit) {
    bounces++;
    lastBounceTick = server.getGameTime();
    powerScale *= ProjectileMods.bouncePower(1, limits.ricochetPowerLoss());
    Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
    Vec3 v = ProjectileMods.reflect(getDeltaMovement(), n);
    setPos(hit.getLocation().add(n.scale(0.15)));
    setDeltaMovement(v);
    homingTarget = null;
    com.arcane.fx.Fx.burst(server, caster, hit.getLocation(), trailElement(), com.arcane.fx.Fx.Role.IMPACT, 8, 0.1, 0.25, 0.4f, 0xFFF2B0);
  }

  /** Fragmentar: N cacos menores saem do ponto de acerto; caco nunca fragmenta nem ricocheteia (NAO visto em jogo). */
  @SuppressWarnings("unchecked")
  private void split(ServerLevel server, LivingEntity caster, HitResult result) {
    int alive = SHARDS.getOrDefault(server, 0);
    int n = Math.min(mods.fragment(), Math.max(0, limits.fragmentsPerWorld() - alive));
    if (n <= 0) return;
    double each = ProjectileMods.fragmentPower(mods.fragment(), limits.fragmentTotalPower(), limits.fragmentMaxEach());
    Vec3 back = getDeltaMovement().lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : getDeltaMovement().normalize().scale(-1);
    if (result instanceof BlockHitResult bh) back = Vec3.atLowerCornerOf(bh.getDirection().getNormal());
    UUID skip = result instanceof EntityHitResult eh && eh.getEntity() != null ? eh.getEntity().getUUID() : null;
    Vec3 at = result.getLocation().add(back.scale(0.3));
    Vec3 side = back.cross(new Vec3(0, 1, 0));
    if (side.lengthSqr() < 1e-6) side = new Vec3(1, 0, 0);
    side = side.normalize();
    Vec3 up = side.cross(back).normalize();
    double speed = Math.max(0.6, getDeltaMovement().length() * 0.7);
    for (int i = 0; i < n; i++) {
      double a = 2 * Math.PI * i / n;
      Vec3 dir = back.scale(0.6).add(side.scale(Math.cos(a) * 0.8)).add(up.scale(Math.sin(a) * 0.8)).normalize();
      ArcaneProjectileEntity s = new ArcaneProjectileEntity((EntityType<? extends ArcaneProjectileEntity>) getType(), server);
      s.setOwner(caster);
      s.shard = true;
      s.ignored = skip;
      s.maxLife = 30;
      s.powerScale = powerScale * each;
      s.setPos(at.x, at.y, at.z);
      s.setDeltaMovement(dir.scale(speed));
      s.setInscription(inscription);
      s.setPlan(plan);
      s.setPrecision(precision);
      s.setEnvironment(environment);
      s.setRitualBonus(geometryBonus, areaScale, ritualLevel);
      s.setMods(ProjectileMods.Mods.NONE, limits);
      if (server.addFreshEntity(s)) SHARDS.merge(server, 1, Integer::sum);
    }
    com.arcane.fx.Fx.burst(server, caster, at, element(), com.arcane.fx.Fx.Role.IMPACT, 10, 0.25, 0.35, 0.5f, -1);
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
