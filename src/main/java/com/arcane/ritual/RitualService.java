package com.arcane.ritual;

import com.arcane.block.AltarBlockEntity;
import com.arcane.block.ArcaneAltarBlock;
import com.arcane.block.ArcaneTraceBlock;
import com.arcane.execution.CastingService;
import com.arcane.execution.ExecutionResult;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionValidator;
import com.arcane.inscription.SpellLimits;
import com.arcane.registry.ModBlocks;
import com.arcane.registry.ModParticles;
import com.arcane.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rituais (design: "circulos grandes, poder multiplicado, componentes fisicos, magias so de ritual").
 *
 * Fluxo: o jogador desenha o circulo em volta de um Altar Arcano (Compasso ou Giz). Ao usar o altar, o servidor
 *  1. mede o circulo (RitualTemplate x tracos no chao) -> nivel (raio 4/8/16 = menor/medio/maior) e completude;
 *  2. confere o que a magia exige (RitualRequirements: nivel e componentes) e a mana;
 *  3. consome componentes, conjura com poder = potencia do nivel x completude e area escalada.
 * O lendario (nivel 4) exige circulo maior + oferenda de uma Estrela do Nether e quebra a regra da sobretaxa exponencial.
 */
public final class RitualService {
  private RitualService() {}

  public enum Outcome {
    SUCCESS, STORED, NO_ALTAR, NO_SPELL, CIRCLE_INCOMPLETE, LEVEL_TOO_LOW, MISSING_COMPONENT, NO_MANA, INVALID, COOLDOWN
  }

  /** symmetry: 1 = o desenho e identico nas 8 simetrias do quadrado; cai quanto mais torto ou desigual ficar. */
  public record CircleReport(RitualLevel level, int radius, double completeness, int found, int expected, double symmetry) {
    public CircleReport(RitualLevel level, int radius, double completeness, int found, int expected) {
      this(level, radius, completeness, found, expected, 1.0);
    }

    /** Qualidade total do desenho: o que falta E o que esta fora de simetria diminuem o poder (teto: 1.0). */
    public double quality() { return Math.max(0.0, Math.min(1.0, completeness * symmetry)); }
  }

  public record Result(Outcome outcome, CircleReport circle, int effectiveLevel, String detail, int amount,
                       ExecutionResult cast, double power, double area) {
    public Result(Outcome outcome, CircleReport circle, int effectiveLevel, String detail, int amount, ExecutionResult cast) {
      this(outcome, circle, effectiveLevel, detail, amount, cast, 1.0, 1.0);
    }

    public boolean success() { return outcome == Outcome.SUCCESS || outcome == Outcome.STORED; }
  }

  // ------------------------------------------------------------------ circulo
  /** Mede o circulo em volta do altar: o maior raio (16, 8, 4) cuja completude passa do minimo; senao, o melhor relato. */
  public static CircleReport evaluate(ServerLevel level, BlockPos altar) {
    CircleReport best = new CircleReport(RitualLevel.NONE, 0, 0.0, 0, 1);
    for (int i = RitualTemplate.HAND_RADII.length - 1; i >= 0; i--) {
      int radius = RitualTemplate.HAND_RADII[i];
      // circulos enormes: confere antes os quatro pontos cardeais (barato e nao carrega chunks longe)
      if (radius > 16 && !(traceAt(level, altar, new RitualTemplate.Cell(radius, 0)) != null && traceAt(level, altar, new RitualTemplate.Cell(-radius, 0)) != null
          && traceAt(level, altar, new RitualTemplate.Cell(0, radius)) != null && traceAt(level, altar, new RitualTemplate.Cell(0, -radius)) != null)) continue;
      Set<RitualTemplate.Cell> cells = RitualTemplate.cells(radius);
      int found = 0;
      Set<RitualTemplate.Cell> present = new java.util.HashSet<>();
      for (RitualTemplate.Cell cell : cells) {
        if (traceAt(level, altar, cell) != null) { found++; present.add(cell); }
      }
      double completeness = found / (double) cells.size();
      if (completeness >= RitualSettings.minCompleteness()) {
        return new CircleReport(RitualLevel.forRadius(radius), radius, completeness, found, cells.size(), symmetry(cells, present));
      }
      if (completeness > best.completeness()) best = new CircleReport(RitualLevel.NONE, radius, completeness, found, cells.size());
    }
    return best;
  }

