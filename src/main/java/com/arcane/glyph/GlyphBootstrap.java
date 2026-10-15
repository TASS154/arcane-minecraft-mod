package com.arcane.glyph;

import com.arcane.execution.handlers.ActivateHandler;
import com.arcane.execution.handlers.AfterSecondsCondition;
import com.arcane.execution.handlers.BlocksFilterResolver;
import com.arcane.execution.handlers.DamageEffectHandler;
import com.arcane.execution.handlers.FireEffectHandler;
import com.arcane.execution.handlers.HealEffectHandler;
import com.arcane.execution.handlers.HostileFilterResolver;
import com.arcane.execution.handlers.OnHitEntityCondition;
import com.arcane.execution.handlers.ProjectileFormHandler;
import com.arcane.execution.handlers.PushEffectHandler;
import com.arcane.execution.handlers.SelfFilterResolver;
import com.arcane.execution.handlers.SelfFormHandler;
import com.arcane.execution.handlers.TouchFormHandler;

public final class GlyphBootstrap {
    private GlyphBootstrap() {}

    public static void registerDefaults() {
        GlyphHandlerRegistry.registerForm(ArcaneIds.SELF, new SelfFormHandler());
        GlyphHandlerRegistry.registerForm(ArcaneIds.TOUCH, new TouchFormHandler());
        GlyphHandlerRegistry.registerForm(ArcaneIds.PROJECTILE, new ProjectileFormHandler());

        GlyphHandlerRegistry.registerEffect(ArcaneIds.DAMAGE, new DamageEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.FIRE, new FireEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.PUSH, new PushEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.HEAL, new HealEffectHandler());

        GlyphHandlerRegistry.registerFilter(ArcaneIds.HOSTILE, new HostileFilterResolver());
        GlyphHandlerRegistry.registerFilter(ArcaneIds.SELF_FILTER, new SelfFilterResolver());
        GlyphHandlerRegistry.registerFilter(ArcaneIds.BLOCKS, new BlocksFilterResolver());

        GlyphHandlerRegistry.registerCondition(ArcaneIds.ON_HIT_ENTITY, new OnHitEntityCondition());
        GlyphHandlerRegistry.registerCondition(ArcaneIds.AFTER_SECONDS, new AfterSecondsCondition());

        GlyphHandlerRegistry.registerActivator(ArcaneIds.ACTIVATE, new ActivateHandler());
    }
}
