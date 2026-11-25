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

/** Glifos da lista aceita (rodada de 23 nov). (dividido de ArcaneGameTests em 25 nov; os testes nao mudaram.) */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class AcceptedGlyphsGameTests extends GameTestSupport {

  @GameTest(template = "empty", batch = "r23_util")
  public static void organizeLockAndBaptizeWork(GameTestHelper h) {
    floor(h);
    var box = new net.minecraft.world.SimpleContainer(5);
    box.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE, 10));
    box.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT, 5));
    box.setItem(3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE, 60));
    com.arcane.execution.handlers.LifeEffects.Organize.sort(box, 0, 5);
    h.assertTrue(box.getItem(0).is(net.minecraft.world.item.Items.DIRT) && box.getItem(1).getCount() == 64 && box.getItem(2).getCount() == 6,
        "Organizar junta as pilhas e ordena (" + box.getItem(0) + ", " + box.getItem(1) + ", " + box.getItem(2) + ")");
    var owner = casterWith(h);
    var other = casterWith(h);
    BlockPos door = h.absolutePos(new BlockPos(2, 1, 2));
    com.arcane.execution.WorldMarks.lock(h.getLevel(), door, owner.getUUID());
    h.assertTrue(com.arcane.execution.WorldMarks.mayOpen(h.getLevel(), door, owner) && !com.arcane.execution.WorldMarks.mayOpen(h.getLevel(), door, other),
        "so o dono abre o que foi trancado");
    com.arcane.execution.GuestLists.get(h.getLevel().getServer()).add(owner.getUUID(), other.getUUID());
    h.assertTrue(com.arcane.execution.WorldMarks.mayOpen(h.getLevel(), door, other), "convidado do dono tambem abre");
    com.arcane.execution.GuestLists.get(h.getLevel().getServer()).remove(owner.getUUID(), other.getUUID());
    h.assertTrue(com.arcane.execution.WorldMarks.unseal(h.getLevel(), door, owner.getUUID(), 1) && com.arcane.execution.WorldMarks.lockOwner(h.getLevel(), door) == null,
        "o dono tira a tranca com Dissipar fraco");
    var centre = h.absoluteVec(new net.minecraft.world.phys.Vec3(2, 1, 2));
    com.arcane.execution.WorldMarks.baptize(h.getLevel(), centre, 6, "Vale Teste", owner.getUUID());
    var area = com.arcane.execution.WorldMarks.areaAt(h.getLevel(), centre.add(3, 0, 0));
    h.assertTrue(area != null && area.name().equals("Vale Teste"), "dentro da area batizada");
    h.assertTrue(com.arcane.execution.WorldMarks.areaAt(h.getLevel(), centre.add(30, 0, 0)) == null, "fora dela, sem nome");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "r23_life")
  public static void lastBreathSavesOncePerDayAndSealBanishesIntoAnItem(GameTestHelper h) {
    floor(h);
    Pig lucky = pig(h, 1, 1);
    com.arcane.execution.WorldMarks.grantLastBreath(lucky, h.getLevel().getGameTime() + 1000);
    lucky.hurt(h.getLevel().damageSources().magic(), 100f);
    h.assertTrue(lucky.isAlive() && lucky.getHealth() >= 1.0f, "Folego Final segura a primeira morte com 1 de vida");
    com.arcane.execution.WorldMarks.grantLastBreath(lucky, h.getLevel().getGameTime() + 1000);
    lucky.invulnerableTime = 0;
    lucky.removeAllEffects();
    lucky.hurt(h.getLevel().damageSources().magic(), 100f);
    h.assertTrue(!lucky.isAlive(), "no mesmo dia nao salva de novo");

    Pig sealed = pig(h, 4, 4);
    sealed.setCustomName(net.minecraft.network.chat.Component.literal("Bacon"));
    var named = com.arcane.glyph.GlyphHandlerRegistry.getFilter(com.arcane.glyph.ArcaneIds.of("named")).orElseThrow();
    h.assertTrue(named.matchesEntity(null, sealed) && !named.matchesEntity(null, pig(h, 6, 6)), "filtro Nomeados");
    var item = com.arcane.execution.handlers.LoreEffects.SealCreature.banish(h.getLevel(), sealed);
    h.assertTrue(sealed.isRemoved() && item.is(com.arcane.registry.ModItems.SEALED_CREATURE.get()) && item.getTag().contains("entity"),
        "o selamento guarda a criatura num item");
    var golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 1, 5));
    var warden = h.spawnWithNoFreeWill(EntityType.WARDEN, new BlockPos(6, 1, 1));
    var caster = pig(h, 0, 0);
    var spell = new Inscription(List.of(g("self"), g("petrify")));
    var ctx = new com.arcane.execution.ExecutionContext(h.getLevel(), caster, spell, com.arcane.inscription.CompiledPlanCache.getOrCompile(spell),
        com.arcane.ambient.AmbientFieldResolver.resolve(h.getLevel(), caster.blockPosition()), com.arcane.execution.PrecisionSnapshot.WORKBENCH,
        com.arcane.execution.ExecutionContext.TriggerEvent.cast());
    h.assertTrue(com.arcane.execution.CreatureLevels.fraction(ctx, warden) == 0.0, "uma criatura que conjura (100 de mana) nao petrifica um Warden (nivel 500)");
    warden.discard();
    h.assertTrue(com.arcane.execution.CreatureLevels.fraction(ctx, pig(h, 7, 2)) == 1.0, "mas petrifica um porco");
    var mage = casterWith(h);
    var strong = casterWith(h);
    var mageMana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(mage);
    var strongMana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(strong);
    mageMana.setBaseMax(100);
    var pctx = new com.arcane.execution.ExecutionContext(h.getLevel(), mage, spell, com.arcane.inscription.CompiledPlanCache.getOrCompile(spell),
        com.arcane.ambient.AmbientFieldResolver.resolve(h.getLevel(), mage.blockPosition()), com.arcane.execution.PrecisionSnapshot.WORKBENCH,
        com.arcane.execution.ExecutionContext.TriggerEvent.cast());
    h.assertTrue(com.arcane.execution.CreatureLevels.fraction(pctx, golem) == 1.0, "um mago com 100 de mana maxima controla um golem (nivel 100)");
    strongMana.setBaseMax(150);
    double weak = com.arcane.execution.CreatureLevels.fraction(pctx, strong);
    h.assertTrue(weak > 0.6 && weak < 0.7, "contra um jogador com 150 de mana maxima, o efeito sai mais fraco (" + weak + ")");
    strongMana.setBaseMax(300);
    h.assertTrue(com.arcane.execution.CreatureLevels.fraction(pctx, strong) == 0.0, "contra 300 de mana maxima, ele resiste");
    golem.discard();
    h.succeed();
  }

  @GameTest(template = "empty", batch = "r23_counter", timeoutTicks = 40)
  public static void counterspellCancelsASpellItSeesAndDawnFiresArmedSpells(GameTestHelper h) {
    floor(h);
    Pig watcher = pig(h, 1, 1), caster = pig(h, 4, 1);
    watcher.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, caster.position());
    var arm = CastingService.tryCast(watcher, new Inscription(List.of(g("self"), g("on_see_cast"), g("counterspell", "strength", 3.0))), h.getLevel());
    h.assertTrue(arm.success() && com.arcane.execution.ArmedSpells.armedCount(watcher.getUUID()) == 1, "a contramagia fica armada");
    float hp = caster.getHealth();
    var hit = CastingService.tryCast(caster, new Inscription(List.of(g("self"), g("damage", "amount", 3.0))), h.getLevel());
    h.assertTrue(!hit.success() && caster.getHealth() == hp, "a magia vista foi anulada (" + hit.message() + ")");
    var again = CastingService.tryCast(caster, new Inscription(List.of(g("self"), g("damage", "amount", 3.0))), h.getLevel());
    h.assertTrue(again.success() && caster.getHealth() < hp, "a contramagia so vale uma vez");

    Pig sleeper = pig(h, 6, 6);
    sleeper.hurt(h.getLevel().damageSources().magic(), 4f);
    float hurt = sleeper.getHealth();
    CastingService.tryCast(sleeper, new Inscription(List.of(g("self"), g("on_dawn"), g("heal", "amount", 4.0))), h.getLevel());
    h.assertTrue(sleeper.getHealth() == hurt, "armada, a cura espera o amanhecer");
    com.arcane.execution.ArmedSpells.dawnNow(h.getLevel());
    h.assertTrue(sleeper.getHealth() > hurt, "ao amanhecer a cura acontece");

    var call = new com.arcane.execution.EffectCall(new ResourceLocation("arcane", "damage"), new ResourceLocation("arcane", "damage"),
        Map.of("amount", 4.0, "random", 1.0), List.of());
    var plain = new Inscription(List.of(g("self"), g("damage")));
    var rctx = new com.arcane.execution.ExecutionContext(h.getLevel(), sleeper, plain, com.arcane.inscription.CompiledPlanCache.getOrCompile(plain),
        com.arcane.ambient.AmbientFieldResolver.resolve(h.getLevel(), sleeper.blockPosition()), com.arcane.execution.PrecisionSnapshot.WORKBENCH,
        com.arcane.execution.ExecutionContext.TriggerEvent.cast());
    java.util.Set<Long> seen = new java.util.HashSet<>();
    for (int i = 0; i < 40; i++) {
      double a = com.arcane.execution.SpellPipeline.randomize(rctx, call).parameters().get("amount");
      h.assertTrue(a >= 2.0 - 1e-6 && a <= 8.0 + 1e-6, "Aleatorio fica entre metade e o dobro (" + a + ")");
      seen.add(Math.round(a * 100));
    }
    h.assertTrue(seen.size() > 10, "e varia a cada conjuracao");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "r23_illusion", timeoutTicks = 40)
  public static void illusionMakesThreeHarmlessClonesThatConfuseMobs(GameTestHelper h) {
    floor(h);
    var player = learn(casterWith(h), "self", "illusion");
    TestPlayers.add(h, player);
    var r = CastingService.tryCast(player, new Inscription(List.of(g("self"), g("illusion"))), com.arcane.execution.PrecisionSnapshot.WORKBENCH);
    var clones = h.getLevel().getEntitiesOfClass(com.arcane.entity.IllusionEntity.class, player.getBoundingBox().inflate(8));
    h.assertTrue(r.success() && clones.size() == 3, "3 clones (" + clones.size() + ", " + r.message() + ")");
    java.util.Set<Object> picks = new java.util.HashSet<>();
    var rnd = net.minecraft.util.RandomSource.create(7);
    for (int i = 0; i < 40; i++) picks.add(com.arcane.entity.IllusionEntity.confuse(player, rnd));
    h.assertTrue(picks.size() >= 3, "as criaturas escolhem entre o dono e os clones (" + picks.size() + " escolhas diferentes)");
    var one = clones.get(0);
    one.hurt(h.getLevel().damageSources().magic(), 1f);
    h.assertTrue(one.isRemoved(), "um golpe desfaz a ilusao");
    clones.forEach(net.minecraft.world.entity.Entity::discard);
    h.succeed();
  }

  @GameTest(template = "empty", batch = "r23_armor", timeoutTicks = 40)
  public static void spellDamageRespectsArmorUnlessPiercedAndOnlyTheGlyphVeils(GameTestHelper h) {
    floor(h);
    // mesma magia em zumbi sem armadura e com armadura (o elemento muda o dano, entao compara-se sempre a mesma magia)
    var bare = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
    var armored = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
    var bareP = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 1, 1));
    var armoredP = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(7, 1, 1));
    for (var z : List.of(armored, armoredP)) {
      z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE));
      z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_LEGGINGS));
    }
    // a armadura de um mob so passa a valer depois de um tick (o jogo confere o equipamento no tick)
    h.runAfterDelay(3, () -> {
      for (var z : List.of(bare, armored)) cast(h, z, g("self"), g("damage", "amount", 2.0));
      for (var z : List.of(bareP, armoredP)) cast(h, z, g("self"), g("damage", "amount", 2.0), g("pierce"), g("pierce"), g("pierce"), g("pierce"), g("pierce"));
      float a = bare.getMaxHealth() - bare.getHealth(), b = armored.getMaxHealth() - armored.getHealth();
      float c = bareP.getMaxHealth() - bareP.getHealth(), d = armoredP.getMaxHealth() - armoredP.getHealth();
      h.assertTrue(b < a * 0.8f, "a armadura reduz o dano comum de magia (sem armadura " + a + ", com armadura " + b + ")");
      h.assertTrue(Math.abs(c - d) < 0.3f, "com 5 camadas de Perfurar a armadura nao reduz nada (" + c + " e " + d + ")");
    });

    Pig glyph = pig(h, 1, 5), potion = pig(h, 3, 5);
    cast(h, glyph, g("self"), g("invisible"));
    potion.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.INVISIBILITY, 200));
    h.assertTrue(glyph.hasEffect(MobEffects.INVISIBILITY) && glyph.hasEffect(com.arcane.registry.ModEffects.ARCANE_VEIL.get()), "o glifo poe o veu arcano");
    h.assertTrue(potion.hasEffect(MobEffects.INVISIBILITY) && !potion.hasEffect(com.arcane.registry.ModEffects.ARCANE_VEIL.get()), "a pocao vanilla nao");
    glyph.removeEffect(MobEffects.INVISIBILITY);
    h.assertTrue(!glyph.hasEffect(com.arcane.registry.ModEffects.ARCANE_VEIL.get()), "tirar a invisibilidade tira o veu");

    var shot = new com.arcane.entity.ArcaneProjectileEntity(com.arcane.registry.ModEntities.ARCANE_PROJECTILE.get(), h.getLevel());
    var start = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 4.5, 7.5));
    shot.setPos(start.x, start.y, start.z);
    shot.setDeltaMovement(0.05, 0, 0);
    h.getLevel().addFreshEntity(shot);
    h.runAfterDelay(15, () -> {
      h.assertTrue(Math.abs(shot.getY() - start.y) < 0.01, "o projetil nao cai (y " + start.y + " -> " + shot.getY() + ")");
      shot.discard();
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "ruins")
  public static void arcaneRuinsBuildThemedRoomsWithLootAndAVault(GameTestHelper h) {
    var level = h.getLevel();
    BlockPos top = h.absolutePos(new BlockPos(2, 0, 2));
    BlockPos centre = top.above(40); // longe da area dos outros testes (aqui o fundo do mundo fica logo abaixo)
    var rooms = com.arcane.world.ArcaneRuins.build(level, centre, net.minecraft.util.RandomSource.create(42));
    h.assertTrue(rooms.size() >= 4 && rooms.size() <= 9, "de 4 a 8 salas (" + rooms.size() + ")");
    h.assertTrue(rooms.get(centre) == com.arcane.world.ArcaneRuins.Room.RITUAL && level.getBlockState(centre).is(com.arcane.registry.ModBlocks.ARCANE_ALTAR.get()),
        "o centro e o salao do ritual, com altar");
    h.assertTrue(rooms.containsValue(com.arcane.world.ArcaneRuins.Room.VAULT), "ha um cofre");
    int chests = 0;
    for (BlockPos q : BlockPos.betweenClosed(centre.offset(-20, -1, -20), centre.offset(20, 6, 20))) {
      if (level.getBlockEntity(q) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity be) {
        var tag = be.saveWithoutMetadata();
        h.assertTrue(tag.getString("LootTable").startsWith("arcane:chests/ruins/"), "bau com loot das ruinas (" + tag.getString("LootTable") + ")");
        chests++;
      }
    }
    h.assertTrue(chests >= 3, "varios baus (" + chests + ")");
    var mages = level.getEntitiesOfClass(com.arcane.entity.NPCMageEntity.class, new net.minecraft.world.phys.AABB(centre).inflate(24));
    h.assertTrue(!mages.isEmpty(), "o cofre tem um mago guardiao");
    mages.forEach(net.minecraft.world.entity.Entity::discard);
    level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, new net.minecraft.world.phys.AABB(centre).inflate(24)).forEach(net.minecraft.world.entity.Entity::discard);
    h.assertTrue(com.arcane.execution.WorldMarks.sites(level, "ruins").contains(centre), "as ruinas ficam registradas para a Bussola Arcana");
    com.arcane.execution.WorldMarks.clearSites(level);
    h.succeed();
  }

  @GameTest(template = "empty", batch = "hydra_heads", timeoutTicks = 60)
  public static void eachHydraHeadPicksItsOwnTarget(GameTestHelper h) {
    floor(h);
    var hydra = h.spawn(com.arcane.registry.ModEntities.HYDRA.get(), new BlockPos(3, 1, 3));
    hydra.setStage(7);
    hydra.setSealMask(0);
    hydra.setNoAi(false);
    Pig a = pig(h, 0, 0), b = pig(h, 6, 6);
    a.setInvulnerable(true); // a Hidra morde de verdade: o alvo tem de sobreviver ate a conferencia
    b.setInvulnerable(true);
    hydra.setTarget(a);
    h.runAfterDelay(25, () -> {
      java.util.Set<Integer> ids = new java.util.HashSet<>();
      for (int i = 0; i < hydra.heads(); i++) ids.add(hydra.headTarget(i));
      // (criaturas de testes vizinhos tambem podem virar alvo; o que importa e o alvo principal e a divisao)
      h.assertTrue(ids.contains(a.getId()) && ids.size() >= 2, "as cabecas se dividem: o alvo principal e mais algum (" + ids + ")");
      h.assertTrue(hydra.headTarget(hydra.heads()) == -1, "cabecas que nao existem ficam sem alvo");
      hydra.discard();
      h.succeed();
    });
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 40)
  public static void manaCompassPrefersGiantRitualsOverCrystals(GameTestHelper h) {
    arena(h);
    var level = h.getLevel();
    BlockPos crystal = altarAbs(h).offset(3, 0, 3);
    level.setBlock(crystal, com.arcane.registry.ModBlocks.MANA_CRYSTAL.get().defaultBlockState(), 3);
    com.arcane.execution.WorldMarks.clearSites(level); // Ruinas de outros testes ficariam na frente do cristal
    var only = com.arcane.item.ManaCompassItem.find(level, altarAbs(h).offset(-5, 0, 0));
    h.assertTrue(only != null && only.kind().equals("crystal"), "sem ritual, aponta o cristal (" + only + ")");
    com.arcane.ritual.RitualService.drawCircle(level, altarAbs(h), 16);
    h.runAfterDelay(30, () -> {
      var t = com.arcane.item.ManaCompassItem.find(level, altarAbs(h).offset(-5, 0, 0));
      h.assertTrue(t != null && t.kind().equals("ritual") && t.pos().equals(altarAbs(h)), "com um ritual gigante, aponta para ele (" + t + ")");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "lair_growth")
  public static void lairGrowsWithTheHydraAndHoldsAHoard(GameTestHelper h) {
    h.assertTrue(com.arcane.world.HydraLairs.chamberRadius(1) == 14 && com.arcane.world.HydraLairs.chamberRadius(14) > 40,
        "a camara vai de 14 a mais de 40 de raio (" + com.arcane.world.HydraLairs.chamberRadius(14) + ")");
    h.assertTrue(com.arcane.entity.HydraEntity.pillarTop(0, 1).equals(com.arcane.entity.HydraEntity.pillarTop(0)), "estagio 1 = pilares de sempre");
    var big = com.arcane.entity.HydraEntity.pillarTop(0, 14);
    h.assertTrue(big.getX() > 30 && big.getY() > 15, "na ancia os pilares se afastam e sobem (" + big + ")");
    var level = h.getLevel();
    BlockPos c = h.absolutePos(new BlockPos(2, 0, 2)).above(60);
    com.arcane.world.HydraLairs.expandChamber(level, c, 1);
    int chests = 0;
    for (BlockPos q : BlockPos.betweenClosed(c.offset(-10, 0, -10), c.offset(10, 0, 10))) {
      if (level.getBlockEntity(q) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity be) {
        h.assertTrue(be.saveWithoutMetadata().getString("LootTable").equals("arcane:chests/hydra/low"), "tesouro da faixa baixa");
        chests++;
      }
    }
    h.assertTrue(chests == 3, "estagio 1: 3 baus (" + chests + ")");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "lair_dungeon", timeoutTicks = 200)
  public static void lairDungeonGeneratesRoomsCorridorsAndTraps(GameTestHelper h) {
    var level = h.getLevel();
    BlockPos chamber = h.absolutePos(new BlockPos(2, 0, 2)).offset(4000, 40, 4000); // longe dos outros testes
    BlockPos entrance = chamber.offset(38, 0, 0);
    var rooms = com.arcane.world.LairDungeon.build(level, chamber, entrance);
    var again = com.arcane.world.LairDungeon.build(level, chamber, entrance);
    h.assertTrue(rooms.size() >= 5 && rooms.size() <= 10, "de 5 a 10 salas (" + rooms.size() + ")");
    h.assertTrue(rooms.keySet().equals(again.keySet()), "o mesmo covil gera sempre a mesma masmorra");
    for (BlockPos r : rooms.keySet()) {
      h.assertTrue(Math.hypot(r.getX() - chamber.getX(), r.getZ() - chamber.getZ()) >= 52, "as salas ficam longe da camara (que cresce)");
      // escavada = a maior parte do miolo e ar ou liquido (o centro pode ter altar, cristais, ossos ou agua)
      int open = 0, total = 0;
      for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 1; dy <= 3; dy++) {
        var st = level.getBlockState(r.offset(dx, dy, dz));
        total++;
        if (st.isAir() || !st.getFluidState().isEmpty()) open++;
      }
      h.assertTrue(open >= total * 0.6, "a sala " + rooms.get(r) + " foi escavada (" + open + " de " + total + " abertos)");
    }
    h.succeed();
  }
}
