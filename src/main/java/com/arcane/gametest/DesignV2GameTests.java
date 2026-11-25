package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.ambient.AmbientFieldResolver;
import com.arcane.entity.NPCMageEntity;
import com.arcane.execution.CastingService;
import com.arcane.execution.EnvironmentalSnapshot;
import com.arcane.glyph.GlyphCategory;
import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphHandlerRegistry;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.inscribing.InscribingService;
import com.arcane.inscribing.InscriptionAnchor;
import com.arcane.inscribing.WorldInscriptionIndex;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.item.GrimoireItem;
import com.arcane.npc.MageElement;
import com.arcane.registry.ModBlocks;
import com.arcane.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Design v2 (rituais, altar, oferendas), juramentos, Ao Morrer e cicatrizes. (dividido de ArcaneGameTests em 25 nov; os testes nao mudaram.) */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class DesignV2GameTests extends GameTestSupport {

  /** Marcar + filtro Marcados / Nao Marcados: o combo "Onda -> Marcar -> Gravidade" do design. */
  @GameTest(template = "empty")
  public static void markedAndUnmarkedFiltersSplitTheTargets(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig a = pig(h, 3, 3);
    Pig b = pig(h, 3, 4);
    cast(h, caster, g("ray"), g("mark"));
    h.assertTrue(com.arcane.execution.Marks.isMarked(a), "o primeiro alvo deveria estar marcado");
    h.assertTrue(!com.arcane.execution.Marks.isMarked(b), "o segundo nao");
    float ha = a.getHealth(), hb = b.getHealth();
    cast(h, caster, g("aura", "radius", 6.0), g("marked"), g("damage", "amount", 3.0));
    assertHurt(h, a, ha, "o marcado");
    assertUnhurt(h, b, hb, "o nao marcado");
    a.invulnerableTime = 0;
    float ha2 = a.getHealth();
    cast(h, caster, g("aura", "radius", 6.0), g("unmarked"), g("damage", "amount", 3.0));
    assertHurt(h, b, hb, "o nao marcado, agora com o outro filtro");
    assertUnhurt(h, a, ha2, "o marcado");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void gravitySlamsCreaturesAndStacks(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig target = pig(h, 3, 4);
    cast(h, caster, g("ray"), g("gravity"));
    double one = target.getDeltaMovement().y;
    h.assertTrue(one < -0.5, "deveria ser jogado para baixo, vy=" + one);
    h.assertTrue(target.fallDistance > 0, "deveria acumular queda");
    target.setDeltaMovement(0, 0, 0);
    cast(h, caster, g("ray"), g("gravity"), g("gravity"), g("gravity"));
    h.assertTrue(target.getDeltaMovement().y < one * 2, "tres Gravidade empurram bem mais que um (vy=" + target.getDeltaMovement().y + ")");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void gravityDropsSand(GameTestHelper h) {
    floor(h);
    h.setBlock(new BlockPos(3, 0, 4), Blocks.AIR);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.SAND);
    Pig caster = pig(h, 3, 1);
    cast(h, caster, g("ray"), g("gravity"));
    h.assertTrue(h.getBlockState(new BlockPos(3, 1, 4)).getBlock() != Blocks.SAND, "a areia deveria ter caido");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void attractPullsTowardTheCaster(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig target = pig(h, 3, 5);
    cast(h, caster, g("ray"), g("attract"));
    h.assertTrue(target.getDeltaMovement().z < -0.3, "deveria ser puxado para o conjurador, vz=" + target.getDeltaMovement().z);
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void cutHurtsAndClearsLeaves(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig target = pig(h, 3, 3);
    float before = target.getHealth();
    cast(h, caster, g("ray"), g("cut", "amount", 3.0));
    assertHurt(h, target, before, "o alvo do corte");
    target.discard();
    h.setBlock(new BlockPos(3, 1, 4), Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
    cast(h, caster, g("ray"), g("cut"));
    h.assertBlockNotPresent(Blocks.OAK_LEAVES, new BlockPos(3, 1, 4));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void freezeTurnsWaterToIceAndLavaToObsidian(GameTestHelper h) {
    floor(h);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.WATER);
    h.setBlock(new BlockPos(4, 1, 3), Blocks.LAVA);
    Pig caster = pig(h, 3, 3);
    cast(h, caster, g("self"), g("freeze"));
    h.assertBlockPresent(Blocks.ICE, new BlockPos(3, 1, 4));
    h.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(4, 1, 3));
    h.assertTrue(caster.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "o proprio conjurador (Eu) tambem congela");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void fireLightsTntMeltsIceAndLightsCampfires(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.TNT);
    cast(h, caster, g("ray"), g("fire"));
    h.assertBlockNotPresent(Blocks.TNT, new BlockPos(3, 1, 4));
    h.assertTrue(!h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
        new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(3, 1, 4))).inflate(3)).isEmpty(), "o TNT deveria ter sido aceso");
    // o TNT aceso (entidade) ficaria no caminho do raio
    h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
        new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(3, 1, 4))).inflate(3)).forEach(e -> e.discard());
    h.setBlock(new BlockPos(3, 1, 4), Blocks.ICE);
    cast(h, caster, g("ray"), g("fire"));
    h.assertBlockNotPresent(Blocks.ICE, new BlockPos(3, 1, 4));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void waveOnlyAffectsTheConeInFront(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Pig front = pig(h, 3, 5);
    Pig side = pig(h, 6, 3);
    Pig behind = pig(h, 3, 0);
    float f = front.getHealth(), s = side.getHealth(), b = behind.getHealth();
    cast(h, caster, g("wave"), g("damage", "amount", 3.0));
    assertHurt(h, front, f, "o alvo a frente");
    assertUnhurt(h, side, s, "o alvo ao lado");
    assertUnhurt(h, behind, b, "o alvo atras");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void pillarHitsTheColumnAroundTheAimedPoint(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    Pig aimed = pig(h, 3, 4);
    Pig neighbor = pig(h, 3, 5);
    Pig faraway = pig(h, 0, 6);
    float a = aimed.getHealth(), n = neighbor.getHealth(), f = faraway.getHealth();
    cast(h, caster, g("pillar", "radius", 2.0), g("damage", "amount", 3.0));
    assertHurt(h, aimed, a, "o alvo mirado");
    assertHurt(h, neighbor, n, "o vizinho dentro da coluna");
    assertUnhurt(h, faraway, f, "o alvo fora da coluna");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 160)
  public static void barrierRaisesAWallThatFadesAway(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    cast(h, caster, g("barrier", "duration", 40.0));
    for (int x = 2; x <= 4; x++) {
      for (int y = 1; y <= 3; y++) {
        h.assertBlockPresent(com.arcane.registry.ModBlocks.ARCANE_BARRIER.get(), new BlockPos(x, y, 4));
      }
    }
    h.succeedWhen(() -> h.assertBlockNotPresent(com.arcane.registry.ModBlocks.ARCANE_BARRIER.get(), new BlockPos(3, 2, 4)));
  }

  @GameTest(template = "empty")
  public static void alliesFilterPicksVillagersAndSparesAnimals(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Pig animal = pig(h, 3, 4);
    var villager = h.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(4, 1, 4));
    float a = animal.getHealth(), v = villager.getHealth();
    cast(h, caster, g("aura", "radius", 5.0), g("allies"), g("damage", "amount", 3.0));
    assertHurt(h, villager, v, "o aldeao (aliado)");
    assertUnhurt(h, animal, a, "o animal comum");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void woodFilterOnlyDigsWood(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.STONE);
    cast(h, caster, g("ray"), g("wood"), g("dig"));
    h.assertBlockPresent(Blocks.STONE, new BlockPos(3, 1, 4));
    h.setBlock(new BlockPos(3, 1, 4), Blocks.OAK_PLANKS);
    cast(h, caster, g("ray"), g("wood"), g("dig"));
    h.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(3, 1, 4));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void standingStillConditionGatesTheSegment(GameTestHelper h) {
    floor(h);
    Pig still = pig(h, 3, 3);
    // espera o porco assentar no chao (recem-criado ainda nao esta "onGround")
    h.runAfterDelay(10, () -> {
      cast(h, still, g("self"), g("standing_still"), g("speed"));
      h.assertTrue(still.hasEffect(MobEffects.MOVEMENT_SPEED), "parado deveria ativar");
      h.succeed();
    });
  }

  /** Magia reativa: "Eu -> Ao Sofrer Dano -> Escudo" fica armada e so dispara quando o conjurador leva dano. */
  @GameTest(template = "empty")
  public static void onHurtArmsTheSpellAndFiresOnce(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    cast(h, p, g("self"), g("on_hurt"), g("shield"));
    h.assertTrue(!p.hasEffect(MobEffects.ABSORPTION), "nao deve disparar ao conjurar");
    h.assertTrue(com.arcane.execution.ArmedSpells.armedCount(p.getUUID()) == 1, "deveria haver 1 magia armada");
    p.hurt(h.getLevel().damageSources().generic(), 1.0f);
    h.assertTrue(p.hasEffect(MobEffects.ABSORPTION), "deveria disparar ao levar dano");
    h.assertTrue(com.arcane.execution.ArmedSpells.armedCount(p.getUUID()) == 0, "dispara uma unica vez");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void armedSpellsAreCappedAtThree(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    for (int i = 0; i < 5; i++) cast(h, p, g("self"), g("on_hurt"), g("shield"));
    h.assertTrue(com.arcane.execution.ArmedSpells.armedCount(p.getUUID()) == 3, "maximo de 3 armadas");
    com.arcane.execution.ArmedSpells.clear(p.getUUID());
    h.succeed();
  }

  /** O loot por estrutura foi injetado nos baus vanilla (e so nos que o mapa lista). */
  @GameTest(template = "empty")
  public static void glyphLootIsInjectedIntoVanillaStructureChests(GameTestHelper h) {
    var loot = h.getLevel().getServer().getLootData();
    for (String path : List.of("chests/desert_pyramid", "chests/ancient_city", "chests/simple_dungeon", "chests/stronghold_library")) {
      var table = loot.getLootTable(new ResourceLocation("minecraft", path));
      h.assertTrue(table.getPool("arcane_glyphs") != null, path + " deveria ter a pool arcane_glyphs");
    }
    var untouched = loot.getLootTable(new ResourceLocation("minecraft", "chests/spawn_bonus_chest"));
    h.assertTrue(untouched.getPool("arcane_glyphs") == null, "o bau de bonus nao deve receber glifos");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void arcanePageAddsPagesToTheGrimoire(GameTestHelper h) {
    var state = new com.arcane.grimoire.GrimoireState();
    state.addPages(com.arcane.item.ArcanePageItem.PAGES_PER_ITEM);
    h.assertTrue(state.pages() == 10, "2 paginas a mais, tem " + state.pages());
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void cutShearsReadySheep(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var sheep = h.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(3, 1, 4));
    h.assertTrue(!sheep.isSheared(), "a ovelha deveria comecar com la");
    cast(h, caster, g("ray"), g("cut"));
    h.assertTrue(sheep.isSheared(), "o Corte deveria tosquiar a ovelha");
    h.succeed();
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 200)
  public static void circleLevelFollowsTheDrawnSize(GameTestHelper h) {
    arena(h);
    var none = com.arcane.ritual.RitualService.evaluate(h.getLevel(), altarAbs(h));
    h.assertTrue(none.level() == com.arcane.ritual.RitualLevel.NONE, "sem tracos nao ha circulo");

    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 4);
    var minor = com.arcane.ritual.RitualService.evaluate(h.getLevel(), altarAbs(h));
    h.assertTrue(minor.level() == com.arcane.ritual.RitualLevel.MINOR && minor.completeness() > 0.99, "raio 4 = menor, foi " + minor);

    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 8);
    h.assertTrue(com.arcane.ritual.RitualService.evaluate(h.getLevel(), altarAbs(h)).level() == com.arcane.ritual.RitualLevel.MEDIUM, "raio 8 = medio");

    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 16);
    h.assertTrue(com.arcane.ritual.RitualService.evaluate(h.getLevel(), altarAbs(h)).level() == com.arcane.ritual.RitualLevel.MAJOR, "raio 16 = maior");
    h.succeed();
  }

  @GameTest(template = "ritual_arena")
  public static void aDamagedCircleLosesItsLevel(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 4);
    var cells = new java.util.ArrayList<>(com.arcane.ritual.RitualTemplate.cells(4));
    // apaga ~30% dos tracos
    for (int i = 0; i < cells.size(); i += 3) {
      var c = cells.get(i);
      h.setBlock(new BlockPos(17 + c.dx(), 1, 17 + c.dz()), Blocks.AIR);
    }
    var report = com.arcane.ritual.RitualService.evaluate(h.getLevel(), altarAbs(h));
    h.assertTrue(report.level() == com.arcane.ritual.RitualLevel.NONE, "um circulo muito falho nao funciona: " + report);
    var result = ritual(h, pigAt(h, 17, 20), com.arcane.ritual.ComponentSource.unlimited(), false, g("self"), g("heal"));
    h.assertTrue(result.outcome() == com.arcane.ritual.RitualService.Outcome.CIRCLE_INCOMPLETE, "deveria falhar: " + result.outcome());
    h.succeed();
  }

  @GameTest(template = "ritual_arena")
  public static void ritualMultipliesPowerAndGrowsTheArea(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 4);
    Pig caster = pigAt(h, 17, 14);
    Pig near = pigAt(h, 17, 16);   // 2 blocos
    Pig mid = pigAt(h, 17, 18);    // 4 blocos: fora do raio 3 normal, dentro do 4,5 do ritual menor
    float n = near.getHealth(), m = mid.getHealth();

    var result = ritual(h, caster, com.arcane.ritual.ComponentSource.unlimited(), false, g("aura", "radius", 3.0), g("damage", "amount", 4.0));
    h.assertTrue(result.success(), "o ritual deveria funcionar: " + result.outcome());
    h.assertTrue(result.power() > 1.4 && result.power() <= 1.5, "poder do ritual menor ~1,5x, foi " + result.power());
    h.assertTrue(Math.abs((n - near.getHealth()) - 6.0f) < 0.2f, "dano 4 x 1,5 = 6, foi " + (n - near.getHealth()));
    assertHurt(h, mid, m, "o alvo a 4 blocos (a area cresceu)");
    h.succeed();
  }

  @GameTest(template = "ritual_arena")
  public static void ritualOnlyGlyphsRefuseOrdinaryCasting(GameTestHelper h) {
    arena(h);
    Pig caster = pigAt(h, 17, 14);
    var result = CastingService.tryCast(caster, new Inscription(List.of(g("summon_wolves"), g("shield"))), h.getLevel());
    h.assertTrue(!result.success() && result.message().contains("requires_ritual"), "deveria exigir ritual: " + result.message());
    h.succeed();
  }

  @GameTest(template = "ritual_arena")
  public static void summoningNeedsComponentsAndConsumesThem(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 4);
    Pig caster = pigAt(h, 17, 14);
    var spell = new GlyphInstance[] {g("summon_wolves"), g("shield")};

    var poor = stock(net.minecraft.world.item.Items.BONE, 2);
    var fail = ritual(h, caster, poor, false, spell);
    h.assertTrue(fail.outcome() == com.arcane.ritual.RitualService.Outcome.MISSING_COMPONENT, "faltam ossos: " + fail.outcome());
    h.assertTrue(poor.count(net.minecraft.world.item.Items.BONE) == 2, "nada deve ser consumido numa falha");
    h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Wolf.class,
        new net.minecraft.world.phys.AABB(altarAbs(h)).inflate(20)).isEmpty(), "nao deveria haver lobos");

    var rich = stock(net.minecraft.world.item.Items.BONE, 8);
    var ok = ritual(h, caster, rich, false, spell);
    h.assertTrue(ok.success(), "com 6 ossos deveria funcionar: " + ok.outcome());
    h.assertTrue(rich.count(net.minecraft.world.item.Items.BONE) == 2, "gasta exatamente 6 ossos");
    var wolves = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Wolf.class, new net.minecraft.world.phys.AABB(altarAbs(h)).inflate(20));
    h.assertTrue(wolves.size() == 3, "3 lobos, foram " + wolves.size());
    h.assertTrue(wolves.stream().allMatch(w -> w.hasEffect(MobEffects.ABSORPTION)), "os lobos sao os alvos do que vem depois (Escudo)");
    h.assertTrue(wolves.stream().allMatch(com.arcane.execution.Summons::isSummon), "invocacoes tem tempo de vida");
    h.succeed();
  }

  @GameTest(template = "ritual_arena")
  public static void aSmallCircleCannotCastAHigherLevelRitual(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 4);
    Pig caster = pigAt(h, 17, 14);
    var result = ritual(h, caster, com.arcane.ritual.ComponentSource.unlimited(), false, g("summon_golem"));
    h.assertTrue(result.outcome() == com.arcane.ritual.RitualService.Outcome.LEVEL_TOO_LOW, "golem exige ritual medio: " + result.outcome());
    h.succeed();
  }

  @GameTest(template = "ritual_arena")
  public static void mediumRitualRaisesAGolem(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 8);
    Pig caster = pigAt(h, 17, 14);
    var result = ritual(h, caster, stock(Blocks.IRON_BLOCK.asItem(), 2, Blocks.CARVED_PUMPKIN.asItem(), 1), false, g("summon_golem"));
    h.assertTrue(result.success(), "ritual medio com componentes: " + result.outcome());
    h.assertTrue(!h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class,
        new net.minecraft.world.phys.AABB(altarAbs(h)).inflate(20)).isEmpty(), "deveria haver um golem");
    h.succeed();
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 100)
  public static void summonsVanishWhenTheirTimeEnds(GameTestHelper h) {
    arena(h);
    Pig p = pigAt(h, 17, 14);
    com.arcane.execution.Summons.mark(p, h.getLevel().getGameTime() + 30);
    h.succeedWhen(() -> h.assertTrue(p.isRemoved(), "a invocacao deveria ter sido dispensada"));
  }

  @GameTest(template = "ritual_arena")
  public static void barrierBecomesARampartInARitual(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 4);
    Pig caster = pigAt(h, 17, 12);
    var result = ritual(h, caster, com.arcane.ritual.ComponentSource.unlimited(), false, g("barrier"));
    h.assertTrue(result.success(), "ritual de barreira: " + result.outcome());
    var barrier = com.arcane.registry.ModBlocks.ARCANE_BARRIER.get();
    h.assertBlockPresent(barrier, new BlockPos(19, 4, 15));   // 2 para o lado, 4 de altura: muralha de 5 x 5
    h.assertBlockNotPresent(barrier, new BlockPos(20, 2, 15)); // alem da largura
    h.succeed();
  }

  @GameTest(template = "ritual_arena")
  public static void storedRitualFiresWithTheActivateGlyph(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 4);
    Pig caster = pigAt(h, 17, 14);
    var be = (com.arcane.block.AltarBlockEntity) h.getLevel().getBlockEntity(altarAbs(h));
    h.assertTrue(be != null, "o altar tem BlockEntity");
    be.store(new Inscription(List.of(g("self"), g("shield"))), caster.getUUID(), false);
    h.assertTrue(!caster.hasEffect(MobEffects.ABSORPTION), "armazenar nao dispara");

    cast(h, caster, g("self"), g("activate"));
    h.assertTrue(caster.hasEffect(MobEffects.ABSORPTION), "o glifo Ativar deveria disparar o ritual armazenado");
    h.assertTrue(be.hasStored(), "a magia guardada nao some: o altar dispara de novo (depois do descanso)");
    h.assertTrue(be.cooldownUntil() > h.getLevel().getGameTime(), "o descanso do altar comeca ao disparar");
    caster.removeEffect(MobEffects.ABSORPTION);
    cast(h, caster, g("self"), g("activate"));
    h.assertTrue(!caster.hasEffect(MobEffects.ABSORPTION), "durante o descanso o altar nao dispara de novo");
    be.setCooldownUntil(0L);
    cast(h, caster, g("self"), g("activate"));
    h.assertTrue(caster.hasEffect(MobEffects.ABSORPTION), "passado o descanso, o altar dispara outra vez");

    // outro dono nao dispara
    Pig stranger = pigAt(h, 16, 14);
    be.store(new Inscription(List.of(g("self"), g("speed"))), caster.getUUID(), false);
    be.setCooldownUntil(0);
    cast(h, stranger, g("self"), g("activate"));
    h.assertTrue(!stranger.hasEffect(MobEffects.MOVEMENT_SPEED), "so o dono dispara o proprio altar");
    h.succeed();
  }

  @GameTest(template = "ritual_arena")
  public static void warpMovesARegionToTheAimedPoint(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 16);
    Pig caster = pigAt(h, 10, 10); // fora da linha do altar, senao a mira bate nele
    Pig a = pigAt(h, 8, 12);
    Pig b = pigAt(h, 12, 12);
    var source = stock(net.minecraft.world.item.Items.ENDER_PEARL, 4, Blocks.CRYING_OBSIDIAN.asItem(), 1);
    var result = ritual(h, caster, source, false, g("aura", "radius", 5.0), g("warp", "range", 12.0));
    h.assertTrue(result.success(), "ritual maior: " + result.outcome());
    var dest = net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(10, 1, 22)));
    h.assertTrue(a.position().distanceTo(dest) < 5 && b.position().distanceTo(dest) < 5,
        "os dois deveriam ter sido levados para o ponto mirado " + dest + ": a=" + a.position() + " b=" + b.position()
        + " caster=" + caster.position() + " look=" + caster.getLookAngle());
    h.succeed();
  }

  @GameTest(template = "ritual_arena")
  public static void legendaryRitualChangesTheWeatherWithAnOffering(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 16);
    Pig caster = pigAt(h, 17, 10);
    h.getLevel().setWeatherParameters(6000, 0, false, false);
    // outro teste do mesmo lote pode ter feito chover: a chuva some aos poucos, entao zera o nivel na hora
    h.getLevel().setRainLevel(0f);
    h.getLevel().setThunderLevel(0f);
    h.assertTrue(!h.getLevel().isRaining(), "comeca com tempo limpo");

    var noStar = ritual(h, caster, stock(Blocks.LIGHTNING_ROD.asItem(), 1), true, g("self"), g("weather", "mode", 2.0));
    h.assertTrue(noStar.outcome() == com.arcane.ritual.RitualService.Outcome.LEVEL_TOO_LOW, "sem a Estrela nao ha ritual lendario: " + noStar.outcome());

    var result = ritual(h, caster, stock(Blocks.LIGHTNING_ROD.asItem(), 1, net.minecraft.world.item.Items.NETHER_STAR, 1), true, g("self"), g("weather", "mode", 2.0));
    h.assertTrue(result.success() && result.effectiveLevel() == 4 && "legendary".equals(result.detail()), "ritual lendario: " + result.outcome());
    h.assertTrue(h.getLevel().getLevelData().isThundering() && h.getLevel().getLevelData().isRaining(), "deveria estar em tempestade");
    h.getLevel().setWeatherParameters(6000, 0, false, false);
    h.succeed();
  }

  @GameTest(template = "ritual_arena")
  public static void legendaryDropsTheGlyphSurchargeAndCostsTheStar(GameTestHelper h) {
    arena(h);
    var spell = new Inscription(List.of(g("self"), g("heal"), g("shield"), g("speed"), g("heal"), g("shield"), g("speed")));
    int normal = CastingService.ritualCost(spell, false);
    int legendary = CastingService.ritualCost(spell, true);
    h.assertTrue(legendary < normal, "o lendario nao paga a sobretaxa exponencial: " + legendary + " < " + normal);
    h.succeed();
  }

  @GameTest(template = "ritual_arena")
  public static void ritualFlashesTheTraces(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 4);
    var result = ritual(h, pigAt(h, 17, 14), com.arcane.ritual.ComponentSource.unlimited(), false, g("self"), g("heal"));
    h.assertTrue(result.success(), "ritual simples: " + result.outcome());
    h.assertBlockState(new BlockPos(21, 1, 17), s -> s.getBlock() == com.arcane.registry.ModBlocks.ARCANE_TRACE.get()
        && s.getValue(com.arcane.block.ArcaneTraceBlock.LIT), () -> "o traco deveria estar aceso");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void vowOfBloodCostsHealthAndRefusesOnLowHealth(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    p.setHealth(p.getMaxHealth());
    float before = p.getHealth();
    cast(h, p, g("self"), g("vow_blood"), g("speed"));
    h.assertTrue(p.getHealth() <= before - 3.9f, "o juramento deveria custar 4 de vida (vida " + p.getHealth() + ")");
    p.setHealth(5.0f);
    var low = CastingService.tryCast(p, new Inscription(List.of(g("self"), g("vow_blood"), g("shield"))), h.getLevel());
    h.assertTrue(!low.success(), "com pouca vida o juramento de sangue deve recusar");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void vowOfDaylightRefusesAtNight(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    h.getLevel().setDayTime(18000);
    var night = CastingService.tryCast(p, new Inscription(List.of(g("self"), g("vow_daylight"), g("shield"))), h.getLevel());
    h.assertTrue(!night.success(), "de noite o juramento da luz deve recusar");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void vowOfStillnessRefusesWhileMoving(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    p.xo = p.getX() - 1.0;
    var moving = CastingService.tryCast(p, new Inscription(List.of(g("self"), g("vow_stillness"), g("shield"))), h.getLevel());
    h.assertTrue(!moving.success(), "em movimento o juramento da quietude deve recusar");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void vowsRaiseDamage(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig plain = pig(h, 1, 4);
    Pig vowed = pig(h, 5, 4);
    plain.setHealth(plain.getMaxHealth());
    vowed.setHealth(vowed.getMaxHealth());
    var level = h.getLevel();
    var base = new Inscription(List.of(g("self"), g("damage")));
    var strong = new Inscription(List.of(g("self"), g("damage"), g("vow_blood")));
    var planBase = com.arcane.inscription.CompiledPlanCache.getOrCompile(base);
    var planStrong = com.arcane.inscription.CompiledPlanCache.getOrCompile(strong);
    h.assertTrue(planBase != null && planStrong != null, "os planos deveriam compilar");
    h.assertTrue(planStrong.baseManaCost() >= planBase.baseManaCost(), "o juramento nao pode baratear a magia");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void onDeathSpellFiresOnceWhenTheCasterDies(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    cast(h, p, g("self"), g("on_death"), g("shield"));
    h.assertTrue(!p.hasEffect(MobEffects.ABSORPTION), "nao deve disparar ao conjurar");
    h.assertTrue(com.arcane.execution.ArmedSpells.armedCount(p.getUUID()) == 1, "deveria estar armada");
    p.hurt(h.getLevel().damageSources().genericKill(), 1000.0f);
    h.assertTrue(com.arcane.execution.ArmedSpells.armedCount(p.getUUID()) == 0, "deveria ter disparado ao morrer");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void strongSpellLeavesAScarThatCanBeAnalyzed(GameTestHelper h) {
    floor(h);
    com.arcane.execution.WorldScars.clearAll();
    Pig caster = pig(h, 3, 1);
    var sheepTarget = h.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(3, 1, 4));
    cast(h, caster, g("ray"), g("damage"));
    var scars = com.arcane.execution.WorldScars.all(h.getLevel());
    h.assertTrue(!scars.isEmpty(), "uma magia de custo 5+ deveria deixar um residuo");
    h.assertTrue(scars.get(0).caster().equals(caster.getUUID()), "o residuo guarda a assinatura de quem conjurou");
    h.assertTrue(com.arcane.execution.WorldScars.signature(caster.getUUID()).matches("[0-9A-F]{2}-[0-9A-F]{2}"), "formato da assinatura");
    com.arcane.execution.WorldScars.clearAll();
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void frostPutsOutFireAndFireLovesFrozenTargets(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    p.setSecondsOnFire(5);
    h.assertTrue(p.isOnFire(), "deveria estar em chamas");
    com.arcane.execution.ElementInteractions.afterHit(h.getLevel(), com.arcane.execution.SpellElement.FROST, p);
    h.assertTrue(!p.isOnFire(), "o gelo deveria apagar o fogo");
    p.setTicksFrozen(60);
    double m = com.arcane.execution.ElementInteractions.damageMultiplier(h.getLevel(), com.arcane.execution.SpellElement.FIRE, p);
    h.assertTrue(m > 1.0, "fogo contra alvo congelado deveria render mais, foi " + m);
    h.succeed();
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 200)
  public static void symmetryMirrorRestoresMissingTraces(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 4);
    var cells = new java.util.ArrayList<>(com.arcane.ritual.RitualTemplate.cells(4));
    for (int i = 0; i < cells.size(); i += 5) {
      var c = cells.get(i);
      h.setBlock(new BlockPos(17 + c.dx(), 1, 17 + c.dz()), Blocks.AIR);
    }
    double before = com.arcane.ritual.RitualService.evaluate(h.getLevel(), altarAbs(h)).completeness();
    int placed = com.arcane.ritual.RitualService.repairBySymmetry(h.getLevel(), altarAbs(h));
    double after = com.arcane.ritual.RitualService.evaluate(h.getLevel(), altarAbs(h)).completeness();
    h.assertTrue(placed > 0, "o espelho deveria completar tracos");
    h.assertTrue(after > before, "a completude deveria subir: " + before + " -> " + after);
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void oathOfSilenceBlocksFurtherCastsUntilItEnds(GameTestHelper h) {
    var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
    com.arcane.execution.OathPenalties.clear(player);
    player.removeAllEffects();
    cast(h, player, g("self"), oath(2, 10), g("speed"));
    h.assertTrue(com.arcane.execution.OathPenalties.active(player, com.arcane.execution.OathPenalties.SILENCE), "silencio ativo");
    var second = CastingService.tryCast(player, new Inscription(List.of(g("self"), g("speed"))), h.getLevel());
    h.assertTrue(!second.success(), "silenciado nao pode conjurar");
    com.arcane.execution.OathPenalties.clear(player);
    var third = CastingService.tryCast(player, new Inscription(List.of(g("self"), g("speed"))), h.getLevel());
    h.assertTrue(third.success(), "sem o castigo volta a conjurar");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void oathOfImmobilityRootsAndSealedManaDrains(GameTestHelper h) {
    var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
    com.arcane.execution.OathPenalties.clear(player);
    player.removeAllEffects();
    cast(h, player, g("self"), oath(1, 20), g("speed"));
    com.arcane.execution.OathPenalties.enforce(player);
    h.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && player.hasEffect(MobEffects.JUMP), "deve estar imobilizado");
    com.arcane.execution.OathPenalties.apply(player, com.arcane.execution.OathPenalties.SEALED_MANA, 10);
    com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(player);
    com.arcane.execution.OathPenalties.enforce(player);
    h.assertTrue(com.arcane.grimoire.GrimoireAttachments.getMana(player).map(m -> m.current() == 0f).orElse(true), "mana selada zera a mana");
    com.arcane.execution.OathPenalties.clear(player);
    h.succeed();
  }
}
