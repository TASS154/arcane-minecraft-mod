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
  private static final Set<ResourceLocation> TRIGGER_CONDITIONS = Set.of(
      new ResourceLocation("arcane", "on_hurt"), new ResourceLocation("arcane", "on_death"), new ResourceLocation("arcane", "on_kill"),
      new ResourceLocation("arcane", "on_fall"), new ResourceLocation("arcane", "on_jump"),
      new ResourceLocation("arcane", "after_hits"),
      new ResourceLocation("arcane", "on_sleep"), new ResourceLocation("arcane", "on_fish"), new ResourceLocation("arcane", "on_dawn"),
      new ResourceLocation("arcane", "on_eat"), new ResourceLocation("arcane", "on_see_cast"));
  /** Gatilhos do dia a dia esperam ate 1 dia de jogo (os de combate, 1 minuto). */
  private static final Set<String> LONG_WAIT = Set.of("on_sleep", "on_fish", "on_dawn", "on_eat", "on_see_cast");

  private record Armed(ExecutionContext context, CompiledExecutionPlan.ExecutionSegment segment, long expiresAt, int[] hits) {}

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
      boolean longWait = segment.condition().map(c -> LONG_WAIT.contains(c.conditionId().getPath())).orElse(false);
      list.add(new Armed(context, segment, context.level().getGameTime() + (longWait ? 24000L : EXPIRE_TICKS), new int[1]));
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
    trigger(event.getEntity(), "hurt", "on_hurt", event.getSource().getEntity());
  }

  /** Ao Morrer: dispara antes da morte ser consumada (a magia pode curar ou teleportar a criatura). */
  @SubscribeEvent
  public static void onDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
    trigger(event.getEntity(), "death", "on_death", event.getSource().getEntity());
  }

  /** Apos N Acertos: conta cada golpe que quem armou da em outra criatura (a magia usa a vitima como "outro"). */
  @SubscribeEvent
  public static void onHitDealt(LivingHurtEvent event) {
    if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != event.getEntity()) {
      trigger(attacker, "hits", "after_hits", event.getEntity());
    }
  }

  /** Ao Matar: quem armou e o autor do golpe fatal (direto ou por magia); o morto vira o "outro" do gatilho. */
  @SubscribeEvent
  public static void onKill(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
    if (event.getSource().getEntity() instanceof LivingEntity killer && killer != event.getEntity()) {
      trigger(killer, "kill", "on_kill", event.getEntity());
    }
  }

  /** Ao Cair: dispara quando quem armou toca o chao depois de cair 3 blocos ou mais (antes do dano da queda). */
  @SubscribeEvent
  public static void onFall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
    if (event.getDistance() >= 3.0f) trigger(event.getEntity(), "fall", "on_fall", null);
  }

  /** Ao Pular. */
  @SubscribeEvent
  public static void onJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
    trigger(event.getEntity(), "jump", "on_jump", null);
  }

  /** Ao Dormir: quando o jogador pega no sono. */
  @SubscribeEvent
  public static void onSleepTick(TickEvent.PlayerTickEvent event) {
    if (event.phase == TickEvent.Phase.END && event.player.isSleeping() && event.player.getSleepTimer() == 2) trigger(event.player, "sleep", "on_sleep", null);
  }

  /** Ao Pescar: ao puxar algo da agua. */
  @SubscribeEvent
  public static void onFish(net.minecraftforge.event.entity.player.ItemFishedEvent event) {
    trigger(event.getEntity(), "fish", "on_fish", event.getHookEntity());
  }

  /** Ao Comer: ao terminar de comer qualquer comida. */
  @SubscribeEvent
  public static void onEat(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
    if (event.getItem().isEdible()) trigger(event.getEntity(), "eat", "on_eat", null);
  }

  private static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, Long> LAST_DAY = new ConcurrentHashMap<>();

  /** Ao Amanhecer: quando o relogio do dia passa pelo nascer do sol (tambem depois de dormir). */
  @SubscribeEvent
  public static void onDawnTick(TickEvent.LevelTickEvent event) {
    if (event.phase != TickEvent.Phase.END || !(event.level instanceof net.minecraft.server.level.ServerLevel level) || level.getGameTime() % 10 != 0) return;
    long day = level.getDayTime() % 24000L;
    Long before = LAST_DAY.put(level.dimension(), day);
    if (before == null || day >= before || ARMED.isEmpty()) return; // passou de 23999 para 0: amanheceu
    for (UUID id : new ArrayList<>(ARMED.keySet())) {
      var e = level.getEntity(id);
      if (e instanceof LivingEntity l) trigger(l, "dawn", "on_dawn", null);
    }
  }

  /** Para os testes: dispara o amanhecer em quem estiver armado nesta dimensao. */
  public static void dawnNow(net.minecraft.server.level.ServerLevel level) {
    for (UUID id : new ArrayList<>(ARMED.keySet())) if (level.getEntity(id) instanceof LivingEntity l) trigger(l, "dawn", "on_dawn", null);
  }

  /** Ao Ver Conjuracao: quem armou e esta a ate 24 blocos, enxergando quem conjura, reage (a magia vista e o "outro"). */
  public static void seeCast(LivingEntity caster) {
    if (ARMED.isEmpty() || !(caster.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
    for (UUID id : new ArrayList<>(ARMED.keySet())) {
      if (id.equals(caster.getUUID())) continue;
      if (!(level.getEntity(id) instanceof LivingEntity watcher) || watcher.distanceTo(caster) > 24 || !watcher.hasLineOfSight(caster)) continue;
      trigger(watcher, "see_cast", "on_see_cast", caster);
    }
  }

  private static void trigger(LivingEntity victim, String type, String conditionPath, net.minecraft.world.entity.Entity source) {
    if (victim.level().isClientSide || FIRING.get()) return;
    List<Armed> list = ARMED.get(victim.getUUID());
    if (list == null || list.isEmpty()) return;

    List<Armed> toFire;
    synchronized (list) {
      long now = victim.level().getGameTime();
      list.removeIf(a -> a.expiresAt() < now);
      toFire = list.stream().filter(a -> a.segment().condition()
          .map(c -> c.conditionId().getPath().equals(conditionPath)).orElse(false))
          // Apos N Acertos: cada acerto conta; so dispara no N-esimo.
          .filter(a -> !conditionPath.equals("after_hits")
              || ++a.hits()[0] >= (int) Math.round(a.segment().condition().get().parameters().getOrDefault("n", 3.0)))
          .toList();
      list.removeAll(toFire);
    }
    FIRING.set(true);
    try {
      for (Armed armed : toFire) {
        ExecutionContext old = armed.context();
        ExecutionContext fresh = new ExecutionContext(old.level(), victim, old.inscription(), old.plan(), old.environment(),
            old.precision(), new ExecutionContext.TriggerEvent(type, Optional.ofNullable(source), Optional.empty()));
        fresh.setGeometryBonus(old.geometryBonus());
        fresh.setAreaScale(old.areaScale());
        fresh.setRitual(old.ritualRadius(), old.ritualCenter());
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
