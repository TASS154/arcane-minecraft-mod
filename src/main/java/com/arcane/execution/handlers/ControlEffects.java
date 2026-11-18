package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.MindStates;
import com.arcane.execution.Phase4Service;
import com.arcane.execution.Phase4State;
import com.arcane.execution.TargetList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Efeitos de combate e controle da Fase 4: Satelites, Elo, Rastro, Encantar, Petrificar, Acido, Eletrificar, Contagio, Reanimar, Dissipar, Refletir, Silencio, Drenar Mana e Maldicao. */
public final class ControlEffects {
  private ControlEffects() {}

  private static double p(ExecutionContext c, EffectCall call, String key, double def) {
    return c.amplify(call.parameters().getOrDefault(key, def), call.magnitudeKeys(), key);
  }

  private static void say(LivingEntity who, String key, Object... args) {
    if (who instanceof Player player) player.displayClientMessage(Component.translatable(key, args), true);
  }

  private static int ticks(ExecutionContext c, EffectCall call, double def, int max) {
    return (int) Math.max(1, Math.min(max, p(c, call, "duration", def)));
  }

  /** Aplica um estado temporario (nome da chave) a cada criatura alvo que nao seja o conjurador. */
  private abstract static class StateEffect implements EffectHandler {
    private final String key; private final double def; private final int max;
    StateEffect(String key, double def, int max) { this.key = key; this.def = def; this.max = max; }

