package com.arcane.glyph;

import com.arcane.execution.handlers.ActivateHandler;
import com.arcane.execution.handlers.AttractEffectHandler;
import com.arcane.execution.handlers.BarrierFormHandler;
import com.arcane.execution.handlers.BlockPredicateFilter;
import com.arcane.execution.handlers.CutEffectHandler;
import com.arcane.execution.handlers.FreezeEffectHandler;
import com.arcane.execution.handlers.GravityEffectHandler;
import com.arcane.execution.handlers.MarkEffectHandler;
import com.arcane.execution.handlers.PillarFormHandler;
import com.arcane.execution.handlers.WaveFormHandler;
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
        GlyphHandlerRegistry.registerForm(ArcaneIds.of("wave"), new WaveFormHandler());
        GlyphHandlerRegistry.registerForm(ArcaneIds.of("pillar"), new PillarFormHandler());
        GlyphHandlerRegistry.registerForm(ArcaneIds.of("barrier"), new BarrierFormHandler());
        GlyphHandlerRegistry.registerForm(ArcaneIds.of("summon_wolves"), new com.arcane.execution.handlers.SummonFormHandlers.Wolves());
        GlyphHandlerRegistry.registerForm(ArcaneIds.of("summon_golem"), new com.arcane.execution.handlers.SummonFormHandlers.Golem());

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
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("weather"), new com.arcane.execution.handlers.WorldRitualEffects.Weather());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("warp"), new com.arcane.execution.handlers.WorldRitualEffects.Warp());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("mark"), new MarkEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("gravity"), new GravityEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("attract"), new AttractEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("cut"), new CutEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("freeze"), new FreezeEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("blind"), new PotionEffectHandler(MobEffects.BLINDNESS, 400));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("wither"), new PotionEffectHandler(MobEffects.WITHER, 300));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("invisible"), new PotionEffectHandler(MobEffects.INVISIBILITY, 2400));

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

        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("marked"), new PredicateFilter((c, e) -> com.arcane.execution.Marks.isMarked(e)));
        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("unmarked"), new PredicateFilter((c, e) -> !com.arcane.execution.Marks.isMarked(e)));
        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("allies"), new PredicateFilter((c, e) ->
            e instanceof Player || e instanceof net.minecraft.world.entity.npc.AbstractVillager
                || e instanceof net.minecraft.world.entity.animal.IronGolem
                || (e instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame())));
        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("wood"), new BlockPredicateFilter(state ->
            state.is(net.minecraft.tags.BlockTags.LOGS) || state.is(net.minecraft.tags.BlockTags.PLANKS)));

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

        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_death"), new PredicateCondition(c -> "death".equals(c.trigger().type())));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_zone_end"), new PredicateCondition(c -> "zone_end".equals(c.trigger().type())));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_hurt"), new PredicateCondition(c -> "hurt".equals(c.trigger().type())));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("low_mana"), new PredicateCondition(c ->
            c.caster() instanceof Player p && com.arcane.grimoire.GrimoireAttachments.getMana(p)
                .map(m -> m.current() < m.max() * 0.2f).orElse(false)));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("standing_still"), new PredicateCondition(c -> {
            LivingEntity e = c.caster();
            double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
            return e.onGround() && dx * dx + dz * dz < 1.0e-4;
        }));

        // Ativador
        GlyphHandlerRegistry.registerActivator(ArcaneIds.ACTIVATE, new ActivateHandler());
    }
}
