package com.arcane.execution;

import com.arcane.ArcaneMod;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Imunidade do conjurador as proprias magias de area: o que ele solta (explosao, relampago, nuvem de efeito nocivo)
 * nao o fere. Efeitos nocivos de formas de area (Aura, Barreira, Pilar...) ja o poupam no pipeline
 * ({@link EffectReach#harmful}); a forma Eu continua valendo para quem quer se ferir de proposito.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class SpellImmunity {
  public static final String CLOUD_TAG = "arcane_cloud";
  public static final String BOLT_TAG = "arcane_caster";

  /** Conjurador da explosao em andamento (definido so durante a chamada de level.explode das nossas magias). */
  private static final ThreadLocal<LivingEntity> EXPLOSION_CASTER = new ThreadLocal<>();

  private SpellImmunity() {}

  /** Se a explosao em andamento destroi blocos (ExplosionInteraction diferente de NONE). */
  private static final ThreadLocal<Boolean> EXPLOSION_BREAKS = new ThreadLocal<>();

  public static void explodeAs(LivingEntity caster, Runnable explosion) {
    explodeAs(caster, net.minecraft.world.level.Level.ExplosionInteraction.NONE, explosion);
  }

  public static void explodeAs(LivingEntity caster, net.minecraft.world.level.Level.ExplosionInteraction interaction, Runnable explosion) {
    EXPLOSION_CASTER.set(caster);
    EXPLOSION_BREAKS.set(interaction != net.minecraft.world.level.Level.ExplosionInteraction.NONE);
    try {
      explosion.run();
    } finally {
      EXPLOSION_CASTER.remove();
      EXPLOSION_BREAKS.remove();
    }
  }

  @SubscribeEvent
  public static void onDetonate(ExplosionEvent.Detonate event) {
    LivingEntity caster = EXPLOSION_CASTER.get();
    if (caster != null) event.getAffectedEntities().remove(caster);
    // Explosoes de magia que destroem blocos: por padrao nao soltam item e entram no diario do Restaurar.
    // Com blastDropsLoot ligado seguem o caminho do vanilla (com loot) e ficam fora do diario.
    if (caster != null && Boolean.TRUE.equals(EXPLOSION_BREAKS.get()) && !com.arcane.restore.BlockJournal.dropsLoot()
        && event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
      for (net.minecraft.core.BlockPos pos : new java.util.ArrayList<>(event.getAffectedBlocks())) {
        var before = level.getBlockState(pos);
        if (before.isAir()) continue;
        level.removeBlock(pos, false);
        com.arcane.restore.BlockJournal.record(level, pos, before);
      }
      event.getAffectedBlocks().clear();
    }
  }

  @SubscribeEvent
  public static void onLightning(EntityStruckByLightningEvent event) {
    LightningBolt bolt = event.getLightning();
    if (bolt.getPersistentData().hasUUID(BOLT_TAG) && bolt.getPersistentData().getUUID(BOLT_TAG).equals(event.getEntity().getUUID())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onEffectAdded(MobEffectEvent.Added event) {
    if (event.getEffectSource() instanceof AreaEffectCloud cloud && cloud.getPersistentData().getBoolean(CLOUD_TAG)
        && cloud.getOwner() != null && cloud.getOwner().getUUID().equals(event.getEntity().getUUID())
        && event.getEffectInstance().getEffect().getCategory() == MobEffectCategory.HARMFUL) {
      final LivingEntity entity = event.getEntity();
      final net.minecraft.world.effect.MobEffect effect = event.getEffectInstance().getEffect();
      // O efeito so entra no mapa depois do evento: remove logo em seguida.
      if (entity.level().getServer() != null) { var srv = entity.level().getServer(); srv.tell(new net.minecraft.server.TickTask(srv.getTickCount() + 1, () -> entity.removeEffect(effect))); }
    }
  }
}
