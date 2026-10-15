package com.arcane.api.activator;

import com.arcane.execution.ExecutionContext;
import com.arcane.inscription.GlyphInstance;

public interface ActivatorHandler {
    void activate(ExecutionContext context, GlyphInstance instance);
}
