package com.arcane.execution;

import com.arcane.ArcaneMod;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.S2CVeilPacket;
import com.arcane.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Invisibilidade arcana (glifo Invisibilidade): alem do efeito vanilla, um marcador (Veu Arcano) que os clientes
 * recebem por pacote. So quem tem o marcador some com armadura e itens e vira a distorcao turva; a pocao vanilla
 * continua como sempre foi. Tirar a invisibilidade (Revelar, leite) tira o veu junto.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class ArcaneVeil {
  private ArcaneVeil() {}

  private static void broadcast(LivingEntity e, boolean on) {
    if (e.level().isClientSide) return;
    ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e), new S2CVeilPacket(e.getId(), on));
  }

  @SubscribeEvent
  public static void onAdded(MobEffectEvent.Added event) {
    if (event.getEffectInstance().getEffect() == ModEffects.ARCANE_VEIL.get()) broadcast(event.getEntity(), true);
  }

  @SubscribeEvent
  public static void onRemoved(MobEffectEvent.Remove event) {
    if (event.getEffect() == ModEffects.ARCANE_VEIL.get()) broadcast(event.getEntity(), false);
    if (event.getEffect() == MobEffects.INVISIBILITY && event.getEntity().hasEffect(ModEffects.ARCANE_VEIL.get())) {
      event.getEntity().removeEffect(ModEffects.ARCANE_VEIL.get());
    }
  }

  @SubscribeEvent
  public static void onExpired(MobEffectEvent.Expired event) {
    if (event.getEffectInstance() != null && event.getEffectInstance().getEffect() == ModEffects.ARCANE_VEIL.get()) broadcast(event.getEntity(), false);
  }

  /** Quem passa a ver a criatura recebe o estado do veu. */
  @SubscribeEvent
  public static void onStartTracking(PlayerEvent.StartTracking event) {
    if (event.getTarget() instanceof LivingEntity e && e.hasEffect(ModEffects.ARCANE_VEIL.get()) && event.getEntity() instanceof ServerPlayer p) {
      ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new S2CVeilPacket(e.getId(), true));
    }
  }
}
