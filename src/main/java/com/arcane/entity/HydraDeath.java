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

/** Morte da Hidra: o efeito final de cada glifo original e o buraco negro do Eclipse. Separado de HydraEntity em 25 nov, sem mudar comportamento. */
final class HydraDeath {
  private HydraDeath() {}

  /** O efeito final de cada glifo original. */
  static void deathEffect(HydraEntity h, ServerLevel sl) {
    Vec3 c = h.position().add(0, h.getBbHeight() * 0.5, 0);
    double r = 24 + HydraEntity.scaleFor(h.stage()) * 3;
    var around = sl.getEntitiesOfClass(LivingEntity.class, h.getBoundingBox().inflate(r), e -> e != h && e.isAlive());
    ModNetwork.sendShape(sl, S2CShapeFxPacket.ORIGINAL, c, Vec3.ZERO, (float) r, 0xFFFFFF, Math.max(0, h.variant()), 100);
    switch (h.variant()) {
      case 0 -> around.forEach(e -> { // tempo parado: tudo em volta congela por 10 s
        e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 200, 9));
        e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 200, 250));
      });
      case 1 -> { // retroceder: cada jogador volta a um momento qualquer da luta (posicao, vida, fome, equipamento)
        for (var e : h.snapshots.entrySet()) {
          Player p = sl.getPlayerByUUID(e.getKey());
          if (p == null || e.getValue().isEmpty()) continue;
          Snapshot snap = e.getValue().get(h.rng().nextInt(e.getValue().size()));
          p.teleportTo(snap.pos().x, snap.pos().y, snap.pos().z);
          p.setHealth(Math.max(1f, snap.health()));
          p.getFoodData().setFoodLevel(snap.food());
          var slots = net.minecraft.world.entity.EquipmentSlot.values();
          for (int k = 0; k < slots.length; k++) p.setItemSlot(slots[k], snap.gear().get(k).copy());
          sl.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.4, 0.8, 0.4, 0.1);
        }
      }
      case 2 -> around.forEach(e -> { // colapso do espaco: todos espalhados ao acaso
        double a = h.rng().nextDouble() * Math.PI * 2, d = 8 + h.rng().nextDouble() * 24;
        e.teleportTo(e.getX() + Math.cos(a) * d, e.getY() + 2, e.getZ() + Math.sin(a) * d);
      });
      case 3 -> h.devastate(sl, h.position(), (float) Math.min(48, 8 + HydraEntity.scaleFor(h.stage()) * 2)); // aniquilar: a cratera final
      case 4 -> blackHole(h, sl, c); // eclipse: buraco negro
      case 5 -> { // genese: a regiao floresce e cura quem esta perto
        around.forEach(e -> { if (e instanceof Player) e.setHealth(e.getMaxHealth()); });
        BlockPos o = h.blockPosition();
        for (int k = 0; k < 400; k++) {
          BlockPos q = o.offset(h.rng().nextInt(41) - 20, h.rng().nextInt(9) - 4, h.rng().nextInt(41) - 20);
          if (sl.getBlockState(q).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) && sl.getBlockState(q.above()).isAir()) {
            net.minecraft.world.item.BoneMealItem.growCrop(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE_MEAL), sl, q);
          }
        }
      }
      case 6 -> around.forEach(e -> { // inversao: tudo flutua e depois desce devagar
        e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LEVITATION, 100, 2));
        e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 300, 0));
      });
      default -> { }
    }
    sl.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 3, 1, 1, 1, 0);
    sl.playSound(null, h.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 8f, 0.4f);
  }

  /** Eclipse: um buraco negro puxa tudo em volta e rasga o terreno por 5 s; depois se consome e some. */
  static void blackHole(HydraEntity h, ServerLevel sl, Vec3 c) {
    boolean grief = HydraEntity.griefing(sl);
    com.arcane.execution.Timed.start(sl, 100, now -> {
      for (var e : sl.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(c, c).inflate(40), x -> !(x instanceof HydraEntity))) {
        Vec3 pull = c.subtract(e.position());
        double d = Math.max(1, pull.length());
        e.setDeltaMovement(e.getDeltaMovement().add(pull.normalize().scale(Math.min(0.6, 6 / d))));
        e.hurtMarked = true;
        if (d < 3 && e instanceof LivingEntity l) l.hurt(sl.damageSources().magic(), 4f);
      }
      sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 80, 6, 6, 6, 0.4);
      sl.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 30, 1.5, 1.5, 1.5, 0.02);
      if (grief && now % 20 == 0) {
        float rr = 4 + (now % 100) / 10f;
        h.devastate(sl, c, Math.min(14f, rr));
      }
    }, () -> {
      sl.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
      sl.playSound(null, BlockPos.containing(c), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 5f, 0.6f);
    });
  }
}
