package com.arcane.execution.handlers;

import com.arcane.execution.ExecutionContext;
import com.arcane.glyph.GlyphHandlerRegistry;
import com.arcane.inscription.GlyphInstance;

public class OnHitEntityCondition implements GlyphHandlerRegistry.ConditionHandler {
    @Override
    public boolean test(ExecutionContext context, GlyphInstance instance) {
        return context.trigger().entity().isPresent();
    }
}
