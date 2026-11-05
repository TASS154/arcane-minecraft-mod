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
            ModNetwork.sendToPlayer(player, new S2CSpellFxPacket(inscription.id(), plan.fingerprint()));
            castFx((ServerLevel) player.level(), player);
        }
        return new ExecutionResult(result.success(), result.message(), manaCost);
    }

    public static ExecutionResult tryCast(LivingEntity caster, Inscription inscription, ServerLevel level) {
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
            castFx(level, caster);
        }
        return result;
    }

    /** Som e particulas de conjuracao, visiveis para todos por perto. */
    private static void castFx(ServerLevel level, LivingEntity caster) {
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                com.arcane.registry.ModSounds.SPELL_CAST.get(), net.minecraft.sounds.SoundSource.PLAYERS,
                0.8f, 0.9f + caster.getRandom().nextFloat() * 0.2f);
        level.sendParticles(com.arcane.registry.ModParticles.ARCANE_SPARK.get(),
                caster.getX(), caster.getY() + caster.getBbHeight() * 0.6, caster.getZ(),
                12, 0.4, 0.4, 0.4, 0.02);
    }
}
