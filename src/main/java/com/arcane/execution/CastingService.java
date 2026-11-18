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
import com.arcane.network.packet.S2CShapeFxPacket;
import com.arcane.network.packet.S2CSpellFxPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class CastingService {
    private CastingService() {}

    public static ExecutionResult tryCast(ServerPlayer player, Inscription inscription, PrecisionSnapshot precision) {
        return cast(player, inscription, precision, 1.0, false);
    }

    /** Custo de mana de uma conjuracao normal (o mesmo cobrado por tryCast). */
    public static int manaCostOf(Inscription inscription, PrecisionSnapshot precision) {
        CompiledExecutionPlan plan = CompiledPlanCache.getOrCompile(inscription);
        return (int) Math.ceil(plan.baseManaCost() * precision.manaCostMultiplier() * InkRegistry.getManaMultiplier(inscription.metadata().ink()));
    }

    /**
     * Conjura como o jogador. `scale` multiplica os parametros de efeito (magias carregadas); com `free` a mana ja foi
     * paga (carga gradual) e nada e cobrado agora.
     */
    public static ExecutionResult cast(ServerPlayer player, Inscription inscription, PrecisionSnapshot precision, double scale, boolean free) {
        return cast(player, inscription, precision, scale, free, false);
    }

    /**
     * Como a conjuracao normal; com {@code silent} (magia mantida) o efeito e aplicado de novo sem a animacao, o clarao,
     * o som e as particulas de conjuracao: a magia ja foi conjurada.
     */
    public static ExecutionResult cast(ServerPlayer player, Inscription inscription, PrecisionSnapshot precision, double scale, boolean free, boolean silent) {
        Optional<GrimoireState> grimoire = GrimoireAttachments.get(player);
        InscriptionValidator.ValidationResult validation = InscriptionValidator.validate(inscription, grimoire);
        if (!validation.valid()) {
            return ExecutionResult.fail(validation.message().getString());
        }

        if (com.arcane.ritual.RitualRequirements.of(inscription).needsRitual()) {
            return ExecutionResult.fail("message.arcane.requires_ritual");
        }

        if (Phase4State.active(player, Phase4State.SILENCE)) {
            return ExecutionResult.fail("message.arcane.silenced");
        }
        String broken = Vows.violation(player, inscription);
        if (broken != null) {
            return ExecutionResult.fail(broken);
        }

        // Foco: precisa de uma criatura viva sob a mira; sem ela, falha antes de cobrar qualquer mana.
        if (com.arcane.execution.handlers.FocusFormHandler.hasFocus(inscription) && com.arcane.execution.handlers.FocusFormHandler.find(
                player, player.getEyePosition(), player.getLookAngle(), com.arcane.execution.handlers.FocusFormHandler.rangeOf(inscription)) == null) {
            return ExecutionResult.fail("message.arcane.focus.no_target");
        }

        // Coro: nao dispara sozinho. Entra numa sessao (ou abre uma) e quem paga e dispara e o ChorusService, no fim da janela.
        if (!ChorusService.firing() && ChorusService.isChorus(inscription)) {
            if (free) return ExecutionResult.fail("message.arcane.chorus.nocharge");
            return ChorusService.handle(player, inscription, scale);
        }

        CompiledExecutionPlan plan = CompiledPlanCache.getOrCompile(inscription);
        int baseCost = plan.baseManaCost();
        double inkMultiplier = InkRegistry.getManaMultiplier(inscription.metadata().ink());
        int manaCost = (int) Math.ceil(baseCost * precision.manaCostMultiplier() * inkMultiplier);

        ManaState mana = GrimoireAttachments.getManaOrCreate(player);
        if (!free && !mana.trySpend(manaCost)) {
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
        context.setScaleFactor(scale);

        ExecutionResult result = SpellPipeline.INSTANCE.execute(context);
        if (result.success()) {
            GrimoireAttachments.get(player).ifPresent(g -> g.prefs().touch(inscription.id(), player.level().getGameTime()));
            Vows.pay(player, inscription);
            if (!silent) {
                PlayerAnims.onCast(player, plan);
                ModNetwork.sendToPlayer(player, new S2CSpellFxPacket(inscription.id(), plan.fingerprint(), SpellElement.of(plan).ordinal()));
                if (!Traces.stealthy(inscription)) castFx((ServerLevel) player.level(), player, SpellElement.of(plan));
            }
        }
        return new ExecutionResult(result.success(), result.message(), manaCost);
    }

    /** Bonus de um ritual: nivel (1-4), poder multiplicado, escala de area e se foi lendario (sem sobretaxa). */
    public record RitualBonus(int level, double power, double areaScale, boolean legendary, double radius, net.minecraft.world.phys.Vec3 center) {
        public RitualBonus(int level, double power, double areaScale, boolean legendary) { this(level, power, areaScale, legendary, 0.0, null); }
    }

    /** Mana de um ritual: o custo normal; no lendario, sem a sobretaxa exponencial por tamanho. */
    public static int ritualCost(Inscription inscription, boolean legendary) {
        double cost = CompiledPlanCache.getOrCompile(inscription).baseManaCost();
        if (legendary) cost /= com.arcane.inscription.SpellLimits.multiplier(inscription.glyphs().size());
        return (int) Math.ceil(cost);
    }

    /** Conjura dentro de um ritual (os componentes e a mana ja foram cobrados por RitualService). */
    public static ExecutionResult castRitual(LivingEntity caster, Inscription inscription, ServerLevel level, RitualBonus bonus) {
        CompiledExecutionPlan plan = CompiledPlanCache.getOrCompile(inscription);
        // O cristal vale onde o ritual acontece (o altar), nao onde o conjurador esta.
        net.minecraft.core.BlockPos where = bonus.center() != null ? net.minecraft.core.BlockPos.containing(bonus.center()) : caster.blockPosition();
        EnvironmentalSnapshot environment = AmbientFieldResolver.resolve(level, where);
        ExecutionContext context = new ExecutionContext(level, caster, inscription, plan, environment,
                PrecisionSnapshot.WORKBENCH, ExecutionContext.TriggerEvent.cast());
        context.setGeometryBonus(bonus.power());
        context.setAreaScale(bonus.areaScale());
        context.setRitualLevel(bonus.level());
        if (bonus.radius() > 0 && bonus.center() != null) context.setRitual(bonus.radius(), bonus.center());
        ExecutionResult result = SpellPipeline.INSTANCE.execute(context);
        if (result.success() && !Traces.stealthy(inscription)) castFx(level, caster, SpellElement.of(context.plan()));
        return result;
    }

    public static ExecutionResult tryCast(LivingEntity caster, Inscription inscription, ServerLevel level) {
        if (com.arcane.ritual.RitualRequirements.of(inscription).needsRitual()) {
            return ExecutionResult.fail("message.arcane.requires_ritual");
        }
        if (Phase4State.active(caster, Phase4State.SILENCE)) return ExecutionResult.fail("message.arcane.silenced");
        if (ChorusService.isChorus(inscription)) return ExecutionResult.fail("message.arcane.chorus.players_only");
        if (com.arcane.execution.handlers.FocusFormHandler.hasFocus(inscription) && com.arcane.execution.handlers.FocusFormHandler.find(
                caster, caster.getEyePosition(), caster.getLookAngle(), com.arcane.execution.handlers.FocusFormHandler.rangeOf(inscription)) == null) {
            return ExecutionResult.fail("message.arcane.focus.no_target");
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
                PrecisionSnapshot.of(inscription),
                ExecutionContext.TriggerEvent.cast()
        );
        ExecutionResult result = SpellPipeline.INSTANCE.execute(context);
        if (result.success()) {
            Vows.pay(caster, inscription);
            if (!Traces.stealthy(inscription)) castFx(level, caster, SpellElement.of(context.plan()));
        }
        return result;
    }

    /** Som e particulas de conjuracao, visiveis para todos por perto. */
    private static void castFx(ServerLevel level, LivingEntity caster, SpellElement element) {
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                com.arcane.registry.ModSounds.SPELL_CAST.get(), net.minecraft.sounds.SoundSource.PLAYERS,
                0.8f, 0.9f + caster.getRandom().nextFloat() * 0.2f);
        ElementSounds.playCast(level, caster.position(), element);
        ModNetwork.sendShape(level, S2CShapeFxPacket.CAST, caster.position(), net.minecraft.world.phys.Vec3.ZERO, 1.5f, element.color(), element.ordinal(), 16);
        level.sendParticles(element.particle(),
                caster.getX(), caster.getY() + caster.getBbHeight() * 0.6, caster.getZ(),
                12, 0.4, 0.4, 0.4, 0.02);
    }
}
