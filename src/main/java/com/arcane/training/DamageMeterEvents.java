package com.arcane.training;

import com.arcane.ArcaneMod;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Liga o {@link DamageMeter} ao jogo: o dano medido e o que sobra depois de armadura, encantamentos e afinidades
 * ({@code LivingDamageEvent}, prioridade mais baixa: depois de qualquer outro ajuste).
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class DamageMeterEvents {
  private DamageMeterEvents() {}

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void onDamage(LivingDamageEvent event) {
    if (event.isCanceled() || event.getEntity().level().isClientSide) return;
    DamageMeter meter = DamageMeter.of(event.getEntity());
    if (meter == null) return;
    var source = event.getSource();
    meter.record(event.getEntity().level().getGameTime(), event.getAmount(), source.getMsgId());
    if (source.getEntity() != null) {
      int cost = DamageMeter.lastCastCost(source.getEntity().getUUID());
      if (cost >= 0) meter.noteManaCost(cost);
    }
  }

  /** Reacao em massa (Fase K): os Bonecos no raio anotam (aparece em /arcane dummy report). */
  public static void massReaction(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 at, String id) {
    for (var d : level.getEntitiesOfClass(com.arcane.entity.ArcaneDummyEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(8))) {
      reaction(d, id);
    }
  }

  /** Reacao elemental disparada em {@code target} (chamado pelas interacoes elementais). */
  public static void reaction(Object target, String id) {
    DamageMeter meter = DamageMeter.of(target);
    if (meter != null) meter.noteReaction(id);
  }
}
