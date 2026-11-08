package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.Targeting;

/** Raio instantaneo. Tambem serve ao Toque (alcance curto, sem rastro). */
public class RayFormHandler implements FormHandler {
  private final boolean trail;

  public RayFormHandler(boolean trail) {
    this.trail = trail;
  }

  @Override
  public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
    double range = Math.max(1.0, Math.min(64.0, spec.range()));
    var targets = Targeting.ray(context, range, trail).targets();
    // O Toque so alcanca o que esta ao seu alcance: nao entrega ponto vazio.
    if (!trail) targets = new com.arcane.execution.TargetList(targets.entities(), targets.blocks());
    return DeliveryResult.of(targets);
  }
}