  /**
   * Simetria do desenho: cada celula tem ate 8 imagens (rotacoes e espelhos do quadrado). Uma orbita com algumas
   * imagens presentes e outras ausentes esta fora de simetria; conta quantas celulas destoam e converte em 0..1.
   */
  static double symmetry(Set<RitualTemplate.Cell> cells, Set<RitualTemplate.Cell> present) {
    Set<RitualTemplate.Cell> seen = new java.util.HashSet<>();
    int crooked = 0;
    for (RitualTemplate.Cell c : cells) {
      if (!seen.add(c)) continue;
      int x = c.dx(), z = c.dz();
      int[][] images = {{-x, z}, {x, -z}, {-x, -z}, {z, x}, {-z, x}, {z, -x}, {-z, -x}};
      Set<RitualTemplate.Cell> orbit = new java.util.HashSet<>();
      orbit.add(c);
      for (int[] img : images) {
        var o = new RitualTemplate.Cell(img[0], img[1]);
        if (cells.contains(o)) { orbit.add(o); seen.add(o); }
      }
      int p = 0;
      for (var o : orbit) if (present.contains(o)) p++;
      crooked += Math.min(p, orbit.size() - p);
    }
    return Math.max(0.0, 1.0 - 2.0 * crooked / Math.max(1, cells.size()));
  }

  /** Posicao do traco na celula (aceita ate 2 blocos abaixo e 2 acima do altar), ou null. */
  public static BlockPos traceAt(ServerLevel level, BlockPos altar, RitualTemplate.Cell cell) {
    for (int dy = -2; dy <= 2; dy++) {
      BlockPos pos = altar.offset(cell.dx(), dy, cell.dz());
      if (!level.hasChunkAt(pos)) return null;
      if (level.getBlockState(pos).is(ModBlocks.ARCANE_TRACE.get())) return pos;
    }
    return null;
  }

  /** Tinta comum necessaria para desenhar o circulo (1 para cada 16 tracos). */
  public static int inkCost(int radius) {
    return (int) Math.ceil(RitualTemplate.cells(radius).size() / 16.0);
  }

  /** Tinta para desenhar a mao (metade do Compasso). */
  public static int handInkCost(int radius) {
    return (int) Math.ceil(RitualTemplate.cells(radius).size() / (double) RitualTemplate.HAND_TRACES_PER_INK);
  }

  /**
   * Desenha o circulo no chao em volta do altar: cada celula recebe um traco sobre o chao firme (de 1 acima a 2 abaixo
   * do altar). Celulas sem chao (ar, agua, parede) ficam de fora. Retorna quantos tracos novos foram colocados.
   */
  public static int drawCircle(ServerLevel level, BlockPos altar, int radius) {
    int placed = 0;
    for (RitualTemplate.Cell cell : RitualTemplate.cells(radius)) if (drawCell(level, altar, cell)) placed++;
    return placed;
  }

  /** Poe o traco de UMA celula (so onde ha chao firme e espaco livre). Devolve se colocou. */
  public static boolean drawCell(ServerLevel level, BlockPos altar, RitualTemplate.Cell cell) {
    if (traceAt(level, altar, cell) != null) return false;
    BlockState trace = ModBlocks.ARCANE_TRACE.get().defaultBlockState();
    for (int dy = 1; dy >= -2; dy--) {
      BlockPos pos = altar.offset(cell.dx(), dy, cell.dz());
      if (!level.hasChunkAt(pos)) break;
      BlockState here = level.getBlockState(pos);
      boolean free = here.isAir() || (here.canBeReplaced() && here.getFluidState().isEmpty());
      if (free && trace.canSurvive(level, pos)) {
        level.setBlock(pos, trace, 3);
        return true;
      }
    }
    return false;
  }

  /**
   * Circulo PERFEITO (item do criativo): todas as celulas recebem traco, mesmo sem chao firme (um apoio de pedra e
   * posto onde faltar). Simetria 1,0 garantida.
   */
  public static int drawPerfectCircle(ServerLevel level, BlockPos altar, int radius) {
    int placed = 0;
    BlockState trace = ModBlocks.ARCANE_TRACE.get().defaultBlockState();
    for (RitualTemplate.Cell cell : RitualTemplate.cells(radius)) {
      BlockPos at = altar.offset(cell.dx(), 0, cell.dz());
      if (!level.hasChunkAt(at)) continue;
      if (traceAt(level, altar, cell) != null) continue;
      BlockPos floor = at.below();
      if (!level.getBlockState(floor).isFaceSturdy(level, floor, net.minecraft.core.Direction.UP)) {
        level.setBlock(floor, net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState(), 3);
      }
      BlockState here = level.getBlockState(at);
      if (!here.isAir() && !here.canBeReplaced()) level.destroyBlock(at, false);
      level.setBlock(at, trace, 3);
      placed++;
    }
    return placed;
  }

