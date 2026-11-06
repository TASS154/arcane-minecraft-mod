package com.arcane.execution.handlers;

import com.arcane.api.condition.ConditionHandler;
import com.arcane.execution.ExecutionContext;
import com.arcane.inscription.GlyphInstance;

import java.util.function.Predicate;

public class PredicateCondition implements ConditionHandler {
  private final Predicate<ExecutionContext> test;

  public PredicateCondition(Predicate<ExecutionContext> test) {
    this.test = test;
  }

  @Override
  public boolean test(ExecutionContext context, GlyphInstance instance) {
    return test.test(context);
  }
}
