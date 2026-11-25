package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.MindStates;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import com.arcane.execution.Wetness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Efeitos baratos da Fase 3: Chuva, Investida, Provocar, Medo, Sono, Estrondo, Molhar, Roubo de Vida, Transferir Vida
 * e Ler Item. Nada aqui roda por tick: tudo acontece na conjuracao (Medo e Sono deixam uma marca lida so por eventos,
 * ver {@link MindStates}).
 */
public final class LowCostEffects {
  private LowCostEffects() {}

  private static double p(ExecutionContext c, EffectCall call, String key, double def) {
    return c.amplify(call.parameters().getOrDefault(key, def), call.magnitudeKeys(), key);
  }

  /** Centros dos alvos: criaturas, blocos e pontos vazios. */
  static List<Vec3> centers(TargetList targets) {
    List<Vec3> out = new ArrayList<>();
    for (Entity e : targets.entities()) out.add(e.position());
    for (var b : targets.blocks()) out.add(Vec3.atCenterOf(b.pos()));
    out.addAll(targets.points());
    return out;
  }

  private static Set<LivingEntity> near(ExecutionContext c, List<Vec3> centers, double radius, boolean includeCaster) {
    Set<LivingEntity> out = new LinkedHashSet<>();
    for (Vec3 center : centers) {
      for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), x -> x.isAlive())) {
        if (e.position().distanceToSqr(center) > radius * radius + 1.0) continue;
        if (e == c.caster() && !includeCaster) continue;
        out.add(e);
      }
    }
    return out;
  }

  // ------------------------------------------------------------ Chuva
  /** Chuva local: molha quem estiver na area, apaga fogo, rega plantacoes. Nao muda o clima (isso e o ritual Clima). */
  public static class Rain implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      double radius = Math.min(Math.min(ExecutionContext.cap(48.0), Math.max(10.0, 10.0 * context.areaFactor())), p(context, call, "radius", 4.0) * context.areaFactor());
      int wet = (int) Math.min(Wetness.MAX_TICKS, p(context, call, "duration", 400.0));
      List<Vec3> centers = centers(targets);
      for (LivingEntity e : near(context, centers, radius, true)) Wetness.soak(context.level(), e, wet);
      for (Vec3 center : centers) {
        context.level().sendParticles(ParticleTypes.RAIN, center.x, center.y + 3.0, center.z, 60, radius * 0.5, 0.2, radius * 0.5, 0.0);
        context.level().sendParticles(ParticleTypes.CLOUD, center.x, center.y + 3.5, center.z, 8, radius * 0.4, 0.1, radius * 0.4, 0.0);
        context.level().playSound(null, center.x, center.y, center.z, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.8f, 1.0f);
      }
      if (!Targeting.mayAlterWorld(context)) return;
      int changed = 0;
      for (Vec3 center : centers) {
        BlockPos base = BlockPos.containing(center);
        int r = (int) Math.ceil(radius);
        for (int x = -r; x <= r && changed < 24; x++) {
          for (int z = -r; z <= r && changed < 24; z++) {
            if (x * x + z * z > r * r) continue;
            for (int y = -1; y <= 1 && changed < 24; y++) {
              BlockPos pos = base.offset(x, y, z);
              var state = context.level().getBlockState(pos);
              if (state.is(Blocks.FIRE)) {
                context.level().removeBlock(pos, false);
                changed++;
              } else if (state.is(Blocks.FARMLAND)
                  && state.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) < 7) {
                context.level().setBlockAndUpdate(pos, state.setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7));
                changed++;
              }
            }
          }
        }
      }
    }
  }

  // ------------------------------------------------------------ Investida
  /** Investida: voce avanca em linha reta contra o alvo (ou para onde olha) ferindo quem estiver no caminho. */
  public static class Charge implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      LivingEntity caster = context.caster();
      double distance = Math.min(16.0, p(context, call, "distance", 8.0));
      float damage = (float) Math.min(12.0, p(context, call, "damage", 3.0));
      Vec3 dir = null;
      for (Entity e : targets.entities()) {
        if (e != caster) { dir = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(caster.getEyePosition()); break; }
      }
      if (dir == null && !targets.points().isEmpty()) dir = targets.points().get(0).subtract(caster.getEyePosition());
      if (dir == null && !targets.blocks().isEmpty()) dir = Vec3.atCenterOf(targets.blocks().get(0).pos()).subtract(caster.getEyePosition());
      if (dir == null) dir = context.look();
      Vec3 flat = new Vec3(dir.x, 0, dir.z);
      if (flat.lengthSqr() < 1.0e-4) flat = new Vec3(context.look().x, 0, context.look().z);
      if (flat.lengthSqr() < 1.0e-4) return;
      flat = flat.normalize();
      Vec3 start = caster.position();
      Vec3 end = start.add(flat.scale(distance));
      var source = com.arcane.execution.Pierce.spell(context.level(), caster);
      AABB sweep = new AABB(start, end).inflate(1.3, 1.5, 1.3);
      for (LivingEntity e : context.level().getEntitiesOfClass(LivingEntity.class, sweep, x -> x.isAlive() && x != caster)) {
        Vec3 rel = e.position().subtract(start);
        double along = rel.dot(flat);
        if (along < 0 || along > distance + 1.0) continue;
        double side = rel.subtract(flat.scale(along)).horizontalDistance();
        if (side > 1.4) continue;
        if (damage > 0) e.hurt(source, damage);
        e.push(flat.x * 0.6, 0.25, flat.z * 0.6);
        e.hurtMarked = true;
      }
      double speed = distance / 2.4;
      caster.setDeltaMovement(flat.x * speed, Math.max(caster.getDeltaMovement().y, 0.18), flat.z * speed);
      caster.hurtMarked = true;
      caster.fallDistance = 0;
      context.level().sendParticles(ParticleTypes.CLOUD, start.x, start.y + 0.2, start.z, 12, 0.3, 0.1, 0.3, 0.05);
      context.level().playSound(null, start.x, start.y, start.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
  }

  // ------------------------------------------------------------ Provocar
  /** Provocar: criaturas hostis passam a perseguir quem conjurou (acorda o Sono e quebra o Medo). */
  public static class Taunt implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      for (Entity e : targets.entities()) {
        if (!(e instanceof Mob mob) || e == context.caster()) continue;
        MindStates.clear(mob);
        mob.setTarget(context.caster());
        mob.setLastHurtByMob(context.caster());
        context.level().sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
      }
    }
  }

  // ------------------------------------------------------------ Medo
  /** Medo: a criatura larga o alvo, foge de quem conjurou e nao o escolhe como alvo ate passar. Jogadores ficam trementes. */
  public static class Fear implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int ticks = (int) Math.max(20, Math.min(400, p(context, call, "duration", 100.0)));
      for (Entity e : targets.entities()) {
        if (!(e instanceof LivingEntity living) || e == context.caster()) continue;
        MindStates.scare(living, context.caster(), ticks);
        if (living instanceof Mob mob) {
          mob.setTarget(null);
          Vec3 away = mob.position().subtract(context.caster().position());
          if (away.horizontalDistanceSqr() < 0.01) away = context.look();
          Vec3 goal = mob.position().add(new Vec3(away.x, 0, away.z).normalize().scale(14.0));
          mob.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.5);
          mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Math.min(ticks, 100), 0, false, false));
        } else {
          living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0), context.caster());
          living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 0), context.caster());
        }
        context.level().sendParticles(ParticleTypes.SMOKE, living.getX(), living.getY() + living.getBbHeight(), living.getZ(), 8, 0.25, 0.2, 0.25, 0.02);
      }
    }
  }

  // ------------------------------------------------------------ Sono
  /** Sono: a criatura larga o alvo e fica imovel e fraca; qualquer dano a acorda. */
  public static class Sleep implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int ticks = (int) Math.max(20, Math.min(600, p(context, call, "duration", 100.0)));
      for (Entity e : targets.entities()) {
        if (!(e instanceof LivingEntity living) || e == context.caster()) continue;
        MindStates.sleep(living, ticks);
        if (living instanceof Mob mob) mob.setTarget(null);
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, false), context.caster());
        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 3, false, false), context.caster());
        context.level().sendParticles(ParticleTypes.NOTE, living.getX(), living.getY() + living.getBbHeight() + 0.2, living.getZ(), 4, 0.2, 0.1, 0.2, 1.0);
      }
    }
  }

  // ------------------------------------------------------------ Estrondo
  /** Estrondo: um trovao seco no alvo: fere de leve, empurra para fora e deixa a criatura zonza. Nao quebra blocos. */
  public static class Rumble implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      double radius = Math.min(Math.min(ExecutionContext.cap(48.0), Math.max(8.0, 8.0 * context.areaFactor())), p(context, call, "radius", 3.0) * context.areaFactor());
      float damage = (float) Math.min(12.0, p(context, call, "damage", 2.0));
      List<Vec3> centers = centers(targets);
      var source = com.arcane.execution.Pierce.spell(context.level(), context.caster());
      for (Vec3 center : centers) {
        context.level().playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5f, 1.4f);
        // poeira e lascas da terra num anel do tamanho do estrondo (antes: explosao e nuvem do jogo base, que tapavam a cena)
        com.arcane.fx.Fx.ring(context.level(), context.caster(), center, radius, com.arcane.execution.SpellElement.EARTH, com.arcane.fx.Fx.Role.IMPACT, 3.0);
        com.arcane.fx.Fx.burst(context.level(), context.caster(), center.add(0, 0.3, 0), com.arcane.execution.SpellElement.EARTH,
            com.arcane.fx.Fx.Role.REACTION, 10, radius * 0.3, 0.04, 1.2f, 0x8A7A66);
      }
      Set<Entity> seen = new LinkedHashSet<>();
      for (Vec3 center : centers) {
        for (LivingEntity e : near(context, List.of(center), radius, false)) {
          if (!seen.add(e)) continue;
          if (damage > 0) e.hurt(source, damage);
          Vec3 away = e.position().subtract(center);
          Vec3 d = away.horizontalDistanceSqr() < 0.04 ? Vec3.ZERO : new Vec3(away.x, 0, away.z).normalize();
          e.push(d.x * 0.9, 0.3, d.z * 0.9);
          e.hurtMarked = true;
          e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false), context.caster());
        }
      }
    }
  }

  // ------------------------------------------------------------ Molhar
  /** Molhar: deixa a criatura molhada (apaga o fogo). Molhado: raio fere mais, gelo congela mais rapido, fogo apaga. */
  public static class Soak implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int ticks = (int) Math.min(Wetness.MAX_TICKS, p(context, call, "duration", 200.0));
      for (Entity e : targets.entities()) Wetness.soak(context.level(), e, ticks);
    }
  }

  // ------------------------------------------------------------ Roubo de Vida
  /** Roubo de Vida: fere o alvo e recupera parte do dano realmente causado. */
  public static class LifeSteal implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      float amount = (float) Math.min(16.0, p(context, call, "amount", 3.0));
      double ratio = Math.max(0.1, Math.min(1.0, call.parameters().getOrDefault("ratio", 0.6)));
      var source = com.arcane.execution.Pierce.spell(context.level(), context.caster());
      float stolen = 0;
      for (Entity e : targets.entities()) {
        if (!(e instanceof LivingEntity living) || e == context.caster()) continue;
        float before = living.getHealth();
        living.hurt(source, amount);
        stolen += Math.max(0, before - Math.max(0, living.getHealth()));
        context.level().sendParticles(ParticleTypes.DAMAGE_INDICATOR, living.getX(), living.getY() + 1.0, living.getZ(), 4, 0.2, 0.3, 0.2, 0.1);
      }
      if (stolen > 0) {
        context.caster().heal((float) (stolen * ratio));
        context.level().sendParticles(ParticleTypes.HEART, context.caster().getX(), context.caster().getY() + 1.6, context.caster().getZ(), 3, 0.3, 0.3, 0.3, 0.0);
      }
    }
  }

  // ------------------------------------------------------------ Transferir Vida
  /** Transferir Vida: voce passa vida ao alvo. Nunca derruba voce abaixo de 1 coracao. Invertido, e o Roubo de Vida. */
  public static class LifeTransfer implements EffectHandler {
    public static final float KEEP = 2.0f;

    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      float amount = (float) Math.min(16.0, p(context, call, "amount", 4.0));
      LivingEntity caster = context.caster();
      for (Entity e : targets.entities()) {
        if (!(e instanceof LivingEntity living) || e == caster) continue;
        float give = Math.min(amount, Math.max(0, caster.getHealth() - KEEP));
        give = Math.min(give, living.getMaxHealth() - living.getHealth());
        if (give <= 0) continue;
        caster.setHealth(caster.getHealth() - give);
        living.heal(give);
        context.level().sendParticles(ParticleTypes.HEART, living.getX(), living.getY() + living.getBbHeight() + 0.2, living.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
      }
    }
  }

  // ------------------------------------------------------------ Ler Item
  /** Ler Item: mostra a quem conjurou o item na mao da criatura (ou o item solto no chao). So informa; nao muda nada. */
  public static class ReadItem implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      if (!(context.caster() instanceof Player player)) return;
      int shown = 0;
      for (Entity e : targets.entities()) {
        ItemStack stack = ItemStack.EMPTY;
        if (e instanceof ItemEntity item) stack = item.getItem();
        else if (e instanceof LivingEntity living) {
          stack = living.getMainHandItem().isEmpty() ? living.getOffhandItem() : living.getMainHandItem();
        }
        if (shown++ >= 4) break;
        player.sendSystemMessage(describe(e, stack));
      }
      if (shown == 0) player.displayClientMessage(Component.translatable("message.arcane.read_item.none"), true);
    }

    public static Component describe(Entity holder, ItemStack stack) {
      Component who = holder.getDisplayName();
      if (stack.isEmpty()) return Component.translatable("message.arcane.read_item.empty", who);
      var out = Component.translatable("message.arcane.read_item", who, stack.getCount(), stack.getHoverName());
      if (stack.isDamageableItem()) {
        out.append(Component.translatable("message.arcane.read_item.durability", stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()));
      }
      var enchants = EnchantmentHelper.getEnchantments(stack);
      if (!enchants.isEmpty()) {
        out.append(Component.translatable("message.arcane.read_item.enchants"));
        boolean first = true;
        for (var entry : enchants.entrySet()) {
          if (!first) out.append(", ");
          first = false;
          out.append(entry.getKey().getFullname(entry.getValue()));
        }
      }
      return out;
    }
  }
}
