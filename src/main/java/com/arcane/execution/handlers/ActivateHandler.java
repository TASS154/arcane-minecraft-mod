package com.arcane.execution.handlers;

import com.arcane.execution.ExecutionContext;
import com.arcane.glyph.GlyphHandlerRegistry;
import com.arcane.inscription.GlyphInstance;
import com.arcane.ritual.RitualService;

import java.util.UUID;

public class ActivateHandler implements GlyphHandlerRegistry.ActivatorHandler {
  @Override
  public void activate(ExecutionContext context, GlyphInstance instance) {
    UUID ritualId = context.ritualSiteId().orElse(null);
    if (ritualId == null && instance.parameters().containsKey("ritual_hash")) {
      long hash = instance.parameters().get("ritual_hash").longValue();
      ritualId = new UUID(0L, hash);
    }
    if (ritualId != null) {
      RitualService.activate(context.level(), ritualId, context.caster());
    }
  }
}
