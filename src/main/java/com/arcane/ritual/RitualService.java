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

  public record CircleReport(RitualLevel level, int radius, double completeness, int found, int expected) {}

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
    for (int i = RitualTemplate.RADII.length - 1; i >= 0; i--) {
      int radius = RitualTemplate.RADII[i];
      Set<RitualTemplate.Cell> cells = RitualTemplate.cells(radius);
      int found = 0;
      for (RitualTemplate.Cell cell : cells) {
        if (traceAt(level, altar, cell) != null) found++;
      }
      double completeness = found / (double) cells.size();
      if (completeness >= RitualSettings.minCompleteness()) {
        return new CircleReport(RitualLevel.forRadius(radius), radius, completeness, found, cells.size());
      }
      if (completeness > best.completeness()) best = new CircleReport(RitualLevel.NONE, radius, completeness, found, cells.size());
    }
    return best;
  }

  /** Posicao do traco na celula (aceita ate 2 blocos abaixo e 2 acima do altar), ou null. */
  private static BlockPos traceAt(ServerLevel level, BlockPos altar, RitualTemplate.Cell cell) {
    for (int dy = -2; dy <= 2; dy++) {
      BlockPos pos = altar.offset(cell.dx(), dy, cell.dz());
      if (level.getBlockState(pos).is(ModBlocks.ARCANE_TRACE.get())) return pos;
    }
    return null;
  }

  /** Tinta comum necessaria para desenhar o circulo (1 para cada 16 tracos). */
  public static int inkCost(int radius) {
    return (int) Math.ceil(RitualTemplate.cells(radius).size() / 16.0);
  }

  /**
   * Desenha o circulo no chao em volta do altar: cada celula recebe um traco sobre o chao firme (de 1 acima a 2 abaixo
   * do altar). Celulas sem chao (ar, agua, parede) ficam de fora. Retorna quantos tracos novos foram colocados.
   */
  public static int drawCircle(ServerLevel level, BlockPos altar, int radius) {
    int placed = 0;
    BlockState trace = ModBlocks.ARCANE_TRACE.get().defaultBlockState();
    for (RitualTemplate.Cell cell : RitualTemplate.cells(radius)) {
      if (traceAt(level, altar, cell) != null) continue;
      for (int dy = 1; dy >= -2; dy--) {
        BlockPos pos = altar.offset(cell.dx(), dy, cell.dz());
        BlockState here = level.getBlockState(pos);
        boolean free = here.isAir() || (here.canBeReplaced() && here.getFluidState().isEmpty());
        if (free && trace.canSurvive(level, pos)) {
          level.setBlock(pos, trace, 3);
          placed++;
          break;
        }
      }
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

  // ------------------------------------------------------------------ conjurar
  private static Map<Item, Integer> neededItems(RitualRequirements req, boolean legendary) {
    Map<Item, Integer> need = new LinkedHashMap<>();
    for (Map.Entry<ResourceLocation, Integer> e : req.components().entrySet()) {
      Item item = ForgeRegistries.ITEMS.getValue(e.getKey());
      if (item != null && item != Items.AIR) need.merge(item, e.getValue(), Integer::sum);
    }
    if (legendary) need.merge(Items.NETHER_STAR, 1, Integer::sum);
    return need;
  }

  /** Lendario: circulo maior + oferenda, e a magia precisa disso (glifo de nivel 4 ou passa da sobretaxa gratis). */
  private static boolean isLegendary(CircleReport circle, Inscription spell, RitualRequirements req, ComponentSource source,
                                     boolean wantsLegendary) {
    return wantsLegendary && circle.level() == RitualLevel.MAJOR && source.count(Items.NETHER_STAR) >= 1
        && (req.level() >= RitualLevel.LEGENDARY.number() || spell.glyphs().size() > SpellLimits.freeGlyphs());
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
      var validation = InscriptionValidator.validate(spell, GrimoireAttachments.get(player));
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
    if (!(level.getBlockState(altar).getBlock() instanceof ArcaneAltarBlock)) {
      return new Result(Outcome.NO_ALTAR, null, 0, "", 0, null);
    }
    AltarBlockEntity be = level.getBlockEntity(altar) instanceof AltarBlockEntity a ? a : null;
    if (be != null && level.getGameTime() < be.cooldownUntil()) return new Result(Outcome.COOLDOWN, null, 0, "", 0, null);

    CircleReport circle = evaluate(level, altar);
    RitualRequirements req = RitualRequirements.of(spell);
    boolean legendary = isLegendary(circle, spell, req, source, wantsLegendary);
    Map<Item, Integer> need = neededItems(req, legendary);
    Result failure = check(performer, spell, source, circle, legendary, req, need);
    if (failure != null) return failure;

    need.forEach(source::take);
    if (performer instanceof ServerPlayer player) {
      GrimoireAttachments.getManaOrCreate(player).trySpend(CastingService.ritualCost(spell, legendary));
    }
    int effective = legendary ? RitualLevel.LEGENDARY.number() : circle.level().number();
    double power = circle.level().power() * circle.completeness();
    double area = circle.level().areaScale();
    ExecutionResult cast = CastingService.castRitual(performer, spell, level,
        new CastingService.RitualBonus(effective, power, area, legendary));
    if (be != null) be.setCooldownUntil(level.getGameTime() + RitualSettings.COOLDOWN_TICKS);
    RitualFx.play(level, altar, circle, legendary);
    if (performer instanceof ServerPlayer sp) com.arcane.execution.PlayerAnims.play(sp, com.arcane.execution.PlayerAnims.RITUAL);
    return new Result(Outcome.SUCCESS, circle, effective, legendary ? "legendary" : "", 0, cast, power, area);
  }

  /** Guarda a magia no altar (consumindo componentes e mana agora); o glifo Ativar a dispara depois. */
  public static Result store(ServerLevel level, BlockPos altar, ServerPlayer performer, Inscription spell,
                             ComponentSource source, boolean wantsLegendary) {
    if (!(level.getBlockState(altar).getBlock() instanceof ArcaneAltarBlock) || !(level.getBlockEntity(altar) instanceof AltarBlockEntity be)) {
      return new Result(Outcome.NO_ALTAR, null, 0, "", 0, null);
    }
    CircleReport circle = evaluate(level, altar);
    RitualRequirements req = RitualRequirements.of(spell);
    boolean legendary = isLegendary(circle, spell, req, source, wantsLegendary);
    Map<Item, Integer> need = neededItems(req, legendary);
    Result failure = check(performer, spell, source, circle, legendary, req, need);
    if (failure != null) return failure;
    need.forEach(source::take);
    GrimoireAttachments.getManaOrCreate(performer).trySpend(CastingService.ritualCost(spell, legendary));
    be.store(spell, performer.getUUID(), legendary);
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
      double power = circle.level().power() * circle.completeness();
      altar.clearStored();
      altar.setCooldownUntil(level.getGameTime() + RitualSettings.COOLDOWN_TICKS);
      CastingService.castRitual(activator, spell, level,
          new CastingService.RitualBonus(effective, power, circle.level().areaScale(), legendary));
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
