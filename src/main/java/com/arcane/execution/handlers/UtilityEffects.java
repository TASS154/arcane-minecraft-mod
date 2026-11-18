package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.MindStates;
import com.arcane.execution.Phase4Service;
import com.arcane.execution.Phase4State;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import com.arcane.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Efeitos de movimento e utilidade da Fase 4: Gancho, Escalar, Atravessar, Retorno, Troca, Sentir, Visao Arcana, Emitir Sinal, Chamariz e Revelar. */
public final class UtilityEffects {
  private UtilityEffects() {}

  private static double p(ExecutionContext c, EffectCall call, String key, double def) {
    return c.amplify(call.parameters().getOrDefault(key, def), call.magnitudeKeys(), key);
  }

  private static void say(LivingEntity who, String key, Object... args) {
    if (who instanceof Player player) player.displayClientMessage(Component.translatable(key, args), true);
  }

  private static Vec3 firstPoint(TargetList targets, LivingEntity caster) {
    for (Entity e : targets.entities()) if (e != caster) return e.position().add(0, e.getBbHeight() * 0.5, 0);
    if (!targets.points().isEmpty()) return targets.points().get(0);
    if (!targets.blocks().isEmpty()) return Vec3.atCenterOf(targets.blocks().get(0).pos());
    return null;
  }

