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

/** Formatos e origem, e o que veio depois na mesma secao: Hidra, covis, ruinas, desenhos em objetos. (dividido de ArcaneGameTests em 25 nov; os testes nao mudaram.) */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class ShapesAndWorldGameTests extends GameTestSupport {

  @GameTest(template = "ritual_arena", timeoutTicks = 100)
  public static void barrierWithDomeBuildsAHollowShellAndHealsThoseInside(GameTestHelper h) {
    for (int x = 0; x < 15; x++) for (int z = 0; z < 15; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    Pig caster = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(7, 1, 7));
    caster.setHealth(2.0f);
    caster.setYRot(0);
    caster.setXRot(0);
    var res = CastingService.tryCast(caster, new Inscription(List.of(g("barrier"), g("shape_dome"), g("heal"))), h.getLevel());
    h.assertTrue(res.success(), "falhou: " + res.message());
    int blocks = countBarrier(h, 6);
    h.assertTrue(blocks > 20, "o domo deveria ter blocos, tem " + blocks);
    h.assertTrue(blocks <= com.arcane.execution.ShapeGeometry.MAX_BLOCKS, "dentro do limite");
    h.assertTrue(!h.getLevel().getBlockState(h.absolutePos(new BlockPos(7, 1, 7))).is(com.arcane.registry.ModBlocks.ARCANE_BARRIER.get()), "oco por dentro");
    h.assertTrue(caster.getHealth() > 2.0f, "quem esta dentro do domo deveria ser curado, vida " + caster.getHealth());
    h.succeed();
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 140, batch = "domain_dome")
  public static void domainKeepsEffectsInsideUntilTheShellBreaks(GameTestHelper h) {
    for (int x = 0; x < 15; x++) for (int z = 0; z < 15; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    Pig caster = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(7, 1, 7));
    var inside = h.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(8, 1, 7));
    com.arcane.execution.Zones.clearFor(caster.getUUID());
    caster.setYRot(0);
    caster.setXRot(0);
    var res = CastingService.tryCast(caster, new Inscription(List.of(g("barrier"), g("shape_dome"), g("domain", "seconds", 30.0, "interval", 0.5), g("damage", "amount", 0.5))), h.getLevel());
    h.assertTrue(res.success(), "falhou: " + res.message());
    int shell = countBarrier(h, 6);
    h.assertTrue(shell > 20, "o domo deveria ter casca, tem " + shell);
    h.assertTrue(com.arcane.execution.Zones.activeFor(caster.getUUID()) == 1, "o Dominio conta como zona");
    float[] mark = new float[1];
    h.runAfterDelay(45, () -> {
      h.assertTrue(inside.getHealth() < inside.getMaxHealth() - 1.0f, "quem esta dentro leva o efeito continuamente (vida " + inside.getHealth() + ")");
      h.assertTrue(countBarrier(h, 6) == shell, "a casca nao se refaz nem some enquanto dura (" + countBarrier(h, 6) + " de " + shell + ")");
      int i = 0;
      for (int x = -6; x <= 6; x++) for (int y = -1; y <= 7; y++) for (int z = -6; z <= 6; z++) {
        BlockPos pos = h.absolutePos(new BlockPos(7 + x, 1 + y, 7 + z));
        if (h.getLevel().getBlockState(pos).is(com.arcane.registry.ModBlocks.ARCANE_BARRIER.get()) && i++ % 4 == 0) h.getLevel().removeBlock(pos, false);
      }
    });
    h.runAfterDelay(70, () -> {
      h.assertTrue(com.arcane.execution.Zones.activeFor(caster.getUUID()) == 0, "o Dominio acaba quando a casca quebra");
      h.assertTrue(countBarrier(h, 6) == 0, "o resto da casca se desfaz (" + countBarrier(h, 6) + ")");
      mark[0] = inside.getHealth();
    });
    h.runAfterDelay(110, () -> {
      h.assertTrue(inside.getHealth() == mark[0], "depois de quebrar, o efeito para (vida " + inside.getHealth() + " vs " + mark[0] + ")");
      h.succeed();
    });
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 60)
  public static void runicGolemStrikeSplashesNearbyEnemies(GameTestHelper h) {
    for (int x = 0; x < 15; x++) for (int z = 0; z < 15; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    var golem = com.arcane.registry.ModEntities.ARCANE_GOLEM.get().create(h.getLevel());
    var at = h.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1, 3.5));
    golem.moveTo(at.x, at.y, at.z, 0, 0);
    h.getLevel().addFreshEntity(golem);
    var a = h.spawnWithNoFreeWill(EntityType.SPIDER, new BlockPos(5, 1, 3));
    var b = h.spawnWithNoFreeWill(EntityType.SPIDER, new BlockPos(6, 1, 4));
    var far = h.spawnWithNoFreeWill(EntityType.SPIDER, new BlockPos(12, 1, 12));
    float ha = a.getHealth(), hb = b.getHealth(), hf = far.getHealth();
    golem.doHurtTarget(a);
    h.assertTrue(a.getHealth() < ha, "o alvo leva o golpe");
    h.assertTrue(b.getHealth() < hb, "o inimigo ao lado do alvo leva o impacto em area (vida " + b.getHealth() + ")");
    h.assertTrue(far.getHealth() == hf, "quem esta longe nao e atingido");
    h.succeed();
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 60)
  public static void spectralWolfPouncesBehindADistantTarget(GameTestHelper h) {
    for (int x = 0; x < 15; x++) for (int z = 0; z < 15; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    var wolf = com.arcane.registry.ModEntities.SPECTRAL_WOLF.get().create(h.getLevel());
    var w = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5));
    wolf.moveTo(w.x, w.y, w.z, 0, 0);
    h.getLevel().addFreshEntity(wolf);
    var prey = h.spawnWithNoFreeWill(EntityType.SPIDER, new BlockPos(11, 1, 11));
    wolf.setTarget(prey);
    double before = wolf.distanceTo(prey);
    h.assertTrue(before > 6.0, "comeca longe");
    wolf.tickCount = 20;
    wolf.aiStep();
    h.assertTrue(wolf.distanceTo(prey) < 3.0, "o lobo salta para perto do alvo (distancia " + wolf.distanceTo(prey) + ")");
    h.succeed();
  }

  // ================================================================== Revisao 3: tetos configuraveis e itens unicos
  @GameTest(template = "empty", batch = "rev_caps_unique")
  public static void ritualCapCanBeRemovedAndUniqueItemsBreakIt(GameTestHelper h) {
    var head = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DRAGON_HEAD);
    var egg = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DRAGON_EGG);
    var iron = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
    h.assertTrue(com.arcane.ritual.RitualSettings.isUnique(head) && com.arcane.ritual.RitualSettings.isUnique(egg) && !com.arcane.ritual.RitualSettings.isUnique(iron), "cabeca e ovo de dragao sao unicos; ferro nao");
    h.assertTrue(com.arcane.ritual.OfferingValue.hasUnique(java.util.List.of(iron, head)) && !com.arcane.ritual.OfferingValue.hasUnique(java.util.List.of(iron)), "a lista detecta o item unico");
    com.arcane.ritual.RitualSettings.setMaxPercent(1500);
    h.assertTrue(com.arcane.ritual.OfferingValue.total(1000, 900, false) == 1500, "sem item unico o teto vale");
    h.assertTrue(com.arcane.ritual.OfferingValue.total(1000, 900, true) == 3800, "com item unico o teto some: (1000+900)x2");
    com.arcane.ritual.RitualSettings.setUnique(false, java.util.List.of("minecraft:dragon_head"));
    h.assertTrue(com.arcane.ritual.OfferingValue.total(1000, 900, true) == 1500, "config desligada: o unico nao quebra o teto");
    h.assertTrue(com.arcane.ritual.RitualSettings.isUnique(head) && !com.arcane.ritual.RitualSettings.isUnique(egg), "a lista de unicos e configuravel");
    com.arcane.ritual.RitualSettings.setMaxPercent(0);
    h.assertTrue(com.arcane.ritual.OfferingValue.total(1000, 900, false) == 3800, "teto 0 = sem limite");
    com.arcane.ritual.RitualSettings.reset();
    h.succeed();
  }

  @GameTest(template = "empty", batch = "rev_world_rules")
  public static void worldRulesCycleThroughTheStepsAndOnlyAdminsEdit(GameTestHelper h) {
    var steps = new double[] {500, 1000, 1500, 0};
    h.assertTrue(com.arcane.config.WorldRules.next(500, steps) == 1000 && com.arcane.config.WorldRules.next(1500, steps) == 0, "avanca nos passos; o ultimo e sem limite");
    h.assertTrue(com.arcane.config.WorldRules.next(0, steps) == 500, "depois do sem limite volta ao primeiro");
    var snap = com.arcane.config.WorldRules.snapshot();
    h.assertTrue(snap.containsKey("ritual_cap") && snap.containsKey("hard_caps") && snap.containsKey("unique_break"), "o estado traz todas as regras");
    var player = casterWith(h);
    h.assertTrue(!com.arcane.config.WorldRules.canEdit(player), "jogador comum nao edita as regras do mundo");
    int before = com.arcane.config.ArcaneConfig.RITUAL_MAX_PERCENT.get();
    boolean hard = com.arcane.config.ArcaneConfig.AREA_HARD_CAPS.get();
    h.assertTrue(com.arcane.config.WorldRules.advance("hard_caps") && com.arcane.config.ArcaneConfig.AREA_HARD_CAPS.get() == !hard, "regra booleana inverte");
    com.arcane.config.ArcaneConfig.AREA_HARD_CAPS.set(hard);
    com.arcane.config.ArcaneConfig.RITUAL_MAX_PERCENT.set(10000);
    com.arcane.config.WorldRules.advance("ritual_cap");
    h.assertTrue(com.arcane.config.ArcaneConfig.RITUAL_MAX_PERCENT.get() == 0 && com.arcane.ritual.RitualSettings.maxPercent() == Integer.MAX_VALUE, "o ultimo passo remove o teto e vale na hora");
    h.assertTrue(!com.arcane.config.WorldRules.advance("nao_existe"), "chave desconhecida e ignorada");
    com.arcane.config.ArcaneConfig.RITUAL_MAX_PERCENT.set(before);
    com.arcane.ritual.RitualSettings.reset();
    h.succeed();
  }

  @GameTest(template = "empty", batch = "rev_pillar_sky")
  public static void pillarAimedAtACreatureInTheSkyLandsOnTheGroundBelowIt(GameTestHelper h) {
    floor(h);
    Pig caster = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 14, 1));
    caster.setNoGravity(true);
    Pig flyer = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 14, 5));
    flyer.setNoGravity(true);
    float before = flyer.getHealth();
    cast(h, caster, g("pillar", "radius", 2.0), g("damage", "amount", 3.0));
    assertHurt(h, flyer, before, "a criatura no ceu, no meio da mira");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "rev_pillar_down")
  public static void pillarLookedDownFromAFarHeightFindsTheGround(GameTestHelper h) {
    floor(h);
    // o terreno natural do mundo de teste fica acima da estrutura: abre uma coluna de ar ate o alto
    for (int y = 1; y <= 92; y++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
      h.getLevel().setBlock(h.absolutePos(new BlockPos(3 + dx, y, 3 + dz)), Blocks.AIR.defaultBlockState(), 2);
    }
    Pig caster = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 90, 3));
    caster.setNoGravity(true);
    Pig below = pig(h, 3, 3);
    float before = below.getHealth();
    caster.setYRot(0);
    caster.setXRot(90);
    var result = CastingService.tryCast(caster, new Inscription(List.of(g("pillar", "radius", 2.0), g("damage", "amount", 3.0))), h.getLevel());
    h.assertTrue(result.success(), "a conjuracao falhou: " + result.message());
    assertHurt(h, below, before, "quem esta no chao la embaixo (mais de 64 blocos)");
    h.assertTrue(com.arcane.config.ArcaneConfig.RAY_RANGE.get() >= 512, "o raio e pseudo-infinito por padrao");
    h.succeed();
  }

  @GameTest(template = "ritual_arena", batch = "compass_slow", timeoutTicks = 160)
  public static void compassDrawsTheCircleOverTimeAndChargesInkAtTheEnd(GameTestHelper h) {
    arena(h);
    var player = casterWith(h);
    player.getInventory().add(new net.minecraft.world.item.ItemStack(com.arcane.registry.ModItems.COMMON_INK.get(), 5));
    int duration = com.arcane.ritual.CompassDrawing.durationTicks(4);
    h.assertTrue(duration > 20, "o desenho nao e instantaneo (" + duration + " ticks)");
    h.assertTrue(com.arcane.ritual.CompassDrawing.start(player, h.getLevel(), altarAbs(h), 4), "comecou");
    h.assertTrue(!com.arcane.ritual.CompassDrawing.start(player, h.getLevel(), altarAbs(h), 4), "nao comeca outro enquanto desenha");
    h.assertTrue(traceCount(h, 4) == 0, "logo no inicio ainda nao ha tracos");
    h.runAfterDelay(duration / 2, () -> {
      int half = traceCount(h, 4);
      int all = com.arcane.ritual.RitualTemplate.cells(4).size();
      h.assertTrue(half > 0 && half < all, "no meio do tempo o circulo esta pela metade (" + half + " de " + all + ")");
      h.assertTrue(com.arcane.ritual.CompassDrawing.countInk(player) == 5, "a tinta so e gasta no fim");
    });
    h.runAfterDelay(duration + 10, () -> {
      var eval = com.arcane.ritual.RitualService.evaluate(h.getLevel(), altarAbs(h));
      h.assertTrue(eval.level() == com.arcane.ritual.RitualLevel.MINOR && eval.completeness() > 0.99, "no fim o circulo esta completo: " + eval);
      h.assertTrue(com.arcane.ritual.CompassDrawing.countInk(player) < 5, "gastou tinta ao terminar");
      h.assertTrue(!com.arcane.ritual.CompassDrawing.isDrawing(player.getUUID()), "a sessao terminou");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "ink_expiry")
  public static void inksFadeWithTheDaysButPermanentOnesDoNot(GameTestHelper h) {
    var level = h.getLevel();
    long now = level.getGameTime();
    long day = com.arcane.inscribing.InkRegistry.DAY_TICKS;
    // Datas de criacao negativas simulam desenhos antigos (o relogio do teste parte de perto de zero).
    BlockPos[] at = {h.absolutePos(new BlockPos(1, 1, 1)), h.absolutePos(new BlockPos(3, 1, 1)), h.absolutePos(new BlockPos(5, 1, 1)),
        h.absolutePos(new BlockPos(1, 1, 3)), h.absolutePos(new BlockPos(3, 1, 3)), h.absolutePos(new BlockPos(5, 1, 3))};
    var up = net.minecraft.core.Direction.UP;
    var index = WorldInscriptionIndex.get(level);
    index.put(drawing(at[0], "common", now - 11 * day, up));      // passou de 10 dias
    index.put(drawing(at[1], "common", now - 9 * day, up));       // ainda vale
    index.put(drawing(at[2], "invisible", now - 8 * day, up));    // passou de 7 dias
    index.put(drawing(at[3], "blood", now - 19 * day, up));       // sangue dura 20
    index.put(drawing(at[4], "permanent", now - 5000 * day, up)); // nunca
    index.put(drawing(at[5], "common_perm", now - 5000 * day, up)); // misturada: nunca
    InscribingService.tickWorld(level, false);
    h.assertTrue(index.at(at[0]).isEmpty(), "tinta comum com 11 dias se desgastou");
    h.assertTrue(!index.at(at[1]).isEmpty(), "tinta comum com 9 dias ainda vale");
    h.assertTrue(index.at(at[2]).isEmpty(), "tinta invisivel com 8 dias se desgastou (dura 7)");
    h.assertTrue(!index.at(at[3]).isEmpty(), "tinta de sangue com 19 dias ainda vale (dura 20)");
    h.assertTrue(!index.at(at[4]).isEmpty(), "tinta permanente nunca se desgasta");
    h.assertTrue(!index.at(at[5]).isEmpty(), "tinta comum misturada com permanente nunca se desgasta");
    for (BlockPos pos : at) index.remove(pos);
    h.succeed();
  }

  @GameTest(template = "empty", batch = "solvent")
  public static void solventErasesEveryFaceAtOnceExceptPurePermanentInk(GameTestHelper h) {
    var level = h.getLevel();
    BlockPos abs = h.absolutePos(new BlockPos(3, 1, 3));
    var index = WorldInscriptionIndex.get(level);
    index.put(drawing(abs, "common", level.getGameTime(), net.minecraft.core.Direction.UP));
    index.put(drawing(abs, "common_perm", level.getGameTime(), net.minecraft.core.Direction.NORTH));
    index.put(drawing(abs, "blood", level.getGameTime(), net.minecraft.core.Direction.EAST));
    h.assertTrue(index.at(abs).size() == 3, "um desenho por face: tres faces, tres desenhos");
    h.assertTrue(InscribingService.dissolve(level, abs) == InscribingService.Dissolve.ERASED, "tudo se dissolve");
    h.assertTrue(index.at(abs).isEmpty(), "o po apagou todas as faces de uma vez, tambem a mistura com permanente");

    index.put(drawing(abs, "common", level.getGameTime(), net.minecraft.core.Direction.UP));
    index.put(drawing(abs, "permanent", level.getGameTime(), net.minecraft.core.Direction.SOUTH));
    h.assertTrue(InscribingService.dissolve(level, abs) == InscribingService.Dissolve.PERMANENT_REMAINS, "a permanente pura fica");
    h.assertTrue(index.at(abs).size() == 1 && index.at(abs).get(0).face() == net.minecraft.core.Direction.SOUTH, "so a permanente pura restou");
    h.assertTrue(InscribingService.dissolve(level, abs.above(5)) == InscribingService.Dissolve.NOTHING, "bloco sem desenho: nada");
    index.remove(abs);
    h.succeed();
  }

  @GameTest(template = "empty", batch = "faces")
  public static void aBlockHoldsOneDrawingPerFaceAndRewritingAFaceReplacesIt(GameTestHelper h) {
    var level = h.getLevel();
    BlockPos abs = h.absolutePos(new BlockPos(3, 1, 3));
    var index = WorldInscriptionIndex.get(level);
    for (var face : net.minecraft.core.Direction.values()) index.put(drawing(abs, "common", level.getGameTime(), face));
    h.assertTrue(index.at(abs).size() == 6, "seis faces, seis desenhos");
    index.put(drawing(abs, "blood", level.getGameTime(), net.minecraft.core.Direction.UP));
    h.assertTrue(index.at(abs).size() == 6, "reescrever a mesma face troca o desenho, nao soma");
    var reloaded = WorldInscriptionIndex.load(index.save(new CompoundTag()));
    h.assertTrue(reloaded.at(abs).size() == 6, "os seis sobrevivem ao salvar e carregar");
    h.assertTrue(reloaded.get(abs, net.minecraft.core.Direction.UP).orElseThrow().ink().getPath().equals("blood"), "a face reescrita guarda a tinta nova");
    index.remove(abs);
    h.succeed();
  }

  @GameTest(template = "empty", batch = "block_pens")
  public static void eachBlockPenWritesOnlyOnItsOwnMaterial(GameTestHelper h) {
    var level = h.getLevel();
    var player = casterWith(h);
    BlockPos at = h.absolutePos(new BlockPos(3, 1, 3));
    java.util.Map<String, net.minecraft.world.level.block.Block[]> own = java.util.Map.of(
        "earth", new net.minecraft.world.level.block.Block[] {Blocks.DIRT, Blocks.SAND, Blocks.GRAVEL, Blocks.GRASS_BLOCK},
        "stone", new net.minecraft.world.level.block.Block[] {Blocks.STONE, Blocks.COBBLESTONE, Blocks.IRON_ORE, Blocks.STONE_BRICKS, Blocks.DEEPSLATE},
        "metal", new net.minecraft.world.level.block.Block[] {Blocks.IRON_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.ANVIL, Blocks.OBSIDIAN},
        "snow", new net.minecraft.world.level.block.Block[] {Blocks.SNOW_BLOCK, Blocks.ICE, Blocks.PACKED_ICE},
        "wood", new net.minecraft.world.level.block.Block[] {Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.SPRUCE_PLANKS});
    java.util.Map<String, net.minecraft.world.item.Item> pens = java.util.Map.of(
        "earth", com.arcane.registry.ModItems.EARTH_PEN.get(), "stone", com.arcane.registry.ModItems.STONE_PEN.get(),
        "metal", com.arcane.registry.ModItems.METAL_PEN.get(), "snow", com.arcane.registry.ModItems.SNOW_PEN.get(),
        "wood", com.arcane.registry.ModItems.WOOD_PEN.get());
    for (var pen : pens.entrySet()) {
      player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(pen.getValue()));
      for (var group : own.entrySet()) {
        for (var block : group.getValue()) {
          level.setBlock(at, block.defaultBlockState(), 3);
          boolean can = com.arcane.inscribing.PenRegistry.canWriteOnBlock(player, at, level);
          h.assertTrue(can == group.getKey().equals(pen.getKey()), "caneta de " + pen.getKey() + " em " + block + (can ? ": nao devia escrever" : ": devia escrever"));
        }
      }
    }
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.arcane.registry.ModItems.ARCANE_PEN.get()));
    level.setBlock(at, Blocks.STONE.defaultBlockState(), 3);
    h.assertTrue(!com.arcane.inscribing.PenRegistry.canWriteOnBlock(player, at, level), "a Pena Arcana comum nao escreve em blocos");
    level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
    h.succeed();
  }

  @GameTest(template = "empty", batch = "pen_menu_valid")
  public static void penMenuStaysOpenWithABlockPenInHand(GameTestHelper h) {
    var player = casterWith(h);
    BlockPos at = h.absolutePos(new BlockPos(3, 1, 3));
    for (var pen : List.of(com.arcane.registry.ModItems.ARCANE_PEN.get(), com.arcane.registry.ModItems.STONE_PEN.get(), com.arcane.registry.ModItems.WOOD_PEN.get())) {
      player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(pen));
      var menu = new com.arcane.menu.ArcaneWorkbenchMenu(1, player.getInventory(), at, com.arcane.menu.ArcaneWorkbenchMenu.BLOCK);
      h.assertTrue(menu.stillValid(player), "o menu de escrever em bloco nao pode fechar com " + pen);
    }
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
    h.assertTrue(!new com.arcane.menu.ArcaneWorkbenchMenu(2, player.getInventory(), at, com.arcane.menu.ArcaneWorkbenchMenu.BLOCK).stillValid(player), "sem caneta o menu fecha");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "consume_trap", timeoutTicks = 120)
  public static void aWorldDrawingFadesAfterFiringUnlessItHasPersistence(GameTestHelper h) {
    floor(h);
    var level = h.getLevel();
    BlockPos plain = new BlockPos(2, 1, 2), kept = new BlockPos(5, 1, 5);
    h.setBlock(plain, Blocks.STONE);
    h.setBlock(kept, Blocks.STONE);
    BlockPos plainAbs = h.absolutePos(plain), keptAbs = h.absolutePos(kept);
    var index = WorldInscriptionIndex.get(level);
    var common = new ResourceLocation("arcane", "common");
    index.put(new InscriptionAnchor(plainAbs, new Inscription(List.of(g("self"), g("damage", "amount", 1.0))), InscribingService.TRIGGER_ON_TOUCH, common));
    index.put(new InscriptionAnchor(keptAbs, new Inscription(List.of(g("self"), g("persistence"), g("damage", "amount", 1.0))), InscribingService.TRIGGER_ON_TOUCH, common));
    Pig a = h.spawnWithNoFreeWill(EntityType.PIG, plain.above());
    Pig b = h.spawnWithNoFreeWill(EntityType.PIG, kept.above());
    h.runAfterDelay(60, () -> {
      h.assertTrue(a.getHealth() < a.getMaxHealth(), "o primeiro porco foi ferido");
      h.assertTrue(b.getHealth() < b.getMaxHealth(), "o segundo porco foi ferido");
      h.assertTrue(index.at(plainAbs).isEmpty(), "sem Persistencia o desenho some depois de disparar");
      h.assertTrue(!index.at(keptAbs).isEmpty(), "com Persistencia o desenho continua");
      index.remove(keptAbs);
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "near_trigger", timeoutTicks = 120)
  public static void proximityDrawingFiresWhenACreatureComesNear(GameTestHelper h) {
    floor(h);
    var level = h.getLevel();
    BlockPos stone = new BlockPos(1, 1, 1);
    h.setBlock(stone, Blocks.STONE);
    BlockPos abs = h.absolutePos(stone);
    var index = WorldInscriptionIndex.get(level);
    index.put(new InscriptionAnchor(abs, new Inscription(List.of(g("self"), g("on_near"), g("damage", "amount", 1.0))),
        InscribingService.TRIGGER_ON_NEAR, new ResourceLocation("arcane", "common"), net.minecraft.core.Direction.NORTH));
    Pig far = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 1, 6));
    Pig near = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 3));
    h.runAfterDelay(60, () -> {
      h.assertTrue(near.getHealth() < near.getMaxHealth(), "quem chegou perto foi atingido");
      h.assertTrue(far.getHealth() == far.getMaxHealth(), "quem esta longe nao foi");
      h.assertTrue(index.at(abs).isEmpty(), "disparou uma vez e o desenho se desfez");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "item_kinds")
  public static void objectsAreClassifiedAndSmallOnesHoldOneDrawingWhileLargeOnesHoldTwo(GameTestHelper h) {
    var kinds = com.arcane.inscribing.ItemDrawings.class;
    var apple = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE);
    var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
    var potion = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION);
    var diamond = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND);
    var armor = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE);
    h.assertTrue(com.arcane.inscribing.ItemDrawings.kindOf(apple) == com.arcane.inscribing.ItemDrawings.Kind.FOOD, "maca e comida");
    h.assertTrue(com.arcane.inscribing.ItemDrawings.kindOf(sword) == com.arcane.inscribing.ItemDrawings.Kind.WEAPON, "espada e arma");
    h.assertTrue(com.arcane.inscribing.ItemDrawings.kindOf(potion) == com.arcane.inscribing.ItemDrawings.Kind.POTION, "pocao e pocao");
    h.assertTrue(com.arcane.inscribing.ItemDrawings.kindOf(diamond) == com.arcane.inscribing.ItemDrawings.Kind.OTHER, "diamante e item comum");
    h.assertTrue(com.arcane.inscribing.ItemDrawings.capacity(apple) == 1 && com.arcane.inscribing.ItemDrawings.capacity(diamond) == 1, "itens pequenos: 1");
    h.assertTrue(com.arcane.inscribing.ItemDrawings.capacity(sword) == 2 && com.arcane.inscribing.ItemDrawings.capacity(armor) == 2, "itens grandes: 2");
    // Passar do limite e permitido (instavel) ate a folga da config; depois recusa.
    for (int i = 0; i < com.arcane.inscribing.ItemDrawings.maxCount(apple); i++) {
      h.assertTrue(com.arcane.inscribing.ItemDrawings.canAdd(apple), "ainda cabe o desenho " + (i + 1));
      com.arcane.inscribing.ItemDrawings.add(apple, objectSpell("common", 0, false));
    }
    h.assertTrue(!com.arcane.inscribing.ItemDrawings.canAdd(apple), "cheio");
    h.assertTrue(com.arcane.inscribing.ItemDrawings.excess(apple) == com.arcane.config.ArcaneConfig.ITEM_EXTRA_DRAWINGS.get(), "o excedente e o que passou da capacidade");
    // Cada tipo de caneta de objeto so escreve na sua classe.
    var foodPen = (com.arcane.item.ObjectPenItem) com.arcane.registry.ModItems.FOOD_PEN.get();
    var weaponPen = (com.arcane.item.ObjectPenItem) com.arcane.registry.ModItems.WEAPON_PEN.get();
    var potionPen = (com.arcane.item.ObjectPenItem) com.arcane.registry.ModItems.POTION_PEN.get();
    h.assertTrue(foodPen.canWriteOn(apple) && !foodPen.canWriteOn(sword) && !foodPen.canWriteOn(potion), "caneta de comida");
    h.assertTrue(weaponPen.canWriteOn(sword) && !weaponPen.canWriteOn(apple), "caneta de arma");
    h.assertTrue(potionPen.canWriteOn(potion) && !potionPen.canWriteOn(diamond), "caneta de pocao");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "item_fire")
  public static void foodHealsOnEatingAndWeaponBlastsOnHitAndTheDrawingFadesUnlessPersistent(GameTestHelper h) {
    var player = learn(casterWith(h), "self", "shield", "persistence");
    var apple = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE);
    com.arcane.inscribing.ItemDrawings.add(apple, objectSpell("common", h.getLevel().getGameTime(), false));
    h.assertTrue(!player.hasEffect(MobEffects.ABSORPTION), "sem desenho disparado, sem efeito");
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish(player, apple, 0, apple));
    h.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "comer a maca com o desenho de Escudo protege quem comeu");
    h.assertTrue(com.arcane.inscribing.ItemDrawings.count(apple) == 0, "o desenho se desfaz depois de disparar");

    player.removeAllEffects();
    var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
    com.arcane.inscribing.ItemDrawings.add(sword, objectSpell("common_perm", h.getLevel().getGameTime(), true));
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
    Pig victim = pig(h, 3, 3);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingHurtEvent(victim, player.damageSources().playerAttack(player), 4f));
    h.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "ferir com a espada desenhada dispara o desenho");
    h.assertTrue(com.arcane.inscribing.ItemDrawings.count(sword) == 1, "com Persistencia o desenho continua na espada");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "item_ink")
  public static void armorDrawingFiresWhenTheWearerIsHitAtMostOncePerSecond(GameTestHelper h) {
    var player = learn(casterWith(h), "self", "shield", "persistence");
    var chest = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE);
    com.arcane.inscribing.ItemDrawings.add(chest, objectSpell("common_perm", h.getLevel().getGameTime(), true));
    player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
    Pig attacker = pig(h, 3, 3);
    var hit = player.damageSources().mobAttack(attacker);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingHurtEvent(player, player.damageSources().fall(), 2f));
    h.assertTrue(!player.hasEffect(MobEffects.ABSORPTION), "queda nao dispara a armadura");
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingHurtEvent(player, hit, 2f));
    h.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "levar golpe com o peitoral desenhado dispara o desenho");
    player.removeAllEffects();
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingHurtEvent(player, hit, 2f));
    h.assertTrue(!player.hasEffect(MobEffects.ABSORPTION), "no mesmo segundo nao dispara de novo");
    h.runAfterDelay(21, () -> {
      net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingHurtEvent(player, hit, 2f));
      h.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "um segundo depois dispara de novo (Persistencia)");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "item_ink")
  public static void itemDrawingsFadeWithInkAndSolventSparesOnlyPurePermanentInk(GameTestHelper h) {
    var player = learn(casterWith(h), "self", "shield");
    long now = h.getLevel().getGameTime();
    long day = com.arcane.inscribing.InkRegistry.DAY_TICKS;
    var old = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD);
    com.arcane.inscribing.ItemDrawings.add(old, objectSpell("common", now - 11 * day, false));
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish(player, old, 0, old));
    h.assertTrue(!player.hasEffect(MobEffects.ABSORPTION), "tinta de 11 dias ja se desgastou: nada dispara");
    h.assertTrue(com.arcane.inscribing.ItemDrawings.count(old) == 0, "o desenho vencido foi apagado");

    var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
    com.arcane.inscribing.ItemDrawings.add(sword, objectSpell("common", now, false));
    com.arcane.inscribing.ItemDrawings.add(sword, objectSpell("permanent", now, false));
    h.assertTrue(com.arcane.inscribing.ItemDrawings.dissolve(sword) == 1, "o po apaga o de tinta comum");
    var left = com.arcane.inscribing.ItemDrawings.list(sword);
    h.assertTrue(left.size() == 1 && left.get(0).metadata().ink().getPath().equals("permanent"), "so a permanente pura ficou");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "ink_units")
  public static void inkPotsHaveUnitsAndPartialPotsAreUsedFirst(GameTestHelper h) {
    var player = casterWith(h);
    int pot = com.arcane.item.InkItem.potUnits();
    var full = new net.minecraft.world.item.ItemStack(com.arcane.registry.ModItems.COMMON_INK.get(), 2);
    player.getInventory().add(full);
    h.assertTrue(com.arcane.item.InkItem.total(player, "common") == 2 * pot, "2 potes cheios somam 2x a quantidade");
    h.assertTrue(com.arcane.item.InkItem.consume(player, "common", 5), "gasta 5 unidades");
    h.assertTrue(com.arcane.item.InkItem.total(player, "common") == 2 * pot - 5, "sobraram as unidades certas");
    h.assertTrue(com.arcane.item.InkItem.consume(player, "common", 3), "gasta mais 3");
    int partial = 0, fullPots = 0;
    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
      var st = player.getInventory().getItem(i);
      if (st.getItem() instanceof com.arcane.item.InkItem) {
        if (com.arcane.item.InkItem.units(st) < pot) partial += st.getCount(); else fullPots += st.getCount();
      }
    }
    h.assertTrue(partial == 1 && fullPots == 1, "um pote ficou pela metade e o outro segue cheio (" + partial + "/" + fullPots + ")");
    h.assertTrue(!com.arcane.item.InkItem.consume(player, "common", 10 * pot), "sem tinta suficiente nao gasta nada");
    h.assertTrue(com.arcane.item.InkItem.total(player, "common") == 2 * pot - 8, "nada foi gasto na tentativa que falhou");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "creature_drawing")
  public static void aDrawnCreatureFiresItsSpellWhenItAttacksAndTheDrawingFades(GameTestHelper h) {
    var level = h.getLevel();
    Pig pig = pig(h, 3, 3);
    var data = pig.getPersistentData();
    var drawings = new java.util.ArrayList<com.arcane.inscription.Inscription>();
    drawings.add(objectSpell("common", level.getGameTime(), false));
    com.arcane.inscribing.ItemDrawings.storeIn(data, drawings);
    h.assertTrue(com.arcane.inscribing.ItemDrawings.listIn(data).size() == 1, "o desenho ficou guardado na criatura");
    var list = com.arcane.inscribing.ItemDrawings.listIn(data);
    int fired = com.arcane.inscribing.ItemDrawings.fireOn(pig, level, list, data);
    h.assertTrue(fired == 1, "a criatura disparou o desenho");
    h.assertTrue(com.arcane.inscribing.ItemDrawings.listIn(data).isEmpty(), "sem Persistencia o desenho some depois de disparar");
    // Tinta vencida nao dispara.
    var old = new java.util.ArrayList<com.arcane.inscription.Inscription>();
    old.add(objectSpell("common", level.getGameTime() - 11L * com.arcane.inscribing.InkRegistry.DAY_TICKS, false));
    com.arcane.inscribing.ItemDrawings.storeIn(data, old);
    h.assertTrue(com.arcane.inscribing.ItemDrawings.fireOn(pig, level, com.arcane.inscribing.ItemDrawings.listIn(data), data) == 0, "tinta vencida nao dispara");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "air_pen")
  public static void phoenixPenOnlyWritesInFreeAirAndItsDrawingNeverFadesWhenConfigured(GameTestHelper h) {
    var player = casterWith(h);
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.arcane.registry.ModItems.PHOENIX_PEN.get()));
    var level = h.getLevel();
    BlockPos air = h.absolutePos(new BlockPos(2, 3, 2));
    BlockPos solid = h.absolutePos(new BlockPos(2, 1, 2));
    level.setBlock(solid, Blocks.STONE.defaultBlockState(), 3);
    h.assertTrue(com.arcane.inscribing.PenRegistry.canWriteOnBlock(player, air, level), "a Caneta de Fenix escreve no ar");
    h.assertTrue(!com.arcane.inscribing.PenRegistry.canWriteOnBlock(player, solid, level), "e nao em bloco solido");
    var menu = new com.arcane.menu.ArcaneWorkbenchMenu(0, player.getInventory(), air, com.arcane.menu.ArcaneWorkbenchMenu.BLOCK);
    h.assertTrue(menu.stillValid(player), "o menu da caneta de ar fica aberto");
    h.succeed();
  }

  // ================================================================== Magias nao-combate, cura nas plantas, voo
  @GameTest(template = "empty", batch = "utility_grow")
  public static void growAndHealMakeCropsGrow(GameTestHelper h) {
    // Arbusto de amora: cresce com farinha de osso e nao precisa de luz para continuar de pe (o trigo, no escuro da
    // estrutura de teste, cai quando o vizinho cresce).
    floor(h);
    for (int i = 1; i <= 3; i++) {
      h.setBlock(new BlockPos(i, 0, 2), Blocks.GRASS_BLOCK);
      h.setBlock(new BlockPos(i, 1, 2), Blocks.SWEET_BERRY_BUSH);
    }
    Pig caster = pig(h, 2, 4);
    cast(h, caster, g("self"), g("grow", "radius", 3.0, "strength", 2.0));
    for (int i = 1; i <= 3; i++) {
      var st = h.getBlockState(new BlockPos(i, 1, 2));
      h.assertTrue(st.is(Blocks.SWEET_BERRY_BUSH), "o arbusto " + i + " continua la (" + st + ")");
      int age = st.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE);
      h.assertTrue(age > 0, "Crescer fez o arbusto " + i + " crescer (idade " + age + ")");
    }
    // Cura numa criatura parada no meio das plantas faz a planta aos pes dela crescer.
    h.setBlock(new BlockPos(5, 0, 5), Blocks.GRASS_BLOCK);
    h.setBlock(new BlockPos(5, 1, 5), Blocks.SWEET_BERRY_BUSH);
    Pig healer = pig(h, 5, 5);
    cast(h, healer, g("self"), g("heal"));
    int after = h.getBlockState(new BlockPos(5, 1, 5)).getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE);
    h.assertTrue(after > 0, "a Cura fez o arbusto crescer (idade " + after + ")");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "utility_light", timeoutTicks = 100)
  public static void lightPlacesATemporaryLightThatGoesOut(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    cast(h, caster, g("self"), g("light", "duration", 20.0));
    java.util.List<BlockPos> lit = new java.util.ArrayList<>();
    for (int y = 1; y <= 5; y++) if (h.getBlockState(new BlockPos(3, y, 3)).is(Blocks.LIGHT)) lit.add(new BlockPos(3, y, 3));
    h.assertTrue(lit.size() == 1, "acendeu uma luz acima do conjurador (" + lit.size() + ")");
    h.runAfterDelay(50, () -> {
      h.assertTrue(!h.getBlockState(lit.get(0)).is(Blocks.LIGHT), "a luz apagou depois do tempo");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "utility_water", timeoutTicks = 80)
  public static void waterWalkKeepsACreatureOnTheSurface(GameTestHelper h) {
    for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
      h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
      h.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
      h.setBlock(new BlockPos(x, 3, z), Blocks.WATER);
    }
    var walker = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 4, 2));
    var sinker = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 4, 3));
    walker.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.arcane.registry.ModEffects.WATER_WALK.get(), 400));
    h.runAfterDelay(60, () -> {
      double surface = h.absolutePos(new BlockPos(0, 4, 0)).getY();
      h.assertTrue(walker.getY() > surface - 0.3, "com Andar na Agua fica na superficie (y " + walker.getY() + ", tona " + surface + ")");
      h.assertTrue(sinker.getY() < walker.getY() - 0.3, "sem o efeito afunda (" + sinker.getY() + ")");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "flight_boost")
  public static void flightBoostLevelsCostMoreManaEach(GameTestHelper h) {
    var e = com.arcane.execution.FlightService.BOOST_COST;
    var v = com.arcane.execution.FlightService.BOOST_SPEED;
    for (int i = 1; i < e.length; i++) {
      h.assertTrue(e[i] > e[i - 1] && v[i] > v[i - 1], "o nivel " + i + " e mais rapido e mais caro que o anterior");
    }
    var player = casterWith(h);
    h.assertTrue(com.arcane.execution.FlightService.boostOf(player) == 0, "sem correr, voo normal");
    com.arcane.execution.FlightService.setBoost((net.minecraft.server.level.ServerPlayer) player, 9);
    player.setSprinting(true);
    h.assertTrue(com.arcane.execution.FlightService.boostOf(player) == com.arcane.execution.FlightService.MAX_BOOST, "o servidor limita ao nivel 3");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "scepter")
  public static void scepterHoldsSixSpellsCyclesAndCastsAt120Percent(GameTestHelper h) {
    var scepter = new net.minecraft.world.item.ItemStack(com.arcane.registry.ModItems.SCEPTER.get());
    for (int i = 0; i < 7; i++) {
      var spell = new Inscription(List.of(g("self"), g("heal")));
      com.arcane.item.ScepterItem.store(scepter, spell);
    }
    h.assertTrue(com.arcane.item.ScepterItem.spells(scepter).size() == com.arcane.item.ScepterItem.SLOTS, "guarda no maximo 6 magias");
    int sel = com.arcane.item.ScepterItem.selected(scepter);
    com.arcane.item.ScepterItem.cycle(scepter);
    h.assertTrue(com.arcane.item.ScepterItem.selected(scepter) == (sel + 1) % 6, "agachar passa para a proxima");
    // aparencia: 10 pontas x 8 hastes x 20 gemas x 8 metais; o valor da volta e a cor de cada tinta segue a escolha
    com.arcane.item.ScepterItem.setStyle(scepter, com.arcane.item.ScepterItem.PART_HEAD, 9);
    com.arcane.item.ScepterItem.setStyle(scepter, com.arcane.item.ScepterItem.PART_SHAFT, 8);
    h.assertTrue(com.arcane.item.ScepterItem.head(scepter) == 9 && com.arcane.item.ScepterItem.shaft(scepter) == 0, "ponta 9 existe; haste 8 da a volta para 0");
    com.arcane.item.ScepterItem.setStyle(scepter, com.arcane.item.ScepterItem.PART_GEM, 7);
    com.arcane.item.ScepterItem.setStyle(scepter, com.arcane.item.ScepterItem.PART_METAL, 1);
    h.assertTrue(com.arcane.item.ScepterItem.tint(scepter, 0) == com.arcane.item.ScepterItem.GEM_COLORS[7]
        && com.arcane.item.ScepterItem.tint(scepter, 1) == com.arcane.item.ScepterItem.METAL_COLORS[1], "a gema e o metal sao pintados com a cor escolhida");
    h.assertTrue(com.arcane.item.ScepterItem.count(0) * com.arcane.item.ScepterItem.count(1) * com.arcane.item.ScepterItem.count(2) * com.arcane.item.ScepterItem.count(3) >= 10000,
        "mais de 10 mil combinacoes de aparencia");
    com.arcane.item.ScepterItem.remove(scepter, 0);
    h.assertTrue(com.arcane.item.ScepterItem.spells(scepter).size() == 5, "x apaga uma magia");
    h.assertTrue(Math.abs(com.arcane.item.ScepterItem.POWER - 1.2) < 1e-9, "forca de 120%");
    // Conjurar com o cetro: a cura sai mais forte que a mesma magia sem cetro.
    var player = learn(casterWith(h), "self", "heal");
    player.setHealth(2f);
    var plain = com.arcane.execution.CastingService.tryCast(player, new Inscription(List.of(g("self"), g("heal"))), com.arcane.execution.PrecisionSnapshot.FIELD);
    float healedPlain = player.getHealth() - 2f;
    player.setHealth(2f);
    var boosted = com.arcane.execution.CastingService.tryCast(player, new Inscription(List.of(g("self"), g("heal"))),
        new com.arcane.execution.PrecisionSnapshot(com.arcane.item.ScepterItem.POWER, 1.0, false));
    float healedBoost = player.getHealth() - 2f;
    h.assertTrue(plain.success() && boosted.success(), "as duas conjuracoes funcionam (" + plain.message() + " / " + boosted.message() + ")");
    h.assertTrue(healedBoost > healedPlain, "com o cetro cura mais (" + healedPlain + " -> " + healedBoost + ")");
    h.succeed();
  }

  // ================================================================== Hidras e glifos originais
  @GameTest(template = "empty", batch = "hydra_rules")
  public static void hydraStagesFirstIsHatchlingLastIsElderMiddleAreDistinct(GameTestHelper h) {
    var rnd = net.minecraft.util.RandomSource.create(42);
    java.util.List<Integer> used = new java.util.ArrayList<>();
    for (int n = 0; n < com.arcane.world.HydraLairs.COUNT; n++) {
      int st = com.arcane.world.HydraLairs.nextStage(used, n, rnd);
      if (n == 0) h.assertTrue(st == 1, "o primeiro covil encontrado tem a recem-nascida (" + st + ")");
      else if (n == 6) h.assertTrue(st == 14, "o ultimo tem a ancia (" + st + ")");
      else h.assertTrue(st >= 2 && st <= 13 && !used.contains(st), "os do meio sorteiam 2..13 sem repetir (" + st + ")");
      used.add(st);
    }
    h.assertTrue(com.arcane.entity.HydraEntity.headsFor(1) == 1 && com.arcane.entity.HydraEntity.headsFor(14) == 9, "1 cabeca na recem-nascida, 9 na ancia");
    h.assertTrue(com.arcane.entity.HydraEntity.playerWeight(1, 0) == 1.0, "um jogador sozinho = forca base");
    h.assertTrue(com.arcane.entity.HydraEntity.playerWeight(2, 0) > com.arcane.entity.HydraEntity.playerWeight(1, 3)
        && com.arcane.entity.HydraEntity.playerWeight(1, 3) > com.arcane.entity.HydraEntity.playerWeight(1, 0), "por perto pesa mais que online longe");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "hydra_entity", timeoutTicks = 60)
  public static void hydraStageScalesSizeHealthAndPowers(GameTestHelper h) {
    floor(h);
    var young = h.spawnWithNoFreeWill(com.arcane.registry.ModEntities.HYDRA.get(), new BlockPos(1, 1, 1));
    young.setStage(1);
    var elder = h.spawnWithNoFreeWill(com.arcane.registry.ModEntities.HYDRA.get(), new BlockPos(5, 1, 5));
    elder.setStage(14);
    h.assertTrue(elder.getBbWidth() > young.getBbWidth() * 3, "a ancia e muito maior (" + young.getBbWidth() + " -> " + elder.getBbWidth() + ")");
    h.assertTrue(elder.effectiveMaxHealth() > young.effectiveMaxHealth() * 20, "e muito mais forte (" + young.effectiveMaxHealth() + " -> " + elder.effectiveMaxHealth() + ")");
    h.assertTrue(elder.heads() == 9 && young.heads() == 1, "cabecas pelo estagio");
    Pig victim = pig(h, 3, 3);
    float before = victim.getHealth();
    elder.use(com.arcane.entity.HydraEntity.SWEEP, victim);
    h.assertTrue(victim.getHealth() < before || victim.isRemoved() || victim.isDeadOrDying(), "o golpe de cauda fere quem esta perto (vida " + victim.getHealth()
        + ", invuln " + victim.invulnerableTime + ", dist " + String.format("%.1f", victim.distanceTo(elder)) + ", dano " + elder.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
        + ", raio " + String.format("%.1f", elder.getBbWidth()) + ")");
    var tag = new net.minecraft.nbt.CompoundTag();
    elder.addAdditionalSaveData(tag);
    h.assertTrue(tag.getInt("HydraStage") == 14, "o estagio e salvo");
    young.discard();
    elder.discard();
    h.succeed();
  }

  @GameTest(template = "empty", batch = "hydra_rules")
  public static void hydraPowerGrowsExponentiallyAndStageFiveDevastates(GameTestHelper h) {
    double ratio = com.arcane.entity.HydraEntity.baseHealth(2) / com.arcane.entity.HydraEntity.baseHealth(1);
    for (int s = 2; s < 14; s++) {
      double r = com.arcane.entity.HydraEntity.baseHealth(s + 1) / com.arcane.entity.HydraEntity.baseHealth(s);
      h.assertTrue(Math.abs(r - ratio) < 1e-9, "a vida cresce sempre pelo mesmo fator (exponencial)");
      h.assertTrue(com.arcane.entity.HydraEntity.baseDamage(s + 1) > com.arcane.entity.HydraEntity.baseDamage(s), "o dano sobe a cada estagio");
    }
    h.assertTrue(com.arcane.entity.HydraEntity.baseHealth(1) <= 80, "a recem-nascida e uma luta justa");
    h.assertTrue(com.arcane.entity.HydraEntity.baseHealth(14) > com.arcane.entity.HydraEntity.baseHealth(1) * 200, "a ancia e centenas de vezes mais forte");
    h.assertTrue(!com.arcane.entity.HydraEntity.devastates(4) && com.arcane.entity.HydraEntity.devastates(5), "do estagio 5 em diante destroi a regiao");
    h.assertTrue(com.arcane.entity.HydraEntity.devastationRadius(5) >= 6, "a cratera do estagio 5 ja e grande");
    h.assertTrue(com.arcane.entity.HydraEntity.sealsFor(1) == 2 && com.arcane.entity.HydraEntity.sealsFor(14) == 8, "2 a 8 selos");
    for (int s = 1; s <= 14; s++) {
      h.assertTrue(Integer.bitCount(com.arcane.entity.HydraEntity.sealMaskFor(s)) == com.arcane.entity.HydraEntity.sealsFor(s), "um pilar por selo (" + s + ")");
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "hydra_seal", timeoutTicks = 60)
  public static void hydraSleepsSealedUntilTheLastSealBreaks(GameTestHelper h) {
    floor(h);
    var level = h.getLevel();
    var hydra = h.spawnWithNoFreeWill(com.arcane.registry.ModEntities.HYDRA.get(), new BlockPos(2, 1, 2));
    hydra.setStage(1);
    BlockPos home = hydra.blockPosition();
    hydra.homeTo(home, 12);
    int mask = com.arcane.entity.HydraEntity.sealMaskFor(1);
    java.util.List<BlockPos> seals = new java.util.ArrayList<>();
    for (int i = 0; i < com.arcane.entity.HydraEntity.PILLARS; i++) {
      if ((mask & (1 << i)) == 0) continue;
      BlockPos p = home.offset(com.arcane.entity.HydraEntity.pillarTop(i));
      level.setBlock(p, com.arcane.registry.ModBlocks.HYDRA_SEAL.get().defaultBlockState(), 3);
      seals.add(p);
    }
    hydra.setSealMask(com.arcane.world.HydraLairs.sealMaskAt(level, home));
    h.assertTrue(hydra.sealed() && hydra.isNoAi() && seals.size() == 2, "nasce selada e parada, presa por 2 selos");
    float hp = hydra.getHealth();
    hydra.hurt(level.damageSources().magic(), 30f);
    h.assertTrue(hydra.getHealth() == hp, "selada nao pode ser ferida");
    level.removeBlock(seals.get(0), false);
    h.assertTrue(hydra.sealed() && Integer.bitCount(hydra.sealMask()) == 1, "um selo quebrado, ainda presa");
    level.removeBlock(seals.get(1), false);
    h.assertTrue(!hydra.sealed() && !hydra.isNoAi(), "o ultimo selo quebrado a desperta");
    hydra.hurt(level.damageSources().magic(), 10f);
    h.assertTrue(hydra.getHealth() < hp, "desperta, pode ser ferida");
    hydra.discard();
    h.succeed();
  }

  @GameTest(template = "empty", batch = "hydra_devour")
  public static void hydraScalesVarietyAndSpeedWithPlayersAndDevoursGrimoires(GameTestHelper h) {
    floor(h);
    h.assertTrue(com.arcane.entity.HydraEntity.varietyStage(2, 1.0) == 2 && com.arcane.entity.HydraEntity.varietyStage(2, 3.0) == 4,
        "cada jogador a mais libera poderes de 1 estagio acima");
    h.assertTrue(com.arcane.entity.HydraEntity.cooldownFactor(1.0) == 1.0 && com.arcane.entity.HydraEntity.cooldownFactor(9.0) == 0.5,
        "mais jogadores, ataques mais rapidos (ate a metade da espera)");
    var owner = learn(casterWith(h), "self", "heal");
    addSpell(owner, com.arcane.grimoire.SpellPrefs.CastMode.TAP, g("self"), g("heal"));
    var tag = new net.minecraft.nbt.CompoundTag();
    com.arcane.grimoire.GrimoireAttachments.get(owner).orElseThrow().save(tag);
    var hydra = h.spawnWithNoFreeWill(com.arcane.registry.ModEntities.HYDRA.get(), new BlockPos(2, 1, 2));
    hydra.setStage(1);
    var drop = h.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 1, 2.5));
    var item = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), drop.x, drop.y, drop.z, com.arcane.item.GrimoireItem.createFor(owner, tag));
    h.getLevel().addFreshEntity(item);
    h.assertTrue(hydra.devourNearby() == 1 && item.isRemoved(), "o grimorio caido perto dela foi devorado");
    h.assertTrue(hydra.devouredSpells().stream().anyMatch(sp -> sp.glyphs().size() == 2), "ela passa a conhecer as magias dele");
    hydra.kill();
    h.runAfterDelay(2, () -> {
      var back = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(2, 1, 2))).inflate(8),
          it -> com.arcane.item.GrimoireItem.isOwnedBy(it.getItem(), owner.getUUID()));
      h.assertTrue(back.size() == 1, "matar a Hidra devolve o grimorio");
      back.forEach(net.minecraft.world.entity.Entity::discard);
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "hydra_devastation", timeoutTicks = 120)
  public static void stageFiveHydraAnnouncesThenCratersTheGround(GameTestHelper h) {
    floor(h);
    for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) for (int y = 1; y <= 2; y++) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
    var hydra = h.spawnWithNoFreeWill(com.arcane.registry.ModEntities.HYDRA.get(), new BlockPos(0, 3, 0));
    hydra.setStage(5);
    Pig victim = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 3, 3));
    float before = victim.getHealth();
    hydra.use(com.arcane.entity.HydraEntity.DEVASTATE, victim);
    h.assertTrue(hydra.pendingStrikes() > 0, "primeiro o aviso: o golpe ainda nao caiu");
    h.assertTrue(victim.getHealth() == before, "durante o aviso da para fugir");
    h.runAfterDelay(com.arcane.entity.HydraEntity.STRIKE_WARNING + 10, () -> {
      h.assertTrue(hydra.pendingStrikes() == 0, "o golpe caiu");
      h.assertTrue(!victim.isAlive() || victim.getHealth() < before, "quem ficou foi ferido");
      int stone = 0;
      for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) for (int y = 1; y <= 2; y++) {
        if (h.getBlockState(new BlockPos(x, y, z)).is(Blocks.STONE)) stone++;
      }
      h.assertTrue(stone < 25, "o chao virou cratera (sobraram " + stone + " de 50 pedras)");
      hydra.discard();
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "original_glyphs", timeoutTicks = 80)
  public static void originalGlyphsStopTimeRewindAndBendSpace(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 1, 1);
    Pig target = pig(h, 4, 4);
    // Parar o tempo: a criatura alvo congela no ar.
    cast(h, caster, g("self"), g("time_stop", "radius", 8.0, "duration", 40.0));
    h.assertTrue(com.arcane.execution.handlers.OriginalGlyphs.isFrozen(target), "o porco foi congelado");
    h.assertTrue(!com.arcane.execution.handlers.OriginalGlyphs.isFrozen(caster), "o conjurador continua livre");
    target.setDeltaMovement(0, 1.5, 0);
    // Retroceder: o conjurador volta para onde estava.
    com.arcane.execution.handlers.OriginalGlyphs.record(caster);
    var home = caster.position();
    caster.teleportTo(home.x + 3, home.y, home.z + 2);
    cast(h, caster, g("self"), g("rewind", "seconds", 1.0));
    h.assertTrue(caster.position().distanceTo(home) < 0.5, "Retroceder devolveu o conjurador (" + caster.position() + ")");
    // Genese: cura total.
    caster.setHealth(2f);
    cast(h, caster, g("self"), g("genesis", "radius", 6.0));
    h.assertTrue(caster.getHealth() >= caster.getMaxHealth() - 0.01f, "Genese curou tudo");
    // Inversao da gravidade: levitacao forte em quem esta perto.
    Pig floaty = pig(h, 2, 5);
    cast(h, caster, g("self"), g("gravity_inversion", "radius", 8.0));
    h.assertTrue(floaty.hasEffect(MobEffects.LEVITATION), "a gravidade virou");
    h.runAfterDelay(10, () -> {
      h.assertTrue(target.getDeltaMovement().lengthSqr() < 1.0e-6, "congelado nao se mexe");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "original_space")
  public static void spaceCollapsePullsAndAnnihilateErasesBlocks(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 1, 1);
    Pig far = pig(h, 6, 6);
    float before = far.getHealth();
    cast(h, caster, g("self"), g("space_collapse", "radius", 10.0));
    h.assertTrue(far.position().distanceTo(caster.position()) < 2.0, "o porco foi arrancado ate o ponto (" + far.position() + ")");
    h.assertTrue(far.getHealth() < before || far.isDeadOrDying(), "e esmagado");
    for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(5, y, 1), Blocks.STONE);
    Pig eraser = pig(h, 5, 2);
    cast(h, eraser, g("self"), g("annihilate", "radius", 3.0));
    // a cratera abre em onda de choque (do centro para fora): espera a frente passar
    h.runAfterDelay(10, () -> {
      int left = 0;
      for (int y = 1; y <= 3; y++) if (!h.getBlockState(new BlockPos(5, y, 1)).isAir()) left++;
      h.assertTrue(left == 0, "Aniquilacao apagou os blocos (" + left + " ficaram)");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "crater_limits")
  public static void craterRadiusFollowsTheFixedCapRuleAndItsOwnConfig(GameTestHelper h) {
    boolean caps = com.arcane.config.ArcaneConfig.AREA_HARD_CAPS.get();
    int cfg = com.arcane.config.ArcaneConfig.CRATER_MAX_RADIUS.get();
    try {
      com.arcane.config.ArcaneConfig.AREA_HARD_CAPS.set(true);
      h.assertTrue(com.arcane.execution.RoundCrater.maxRadius() == 24f, "com o teto fixo ligado a cratera para em 24");
      com.arcane.config.ArcaneConfig.AREA_HARD_CAPS.set(false);
      com.arcane.config.ArcaneConfig.CRATER_MAX_RADIUS.set(128);
      h.assertTrue(com.arcane.execution.RoundCrater.maxRadius() == 128f, "desligado, vale craterMaxRadius");
      com.arcane.config.ArcaneConfig.CRATER_MAX_RADIUS.set(0);
      h.assertTrue(com.arcane.execution.RoundCrater.maxRadius() >= 4096f, "0 = sem limite");
    } finally {
      com.arcane.config.ArcaneConfig.AREA_HARD_CAPS.set(caps);
      com.arcane.config.ArcaneConfig.CRATER_MAX_RADIUS.set(cfg);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "crater_wave", timeoutTicks = 100)
  public static void aCraterOpensFromTheCentreOutwardLikeAShockwave(GameTestHelper h) {
    var level = h.getLevel();
    var list = new it.unimi.dsi.fastutil.longs.LongArrayList();
    BlockPos c = h.absolutePos(new BlockPos(3, 3, 3));
    for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) for (int y = 1; y < 6; y++) {
      BlockPos pos = h.absolutePos(new BlockPos(x, y, z));
      level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
      list.add(pos.asLong());
    }
    com.arcane.execution.CraterQueue.submit(level, list, net.minecraft.world.phys.Vec3.atCenterOf(c));
    h.runAfterDelay(1, () -> {
      h.assertTrue(level.getBlockState(c).isAir(), "o centro sai primeiro");
      h.assertTrue(!level.getBlockState(h.absolutePos(new BlockPos(0, 1, 0))).isAir(), "o canto longe ainda esta la");
    });
    h.runAfterDelay(30, () -> {
      // confere so os blocos desta cratera (outros testes em paralelo tambem usam a fila do mundo)
      int left = 0;
      for (long l : list) if (!level.getBlockState(BlockPos.of(l)).isAir()) left++;
      h.assertTrue(left == 0, "a onda chegou ate a borda (faltam " + left + ")");
      h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(0, 1, 0))).isAir(), "e levou o canto");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "crater_barrier", timeoutTicks = 100)
  public static void aBarrierShieldsWhatIsBehindItFromACrater(GameTestHelper h) {
    var level = h.getLevel();
    var barrier = com.arcane.registry.ModBlocks.ARCANE_BARRIER.get().defaultBlockState();
    var list = new it.unimi.dsi.fastutil.longs.LongArrayList();
    for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) for (int y = 1; y < 4; y++) {
      BlockPos rel = new BlockPos(x, y, z);
      BlockPos pos = h.absolutePos(rel);
      boolean wall = z == 3 && x >= 1 && x <= 5;
      level.setBlock(pos, wall ? barrier : Blocks.STONE.defaultBlockState(), 2);
      list.add(pos.asLong());
    }
    com.arcane.execution.CraterQueue.submit(level, list, net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 2, 1))));
    h.runAfterDelay(30, () -> {
      h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(3, 2, 1))).isAir() && level.getBlockState(h.absolutePos(new BlockPos(0, 2, 0))).isAir(),
          "na frente da barreira a cratera abriu");
      for (int x = 1; x <= 5; x++) h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(x, 2, 3))).is(barrier.getBlock()), "a barreira aguentou");
      for (int z = 4; z < 7; z++) {
        h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(3, 2, z))).is(Blocks.STONE), "atras da barreira ficou intacto (z " + z + ")");
      }
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "barrier_ray")
  public static void aBarrierStopsRaysAndCannotBeDugByMagic(GameTestHelper h) {
    floor(h);
    var barrier = com.arcane.registry.ModBlocks.ARCANE_BARRIER.get().defaultBlockState();
    for (int x = 1; x <= 5; x++) for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, 2), barrier);
    Pig caster = pig(h, 3, 0);
    Pig target = pig(h, 3, 4);
    float hp = target.getHealth();
    cast(h, caster, g("ray"), g("damage", "amount", 6.0));
    h.assertTrue(target.getHealth() == hp, "o raio parou na barreira (vida " + hp + " -> " + target.getHealth() + ")");
    cast(h, caster, g("ray"), g("dig", "power", 5.0));
    int left = 0;
    for (int x = 1; x <= 5; x++) for (int y = 1; y <= 3; y++) if (h.getBlockState(new BlockPos(x, y, 2)).is(barrier.getBlock())) left++;
    h.assertTrue(left == 15, "Cavar nao abre a barreira (" + left + " de 15)");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "crater_queue", timeoutTicks = 100)
  public static void aBigCraterIsRemovedOverSeveralTicksNotAllAtOnce(GameTestHelper h) {
    var level = h.getLevel();
    int per = com.arcane.config.ArcaneConfig.CRATER_BLOCKS_PER_TICK.get();
    var list = new it.unimi.dsi.fastutil.longs.LongArrayList();
    BlockPos origin = h.absolutePos(new BlockPos(0, 2, 0));
    for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) for (int y = 0; y < 9; y++) {
      BlockPos pos = origin.offset(x, y, z);
      level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
      list.add(pos.asLong());
    }
    com.arcane.config.ArcaneConfig.CRATER_BLOCKS_PER_TICK.set(500);
    try {
      com.arcane.execution.CraterQueue.submit(level, list);
      int left = com.arcane.execution.CraterQueue.pending(level);
      h.assertTrue(left > 0 && left < list.size(), "so parte saiu no primeiro tick (sobram " + left + " de " + list.size() + ")");
      h.runAfterDelay(30, () -> {
        h.assertTrue(com.arcane.execution.CraterQueue.pending(level) == 0, "a fila terminou");
        int solid = 0;
        for (int i = 0; i < list.size(); i++) if (!level.getBlockState(BlockPos.of(list.getLong(i))).isAir()) solid++;
        h.assertTrue(solid == 0, "todos os blocos da cratera sairam (" + solid + " ficaram)");
        com.arcane.config.ArcaneConfig.CRATER_BLOCKS_PER_TICK.set(per);
        h.succeed();
      });
    } catch (RuntimeException e) {
      com.arcane.config.ArcaneConfig.CRATER_BLOCKS_PER_TICK.set(per);
      throw e;
    }
  }

  // ================================================================== Revisao 2: cratera redonda, oferendas, pilar mantido
  @GameTest(template = "empty", batch = "rev_crater", timeoutTicks = 60)
  public static void blastCraterIsASphereNotACube(GameTestHelper h) {
    BlockPos c = h.absolutePos(new BlockPos(3, 10, 3));
    var level = h.getLevel();
    for (int x = -7; x <= 7; x++) for (int y = -7; y <= 7; y++) for (int z = -7; z <= 7; z++) level.setBlock(c.offset(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
    var crater = com.arcane.execution.RoundCrater.blocks(level, net.minecraft.world.phys.Vec3.atCenterOf(c), 6.0f);
    var set = new java.util.HashSet<>(crater);
    h.assertTrue(set.contains(c) && set.contains(c.offset(3, 0, 0)) && set.contains(c.offset(0, -3, 0)), "o miolo e os eixos saem");
    h.assertTrue(!set.contains(c.offset(5, 5, 5)) && !set.contains(c.offset(6, 6, 0)) && !set.contains(c.offset(0, 6, 6)), "os cantos do cubo nao saem");
    int outside = 0;
    for (BlockPos b : crater) if (b.distSqr(c) > 6.6 * 6.6) outside++;
    h.assertTrue(outside == 0, "nada alem do raio (" + outside + ")");
    double ideal = 4.0 / 3.0 * Math.PI * 6 * 6 * 6;
    h.assertTrue(crater.size() > ideal * 0.5 && crater.size() < ideal * 1.05, "volume de esfera: " + crater.size() + " de ~" + (int) ideal);
    for (int x = -7; x <= 7; x++) for (int y = -7; y <= 7; y++) for (int z = -7; z <= 7; z++) level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
    h.succeed();
  }

  @GameTest(template = "empty", batch = "rev_offer_value")
  public static void offeringValuesAddToTheRitualAndLegendaryDoublesWithACap(GameTestHelper h) {
    var star = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR);
    var iron = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
    var stick = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK);
    h.assertTrue(com.arcane.ritual.OfferingValue.of(iron) > com.arcane.ritual.OfferingValue.of(stick) && com.arcane.ritual.OfferingValue.of(stick) > 0, "qualquer item vale algo; o precioso vale mais");
    h.assertTrue(com.arcane.ritual.OfferingValue.legendary(com.arcane.ritual.OfferingValue.of(star)), "a Estrela sozinha ja passa de 300%");
    h.assertTrue(!com.arcane.ritual.OfferingValue.legendary(300), "300% exatos ainda nao e lendario");
    com.arcane.ritual.RitualSettings.setMaxPercent(1500);
    h.assertTrue(com.arcane.ritual.OfferingValue.total(300, 120) == 420, "sem lendario so soma: " + com.arcane.ritual.OfferingValue.total(300, 120));
    h.assertTrue(com.arcane.ritual.OfferingValue.total(150, 350) == 1000, "lendario dobra: (150+350)x2 = 1000");
    h.assertTrue(com.arcane.ritual.OfferingValue.total(300, 400) == 1400, "700% vira 1400%");
    h.assertTrue(com.arcane.ritual.OfferingValue.total(1000, 500) == 1500, "limite de 1500%");
    com.arcane.ritual.RitualSettings.setMaxPercent(2000);
    h.assertTrue(com.arcane.ritual.OfferingValue.total(1000, 500) == 2000, "o limite e configuravel");
    com.arcane.ritual.RitualSettings.setMaxPercent(com.arcane.ritual.RitualSettings.DEFAULT_MAX_PERCENT);
    h.succeed();
  }

  @GameTest(template = "ritual_arena", batch = "rev_offer_ritual", timeoutTicks = 100)
  public static void offeringsPowerTheRitualAndAreConsumedOnePerItem(GameTestHelper h) {
    arena(h);
    var player = learn(casterWith(h), "self", "heal");
    player.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(ALTAR.getX() + 2.5, 1, ALTAR.getZ() + 0.5)));
    TestPlayers.add(h, player);
    com.arcane.ritual.RitualService.drawPerfectCircle(h.getLevel(), altarAbs(h), 4);
    player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 3));
    player.getInventory().setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_BLOCK, 2));
    player.getInventory().setItem(2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_BLOCK, 2));
    var entry = new com.arcane.network.packet.C2SSaveInscriptionPacket.Entry("arcane:self", Map.of());
    var heal = new com.arcane.network.packet.C2SSaveInscriptionPacket.Entry("arcane:heal", Map.of());
    // pouca oferenda: soma, sem lendario, consome 1 ferro
    var small = com.arcane.network.packet.C2SRitualPacket.offeringsOf(player, List.of(0));
    var preview = com.arcane.ritual.RitualService.preview(h.getLevel(), altarAbs(h), player, new Inscription(List.of(g("self"), g("heal"))), small);
    h.assertTrue(!preview.legendary() && preview.offeringPct() == 4 && preview.totalPct() == preview.sizePct() + 4, "previa: " + preview.offeringPct() + "% / total " + preview.totalPct());
    var tried = com.arcane.ritual.RitualService.perform(h.getLevel(), altarAbs(h), player, new Inscription(List.of(g("self"), g("heal"))),
        com.arcane.ritual.ComponentSource.of(player), small);
    h.assertTrue(tried.success(), "ritual com oferenda: " + tried.outcome() + " " + tried.detail());
    h.assertTrue(player.getInventory().getItem(0).getCount() == 2, "uma unidade do ferro foi para o altar");
    // oferenda grande (2 blocos de diamante = 320%): lendario; reduz a mesma pilha em 1 cada, e slot repetido conta uma vez
    var big = com.arcane.network.packet.C2SRitualPacket.offeringsOf(player, List.of(1, 2, 2, 1, 0));
    h.assertTrue(big.stacks().size() == 3, "ate 3 slots distintos");
    var bigPreview = com.arcane.ritual.RitualService.preview(h.getLevel(), altarAbs(h), player, new Inscription(List.of(g("self"), g("heal"))), big);
    h.assertTrue(bigPreview.legendary() && bigPreview.totalPct() == (bigPreview.sizePct() + bigPreview.offeringPct()) * 2, "passou de 300%: lendario e dobra (" + bigPreview.totalPct() + "%)");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "rev_hold_pillar", timeoutTicks = 80)
  public static void heldPillarStaysWhereItWasCastAndRunesShowTheRealRadius(GameTestHelper h) {
    floor(h);
    var player = learn(casterWith(h), "pillar", "damage");
    player.setXRot(70f);
    player.setYRot(0f);
    var spell = addSpell(player, com.arcane.grimoire.SpellPrefs.CastMode.HOLD, g("pillar", "radius", 2.0), g("damage", "amount", 0.5));
    com.arcane.execution.CastChannels.press(player, spell.id());
    var anchor = com.arcane.execution.CastChannels.heldAnchor(player.getUUID());
    var tryIt = com.arcane.execution.CastingService.tryCast(player, spell, com.arcane.execution.PrecisionSnapshot.of(spell));
    h.assertTrue(anchor != null, "o pilar mantido registra onde nasceu (ativo=" + com.arcane.execution.CastChannels.isActive(player.getUUID())
        + ", tentativa=" + tryIt.success() + " " + tryIt.message() + ", mana=" + com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(player).current() + ")");
    float r1 = com.arcane.execution.CastChannels.areaRadius(player, spell, 1.0);
    float r5 = com.arcane.execution.CastChannels.areaRadius(player, spell, 5.0);
    h.assertTrue(r1 > 0 && Math.abs(r5 - r1 * 5f) < 0.01f, "a carga x5 aumenta o raio x5 (" + r1 + " -> " + r5 + ")");
    com.arcane.execution.AreaDial.add(player.getUUID(), 4);
    float dialed = com.arcane.execution.CastChannels.areaRadius(player, spell, 1.0);
    com.arcane.execution.AreaDial.reset(player.getUUID());
    h.assertTrue(dialed > r1 * 2.0f, "o regulador tambem entra no raio mostrado (" + dialed + ")");
    // vira a mira: o pilar nao acompanha
    player.setYRot(90f);
    player.setXRot(35f);
    h.runAfterDelay(30, () -> {
      var later = com.arcane.execution.CastChannels.heldAnchor(player.getUUID());
      h.assertTrue(later != null && later.distanceToSqr(anchor) < 1.0e-6, "mirar em outro lugar nao move a coluna mantida");
      com.arcane.execution.CastChannels.release(player);
      h.assertTrue(com.arcane.execution.CastChannels.heldAnchor(player.getUUID()) == null, "soltar libera o ponto");
      h.succeed();
    });
  }

  @GameTest(template = "empty")
  public static void configProfilesSwitchSeveralRulesAtOnce(GameTestHelper h) {
    var before = com.arcane.config.WorldRules.snapshot();
    try {
      com.arcane.config.WorldRules.applyProfile(1);
      h.assertTrue(com.arcane.config.WorldRules.profile() == 1, "perfil Seguro reconhecido");
      h.assertTrue(!com.arcane.config.ArcaneConfig.BLOCK_BREAKING_SPELLS.get() && !com.arcane.config.ArcaneConfig.HYDRA_DEVASTATION.get(), "Seguro: magia nao quebra blocos e a Hidra nao devasta");
      com.arcane.config.WorldRules.applyProfile(3);
      h.assertTrue(com.arcane.config.WorldRules.profile() == 3 && !com.arcane.config.ArcaneConfig.AREA_HARD_CAPS.get(), "Sem limites tira os tetos");
      com.arcane.config.WorldRules.advance("scars");
      h.assertTrue(com.arcane.config.WorldRules.profile() == 0, "mudar uma regra sozinha vira personalizado");
      com.arcane.config.WorldRules.advance("profile");
      h.assertTrue(com.arcane.config.WorldRules.profile() == 1, "avancar o perfil a partir do personalizado vai para o Seguro");
    } finally {
      com.arcane.config.WorldRules.applyProfile(2);
      for (var r : com.arcane.config.WorldRules.ALL) if (!r.key().equals("profile")) r.set().accept(before.get(r.key()));
      com.arcane.config.WorldRules.refresh();
    }
    h.succeed();
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 40)
  public static void ritualCirclesOfRadiusSixteenOrMoreLeakMana(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 8);
    h.assertTrue(com.arcane.ritual.RitualLeaks.leakingRadius(h.getLevel(), altarAbs(h)) == 0, "circulo de raio 8 nao vaza");
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 16);
    h.assertTrue(com.arcane.ritual.RitualLeaks.leakingRadius(h.getLevel(), altarAbs(h)) == 16, "circulo de raio 16 vaza mana");
    h.assertTrue(com.arcane.ritual.RitualLeaks.senseReach(64) > com.arcane.ritual.RitualLeaks.senseReach(16)
        && com.arcane.ritual.RitualLeaks.soundReach(32) == 192, "quanto maior o circulo, mais longe chegam os sinais");
    h.assertTrue("ne".equals(com.arcane.ritual.RitualLeaks.direction(new BlockPos(0, 0, 0), new BlockPos(10, 0, -10))), "direcao do vazamento");
    h.assertTrue(com.arcane.ritual.RitualService.offeringArea(0) == 1.0 && com.arcane.ritual.RitualService.offeringArea(100) == 1.5
        && com.arcane.ritual.RitualService.offeringArea(900) == 2.0, "oferendas aumentam o raio do ritual (k = 0,5, ate o dobro)");
    h.runAfterDelay(2, () -> h.assertTrue(com.arcane.ritual.RitualLeaks.registered(h.getLevel(), altarAbs(h)), "o altar carregado se registrou"));
    h.runAfterDelay(30, () -> {
      h.assertTrue(com.arcane.ritual.RitualLeaks.measuredRadius(h.getLevel(), altarAbs(h)) == 16, "o servico mediu o circulo sozinho e ele esta vazando");
      h.succeed();
    });
  }

  @GameTest(template = "empty")
  public static void pierceStacksTwentyPercentPerLayerAndEachLayerCostsDouble(GameTestHelper h) {
    floor(h);
    Pig plain = pig(h, 1, 1), pierced = pig(h, 4, 4);
    for (Pig p : List.of(plain, pierced)) p.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 3));
    cast(h, plain, g("self"), g("damage", "amount", 5.0));
    cast(h, pierced, g("self"), g("damage", "amount", 5.0), g("pierce"), g("pierce"), g("pierce"), g("pierce"), g("pierce"));
    float lostPlain = plain.getMaxHealth() - plain.getHealth(), lostPierced = pierced.getMaxHealth() - pierced.getHealth();
    h.assertTrue(lostPlain < 1.5f, "Resistencia IV segura 80% do dano comum (perdeu " + lostPlain + ")");
    h.assertTrue(Math.abs(lostPierced - 5.0f) < 0.3f, "5 camadas de Perfurar passam o dano inteiro (perdeu " + lostPierced + ")");
    Inscription.GlyphRegistryAccessor reg = com.arcane.glyph.GlyphRegistry::get;
    int one = new Inscription(List.of(g("self"), g("damage"), g("pierce"))).totalManaCost(reg);
    int three = new Inscription(List.of(g("self"), g("damage"), g("pierce"), g("pierce"), g("pierce"))).totalManaCost(reg);
    h.assertTrue(three - one == 8 * 2 + 8 * 4, "a 2a camada custa o dobro e a 3a o quadruplo (" + one + " -> " + three + ")");
    var p1 = com.arcane.execution.CastingService.manaCostOf(new Inscription(List.of(g("self"), g("damage"), g("pierce"))), com.arcane.execution.PrecisionSnapshot.WORKBENCH);
    var p5 = com.arcane.execution.CastingService.manaCostOf(new Inscription(List.of(g("self"), g("damage"), g("pierce"), g("pierce"), g("pierce"), g("pierce"), g("pierce"))), com.arcane.execution.PrecisionSnapshot.WORKBENCH);
    h.assertTrue(p5 > p1 * 10, "100% de perfuracao custa muito caro (" + p1 + " -> " + p5 + " de mana)");
    h.succeed();
  }
}