  private static boolean placeTraceAt(ServerLevel level, BlockPos altar, RitualTemplate.Cell cell) {
    BlockState trace = ModBlocks.ARCANE_TRACE.get().defaultBlockState();
    for (int dy = 1; dy >= -2; dy--) {
      BlockPos pos = altar.offset(cell.dx(), dy, cell.dz());
      BlockState here = level.getBlockState(pos);
      boolean free = here.isAir() || (here.canBeReplaced() && here.getFluidState().isEmpty());
      if (free && trace.canSurvive(level, pos)) {
        level.setBlock(pos, trace, 3);
        return true;
      }
    }
    return false;
  }

  /**
   * Espelho de Simetria: completa os tracos que faltam copiando os simetricos (as 8 simetrias do quadrado) de quem ja
   * foi desenhado. Nao inventa um circulo do zero: precisa de pelo menos um traco de cada tipo para refletir.
   * Retorna quantos tracos foram acrescentados.
   */
  public static int repairBySymmetry(ServerLevel level, BlockPos altar) {
    CircleReport report = evaluate(level, altar);
    if (report.radius() <= 0) return 0;
    var cells = RitualTemplate.cells(report.radius());
    int placed = 0;
    for (RitualTemplate.Cell cell : cells) {
      if (traceAt(level, altar, cell) != null) continue;
      int x = cell.dx(), z = cell.dz();
      int[][] images = {{-x, z}, {x, -z}, {-x, -z}, {z, x}, {-z, x}, {z, -x}, {-z, -x}};
      boolean mirrored = false;
      for (int[] img : images) {
        var other = new RitualTemplate.Cell(img[0], img[1]);
        if (cells.contains(other) && traceAt(level, altar, other) != null) { mirrored = true; break; }
      }
      if (mirrored && placeTraceAt(level, altar, cell)) placed++;
    }
    return placed;
  }

  // ------------------------------------------------------------------ previa (tela do altar)
  public record Need(ResourceLocation item, int need, int have) {}

  /** O que a tela do altar mostra: validade, custo, circulo, nivel exigido e as oferendas (necessario x que voce tem). */
  public record Report(boolean valid, net.minecraft.network.chat.Component message, int cost, int mana, int circleLevel,
                       int completenessPct, int needLevel, List<Need> needs, boolean legendary, int sizePct, int offeringPct, int totalPct) {}

  /**
   * Oferendas livres do altar (ate {@link OfferingValue#MAX_ITEMS} itens, qualquer um). Cada uma vale uma porcentagem
   * ({@link OfferingValue}); {@code virtual} = itens que o ComponentSource entrega (testes e o modo antigo da Estrela),
   * senao sao as pilhas vivas do jogador, que perdem 1 unidade cada ao realizar.
   */
  public record Offerings(List<net.minecraft.world.item.ItemStack> stacks, boolean virtual) {
    public static final Offerings NONE = new Offerings(List.of(), true);

    public int percent() { return OfferingValue.sum(stacks); }
    /** Alguma oferenda e item unico (e a config deixa isso quebrar o teto)? Oferenda virtual (legado) nao conta. */
    public boolean unique() { return !virtual && OfferingValue.hasUnique(stacks); }

    /** Modo antigo (boolean): uma Estrela do Nether tirada do ComponentSource, se houver. */
    static Offerings legacy(ComponentSource source, boolean wantsLegendary) {
      return wantsLegendary && source.count(Items.NETHER_STAR) >= 1
          ? new Offerings(List.of(new net.minecraft.world.item.ItemStack(Items.NETHER_STAR)), true) : NONE;
    }
  }

  private static int sizePercent(CircleReport circle) { return (int) Math.round(circle.level().power() * 100.0); }

