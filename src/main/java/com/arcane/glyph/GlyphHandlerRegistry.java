package com.arcane.glyph;

import com.arcane.api.effect.EffectHandler;
import com.arcane.api.filter.FilterResolver;
import com.arcane.api.form.FormHandler;
import com.arcane.execution.handlers.DamageEffectHandler;
import com.arcane.execution.handlers.FireEffectHandler;
import com.arcane.execution.handlers.HealEffectHandler;
import com.arcane.execution.handlers.HostileFilterResolver;
import com.arcane.execution.handlers.BlocksFilterResolver;
import com.arcane.execution.handlers.SelfFilterResolver;
import com.arcane.execution.handlers.ProjectileFormHandler;
import com.arcane.execution.handlers.PushEffectHandler;
import com.arcane.execution.handlers.SelfFormHandler;
import com.arcane.execution.handlers.TouchFormHandler;
import com.arcane.execution.handlers.ActivateHandler;
import com.arcane.execution.handlers.OnHitEntityCondition;
import com.arcane.execution.handlers.AfterSecondsCondition;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class GlyphHandlerRegistry {
    private static final Map<ResourceLocation, FormHandler> FORM_HANDLERS = new HashMap<>();
    private static final Map<ResourceLocation, EffectHandler> EFFECT_HANDLERS = new HashMap<>();
    private static final Map<ResourceLocation, FilterResolver> FILTER_RESOLVERS = new HashMap<>();
    private static final Map<ResourceLocation, ConditionHandler> CONDITION_HANDLERS = new HashMap<>();
    private static final Map<ResourceLocation, ActivatorHandler> ACTIVATOR_HANDLERS = new HashMap<>();

    private GlyphHandlerRegistry() {}

    public static void bootstrap() {
        registerForm(new ResourceLocation("arcane", "self"), new SelfFormHandler());
        registerForm(new ResourceLocation("arcane", "touch"), new TouchFormHandler());
        registerForm(new ResourceLocation("arcane", "projectile"), new ProjectileFormHandler());

        registerEffect(new ResourceLocation("arcane", "damage"), new DamageEffectHandler());
        registerEffect(new ResourceLocation("arcane", "fire"), new FireEffectHandler());
        registerEffect(new ResourceLocation("arcane", "push"), new PushEffectHandler());
        registerEffect(new ResourceLocation("arcane", "heal"), new HealEffectHandler());

        registerFilter(new ResourceLocation("arcane", "hostile"), new HostileFilterResolver());
        registerFilter(new ResourceLocation("arcane", "self_filter"), new SelfFilterResolver());
        registerFilter(new ResourceLocation("arcane", "blocks"), new BlocksFilterResolver());

        registerCondition(new ResourceLocation("arcane", "on_hit_entity"), new OnHitEntityCondition());
        registerCondition(new ResourceLocation("arcane", "after_seconds"), new AfterSecondsCondition());

        registerActivator(new ResourceLocation("arcane", "activate"), new ActivateHandler());
    }

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

    public interface ConditionHandler {
        boolean test(com.arcane.execution.ExecutionContext context, com.arcane.inscription.GlyphInstance instance);
    }

    public interface ActivatorHandler {
        void activate(com.arcane.execution.ExecutionContext context, com.arcane.inscription.GlyphInstance instance);
    }
}
