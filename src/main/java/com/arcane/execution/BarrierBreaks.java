package com.arcane.execution;

import com.arcane.ArcaneMod;
import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Condicao "Ao Quebrar a Barreira": o trecho fica esperando ate que alguem quebre (a mao ou com ferramenta; explosoes e magia nao quebram barreira) um dos blocos
 * de barreira criados por esta conjuracao. Roda uma vez, no ponto da quebra, com as criaturas a ate 3 blocos dali
 * como alvos (Barreira, Ao Quebrar a Barreira, Forasteiros, Marcar: marca quem arrombou). A barreira sumir sozinha
 * (Duracao) ou ao soltar o Segurar nao conta. Usa o barramento {@link SpellEvents}; a espera vence em 15 minutos.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class BarrierBreaks {
  public static final String CONDITION = "on_barrier_break";
  public static final double REACH = 3.0;
  private static final long EXPIRE_TICKS = 20L * 60 * 15;

  private record Key(ResourceKey<Level> dim, long pos) {}
  private record Watch(String event, long expiresAt) {}

  private static final Map<Key, Watch> WATCHED = new HashMap<>();

  private BarrierBreaks() {}

  public static boolean isBreakSegment(CompiledExecutionPlan.ExecutionSegment segment) {
    return segment.condition().map(c -> c.conditionId().getPath().equals(CONDITION)).orElse(false);
  }

  /** Liga o trecho aos blocos de barreira que esta conjuracao acabou de criar. Sem barreira, nunca roda. */
  public static void attach(ExecutionContext context, CompiledExecutionPlan.ExecutionSegment segment) {
    var barrier = ModBlocks.ARCANE_BARRIER.get();
    List<BlockPos> mine = new ArrayList<>();
    for (var b : context.resolvedTargets().blocks()) if (b.state().is(barrier)) mine.add(b.pos());
    if (mine.isEmpty()) return;
    String event = "barrier_break:" + UUID.randomUUID();
    long expires = context.level().getGameTime() + EXPIRE_TICKS;
    prune(context.level().getGameTime());
    for (BlockPos p : mine) WATCHED.put(new Key(context.level().dimension(), p.asLong()), new Watch(event, expires));
    SpellEvents.once(event, ev -> {
      WATCHED.values().removeIf(w -> w.event().equals(event));
      if (context.caster() != null && !context.caster().isRemoved()) {
        ExecutionContext c = new ExecutionContext(ev.level(), context.caster(), context.inscription(), context.plan(), context.environment(),
            context.precision(), new ExecutionContext.TriggerEvent("barrier_break", Optional.empty(), Optional.empty()));
        c.setGeometryBonus(context.geometryBonus());
        c.setAreaScale(context.areaScale());
        List<Entity> near = ev.level().getEntities((Entity) null, new AABB(ev.pos(), ev.pos()).inflate(REACH),
            e -> e instanceof net.minecraft.world.entity.LivingEntity && e.isAlive() && e.position().distanceTo(ev.pos()) <= REACH + 1);
        c.setResolvedTargets(new TargetList(near, List.of(), List.of(ev.pos())));
        SpellPipeline.INSTANCE.executeSegment(c, segment);
      }
    });
  }

  private static void prune(long now) {
    if (WATCHED.size() < 4096) return;
    WATCHED.values().removeIf(w -> w.expiresAt() < now);
  }

  /** Um bloco vigiado quebrou: dispara o evento da conjuracao dona dele. */
  public static void broken(ServerLevel level, BlockPos pos) {
    Watch w = WATCHED.remove(new Key(level.dimension(), pos.asLong()));
    if (w == null) return;
    if (w.expiresAt() < level.getGameTime()) {
      SpellEvents.drop(w.event());
      return;
    }
    SpellEvents.emit(new SpellEvents.Event(w.event(), level, Vec3.atCenterOf(pos)));
  }

  public static int watched() { return WATCHED.size(); }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void onBreak(BlockEvent.BreakEvent event) {
    if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
    if (event.getState().is(ModBlocks.ARCANE_BARRIER.get())) broken(level, event.getPos());
  }

  @SubscribeEvent
  public static void onStopped(ServerStoppedEvent event) { WATCHED.clear(); }
}
