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

/** Fases 0 a 4 dos glifos: persistencia, zonas, convidados, modos de conjuracao, voo, glifos de custo baixo e medio. (dividido de ArcaneGameTests em 25 nov; os testes nao mudaram.) */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class GlyphPhaseGameTests extends GameTestSupport {

  @GameTest(template = "empty")
  public static void scarsSurviveASaveAndReload(GameTestHelper h) {
    floor(h);
    com.arcane.execution.WorldScars.clearAll();
    Pig caster = pig(h, 3, 1);
    h.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(3, 1, 4));
    cast(h, caster, g("ray"), g("damage"));
    var store = com.arcane.execution.WorldScars.store(h.getLevel());
    var before = com.arcane.execution.WorldScars.all(h.getLevel());
    h.assertTrue(!before.isEmpty(), "ha cicatriz para salvar");
    var reloaded = com.arcane.execution.WorldScars.ScarStore.load(store.save(new net.minecraft.nbt.CompoundTag()));
    h.assertTrue(reloaded.size() == before.size(), "a cicatriz volta do NBT (" + reloaded.size() + " de " + before.size() + ")");
    com.arcane.execution.WorldScars.clearAll();
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80, batch = "zone_restore")
  public static void zonesAreSavedAndComeBackAfterARestart(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    var victim = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 4));
    com.arcane.execution.Zones.clearAll();
    cast(h, caster, g("aura"), g("zone", "seconds", 6.0), g("damage", "amount", 1.0));
    h.assertTrue(com.arcane.execution.Zones.savedCount() == 1, "a zona foi salva: " + com.arcane.execution.Zones.savedCount());
    com.arcane.execution.Zones.reloadFromStore(h.getLevel().getServer());
    h.assertTrue(com.arcane.execution.Zones.activeFor(caster.getUUID()) == 1, "a zona voltou depois do 'restart'");
    float afterReload = victim.getHealth();
    h.runAfterDelay(30, () -> {
      h.assertTrue(victim.getHealth() < afterReload, "a zona restaurada continua ferindo (vida " + victim.getHealth() + ")");
      com.arcane.execution.Zones.clearAll();
      h.assertTrue(com.arcane.execution.Zones.savedCount() == 0, "limpar apaga o salvo");
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void onBarrierBreakFiresOnceWhereTheBarrierWasBroken(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 1, 1);
    cast(h, caster, g("barrier", "duration", 400.0), g("on_barrier_break"), g("damage", "amount", 4.0));
    BlockPos found = null;
    var barrier = com.arcane.registry.ModBlocks.ARCANE_BARRIER.get();
    for (BlockPos p : BlockPos.betweenClosed(h.absolutePos(new BlockPos(-6, 0, -6)), h.absolutePos(new BlockPos(8, 8, 8)))) {
      if (h.getLevel().getBlockState(p).is(barrier)) { found = p.immutable(); break; }
    }
    h.assertTrue(found != null, "a barreira foi criada");
    BlockPos at = found;
    var thief = h.spawnWithNoFreeWill(EntityType.PIG, h.relativePos(at.above(0)));
    thief.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 1.8);
    float start = thief.getHealth(), casterStart = caster.getHealth();
    h.assertTrue(com.arcane.execution.BarrierBreaks.watched() > 0, "os blocos da barreira estao vigiados");
    h.runAfterDelay(2, () -> {
      com.arcane.execution.BarrierBreaks.broken(h.getLevel(), at);
      h.getLevel().removeBlock(at, false);
      com.arcane.execution.BarrierBreaks.broken(h.getLevel(), at); // de novo: nao repete
    });
    h.runAfterDelay(6, () -> {
      h.assertTrue(thief.getHealth() == start - 4.0f, "quem estava perto da quebra levou o dano uma vez (vida " + thief.getHealth() + " de " + start + ")");
      h.assertTrue(caster.getHealth() == casterStart || caster.distanceTo(thief) < 4, "o dono nao e atingido se estiver longe");
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void zoneEnterAndExitConditionsFireOnlyOnTransitions(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    var walker = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(0, 1, 0));
    com.arcane.execution.Zones.clearFor(caster.getUUID());
    cast(h, caster, g("aura", "radius", 1.5), g("zone", "seconds", 8.0), g("on_zone_enter"), g("damage", "amount", 2.0), g("on_zone_exit"), g("damage", "amount", 3.0));
    float start = walker.getHealth();
    h.runAfterDelay(5, () -> { var p = h.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1, 4.0)); walker.moveTo(p.x, p.y, p.z); });
    h.runAfterDelay(28, () -> {
      h.assertTrue(walker.getHealth() == start - 2.0f, "entrar fere uma vez com 2 (vida " + walker.getHealth() + " de " + start + ")");
      var p = h.absoluteVec(new net.minecraft.world.phys.Vec3(0.5, 1, 0.5)); walker.moveTo(p.x, p.y, p.z);
    });
    h.runAfterDelay(60, () -> {
      h.assertTrue(walker.getHealth() == start - 5.0f, "sair fere uma vez com 3 e ficar dentro nao repete (vida " + walker.getHealth() + ")");
      com.arcane.execution.Zones.clearFor(caster.getUUID());
      h.succeed();
    });
  }

  @GameTest(template = "empty")
  public static void outsidersFilterSparesTheOwnerGuestsAndTheirPets(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    var guest = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 4));
    var stranger = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 3));
    var lists = com.arcane.execution.GuestLists.get(h.getLevel().getServer());
    lists.remove(caster.getUUID(), guest.getUUID());
    lists.add(caster.getUUID(), guest.getUUID());
    float gh = guest.getHealth(), sh = stranger.getHealth(), ch = caster.getHealth();
    cast(h, caster, g("aura"), g("outsiders"), g("damage", "amount", 3.0));
    h.assertTrue(guest.getHealth() == gh, "o convidado nao e ferido");
    h.assertTrue(caster.getHealth() == ch, "o dono nao e ferido");
    h.assertTrue(stranger.getHealth() < sh, "o forasteiro e ferido");
    lists.remove(caster.getUUID(), guest.getUUID());
    var reloaded = com.arcane.execution.GuestLists.load(lists.save(new net.minecraft.nbt.CompoundTag()));
    h.assertTrue(reloaded.of(caster.getUUID()).isEmpty(), "lista vazia apos remover, salva e relida");
    lists.add(caster.getUUID(), guest.getUUID());
    reloaded = com.arcane.execution.GuestLists.load(lists.save(new net.minecraft.nbt.CompoundTag()));
    h.assertTrue(reloaded.of(caster.getUUID()).contains(guest.getUUID()), "o convidado persiste no NBT");
    lists.remove(caster.getUUID(), guest.getUUID());
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void holdModeRepeatsWhileHeldAndStopsOnRelease(GameTestHelper h) {
    floor(h);
    var player = casterWith(h);
    var victim = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 4));
    var spell = addSpell(player, com.arcane.grimoire.SpellPrefs.CastMode.HOLD, g("aura", "radius", 2.5), g("damage", "amount", 0.5));
    float start = victim.getHealth();
    com.arcane.execution.CastChannels.press(player, spell.id());
    h.assertTrue(victim.getHealth() < start, "segurar conjura na hora");
    h.assertTrue(com.arcane.execution.CastChannels.isActive(player.getUUID()), "o canal ficou ativo");
    h.runAfterDelay(32, () -> {
      float held = victim.getHealth();
      h.assertTrue(held <= start - 1.0f, "repetiu enquanto segurava (vida " + held + " de " + start + ")");
      com.arcane.execution.CastChannels.release(player);
      h.assertTrue(!com.arcane.execution.CastChannels.isActive(player.getUUID()), "soltar encerra o canal");
      h.runAfterDelay(25, () -> {
        h.assertTrue(victim.getHealth() >= held, "nao repete depois de soltar");
        h.succeed();
      });
    });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void chargeModeCastsStrongerTheLongerItCharges(GameTestHelper h) {
    floor(h);
    var player = casterWith(h);
    var tapSpell = addSpell(player, com.arcane.grimoire.SpellPrefs.CastMode.TAP, g("aura", "radius", 1.5), g("damage", "amount", 2.0));
    var chargeSpell = addSpell(player, com.arcane.grimoire.SpellPrefs.CastMode.CHARGE, g("aura", "radius", 1.5), g("damage", "amount", 2.0));
    var first = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 4));
    float a0 = first.getHealth();
    com.arcane.execution.CastChannels.press(player, tapSpell.id());
    float tapDamage = a0 - first.getHealth();
    h.assertTrue(tapDamage > 0, "o toque fere");
    first.discard();
    var second = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 4));
    float b0 = second.getHealth();
    float mana0 = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(player).current();
    com.arcane.execution.CastChannels.press(player, chargeSpell.id());
    h.assertTrue(second.getHealth() == b0, "carregar nao lanca ao apertar");
    h.runAfterDelay(30, () -> {
      h.assertTrue(com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(player).current() < mana0, "carregar drena mana aos poucos");
      com.arcane.execution.CastChannels.release(player);
      float chargedDamage = b0 - second.getHealth();
      h.assertTrue(chargedDamage > tapDamage * 1.4f, "carga de 0,5 s rende mais que o toque (" + chargedDamage + " contra " + tapDamage + ")");
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void toggleModeStartsAndStopsTheFlight(GameTestHelper h) {
    floor(h);
    var player = casterWith(h);
    var spell = addSpell(player, com.arcane.grimoire.SpellPrefs.CastMode.TOGGLE, g("self"), g("float"));
    com.arcane.execution.CastChannels.press(player, spell.id());
    h.assertTrue(player.hasEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get()), "o primeiro toque liga o voo");
    com.arcane.execution.CastChannels.press(player, spell.id());
    h.assertTrue(!player.hasEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get()), "o segundo toque desliga");
    var plain = addSpell(player, com.arcane.grimoire.SpellPrefs.CastMode.TAP, g("aura"), g("damage"));
    com.arcane.network.packet.C2SGrimoireOrgPacket.apply(player, new com.arcane.network.packet.C2SGrimoireOrgPacket(com.arcane.network.packet.C2SGrimoireOrgPacket.MODE, plain.id(), com.arcane.grimoire.SpellPrefs.CastMode.TOGGLE.ordinal(), ""));
    var prefs = com.arcane.grimoire.GrimoireAttachments.get(player).orElseThrow().prefs();
    h.assertTrue(prefs.mode(plain.id()) == com.arcane.grimoire.SpellPrefs.CastMode.TAP, "Alternar nao vale para magia comum");
    com.arcane.network.packet.C2SGrimoireOrgPacket.apply(player, new com.arcane.network.packet.C2SGrimoireOrgPacket(com.arcane.network.packet.C2SGrimoireOrgPacket.MODE, spell.id(), com.arcane.grimoire.SpellPrefs.CastMode.CHARGE.ordinal(), ""));
    h.assertTrue(prefs.mode(spell.id()) == com.arcane.grimoire.SpellPrefs.CastMode.CHARGE, "o modo muda no servidor");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void loadoutsFavoritesAndFoldersAreValidatedAndSynced(GameTestHelper h) {
    var player = casterWith(h);
    var spell = addSpell(player, com.arcane.grimoire.SpellPrefs.CastMode.TAP, g("self"), g("damage"));
    var prefs = com.arcane.grimoire.GrimoireAttachments.get(player).orElseThrow().prefs();
    var none = new java.util.UUID(0, 0);
    com.arcane.network.packet.C2SGrimoireOrgPacket.apply(player, new com.arcane.network.packet.C2SGrimoireOrgPacket(com.arcane.network.packet.C2SGrimoireOrgPacket.FAVORITE, spell.id(), 1, ""));
    com.arcane.network.packet.C2SGrimoireOrgPacket.apply(player, new com.arcane.network.packet.C2SGrimoireOrgPacket(com.arcane.network.packet.C2SGrimoireOrgPacket.FOLDER, spell.id(), 0, "  uma pasta com nome enorme demais  "));
    com.arcane.network.packet.C2SGrimoireOrgPacket.apply(player, new com.arcane.network.packet.C2SGrimoireOrgPacket(com.arcane.network.packet.C2SGrimoireOrgPacket.LOADOUT_TOGGLE, spell.id(), 2, ""));
    com.arcane.network.packet.C2SGrimoireOrgPacket.apply(player, new com.arcane.network.packet.C2SGrimoireOrgPacket(com.arcane.network.packet.C2SGrimoireOrgPacket.LOADOUT_SWITCH, none, 2, ""));
    h.assertTrue(prefs.get(spell.id()).favorite, "favorita");
    h.assertTrue(prefs.get(spell.id()).folder.length() <= com.arcane.grimoire.SpellPrefs.MAX_FOLDER, "pasta limitada: " + prefs.get(spell.id()).folder);
    h.assertTrue(prefs.loadout(2).contains(spell.id()) && prefs.activeLoadout() == 2, "conjunto 3 ativo com a magia");
    com.arcane.network.packet.C2SGrimoireOrgPacket.apply(player, new com.arcane.network.packet.C2SGrimoireOrgPacket(com.arcane.network.packet.C2SGrimoireOrgPacket.FAVORITE, java.util.UUID.randomUUID(), 1, ""));
    com.arcane.network.packet.C2SGrimoireOrgPacket.apply(player, new com.arcane.network.packet.C2SGrimoireOrgPacket(com.arcane.network.packet.C2SGrimoireOrgPacket.LOADOUT_SWITCH, none, 99, ""));
    h.assertTrue(prefs.activeLoadout() >= 0 && prefs.activeLoadout() < com.arcane.grimoire.SpellPrefs.LOADOUTS, "indice normalizado");
    com.arcane.grimoire.GrimoireAttachments.get(player).orElseThrow().removeInscription(spell.id());
    h.assertTrue(!prefs.loadout(2).contains(spell.id()), "magia apagada sai dos conjuntos");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 160)
  public static void flightIsChanneledPaysManaEveryTickAndLandsSafely(GameTestHelper h) {
    floor(h);
    var player = casterWith(h);
    TestPlayers.add(h, player);
    h.onEachTick(player::doTick); // o servidor ticka o jogador pela conexao (doTick): efeitos e custo por tick
    var spell = addSpell(player, com.arcane.grimoire.SpellPrefs.CastMode.TOGGLE, g("self"), g("float", "speed", 1.0, "duration", 40.0));
    com.arcane.execution.CastChannels.press(player, spell.id());
    var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(player);
    h.assertTrue(player.hasEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get()), "o voo ligou");
    float mana0 = mana.current();
    h.runAfterDelay(30, () -> {
      h.assertTrue(player.getAbilities().mayfly, "a permissao de voo existe durante o voo (tick " + player.tickCount + ", efeito " + player.hasEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get()) + ", removido " + player.isRemoved() + ", mana " + mana.current() + ")");
      h.assertTrue(mana0 - mana.current() >= 5.0f, "custa mana a cada tick (gastou " + (mana0 - mana.current()) + ")");
    });
    h.runAfterDelay(70, () -> {
      h.assertTrue(player.hasEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get()), "canalizando, o voo passa da duracao de 40 ticks");
      mana.set(0.05f);
    });
    h.runAfterDelay(80, () -> {
      h.assertTrue(!player.hasEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get()), "sem mana o voo acaba");
      h.assertTrue(!player.getAbilities().mayfly, "a permissao de voo foi devolvida");
      float life = player.getHealth();
      player.causeFallDamage(8.0f, 1.0f, h.getLevel().damageSources().fall());
      h.assertTrue(player.getHealth() == life, "a primeira queda depois do voo nao machuca (vida " + player.getHealth() + ")");
      player.causeFallDamage(8.0f, 1.0f, h.getLevel().damageSources().fall());
      h.assertTrue(player.getHealth() < life, "a protecao vale uma vez so");
      player.discard();
      h.succeed();
    });
  }

  @GameTest(template = "empty")
  public static void flightSpeedAndGlideTravelInTheAmplifier(GameTestHelper h) {
    Pig pig = pig(h, 3, 3);
    cast(h, pig, g("self"), g("float", "speed", 1.5, "glide", 1.0));
    var effect = pig.getEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get());
    h.assertTrue(effect != null, "efeito aplicado");
    h.assertTrue(Math.abs(com.arcane.execution.FlightService.speedOf(effect.getAmplifier()) - 1.5) < 0.06, "velocidade 1,5");
    h.assertTrue(com.arcane.execution.FlightService.glideOf(effect.getAmplifier()) > 0.99, "planagem total");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void rainSoaksPutsOutFireAndWatersFarmland(GameTestHelper h) {
    floor(h);
    h.setBlock(new BlockPos(5, 0, 3), Blocks.FARMLAND);
    Pig caster = pig(h, 3, 3);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 4));
    cow.setSecondsOnFire(10);
    cast(h, caster, g("self"), g("rain", "radius", 4.0));
    h.assertTrue(com.arcane.execution.Wetness.isSoaked(caster) && com.arcane.execution.Wetness.isSoaked(cow), "a chuva molha quem esta na area");
    h.assertTrue(!cow.isOnFire(), "a chuva apaga o fogo");
    int moisture = h.getLevel().getBlockState(h.absolutePos(new BlockPos(5, 0, 3))).getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE);
    h.assertTrue(moisture == 7, "a chuva rega a plantacao (umidade " + moisture + ")");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void wetStatusFeedsTheJsonInteractionRules(GameTestHelper h) {
    floor(h);
    Pig wet = pig(h, 2, 3);
    Pig dry = pig(h, 4, 3);
    cast(h, wet, g("self"), g("wet"));
    h.assertTrue(com.arcane.execution.Wetness.isSoaked(wet) && !com.arcane.execution.Wetness.isSoaked(dry), "so um esta molhado");
    var level = h.getLevel();
    double stormWet = com.arcane.execution.ElementInteractions.damageMultiplier(level, com.arcane.execution.SpellElement.STORM, wet);
    double stormDry = com.arcane.execution.ElementInteractions.damageMultiplier(level, com.arcane.execution.SpellElement.STORM, dry);
    h.assertTrue(stormWet > stormDry + 0.2, "molhado: raio fere mais (" + stormWet + " contra " + stormDry + ")");
    cast(h, wet, g("self"), g("freeze", "duration", 60.0));
    cast(h, dry, g("self"), g("freeze", "duration", 60.0));
    h.assertTrue(wet.getTicksFrozen() > dry.getTicksFrozen() + 60, "molhado: gelo congela mais (" + wet.getTicksFrozen() + " contra " + dry.getTicksFrozen() + ")");
    wet.setSecondsOnFire(0);
    cast(h, wet, g("self"), g("fire"));
    h.assertTrue(!wet.isOnFire(), "molhado: o fogo nem pega");
    h.assertTrue(!com.arcane.execution.Wetness.isSoaked(wet), "o vapor seca a criatura");
    cast(h, dry, g("self"), g("fire"));
    h.assertTrue(dry.isOnFire(), "seco: o fogo pega");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void invertSwapsEffectsForTheirOpposites(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    p.setHealth(5.0f);
    cast(h, p, g("self"), g("damage", "amount", 3.0), g("invert"));
    h.assertTrue(p.getHealth() > 5.0f, "Dano invertido cura (vida " + p.getHealth() + ")");
    float before = p.getHealth();
    cast(h, p, g("self"), g("heal", "amount", 3.0), g("invert"));
    h.assertTrue(p.getHealth() < before, "Cura invertida fere");
    cast(h, p, g("self"), g("slow"), g("invert"));
    h.assertTrue(p.hasEffect(MobEffects.MOVEMENT_SPEED) && !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Lentidao invertida vira Velocidade");
    cast(h, p, g("self"), g("blind"));
    h.assertTrue(p.hasEffect(MobEffects.BLINDNESS), "sem Inverter, Cegar cega");
    cast(h, p, g("self"), g("glide"), g("invert"));
    h.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), "efeito sem par ignora o Inverter");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void lifeStealHealsTheCasterAndTransferGivesLifeAway(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 4));
    caster.setHealth(4.0f);
    cast(h, caster, g("aura", "radius", 3.0), g("lifesteal", "amount", 4.0));
    h.assertTrue(cow.getHealth() < cow.getMaxHealth(), "Roubo fere o alvo");
    h.assertTrue(Math.abs(caster.getHealth() - 6.4f) < 0.05f, "devolve 60% do dano real (vida " + caster.getHealth() + ")");
    caster.setHealth(8.0f);
    cow.setHealth(4.0f);
    cast(h, caster, g("aura", "radius", 3.0), g("life_transfer", "amount", 3.0));
    h.assertTrue(Math.abs(caster.getHealth() - 5.0f) < 0.05f && Math.abs(cow.getHealth() - 7.0f) < 0.05f,
        "Transferir passa 3 de vida (caster " + caster.getHealth() + ", vaca " + cow.getHealth() + ")");
    caster.setHealth(3.0f);
    cow.setHealth(4.0f);
    cast(h, caster, g("aura", "radius", 3.0), g("life_transfer", "amount", 8.0));
    h.assertTrue(caster.getHealth() >= com.arcane.execution.handlers.LowCostEffects.LifeTransfer.KEEP - 0.01f, "nunca derruba abaixo de 1 coracao (vida " + caster.getHealth() + ")");
    caster.setHealth(8.0f);
    cow.setHealth(10.0f);
    float casterBefore = caster.getHealth();
    cow.invulnerableTime = 0; // o golpe anterior dela ainda vale na mesma tick
    cast(h, caster, g("aura", "radius", 3.0), g("life_transfer", "amount", 3.0), g("invert"));
    h.assertTrue(cow.getHealth() < 10.0f && caster.getHealth() > casterBefore, "Transferir invertido e Roubo de Vida");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void tauntFearAndSleepControlWhoAMobTargets(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 2);
    var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 4));
    cast(h, caster, g("aura", "radius", 4.0), g("taunt"));
    h.assertTrue(zombie.getTarget() == caster, "Provocar faz o zumbi perseguir o conjurador");
    cast(h, caster, g("aura", "radius", 4.0), g("fear", "duration", 100.0));
    h.assertTrue(zombie.getTarget() == null, "Medo faz largar o alvo");
    zombie.setTarget(caster);
    h.assertTrue(zombie.getTarget() == null, "com medo, nao escolhe o conjurador como alvo");
    cast(h, caster, g("aura", "radius", 4.0), g("taunt"));
    h.assertTrue(zombie.getTarget() == caster, "Provocar quebra o Medo");
    cast(h, caster, g("aura", "radius", 4.0), g("sleep", "duration", 200.0));
    h.assertTrue(com.arcane.execution.MindStates.isAsleep(zombie) && zombie.getTarget() == null, "Sono: dorme sem alvo");
    h.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "dormindo, nao anda");
    zombie.setTarget(caster);
    h.assertTrue(zombie.getTarget() == null, "dormindo, nao escolhe alvo");
    zombie.hurt(h.getLevel().damageSources().generic(), 1.0f);
    h.assertTrue(!com.arcane.execution.MindStates.isAsleep(zombie) && !zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "o dano acorda");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void rumbleHurtsShovesAndDazes(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 4));
    float before = cow.getHealth();
    cast(h, caster, g("ray", "range", 8.0), g("rumble", "radius", 3.0));
    h.assertTrue(cow.getHealth() < before, "Estrondo fere");
    h.assertTrue(cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "e deixa zonzo");
    h.assertTrue(caster.getHealth() >= caster.getMaxHealth(), "o conjurador nao sofre");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void chargeRushesForwardHurtingWhoIsInTheWay(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 4));
    float before = cow.getHealth();
    cast(h, caster, g("self"), g("charge", "distance", 8.0));
    h.assertTrue(caster.getDeltaMovement().z > 0.5, "o conjurador avanca (vz " + caster.getDeltaMovement().z + ")");
    h.assertTrue(cow.getHealth() < before, "ferindo quem esta no caminho");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void glideAndDarknessApplyTheirEffects(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    cast(h, p, g("self"), g("glide", "duration", 100.0));
    cast(h, p, g("self"), g("darkness", "duration", 100.0));
    h.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), "Planar: queda lenta");
    h.assertTrue(p.hasEffect(MobEffects.DARKNESS), "Escuridao");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void readItemDescribesTheHeldItem(GameTestHelper h) {
    floor(h);
    var stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
    stack.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 2);
    stack.setDamageValue(10);
    var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
    var msg = com.arcane.execution.handlers.LowCostEffects.ReadItem.describe(zombie, stack);
    var content = (net.minecraft.network.chat.contents.TranslatableContents) msg.getContents();
    h.assertTrue(content.getArgs()[2] instanceof net.minecraft.network.chat.Component name && name.getString().equals(stack.getHoverName().getString()), "traz o nome do item");
    h.assertTrue(msg.getSiblings().size() >= 2, "traz durabilidade e encantamentos");
    var empty = com.arcane.execution.handlers.LowCostEffects.ReadItem.describe(zombie, net.minecraft.world.item.ItemStack.EMPTY);
    h.assertTrue(((net.minecraft.network.chat.contents.TranslatableContents) empty.getContents()).getKey().endsWith("empty"), "mao vazia");
    var before = stack.copy();
    h.assertTrue(net.minecraft.world.item.ItemStack.matches(before, stack), "ler nao muda o item");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void targetHealthConditionGatesOnTheTargetsLife(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 2);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 4));
    cow.setHealth(2.0f);
    cast(h, caster, g("aura", "radius", 4.0), g("target_health", "percent", 50.0), g("damage", "amount", 1.0));
    h.assertTrue(cow.getHealth() < 2.0f, "vaca ferida (20%) passa em 50%");
    cow.setHealth(10.0f);
    cast(h, caster, g("aura", "radius", 4.0), g("target_health", "percent", 50.0), g("damage", "amount", 1.0));
    h.assertTrue(cow.getHealth() >= 10.0f, "vaca saudavel nao passa");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void regionAndHoldingConditionsGateTheCast(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    cast(h, p, g("self"), g("region", "kind", 2.0), g("shield"));
    h.assertTrue(!p.hasEffect(MobEffects.ABSORPTION), "no Overworld, a condicao Nether nao passa");
    cast(h, p, g("self"), g("region", "kind", 1.0), g("shield"));
    h.assertTrue(p.hasEffect(MobEffects.ABSORPTION), "no Overworld, a condicao Superficie passa");
    p.removeAllEffects();
    p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
    cast(h, p, g("self"), g("holding", "kind", 4.0), g("speed"));
    h.assertTrue(!p.hasEffect(MobEffects.MOVEMENT_SPEED), "espada nao e comida");
    cast(h, p, g("self"), g("holding", "kind", 1.0), g("speed"));
    h.assertTrue(p.hasEffect(MobEffects.MOVEMENT_SPEED), "espada passa em arma");
    p.removeAllEffects();
    p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
    cast(h, p, g("self"), g("holding", "kind", 0.0), g("speed"));
    h.assertTrue(p.hasEffect(MobEffects.MOVEMENT_SPEED), "mao vazia passa em kind 0");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase3_weather")
  public static void rainingConditionFollowsTheWorldWeather(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    h.getLevel().setWeatherParameters(6000, 0, false, false);
    h.getLevel().setRainLevel(0.0f);
    cast(h, p, g("self"), g("raining"), g("speed"));
    h.assertTrue(!p.hasEffect(MobEffects.MOVEMENT_SPEED), "tempo limpo: nao passa");
    h.getLevel().setWeatherParameters(0, 6000, true, false);
    h.getLevel().setRainLevel(1.0f); // a chuva sobe aos poucos; aqui ja vale cheia
    cast(h, p, g("self"), g("raining"), g("speed"));
    h.assertTrue(p.hasEffect(MobEffects.MOVEMENT_SPEED), "chovendo: passa");
    p.removeAllEffects();
    cast(h, p, g("self"), g("raining", "storm", 1.0), g("speed"));
    h.assertTrue(!p.hasEffect(MobEffects.MOVEMENT_SPEED), "so chuva: a variante tempestade nao passa");
    h.getLevel().setWeatherParameters(6000, 0, false, false);
    h.getLevel().setRainLevel(0.0f);
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void onKillArmsAndFiresOnlyOnTheCastersOwnKill(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(1, 1, 1));
    cast(h, caster, g("self"), g("on_kill"), g("shield"));
    h.assertTrue(!caster.hasEffect(MobEffects.ABSORPTION) && com.arcane.execution.ArmedSpells.armedCount(caster.getUUID()) == 1, "arma sem disparar");
    cow.hurt(h.getLevel().damageSources().genericKill(), 1000.0f);
    h.assertTrue(!caster.hasEffect(MobEffects.ABSORPTION), "morte alheia nao dispara");
    var sheep = h.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(5, 1, 5));
    sheep.hurt(h.getLevel().damageSources().mobAttack(caster), 1000.0f);
    h.assertTrue(caster.hasEffect(MobEffects.ABSORPTION), "matar dispara");
    h.assertTrue(com.arcane.execution.ArmedSpells.armedCount(caster.getUUID()) == 0, "dispara uma vez");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void onFallAndOnJumpFireFromTheirEvents(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    cast(h, p, g("self"), g("on_fall"), g("shield"));
    p.causeFallDamage(2.0f, 1.0f, h.getLevel().damageSources().fall());
    h.assertTrue(!p.hasEffect(MobEffects.ABSORPTION), "queda curta nao dispara");
    p.causeFallDamage(6.0f, 1.0f, h.getLevel().damageSources().fall());
    h.assertTrue(p.hasEffect(MobEffects.ABSORPTION), "queda de 3+ blocos dispara");
    p.removeAllEffects();
    cast(h, p, g("self"), g("on_jump"), g("speed"));
    h.assertTrue(!p.hasEffect(MobEffects.MOVEMENT_SPEED), "conjurar nao pula");
    net.minecraftforge.common.ForgeHooks.onLivingJump(p);
    h.assertTrue(p.hasEffect(MobEffects.MOVEMENT_SPEED), "pular dispara");
    h.assertTrue(com.arcane.execution.ArmedSpells.armedCount(p.getUUID()) == 0, "dispara uma vez");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void everyEffectAndConditionGlyphHasAHandler(GameTestHelper h) {
    for (var entry : GlyphRegistry.all().entrySet()) {
      var def = entry.getValue();
      var handler = def.behavior().handler();
      boolean ok = switch (def.category()) {
        case EFFECT -> GlyphHandlerRegistry.getEffect(handler).isPresent();
        case CONDITION -> GlyphHandlerRegistry.getCondition(handler).isPresent();
        case FILTER -> GlyphHandlerRegistry.getFilter(handler).isPresent();
        case FORM -> GlyphHandlerRegistry.getForm(handler).isPresent();
        default -> true;
      };
      h.assertTrue(ok, "glifo sem handler: " + entry.getKey());
    }
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void hookPullsTheCasterTowardTheTarget(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 6));
    cast(h, caster, g("ray", "range", 12.0), g("hook"));
    h.assertTrue(caster.getDeltaMovement().z > 0.3, "o gancho puxa para o alvo (vz " + caster.getDeltaMovement().z + ")");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void phaseCrossesAThinWallButNotWithoutRoom(GameTestHelper h) {
    floor(h);
    for (int y = 1; y <= 2; y++) for (int x = 2; x <= 4; x++) h.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
    Pig caster = pig(h, 3, 1);
    double wallZ = h.absolutePos(new BlockPos(3, 1, 3)).getZ() + 1.0;
    cast(h, caster, g("self"), g("phase", "thickness", 3.0));
    h.assertTrue(caster.getZ() > wallZ, "atravessou a parede (z " + caster.getZ() + " contra " + wallZ + ")");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void recallMarksThenReturns(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 1, 1);
    Vec3 start = caster.position();
    cast(h, caster, g("self"), g("recall"));
    h.assertTrue(caster.getPersistentData().contains(com.arcane.execution.Phase4State.RECALL), "marcou");
    caster.teleportTo(start.x + 3.0, start.y, start.z + 3.0);
    cast(h, caster, g("self"), g("recall"));
    h.assertTrue(caster.position().distanceTo(start) < 1.5, "voltou ao lugar marcado (a " + caster.position().distanceTo(start) + ")");
    h.assertTrue(!caster.getPersistentData().contains(com.arcane.execution.Phase4State.RECALL), "a marca foi gasta");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void swapTradesPlacesWithTheTarget(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 1, 1);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(5, 1, 1));
    Vec3 a = caster.position(), b = cow.position();
    cast(h, caster, g("aura", "radius", 6.0), g("swap"));
    h.assertTrue(caster.position().distanceTo(b) < 1.5 && cow.position().distanceTo(a) < 1.5, "trocaram de lugar");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100, batch = "phase4_orbit")
  public static void satellitesAndTrailHurtThoseWhoTouchThem(GameTestHelper h) {
    floor(h);
    com.arcane.execution.Phase4Service.clearAll();
    Pig caster = pig(h, 3, 3);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(5, 1, 3));
    float before = cow.getHealth();
    cast(h, caster, g("self"), g("satellites", "count", 4.0, "damage", 3.0));
    h.assertTrue(com.arcane.execution.Phase4Service.hasSatellites(caster.getUUID()), "satelites ativos");
    cast(h, caster, g("self"), g("trail", "damage", 2.0));
    h.assertTrue(com.arcane.execution.Phase4Service.trailOf(caster.getUUID()) != null, "rastro ativo");
    h.runAfterDelay(40, () -> {
      h.assertTrue(cow.getHealth() < before, "o satelite feriu a vaca (vida " + cow.getHealth() + ")");
      h.assertTrue(caster.getHealth() >= caster.getMaxHealth(), "o dono nao e ferido (vida " + caster.getHealth() + " de " + caster.getMaxHealth() + ")");
      com.arcane.execution.Phase4Service.clearAll();
      h.succeed();
    });
  }

  @GameTest(template = "empty")
  public static void climbIsRegisteredAndEnds(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    cast(h, p, g("self"), g("climb", "duration", 100.0));
    h.assertTrue(com.arcane.execution.Phase4Service.isClimber(p.getUUID()), "escalada ativa");
    com.arcane.execution.Phase4Service.dispelRegistries(p.getUUID());
    h.assertTrue(!com.arcane.execution.Phase4Service.isClimber(p.getUUID()), "dissipar encerra");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void linkSharesDamageWithThePartner(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var a = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 1, 3));
    var b = h.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(4, 1, 3));
    cast(h, caster, g("aura", "radius", 5.0), g("link"));
    float before = b.getHealth();
    a.hurt(h.getLevel().damageSources().generic(), 4.0f);
    h.assertTrue(Math.abs((before - b.getHealth()) - 2.0f) < 0.1f, "o parceiro sofre metade (" + (before - b.getHealth()) + ")");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void curseAmplifiesDamageAndBlocksHealing(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 3));
    cast(h, caster, g("aura", "radius", 4.0), g("curse"));
    cow.hurt(h.getLevel().damageSources().generic(), 4.0f);
    h.assertTrue(Math.abs((cow.getMaxHealth() - cow.getHealth()) - 5.0f) < 0.1f, "25% a mais de dano (" + (cow.getMaxHealth() - cow.getHealth()) + ")");
    float hurt = cow.getHealth();
    cow.heal(3.0f);
    h.assertTrue(cow.getHealth() == hurt, "nao cura");
    cast(h, cow, g("self"), g("dispel"));
    cow.heal(3.0f);
    h.assertTrue(cow.getHealth() > hurt, "Dissipar em si mesmo tira a maldicao");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void silenceBlocksCastingUntilDispelled(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig victim = pig(h, 3, 3);
    cast(h, caster, g("aura", "radius", 4.0), g("silence"));
    var blocked = CastingService.tryCast(victim, new Inscription(List.of(g("self"), g("shield"))), h.getLevel());
    h.assertTrue(!blocked.success() && blocked.message().contains("silenced"), "silenciado nao conjura: " + blocked.message());
    var selfDispel = CastingService.tryCast(victim, new Inscription(List.of(g("self"), g("dispel"))), h.getLevel());
    h.assertTrue(!selfDispel.success(), "o silenciado nao consegue nem se dissipar");
    com.arcane.execution.Phase4State.clear(victim, com.arcane.execution.Phase4State.SILENCE); // como se o tempo passasse
    var ok = CastingService.tryCast(victim, new Inscription(List.of(g("self"), g("shield"))), h.getLevel());
    h.assertTrue(ok.success(), "passado o tempo, conjura de novo");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void petrifyHalvesDamageAndHoldsTheMob(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
    cast(h, caster, g("aura", "radius", 4.0), g("petrify", "duration", 100.0));
    h.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "imovel");
    zombie.setTarget(caster);
    h.assertTrue(zombie.getTarget() == null, "nao escolhe alvo");
    float before = zombie.getHealth();
    zombie.hurt(h.getLevel().damageSources().generic(), 4.0f);
    h.assertTrue(Math.abs((before - zombie.getHealth()) - 2.0f) < 0.1f, "metade do dano (" + (before - zombie.getHealth()) + ")");
    h.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "dano nao quebra a pedra");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void acidIgnoresArmorAndCorrodesIt(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
    var plate = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
    zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, plate);
    float before = zombie.getHealth();
    cast(h, caster, g("aura", "radius", 4.0), g("acid", "amount", 4.0, "corrode", 20.0));
    h.assertTrue(Math.abs((before - zombie.getHealth()) - 4.0f) < 0.1f, "o acido ignora a armadura (" + (before - zombie.getHealth()) + ")");
    h.assertTrue(plate.getDamageValue() >= 20, "corroeu a peca (" + plate.getDamageValue() + ")");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void electrifyArcsDamageToNeighbours(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 1, 1);
    var a = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 3));
    var b = h.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(4, 1, 3));
    cast(h, a, g("self"), g("electrify"));
    float before = b.getHealth();
    a.hurt(h.getLevel().damageSources().generic(), 4.0f);
    h.assertTrue(b.getHealth() < before, "o dano saltou para a vizinha (" + b.getHealth() + ")");
    h.succeed();
  }

  // Lote proprio: o condutor e global (Phase4Service.CONDUCTORS) e roubava o raio do teste do Trovao quando os dois
  // rodavam juntos no mesmo lote (falha que dependia da ordem dentro do lote).
  @GameTest(template = "empty", batch = "isolated_conductor")
  public static void conductorRedirectsNearbyLightning(GameTestHelper h) {
    floor(h);
    Pig conductor = pig(h, 1, 1);
    cast(h, conductor, g("self"), g("electrify", "mode", 1.0));
    var bolt = EntityType.LIGHTNING_BOLT.create(h.getLevel());
    bolt.setVisualOnly(true);
    Vec3 far = h.absoluteVec(new Vec3(6.5, 1, 6.5));
    bolt.moveTo(far.x, far.y, far.z);
    h.getLevel().addFreshEntity(bolt);
    h.assertTrue(bolt.position().distanceTo(conductor.position()) < 1.0, "o raio caiu no condutor (a " + bolt.position().distanceTo(conductor.position()) + ")");
    com.arcane.execution.Phase4Service.clearAll();
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void contagionSpreadsHarmfulEffectsToNeighbours(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 1, 1);
    var a = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 3));
    var b = h.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(4, 1, 3));
    a.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, 200, 0));
    cast(h, caster, g("aura", "radius", 6.0), g("contagion"));
    h.assertTrue(b.hasEffect(MobEffects.POISON), "o veneno passou para a vizinha");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void reanimateRaisesARecentCorpseAsAnAlly(GameTestHelper h) {
    floor(h);
    com.arcane.execution.Phase4Service.clearAll();
    Pig caster = pig(h, 3, 2);
    var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 4));
    zombie.hurt(h.getLevel().damageSources().genericKill(), 1000.0f);
    cast(h, caster, g("self"), g("reanimate"));
    var minions = com.arcane.execution.Phase4Service.minionsOf(caster.getUUID());
    h.assertTrue(minions.size() == 1, "um aliado reanimado");
    var ally = (net.minecraft.world.entity.Mob) h.getLevel().getEntity(minions.get(0));
    h.assertTrue(ally != null && ally.isAlive() && ally.getType() == EntityType.ZOMBIE, "e um zumbi vivo");
    h.assertTrue(ally.getHealth() <= ally.getMaxHealth() * 0.51f, "com metade da vida");
    ally.setTarget(caster);
    h.assertTrue(ally.getTarget() == null, "nunca ataca o dono");
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(1, 1, 1));
    cow.hurt(h.getLevel().damageSources().mobAttack(caster), 1.0f);
    h.assertTrue(ally.getTarget() == cow, "vai atras de quem o dono atacou");
    cast(h, caster, g("self"), g("reanimate"));
    h.assertTrue(com.arcane.execution.Phase4Service.minionsOf(caster.getUUID()).size() == 1, "sem outro corpo, nao ergue mais");
    com.arcane.execution.Phase4Service.clearAll();
    ally.discard();
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void dispelStripsBeneficialEffectsFromOthers(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 3));
    cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
    cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, 200, 0));
    cast(h, cow, g("self"), g("electrify"));
    cast(h, caster, g("aura", "radius", 4.0), g("dispel"));
    h.assertTrue(!cow.hasEffect(MobEffects.MOVEMENT_SPEED) && cow.hasEffect(MobEffects.POISON), "tira o benefico e deixa o nocivo");
    h.assertTrue(!com.arcane.execution.Phase4State.active(cow, com.arcane.execution.Phase4State.CHARGED), "e o estado magico");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void reflectSendsDamageBackToTheAttacker(GameTestHelper h) {
    floor(h);
    Pig victim = pig(h, 3, 3);
    var attacker = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 4));
    cast(h, victim, g("self"), g("reflect"));
    float before = attacker.getHealth();
    victim.hurt(h.getLevel().damageSources().mobAttack(attacker), 5.0f);
    h.assertTrue(Math.abs((before - attacker.getHealth()) - 2.0f) < 0.1f, "o agressor sente 40% (" + (before - attacker.getHealth()) + ")");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void imbueAddsDamageAndAnEffectForAFewHits(GameTestHelper h) {
    floor(h);
    var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
    zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(4, 1, 3));
    cast(h, zombie, g("self"), g("imbue", "kind", 3.0, "hits", 2.0));
    h.assertTrue(zombie.getMainHandItem().getTagElement("arcane_imbue") != null, "arma encantada");
    float before = cow.getHealth();
    cow.hurt(h.getLevel().damageSources().mobAttack(zombie), 2.0f);
    h.assertTrue(before - cow.getHealth() >= 6.9f, "+2 de dano e +3 do raio (" + (before - cow.getHealth()) + ")");
    cow.invulnerableTime = 0;
    cow.hurt(h.getLevel().damageSources().mobAttack(zombie), 1.0f);
    h.assertTrue(zombie.getMainHandItem().getTagElement("arcane_imbue") == null, "acabaram os golpes");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void drainManaMovesManaBetweenPlayers(GameTestHelper h) {
    floor(h);
    var victim = casterWith(h);
    TestPlayers.add(h, victim);
    var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(victim);
    float before = mana.current();
    Pig thief = pig(h, 1, 1);
    cast(h, thief, g("aura", "radius", 6.0), g("drain_mana", "amount", 30.0));
    h.assertTrue(Math.abs((before - mana.current()) - 30.0f) < 0.5f, "tirou 30 de mana (" + (before - mana.current()) + ")");
    victim.discard();
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void emitSignalPowersAnAirBlockThenVanishes(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    BlockPos at = new BlockPos(3, 1, 3);
    cast(h, p, g("self"), g("emit_signal", "duration", 20.0));
    var state = h.getLevel().getBlockState(h.absolutePos(at));
    h.assertTrue(state.is(com.arcane.registry.ModBlocks.ARCANE_SIGNAL.get()), "bloco de sinal no ar");
    h.assertTrue(state.getSignal(h.getLevel(), h.absolutePos(at), net.minecraft.core.Direction.UP) == 15, "forca 15");
    h.runAfterDelay(40, () -> {
      h.assertTrue(h.getLevel().getBlockState(h.absolutePos(at)).isAir(), "sumiu sozinho");
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void signalInscriptionFiresOnTheRisingRedstoneEdge(GameTestHelper h) {
    floor(h);
    BlockPos stone = new BlockPos(3, 1, 3);
    h.setBlock(stone, Blocks.STONE);
    BlockPos abs = h.absolutePos(stone);
    Inscription spell = new Inscription(List.of(g("self"), g("on_signal"), g("shield")));
    WorldInscriptionIndex.get(h.getLevel()).put(new InscriptionAnchor(abs, spell, InscribingService.TRIGGER_ON_SIGNAL, new ResourceLocation("arcane", "common")));
    Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, stone.above());
    h.runAfterDelay(25, () -> {
      h.assertTrue(!pig.hasEffect(MobEffects.ABSORPTION), "sem sinal, nao dispara");
      h.setBlock(new BlockPos(3, 1, 4), Blocks.REDSTONE_BLOCK);
    });
    h.succeedWhen(() -> {
      h.assertTrue(pig.hasEffect(MobEffects.ABSORPTION), "com sinal, dispara");
      InscribingService.erase(h.getLevel(), abs);
    });
  }

  @GameTest(template = "empty")
  public static void afterHitsFiresOnTheNthHit(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 3));
    cast(h, caster, g("self"), g("after_hits", "n", 2.0), g("shield"));
    h.assertTrue(com.arcane.execution.ArmedSpells.armedCount(caster.getUUID()) == 1, "armada");
    cow.hurt(h.getLevel().damageSources().mobAttack(caster), 1.0f);
    h.assertTrue(!caster.hasEffect(MobEffects.ABSORPTION), "um acerto ainda nao dispara");
    cow.invulnerableTime = 0;
    cow.hurt(h.getLevel().damageSources().mobAttack(caster), 1.0f);
    h.assertTrue(caster.hasEffect(MobEffects.ABSORPTION), "o segundo acerto dispara");
    h.assertTrue(com.arcane.execution.ArmedSpells.armedCount(caster.getUUID()) == 0, "uma vez so");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase4_scars")
  public static void stealthShortensAndFalseTraceForgesTheResidue(GameTestHelper h) {
    floor(h);
    h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 5));
    Pig plain = pig(h, 1, 1);
    Pig sneaky = pig(h, 5, 1);
    Pig liar = pig(h, 3, 1);
    var level = h.getLevel();
    java.util.function.Function<Pig, List<com.arcane.execution.WorldScars.Scar>> mine = pig -> {
      var all = com.arcane.execution.WorldScars.all(level);
      return all.stream().filter(sc -> sc.pos().distanceToSqr(pig.position()) < 900.0).toList();
    };
    int base = mine.apply(plain).size();
    cast(h, plain, g("ray", "range", 8.0), g("damage"));
    cast(h, sneaky, g("ray", "range", 8.0), g("damage"), g("stealth"));
    cast(h, liar, g("ray", "range", 8.0), g("damage"), g("false_trace"));
    var scars = com.arcane.execution.WorldScars.all(level);
    var normal = scars.stream().filter(sc -> sc.caster().equals(plain.getUUID())).findFirst();
    var quiet = scars.stream().filter(sc -> sc.caster().equals(sneaky.getUUID())).findFirst();
    h.assertTrue(normal.isPresent() && quiet.isPresent(), "ambos deixaram residuo");
    h.assertTrue(quiet.get().expiresAt() - level.getGameTime() < (normal.get().expiresAt() - level.getGameTime()) / 2, "Furtivo: dura bem menos");
    h.assertTrue(scars.stream().noneMatch(sc -> sc.caster().equals(liar.getUUID())), "Falso Rastro: a assinatura nao e a do conjurador");
    h.assertTrue(scars.size() >= base + 4, "Falso Rastro deixa um residuo isca a mais");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void overchargeDoublesTheEffectAtAPrice(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 3));
    float before = cow.getHealth(), mine = caster.getHealth();
    cast(h, caster, g("aura", "radius", 4.0), g("damage", "amount", 2.0), g("overcharge"));
    h.assertTrue(Math.abs((before - cow.getHealth()) - 4.0f) < 0.2f, "dano dobrado (" + (before - cow.getHealth()) + ")");
    h.assertTrue(mine - caster.getHealth() >= 1.9f, "o conjurador paga 1 coracao");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void bindEndsPotionEffectsWhenTheCasterDies(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 3));
    cast(h, caster, g("aura", "radius", 4.0), g("slow", "duration", 400.0), g("bind"));
    h.assertTrue(cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "lentidao aplicada");
    caster.hurt(h.getLevel().damageSources().genericKill(), 1000.0f);
    h.assertTrue(!cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "o conjurador morreu: o efeito vinculado acabou");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void decoyDistractsMobsFromTheCaster(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
    zombie.setTarget(caster);
    cast(h, caster, g("self"), g("decoy", "duration", 100.0));
    h.assertTrue(zombie.getTarget() == null, "largou o alvo");
    zombie.setTarget(caster);
    h.assertTrue(zombie.getTarget() == null, "distraido: nao escolhe o conjurador");
    com.arcane.execution.Phase4Service.clearAll();
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void revealStripsInvisibilityAndSenseAndSightRun(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 3));
    cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.INVISIBILITY, 400, 0));
    cast(h, caster, g("self"), g("sense", "radius", 8.0));
    cast(h, caster, g("self"), g("arcane_sight", "duration", 100.0));
    h.assertTrue(caster.hasEffect(MobEffects.NIGHT_VISION), "Visao Arcana da visao noturna");
    cast(h, caster, g("self"), g("reveal", "radius", 8.0));
    h.assertTrue(!cow.hasEffect(MobEffects.INVISIBILITY), "Revelar tira a invisibilidade");
    h.succeed();
  }
}
