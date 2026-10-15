package com.arcane.api.condition;

import com.arcane.execution.ExecutionContext;
import com.arcane.inscription.GlyphInstance;

public interface ConditionHandler {
    boolean test(ExecutionContext context, GlyphInstance instance);
}
