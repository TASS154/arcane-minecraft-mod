package com.arcane.glyph;

import com.arcane.execution.handlers.ActivateHandler;
import com.arcane.execution.handlers.AfterSecondsCondition;
import com.arcane.execution.handlers.AuraFormHandler;
import com.arcane.execution.handlers.BlinkEffectHandler;
import com.arcane.execution.handlers.BlocksFilterResolver;
import com.arcane.execution.handlers.DamageEffectHandler;
import com.arcane.execution.handlers.DigEffectHandler;
import com.arcane.execution.handlers.ExplodeEffectHandler;
import com.arcane.execution.handlers.FireEffectHandler;
import com.arcane.execution.handlers.HealEffectHandler;
import com.arcane.execution.handlers.HostileFilterResolver;
import com.arcane.execution.handlers.IgniteEffectHandler;
import com.arcane.execution.handlers.NearestFilter;
import com.arcane.execution.handlers.OnHitEntityCondition;
import com.arcane.execution.handlers.PotionEffectHandler;
import com.arcane.execution.handlers.PredicateCondition;
import com.arcane.execution.handlers.PredicateFilter;
import com.arcane.execution.handlers.ProjectileFormHandler;
import com.arcane.execution.handlers.PushEffectHandler;
import com.arcane.execution.handlers.RayFormHandler;
import com.arcane.execution.handlers.SelfFilterResolver;
import com.arcane.execution.handlers.SelfFormHandler;
import com.arcane.execution.handlers.ThunderEffectHandler;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

/** Liga cada glifo (data/arcane/glyphs/*.json) ao seu codigo. O id do glifo e o id do handler. */
public final class GlyphBootstrap {
    private GlyphBootstrap() {}

    public static void registerDefaults() {
        // Formas
        GlyphHandlerRegistry.registerForm(ArcaneIds.SELF, new SelfFormHandler());
        GlyphHandlerRegistry.registerForm(ArcaneIds.TOUCH, new RayFormHandler(false));
        GlyphHandlerRegistry.registerForm(ArcaneIds.PROJECTILE, new ProjectileFormHandler());
        GlyphHandlerRegistry.registerForm(ArcaneIds.of("ray"), new RayFormHandler(true));
        GlyphHandlerRegistry.registerForm(ArcaneIds.of("aura"), new AuraFormHandler());

        // Efeitos
        GlyphHandlerRegistry.registerEffect(ArcaneIds.DAMAGE, new DamageEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.FIRE, new FireEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.PUSH, new PushEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.HEAL, new HealEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("slow"), new PotionEffectHandler(MobEffects.MOVEMENT_SLOWDOWN, 1200));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("poison"), new PotionEffectHandler(MobEffects.POISON, 600));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("speed"), new PotionEffectHandler(MobEffects.MOVEMENT_SPEED, 2400));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("levitate"), new PotionEffectHandler(MobEffects.LEVITATION, 200));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("shield"), new PotionEffectHandler(MobEffects.ABSORPTION, 4800));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("thunder"), new ThunderEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("explode"), new ExplodeEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("blink"), new BlinkEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("ignite"), new IgniteEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("dig"), new DigEffectHandler());

        // Filtros
        GlyphHandlerRegistry.registerFilter(ArcaneIds.HOSTILE, new HostileFilterResolver());
        GlyphHandlerRegistry.registerFilter(ArcaneIds.SELF_FILTER, new SelfFilterResolver());
        GlyphHandlerRegistry.registerFilter(ArcaneIds.BLOCKS, new BlocksFilterResolver());
        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("players"), new PredicateFilter((c, e) -> e instanceof Player));
        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("animals"), new PredicateFilter((c, e) -> e instanceof Animal));
        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("undead"),
            new PredicateFilter((c, e) -> e instanceof LivingEntity l && l.getMobType() == MobType.UNDEAD));
        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("not_self"), new PredicateFilter((c, e) -> e != c.caster()));
        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("nearest"), new NearestFilter());
        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("wounded"),
            new PredicateFilter((c, e) -> e instanceof LivingEntity l && l.getHealth() < l.getMaxHealth() * 0.5f));

        // Condicoes
        GlyphHandlerRegistry.registerCondition(ArcaneIds.ON_HIT_ENTITY, new OnHitEntityCondition());
        GlyphHandlerRegistry.registerCondition(ArcaneIds.AFTER_SECONDS, new AfterSecondsCondition());
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_hit_block"),
            new PredicateCondition(c -> c.trigger().blockPos().isPresent()));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("low_health"),
            new PredicateCondition(c -> c.caster().getHealth() <= c.caster().getMaxHealth() * 0.5f));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("sneaking"), new PredicateCondition(c -> c.caster().isShiftKeyDown()));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("night"), new PredicateCondition(c -> c.level().isNight()));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("in_water"), new PredicateCondition(c -> c.caster().isInWater()));

        // Ativador
        GlyphHandlerRegistry.registerActivator(ArcaneIds.ACTIVATE, new ActivateHandler());
    }
}