    void extra(ExecutionContext context, LivingEntity living, int ticks) {}

    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int t = ticks(context, call, def, max);
      for (Entity e : targets.entities()) {
        if (e instanceof LivingEntity living && living != context.caster()) {
          Phase4State.set(living, key, t);
          extra(context, living, t);
          context.level().sendParticles(ParticleTypes.ENCHANT, living.getX(), living.getY() + living.getBbHeight() * 0.6, living.getZ(), 10, 0.3, 0.4, 0.3, 0.3);
        }
      }
    }
  }

  /** Maldicao: o alvo leva 25% a mais de dano e nao recupera vida enquanto durar. */
  public static class Curse extends StateEffect { public Curse() { super(Phase4State.CURSE, 300, 2400); } }

  /** Silencio: o alvo nao consegue conjurar (jogadores e magos) enquanto durar. */
  public static class Silence extends StateEffect {
    public Silence() { super(Phase4State.SILENCE, 100, 600); }
    @Override void extra(ExecutionContext context, LivingEntity living, int ticks) { say(living, "message.arcane.silenced"); }
  }

  /** Petrificar: imovel, com metade do dano recebido e sem escolher alvo; o dano nao quebra o efeito. */
  public static class Petrify extends StateEffect {
    public Petrify() { super(Phase4State.PETRIFY, 60, 300); }
    @Override void extra(ExecutionContext context, LivingEntity living, int ticks) {
      living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, false), context.caster());
      living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 3, false, false), context.caster());
      living.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 128, false, false), context.caster());
      // marcador sincronizado: o cliente desenha o modelo dela em pedra (PetrifyLayer)
      living.addEffect(new MobEffectInstance(com.arcane.registry.ModEffects.PETRIFIED.get(), ticks, 0, false, false, false), context.caster());
      context.level().sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
          living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(), 24, 0.4, living.getBbHeight() * 0.4, 0.4, 0.05);
      context.level().playSound(null, living.blockPosition(), net.minecraft.sounds.SoundEvents.STONE_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.7f);
      if (living instanceof Mob mob) mob.setTarget(null);
    }
  }

  /** Refletir: projeteis voltam para quem os lancou e 40% do dano de perto volta ao agressor. Vale para o conjurador tambem. */
  public static class Reflect implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int t = ticks(context, call, 100, 400);
      for (Entity e : targets.entities()) {
        if (e instanceof LivingEntity living) {
          Phase4State.set(living, Phase4State.REFLECT, t);
          context.level().sendParticles(ParticleTypes.ENCHANTED_HIT, living.getX(), living.getY() + 1.0, living.getZ(), 10, 0.4, 0.6, 0.4, 0.1);
        }
      }
    }
  }

  // ------------------------------------------------------------ Satelites
  public static class Satellites implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int count = (int) Math.round(p(context, call, "count", 2.0));
      int t = ticks(context, call, 200, 600);
      float damage = (float) Math.min(8.0, p(context, call, "damage", 2.0));
      for (Entity e : targets.entities()) {
        if (e instanceof LivingEntity living) Phase4Service.addSatellites(living, count, t, damage);
      }
    }
  }

  // ------------------------------------------------------------ Rastro
  public static class Trail implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int t = ticks(context, call, 200, 600);
      float damage = (float) Math.min(6.0, p(context, call, "damage", 1.5));
      for (Entity e : targets.entities()) {
        if (e instanceof LivingEntity living) Phase4Service.addTrail(living, t, damage);
      }
    }
  }

  // ------------------------------------------------------------ Elo
  /** Elo: liga duas criaturas (as duas primeiras alvo, ou voce e o alvo): metade do dano de uma passa para a outra. */
  public static class Link implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int t = ticks(context, call, 300, 1200);
      List<LivingEntity> pair = new ArrayList<>();
      for (Entity e : targets.entities()) if (e instanceof LivingEntity l && pair.size() < 2) pair.add(l);
      if (pair.size() == 1 && pair.get(0) != context.caster()) pair.add(context.caster());
      if (pair.size() < 2 || pair.get(0) == pair.get(1)) return;
      LivingEntity a = pair.get(0), b = pair.get(1);
      a.getPersistentData().putUUID(Phase4State.LINK_OF, b.getUUID());
      b.getPersistentData().putUUID(Phase4State.LINK_OF, a.getUUID());
      Phase4State.set(a, Phase4State.LINK, t);
      Phase4State.set(b, Phase4State.LINK, t);
      for (LivingEntity l : pair) context.level().sendParticles(ParticleTypes.ENCHANT, l.getX(), l.getY() + 1.0, l.getZ(), 12, 0.3, 0.5, 0.3, 0.4);
    }
  }

  // ------------------------------------------------------------ Encantar
  /** Encantar: a arma na mao do alvo ganha por um tempo e por poucos golpes um efeito (1 fogo, 2 gelo, 3 raio, 4 veneno). */
  public static class Imbue implements EffectHandler {
    public static final int MAX_HITS = 10;

    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int t = ticks(context, call, 1200, 6000);
      int kind = (int) Math.max(1, Math.min(4, Math.round(call.parameters().getOrDefault("kind", 1.0))));
      int hits = (int) Math.max(1, Math.min(MAX_HITS, Math.round(call.parameters().getOrDefault("hits", 6.0))));
      for (Entity e : targets.entities()) {
        if (!(e instanceof LivingEntity living)) continue;
        ItemStack stack = living.getMainHandItem();
        if (stack.isEmpty()) { say(context.caster(), "message.arcane.imbue.empty"); continue; }
        CompoundTag tag = stack.getOrCreateTagElement("arcane_imbue");
        tag.putInt("kind", kind);
        tag.putInt("hits", hits);
        tag.putLong("until", context.level().getGameTime() + t);
        context.level().sendParticles(ParticleTypes.ENCHANT, living.getX(), living.getY() + 1.2, living.getZ(), 12, 0.3, 0.3, 0.3, 0.3);
      }
    }
  }

  // ------------------------------------------------------------ Acido
  /** Acido: fere ignorando armadura e corroi as pecas de armadura (nunca as quebra). */
  public static class Acid implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      float amount = (float) Math.min(12.0, p(context, call, "amount", 3.0));
      int corrode = (int) Math.min(60, Math.round(call.parameters().getOrDefault("corrode", 8.0)));
      var source = context.level().damageSources().indirectMagic(context.caster(), context.caster());
      for (Entity e : targets.entities()) {
        if (!(e instanceof LivingEntity living) || e == context.caster()) continue;
        living.hurt(source, amount);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
          ItemStack piece = living.getItemBySlot(slot);
          if (piece.isDamageableItem()) piece.setDamageValue(Math.min(piece.getMaxDamage() - 1, piece.getDamageValue() + corrode));
        }
        context.level().sendParticles(ParticleTypes.ITEM_SLIME, living.getX(), living.getY() + 1.0, living.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
      }
    }
  }

  // ------------------------------------------------------------ Eletrificar / Condutor
  /** Eletrificar (mode 0): o dano que o alvo sofre salta para vizinhos. Condutor (mode 1): raios por perto caem nele. */
  public static class Electrify implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int t = ticks(context, call, 200, 600);
      boolean conductor = call.parameters().getOrDefault("mode", 0.0) >= 0.5;
      for (Entity e : targets.entities()) {
        if (!(e instanceof LivingEntity living)) continue;
        if (conductor) Phase4Service.addConductor(living, t); else Phase4State.set(living, Phase4State.CHARGED, t);
        context.level().sendParticles(com.arcane.registry.ModParticles.ARCANE_BOLT.get(), living.getX(), living.getY() + 1.0, living.getZ(), 8, 0.3, 0.5, 0.3, 0.05);
      }
    }
  }

  // ------------------------------------------------------------ Contagio
  /** Contagio: os efeitos nocivos do alvo (e o fogo) passam para as criaturas ao redor, mais curtos (60%). */
  public static class Contagion implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      double radius = Math.min(8.0, p(context, call, "radius", 4.0) * context.areaFactor());
      for (Entity e : targets.entities()) {
        if (!(e instanceof LivingEntity source)) continue;
        List<MobEffectInstance> harmful = new ArrayList<>();
        for (MobEffectInstance inst : source.getActiveEffects()) {
          if (inst.getEffect().getCategory() == MobEffectCategory.HARMFUL) harmful.add(inst);
        }
        boolean fire = source.isOnFire();
        if (harmful.isEmpty() && !fire) continue;
        for (LivingEntity near : context.level().getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(radius),
            n -> n != source && n != context.caster() && n.isAlive())) {
          for (MobEffectInstance inst : harmful) {
            near.addEffect(new MobEffectInstance(inst.getEffect(), Math.max(20, (int) (inst.getDuration() * 0.6)), inst.getAmplifier()), context.caster());
          }
          if (fire) near.setSecondsOnFire(3);
          context.level().sendParticles(ParticleTypes.SNEEZE, near.getX(), near.getY() + 1.0, near.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
        }
      }
    }
  }

  // ------------------------------------------------------------ Reanimar Aliado
  /** Reanimar Aliado: ergue, perto do alvo, uma criatura que morreu ha pouco (ate 2 min), com metade da vida, que luta por voce. */
  public static class Reanimate implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      int t = ticks(context, call, 600, 2400);
      List<Vec3> spots = new ArrayList<>();
      for (Entity e : targets.entities()) spots.add(e.position());
      spots.addAll(targets.points());
      for (var b : targets.blocks()) spots.add(Vec3.atCenterOf(b.pos()));
      if (spots.isEmpty()) spots.add(context.caster().position());
      for (Vec3 spot : spots) {
        var corpse = Phase4Service.takeCorpse(context.level(), spot, 6.0);
        if (corpse.isEmpty()) continue;
        Entity made = corpse.get().type().create(context.level());
        if (!(made instanceof Mob mob)) continue;
        mob.moveTo(corpse.get().pos().x, corpse.get().pos().y, corpse.get().pos().z, context.caster().getYRot(), 0);
        mob.setHealth(Math.max(1.0f, mob.getMaxHealth() * 0.5f));
        mob.setPersistenceRequired();
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, t, 0, false, false));
        if (!Phase4Service.addMinion(context.caster(), mob, t)) {
          mob.discard();
          say(context.caster(), "message.arcane.reanimate.full");
          return;
        }
        context.level().addFreshEntity(mob);
        context.level().sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1.0, mob.getZ(), 16, 0.3, 0.6, 0.3, 0.05);
        return;
      }
      say(context.caster(), "message.arcane.reanimate.none");
    }
  }

  // ------------------------------------------------------------ Dissipar
  /** Dissipar: em outros, tira efeitos benéficos e estados magicos; em voce, tira os nocivos (maldicao, silencio, medo...). */
  public static class Dispel implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      for (Entity e : targets.entities()) {
        if (!(e instanceof LivingEntity living)) continue;
        boolean self = living == context.caster();
        MobEffectCategory remove = self ? MobEffectCategory.HARMFUL : MobEffectCategory.BENEFICIAL;
        for (MobEffect effect : new ArrayList<>(living.getActiveEffectsMap().keySet())) {
          if (effect.getCategory() == remove) living.removeEffect(effect);
        }
        for (String key : self ? Phase4State.NEGATIVE : Phase4State.MAGICAL) Phase4State.clear(living, key);
        if (!self) Phase4Service.dispelRegistries(living.getUUID());
        if (self) living.clearFire();
        context.level().sendParticles(ParticleTypes.POOF, living.getX(), living.getY() + 1.0, living.getZ(), 12, 0.4, 0.5, 0.4, 0.05);
      }
    }
  }

  // ------------------------------------------------------------ Drenar Mana
  /** Drenar Mana: tira mana do alvo (jogadores) e a passa para voce. */
  public static class DrainMana implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      float amount = (float) Math.min(100.0, p(context, call, "amount", 20.0));
      float total = 0;
      for (Entity e : targets.entities()) {
        if (!(e instanceof ServerPlayer victim) || e == context.caster()) continue;
        var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(victim);
        float take = Math.min(amount, mana.current());
        mana.set(mana.current() - take);
        total += take;
        context.level().sendParticles(ParticleTypes.ENCHANT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 12, 0.3, 0.5, 0.3, 0.5);
      }
      if (total > 0 && context.caster() instanceof ServerPlayer me) {
        var mine = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(me);
        mine.set(Math.min(mine.max(), mine.current() + total));
      }
    }
  }
}
