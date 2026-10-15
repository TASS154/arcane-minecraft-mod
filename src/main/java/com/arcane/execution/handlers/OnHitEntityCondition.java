package com.arcane.execution.handlers;

import com.arcane.api.condition.ConditionHandler;
import com.arcane.execution.ExecutionContext;
import com.arcane.inscription.GlyphInstance;

public class OnHitEntityCondition implements ConditionHandler {
    @Override
    public boolean test(ExecutionContext context, GlyphInstance instance) {
        return context.trigger().entity().isPresent();
    }
}