  public static Report preview(ServerLevel level, BlockPos altar, ServerPlayer player, Inscription spell, Offerings offerings) {
    ComponentSource source = ComponentSource.of(player);
    CircleReport circle = evaluate(level, altar);
    RitualRequirements req = RitualRequirements.of(spell);
    int offeringPct = offerings.percent();
    boolean legendary = isLegendary(circle, offeringPct);
    var validation = InscriptionValidator.validate(spell, GrimoireAttachments.get(player), true);
    List<Need> needs = new ArrayList<>();
    for (Map.Entry<Item, Integer> e : neededItems(req, legendary).entrySet()) {
      needs.add(new Need(ForgeRegistries.ITEMS.getKey(e.getKey()), e.getValue(), Math.min(source.count(e.getKey()), 9999)));
    }
    int cost = spell.glyphs().isEmpty() ? 0 : CastingService.ritualCost(spell, legendary);
    return new Report(validation.valid(), validation.message(), cost, (int) GrimoireAttachments.getManaOrCreate(player).current(),
        circle.level().number(), (int) Math.round(circle.completeness() * 100), req.level(), needs, legendary,
        sizePercent(circle), offeringPct, OfferingValue.total(sizePercent(circle), offeringPct, offerings.unique()));
  }

  // ------------------------------------------------------------------ conjurar
  private static Map<Item, Integer> neededItems(RitualRequirements req, boolean legendary) {
    Map<Item, Integer> need = new LinkedHashMap<>();
    for (Map.Entry<ResourceLocation, Integer> e : req.components().entrySet()) {
      Item item = ForgeRegistries.ITEMS.getValue(e.getKey());
      if (item != null && item != Items.AIR) need.merge(item, e.getValue(), Integer::sum);
    }
    return need;
  }

  /** Lendario: as oferendas, sozinhas, passam de 300% (e ha um circulo valido). */
  private static boolean isLegendary(CircleReport circle, int offeringPercent) {
    return circle.level() != RitualLevel.NONE && OfferingValue.legendary(offeringPercent);
  }

  /** Falta alguma oferenda virtual no ComponentSource? (As vivas ja saem do inventario do jogador.) */
  private static boolean offeringsMissing(Offerings offerings, ComponentSource source) {
    if (!offerings.virtual()) return false;
    Map<Item, Integer> count = new LinkedHashMap<>();
    for (var s : offerings.stacks()) count.merge(s.getItem(), 1, Integer::sum);
    for (var e : count.entrySet()) if (source.count(e.getKey()) < e.getValue()) return true;
    return false;
  }

  private static void consumeOfferings(Offerings offerings, ComponentSource source, LivingEntity performer) {
    for (var s : offerings.stacks()) {
      if (offerings.virtual()) source.take(s.getItem(), 1);
      else if (!(performer instanceof ServerPlayer p && p.getAbilities().instabuild)) s.shrink(1);
    }
  }

  /** Confere tudo antes de consumir qualquer coisa. Retorna a falha, ou null se esta tudo certo. */
  private static Result check(LivingEntity performer, Inscription spell, ComponentSource source, CircleReport circle,
                              boolean legendary, RitualRequirements req, Map<Item, Integer> need) {
    if (circle.level() == RitualLevel.NONE) {
      return new Result(Outcome.CIRCLE_INCOMPLETE, circle, 0, "", (int) Math.round(circle.completeness() * 100), null);
    }
    int effective = legendary ? RitualLevel.LEGENDARY.number() : circle.level().number();
    if (req.level() > effective) {
      return new Result(Outcome.LEVEL_TOO_LOW, circle, effective, String.valueOf(req.level()), req.level(), null);
    }
    for (Map.Entry<Item, Integer> e : need.entrySet()) {
      if (source.count(e.getKey()) < e.getValue()) {
        return new Result(Outcome.MISSING_COMPONENT, circle, effective, e.getKey().getDescriptionId(), e.getValue(), null);
      }
    }
    if (performer instanceof ServerPlayer player) {
      var validation = InscriptionValidator.validate(spell, GrimoireAttachments.get(player), true);
      if (!validation.valid()) {
        return new Result(Outcome.INVALID, circle, effective, validation.message().getString(), 0, null);
      }
      int cost = CastingService.ritualCost(spell, legendary);
      if (GrimoireAttachments.getManaOrCreate(player).current() < cost) {
        return new Result(Outcome.NO_MANA, circle, effective, "", cost, null);
      }
    }
    return null;
  }

  public static Result perform(ServerLevel level, BlockPos altar, LivingEntity performer, Inscription spell,
                               ComponentSource source, boolean wantsLegendary) {
    return perform(level, altar, performer, spell, source, Offerings.legacy(source, wantsLegendary));
  }

