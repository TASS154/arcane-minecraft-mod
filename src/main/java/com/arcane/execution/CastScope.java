package com.arcane.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * O efeito que esta rodando agora (o pipeline empilha um por efeito, Fase K): quem conjurou, o elemento, os alvos e as
 * reacoes que ja aconteceram nele. Serve ao Diario de descobertas (quem causou a reacao) e as reacoes em massa (o
 * limiar e contado por efeito de uma conjuracao). No fim do efeito ({@link #pop}), avalia as reacoes em massa.
 */
public final class CastScope {
  public static final class Frame {
    public final ExecutionContext context;
    public final SpellElement element;
    public final TargetList targets;
    public final Map<String, Integer> reactions = new HashMap<>();
    /** Reacao x alvo ja contada (a mesma regra passa pelo dano e pelo depois-do-acerto). */
    final java.util.Set<String> seen = new java.util.HashSet<>();

    Frame(ExecutionContext context, SpellElement element, TargetList targets) {
      this.context = context;
      this.element = element;
      this.targets = targets;
    }
  }

  private static final Deque<Frame> STACK = new ArrayDeque<>();

  private CastScope() {}

  public static void push(ExecutionContext context, SpellElement element, TargetList targets) {
    STACK.push(new Frame(context, element, targets));
  }

  public static Frame current() { return STACK.peek(); }

  /** Fecha o efeito: avalia as reacoes em massa com o que aconteceu nele. */
  public static void pop() {
    Frame f = STACK.poll();
    if (f != null) {
      try {
        evaluate(f);
      } catch (RuntimeException e) {
        com.mojang.logging.LogUtils.getLogger().warn("Arcane: reacao em massa falhou", e);
      }
    }
  }

  /**
   * Uma reacao aconteceu em um alvo dentro do efeito atual: conta para as reacoes em massa e entra no Diario de quem
   * conjurou (so jogadores; uma reacao fora de conjuracao, ou de outra criatura, nao conta).
   */
  public static void noteReaction(ServerLevel level, String id, String nameKey, net.minecraft.world.entity.Entity target) {
    Frame f = STACK.peek();
    if (f == null) return;
    if (!f.seen.add(id + "@" + (target == null ? "-" : target.getUUID()))) return;
    f.reactions.merge(id, 1, Integer::sum);
    if (target instanceof com.arcane.entity.RuinGuardianEntity guardian) guardian.onReaction(id); // Fase N: a casca racha
    if (f.context.caster() instanceof ServerPlayer player) ReactionJournal.discover(level, player, id, nameKey, f.context.inscription());
  }

  static void evaluate(Frame f) {
    if (MassReactions.defs().isEmpty()) return;
    ServerLevel level = f.context.level();
    String form = f.context.plan().delivery().formId().getPath();
    Vec3 center = center(f);
    if (center == null) return;
    MassReactions.Def def = MassReactions.firstFiring(f.element, form, f.reactions, (block, radius) -> countBlocks(level, center, block, radius));
    if (def == null) return;
    var owner = f.context.caster();
    ReactionZones.spawn(level, def, center, owner == null ? null : owner.getUUID());
    com.arcane.fx.Fx.sound(level, center, new ResourceLocation("arcane", "reaction.vapor"), 1.0f, 1.0f);
    if (owner instanceof ServerPlayer player) ReactionJournal.discover(level, player, def.id(), def.nameKey(), f.context.inscription());
    com.arcane.training.DamageMeterEvents.massReaction(level, center, def.id());
  }

  /** Centro do que o efeito atingiu: media dos alvos (criaturas, blocos, pontos); sem alvos, a posicao da magia. */
  static Vec3 center(Frame f) {
    double x = 0, y = 0, z = 0;
    int n = 0;
    for (var e : f.targets.entities()) { var p = e.position(); x += p.x; y += p.y + e.getBbHeight() * 0.5; z += p.z; n++; }
    for (var b : f.targets.blocks()) { var p = Vec3.atCenterOf(b.pos()); x += p.x; y += p.y; z += p.z; n++; }
    for (var p : f.targets.points()) { x += p.x; y += p.y; z += p.z; n++; }
    if (n == 0) return f.context.position();
    return new Vec3(x / n, y / n, z / n);
  }

  static int countBlocks(ServerLevel level, Vec3 center, ResourceLocation block, double radius) {
    var target = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(block);
    if (target == null) return 0;
    int r = (int) Math.ceil(radius);
    BlockPos base = BlockPos.containing(center);
    int count = 0;
    for (BlockPos pos : BlockPos.betweenClosed(base.offset(-r, -r, -r), base.offset(r, r, r))) {
      if (Vec3.atCenterOf(pos).distanceToSqr(center) > radius * radius) continue;
      var state = level.getBlockState(pos);
      if (state.is(target) && (state.getFluidState().isEmpty() || state.getFluidState().isSource())) count++;
    }
    return count;
  }
}
