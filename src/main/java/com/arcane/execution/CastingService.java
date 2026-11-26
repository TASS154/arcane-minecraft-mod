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

    /** Conjurando de um grimorio alheio: o portador nao precisa conhecer os glifos. */
    private static final ThreadLocal<Boolean> FOREIGN = ThreadLocal.withInitial(() -> false);

    /** Conjura uma magia de grimorio alheio com a fracao {@code power} do poder (60% pela regra do portador). */
    public static ExecutionResult castForeign(ServerPlayer player, Inscription inscription, double power) {
        FOREIGN.set(true);
        try {
            PrecisionSnapshot base = PrecisionSnapshot.of(inscription);
            return cast(player, inscription, new PrecisionSnapshot(base.powerMultiplier() * power, base.manaCostMultiplier(), false), 1.0, false);
        } finally {
            FOREIGN.set(false);
        }
    }

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
        InscriptionValidator.ValidationResult validation = InscriptionValidator.validate(inscription, FOREIGN.get() ? Optional.empty() : grimoire);
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
        context.setAreaDial(AreaDial.factorFor(player.getUUID(), plan));
        context.setHoldTicks(CastChannels.holdTicks());

        // Coluna (Pilar): nao se manifesta no ar. Falha antes de cobrar mana.
        if (DeliverySpec.fromPlan(plan.delivery()).handler().getPath().equals("pillar")) {
            var spec = DeliverySpec.fromPlan(plan.delivery());
            double radius = Math.max(1.0, spec.parameters().getOrDefault("radius", 2.0));
            if (!Region.mayManifest(context, spec, radius)) return ExecutionResult.fail("message.arcane.pillar.needs_ground");
        }

        ManaState mana = GrimoireAttachments.getManaOrCreate(player);
        if (!free && !mana.trySpend(manaCost)) {
            return ExecutionResult.fail("message.arcane.mana.insufficient");
        }
        // Contramagia: quem viu a conjuracao e tinha "Ao Ver Conjuracao" pode anula-la (a mana ja foi gasta).
        if (!silent && com.arcane.execution.handlers.LoreEffects.Counterspell.intercept(player, manaCost)) {
            return new ExecutionResult(false, "message.arcane.counterspell.countered", manaCost);
        }

        com.arcane.training.DamageMeter.noteCast(player.getUUID(), manaCost);
        ExecutionResult result = SpellPipeline.INSTANCE.execute(context);
        if (result.success()) {
            GrimoireAttachments.get(player).ifPresent(g -> g.prefs().touch(inscription.id(), player.level().getGameTime()));
            Vows.pay(player, inscription);
            com.arcane.execution.handlers.LoreEffects.Illusion.ownerCast(player);
            if (!silent) {
                PlayerAnims.onCast(player, plan);
                ModNetwork.sendToPlayer(player, new S2CSpellFxPacket(inscription.id(), plan.fingerprint(), SpellElement.of(plan).ordinal()));
                if (!Traces.stealthy(inscription)) castFx((ServerLevel) player.level(), player, SpellElement.of(plan), inscription, false);
            }
        }
        return new ExecutionResult(result.success(), result.message(), manaCost);
    }

    /** Bonus de um ritual: nivel (1-4), poder multiplicado, escala de area e se foi lendario (sem sobretaxa). */
    public record RitualBonus(int level, double power, double areaScale, boolean legendary, double radius, net.minecraft.world.phys.Vec3 center) {
        public RitualBonus(int level, double power, double areaScale, boolean legendary) { this(level, power, areaScale, legendary, 0.0, null); }
    }

    /**
     * Ritual sem forma: o altar e a forma. Sem glifo de forma, a magia vira uma Aura no centro do circulo (que cobre o
     * circulo inteiro). Com forma (ex.: Invocar Lobos), nada muda.
     */
    public static Inscription withRitualForm(Inscription inscription) {
        boolean hasForm = inscription.glyphs().stream().anyMatch(g -> com.arcane.glyph.GlyphRegistry.get(g.glyphId())
                .map(d -> d.category() == com.arcane.glyph.GlyphCategory.FORM).orElse(false));
        if (hasForm) return inscription;
        java.util.List<com.arcane.inscription.GlyphInstance> glyphs = new java.util.ArrayList<>();
        glyphs.add(new com.arcane.inscription.GlyphInstance(com.arcane.glyph.ArcaneIds.of("aura"), java.util.Map.of()));
        glyphs.addAll(inscription.glyphs());
        return new Inscription(java.util.List.copyOf(glyphs), inscription.metadata());
    }

    /** Mana de um ritual: o custo normal; no lendario, sem a sobretaxa exponencial por tamanho. */
    public static int ritualCost(Inscription inscription, boolean legendary) {
        Inscription withForm = withRitualForm(inscription);
        double cost = CompiledPlanCache.getOrCompile(withForm).baseManaCost();
        if (legendary) cost /= com.arcane.inscription.SpellLimits.multiplier(withForm.glyphs().size());
        return (int) Math.ceil(cost);
    }

    /** Conjura dentro de um ritual (os componentes e a mana ja foram cobrados por RitualService). */
    public static ExecutionResult castRitual(LivingEntity caster, Inscription inscription, ServerLevel level, RitualBonus bonus) {
        inscription = withRitualForm(inscription);
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
        if (result.success() && !Traces.stealthy(inscription)) castFx(level, caster, SpellElement.of(context.plan()), inscription, true);
        return result;
    }

    /**
     * Conjura de um ponto e numa direcao dados (ex.: a boca de uma cabeca da Hidra), sem custo de mana: a forma sai de
     * {@code origin} na direcao {@code look}.
     */
    public static ExecutionResult castFrom(LivingEntity caster, Inscription inscription, ServerLevel level,
                                           net.minecraft.world.phys.Vec3 origin, net.minecraft.world.phys.Vec3 look) {
        CompiledExecutionPlan plan = CompiledPlanCache.getOrCompile(inscription);
        ExecutionContext context = new ExecutionContext(level, caster, inscription, plan,
                AmbientFieldResolver.resolve(level, caster.blockPosition()), PrecisionSnapshot.of(inscription), ExecutionContext.TriggerEvent.cast());
        context.freeze(origin, look, origin);
        return SpellPipeline.INSTANCE.execute(context);
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
        if (com.arcane.execution.handlers.LoreEffects.Counterspell.intercept(caster, plan.baseManaCost())) {
            return ExecutionResult.fail("message.arcane.counterspell.countered");
        }
        ExecutionResult result = SpellPipeline.INSTANCE.execute(context);
        if (result.success()) {
            Vows.pay(caster, inscription);
            if (!Traces.stealthy(inscription)) castFx(level, caster, SpellElement.of(context.plan()), inscription, false);
        }
        return result;
    }

    /** Som e particulas de conjuracao, visiveis para todos por perto. */
    /** Paleta do glifo Cor (16 cores, parametro "hue"). */
    public static final int[] PALETTE = {0xFFFFFF, 0xFF4040, 0xFF9020, 0xFFE040, 0x80FF40, 0x20C060, 0x40FFE0, 0x40A0FF,
            0x3050FF, 0x9040FF, 0xFF40E0, 0xFF80B0, 0xA06030, 0x808080, 0x202020, 0xFFD700};

    /** Cor da conjuracao: a do glifo Cor, se a magia tiver um; senao, a do elemento. */
    public static int spellColor(Inscription inscription, SpellElement element) {
        for (var g : inscription.glyphs()) {
            if (g.glyphId().getPath().equals("color")) {
                int hue = (int) Math.round(g.parameters().getOrDefault("hue", 0.0));
                return PALETTE[Math.floorMod(hue, PALETTE.length)];
            }
        }
        return element.color();
    }

    /**
     * Conjuracao: circulo de runas (pacote de forma), som do mod, espiral e faiscas do elemento ({@link com.arcane.fx.Fx}).
     * O glifo Cor troca a cor do circulo e das particulas.
     */
    /** Ultima voz de cada jogador (Fase L): no maximo 1 a cada 0,5 s; fora disso, so o som simples. */
    private static final java.util.Map<java.util.UUID, Long> LAST_VOICE = new java.util.HashMap<>();
    public static final int VOICE_GAP_TICKS = 10;

    /** A voz pode tocar agora para este conjurador? (registra se sim) */
    public static boolean voiceAllowed(java.util.UUID caster, long now) {
        Long last = LAST_VOICE.get(caster);
        if (last != null && now - last < VOICE_GAP_TICKS && now >= last) return false;
        LAST_VOICE.put(caster, now);
        if (LAST_VOICE.size() > 512) LAST_VOICE.clear();
        return true;
    }

    /** Dev (folha de contato da maestria): forca a maestria e a assinatura da proxima conjuracao. null = normal. */
    public static Double devMastery;
    public static Integer devSignature;

    /** Maestria da inscricao para quem conjura: jogador pelo grimorio; criaturas e suportes, 1 (Fase M, so aparencia). */
    public static double masteryOf(LivingEntity caster, Inscription inscription) {
        if (devMastery != null) return devMastery;
        if (!(caster instanceof net.minecraft.world.entity.player.Player player)) return 1.0;
        java.util.Map<String, Integer> levels = new java.util.HashMap<>();
        GrimoireAttachments.get(player).ifPresent(g -> g.knowledge().forEach((id, k) -> levels.put(id.getPath(), k.ordinal())));
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (var g : inscription.glyphs()) ids.add(g.glyphId().getPath());
        return com.arcane.fx.CastStyle.mastery(ids, levels, id -> com.arcane.glyph.GlyphRegistry
                .get(new net.minecraft.resources.ResourceLocation("arcane", id)).map(d -> d.cost().mana()).orElse(1));
    }

    /** Semente da assinatura (0 = nenhuma): so jogadores com estilo; o modificador Cor desliga. */
    public static int signatureOf(LivingEntity caster, boolean colorOverride) {
        if (devSignature != null) return devSignature;
        if (colorOverride || !(caster instanceof net.minecraft.world.entity.player.Player player)) return 0;
        SpellElement[] top = StyleTally.top(player);
        return top == null ? 0 : com.arcane.fx.CastStyle.seed(player.getUUID(), top[0], top[1]);
    }

    /** Voz da magia: as notas da inscricao, para o pacote de efeito. Rituais e magias grandes ganham o floreio. */
    public static S2CShapeFxPacket.Voice voiceFor(Inscription inscription, SpellElement element, boolean big) {
        return voiceFor(inscription, element, big, 1.0, 0, -1);
    }

    public static S2CShapeFxPacket.Voice voiceFor(Inscription inscription, SpellElement element, boolean big,
                                                  double mastery, int signature, int casterId) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (var g : inscription.glyphs()) ids.add(g.glyphId().getPath());
        boolean grand = big || CompiledPlanCache.getOrCompile(inscription).baseManaCost() >= BIG_SPELL_MANA;
        var notes = com.arcane.fx.SpellVoice.withMastery(com.arcane.fx.SpellVoice.compose(ids, element, 1.0, grand), mastery);
        int family = com.arcane.fx.SpellVoice.FAMILIES.indexOf(com.arcane.fx.SpellVoice.family(element));
        return new S2CShapeFxPacket.Voice(Math.max(0, family), com.arcane.fx.SpellVoice.pack(notes), (float) mastery, signature, casterId);
    }

    /** Custo a partir do qual a magia conta como grande para a voz (PROVISORIO, docs/PERGUNTAS.md). */
    public static final double BIG_SPELL_MANA = 40.0;

    private static void castFx(ServerLevel level, LivingEntity caster, SpellElement element, Inscription inscription, boolean big) {
        int color = spellColor(inscription, element);
        int override = color == element.color() ? -1 : color;
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                com.arcane.registry.ModSounds.SPELL_CAST.get(), net.minecraft.sounds.SoundSource.PLAYERS,
                0.8f, 0.9f + caster.getRandom().nextFloat() * 0.2f);
        // Fase M (so aparencia): maestria da inscricao e assinatura de quem conjura
        double mastery = masteryOf(caster, inscription);
        int signature = signatureOf(caster, override != -1);
        if (caster instanceof net.minecraft.world.entity.player.Player player) StyleTally.count(player, element);
        S2CShapeFxPacket.Voice voice = voiceAllowed(caster.getUUID(), level.getGameTime())
                ? voiceFor(inscription, element, big, mastery, signature, caster.getId())
                // sem voz (limite de 0,5 s): o campo vai so com a maestria e a assinatura (nenhuma nota)
                : new S2CShapeFxPacket.Voice(0, new long[3], (float) mastery, signature, caster.getId());
        ModNetwork.sendShape(level, S2CShapeFxPacket.CAST, caster.position(), net.minecraft.world.phys.Vec3.ZERO, 1.5f, color, element.ordinal(), 16, voice);
        com.arcane.fx.Fx.cast(level, caster, element, 1f, override, mastery);
        var hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.7)).add(0, -0.3, 0);
        com.arcane.fx.Fx.burst(level, caster, hand, element, com.arcane.fx.Fx.Role.IMPACT, 6, 0.1, 0.06, 0.7f, override);
    }
}
