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
        return SpellPipeline.INSTANCE.execute(context);
    }
}
