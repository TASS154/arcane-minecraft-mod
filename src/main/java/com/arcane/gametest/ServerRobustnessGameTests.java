package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.api.ProtectionProvider;
import com.arcane.execution.CastingService;
import com.arcane.execution.SpellReentry;
import com.arcane.inscription.Inscription;
import com.arcane.restore.BlockJournal;
import com.arcane.restore.JournalData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.arcane.gametest.Phase5GameTests.floor;
import static com.arcane.gametest.Phase5GameTests.g;

/** Robustez do servidor (Fase D): diario persistente, orcamento de reentrancia e protecao de terreno de outros mods. */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class ServerRobustnessGameTests {

  /** O diario do Restaurar sobrevive a salvar e carregar, com quem destruiu, e respeita o teto. */
  @GameTest(template = "empty", batch = "robust_journal_1")
  public static void restoreJournalSurvivesSaveLoadWithAuthorAndCap(GameTestHelper h) {
    var level = h.getLevel();
    floor(h);
    BlockJournal.clear(level);
    Pig breaker = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 1));
    BlockPos a = h.absolutePos(new BlockPos(3, 1, 3)), b = h.absolutePos(new BlockPos(4, 1, 3));
    level.setBlock(a, Blocks.DIRT.defaultBlockState(), 3);
    level.setBlock(b, Blocks.OAK_LOG.defaultBlockState(), 3);
    h.assertTrue(BlockJournal.destroy(level, a, breaker), "quebrou a terra");
    BlockJournal.record(level, b, Blocks.OAK_LOG.defaultBlockState()); // sem dono (a tora continua: nada a registrar)
    level.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
    BlockJournal.record(level, b, Blocks.OAK_LOG.defaultBlockState());

    CompoundTag disk = JournalData.of(level).save(new CompoundTag());
    BlockJournal.clear(level);
    var loaded = JournalData.load(level, disk);
    var entries = loaded.save(new CompoundTag()).getList("entries", 10);
    h.assertTrue(entries.size() == 2, "as duas entradas voltaram (" + entries.size() + ")");
    var first = entries.getCompound(0);
    h.assertTrue(BlockPos.of(first.getLong("p")).equals(a) && first.hasUUID("w") && first.getUUID("w").equals(breaker.getUUID()),
        "a primeira guarda a posicao e quem destruiu");
    h.assertTrue(!entries.getCompound(1).hasUUID("w"), "a segunda nao tem dono");

    // teto: 70 entradas com teto 64 salvam 64
    int cap = com.arcane.config.ArcaneConfig.RESTORE_JOURNAL_CAP.get();
    com.arcane.config.ArcaneConfig.RESTORE_JOURNAL_CAP.set(64);
    try {
      BlockJournal.clear(level);
      for (int i = 0; i < 70; i++) {
        BlockPos p = h.absolutePos(new BlockPos(i % 7, 2 + i / 49, (i / 7) % 7));
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        BlockJournal.record(level, p, Blocks.STONE.defaultBlockState());
      }
      int saved = JournalData.of(level).save(new CompoundTag()).getList("entries", 10).size();
      h.assertTrue(saved == 64, "salva no maximo o teto (" + saved + ")");
    } finally {
      com.arcane.config.ArcaneConfig.RESTORE_JOURNAL_CAP.set(cap);
      BlockJournal.clear(level);
    }
    h.succeed();
  }

  /**
   * Laco de realimentacao: criatura desenhada com Persistencia e "Aura + Dano" (o dano da propria magia conta como ataque
   * dela). O desenho dispara uma vez e para (ItemDrawings.DEPTH); e o orcamento global corta aninhamento e excesso por
   * tick para lacos entre sistemas.
   */
  @GameTest(template = "empty", batch = "robust_loop_1")
  public static void drawnCreatureFeedbackLoopIsCutByTheReentryBudget(GameTestHelper h) {
    var level = h.getLevel();
    floor(h);
    var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
    var villager = h.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(4, 1, 3));
    zombie.setInvulnerable(true);
    villager.setInvulnerable(false);
    // tinta criada agora (com "created" 0 a tinta ja nasce vencida e o desenho some sem disparar)
    var drawing = new Inscription(List.of(g("aura"), g("damage", "amount", 0.5), g("persistence")),
        new com.arcane.inscription.InscriptionMetadata("", new net.minecraft.resources.ResourceLocation("arcane", "common"),
            new net.minecraft.resources.ResourceLocation("arcane", "arcane_pen"), 1.0, java.util.Optional.empty(), level.getGameTime(),
            com.arcane.inscription.InscriptionSourceKind.ITEM));
    var data = zombie.getPersistentData();
    com.arcane.inscribing.ItemDrawings.storeIn(data, List.of(drawing));
    long dropped0 = SpellReentry.dropped();
    long t0 = System.nanoTime();
    float hp0 = villager.getHealth();
    villager.hurt(level.damageSources().mobAttack(zombie), 0.5f); // o primeiro ataque do zumbi
    double ms = (System.nanoTime() - t0) / 1.0e6;
    h.assertTrue(villager.getHealth() < hp0 - 0.5f, "o desenho disparou (vida " + hp0 + " -> " + villager.getHealth() + ")");
    h.assertTrue(SpellReentry.depth() == 0, "a profundidade volta a zero");
    h.assertTrue(ms < 2000, "o laco termina rapido (" + ms + " ms)");
    // o orcamento em si: profundidade e execucoes por tick
    int depthCap = com.arcane.config.ArcaneConfig.SPELL_MAX_DEPTH.get();
    int entered = 0;
    while (entered < depthCap + 5 && SpellReentry.enter(level)) entered++;
    for (int i = 0; i < entered; i++) SpellReentry.exit();
    h.assertTrue(entered == depthCap, "aninhar para na profundidade " + depthCap + " (" + entered + ")");
    int perTick = com.arcane.config.ArcaneConfig.SPELL_MAX_EXECUTIONS_PER_TICK.get(), ran = 0;
    for (int i = 0; i < perTick + 50; i++) {
      if (SpellReentry.enter(level)) { ran++; SpellReentry.exit(); }
    }
    h.assertTrue(ran <= perTick, "no maximo " + perTick + " trechos por tick (" + ran + ")");
    System.out.println("[reentrancia] laco do desenho: " + (SpellReentry.dropped() - dropped0) + " trechos descartados em "
        + String.format("%.1f", ms) + " ms");
    h.succeed();
  }

  /**
   * /kill numa Hidra de estagio 7 (visto em 24 nov: "nao matou, duas barras de chefe"). Ela morre na hora e some depois
   * da morte em runas (110 ticks); a barra de chefe some ja na morte. O roteiro de 24 nov invocou a Hidra seguinte antes
   * desses 5,5 s, por isso parecia viva.
   */
  @GameTest(template = "empty", batch = "robust_hydra_kill_1", timeoutTicks = 200)
  public static void killCommandKillsAStage7HydraAndItsBarGoes(GameTestHelper h) {
    var level = h.getLevel();
    var hydra = com.arcane.registry.ModEntities.HYDRA.get().create(level);
    var at = h.absoluteVec(new net.minecraft.world.phys.Vec3(3, 30, 3)); // no alto: nao destroi a area dos outros testes
    hydra.setStage(7);
    hydra.moveTo(at.x, at.y, at.z, 0f, 0f);
    level.addFreshEntity(hydra);
    hydra.setSealMask(0);
    hydra.setNoAi(true);
    hydra.kill();
    h.assertTrue(hydra.isDeadOrDying(), "morreu com /kill (vida " + hydra.getHealth() + ")");
    h.runAfterDelay(com.arcane.entity.HydraEntity.DEATH_TICKS + 15, () -> {
      h.assertTrue(hydra.isRemoved(), "sumiu depois da morte em runas");
      h.succeed();
    });
  }

  /** Um provedor de protecao de outro mod nega quebrar blocos: Cavar nao quebra; sem ele, quebra. */
  @GameTest(template = "empty", batch = "robust_protection_1")
  public static void protectionProviderCanVetoBlockBreaking(GameTestHelper h) {
    floor(h);
    Pig caster = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 1));
    caster.setYRot(0);
    caster.setXRot(0);
    BlockPos target = new BlockPos(3, 1, 2);
    ProtectionProvider deny = (level, who, pos, kind) -> kind != ProtectionProvider.Kind.BREAK;
    ProtectionProvider.register(deny);
    try {
      h.setBlock(target, Blocks.DIRT);
      CastingService.tryCast(caster, new Inscription(List.of(g("ray"), g("dig"))), h.getLevel());
      h.assertBlockPresent(Blocks.DIRT, target);
    } finally {
      ProtectionProvider.unregister(deny);
    }
    CastingService.tryCast(caster, new Inscription(List.of(g("ray"), g("dig"))), h.getLevel());
    h.assertBlockNotPresent(Blocks.DIRT, target);
    h.succeed();
  }
}
