package com.arcane.execution;

import com.arcane.ArcaneMod;
import com.arcane.api.form.FormHandler;
import com.arcane.glyph.GlyphHandlerRegistry;
import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.registry.ModParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Magias reativas (design: "Self -> Ao sofrer dano -> Teleporte"). Um trecho cuja condicao e de gatilho
 * (hoje: Ao Sofrer Dano) nao roda ao conjurar: fica ARMADO no conjurador e dispara uma vez quando o evento acontece,
 * ou expira. Maximo de 3 armadas por criatura; a mana foi paga ao armar.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class ArmedSpells {
  public static final long EXPIRE_TICKS = 1200L;
  public static final int MAX_ARMED = 3;
  private static final Set<ResourceLocation> TRIGGER_CONDITIONS = Set.of(new ResourceLocation("arcane", "on_hurt"));

  private record Armed(ExecutionContext context, CompiledExecutionPlan.ExecutionSegment segment, long expiresAt) {}

  private static final Map<UUID, List<Armed>> ARMED = new ConcurrentHashMap<>();
  private static final ThreadLocal<Boolean> FIRING = ThreadLocal.withInitial(() -> false);

  private ArmedSpells() {}

  public static boolean isTriggerSegment(CompiledExecutionPlan.ExecutionSegment segment) {
    return segment.condition().map(c -> TRIGGER_CONDITIONS.contains(c.conditionId())).orElse(false);
  }

  public static void arm(ExecutionContext context, CompiledExecutionPlan.ExecutionSegment segment) {
    List<Armed> list = ARMED.computeIfAbsent(context.caster().getUUID(), id -> new ArrayList<>());
    synchronized (list) {
      while (list.size() >= MAX_ARMED) list.remove(0);
      list.add(new Armed(context, segment, context.level().getGameTime() + EXPIRE_TICKS));
    }
    if (context.caster() instanceof Player player) {
      player.displayClientMessage(Component.translatable("message.arcane.armed"), true);
    }
  }

  public static int armedCount(UUID entity) {
    List<Armed> list = ARMED.get(entity);
    return list == null ? 0 : list.size();
  }

  public static void clear(UUID entity) {
    ARMED.remove(entity);
  }

  @SubscribeEvent
  public static void onHurt(LivingHurtEvent event) {
    LivingEntity victim = event.getEntity();
    if (victim.level().isClientSide || FIRING.get()) return;
    List<Armed> list = ARMED.get(victim.getUUID());
    if (list == null || list.isEmpty()) return;

    List<Armed> toFire;
    synchronized (list) {
      long now = victim.level().getGameTime();
      list.removeIf(a -> a.expiresAt() < now);
      toFire = new ArrayList<>(list);
      list.clear();
    }
    FIRING.set(true);
    try {
      for (Armed armed : toFire) {
        ExecutionContext old = armed.context();
        ExecutionContext fresh = new ExecutionContext(old.level(), victim, old.inscription(), old.plan(), old.environment(),
            old.precision(), new ExecutionContext.TriggerEvent("hurt", Optional.ofNullable(event.getSource().getEntity()), Optional.empty()));
        fresh.setGeometryBonus(old.geometryBonus());
        fresh.setAreaScale(old.areaScale());
        fresh.setRitualLevel(old.ritualLevel());
        fire(fresh, armed.segment());
        old.level().sendParticles(ModParticles.ARCANE_SPARK.get(), victim.getX(), victim.getY() + 1.0, victim.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
      }
    } finally {
      FIRING.set(false);
    }
  }

  /** Entrega de novo a forma da magia (a partir do estado atual do conjurador) e roda so o trecho armado. */
  private static void fire(ExecutionContext context, CompiledExecutionPlan.ExecutionSegment segment) {
    DeliverySpec spec = DeliverySpec.fromPlan(context.plan().delivery());
    FormHandler form = GlyphHandlerRegistry.getForm(spec.handler()).orElse(null);
    if (form == null) return;
    DeliveryResult delivery = form.deliver(context, spec);
    context.setResolvedTargets(delivery.targets());
    SpellPipeline.INSTANCE.executeSegment(context, segment);
  }

  @SubscribeEvent
  public static void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 100 != 0) return;
    ARMED.entrySet().removeIf(e -> {
      List<Armed> list = e.getValue();
      synchronized (list) {
        list.removeIf(a -> a.context().level().getGameTime() > a.expiresAt() || !a.context().caster().isAlive());
        return list.isEmpty();
      }
    });
  }
}
