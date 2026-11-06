package com.arcane.execution.handlers;

import com.arcane.api.activator.ActivatorHandler;
import com.arcane.execution.ExecutionContext;
import com.arcane.inscription.GlyphInstance;
import com.arcane.ritual.RitualService;

import java.util.UUID;

public class ActivateHandler implements ActivatorHandler {
  @Override
  public void activate(ExecutionContext context, GlyphInstance instance) {
    int fired = RitualService.triggerNearby(context.level(), context.caster());
    if (fired > 0 && context.caster() instanceof net.minecraft.world.entity.player.Player player) {
      player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.arcane.ritual.triggered"), true);
    }
  }
}
