package com.arcane.inscription;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CompiledPlanCache {
    private static final Map<String, CompiledExecutionPlan> CACHE = new ConcurrentHashMap<>();

    private CompiledPlanCache() {}

    public static CompiledExecutionPlan getOrCompile(Inscription inscription) {
        String key = inscription.fingerprint();
        return CACHE.computeIfAbsent(key, k -> InscriptionCompiler.compile(inscription));
    }

    public static void clear() {
        CACHE.clear();
    }
}