  public static Result perform(ServerLevel level, BlockPos altar, LivingEntity performer, Inscription spell,
                               ComponentSource source, Offerings offerings) {
    if (!(level.getBlockState(altar).getBlock() instanceof ArcaneAltarBlock)) {
      return new Result(Outcome.NO_ALTAR, null, 0, "", 0, null);
    }
    AltarBlockEntity be = level.getBlockEntity(altar) instanceof AltarBlockEntity a ? a : null;
    if (be != null && level.getGameTime() < be.cooldownUntil()) return new Result(Outcome.COOLDOWN, null, 0, "", 0, null);

    CircleReport circle = evaluate(level, altar);
    RitualRequirements req = RitualRequirements.of(spell);
    int offeringPct = offerings.percent();
    boolean legendary = isLegendary(circle, offeringPct);
    Map<Item, Integer> need = neededItems(req, legendary);
    if (offeringsMissing(offerings, source)) return new Result(Outcome.MISSING_COMPONENT, circle, 0, "offering", 1, null);
    Result failure = check(performer, spell, source, circle, legendary, req, need);
    if (failure != null) return failure;

    need.forEach(source::take);
    consumeOfferings(offerings, source, performer);
    if (performer instanceof ServerPlayer player) {
      GrimoireAttachments.getManaOrCreate(player).trySpend(CastingService.ritualCost(spell, legendary));
    }
    int effective = legendary ? RitualLevel.LEGENDARY.number() : circle.level().number();
    double power = OfferingValue.total(sizePercent(circle), offeringPct, offerings.unique()) / 100.0 * circle.quality();
    double area = circle.level().areaScale();
    ExecutionResult cast = CastingService.castRitual(performer, spell, level,
        new CastingService.RitualBonus(effective, power, area, legendary, circle.radius(), net.minecraft.world.phys.Vec3.atCenterOf(altar)));
    if (be != null) be.setCooldownUntil(level.getGameTime() + RitualSettings.COOLDOWN_TICKS);
    RitualFx.play(level, altar, circle, legendary);
    if (performer instanceof ServerPlayer sp) com.arcane.execution.PlayerAnims.play(sp, com.arcane.execution.PlayerAnims.RITUAL);
    return new Result(Outcome.SUCCESS, circle, effective, legendary ? "legendary" : "", 0, cast, power, area);
  }

  /** Guarda a magia no altar (consumindo componentes e mana agora); o glifo Ativar a dispara depois. */
  public static Result store(ServerLevel level, BlockPos altar, ServerPlayer performer, Inscription spell,
                             ComponentSource source, boolean wantsLegendary) {
    return store(level, altar, performer, spell, source, Offerings.legacy(source, wantsLegendary));
  }

  public static Result store(ServerLevel level, BlockPos altar, ServerPlayer performer, Inscription spell,
                             ComponentSource source, Offerings offerings) {
    if (!(level.getBlockState(altar).getBlock() instanceof ArcaneAltarBlock) || !(level.getBlockEntity(altar) instanceof AltarBlockEntity be)) {
      return new Result(Outcome.NO_ALTAR, null, 0, "", 0, null);
    }
    CircleReport circle = evaluate(level, altar);
    RitualRequirements req = RitualRequirements.of(spell);
    int offeringPct = offerings.percent();
    boolean legendary = isLegendary(circle, offeringPct);
    Map<Item, Integer> need = neededItems(req, legendary);
    if (offeringsMissing(offerings, source)) return new Result(Outcome.MISSING_COMPONENT, circle, 0, "offering", 1, null);
    Result failure = check(performer, spell, source, circle, legendary, req, need);
    if (failure != null) return failure;
    need.forEach(source::take);
    consumeOfferings(offerings, source, performer);
    GrimoireAttachments.getManaOrCreate(performer).trySpend(CastingService.ritualCost(spell, legendary));
    be.store(spell, performer.getUUID(), legendary, offeringPct, offerings.unique());
    level.playSound(null, altar, ModSounds.WORKBENCH_SAVE.get(), SoundSource.BLOCKS, 1.0f, 0.7f);
    return new Result(Outcome.STORED, circle, circle.level().number(), "", 0, null);
  }

