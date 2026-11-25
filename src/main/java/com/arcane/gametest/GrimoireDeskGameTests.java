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

/** Grimorio na bancada: posse, escrita e estudo. (dividido de ArcaneGameTests em 25 nov; os testes nao mudaram.) */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class GrimoireDeskGameTests extends GameTestSupport {

  @GameTest(template = "empty", batch = "grimoire_desk")
  public static void writingLeavesTheBookOnTheDeskUntilTheOwnerComesBack(GameTestHelper h) {
    floor(h);
    var player = learn(casterWith(h), "self", "heal");
    TestPlayers.add(h, player);
    BlockPos desk = h.absolutePos(new BlockPos(1, 1, 1));
    com.arcane.grimoire.GrimoireDesk.startWriting(player, desk, 3);
    var st = com.arcane.grimoire.GrimoireAttachments.get(player).orElseThrow();
    h.assertTrue(st.writingUntil() == h.getLevel().getGameTime() + 60, "3 glifos = 3 s de escrita");
    h.assertTrue("message.arcane.grimoire.writing".equals(com.arcane.grimoire.GrimoireDesk.blockedReason(player)), "escrevendo nao conjura");
    st.setWritingUntil(h.getLevel().getGameTime() - 5);
    var far = h.absoluteVec(new net.minecraft.world.phys.Vec3(10.5, 1, 10.5));
    player.moveTo(far.x, far.y, far.z);
    com.arcane.grimoire.GrimoireDesk.tickPlayer(player);
    h.assertTrue("message.arcane.grimoire.on_desk".equals(com.arcane.grimoire.GrimoireDesk.blockedReason(player)), "longe, o livro fica na mesa");
    var near = h.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1, 1.5));
    player.moveTo(near.x, near.y, near.z);
    com.arcane.grimoire.GrimoireDesk.tickPlayer(player);
    h.assertTrue(com.arcane.grimoire.GrimoireDesk.blockedReason(player) == null && st.deskPos().isEmpty(), "a ate 4 blocos o dono pega o livro");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "grimoire_desk")
  public static void anotherMageTakesTheBookFromTheDeskAndCastsItAtSixtyPercent(GameTestHelper h) {
    floor(h);
    var owner = learn(casterWith(h), "self", "heal");
    var thief = casterWith(h);
    TestPlayers.add(h, owner);
    TestPlayers.add(h, thief);
    var spell = addSpell(owner, com.arcane.grimoire.SpellPrefs.CastMode.TAP, g("self"), g("heal", "amount", 10.0));
    owner.setHealth(2f);
    var r = com.arcane.execution.CastingService.tryCast(owner, spell, com.arcane.execution.PrecisionSnapshot.WORKBENCH);
    float ownerHeal = owner.getHealth() - 2f;
    h.assertTrue(r.success() && ownerHeal > 0, "o dono cura (" + ownerHeal + ")");
    BlockPos desk = h.absolutePos(new BlockPos(1, 1, 1));
    com.arcane.grimoire.GrimoireDesk.startWriting(owner, desk, 2);
    com.arcane.grimoire.GrimoireDesk.steal(thief, owner);
    var ownerState = com.arcane.grimoire.GrimoireAttachments.get(owner).orElseThrow();
    h.assertTrue(!ownerState.grimoirePresent() && ownerState.getInscription(spell.id()).isEmpty(), "o dono ficou sem o grimorio");
    var book = thief.getInventory().items.stream().filter(s -> com.arcane.item.GrimoireItem.contents(s) != null).findFirst().orElse(null);
    h.assertTrue(book != null && com.arcane.item.GrimoireItem.isOwnedBy(book, owner.getUUID()), "o ladrao carrega o livro, mas o dono continua sendo o outro");
    thief.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, book);
    thief.setHealth(2f);
    com.arcane.grimoire.GrimoireDesk.useForeign(thief, book, false);
    float thiefHeal = thief.getHealth() - 2f;
    h.assertTrue(thiefHeal > 0 && Math.abs(thiefHeal / ownerHeal - 0.6f) < 0.12f, "o portador conjura sem conhecer os glifos, com 60% (" + thiefHeal + " de " + ownerHeal + ")");
    var desc = com.arcane.grimoire.GrimoireDesk.describe(thief, spell).getString();
    h.assertTrue(desc.contains("???"), "glifos desconhecidos aparecem como ??? (" + desc + ")");
    com.arcane.grimoire.GrimoireDesk.restoreOwner(owner, book);
    h.assertTrue(ownerState.grimoirePresent() && ownerState.getInscription(spell.id()).isPresent(), "o dono recupera tudo com o livro");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "grimoire_desk")
  public static void studyIdentifiesOneUnknownGlyphPerDayAndKeepsProgressInTheBook(GameTestHelper h) {
    floor(h);
    var owner = learn(casterWith(h), "self", "heal", "fire");
    var thief = casterWith(h);
    TestPlayers.add(h, owner);
    TestPlayers.add(h, thief);
    addSpell(owner, com.arcane.grimoire.SpellPrefs.CastMode.TAP, g("self"), g("heal"));
    addSpell(owner, com.arcane.grimoire.SpellPrefs.CastMode.TAP, g("self"), g("fire"));
    var tag = new net.minecraft.nbt.CompoundTag();
    com.arcane.grimoire.GrimoireAttachments.get(owner).orElseThrow().save(tag);
    var book = com.arcane.item.GrimoireItem.createFor(owner, tag);
    thief.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, book);
    var unknown = com.arcane.grimoire.GrimoireDesk.unknownGlyphs(thief, book);
    h.assertTrue(unknown.size() >= 2, "glifos desconhecidos (" + unknown + ")");
    BlockPos desk = h.absolutePos(new BlockPos(1, 1, 1));
    com.arcane.grimoire.GrimoireDesk.startStudy(thief, h.getLevel(), desk, book);
    h.assertTrue(thief.getMainHandItem().isEmpty() && com.arcane.grimoire.GrimoireDesk.studyAt(h.getLevel(), desk), "o livro foi pousado na mesa");
    com.arcane.grimoire.GrimoireDesk.advanceStudy(h.getLevel(), desk, com.arcane.grimoire.GrimoireDesk.STUDY_TICKS - 1);
    com.arcane.grimoire.GrimoireDesk.tickStudies(h.getLevel().getServer());
    var mine = com.arcane.grimoire.GrimoireAttachments.get(thief).orElseThrow();
    h.assertTrue(mine.knowsGlyph(unknown.get(0), com.arcane.grimoire.GlyphKnowledge.IDENTIFIED), "um dia de estudo identifica o primeiro glifo");
    h.assertTrue(!mine.knowsGlyph(unknown.get(1), com.arcane.grimoire.GlyphKnowledge.IDENTIFIED), "so um por dia");
    com.arcane.grimoire.GrimoireDesk.advanceStudy(h.getLevel(), desk, 5000);
    h.assertTrue(com.arcane.grimoire.GrimoireDesk.onWorkbenchUse(thief, h.getLevel(), desk), "botao direito tira o livro da mesa");
    var back = thief.getInventory().items.stream().filter(s -> com.arcane.item.GrimoireItem.contents(s) != null).findFirst().orElseThrow();
    h.assertTrue(com.arcane.grimoire.GrimoireDesk.savedProgress(back, thief.getUUID()) >= 5000, "o progresso do segundo glifo ficou salvo no livro");
    h.assertTrue(!com.arcane.grimoire.GrimoireDesk.studyAt(h.getLevel(), desk), "fora da mesa o estudo pausa");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "rev_toggle", timeoutTicks = 60)
  public static void toggleSpellSurvivesCastingAnotherSpellAndOnlyTurnsOffOnRequest(GameTestHelper h) {
    floor(h);
    var player = casterWith(h);
    var flight = addSpell(player, com.arcane.grimoire.SpellPrefs.CastMode.TOGGLE, g("self"), g("float"));
    var other = addSpell(player, com.arcane.grimoire.SpellPrefs.CastMode.TAP, g("aura"), g("damage"));
    com.arcane.execution.CastChannels.press(player, flight.id());
    h.assertTrue(player.hasEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get()) && com.arcane.execution.CastChannels.hasToggle(player.getUUID()), "o voo ligou");
    com.arcane.execution.CastChannels.press(player, other.id());
    h.assertTrue(player.hasEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get()), "conjurar outra magia NAO desliga o voo");
    h.assertTrue(com.arcane.execution.CastChannels.hasToggle(player.getUUID()), "o toggle continua registrado");
    com.arcane.execution.CastChannels.press(player, flight.id());
    h.assertTrue(!player.hasEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get()) && !com.arcane.execution.CastChannels.hasToggle(player.getUUID()), "apertar de novo desliga");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "rev_dial", timeoutTicks = 60)
  public static void areaDialTradesAreaForDamageInverselyAndChargeGrowsArea(GameTestHelper h) {
    floor(h);
    var player = casterWith(h);
    player.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1, 0.5)));
    var near = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 2));
    var far = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 6));
    var spell = new Inscription(List.of(g("aura", "radius", 2.5), g("damage", "amount", 3.0)));
    var precision = com.arcane.execution.PrecisionSnapshot.of(spell);
    float n0 = near.getHealth(), f0 = far.getHealth();
    h.assertTrue(com.arcane.execution.CastingService.tryCast(player, spell, precision).success(), "lancou sem regulador");
    float base = n0 - near.getHealth();
    h.assertTrue(base > 0 && far.getHealth() == f0, "sem regulador so o perto leva dano (dano " + base + ")");
    // regulador no maximo: area bem maior, dano na mesma proporcao menor
    for (int i = 0; i < 8; i++) com.arcane.execution.AreaDial.add(player.getUUID(), 1);
    double factor = com.arcane.execution.AreaDial.factorOf(com.arcane.execution.AreaDial.step(player.getUUID()));
    float n1 = near.getHealth();
    near.invulnerableTime = 0; // o segundo golpe vem no mesmo tick: tira a invulnerabilidade do primeiro
    far.invulnerableTime = 0;
    h.assertTrue(com.arcane.execution.CastingService.tryCast(player, spell, precision).success(), "lancou com o regulador");
    h.assertTrue(far.getHealth() < f0, "com mais area o longe tambem leva dano");
    float reduced = n1 - near.getHealth();
    h.assertTrue(reduced > 0 && Math.abs(reduced - base / factor) < 0.35f, "dano proporcional inverso: " + reduced + " ~ " + (base / factor));
    com.arcane.execution.AreaDial.reset(player.getUUID());
    // carregar: potencia x6 tambem cresce a area (aura de 2,5 passa a alcancar 6 blocos)
    var far2 = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 6));
    float g0 = far2.getHealth();
    h.assertTrue(com.arcane.execution.CastingService.cast(player, spell, precision, 6.0, true).success(), "carga lancou");
    h.assertTrue(far2.getHealth() < g0, "a carga amplia a area, nao so o dano");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "rev_cost")
  public static void costModelChargesForDamageOverTimeAreaAndSummonSize(GameTestHelper h) {
    var reg = com.arcane.glyph.GlyphRegistry.get(new ResourceLocation("arcane", "summon_golem")).orElseThrow();
    h.assertTrue(!reg.parameters().containsKey("count"), "golem nao tem mais quantidade (so tamanho e duracao)");
    java.util.function.Function<GlyphInstance[], Integer> cost = gl -> com.arcane.inscription.CompiledPlanCache.getOrCompile(new Inscription(List.of(gl))).baseManaCost();
    int golem1 = cost.apply(new GlyphInstance[] {g("summon_golem")});
    int golem3 = cost.apply(new GlyphInstance[] {g("summon_golem", "size", 3.0)});
    h.assertTrue(golem3 >= golem1 * 3, "golem tamanho 3 custa bem mais (" + golem3 + " contra " + golem1 + ")");
    int wolves1 = cost.apply(new GlyphInstance[] {g("summon_wolves", "count", 1.0)});
    int wolves6 = cost.apply(new GlyphInstance[] {g("summon_wolves", "count", 6.0)});
    h.assertTrue(wolves6 >= wolves1 * 4, "seis lobos custam bem mais que um (" + wolves6 + " contra " + wolves1 + ")");
    int shortDome = cost.apply(new GlyphInstance[] {g("barrier"), g("domain", "seconds", 5.0, "interval", 1.0), g("cut")});
    int longDome = cost.apply(new GlyphInstance[] {g("barrier"), g("domain", "seconds", 60.0, "interval", 0.5), g("cut")});
    h.assertTrue(longDome >= shortDome * 8, "dominio longo e rapido custa muito mais (" + longDome + " contra " + shortDome + ")");
    int plain = cost.apply(new GlyphInstance[] {g("aura"), g("damage")});
    int wide = cost.apply(new GlyphInstance[] {g("aura"), g("damage"), g("area")});
    h.assertTrue(wide >= plain * 2, "area maior custa mais (" + wide + " contra " + plain + ")");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "rev_ritual_form")
  public static void ritualNeedsNoFormAndFallsBackToTheAltarAsAura(GameTestHelper h) {
    var spell = new Inscription(List.of(g("damage", "amount", 2.0)));
    h.assertTrue(!com.arcane.inscription.InscriptionValidator.validate(spell).valid(), "fora de ritual, sem forma e invalido");
    h.assertTrue(com.arcane.inscription.InscriptionValidator.validate(spell, java.util.Optional.empty(), true).valid(), "no ritual a forma e dispensada");
    var withForm = com.arcane.execution.CastingService.withRitualForm(spell);
    h.assertTrue(withForm.glyphs().get(0).glyphId().getPath().equals("aura"), "o altar vira uma Aura");
    var kept = new Inscription(List.of(g("summon_wolves"), g("damage")));
    h.assertTrue(com.arcane.execution.CastingService.withRitualForm(kept) == kept, "com forma nada muda");
    h.succeed();
  }

  @GameTest(template = "ritual_arena", batch = "rev_altar_ui", timeoutTicks = 100)
  public static void altarScreenPacketPreviewsAndPerformsADrawnRitualWithoutTheActiveSpell(GameTestHelper h) {
    arena(h);
    var player = learn(casterWith(h), "summon_wolves");
    player.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(ALTAR.getX() + 2.5, 1, ALTAR.getZ() + 0.5)));
    TestPlayers.add(h, player);
    com.arcane.ritual.RitualService.drawPerfectCircle(h.getLevel(), altarAbs(h), 4);
    var entry = new com.arcane.network.packet.C2SSaveInscriptionPacket.Entry("arcane:summon_wolves", Map.of("count", 2.0));
    // sem ossos: a oferenda falta e o ritual nao acontece
    com.arcane.network.packet.C2SRitualPacket.apply(player, new com.arcane.network.packet.C2SRitualPacket(altarAbs(h), com.arcane.network.packet.C2SRitualPacket.PERFORM, List.of(), List.of(entry)));
    h.assertTrue(h.getLevel().getEntitiesOfClass(com.arcane.entity.SpectralWolfEntity.class, new net.minecraft.world.phys.AABB(altarAbs(h)).inflate(12)).isEmpty(), "sem a oferenda nao invoca");
    // com 6 ossos: invoca; os ossos saem do inventario
    player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE, 6));
    var preview = com.arcane.ritual.RitualService.preview(h.getLevel(), altarAbs(h), player, new Inscription(List.of(new GlyphInstance(new ResourceLocation("arcane", "summon_wolves"), Map.of("count", 2.0)))), com.arcane.ritual.RitualService.Offerings.NONE);
    h.assertTrue(preview.valid() && preview.needs().size() == 1 && preview.needs().get(0).need() == 6 && preview.needs().get(0).have() >= 6, "a previa lista a oferenda (6 ossos)");
    com.arcane.network.packet.C2SRitualPacket.apply(player, new com.arcane.network.packet.C2SRitualPacket(altarAbs(h), com.arcane.network.packet.C2SRitualPacket.PERFORM, List.of(), List.of(entry)));
    var wolves = h.getLevel().getEntitiesOfClass(com.arcane.entity.SpectralWolfEntity.class, new net.minecraft.world.phys.AABB(altarAbs(h)).inflate(12));
    h.assertTrue(wolves.size() == 2, "o ritual desenhado na tela invocou 2 lobos (" + wolves.size() + ")");
    h.succeed();
  }

  @GameTest(template = "ritual_arena", timeoutTicks = 100)
  public static void areaModifierEnlargesTheDome(GameTestHelper h) {
    for (int x = 0; x < 15; x++) for (int z = 0; z < 15; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    Pig c1 = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(7, 1, 7));
    c1.setYRot(0);
    c1.setXRot(0);
    h.assertTrue(CastingService.tryCast(c1, new Inscription(List.of(g("barrier"), g("shape_dome"))), h.getLevel()).success(), "domo simples");
    int small = countBarrier(h, 7);
    for (int x = 0; x < 15; x++) for (int y = 0; y < 10; y++) for (int z = 0; z < 15; z++) {
      var p = new BlockPos(x, 1 + y, z);
      if (h.getBlockState(p).is(com.arcane.registry.ModBlocks.ARCANE_BARRIER.get())) h.setBlock(p, Blocks.AIR);
    }
    h.assertTrue(CastingService.tryCast(c1, new Inscription(List.of(g("barrier"), g("shape_dome"), g("area"))), h.getLevel()).success(), "domo com area");
    int big = countBarrier(h, 7);
    h.assertTrue(big > small, "o Area deveria ampliar o domo: " + small + " -> " + big);
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void auraWithDomeHitsCreaturesInside(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    var above = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 2, 4));
    float before = above.getHealth();
    cast(h, caster, g("aura"), g("shape_dome"), g("damage"));
    assertHurt(h, above, before, "criatura dentro do domo da aura");
    h.succeed();
  }
}
