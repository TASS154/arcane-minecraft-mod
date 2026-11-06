package com.arcane.execution;

import com.arcane.ambient.AmbientFieldResolver;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.grimoire.GrimoireState;
import com.arcane.inscribing.InkRegistry;
import com.arcane.inscription.CompiledPlanCache;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionValidator;
import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.S2CSpellFxPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class CastingService {
    private CastingService() {}

    public static ExecutionResult tryCast(ServerPlayer player, Inscription inscription, PrecisionSnapshot precision) {
        Optional<GrimoireState> grimoire = GrimoireAttachments.get(player);
        InscriptionValidator.ValidationResult validation = InscriptionValidator.validate(inscription, grimoire);
        if (!validation.valid()) {
            return ExecutionResult.fail(validation.message().getString());
        }

        if (com.arcane.ritual.RitualRequirements.of(inscription).needsRitual()) {
            return ExecutionResult.fail("message.arcane.requires_ritual");
        }

        String broken = Vows.violation(player, inscription);
        if (broken != null) {
            return ExecutionResult.fail(broken);
        }

        CompiledExecutionPlan plan = CompiledPlanCache.getOrCompile(inscription);
        int baseCost = plan.baseManaCost();
        double inkMultiplier = InkRegistry.getManaMultiplier(inscription.metadata().ink());
        int manaCost = (int) Math.ceil(baseCost * precision.manaCostMultiplier() * inkMultiplier);

        ManaState mana = GrimoireAttachments.getManaOrCreate(player);
        if (!mana.trySpend(manaCost)) {
            return ExecutionResult.fail("message.arcane.mana.insufficient");
        }

        EnvironmentalSnapshot environment = AmbientFieldResolver.resolve((ServerLevel) player.level(), player.blockPosition());
        ExecutionContext context = new ExecutionContext(
                (ServerLevel) player.level(),
                player,
                inscription,
                plan,
                environment,
                precision,
                ExecutionContext.TriggerEvent.cast()
        );

        ExecutionResult result = SpellPipeline.INSTANCE.execute(context);
        if (result.success()) {
            Vows.pay(player, inscription);
            PlayerAnims.onCast(player, plan);
            ModNetwork.sendToPlayer(player, new S2CSpellFxPacket(inscription.id(), plan.fingerprint(), SpellElement.of(plan).ordinal()));
            castFx((ServerLevel) player.level(), player, SpellElement.of(plan));
        }
        return new ExecutionResult(result.success(), result.message(), manaCost);
    }

    /** Bonus de um ritual: nivel (1-4), poder multiplicado, escala de area e se foi lendario (sem sobretaxa). */
    public record RitualBonus(int level, double power, double areaScale, boolean legendary) {}

    /** Mana de um ritual: o custo normal; no lendario, sem a sobretaxa exponencial por tamanho. */
    public static int ritualCost(Inscription inscription, boolean legendary) {
        double cost = CompiledPlanCache.getOrCompile(inscription).baseManaCost();
        if (legendary) cost /= com.arcane.inscription.SpellLimits.multiplier(inscription.glyphs().size());
        return (int) Math.ceil(cost);
    }

    /** Conjura dentro de um ritual (os componentes e a mana ja foram cobrados por RitualService). */
    public static ExecutionResult castRitual(LivingEntity caster, Inscription inscription, ServerLevel level, RitualBonus bonus) {
        CompiledExecutionPlan plan = CompiledPlanCache.getOrCompile(inscription);
        EnvironmentalSnapshot environment = AmbientFieldResolver.resolve(level, caster.blockPosition());
        ExecutionContext context = new ExecutionContext(level, caster, inscription, plan, environment,
                PrecisionSnapshot.WORKBENCH, ExecutionContext.TriggerEvent.cast());
        context.setGeometryBonus(bonus.power());
        context.setAreaScale(bonus.areaScale());
        context.setRitualLevel(bonus.level());
        ExecutionResult result = SpellPipeline.INSTANCE.execute(context);
        if (result.success()) castFx(level, caster, SpellElement.of(context.plan()));
        return result;
    }

    public static ExecutionResult tryCast(LivingEntity caster, Inscription inscription, ServerLevel level) {
        if (com.arcane.ritual.RitualRequirements.of(inscription).needsRitual()) {
            return ExecutionResult.fail("message.arcane.requires_ritual");
        }
        String broken = Vows.violation(caster, inscription);
        if (broken != null) return ExecutionResult.fail(broken);
        CompiledExecutionPlan plan = CompiledPlanCache.getOrCompile(inscription);
        EnvironmentalSnapshot environment = AmbientFieldResolver.resolve(level, caster.blockPosition());
        ExecutionContext context = new ExecutionContext(
                level,
                caster,
                inscription,
                plan,
                environment,
                PrecisionSnapshot.WORKBENCH,
                ExecutionContext.TriggerEvent.cast()
        );
        ExecutionResult result = SpellPipeline.INSTANCE.execute(context);
        if (result.success()) {
            Vows.pay(caster, inscription);
            castFx(level, caster, SpellElement.of(context.plan()));
        }
        return result;
    }

    /** Som e particulas de conjuracao, visiveis para todos por perto. */
    private static void castFx(ServerLevel level, LivingEntity caster, SpellElement element) {
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                com.arcane.registry.ModSounds.SPELL_CAST.get(), net.minecraft.sounds.SoundSource.PLAYERS,
                0.8f, 0.9f + caster.getRandom().nextFloat() * 0.2f);
        ElementSounds.playCast(level, caster.position(), element);
        level.sendParticles(element.particle(),
                caster.getX(), caster.getY() + caster.getBbHeight() * 0.6, caster.getZ(),
                12, 0.4, 0.4, 0.4, 0.02);
    }
}
