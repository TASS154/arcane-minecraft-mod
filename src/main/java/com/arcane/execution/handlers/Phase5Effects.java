package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.pocket.ArcanePocket;
import net.minecraft.server.level.ServerPlayer;

/** Efeitos da Fase 5: Bolso Arcano e Restaurar. */
public final class Phase5Effects {
  private Phase5Effects() {}

  /**
   * Restaurar: recompoe os blocos que magias destruiram ha pouco, no raio (padrao 6, ate 12) em volta do alvo ou de voce.
   * Cobra mana por bloco que de fato voltou; sem mana para todos, volta so o que ela paga. Sem recarga.
   */
  public static class Restore implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      var caster = context.caster();
      if (!com.arcane.config.ArcaneConfig.RESTORE_ENABLED.get()) {
        say(caster, "message.arcane.restore.disabled");
        return;
      }
      net.minecraft.world.phys.Vec3 center = caster.position();
      for (var e : targets.entities()) if (e != caster) { center = e.position(); break; }
      if (center == caster.position()) {
        if (!targets.points().isEmpty()) center = targets.points().get(0);
        else if (!targets.blocks().isEmpty()) center = net.minecraft.world.phys.Vec3.atCenterOf(targets.blocks().get(0).pos());
      }
      double radius = Math.max(2.0, Math.min(12.0, context.amplify(call.parameters().getOrDefault("radius", 6.0), call.magnitudeKeys(), "radius")));
      int cap = com.arcane.config.ArcaneConfig.RESTORE_MAX_BLOCKS.get();
      double perBlock = com.arcane.config.ArcaneConfig.RESTORE_MANA_PER_BLOCK.get();
      com.arcane.execution.ManaState mana = caster instanceof net.minecraft.world.entity.player.Player pl ? com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(pl) : null;
      if (mana != null && perBlock > 0) cap = Math.min(cap, (int) Math.floor(mana.current() / perBlock));
      int done = cap <= 0 ? 0 : com.arcane.restore.BlockJournal.restore(context.level(), center, radius, cap);
      if (mana != null && done > 0) mana.set((float) Math.max(0.0, mana.current() - done * perBlock));
      if (done > 0) {
        context.level().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, center.x, center.y + 1.0, center.z, 24, radius * 0.35, 0.8, radius * 0.35, 0.02);
      }
      say(caster, done > 0 ? "message.arcane.restore.done" : "message.arcane.restore.nothing", done);
    }

    private static void say(net.minecraft.world.entity.LivingEntity who, String key, Object... args) {
      if (who instanceof net.minecraft.world.entity.player.Player p) p.displayClientMessage(net.minecraft.network.chat.Component.translatable(key, args), true);
    }
  }

  /** Bolso Arcano: abre o armazenamento pessoal de quem conjura (so jogadores; magos NPC nao tem bolso). */
  public static class Pocket implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      if (context.caster() instanceof ServerPlayer player) ArcanePocket.open(player);
    }
  }
}
