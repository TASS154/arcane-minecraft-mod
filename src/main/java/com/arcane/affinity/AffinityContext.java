package com.arcane.affinity;

import com.arcane.ArcaneMod;
import com.arcane.config.ArcaneConfig;
import com.arcane.execution.SpellElement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Elemento do efeito que esta rodando agora (o pipeline empilha um por efeito) e o multiplicador de reacao que as
 * interacoes ja deram a cada alvo nesse efeito. O dano que acontece DENTRO do efeito passa por {@link #onHurt}, que
 * aplica a afinidade uma unica vez por golpe, com o teto da afinidade acumulada com reacoes.
 *
 * <p>Dano que acontece depois (fogo queimando, veneno) nao tem efeito na pilha e nao muda.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class AffinityContext {
  private record Frame(SpellElement element, Map<Entity, Double> reactions) {}

  private static final Deque<Frame> STACK = new ArrayDeque<>();

  private AffinityContext() {}

  public static void push(SpellElement element) { STACK.push(new Frame(element, new IdentityHashMap<>())); }

  public static void pop() { if (!STACK.isEmpty()) STACK.pop(); }

  public static SpellElement current() { return STACK.isEmpty() ? null : STACK.peek().element(); }

  /** Multiplicador das reacoes (InteractionRules) dado a {@code target} no efeito atual. */
  public static void noteReaction(Entity target, double multiplier) {
    if (!STACK.isEmpty()) STACK.peek().reactions().put(target, multiplier);
  }

  static double reactionOf(Entity target) {
    if (STACK.isEmpty()) return 1.0;
    return STACK.peek().reactions().getOrDefault(target, 1.0);
  }

  static boolean enabled() {
    try {
      return ArcaneConfig.AFFINITIES_ENABLED.get();
    } catch (IllegalStateException e) {
      return true; // config ainda nao carregada (testes)
    }
  }

  static double cap() {
    try {
      return ArcaneConfig.AFFINITY_MAX_TOTAL.get();
    } catch (IllegalStateException e) {
      return 2.0;
    }
  }

  /** Multiplicador final de afinidade (ja com o teto) de {@code target} contra o efeito atual; 1 sem efeito na pilha. */
  public static double finalMultiplier(Entity target) {
    SpellElement e = current();
    if (e == null || !enabled()) return 1.0;
    double a = Affinities.multiplier(target, e);
    return Affinities.capped(a, reactionOf(target), cap());
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onHurt(LivingHurtEvent event) {
    var target = event.getEntity();
    if (target.level().isClientSide || current() == null) return;
    double m = finalMultiplier(target);
    if (m == 1.0) return;
    SpellElement element = current();
    if (m <= 0.0) {
      event.setCanceled(true);
    } else {
      event.setAmount((float) (event.getAmount() * m));
    }
    if (target.level() instanceof ServerLevel level) com.arcane.fx.Fx.affinity(level, target, element, m);
  }
}
