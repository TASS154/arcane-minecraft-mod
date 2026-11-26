package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.execution.MassReactions;
import com.arcane.execution.ReactionJournal;
import com.arcane.execution.ReactionZones;
import com.arcane.execution.Wetness;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Fase K: Nuvem de Vapor (limiar, efeitos, limites) e Diario de descobertas por conjurador. */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class VaporGameTests extends GameTestSupport {

  static MassReactions.Def vapor() {
    var d = MassReactions.get("vapor_cloud");
    if (d == null) throw new net.minecraft.gametest.framework.GameTestAssertException("a Nuvem de Vapor nao carregou do default.json");
    return d;
  }

  /** Tres porcos molhados queimados na mesma conjuracao levantam a nuvem; o conjurador descobre Vapor e Nuvem. */
  @GameTest(template = "empty", batch = "vapor_1", timeoutTicks = 60)
  public static void threeWetTargetsBurnedRaiseACloudAndEnterTheJournal(GameTestHelper h) {
    floor(h);
    ReactionZones.clearAll();
    var p = Phase5GameTests.player(h, 3.5, 3.5, "aura", "fire");
    var other = Phase5GameTests.player(h, 0.5, 0.5);
    java.util.List<net.minecraft.world.entity.animal.Pig> pigs = new java.util.ArrayList<>();
    for (int i = 0; i < 3; i++) {
      var pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2 + i, 1, 5));
      Wetness.soak(h.getLevel(), pig, 400);
      pigs.add(pig);
    }
    var journal = ReactionJournal.get(h.getLevel().getServer());
    h.assertTrue(!journal.has(p.getUUID(), "vapor_cloud"), "comeca sem a descoberta");
    var r = Phase5GameTests.cast(h, p, g("aura"), g("fire"));
    h.assertTrue(r.success(), "conjurou: " + r.message());
    long[] when = {-1};
    h.succeedWhen(() -> {
      h.assertTrue(ReactionZones.count(h.getLevel()) >= 1, "uma nuvem (" + ReactionZones.count(h.getLevel()) + "; diario "
          + journal.of(p.getUUID()).keySet() + "; molhados " + pigs.stream().filter(Wetness::isSoaked).count()
          + "; queimando " + pigs.stream().filter(net.minecraft.world.entity.Entity::isOnFire).count()
          + "; defs " + MassReactions.defs().size() + ")");
      h.assertTrue(journal.has(p.getUUID(), "steam"), "Vapor no diario de quem conjurou");
      h.assertTrue(journal.has(p.getUUID(), "vapor_cloud"), "Nuvem de Vapor no diario de quem conjurou");
      h.assertTrue(!journal.has(other.getUUID(), "steam"), "o outro jogador, que so estava perto, nao descobriu nada");
      h.assertTrue(journal.of(p.getUUID()).get("steam").excerpt().startsWith("aura"), "trecho da inscricao guardado");
      if (when[0] < 0) {
        when[0] = journal.of(p.getUUID()).get("steam").epochMillis();
        Phase5GameTests.cast(h, p, g("aura"), g("fire"));
        h.fail("segunda conjuracao: conferir que nao regrava");
      }
      h.assertTrue(journal.of(p.getUUID()).get("steam").epochMillis() == when[0], "registra uma vez so");
      h.assertTrue(journal.of(p.getUUID()).size() == 2, "so Vapor e Nuvem (" + journal.of(p.getUUID()).keySet() + ")");
      ReactionZones.clearAll();
    });
  }

  /** Dois molhados nao bastam (o limiar e 3). Fogo fora de conjuracao (o pig queimando sozinho) nao entra no diario. */
  @GameTest(template = "empty", batch = "vapor_2", timeoutTicks = 40)
  public static void belowTheThresholdNothingHappens(GameTestHelper h) {
    floor(h);
    ReactionZones.clearAll();
    var p = Phase5GameTests.player(h, 3.5, 3.5, "aura", "fire");
    for (int i = 0; i < 2; i++) Wetness.soak(h.getLevel(), h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2 + i, 1, 5)), 400);
    Phase5GameTests.cast(h, p, g("aura"), g("fire"));
    h.runAfterDelay(5, () -> {
        h.assertTrue(ReactionZones.count(h.getLevel()) == 0, "2 vapores: sem nuvem");
    var stray = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(5, 1, 1));
    Wetness.soak(h.getLevel(), stray, 400);
    com.arcane.execution.ElementInteractions.afterHit(h.getLevel(), com.arcane.execution.SpellElement.FIRE, stray);
    h.assertTrue(ReactionZones.count(h.getLevel()) == 0, "reacao fora de conjuracao nao conta");
    h.succeed();
    });
  }

  /** A nuvem apaga fogo (criatura e bloco), molha e fere de leve; o dono nao se fere. */
  @GameTest(template = "empty", batch = "vapor_2", timeoutTicks = 40)
  public static void cloudPutsOutFireSoaksAndHurtsLightly(GameTestHelper h) {
    floor(h);
    ReactionZones.clearAll();
    var owner = Phase5GameTests.player(h, 1.5, 1.5);
    var pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
    pig.setSecondsOnFire(10);
    h.setBlock(new BlockPos(4, 1, 3), Blocks.FIRE);
    float hp = pig.getHealth();
    float ownerHp = owner.getHealth();
    var c = ReactionZones.spawn(h.getLevel(), vapor(), h.absoluteVec(new Vec3(3.5, 1.5, 3.5)), owner.getUUID());
    ReactionZones.pulse(h.getLevel(), c);
    h.assertTrue(!pig.isOnFire(), "fogo da criatura apagado");
    h.assertTrue(Wetness.isSoaked(pig), "molhado");
    float lost = hp - pig.getHealth();
    h.assertTrue(lost > 0 && lost <= 1.0f, "dano leve (" + lost + " HP)");
    h.assertTrue(owner.getHealth() == ownerHp, "o dono nao se fere");
    h.assertTrue(!h.getBlockState(new BlockPos(4, 1, 3)).is(Blocks.FIRE), "bloco de fogo apagado");
    ReactionZones.clearAll();
    h.succeed();
  }

  /** Limites: nuvens perto se fundem; teto por chunk e por mundo (alem dele, funde na mais proxima). */
  @GameTest(template = "empty", batch = "vapor_3")
  public static void cloudLimitsHold(GameTestHelper h) {
    ReactionZones.clearAll();
    var level = h.getLevel();
    var d = vapor();
    var base = h.absoluteVec(new Vec3(3.5, 2, 3.5));
    var first = ReactionZones.spawn(level, d, base, null);
    var second = ReactionZones.spawn(level, d, base.add(1, 0, 0), null);
    h.assertTrue(first == second && ReactionZones.count(level) == 1, "perto: fundiu");
    h.assertTrue(first.radius > d.radius(), "a fundida cresceu (" + first.radius + ")");
    for (int i = 0; i < 20; i++) ReactionZones.spawn(level, d, base.add((i % 2) * 7.5, 0, (i / 2 % 2) * 7.5), null);
    var chunk = new net.minecraft.world.level.ChunkPos(BlockPos.containing(base));
    long inChunk = ReactionZones.clouds().stream().filter(c -> new net.minecraft.world.level.ChunkPos(BlockPos.containing(c.center)).equals(chunk)).count();
    h.assertTrue(inChunk <= d.maxPerChunk(), "teto por chunk (" + inChunk + ")");
    for (int i = 0; i < 80; i++) ReactionZones.spawn(level, d, base.add(i * 20, 0, 0), null);
    h.assertTrue(ReactionZones.count(level) <= d.maxPerWorld(), "teto por mundo (" + ReactionZones.count(level) + ")");
    for (var c : ReactionZones.clouds()) h.assertTrue(c.radius <= ReactionZones.MAX_RADIUS, "raio no teto");
    ReactionZones.clearAll();
    h.succeed();
  }
}
