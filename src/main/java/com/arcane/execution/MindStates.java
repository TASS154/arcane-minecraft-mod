package com.arcane.execution;

import com.arcane.ArcaneMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Estados mentais temporarios (Medo e Sono), guardados na propria criatura. Sem custo por tick: so os eventos de
 * escolha de alvo e de dano os consultam.
 *  - Medo: a criatura nao escolhe quem a assustou como alvo ate o medo passar.
 *  - Sono: a criatura nao escolhe ninguem como alvo; qualquer dano a acorda.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class MindStates {
  public static final String FEAR_KEY = "arcane_fear_until", SLEEP_KEY = "arcane_sleep_until", DISTRACT_KEY = "arcane_distract_until";
  private static final String FEAR = FEAR_KEY, FEAR_OF = "arcane_fear_of", SLEEP = SLEEP_KEY, DISTRACT_OF = "arcane_distract_of";

  private MindStates() {}

  public static void scare(LivingEntity victim, LivingEntity source, int ticks) {
    victim.getPersistentData().putLong(FEAR, victim.level().getGameTime() + ticks);
    victim.getPersistentData().putUUID(FEAR_OF, source.getUUID());
  }

  public static boolean isScaredOf(LivingEntity victim, LivingEntity other) {
    var data = victim.getPersistentData();
    return data.getLong(FEAR) > victim.level().getGameTime() && data.hasUUID(FEAR_OF) && data.getUUID(FEAR_OF).equals(other.getUUID());
  }

  public static boolean isScared(LivingEntity victim) {
    return victim.getPersistentData().getLong(FEAR) > victim.level().getGameTime();
  }

  public static void sleep(LivingEntity victim, int ticks) {
    victim.getPersistentData().putLong(SLEEP, victim.level().getGameTime() + ticks);
  }

  /** Chamariz: ate passar, a criatura nao escolhe quem a distraiu como alvo. */
  public static void distract(LivingEntity victim, LivingEntity source, int ticks) {
    victim.getPersistentData().putLong(DISTRACT_KEY, victim.level().getGameTime() + ticks);
    victim.getPersistentData().putUUID(DISTRACT_OF, source.getUUID());
  }

  public static boolean isDistractedFrom(LivingEntity victim, LivingEntity other) {
    var data = victim.getPersistentData();
    return data.getLong(DISTRACT_KEY) > victim.level().getGameTime() && data.hasUUID(DISTRACT_OF) && data.getUUID(DISTRACT_OF).equals(other.getUUID());
  }

  /** Tira Medo e Sono (Provocar faz isso). */
  public static void clear(LivingEntity victim) {
    victim.getPersistentData().remove(FEAR);
    victim.getPersistentData().remove(SLEEP);
  }

  public static boolean isAsleep(LivingEntity victim) {
    return victim.getPersistentData().getLong(SLEEP) > victim.level().getGameTime();
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) return;
    LivingEntity next = event.getNewTarget();
    if (next == null) return;
    if (isAsleep(mob) || isScaredOf(mob, next) || isDistractedFrom(mob, next) || Phase4State.active(mob, Phase4State.PETRIFY)
        || Phase4Service.sameTeam(mob, next)) event.setCanceled(true);
  }

  @SubscribeEvent
  public static void onHurt(LivingHurtEvent event) {
    LivingEntity victim = event.getEntity();
    if (victim.level().isClientSide || !isAsleep(victim)) return;
    victim.getPersistentData().remove(SLEEP);
    // Acorda: tira a letargia que o glifo aplicou.
    victim.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
    victim.removeEffect(net.minecraft.world.effect.MobEffects.JUMP);
    victim.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
  }
}