  /**
   * O glifo Ativar: dispara os altares com magia armazenada (do proprio ativador) ate 48 blocos. Cada altar
   * reavalia o circulo na hora (um circulo estragado nao funciona). Retorna quantos dispararam.
   */
  public static int triggerNearby(ServerLevel level, LivingEntity activator) {
    List<AltarBlockEntity> altars = new ArrayList<>();
    ChunkPos center = new ChunkPos(activator.blockPosition());
    int chunkRadius = RitualSettings.TRIGGER_RANGE / 16 + 1;
    double rangeSq = (double) RitualSettings.TRIGGER_RANGE * RitualSettings.TRIGGER_RANGE;
    for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
      for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
        if (chunk == null) continue;
        for (var be : chunk.getBlockEntities().values()) {
          if (be instanceof AltarBlockEntity altar && altar.hasStored() && activator.getUUID().equals(altar.owner())
              && altar.getBlockPos().distSqr(activator.blockPosition()) <= rangeSq) {
            altars.add(altar);
          }
        }
      }
    }
    int fired = 0;
    for (AltarBlockEntity altar : altars) {
      if (level.getGameTime() < altar.cooldownUntil()) continue;
      CircleReport circle = evaluate(level, altar.getBlockPos());
      if (circle.level() == RitualLevel.NONE) continue;
      Inscription spell = altar.stored();
      if (spell == null) continue;
      boolean legendary = altar.storedLegendary();
      int effective = legendary ? RitualLevel.LEGENDARY.number() : circle.level().number();
      if (RitualRequirements.of(spell).level() > effective) continue;
      double power = OfferingValue.total(sizePercent(circle), altar.storedOffering(), altar.storedUnique()) / 100.0 * circle.quality();
      // A magia guardada nao some: o altar dispara de novo a cada ativacao, pagando a mana de cada vez.
      if (activator instanceof net.minecraft.server.level.ServerPlayer sp && !sp.getAbilities().instabuild) {
        var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(sp);
        if (!mana.trySpend(CastingService.ritualCost(spell, legendary))) {
          sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.arcane.mana.insufficient"), true);
          continue;
        }
      }
      altar.setCooldownUntil(level.getGameTime() + RitualSettings.COOLDOWN_TICKS);
      CastingService.castRitual(activator, spell, level,
          new CastingService.RitualBonus(effective, power, circle.level().areaScale(), legendary, circle.radius(), net.minecraft.world.phys.Vec3.atCenterOf(altar.getBlockPos())));
      RitualFx.play(level, altar.getBlockPos(), circle, legendary);
      fired++;
    }
    return fired;
  }

  /** Efeito visual e sonoro do ritual: aneis de faiscas, coluna de luz e tracos acesos. */
  static final class RitualFx {
    private RitualFx() {}

    static void play(ServerLevel level, BlockPos altar, CircleReport circle, boolean legendary) {
      level.playSound(null, altar, ModSounds.RITUAL_ACTIVATE.get(), SoundSource.BLOCKS, 1.4f, legendary ? 0.6f : 1.0f);
      double cx = altar.getX() + 0.5, cy = altar.getY() + 0.3, cz = altar.getZ() + 0.5;
      int radius = Math.max(circle.radius(), RitualTemplate.RADII[0]);
      com.arcane.network.ModNetwork.sendShape(level, com.arcane.network.packet.S2CShapeFxPacket.RITUAL,
          new net.minecraft.world.phys.Vec3(cx, cy, cz), net.minecraft.world.phys.Vec3.ZERO, radius,
          legendary ? 0xFFD060 : 0xB48CFF, 0, legendary ? 70 : 50);
      for (int ring = 1; ring <= 3; ring++) {
        double r = radius * ring / 3.0;
        int points = (int) Math.min(72, 10 + r * 4);
        for (int i = 0; i < points; i++) {
          double a = Math.PI * 2 * i / points;
          level.sendParticles(ModParticles.ARCANE_SPARK.get(), cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r, 1, 0.0, 0.15, 0.0, 0.02);
        }
      }
      int height = legendary ? 28 : 14;
      for (int y = 0; y < height; y++) {
        level.sendParticles(ModParticles.ARCANE_SPARK.get(), cx, cy + y, cz, 2, 0.15, 0.0, 0.15, 0.0);
      }
      int lit = 0;
      for (RitualTemplate.Cell cell : RitualTemplate.cells(radius)) {
        BlockPos pos = traceAt(level, altar, cell);
        if (pos != null && lit++ < 700) ArcaneTraceBlock.glow(level, pos);
      }
    }
  }
}