  // ------------------------------------------------------------ Gancho
  /** Gancho: voce e puxado em linha reta ate o alvo (criatura, bloco ou ponto), como um cabo preso nele. */
  public static class Hook implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      LivingEntity caster = context.caster();
      Vec3 goal = firstPoint(targets, caster);
      if (goal == null) { say(caster, "message.arcane.hook.none"); return; }
      Vec3 to = goal.subtract(caster.position().add(0, caster.getBbHeight() * 0.5, 0));
      double dist = to.length();
      if (dist < 1.5) return;
      double strength = Math.min(3.0, p(context, call, "strength", 1.2));
      double speed = Math.min(2.4, (0.35 + dist * 0.08)) * strength;
      Vec3 v = to.normalize().scale(speed);
      caster.setDeltaMovement(v.x, Math.max(v.y, 0.25), v.z);
      caster.hurtMarked = true;
      caster.fallDistance = 0;
      for (int i = 0; i < 8; i++) {
        Vec3 pt = caster.position().add(0, 1.0, 0).add(to.scale(i / 8.0));
        context.level().sendParticles(ParticleTypes.CRIT, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
      }
    }
  }

  // ------------------------------------------------------------ Escalar
  /** Escalar: por um tempo, quem avanca contra uma parede sobe por ela (sem cair; agache para parar). */
  public static class Climb implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int ticks = (int) Math.min(1200, p(context, call, "duration", 200.0));
      for (Entity e : targets.entities()) {
        if (e instanceof LivingEntity living) {
          Phase4Service.addClimber(living, ticks);
          context.level().sendParticles(ParticleTypes.POOF, living.getX(), living.getY() + 0.5, living.getZ(), 4, 0.2, 0.2, 0.2, 0.01);
        }
      }
    }
  }

  // ------------------------------------------------------------ Atravessar
  /** Atravessar: voce passa por uma parede (ate a espessura escolhida) e sai do outro lado, onde houver espaco. */
  public static class Phase implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      LivingEntity caster = context.caster();
      int thickness = (int) Math.max(1, Math.min(6, Math.round(p(context, call, "thickness", 4.0))));
      Vec3 look = context.look();
      Vec3 dir = new Vec3(look.x, 0, look.z);
      if (dir.lengthSqr() < 1.0e-4) return;
      dir = dir.normalize();
      var level = context.level();
      boolean sawSolid = false;
      for (int k = 1; k <= thickness + 1; k++) {
        Vec3 at = caster.position().add(dir.scale(k));
        BlockPos foot = BlockPos.containing(at.x, caster.getY() + 0.01, at.z);
        boolean free = passable(level.getBlockState(foot), level, foot) && passable(level.getBlockState(foot.above()), level, foot.above());
        if (!free) { sawSolid = true; continue; }
        if (!sawSolid) continue; // so interessa depois de uma parede
        Vec3 dest = new Vec3(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5);
        Vec3 from = caster.position();
        if (caster.randomTeleport(dest.x, dest.y, dest.z, true)) {
          caster.fallDistance = 0;
          level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0, from.z, 14, 0.3, 0.5, 0.3, 0.3);
          level.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0, dest.z, 14, 0.3, 0.5, 0.3, 0.3);
          com.arcane.execution.ElectricTrail.draw(level, from.add(0, 1.0, 0), dest.add(0, 1.0, 0)); // raios por dentro da parede
          return;
        }
      }
      say(caster, "message.arcane.phase.blocked");
    }

    private static boolean passable(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
      return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty() && !state.is(net.minecraft.world.level.block.Blocks.FIRE);
    }
  }

  // ------------------------------------------------------------ Retorno
  /** Retorno: o primeiro uso marca o lugar (10 min); o segundo leva voce de volta, na mesma dimensao. */
  public static class Recall implements EffectHandler {
    public static final int MARK_TICKS = 12000;

    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      LivingEntity caster = context.caster();
      CompoundTag data = caster.getPersistentData();
      CompoundTag mark = data.getCompound(Phase4State.RECALL);
      long now = context.level().getGameTime();
      boolean valid = mark.contains("x") && mark.getLong("until") > now;
      if (valid && mark.getString("dim").equals(context.level().dimension().location().toString())) {
        Vec3 from = caster.position();
        if (caster.randomTeleport(mark.getDouble("x"), mark.getDouble("y"), mark.getDouble("z"), true)) {
          caster.fallDistance = 0;
          data.remove(Phase4State.RECALL);
          context.level().sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1.0, from.z, 16, 0.3, 0.5, 0.3, 0.2);
          say(caster, "message.arcane.recall.back");
        }
        return;
      }
      CompoundTag fresh = new CompoundTag();
      fresh.putDouble("x", caster.getX());
      fresh.putDouble("y", caster.getY());
      fresh.putDouble("z", caster.getZ());
      fresh.putString("dim", context.level().dimension().location().toString());
      fresh.putLong("until", now + MARK_TICKS);
      data.put(Phase4State.RECALL, fresh);
      context.level().sendParticles(ParticleTypes.ENCHANT, caster.getX(), caster.getY() + 1.0, caster.getZ(), 16, 0.4, 0.6, 0.4, 0.3);
      say(caster, valid ? "message.arcane.recall.otherdim" : "message.arcane.recall.marked");
    }
  }

  // ------------------------------------------------------------ Troca
  /** Troca: voce e o alvo trocam de lugar (mesma dimensao). */
  public static class Swap implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      LivingEntity caster = context.caster();
      for (Entity e : targets.entities()) {
        if (e == caster || !(e instanceof LivingEntity other) || e.level() != caster.level()) continue;
        Vec3 a = caster.position(), b = other.position();
        float yawA = caster.getYRot(), yawB = other.getYRot();
        if (!other.randomTeleport(a.x, a.y, a.z, true)) return;
        if (!caster.randomTeleport(b.x, b.y, b.z, true)) { other.randomTeleport(b.x, b.y, b.z, true); return; }
        caster.fallDistance = 0;
        other.fallDistance = 0;
        context.level().sendParticles(ParticleTypes.REVERSE_PORTAL, a.x, a.y + 1.0, a.z, 12, 0.3, 0.5, 0.3, 0.2);
        context.level().sendParticles(ParticleTypes.REVERSE_PORTAL, b.x, b.y + 1.0, b.z, 12, 0.3, 0.5, 0.3, 0.2);
        return;
      }
    }
  }

  // ------------------------------------------------------------ Sentir
  /** Criaturas vivas dentro do raio, menos o conjurador. */
  public static List<LivingEntity> scan(ExecutionContext context, double radius) {
    return context.level().getEntitiesOfClass(LivingEntity.class, context.caster().getBoundingBox().inflate(radius),
        e -> e != context.caster() && e.isAlive() && e.distanceToSqr(context.caster()) <= radius * radius);
  }

  /** Sentir: so voce percebe as criaturas por perto, mesmo atras de paredes ou invisiveis (particulas so para voce). */
  public static class Sense implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      double radius = Math.min(Math.min(64.0, Math.max(32.0, 32.0 * context.areaFactor())), p(context, call, "radius", 16.0) * context.areaFactor());
      List<LivingEntity> found = scan(context, radius);
      int hostile = 0;
      for (LivingEntity e : found) {
        if (e instanceof net.minecraft.world.entity.monster.Enemy) hostile++;
        if (context.caster() instanceof ServerPlayer sp) {
          context.level().sendParticles(sp, ParticleTypes.END_ROD, true, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
        }
      }
      say(context.caster(), "message.arcane.sense", found.size(), hostile);
    }
  }

  // ------------------------------------------------------------ Visao Arcana
  /** Visao Arcana: visao noturna por um tempo e mostra, so para voce, residuos de magia e inscricoes por perto. */
  public static class ArcaneSight implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      LivingEntity caster = context.caster();
      int ticks = (int) Math.min(1200, p(context, call, "duration", 400.0));
      double radius = Math.min(32.0, p(context, call, "radius", 24.0));
      caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks, 0, true, false));
      int scars = 0, inscriptions = 0;
      for (var scar : com.arcane.execution.WorldScars.all(context.level())) {
        if (scar.pos().distanceToSqr(caster.position()) > radius * radius) continue;
        scars++;
        if (caster instanceof ServerPlayer sp) context.level().sendParticles(sp, ParticleTypes.WITCH, true, scar.pos().x, scar.pos().y + 0.5, scar.pos().z, 6, 0.3, 0.3, 0.3, 0.02);
      }
      for (var anchor : com.arcane.inscribing.WorldInscriptionIndex.get(context.level()).all()) {
        BlockPos pos = anchor.pos();
        if (pos.distSqr(caster.blockPosition()) > radius * radius) continue;
        inscriptions++;
        if (caster instanceof ServerPlayer sp) context.level().sendParticles(sp, ParticleTypes.ENCHANT, true, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.3);
      }
      say(caster, "message.arcane.sight", scars, inscriptions);
    }
  }

  // ------------------------------------------------------------ Emitir Sinal
  /** Emitir Sinal: acende por instantes um bloco que emite sinal de redstone (forca 15) no ar, onde o alvo esta. */
  public static class EmitSignal implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      if (!Targeting.mayAlterWorld(context)) return;
      int ticks = (int) Math.max(10, Math.min(200, p(context, call, "duration", 40.0)));
      List<BlockPos> spots = new ArrayList<>();
      for (var b : targets.blocks()) spots.add(b.pos().above());
      for (Entity e : targets.entities()) spots.add(e.blockPosition());
      for (Vec3 v : targets.points()) spots.add(BlockPos.containing(v));
      int placed = 0;
      for (BlockPos pos : spots) {
        if (placed >= 4) break;
        if (!context.level().getBlockState(pos).canBeReplaced()) continue;
        context.level().setBlockAndUpdate(pos, ModBlocks.ARCANE_SIGNAL.get().defaultBlockState());
        context.level().scheduleTick(pos, ModBlocks.ARCANE_SIGNAL.get(), ticks);
        placed++;
      }
    }
  }

  // ------------------------------------------------------------ Chamariz
  /** Chamariz: uma ilusao atrai as criaturas para o ponto e elas deixam de te escolher como alvo enquanto durar. */
  public static class Decoy implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      LivingEntity caster = context.caster();
      int ticks = (int) Math.max(20, Math.min(300, p(context, call, "duration", 100.0)));
      Vec3 where = firstPoint(targets, caster);
      if (where == null) where = caster.position().add(context.look().scale(6.0));
      Phase4Service.addDecoy(caster, where, ticks);
      for (Mob mob : context.level().getEntitiesOfClass(Mob.class, new AABB(where, where).inflate(20.0), m -> m.isAlive() && m != caster
          && Phase4Service.ownerOf(m) == null)) {
        if (mob.getTarget() == caster) mob.setTarget(null);
        MindStates.distract(mob, caster, 40);
        mob.getNavigation().moveTo(where.x, where.y, where.z, 1.0);
      }
    }
  }

  // ------------------------------------------------------------ Revelar
  /** Revelar: tira a invisibilidade e faz brilhar as criaturas no raio, mostrando o que estava escondido. */
  public static class Reveal implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      double radius = Math.min(Math.min(64.0, Math.max(24.0, 24.0 * context.areaFactor())), p(context, call, "radius", 12.0) * context.areaFactor());
      int revealed = 0;
      for (LivingEntity e : scan(context, radius)) {
        if (e.hasEffect(MobEffects.INVISIBILITY) || e.isInvisible()) revealed++;
        e.removeEffect(MobEffects.INVISIBILITY);
        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false), context.caster());
      }
      say(context.caster(), "message.arcane.reveal", revealed);
    }
  }
}
