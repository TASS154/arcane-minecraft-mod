package com.arcane.glyph;

import com.arcane.api.activator.ActivatorHandler;
import com.arcane.api.condition.ConditionHandler;
import com.arcane.api.effect.EffectHandler;
import com.arcane.api.filter.FilterResolver;
import com.arcane.api.form.FormHandler;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class GlyphHandlerRegistry {
    private static final Map<ResourceLocation, FormHandler> FORM_HANDLERS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EffectHandler> EFFECT_HANDLERS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, FilterResolver> FILTER_RESOLVERS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ConditionHandler> CONDITION_HANDLERS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ActivatorHandler> ACTIVATOR_HANDLERS = new ConcurrentHashMap<>();

    private GlyphHandlerRegistry() {}

    public static void registerForm(ResourceLocation id, FormHandler handler) {
        FORM_HANDLERS.put(id, handler);
    }

    public static void registerEffect(ResourceLocation id, EffectHandler handler) {
        EFFECT_HANDLERS.put(id, handler);
    }

    public static void registerFilter(ResourceLocation id, FilterResolver resolver) {
        FILTER_RESOLVERS.put(id, resolver);
    }

    public static void registerCondition(ResourceLocation id, ConditionHandler handler) {
        CONDITION_HANDLERS.put(id, handler);
    }

    public static void registerActivator(ResourceLocation id, ActivatorHandler handler) {
        ACTIVATOR_HANDLERS.put(id, handler);
    }

    public static Optional<FormHandler> getForm(ResourceLocation id) {
        return Optional.ofNullable(FORM_HANDLERS.get(id));
    }

    public static Optional<EffectHandler> getEffect(ResourceLocation id) {
        return Optional.ofNullable(EFFECT_HANDLERS.get(id));
    }

    public static Optional<FilterResolver> getFilter(ResourceLocation id) {
        return Optional.ofNullable(FILTER_RESOLVERS.get(id));
    }

    public static Optional<ConditionHandler> getCondition(ResourceLocation id) {
        return Optional.ofNullable(CONDITION_HANDLERS.get(id));
    }

    public static Optional<ActivatorHandler> getActivator(ResourceLocation id) {
        return Optional.ofNullable(ACTIVATOR_HANDLERS.get(id));
    }
}
