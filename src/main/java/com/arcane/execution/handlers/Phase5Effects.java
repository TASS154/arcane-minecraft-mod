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

  /** Bolso Arcano: abre o armazenamento pessoal de quem conjura (so jogadores; magos NPC nao tem bolso). */
  public static class Pocket implements EffectHandler {
    @Override
    public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
      if (context.caster() instanceof ServerPlayer player) ArcanePocket.open(player);
    }
  }
}
