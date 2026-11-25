package com.arcane.entity;

import com.arcane.config.ArcaneConfig;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.S2CShapeFxPacket;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static com.arcane.entity.HydraEntity.*;

/** Poderes da Hidra: a escolha e o uso de cada poder, o sopro, a magia original e quem esta ao alcance. Separado de HydraEntity em 25 nov, sem mudar comportamento. */
final class HydraPowers {
  private HydraPowers() {}

  /** Usa um poder. {@code target} pode ser nulo (o rugido de quando desperta). */
  static void use(HydraEntity h, int choice, LivingEntity target) {
    if (!(h.level() instanceof ServerLevel sl)) return;
    h.data().set(POWER, choice);
    int s = h.stage();
    double reach = HydraEntity.reach(s);
    double dmg = h.getAttributeValue(Attributes.ATTACK_DAMAGE);
    switch (choice) {
      case BREATH -> {
        h.powerTicks = 30;
        sl.playSound(null, h.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 1.6f - s * 0.06f);
      }
      case SWEEP -> {
        h.powerTicks = 10;
        double r = HydraEntity.sweepRadius(s);
        for (LivingEntity e : enemiesWithin(h, r)) {
          e.hurt(h.damageSources().mobAttack(h), (float) (dmg * 0.9));
          Vec3 push = e.position().subtract(h.position()).normalize().scale(1.2 + reach * 0.3);
          e.push(push.x, 0.5 + reach * 0.1, push.z);
          e.hurtMarked = true;
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, h.getX(), h.getY() + 0.5, h.getZ(), 12, r * 0.4, 0.2, r * 0.4, 0);
        sl.sendParticles(ParticleTypes.EXPLOSION, h.getX(), h.getY() + 0.3, h.getZ(), 6, r * 0.3, 0.1, r * 0.3, 0);
        sl.playSound(null, h.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5f, 0.6f);
      }
      case ROAR -> {
        h.powerTicks = 30;
        double r = Math.min(64, 20 * Math.sqrt(reach));
        for (LivingEntity e : enemiesWithin(h, r)) {
          e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1 + s / 6), h);
          e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, s / 5), h);
          e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0), h);
        }
        sl.sendParticles(ParticleTypes.SONIC_BOOM, h.getX(), h.getEyeY(), h.getZ(), 1, 0, 0, 0, 0);
        ModNetwork.sendShape(sl, S2CShapeFxPacket.ORIGINAL, h.position(), Vec3.ZERO, (float) r, 0xB040FF, S2CShapeFxPacket.FX_ROAR, 24);
        sl.playSound(null, h.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0f, 0.5f);
      }
      case STORM -> {
        h.powerTicks = 20;
        int bolts = 1 + (int) (reach * 1.2);
        List<LivingEntity> foes = enemiesWithin(h, 40 * Math.sqrt(reach));
        for (int i = 0; i < bolts; i++) {
          Vec3 at = !foes.isEmpty() && h.rng().nextInt(3) > 0 ? foes.get(h.rng().nextInt(foes.size())).position()
              : h.position().add(h.rng().nextGaussian() * 10 * reach, 0, h.rng().nextGaussian() * 10 * reach);
          var bolt = EntityType.LIGHTNING_BOLT.create(sl);
          if (bolt == null) continue;
          bolt.moveTo(at.x, at.y, at.z);
          bolt.setVisualOnly(false);
          sl.addFreshEntity(bolt);
        }
      }
      case METEOR -> {
        h.powerTicks = 20;
        int rocks = 2 + (int) reach;
        int power = HydraEntity.griefing(sl) ? 1 + (s - 1) / 4 : 0;
        Vec3 aim = target != null ? target.position() : h.position();
        for (int i = 0; i < rocks; i++) {
          Vec3 at = aim.add(h.rng().nextGaussian() * 6 * Math.sqrt(reach), 0, h.rng().nextGaussian() * 6 * Math.sqrt(reach));
          LargeFireball ball = new LargeFireball(sl, h, 0, -1.6, 0, power);
          ball.setPos(at.x, at.y + 22 + h.rng().nextInt(8), at.z);
          sl.addFreshEntity(ball);
        }
        sl.playSound(null, h.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0f, 0.4f);
      }
      case HEAL -> {
        h.powerTicks = 30;
        h.heal(h.getMaxHealth() * 0.08f);
        sl.sendParticles(ParticleTypes.HEART, h.getX(), h.getY() + h.getBbHeight(), h.getZ(), 20, h.getBbWidth() * 0.4, 1, h.getBbWidth() * 0.4, 0);
        sl.playSound(null, h.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 4f, 0.5f);
      }
      case ORIGINAL -> castOriginal(h, sl, target);
      case DEVASTATE -> {
        // aviso: um circulo vermelho em volta de cada alvo; 1,5 s depois o chao explode e some
        h.powerTicks = 16;
        Vec3 aim = target != null ? target.position() : h.position();
        int n = cratersPerStrike(s);
        float r = devastationRadius(s);
        for (int i = 0; i < n; i++) {
          Vec3 at = i == 0 ? aim : aim.add(h.rng().nextGaussian() * r * 1.4, 0, h.rng().nextGaussian() * r * 1.4);
          h.announceStrike(sl, at, r, STRIKE_WARNING + i * 6);
        }
        sl.playSound(null, h.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 4.0f, 0.5f);
      }
      case CATACLYSM -> {
        // O fim do mundo em miniatura: onda de choque enorme, cura com o dano causado e um anel de fogo.
        h.powerTicks = 40;
        double r = Math.min(48, 20 * Math.sqrt(reach));
        float healed = 0;
        for (LivingEntity e : enemiesWithin(h, r)) {
          float before = e.getHealth();
          e.hurt(h.damageSources().indirectMagic(h, h), (float) (dmg * 1.6));
          healed += Math.max(0, before - e.getHealth());
          Vec3 push = e.position().subtract(h.position()).normalize().scale(2.5);
          e.push(push.x, 1.2, push.z);
          e.hurtMarked = true;
          e.setSecondsOnFire(8);
        }
        h.heal(healed * 0.5f);
        for (int i = 0; i < 72; i++) {
          double a = Math.PI * 2 * i / 72;
          sl.sendParticles(ParticleTypes.FLAME, h.getX() + Math.cos(a) * 10, h.getY() + 0.3, h.getZ() + Math.sin(a) * 10, 2, 0.2, 0.1, 0.2, 0.05);
          sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, h.getX() + Math.cos(a) * 18, h.getY() + 0.3, h.getZ() + Math.sin(a) * 18, 1, 0.2, 0.1, 0.2, 0.05);
        }
        ModNetwork.sendShape(sl, S2CShapeFxPacket.ORIGINAL, h.position(), Vec3.ZERO, (float) r, 0xFF5020, S2CShapeFxPacket.FX_DEVASTATE, 40);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, h.getX(), h.getY() + 1, h.getZ(), 3, 3, 1, 3, 0);
        sl.playSound(null, h.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0f, 0.6f);
      }
      default -> h.data().set(POWER, 0);
    }
  }

  /** Sopro: um cone de fogo (estagio 8+ vira sopro do dragao, que tambem seca) de cada cabeca na direcao do olhar. */
  static void breathTick(HydraEntity h) {
    if (!(h.level() instanceof ServerLevel sl)) return;
    int s = h.stage();
    LivingEntity target = h.getTarget();
    if (target != null) h.getLookControl().setLookAt(target, 30f, 30f);
    Vec3 look = target != null ? target.getEyePosition().subtract(h.getEyePosition()).normalize() : h.getLookAngle();
    double range = Math.min(32, 6 * Math.pow(HydraEntity.reach(s), 0.7) + s * 0.4);
    Vec3 mouth = h.getEyePosition().add(look.scale(h.getBbWidth() * 0.6));
    var particle = s >= 8 ? ParticleTypes.DRAGON_BREATH : ParticleTypes.FLAME;
    for (int i = 0; i < 4 + h.heads(); i++) {
      double f = 0.6 + h.rng().nextDouble() * 0.6;
      Vec3 v = look.add(h.rng().nextGaussian() * 0.12, h.rng().nextGaussian() * 0.12, h.rng().nextGaussian() * 0.12).scale(f);
      sl.sendParticles(particle, mouth.x, mouth.y, mouth.z, 0, v.x, v.y, v.z, 1.0);
    }
    if (h.powerTicks % 5 != 0) return;
    double dmg = h.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.35 * Math.max(1, h.heads() / 2.0);
    for (LivingEntity e : enemiesWithin(h, range)) {
      Vec3 to = e.getEyePosition().subtract(mouth);
      if (to.length() > range || to.normalize().dot(look) < 0.82) continue;
      e.hurt(h.damageSources().indirectMagic(h, h), (float) dmg);
      e.setSecondsOnFire(4);
      if (s >= 8) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, s >= 12 ? 1 : 0), h);
    }
  }

  static List<LivingEntity> enemiesWithin(HydraEntity h, double r) {
    return enemiesNear(h, h.position(), r);
  }

  static List<LivingEntity> enemiesNear(HydraEntity h, Vec3 c, double r) {
    return h.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
        e -> e != h && e.isAlive() && !(e instanceof HydraEntity) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
            && e.distanceToSqr(c) <= r * r);
  }

  /**
   * A magia original da Hidra (o glifo que ela guarda), a cada 3 minutos: metade das cabecas ergue e a magia cai na
   * regiao. Depois dela, 5 s de fraqueza (dano dobrado, sem mordidas).
   */
  static void castOriginal(HydraEntity h, ServerLevel sl, LivingEntity target) {
    h.powerTicks = 40;
    h.originalCooldown = ORIGINAL_COOLDOWN;
    h.markWeak(40 + 100);
    double r = 32 + HydraEntity.scaleFor(h.stage()) * 2;
    var players = sl.getEntitiesOfClass(Player.class, h.getBoundingBox().inflate(r), p -> !p.isCreative() && !p.isSpectator());
    ModNetwork.sendShape(sl, S2CShapeFxPacket.ORIGINAL, h.position(), Vec3.ZERO, (float) r, 0xFFFFFF, Math.max(0, h.variant()), 80);
    switch (h.variant()) {
      case 0 -> players.forEach(p -> { // Parar o Tempo: todos quase parados
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 6));
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 100, 3));
      });
      case 1 -> { // Retroceder: a Hidra volta a vida que tinha ha 20 s
        Float past = h.healthHistory.peekFirst();
        if (past != null && past > h.getHealth()) h.setHealth(past);
      }
      case 2 -> players.forEach(p -> { // Colapso do Espaco: todos sugados para perto dela
        Vec3 to = h.position().add(p.position().subtract(h.position()).normalize().scale(h.getBbWidth() + 3));
        p.teleportTo(to.x, to.y, to.z);
        p.hurt(h.damageSources().magic(), 6f);
      });
      case 3 -> { // Aniquilar: cratera gigante no alvo
        if (target != null) h.devastate(sl, target.position(), devastationRadius(h.stage()) * 1.5f + 6);
      }
      case 4 -> players.forEach(p -> { // Eclipse: escuridao e definhar
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 200, 0));
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 200, 1));
      });
      case 5 -> { // Genese: a Hidra se regenera
        h.heal(h.getMaxHealth() * 0.2f);
        h.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200, 1));
      }
      case 6 -> players.forEach(p -> // Inversao da Gravidade: todos jogados para cima
          p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LEVITATION, 60, 3)));
      default -> { }
    }
    sl.playSound(null, h.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 6f, 0.4f);
  }
}
