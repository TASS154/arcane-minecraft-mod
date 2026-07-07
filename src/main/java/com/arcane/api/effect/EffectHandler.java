package com.arcane.api.effect;

import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;

public interface EffectHandler {
    void apply(ExecutionContext context, EffectCall call, TargetList targets);
}
