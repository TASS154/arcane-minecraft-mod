package com.arcane.execution;

public record ExecutionResult(boolean success, String message, int manaSpent) {
    public static ExecutionResult ok(int manaSpent) {
        return new ExecutionResult(true, "", manaSpent);
    }

    public static ExecutionResult fail(String message) {
        return new ExecutionResult(false, message, 0);
    }
}
