package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.execution.CastingService;
import com.arcane.execution.PrecisionSnapshot;
import com.arcane.execution.SpellReentry;
import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionCompiler;
import com.arcane.inscription.InscriptionValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static com.arcane.gametest.Phase5GameTests.floor;
import static com.arcane.gametest.Phase5GameTests.player;

/**
 * Balanco por simulacao (Fase F). So roda se existir {@code arcane_balance.flag} na pasta do servidor de testes
 * ({@code build/gametest-run}); sem o arquivo, passa na hora (a suite normal nao fica mais lenta).
 *
 * <p>Gera N inscricoes validas aleatorias (semente fixa: o relatorio e reprodutivel), conjura cada uma de um jogador de
 * teste contra 3 aldeoes de 1000 de vida e mede: mana paga, dano em 5 ticks e em 30 ticks (o resto e dano que demora:
 * veneno, fogo, zona), tempo de servidor da conjuracao e trechos descartados pelo orcamento de reentrancia (lacos).
 * Escreve {@code balance_report.md} na mesma pasta. Nao muda nenhum valor do jogo.
 */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class BalanceSimulationGameTests {
  private static final int N = 120, SLOT = 32, SEED = 20261125;

  private static final List<String> FORMS = List.of("touch", "projectile", "ray", "aura", "wave", "pillar");
  private static final List<String> EFFECTS = List.of("damage", "fire", "poison", "wither", "cut", "acid", "lifesteal",
      "explode", "rumble", "freeze", "slow", "curse", "electrify", "push", "gravity", "levitate", "trail", "satellites");
  private static final List<String> MODIFIERS = List.of("power", "weaken", "lengthen", "area", "repeat", "overcharge", "chain",
      "pierce", "more_range", "random");

  private record Result(String spell, int mana, float early, float total, double castMs, long dropped, String failed) {}

  @GameTest(template = "empty", batch = "balance_sim_1", timeoutTicks = N * SLOT + 200)
  public static void randomInscriptionsReport(GameTestHelper h) {
    Path flag = Path.of("arcane_balance.flag");
    if (!Files.exists(flag)) {
      h.succeed();
      return;
    }
    floor(h);
    List<String> all = new ArrayList<>(FORMS);
    all.addAll(EFFECTS);
    all.addAll(MODIFIERS);
    var p = player(h, 3.5, 0.5, all.toArray(String[]::new));
    // Explosao e afins ferem quem conjura: se o jogador de teste morre, perde o grimorio e todo o resto falha
    p.setInvulnerable(true);
    p.getAbilities().invulnerable = true;
    List<Villager> dummies = new ArrayList<>();
    for (int[] at : new int[][] {{3, 3}, {2, 4}, {4, 5}}) {
      Villager v = h.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(at[0], 1, at[1]));
      v.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
      dummies.add(v);
    }
    Random rnd = new Random(SEED);
    List<Inscription> spells = new ArrayList<>();
    int attempts = 0;
    while (spells.size() < N && attempts++ < N * 50) {
      Inscription s = random(rnd);
      if (InscriptionValidator.validate(s).valid()) spells.add(s);
    }
    List<Result> results = new ArrayList<>();
    run(h, p, dummies, spells, 0, results);
  }

  private static Inscription random(Random rnd) {
    List<GlyphInstance> glyphs = new ArrayList<>();
    glyphs.add(withParams(FORMS.get(rnd.nextInt(FORMS.size())), rnd));
    int effects = 1 + rnd.nextInt(3);
    for (int i = 0; i < effects; i++) {
      glyphs.add(withParams(EFFECTS.get(rnd.nextInt(EFFECTS.size())), rnd));
      if (rnd.nextInt(3) == 0) glyphs.add(withParams(MODIFIERS.get(rnd.nextInt(MODIFIERS.size())), rnd));
    }
    return new Inscription(glyphs);
  }

  /** Parametros sorteados entre o minimo e o maximo da bancada (o principal sempre; os outros, as vezes). */
  private static GlyphInstance withParams(String id, Random rnd) {
    ResourceLocation rl = new ResourceLocation("arcane", id);
    GlyphDefinition def = GlyphRegistry.get(rl).orElseThrow();
    Map<String, Double> params = new HashMap<>();
    String primary = def.primaryParam().orElse(null);
    def.parameters().forEach((k, spec) -> {
      if (!k.equals(primary) && rnd.nextInt(2) == 0) return;
      double v = spec.min() + rnd.nextDouble() * (spec.max() - spec.min());
      params.put(k, "int".equals(spec.type()) ? Math.rint(v) : Math.round(v * 10) / 10.0);
    });
    return new GlyphInstance(rl, params);
  }

  private static void run(GameTestHelper h, net.minecraft.server.level.ServerPlayer p, List<Villager> dummies, List<Inscription> spells,
                          int i, List<Result> results) {
    if (i >= spells.size()) {
      write(results);
      // limpa o que a simulacao deixou no mundo e nos registros globais (condutores, satelites, zonas, residuos):
      // o teste do condutor pegava um aldeao daqui como para-raios
      dummies.forEach(net.minecraft.world.entity.Entity::discard);
      com.arcane.execution.Phase4Service.clearAll();
      com.arcane.execution.Zones.clearAll();
      com.arcane.execution.WorldScars.clearAll();
      h.succeed();
      return;
    }
    Inscription s = spells.get(i);
    for (Villager v : dummies) {
      v.removeAllEffects();
      v.clearFire();
      v.setTicksFrozen(0);
      v.setHealth(v.getMaxHealth());
      v.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }
    var start = new BlockPos[] {new BlockPos(3, 1, 3), new BlockPos(2, 1, 4), new BlockPos(4, 1, 5)};
    for (int k = 0; k < dummies.size(); k++) {
      var at = h.absoluteVec(net.minecraft.world.phys.Vec3.atBottomCenterOf(start[k]));
      dummies.get(k).teleportTo(at.x, at.y, at.z);
    }
    p.setHealth(p.getMaxHealth());
    p.removeAllEffects();
    p.clearFire();
    var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(p);
    mana.setBaseMax(100000f); // o teto normal (~100) recusaria as magias caras: aqui medimos o custo, nao o limite
    mana.set(100000f);
    p.setYRot(0);
    p.setXRot(10);
    int planMana = InscriptionCompiler.compile(s).baseManaCost();
    long dropped0 = SpellReentry.dropped();
    long t0 = System.nanoTime();
    var r = CastingService.tryCast(p, s, PrecisionSnapshot.of(s));
    double ms = (System.nanoTime() - t0) / 1.0e6;
    float paid = mana.max() - mana.current(); // o que saiu da barra (com sobretaxas de campo); o plano e o custo base
    String failed = r.success() ? null : r.message();
    h.runAfterDelay(5, () -> {
      float early = damage(dummies);
      h.runAfterDelay(SLOT - 5, () -> {
        results.add(new Result(describe(s), Math.max(planMana, Math.round(paid)), early, damage(dummies), ms,
            SpellReentry.dropped() - dropped0, failed));
        run(h, p, dummies, spells, i + 1, results);
      });
    });
  }

  private static float damage(List<Villager> dummies) {
    float d = 0;
    for (Villager v : dummies) d += v.isAlive() ? v.getMaxHealth() - v.getHealth() : v.getMaxHealth();
    return d;
  }

  private static String describe(Inscription s) {
    StringBuilder b = new StringBuilder();
    for (GlyphInstance g : s.glyphs()) {
      if (b.length() > 0) b.append(" + ");
      b.append(g.glyphId().getPath());
      if (!g.parameters().isEmpty()) {
        b.append('(');
        g.parameters().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(e -> b.append(e.getKey()).append('=').append(String.format(Locale.ROOT, "%.1f", e.getValue())).append(' '));
        b.setLength(b.length() - 1);
        b.append(')');
      }
    }
    return b.toString();
  }

  private static void write(List<Result> results) {
    List<Result> ok = results.stream().filter(r -> r.failed() == null).toList();
    double[] eff = ok.stream().mapToDouble(r -> r.total() / Math.max(1, r.mana())).sorted().toArray();
    double median = eff.length == 0 ? 0 : eff[eff.length / 2];
    StringBuilder md = new StringBuilder();
    md.append("# Balanço automático (Fase F)\n\n");
    md.append("Gerado por `BalanceSimulationGameTests` (semente ").append(SEED).append(", ").append(results.size())
        .append(" inscrições válidas aleatórias). Alvo: 3 aldeões de 1000 de vida parados à frente. Dano somado nos 3. ")
        .append("**Nenhum valor do jogo foi mudado.**\n\n");
    md.append(String.format(Locale.ROOT, "- Conjurações que funcionaram: %d de %d.%n", ok.size(), results.size()));
    md.append(String.format(Locale.ROOT, "- Eficiência mediana: %.2f de dano por mana.%n%n", median));

    md.append("## Mais eficientes (dano por mana, acima de 3x a mediana)\n\n| Dano 30 t | Mana | Dano/mana | Magia |\n|---|---|---|---|\n");
    ok.stream().filter(r -> r.total() / Math.max(1, r.mana()) > 3 * median && r.total() > 0)
        .sorted((a, b) -> Double.compare(b.total() / Math.max(1, b.mana()), a.total() / Math.max(1, a.mana())))
        .limit(15).forEach(r -> md.append(String.format(Locale.ROOT, "| %.0f | %d | %.1f | %s |%n", r.total(), r.mana(), r.total() / Math.max(1, r.mana()), r.spell())));

    md.append("\n## Mais dano numa conjuração\n\n| Dano 5 t | Dano 30 t | Mana | Magia |\n|---|---|---|---|\n");
    ok.stream().sorted((a, b) -> Float.compare(b.total(), a.total())).limit(10)
        .forEach(r -> md.append(String.format(Locale.ROOT, "| %.0f | %.0f | %d | %s |%n", r.early(), r.total(), r.mana(), r.spell())));

    md.append("\n## Caras e sem efeito nos alvos (mana alta, dano zero)\n\n| Mana | Magia |\n|---|---|\n");
    ok.stream().filter(r -> r.total() == 0).sorted((a, b) -> Integer.compare(b.mana(), a.mana())).limit(10)
        .forEach(r -> md.append(String.format(Locale.ROOT, "| %d | %s |%n", r.mana(), r.spell())));

    md.append("\n## Laços e lentidão (trechos descartados ou conjuração acima de 20 ms)\n\n| Descartados | ms | Magia |\n|---|---|---|\n");
    long flagged = ok.stream().filter(r -> r.dropped() > 0 || r.castMs() > 20).count();
    ok.stream().filter(r -> r.dropped() > 0 || r.castMs() > 20).sorted((a, b) -> Double.compare(b.castMs(), a.castMs()))
        .forEach(r -> md.append(String.format(Locale.ROOT, "| %d | %.1f | %s |%n", r.dropped(), r.castMs(), r.spell())));
    if (flagged == 0) md.append("| - | - | nenhuma |\n");

    md.append("\n## Recusadas pelo jogo (válidas na gramática, mas a conjuração falhou)\n\n| Motivo | Magia |\n|---|---|\n");
    results.stream().filter(r -> r.failed() != null).limit(15).forEach(r -> md.append("| ").append(r.failed()).append(" | ").append(r.spell()).append(" |\n"));

    md.append("\n## Todas\n\n| Mana | Dano 5 t | Dano 30 t | ms | Magia |\n|---|---|---|---|---|\n");
    results.forEach(r -> md.append(String.format(Locale.ROOT, "| %d | %.0f | %.0f | %.1f | %s%s |%n", r.mana(), r.early(), r.total(),
        r.castMs(), r.spell(), r.failed() == null ? "" : " (falhou: " + r.failed() + ")")));
    try {
      Files.writeString(Path.of("balance_report.md"), md.toString(), StandardCharsets.UTF_8);
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
    System.out.println("[balanco] " + ok.size() + "/" + results.size() + " magias; relatorio em " + Path.of("balance_report.md").toAbsolutePath());
  }
}
