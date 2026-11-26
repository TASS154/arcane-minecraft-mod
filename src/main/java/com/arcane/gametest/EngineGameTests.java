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

/** Motor: regioes, alvos tipados, zonas e eventos, matriz, imunidade, potencia pela escrita, mana, area do ritual. (dividido de ArcaneGameTests em 25 nov; os testes nao mudaram.) */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class EngineGameTests extends GameTestSupport {

  @GameTest(template = "empty")
  public static void auraWithConeOnlyHitsCreaturesInFront(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    var front = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 5));
    var behind = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 1));
    float fb = front.getHealth(), bb = behind.getHealth();
    cast(h, caster, g("aura"), g("shape_cone"), g("damage"));
    assertHurt(h, front, fb, "criatura na frente do cone");
    assertUnhurt(h, behind, bb, "criatura atras do cone");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void waveWithSphereHitsEveryoneAround(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    var behind = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 1));
    float bb = behind.getHealth();
    cast(h, caster, g("wave"), g("shape_sphere"), g("damage"));
    assertHurt(h, behind, bb, "a Onda com Esfera atinge tambem quem esta atras");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void pillarWithSelfOriginStrikesAroundTheCaster(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    var near = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 3));
    float nb = near.getHealth();
    cast(h, caster, g("pillar"), g("origin_self"), g("damage"));
    assertHurt(h, near, nb, "o Pilar com origem Eu cai em volta do conjurador");
    h.succeed();
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 100)
  public static void barrierWithColumnAndConeBuildBlocks(GameTestHelper h) {
    for (int x = 0; x < 15; x++) for (int z = 0; z < 15; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    Pig c = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(7, 1, 7));
    c.setYRot(0);
    c.setXRot(0);
    h.assertTrue(CastingService.tryCast(c, new Inscription(List.of(g("barrier"), g("shape_column"), g("origin_self"))), h.getLevel()).success(), "coluna");
    int column = countBarrier(h, 7);
    h.assertTrue(column > 20, "cilindro oco com blocos, tem " + column);
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void fireOnEmptyPointLightsTheGround(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    // O Raio nao entrega mais ponto vazio (26 nov): o ponto vem direto ao efeito, como na detonacao do Projetil ou na
    // marca da Coluna
    var spell = new com.arcane.inscription.Inscription(List.of(g("self"), g("fire")));
    var ctx = new com.arcane.execution.ExecutionContext(h.getLevel(), caster, spell,
        com.arcane.inscription.InscriptionCompiler.compile(spell), com.arcane.execution.EnvironmentalSnapshot.NEUTRAL,
        com.arcane.execution.PrecisionSnapshot.of(spell), com.arcane.execution.ExecutionContext.TriggerEvent.cast());
    var fire = new ResourceLocation("arcane", "fire");
    com.arcane.glyph.GlyphHandlerRegistry.getEffect(fire).orElseThrow().apply(ctx,
        new com.arcane.execution.EffectCall(fire, fire, java.util.Map.of("duration", 60.0), List.of()),
        com.arcane.execution.TargetList.ofPoint(h.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1.2, 5.5))));
    boolean found = false;
    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
      if (h.getBlockState(new BlockPos(3 + dx, 1, 5 + dz)).is(Blocks.FIRE)) found = true;
    }
    h.assertTrue(found, "o Fogo num ponto vazio deveria acender uma chama no chao");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void poisonOnEmptyPointLeavesACloud(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    cast(h, caster, g("pillar"), g("origin_self"), g("poison"));
    var clouds = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class,
        new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(3, 1, 3))).inflate(4));
    h.assertTrue(!clouds.isEmpty(), "o Veneno num ponto vazio deveria deixar uma nuvem");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void damageBreaksFragileBlockButNotStone(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    h.setBlock(new BlockPos(3, 1, 3), Blocks.GLASS);
    cast(h, caster, g("ray"), g("damage"));
    h.assertTrue(h.getBlockState(new BlockPos(3, 1, 3)).isAir(), "o Dano deveria quebrar o vidro mirado");
    Pig caster2 = pig(h, 5, 1);
    h.setBlock(new BlockPos(5, 1, 3), Blocks.STONE);
    cast(h, caster2, g("ray"), g("damage"));
    h.assertTrue(h.getBlockState(new BlockPos(5, 1, 3)).is(Blocks.STONE), "a pedra nao deveria quebrar");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void previewWarnsAboutNoOpCombinations(GameTestHelper h) {
    var ok = com.arcane.execution.SpellPreview.describe(new Inscription(List.of(g("ray"), g("fire"))));
    h.assertTrue(countWarnings(ok) == 0, "Raio + Fogo nao deveria ter avisos");
    var selfDig = com.arcane.execution.SpellPreview.describe(new Inscription(List.of(g("self"), g("dig"))));
    h.assertTrue(countWarnings(selfDig) == 1, "Eu + Escavar nao faz nada e deveria avisar");
    var shapeOnRay = com.arcane.execution.SpellPreview.describe(new Inscription(List.of(g("ray"), g("shape_dome"), g("damage"))));
    h.assertTrue(countWarnings(shapeOnRay) == 1, "formato no Raio e ignorado e deveria avisar");
    var hostileBlocks = com.arcane.execution.SpellPreview.describe(new Inscription(List.of(g("touch"), g("hostile"), g("dig"))));
    h.assertTrue(countWarnings(hostileBlocks) == 1, "filtro Hostil deixa so criaturas, Escavar so age em blocos");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void zoneRepeatsTheSpellOverTime(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    var victim = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 4));
    cast(h, caster, g("aura"), g("zone", "seconds", 2.0), g("damage", "amount", 1.0));
    float afterCast = victim.getHealth();
    h.assertTrue(afterCast < victim.getMaxHealth(), "a primeira entrega fere");
    h.assertTrue(com.arcane.execution.Zones.activeFor(caster.getUUID()) == 1, "deveria haver 1 zona ativa");
    h.runAfterDelay(30, () -> {
      h.assertTrue(victim.getHealth() < afterCast, "a zona deveria ferir de novo (vida " + victim.getHealth() + ")");
      com.arcane.execution.Zones.clearFor(caster.getUUID());
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void zoneEndEventRunsOnlyWhenTheZoneEnds(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    var victim = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 4));
    float before = victim.getHealth();
    cast(h, caster, g("aura"), g("zone", "seconds", 2.0), g("on_zone_end"), g("damage", "amount", 1.0));
    h.assertTrue(victim.getHealth() >= before, "o trecho 'Quando a zona acabar' nao roda ao conjurar");
    h.runAfterDelay(25, () -> h.assertTrue(victim.getHealth() >= before, "nem durante a zona"));
    h.runAfterDelay(55, () -> {
      h.assertTrue(victim.getHealth() < before, "deveria ferir quando a zona acabou");
      h.assertTrue(com.arcane.execution.Zones.activeFor(caster.getUUID()) == 0, "zona encerrada");
      h.succeed();
    });
  }

  @GameTest(template = "empty")
  public static void casterHasAtMostTwoZones(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    for (int i = 0; i < 3; i++) cast(h, caster, g("aura"), g("zone"), g("damage"));
    h.assertTrue(com.arcane.execution.Zones.activeFor(caster.getUUID()) == 2, "maximo de 2 zonas por conjurador");
    com.arcane.execution.Zones.clearFor(caster.getUUID());
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void summonsIgnoreZoneAndPreviewWarns(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    h.assertTrue(!com.arcane.execution.Zones.zoneable(com.arcane.inscription.InscriptionCompiler.compile(new Inscription(List.of(g("summon_wolves"), g("zone"))))), "invocacao nao vira zona");
    var summonZone = com.arcane.execution.SpellPreview.describe(new Inscription(List.of(g("summon_wolves"), g("zone"))));
    h.assertTrue(countWarnings(summonZone) == 1, "Invocar + Zona deveria avisar");
    var endNoZone = com.arcane.execution.SpellPreview.describe(new Inscription(List.of(g("aura"), g("on_zone_end"), g("damage"))));
    h.assertTrue(countWarnings(endNoZone) == 1, "Quando a zona acabar sem Zona deveria avisar");
    var good = com.arcane.execution.SpellPreview.describe(new Inscription(List.of(g("aura"), g("zone"), g("on_zone_end"), g("damage"))));
    h.assertTrue(countWarnings(good) == 0, "Aura + Zona + fim nao deveria avisar");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void interactionTableIsLoadedWithoutProblems(GameTestHelper h) {
    h.assertTrue(com.arcane.execution.InteractionRules.rules().size() >= 7, "regras carregadas: " + com.arcane.execution.InteractionRules.rules().size());
    h.assertTrue(com.arcane.execution.InteractionRules.problems().isEmpty(), "problemas: " + com.arcane.execution.InteractionRules.problems());
    // Adia o fim: limpar a arena na hora apagaria criaturas de testes vizinhos que ainda rodam.
    h.runAfterDelay(15, h::succeed);
  }

  // Lote proprio: as ondas e atracoes da matriz alcancam arenas vizinhas.
  @GameTest(template = "empty", timeoutTicks = 60, batch = "isolated_matrix")
  public static void everyFormEffectFilterAndModifierCombinationRuns(GameTestHelper h) {
    floor(h);
    List<String> forms = List.of("self", "touch", "projectile", "ray", "aura", "wave", "pillar", "barrier");
    List<String> regionForms = List.of("aura", "wave", "pillar", "barrier");
    List<String> effects = glyphsOf(com.arcane.glyph.GlyphCategory.EFFECT, false);
    List<String> filters = glyphsOf(com.arcane.glyph.GlyphCategory.FILTER, false);
    List<String> shapes = List.of("shape_dome", "shape_sphere", "shape_ring", "shape_cone", "shape_column", "shape_wall", "origin_aim", "origin_self");
    List<String> probe = List.of("damage", "fire", "heal", "poison", "push");
    List<String> failures = new ArrayList<>();
    int runs = 0;
    for (String form : forms) for (String effect : effects) { runs++; add(failures, tryCombo(h, g(form), g(effect))); }
    for (String form : regionForms) for (String mod : shapes) for (String effect : probe) { runs++; add(failures, tryCombo(h, g(form), g(mod), g(effect))); }
    for (String form : forms) for (String filter : filters) { runs++; add(failures, tryCombo(h, g(form), g(filter), g("damage"))); }
    for (String form : forms) for (String effect : List.of("damage", "fire", "heal")) {
      runs++;
      add(failures, tryCombo(h, g(form), g("zone", "seconds", 2.0), g(effect)));
      add(failures, tryCombo(h, g(form), g("zone", "seconds", 2.0), g("on_zone_end"), g(effect)));
    }
    h.assertTrue(runs > 300, "a matriz deveria ter centenas de combinacoes, tem " + runs);
    h.assertTrue(failures.isEmpty(), failures.size() + " combinacoes com problema, ex.: " + failures.subList(0, Math.min(8, failures.size())));
    h.runAfterDelay(15, h::succeed);
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void casterIsImmuneToItsOwnAreaSpells(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    float hp = caster.getHealth();
    StringBuilder trail = new StringBuilder();
    cast(h, caster, g("pillar"), g("origin_self"), g("poison"));
    trail.append("veneno=").append(caster.getHealth()).append(' ');
    cast(h, caster, g("barrier"), g("shape_column"), g("origin_self"), g("damage"));
    trail.append("barreira=").append(caster.getHealth()).append(' ');
    cast(h, caster, g("aura"), g("damage"));
    trail.append("aura=").append(caster.getHealth()).append(' ');
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 5));
    float cowHp = cow.getHealth();
    cast(h, caster, g("ray"), g("explode", "radius", 6.0));
    trail.append("explosao=").append(caster.getHealth());
    h.runAfterDelay(10, () -> {
      h.assertTrue(caster.isAlive() && !caster.isRemoved(), "o conjurador continua vivo e continua um porco");
      h.assertTrue(caster.getHealth() >= hp, "o conjurador nao deveria se ferir (vida " + caster.getHealth() + "; " + trail + "; efeitos=" + caster.getActiveEffects() + "; fogo=" + caster.isOnFire() + ")");
      h.assertTrue(!caster.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "nem se envenenar com a propria nuvem");
      h.assertTrue(cow.getHealth() < cowHp || !cow.isAlive(), "mas a vaca continua sendo atingida");
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void casterIsImmuneToItsOwnLightning(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 3));
    cast(h, caster, g("ray"), g("thunder"));
    h.runAfterDelay(10, () -> {
      h.assertTrue(caster.isAlive() && !caster.isRemoved(), "o porco conjurador nao vira piglin com o proprio raio");
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void selfFormStillHurtsOnPurpose(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    float hp = caster.getHealth();
    cast(h, caster, g("self"), g("damage"));
    h.assertTrue(caster.getHealth() < hp, "Eu + Dano ainda fere o conjurador (de proposito)");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void destructiveBlastBreaksBlocks(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.STONE);
    h.setBlock(new BlockPos(3, 2, 4), Blocks.DIRT);
    float hp = caster.getHealth();
    cast(h, caster, g("ray"), g("blast"));
    h.assertTrue(h.getBlockState(new BlockPos(3, 2, 4)).isAir(), "a Explosao Destrutiva deveria quebrar a terra do alvo");
    h.assertTrue(caster.getHealth() >= hp, "o conjurador e imune");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void plainExplosionStillKeepsBlocks(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.DIRT);
    cast(h, caster, g("ray"), g("explode"));
    h.assertTrue(h.getBlockState(new BlockPos(3, 1, 4)).is(Blocks.DIRT), "a Explosao comum nao destroi blocos");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void chainArcsDamageToNearbyCreatures(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    var a = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 3));
    var b = h.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(1, 1, 4));
    float bHp = b.getHealth();
    cast(h, caster, g("ray"), g("damage"));
    h.assertTrue(b.getHealth() >= bHp, "sem Cadeia a segunda criatura nao e ferida");
    cast(h, caster, g("ray"), g("damage"), g("chain"));
    h.assertTrue(b.getHealth() < bHp, "com Cadeia o dano salta para a criatura proxima (vida " + b.getHealth() + ")");
    h.assertTrue(com.arcane.execution.SpellPreview.describe(new Inscription(List.of(g("ray"), g("heal"), g("chain")))).stream()
        .anyMatch(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().endsWith("chain_unsupported")), "Cadeia em Cura deveria avisar");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void chainMakesExplosionsDetonateAgain(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    var a = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(3, 1, 3));
    var b = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 1, 5));
    float aHp = a.getHealth(), bHp = b.getHealth();
    cast(h, caster, g("ray"), g("explode"));
    h.runAfterDelay(15, () -> {
      float lostPlain = (aHp - a.getHealth()) + (bHp - b.getHealth());
      a.setHealth(aHp);
      b.setHealth(bHp);
      a.invulnerableTime = 0;
      b.invulnerableTime = 0;
      var pa = h.absolutePos(new BlockPos(3, 1, 3));
      var pb = h.absolutePos(new BlockPos(1, 1, 5));
      a.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
      b.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
      a.moveTo(pa.getX() + 0.5, pa.getY(), pa.getZ() + 0.5);
      b.moveTo(pb.getX() + 0.5, pb.getY(), pb.getZ() + 0.5);
      cast(h, caster, g("ray"), g("explode"), g("chain"));
      h.runAfterDelay(25, () -> {
        float lostChain = (aHp - a.getHealth()) + (bHp - b.getHealth());
        h.assertTrue(lostChain > lostPlain, "com Cadeia a perda total deveria ser maior: " + lostChain + " vs " + lostPlain);
        h.succeed();
      });
    });
  }

  @GameTest(template = "empty")
  public static void powerComesFromWhereTheSpellWasWritten(GameTestHelper h) {
    var bench = new Inscription(List.of(g("self"), g("heal")));
    h.assertTrue(com.arcane.execution.PrecisionSnapshot.of(bench).powerMultiplier() == 1.0, "escrita na Bancada: 100%");
    var field = bench.withMetadata(new com.arcane.inscription.InscriptionMetadata("x", new ResourceLocation("arcane", "common"),
        new ResourceLocation("arcane", "arcane_pen"), com.arcane.execution.PrecisionSnapshot.FIELD_POWER, java.util.Optional.empty(), 0L,
        com.arcane.inscription.InscriptionSourceKind.SCROLL));
    h.assertTrue(Math.abs(com.arcane.execution.PrecisionSnapshot.of(field).powerMultiplier() - 0.3) < 1e-9, "escrita no campo (caneta): 30%");
    h.assertTrue(com.arcane.execution.PrecisionSnapshot.of(field).manaCostMultiplier() == 1.0, "o custo de mana e o mesmo exibido (sem sobretaxa escondida)");
    h.assertTrue(com.arcane.execution.PrecisionSnapshot.FIELD.manaCostMultiplier() == 1.0, "FIELD sem sobretaxa");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void handDrawnCirclesGoBeyondTheCompass(GameTestHelper h) {
    int[] hand = com.arcane.ritual.RitualTemplate.HAND_RADII;
    h.assertTrue(hand[hand.length - 1] == 64, "a mao chega a raio 64 (128x128)");
    h.assertTrue(com.arcane.ritual.RitualTemplate.RADII[com.arcane.ritual.RitualTemplate.RADII.length - 1] == 16, "o Compasso continua em 16");
    for (int r : hand) {
      h.assertTrue(com.arcane.ritual.RitualService.handInkCost(r) * 2 <= com.arcane.ritual.RitualService.inkCost(r) + 1,
          "a mao gasta cerca de metade da tinta (raio " + r + ")");
    }
    h.assertTrue(com.arcane.ritual.RitualLevel.forRadius(64) == com.arcane.ritual.RitualLevel.MAJOR, "raio acima de 16 conta como ritual maior");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void summonedCreaturesGrowWithTheirTier(GameTestHelper h) {
    var small = ModEntities.SPECTRAL_WOLF.get().create(h.getLevel());
    var big = ModEntities.SPECTRAL_WOLF.get().create(h.getLevel());
    small.setTier(0);
    big.setTier(2);
    h.assertTrue(big.getMaxHealth() > small.getMaxHealth() * 2, "o lobo maior tem bem mais vida (" + small.getMaxHealth() + " -> " + big.getMaxHealth() + ")");
    h.assertTrue(big.getBbHeight() > small.getBbHeight() * 2, "e e maior de fato (hitbox)");
    h.assertTrue(big.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
        > small.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), "e bate mais forte");
    var g0 = ModEntities.ARCANE_GOLEM.get().create(h.getLevel());
    var g2 = ModEntities.ARCANE_GOLEM.get().create(h.getLevel());
    g0.setTier(0);
    g2.setTier(2);
    h.assertTrue(g2.getMaxHealth() > g0.getMaxHealth() * 2 && g2.getBbHeight() > g0.getBbHeight() * 1.5, "o golem maior tambem cresce");
    h.succeed();
  }

  @GameTest(template = "ritual_arena")
  public static void summoningFormsHonourCountAndSize(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 8);
    Pig caster = pigAt(h, 17, 14);
    var result = ritual(h, caster, com.arcane.ritual.ComponentSource.unlimited(), false, new GlyphInstance(new ResourceLocation("arcane", "summon_golem"), Map.of("count", 2.0, "size", 3.0))); // "count" foi removido do golem: so ha um
    h.assertTrue(result.success(), "ritual do golem: " + result.outcome());
    var golems = h.getLevel().getEntitiesOfClass(com.arcane.entity.ArcaneGolemEntity.class, new net.minecraft.world.phys.AABB(altarAbs(h)).inflate(30));
    h.assertTrue(golems.size() == 1, "um golem so (a quantidade nao e mais opcao), vieram " + golems.size());
    h.assertTrue(golems.stream().allMatch(x -> x.tier() == 2), "do tamanho maximo");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void maxManaGrowsWithGlyphsPotionsAndTheCommandBase(GameTestHelper h) {
    var player = h.makeMockPlayer();
    var mana = com.arcane.grimoire.GrimoireAttachments.getMana(player).orElseThrow();
    mana.setBaseMax(100f);
    com.arcane.execution.ManaBonuses.refresh(player);
    float start = mana.max();
    int known0 = com.arcane.execution.ManaBonuses.discoveredGlyphs(player);
    h.assertTrue(Math.abs(start - (100f + known0 * com.arcane.config.ArcaneConfig.MANA_PER_GLYPH.get().floatValue())) < 0.01f, "max = base + glifos (" + start + ")");

    int learned = 0;
    for (var id : com.arcane.glyph.GlyphRegistry.all().keySet()) {
      if (learned >= 10) break;
      com.arcane.grimoire.GrimoireAttachments.get(player).orElseThrow().learnGlyph(id, com.arcane.grimoire.GlyphKnowledge.MASTERED);
      learned++;
    }
    com.arcane.execution.ManaBonuses.refresh(player);
    h.assertTrue(mana.max() > start, "descobrir glifos aumenta o maximo (" + start + " -> " + mana.max() + ")");

    float before = mana.max();
    player.getActiveEffectsMap().put(com.arcane.registry.ModEffects.MANA_VESSEL.get(), new net.minecraft.world.effect.MobEffectInstance(com.arcane.registry.ModEffects.MANA_VESSEL.get(), 200));
    com.arcane.execution.ManaBonuses.refresh(player);
    h.assertTrue(Math.abs(mana.max() - (before + 50f)) < 0.01f, "pocao de vaso soma 50% da base (" + before + " -> " + mana.max() + ")");

    mana.set(0f);
    float regen1 = mana.regenPerTick();
    player.getActiveEffectsMap().put(com.arcane.registry.ModEffects.MANA_SURGE.get(), new net.minecraft.world.effect.MobEffectInstance(com.arcane.registry.ModEffects.MANA_SURGE.get(), 200));
    com.arcane.execution.ManaBonuses.refresh(player);
    h.assertTrue(mana.regenPerTick() > regen1 * 3.9f, "pocao de fluxo multiplica a regeneracao (" + regen1 + " -> " + mana.regenPerTick() + ")");

    mana.setBaseMax(300f);
    com.arcane.execution.ManaBonuses.refresh(player);
    h.assertTrue(mana.baseMax() == 300f && mana.max() > 300f, "o comando edita a base e os bonus continuam por cima");
    mana.set(99999f);
    h.assertTrue(mana.current() <= mana.max(), "a mana atual nunca passa do maximo");
    h.succeed();
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 200)
  public static void ritualAreaSpellsCoverTheWholeCircle(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 8);
    Pig inside = pigAt(h, 24, 17);   // 7 blocos do altar: dentro do circulo, alem do raio normal do Pilar
    Pig outside = pigAt(h, 30, 17);  // 13 blocos: fora do circulo
    float insideHp = inside.getHealth(), outsideHp = outside.getHealth();
    Pig caster = pigAt(h, 17, 22);
    var result = ritual(h, caster, com.arcane.ritual.ComponentSource.unlimited(), false, g("pillar"), g("damage", "amount", 6.0));
    h.assertTrue(result.success(), "ritual do pilar: " + result.outcome());
    h.assertTrue(inside.getHealth() < insideHp, "o pilar do ritual cobre o circulo inteiro (vida " + inside.getHealth() + ")");
    h.assertTrue(outside.getHealth() == outsideHp, "e nao passa do circulo");
    h.succeed();
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 200)
  public static void ritualExplosionsTileTheWholeCircle(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 8);
    Pig edge = pigAt(h, 24, 17);
    float hp = edge.getHealth();
    Pig caster = pigAt(h, 17, 22);
    var result = ritual(h, caster, com.arcane.ritual.ComponentSource.unlimited(), false, g("self"), g("explode", "radius", 2.0));
    h.assertTrue(result.success(), "ritual da explosao: " + result.outcome());
    h.assertTrue(edge.getHealth() < hp, "a explosao do ritual se espalha pelo circulo todo, nao so no alvo (vida " + edge.getHealth() + ")");
    h.succeed();
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 200)
  public static void symmetryAndPatternShapeRitualPower(GameTestHelper h) {
    arena(h);
    com.arcane.ritual.RitualService.drawCircle(h.getLevel(), altarAbs(h), 4);
    var perfect = com.arcane.ritual.RitualService.evaluate(h.getLevel(), altarAbs(h));
    h.assertTrue(perfect.symmetry() > 0.999 && perfect.quality() > 0.99, "circulo completo: simetria total, foi " + perfect);
    var cell = new java.util.ArrayList<>(com.arcane.ritual.RitualTemplate.cells(4)).get(0);
    h.setBlock(new BlockPos(17 + cell.dx(), 1, 17 + cell.dz()), Blocks.AIR);
    var crooked = com.arcane.ritual.RitualService.evaluate(h.getLevel(), altarAbs(h));
    h.assertTrue(crooked.level() == com.arcane.ritual.RitualLevel.MINOR, "um traco a menos ainda vale: " + crooked);
    h.assertTrue(crooked.symmetry() < 1.0 && crooked.quality() < crooked.completeness(),
        "fora de simetria diminui o poder alem do que falta (simetria " + crooked.symmetry() + ")");
    com.arcane.ritual.RitualService.repairBySymmetry(h.getLevel(), altarAbs(h));
    h.assertTrue(com.arcane.ritual.RitualService.evaluate(h.getLevel(), altarAbs(h)).symmetry() > 0.999, "o Espelho devolve a simetria");

    // o Compasso Perfeito (criativo) nao depende do chao e e perfeito mesmo gigante
    for (int x = 0; x < 35; x++) for (int z = 0; z < 35; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.AIR);
    com.arcane.ritual.RitualService.drawPerfectCircle(h.getLevel(), altarAbs(h), 16);
    var big = com.arcane.ritual.RitualService.evaluate(h.getLevel(), altarAbs(h));
    h.assertTrue(big.level() == com.arcane.ritual.RitualLevel.MAJOR && big.symmetry() > 0.999 && big.completeness() > 0.999, "circulo perfeito no ar: " + big);
    h.assertTrue(com.arcane.ritual.RitualLevel.MAJOR.power() >= 10.0 && big.quality() <= 1.0, "teto de 1000% (x10)");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void auraReachesBlocksNotOnlyCreatures(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    int before = 0;
    for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) if (h.getBlockState(new BlockPos(x, 0, z)).is(Blocks.STONE)) before++;
    cast(h, caster, g("aura", "radius", 3.0), g("dig", "power", 5.0));
    int after = 0;
    for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) if (h.getBlockState(new BlockPos(x, 0, z)).is(Blocks.STONE)) after++;
    h.assertTrue(after < before, "a Aura entrega os blocos ao redor aos efeitos (pedras " + before + " -> " + after + ")");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void telekinesisBlockGlidesToTheAimWithoutOvershooting(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 1, 1);
    caster.setYRot(0);
    h.setBlock(new BlockPos(1, 1, 4), Blocks.STONE);
    cast(h, caster, g("ray"), g("telekinesis", "distance", 4.0));
    var fbs = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(1, 1, 4))).inflate(4));
    h.assertTrue(fbs.size() == 1, "o bloco virou entidade (" + fbs.size() + ")");
    var fb = fbs.get(0);
    java.util.List<Double> dist = new java.util.ArrayList<>();
    for (int k = 1; k <= 6; k++) {
      int at = k * 5;
      h.runAfterDelay(at, () -> {
        var anchor = caster.getEyePosition().add(caster.getLookAngle().scale(4.0));
        dist.add(fb.position().add(0, 0.49, 0).distanceTo(anchor));
      });
    }
    h.runAfterDelay(40, () -> {
      h.assertTrue(fb.isAlive(), "continua no ar (nao caiu nem virou bloco)");
      h.assertTrue(dist.get(dist.size() - 1) < 0.6, "chegou na mira: " + dist);
      for (int i = 1; i < dist.size(); i++) h.assertTrue(dist.get(i) <= dist.get(i - 1) + 0.05, "aproxima sem passar do ponto e voltar: " + dist);
      com.arcane.execution.Telekinesis.release(caster.getUUID());
      h.succeed();
    });
  }

  @GameTest(template = "empty")
  public static void telekinesisHoldsCreaturesAndBlockGroups(GameTestHelper h) {
    floor(h);
    // grupos: mesmo tipo conectado, esfera e bloco unico
    for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
    var seed = h.absolutePos(new BlockPos(3, 1, 3));
    h.assertTrue(com.arcane.execution.Telekinesis.collect(h.getLevel(), seed, 0, true, 120).size() == 9, "9 blocos de terra conectados");
    h.assertTrue(com.arcane.execution.Telekinesis.collect(h.getLevel(), seed, 1, false, 120).size() == 6, "esfera de raio 1: o bloco e 5 vizinhos solidos");
    h.assertTrue(com.arcane.execution.Telekinesis.collect(h.getLevel(), seed, 0, false, 120).size() == 1, "sem grupo: so o bloco");
    h.assertTrue(com.arcane.execution.Telekinesis.collect(h.getLevel(), seed, 1, true, 120).size() == 5, "mesmo tipo limitado pela esfera: terra dentro do raio 1");

    // criatura: segurada e levada para onde o conjurador mira (tira a terra do caminho da mira)
    for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
    Pig caster = pig(h, 3, 1);
    Pig target = pig(h, 3, 4);
    double z0 = target.getZ();
    cast(h, caster, g("ray"), g("telekinesis"));
    h.assertTrue(com.arcane.execution.Telekinesis.heldCount(caster.getUUID()) >= 1, "a criatura foi segurada");
    for (int i = 0; i < 20; i++) com.arcane.execution.Telekinesis.tick(h.getLevel().getServer());
    h.assertTrue(target.getZ() > z0 + 0.5 && target.isNoGravity(), "e levada em direcao a mira, sem gravidade (z " + z0 + " -> " + target.getZ() + ")");
    com.arcane.execution.Telekinesis.release(caster.getUUID());
    h.assertTrue(!target.isNoGravity() && com.arcane.execution.Telekinesis.heldCount(caster.getUUID()) == 0, "soltar devolve a gravidade");

    // bloco: o grupo vira entidades de bloco flutuantes
    Pig caster2 = pig(h, 1, 1);
    caster2.setYRot(0);
    h.setBlock(new BlockPos(1, 1, 4), Blocks.STONE);
    h.setBlock(new BlockPos(1, 2, 4), Blocks.STONE);
    cast(h, caster2, g("ray"), g("telekinesis", "same_type", 1.0));
    int blocks = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(1, 1, 4))).inflate(8)).size();
    h.assertTrue(blocks >= 2, "os dois blocos de pedra viraram entidades flutuantes (" + blocks + ")");
    com.arcane.execution.Telekinesis.release(caster2.getUUID());
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void floatGlyphGrantsTheFlightEffect(GameTestHelper h) {
    Pig pig = pig(h, 3, 3);
    cast(h, pig, g("self"), g("float", "speed", 1.5));
    var effect = pig.getEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get());
    h.assertTrue(effect != null, "o efeito Flutuar foi aplicado");
    h.assertTrue(effect.getAmplifier() == 15, "a velocidade vai na amplificacao (x10): " + effect.getAmplifier());
    h.assertTrue(effect.getDuration() >= 190, "duracao padrao ~200 ticks: " + effect.getDuration());
    h.succeed();
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 200)
  public static void sealedChamberHoldsACreatureUntilTheAltarBreaks(GameTestHelper h) {
    arena(h);
    var altar = altarAbs(h);
    com.arcane.world.SealedChambers.build(h.getLevel(), altar, net.minecraft.util.RandomSource.create(7));
    h.assertTrue(h.getLevel().getBlockState(altar).getBlock() instanceof com.arcane.block.ArcaneAltarBlock, "altar no centro");
    boolean chest = false;
    for (int dx = -5; dx <= 5 && !chest; dx++) for (int dz = -5; dz <= 5 && !chest; dz++)
      chest = h.getLevel().getBlockState(altar.offset(dx, 0, dz)).is(Blocks.CHEST);
    h.assertTrue(chest, "bau com o que sobrou do ritual");
    var box = new net.minecraft.world.phys.AABB(altar).inflate(6);
    var sealed = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box, m -> m.getPersistentData().contains(com.arcane.world.SealedChambers.SEALED_TAG));
    h.assertTrue(sealed.size() == 1, "uma criatura selada (" + sealed.size() + ")");
    var mob = sealed.get(0);
    h.assertTrue(mob.isNoAi() && mob.isInvulnerable() && mob.isCurrentlyGlowing() || (mob.isNoAi() && mob.isInvulnerable()), "selada: sem IA e invulneravel");
    int freed = com.arcane.world.SealedChambers.free(h.getLevel(), altar);
    h.assertTrue(freed == 1 && !mob.isNoAi() && !mob.isInvulnerable(), "quebrar o altar solta a criatura");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void inscriptionRemembersTheSurfaceItWasDrawnOn(GameTestHelper h) {
    BlockPos abs = h.absolutePos(new BlockPos(3, 1, 3));
    Inscription spell = new Inscription(List.of(g("self"), g("heal")));
    var index = new WorldInscriptionIndex();
    index.put(new InscriptionAnchor(abs, spell, InscribingService.TRIGGER_ON_TOUCH, new ResourceLocation("arcane", "common"), net.minecraft.core.Direction.NORTH));
    var reloaded = WorldInscriptionIndex.load(index.save(new CompoundTag()));
    h.assertTrue(reloaded.get(abs).orElseThrow().face() == net.minecraft.core.Direction.NORTH, "a face do desenho sobrevive ao salvar");
    h.succeed();
  }
}
