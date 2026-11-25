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

/** Fases da luta: a vida e dividida entre as cabecas; cada limiar arranca uma, e abaixo de 25% vem a furia. Separado de HydraEntity em 25 nov, sem mudar comportamento. */
final class HydraPhases {
  private HydraPhases() {}

  /**
   * Fases: a vida e dividida em tantas partes quantas cabecas; cada vez que cruza um limiar, uma cabeca AO ACASO cai
   * (a ultima fica ate a morte). Cada cabeca perdida deixa a Hidra mais forte e rapida. Abaixo de 25%: furia.
   */
  static void checkPhases(HydraEntity h) {
    if (!(h.level() instanceof ServerLevel sl)) return;
    int n = h.heads();
    float frac = h.getHealth() / h.getMaxHealth();
    int shouldHave = Math.max(1, (int) Math.ceil(frac * n));
    while (h.aliveHeads() > shouldHave) {
      java.util.List<Integer> alive = new java.util.ArrayList<>();
      for (int i = 0; i < n; i++) if (h.headAlive(i)) alive.add(i);
      int lost = alive.get(h.rng().nextInt(alive.size()));
      h.data().set(ALIVE, h.data().get(ALIVE) & ~(1 << lost));
      Vec3 at = h.headPos(lost, h.position().add(0, h.getBbHeight(), 0));
      sl.sendParticles(ParticleTypes.ENCHANT, at.x, at.y, at.z, 120, 1.5, 1.5, 1.5, 1.2);
      sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
      sl.playSound(null, BlockPos.containing(at), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 4f, 0.5f);
      sl.playSound(null, h.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6f, 0.4f);
      int gone = n - h.aliveHeads();
      HydraEntity.setModifier(h.getAttribute(Attributes.ATTACK_DAMAGE), ENRAGE_ID, 0.12 * gone);
      h.powerCooldown = Math.min(h.powerCooldown, 20);
    }
    boolean berserk = frac < 0.25f;
    if (berserk != h.flag(F_BERSERK)) {
      h.setFlag(F_BERSERK, berserk);
      if (berserk) sl.playSound(null, h.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 5f, 0.6f);
    }
  }
}
