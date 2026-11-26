package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.entity.ArcaneProjectileEntity;
import com.arcane.execution.CastingService;
import com.arcane.execution.PrecisionSnapshot;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.inscription.Inscription;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Verbo de cada forma e a regra de ordem do Mais Proximo (prompt de 26 nov, Partes A1 e A2). Mecanica so: a presenca
 * (feixe que se apaga no ar, estouro do orbe, fuste) fica para o roteiro de capturas e o olho humano.
 */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class FormVerbGameTests extends GameTestSupport {

  private static Cow cow(GameTestHelper h, int x, int z) {
    return h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(x, 1, z));
  }

  // ---------------------------------------------------------------- A1: Mais Proximo

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void nearestRightAfterRayAimsAtTheClosestCreature(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Cow behind = cow(h, 3, 1); // atras do conjurador (que olha para +Z), 2 blocos
    Cow ahead = cow(h, 3, 6);  // na mira, 3 blocos
    float b = behind.getHealth(), a = ahead.getHealth();
    h.runAfterDelay(2, () -> {
      cast(h, caster, g("ray"), g("nearest"), g("damage", "amount", 3.0));
      assertHurt(h, behind, b, "o mais proximo (fora da mira)");
      assertUnhurt(h, ahead, a, "o da mira, mais longe");
      h.succeed();
    });
  }

  @GameTest(template = "empty")
  public static void nearestAfterAnotherFilterOnlyFiltersWhatTheRayHit(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Cow behind = cow(h, 3, 1);
    Cow ahead = cow(h, 3, 6);
    float b = behind.getHealth(), a = ahead.getHealth();
    cast(h, caster, g("ray"), g("not_self"), g("nearest"), g("damage", "amount", 3.0));
    assertHurt(h, ahead, a, "o da mira (o Raio entrega so ele; o Mais Proximo so filtra)");
    assertUnhurt(h, behind, b, "o mais perto, fora da mira");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void deliveryModifiersBetweenFormAndNearestKeepTheSeek(GameTestHelper h) {
    var plan = com.arcane.inscription.InscriptionCompiler.compile(new Inscription(List.of(
        g("ray"), g("more_range"), g("nearest"), g("damage"))));
    h.assertTrue(com.arcane.execution.Seek.on(plan.delivery().parameters()), "Alcance no meio nao quebra o 'logo apos a forma'");
    h.assertTrue(plan.segments().get(0).filters().isEmpty(), "o Mais Proximo virou entrega, nao filtro");
    var filtered = com.arcane.inscription.InscriptionCompiler.compile(new Inscription(List.of(
        g("ray"), g("hostile"), g("nearest"), g("damage"))));
    h.assertTrue(!com.arcane.execution.Seek.on(filtered.delivery().parameters()), "outro filtro no meio: so filtro");
    h.assertTrue(filtered.segments().get(0).filters().size() == 2, "Hostis e Mais Proximo como filtros");
    var self = com.arcane.inscription.InscriptionCompiler.compile(new Inscription(List.of(g("self"), g("nearest"), g("heal"))));
    h.assertTrue(!com.arcane.execution.Seek.on(self.delivery().parameters()), "Eu nao mira: fica o filtro");
    long warn = countWarnings(com.arcane.execution.SpellPreview.describe(new Inscription(List.of(g("self"), g("nearest"), g("heal")))));
    h.assertTrue(warn >= 1, "a previa avisa que Eu + Mais Proximo nao muda a entrega");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void auraWithNearestHitsOnlyTheClosestOne(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Cow near = cow(h, 3, 5);
    Cow far = cow(h, 6, 6);
    float n = near.getHealth(), f = far.getHealth();
    h.runAfterDelay(2, () -> {
      cast(h, caster, g("aura", "radius", 6.0), g("nearest"), g("damage", "amount", 2.0));
      assertHurt(h, near, n, "a mais perto");
      assertUnhurt(h, far, f, "a outra dentro da aura");
      h.succeed();
    });
  }

  @GameTest(template = "empty")
  public static void nearestWithNobodyAroundFailsBeforeSpendingMana(GameTestHelper h) {
    floor(h);
    var p = Phase5GameTests.player(h, 3.5, 3.5, "touch", "nearest", "damage");
    var spell = new Inscription(List.of(g("touch"), g("nearest"), g("damage")));
    float before = GrimoireAttachments.getManaOrCreate(p).current();
    var r = CastingService.tryCast(p, spell, PrecisionSnapshot.of(spell));
    h.assertTrue(!r.success() && "message.arcane.seek.no_target".equals(r.message()), "falha sem alvo (" + r.message() + ")");
    h.assertTrue(GrimoireAttachments.getManaOrCreate(p).current() == before, "nao gastou mana");
    h.succeed();
  }

  // ---------------------------------------------------------------- A2: Raio

  @GameTest(template = "empty")
  public static void rayIntoThinAirFailsWithoutMana(GameTestHelper h) {
    floor(h);
    var p = Phase5GameTests.player(h, 3.5, 0.5, "ray", "fire");
    var spell = new Inscription(List.of(g("ray", "range", 4.0), g("fire"))); // 4 blocos de ar a frente
    float before = GrimoireAttachments.getManaOrCreate(p).current();
    var r = CastingService.tryCast(p, spell, PrecisionSnapshot.of(spell));
    h.assertTrue(!r.success() && "message.arcane.ray.miss".equals(r.message()), "Raio no ar falha (" + r.message() + ")");
    h.assertTrue(GrimoireAttachments.getManaOrCreate(p).current() == before, "nao gastou mana");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void rayDeliversNoEmptyPoint(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var spell = new Inscription(List.of(g("ray", "range", 4.0), g("fire"))); // 4 blocos de ar a frente
    var ctx = new com.arcane.execution.ExecutionContext(h.getLevel(), caster, spell,
        com.arcane.inscription.InscriptionCompiler.compile(spell), com.arcane.execution.EnvironmentalSnapshot.NEUTRAL,
        PrecisionSnapshot.of(spell), com.arcane.execution.ExecutionContext.TriggerEvent.cast());
    var spec = com.arcane.execution.DeliverySpec.fromPlan(ctx.plan().delivery());
    var got = new com.arcane.execution.handlers.RayFormHandler(true).deliver(ctx, spec).targets();
    h.assertTrue(got.points().isEmpty() && got.entities().isEmpty() && got.blocks().isEmpty(), "o Raio no ar nao entrega ponto vazio");
    h.succeed();
  }

  // ---------------------------------------------------------------- A2: Projetil

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void projectileDetonatesWhereItIsWhenItsLifeEnds(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    cast(h, caster, g("projectile"), g("fire"));
    var box = new AABB(h.absolutePos(new BlockPos(0, 0, 0))).inflate(8);
    var shots = h.getLevel().getEntitiesOfClass(ArcaneProjectileEntity.class, box);
    h.assertTrue(!shots.isEmpty(), "o orbe saiu");
    ArcaneProjectileEntity orb = shots.get(0);
    var stop = h.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1.3, 5.5));
    orb.setNoGravity(true);
    orb.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    orb.setPos(stop.x, stop.y, stop.z);
    orb.tickCount = orb.maxLife(); // ultimo tick de vida
    h.succeedWhen(() -> {
      h.assertTrue(orb.isRemoved() && orb.expired(), "o orbe detonou no fim da vida");
      h.assertBlockPresent(Blocks.FIRE, new BlockPos(3, 1, 5));
    });
  }

  // ---------------------------------------------------------------- A2: Coluna

  @GameTest(template = "empty")
  public static void columnHitsEveryCreatureInTheShaftAtAnyHeight(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Cow ground = cow(h, 3, 4);
    var high = h.spawnWithNoFreeWill(EntityType.CHICKEN, new BlockPos(3, 9, 4));
    high.setNoGravity(true);
    Cow outside = cow(h, 6, 4);
    float g0 = ground.getHealth(), h0 = high.getHealth(), o0 = outside.getHealth();
    cast(h, caster, g("pillar"), g("damage", "amount", 2.0));
    assertHurt(h, ground, g0, "a vaca no chao do fuste");
    assertHurt(h, high, h0, "a galinha 8 blocos acima, no mesmo fuste");
    assertUnhurt(h, outside, o0, "a vaca fora do raio");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void columnWithoutGroundFailsBeforeSpendingMana(GameTestHelper h) {
    floor(h);
    var p = Phase5GameTests.player(h, 3.5, 3.5, "pillar", "damage");
    p.setXRot(-90f);
    var spell = new Inscription(List.of(g("pillar"), g("damage")));
    float before = GrimoireAttachments.getManaOrCreate(p).current();
    var r = CastingService.tryCast(p, spell, PrecisionSnapshot.of(spell));
    h.assertTrue(!r.success() && "message.arcane.pillar.needs_ground".equals(r.message()), "sem chao (" + r.message() + ")");
    h.assertTrue(GrimoireAttachments.getManaOrCreate(p).current() == before, "nao gastou mana");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void columnWithNearestSettlesUnderTheClosestAndTakesTheShaftTerrain(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 1, 1);
    Cow target = cow(h, 4, 4);
    // chao duro (Cavar nao quebra) e um bloco mole 3 acima do chao, dentro do fuste e longe de qualquer mira
    for (int x = 2; x < 7; x++) for (int z = 2; z < 7; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.OBSIDIAN);
    h.setBlock(new BlockPos(5, 3, 4), Blocks.DIRT);
    float t0 = target.getHealth();
    h.runAfterDelay(2, () -> {
      cast(h, caster, g("pillar"), g("nearest"), g("damage", "amount", 2.0), g("dig"));
      assertHurt(h, target, t0, "a coluna assentou sob a vaca mais proxima");
      h.assertBlockNotPresent(Blocks.DIRT, new BlockPos(5, 3, 4));
      h.succeed();
    });
  }

  // ---------------------------------------------------------------- A2: Toque e Foco

  @GameTest(template = "empty")
  public static void touchContactStrengthensTheEffect(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Cow cow = cow(h, 3, 4);
    float before = cow.getHealth();
    cast(h, caster, g("touch"), g("damage", "amount", 4.0));
    float dealt = before - cow.getHealth();
    float want = (float) (4.0 * com.arcane.execution.ExecutionContext.touchPowerBonus());
    h.assertTrue(Math.abs(dealt - want) < 0.15f, "Toque fere " + want + " (feriu " + dealt + ")");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void touchReachesTheCreatureAndTheBlockBehindIt(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Cow cow = cow(h, 3, 4);
    h.setBlock(new BlockPos(3, 1, 5), Blocks.DIRT);
    float before = cow.getHealth();
    cast(h, caster, g("touch"), g("damage", "amount", 2.0), g("dig"));
    assertHurt(h, cow, before, "a criatura tocada");
    h.assertBlockNotPresent(Blocks.DIRT, new BlockPos(3, 1, 5));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void focusWithNearestLocksTheClosestEvenOutOfAim(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Cow behind = cow(h, 3, 1);
    float b = behind.getHealth();
    h.runAfterDelay(2, () -> {
      cast(h, caster, g("focus"), g("nearest"), g("damage", "amount", 2.0));
      assertHurt(h, behind, b, "o Foco travou na mais proxima, fora da mira");
      h.succeed();
    });
  }
}
