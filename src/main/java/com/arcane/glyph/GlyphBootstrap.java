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
        GlyphHandlerRegistry.registerForm(ArcaneIds.of("focus"), new com.arcane.execution.handlers.FocusFormHandler());
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
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("blast"), new com.arcane.execution.handlers.BlastEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("blink"), new BlinkEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("ignite"), new IgniteEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("dig"), new DigEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("grow"), new com.arcane.execution.handlers.GrowEffectHandler());
        // Glifos originais (so das Hidras)
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("time_stop"), new com.arcane.execution.handlers.OriginalGlyphs.TimeStop());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("rewind"), new com.arcane.execution.handlers.OriginalGlyphs.Rewind());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("space_collapse"), new com.arcane.execution.handlers.OriginalGlyphs.SpaceCollapse());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("annihilate"), new com.arcane.execution.handlers.OriginalGlyphs.Annihilate());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("eclipse"), new com.arcane.execution.handlers.OriginalGlyphs.Eclipse());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("genesis"), new com.arcane.execution.handlers.OriginalGlyphs.Genesis());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("gravity_inversion"), new com.arcane.execution.handlers.OriginalGlyphs.GravityInversion());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("light"), new com.arcane.execution.handlers.LightEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("water_walk"), new PotionEffectHandler(com.arcane.registry.ModEffects.WATER_WALK.get(), 24000));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("breathe"), new PotionEffectHandler(MobEffects.WATER_BREATHING, 24000));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("night_eye"), new PotionEffectHandler(MobEffects.NIGHT_VISION, 24000));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("feather"), new PotionEffectHandler(MobEffects.SLOW_FALLING, 24000));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("weather"), new com.arcane.execution.handlers.WorldRitualEffects.Weather());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("warp"), new com.arcane.execution.handlers.WorldRitualEffects.Warp());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("mark"), new MarkEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("gravity"), new GravityEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("float"), new com.arcane.execution.handlers.FloatEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("telekinesis"), new com.arcane.execution.handlers.TelekinesisEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("attract"), new AttractEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("cut"), new CutEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("freeze"), new FreezeEffectHandler());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("blind"), new PotionEffectHandler(MobEffects.BLINDNESS, 400));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("wither"), new PotionEffectHandler(MobEffects.WITHER, 300));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("invisible"), new com.arcane.execution.handlers.LifeEffects.ArcaneInvisibility());

        // Fase 3: efeitos baratos
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("rain"), new com.arcane.execution.handlers.LowCostEffects.Rain());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("charge"), new com.arcane.execution.handlers.LowCostEffects.Charge());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("glide"), new PotionEffectHandler(MobEffects.SLOW_FALLING, 1200));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("taunt"), new com.arcane.execution.handlers.LowCostEffects.Taunt());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("fear"), new com.arcane.execution.handlers.LowCostEffects.Fear());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("sleep"), new com.arcane.execution.handlers.LowCostEffects.Sleep());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("rumble"), new com.arcane.execution.handlers.LowCostEffects.Rumble());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("darkness"), new PotionEffectHandler(MobEffects.DARKNESS, 600));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("wet"), new com.arcane.execution.handlers.LowCostEffects.Soak());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("lifesteal"), new com.arcane.execution.handlers.LowCostEffects.LifeSteal());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("life_transfer"), new com.arcane.execution.handlers.LowCostEffects.LifeTransfer());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("read_item"), new com.arcane.execution.handlers.LowCostEffects.ReadItem());

        // Fase 5
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("pocket"), new com.arcane.execution.handlers.Phase5Effects.Pocket());

        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("restore"), new com.arcane.execution.handlers.Phase5Effects.Restore());

        // Fase 4: glifos medios
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("satellites"), new com.arcane.execution.handlers.ControlEffects.Satellites());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("link"), new com.arcane.execution.handlers.ControlEffects.Link());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("trail"), new com.arcane.execution.handlers.ControlEffects.Trail());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("hook"), new com.arcane.execution.handlers.UtilityEffects.Hook());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("climb"), new com.arcane.execution.handlers.UtilityEffects.Climb());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("phase"), new com.arcane.execution.handlers.UtilityEffects.Phase());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("recall"), new com.arcane.execution.handlers.UtilityEffects.Recall());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("imbue"), new com.arcane.execution.handlers.ControlEffects.Imbue());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("petrify"), new com.arcane.execution.handlers.ControlEffects.Petrify());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("acid"), new com.arcane.execution.handlers.ControlEffects.Acid());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("electrify"), new com.arcane.execution.handlers.ControlEffects.Electrify());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("contagion"), new com.arcane.execution.handlers.ControlEffects.Contagion());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("reanimate"), new com.arcane.execution.handlers.ControlEffects.Reanimate());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("sense"), new com.arcane.execution.handlers.UtilityEffects.Sense());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("arcane_sight"), new com.arcane.execution.handlers.UtilityEffects.ArcaneSight());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("emit_signal"), new com.arcane.execution.handlers.UtilityEffects.EmitSignal());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("dispel"), new com.arcane.execution.handlers.ControlEffects.Dispel());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("reflect"), new com.arcane.execution.handlers.ControlEffects.Reflect());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("silence"), new com.arcane.execution.handlers.ControlEffects.Silence());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("drain_mana"), new com.arcane.execution.handlers.ControlEffects.DrainMana());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("swap"), new com.arcane.execution.handlers.UtilityEffects.Swap());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("reveal"), new com.arcane.execution.handlers.UtilityEffects.Reveal());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("decoy"), new com.arcane.execution.handlers.UtilityEffects.Decoy());
        // rodada de 23 nov (lista aceita)
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("illusion"), new com.arcane.execution.handlers.LoreEffects.Illusion());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("mend"), new com.arcane.execution.handlers.LifeEffects.Mend());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("organize"), new com.arcane.execution.handlers.LifeEffects.Organize());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("collect"), new com.arcane.execution.handlers.LifeEffects.Collect());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("mark_point"), new com.arcane.execution.handlers.LifeEffects.MarkPoint());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("donate_mana"), new com.arcane.execution.handlers.LifeEffects.DonateMana());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("warm"), new com.arcane.execution.handlers.LifeEffects.Warm());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("rest_zone"), new com.arcane.execution.handlers.LifeEffects.RestZone());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("last_breath"), new com.arcane.execution.handlers.LifeEffects.LastBreath());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("cook"), new com.arcane.execution.handlers.LifeEffects.Cook());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("dry"), new com.arcane.execution.handlers.LifeEffects.Dry());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("harvest"), new com.arcane.execution.handlers.LifeEffects.Harvest());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("herd"), new com.arcane.execution.handlers.LifeEffects.Herd());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("pet"), new com.arcane.execution.handlers.LifeEffects.Pet());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("echo"), new com.arcane.execution.handlers.LifeEffects.Echo());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("prospect"), new com.arcane.execution.handlers.LifeEffects.Prospect());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("remember"), new com.arcane.execution.handlers.LifeEffects.Remember());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("echo_past"), new com.arcane.execution.handlers.LifeEffects.EchoOfPast());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("arcane_fireworks"), new com.arcane.execution.handlers.LifeEffects.Fireworks());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("flower_trail"), new com.arcane.execution.handlers.LifeEffects.FlowerTrail());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("bubbles"), new com.arcane.execution.handlers.LifeEffects.Bubbles());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("tickle"), new com.arcane.execution.handlers.LifeEffects.Tickle());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("twirl"), new com.arcane.execution.handlers.LifeEffects.Twirl());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("playful_shadow"), new com.arcane.execution.handlers.LifeEffects.PlayfulShadow());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("starry_sky"), new com.arcane.execution.handlers.LifeEffects.StarrySky());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("melody"), new com.arcane.execution.handlers.LifeEffects.Melody());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("glow_item"), new com.arcane.execution.handlers.LifeEffects.GlowItem());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("seal_creature"), new com.arcane.execution.handlers.LoreEffects.SealCreature());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("read_residue"), new com.arcane.execution.handlers.LoreEffects.ReadResidue());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("track_possession"), new com.arcane.execution.handlers.LoreEffects.TrackPossession());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("paired_portal"), new com.arcane.execution.handlers.LoreEffects.PairedPortal());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("counterspell"), new com.arcane.execution.handlers.LoreEffects.Counterspell());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("lock"), new com.arcane.execution.handlers.LoreEffects.Lock());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("seal_bench"), new com.arcane.execution.handlers.LoreEffects.SealBench());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("baptize"), new com.arcane.execution.handlers.LoreEffects.Baptize());
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("silent_step"), new PotionEffectHandler(com.arcane.registry.ModEffects.SILENT_STEP.get(), 6000));
        GlyphHandlerRegistry.registerEffect(ArcaneIds.of("curse"), new com.arcane.execution.handlers.ControlEffects.Curse());

        // Filtros
        GlyphHandlerRegistry.registerFilter(ArcaneIds.HOSTILE, new HostileFilterResolver());
        GlyphHandlerRegistry.registerFilter(ArcaneIds.SELF_FILTER, new SelfFilterResolver());
        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("outsiders"), new com.arcane.execution.handlers.OutsidersFilterResolver());
        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("named"), new PredicateFilter((c, e) -> e.hasCustomName()));
        GlyphHandlerRegistry.registerFilter(ArcaneIds.of("aerial"), new PredicateFilter((c, e) -> e instanceof net.minecraft.world.entity.animal.FlyingAnimal
            || e instanceof net.minecraft.world.entity.FlyingMob || (!e.onGround() && !e.isInWater() && !e.isPassenger())));
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
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_zone_enter"), new PredicateCondition(c -> "zone_enter".equals(c.trigger().type())));
        for (String[] t : new String[][] {{"on_sleep", "sleep"}, {"on_fish", "fish"}, {"on_dawn", "dawn"}, {"on_eat", "eat"}, {"on_see_cast", "see_cast"}}) {
            GlyphHandlerRegistry.registerCondition(ArcaneIds.of(t[0]), new PredicateCondition(c -> t[1].equals(c.trigger().type())));
        }
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_barrier_break"), new PredicateCondition(c -> "barrier_break".equals(c.trigger().type())));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_zone_exit"), new PredicateCondition(c -> "zone_exit".equals(c.trigger().type())));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_hurt"), new PredicateCondition(c -> "hurt".equals(c.trigger().type())));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("low_mana"), new PredicateCondition(c ->
            c.caster() instanceof Player p && com.arcane.grimoire.GrimoireAttachments.getMana(p)
                .map(m -> m.current() < m.max() * 0.2f).orElse(false)));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("standing_still"), new PredicateCondition(c -> {
            LivingEntity e = c.caster();
            double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
            return e.onGround() && dx * dx + dz * dz < 1.0e-4;
        }));

        // Fase 3: gatilhos e condicoes de estado
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_kill"), new PredicateCondition(c -> "kill".equals(c.trigger().type())));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_fall"), new PredicateCondition(c -> "fall".equals(c.trigger().type())));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_jump"), new PredicateCondition(c -> "jump".equals(c.trigger().type())));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("target_health"), new com.arcane.execution.handlers.StateConditions.TargetHealth());
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("region"), new com.arcane.execution.handlers.StateConditions.Region());
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("holding"), new com.arcane.execution.handlers.StateConditions.Holding());
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("raining"), new com.arcane.execution.handlers.StateConditions.Raining());

        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("after_hits"), new PredicateCondition(c -> "hits".equals(c.trigger().type())));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_signal"), new PredicateCondition(c -> true));
        GlyphHandlerRegistry.registerCondition(ArcaneIds.of("on_near"), new PredicateCondition(c -> true));

        // Ativador
        GlyphHandlerRegistry.registerActivator(ArcaneIds.ACTIVATE, new ActivateHandler());
    }
}
