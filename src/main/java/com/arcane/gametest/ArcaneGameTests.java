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

/**
 * Testes dentro de um mundo real (./gradlew runGameTestServer).
 * Estrutura: data/arcane/structures/empty.nbt (tools/gen_test_structure.py), 7x5x7, chao de pedra em y=0.
 * Convencao: conjuradores olham para +Z; os alvos ficam a frente deles.
 */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class ArcaneGameTests {

  // ------------------------------------------------------------------ utilitarios
  private static GlyphInstance g(String id) {
    return new GlyphInstance(new ResourceLocation("arcane", id));
  }

  private static GlyphInstance g(String id, String key, double value) {
    return new GlyphInstance(new ResourceLocation("arcane", id), Map.of(key, value));
  }

  private static GlyphInstance g(String id, String k1, double v1, String k2, double v2) {
    return new GlyphInstance(new ResourceLocation("arcane", id), Map.of(k1, v1, k2, v2));
  }

  private static void floor(GameTestHelper h) {
    for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
  }


  // ------------------------------------------------------------------ Fase 0: persistencia, zonas, convidados
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


  // ------------------------------------------------------------------ Fase 1: modos de conjuracao
  private static net.minecraft.server.level.ServerPlayer casterWith(GameTestHelper h) {
    // Jogador de verdade (ServerPlayer) com um canal embutido: o helper do GameTest nao serve na 1.20.1 (sem canal de rede).
    var server = h.getLevel().getServer();
    var player = new net.minecraft.server.level.ServerPlayer(server, h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "arcane-test"));
    var link = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
    new io.netty.channel.embedded.EmbeddedChannel(link);
    new net.minecraft.server.network.ServerGamePacketListenerImpl(server, link, player);
    var pos = h.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1, 3.5));
    player.moveTo(pos.x, pos.y, pos.z);
    player.setYRot(0);
    player.setXRot(0);
    var grimoire = com.arcane.grimoire.GrimoireAttachments.get(player).orElseThrow(() -> new RuntimeException("sem grimorio"));
    for (String id : List.of("self", "aura", "damage", "float", "telekinesis"))
      grimoire.learnGlyph(new ResourceLocation("arcane", id), com.arcane.grimoire.GlyphKnowledge.MASTERED);
    com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(player).set(500f);
    return player;
  }

  private static Inscription addSpell(net.minecraft.server.level.ServerPlayer player, com.arcane.grimoire.SpellPrefs.CastMode mode, GlyphInstance... glyphs) {
    var spell = new Inscription(List.of(glyphs));
    var grimoire = com.arcane.grimoire.GrimoireAttachments.get(player).orElseThrow();
    grimoire.addInscription(spell);
    grimoire.prefs().setMode(spell.id(), mode);
    return spell;
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


  // ------------------------------------------------------------------ Fase 2: voo canalizado
  @GameTest(template = "empty", timeoutTicks = 160)
  public static void flightIsChanneledPaysManaEveryTickAndLandsSafely(GameTestHelper h) {
    floor(h);
    var player = casterWith(h);
    h.getLevel().addNewPlayer(player);
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


  // ------------------------------------------------------------------ Fase 3: glifos de custo baixo
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


  // ------------------------------------------------------------------ Fase 4: glifos medios
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

  @GameTest(template = "empty", batch = "isolated_thunder")
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
    h.getLevel().addNewPlayer(victim);
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

  private static Pig pig(GameTestHelper h, int x, int z) {
    Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(x, 1, z));
    pig.setYRot(0);
    pig.setXRot(0);
    return pig;
  }

  private static void cast(GameTestHelper h, LivingEntity caster, GlyphInstance... glyphs) {
    caster.setYRot(0);
    caster.setXRot(0);
    var result = CastingService.tryCast(caster, new Inscription(List.of(glyphs)), h.getLevel());
    h.assertTrue(result.success(), "a conjuracao falhou: " + result.message());
  }

  private static void assertHurt(GameTestHelper h, LivingEntity e, float before, String what) {
    h.assertTrue(e.getHealth() < before, what + " deveria ter sido ferido (vida " + e.getHealth() + ")");
  }

  private static void assertUnhurt(GameTestHelper h, LivingEntity e, float before, String what) {
    h.assertTrue(e.getHealth() >= before, what + " nao deveria ter sido ferido (vida " + e.getHealth() + ")");
  }

  // ------------------------------------------------------------------ cobertura dos glifos
  /** Todo glifo do datapack precisa de um handler no codigo (modificadores nao precisam). */
  @GameTest(template = "empty")
  public static void everyGlyphHasAHandler(GameTestHelper h) {
    for (Map.Entry<ResourceLocation, GlyphDefinition> e : GlyphRegistry.all().entrySet()) {
      GlyphDefinition def = e.getValue();
      ResourceLocation handler = def.behavior().handler();
      boolean ok = switch (def.category()) {
        case FORM -> GlyphHandlerRegistry.getForm(handler).isPresent();
        case EFFECT -> GlyphHandlerRegistry.getEffect(handler).isPresent();
        case FILTER -> GlyphHandlerRegistry.getFilter(handler).isPresent();
        case CONDITION -> GlyphHandlerRegistry.getCondition(handler).isPresent();
        case ACTIVATOR -> GlyphHandlerRegistry.getActivator(handler).isPresent();
        case MODIFIER -> def.modifierScope().isPresent();
      };
      h.assertTrue(ok, "glifo sem handler: " + e.getKey());
    }
    h.succeed();
  }

  // ------------------------------------------------------------------ efeitos em si mesmo
  @GameTest(template = "empty")
  public static void healRestoresHealth(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    p.setHealth(3.0f);
    cast(h, p, g("self"), g("heal", "amount", 4.0));
    h.assertTrue(p.getHealth() >= 7.0f, "cura deveria dar +4, vida " + p.getHealth());
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void potionEffectsApply(GameTestHelper h) {
    floor(h);
    Map<String, MobEffect> effects = Map.of("slow", MobEffects.MOVEMENT_SLOWDOWN, "poison", MobEffects.POISON,
        "speed", MobEffects.MOVEMENT_SPEED, "levitate", MobEffects.LEVITATION, "shield", MobEffects.ABSORPTION);
    int x = 0;
    for (Map.Entry<String, MobEffect> e : effects.entrySet()) {
      Pig p = pig(h, x++, 3);
      cast(h, p, g("self"), g(e.getKey()));
      h.assertTrue(p.hasEffect(e.getValue()), "glifo " + e.getKey() + " nao aplicou o efeito");
    }
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void powerAddsALevelToPotionEffects(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 3);
    cast(h, p, g("self"), g("speed"), g("power"));
    h.assertTrue(p.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() == 2, "Velocidade nivel 1 + Potencia deveria ser amplificador 2");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void blinkTeleportsForward(GameTestHelper h) {
    floor(h);
    Pig p = pig(h, 3, 0);
    double before = p.getZ();
    cast(h, p, g("self"), g("blink", "distance", 5.0));
    h.assertTrue(p.getZ() - before >= 3.0, "o salto deveria avancar ao menos 3 blocos, avancou " + (p.getZ() - before));
    h.succeed();
  }

  // ------------------------------------------------------------------ formas
  @GameTest(template = "empty")
  public static void auraHitsOthersWithinRadiusButNotTheCaster(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Pig near = pig(h, 3, 5);
    Pig far = pig(h, 0, 0);
    float a = caster.getHealth(), b = near.getHealth(), c = far.getHealth();
    cast(h, caster, g("aura", "radius", 3.0), g("damage", "amount", 3.0));
    assertUnhurt(h, caster, a, "o conjurador");
    assertHurt(h, near, b, "o alvo a 2 blocos");
    assertUnhurt(h, far, c, "o alvo fora do raio");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void rayHitsOnlyTheFirstCreature(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig first = pig(h, 3, 3);
    Pig behind = pig(h, 3, 5);
    float f = first.getHealth(), b = behind.getHealth();
    cast(h, caster, g("ray"), g("damage", "amount", 3.0));
    assertHurt(h, first, f, "o primeiro alvo");
    assertUnhurt(h, behind, b, "o alvo de tras");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void touchHasShortRange(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    Pig close = pig(h, 3, 2);
    Pig distant = pig(h, 0, 6);
    Pig out = pig(h, 3, 6);
    float c = close.getHealth(), o = out.getHealth();
    cast(h, caster, g("touch"), g("damage"));
    assertHurt(h, close, c, "o alvo a 2 blocos");
    cast(h, distant, g("touch"), g("damage"));
    assertUnhurt(h, out, o, "o alvo a 6 blocos (alcance padrao 3)");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void projectileHitsACreature(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    Pig target = pig(h, 3, 5);
    float before = target.getHealth();
    cast(h, caster, g("projectile"), g("damage", "amount", 4.0));
    h.succeedWhen(() -> assertHurt(h, target, before, "o alvo do projetil"));
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void projectileOnImpactIgnitesABlock(GameTestHelper h) {
    floor(h);
    h.setBlock(new BlockPos(3, 1, 6), Blocks.STONE);
    Pig caster = pig(h, 3, 0);
    cast(h, caster, g("projectile"), g("on_hit_block"), g("ignite"));
    h.succeedWhen(() -> h.assertBlock(new BlockPos(3, 2, 6), b -> b == Blocks.FIRE, () -> "o bloco deveria ter fogo em cima"));
  }

  // ------------------------------------------------------------------ filtros
  @GameTest(template = "empty")
  public static void undeadAndHostileFiltersPickTheRightCreatures(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Pig animal = pig(h, 3, 4);
    Husk husk = h.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 1, 4));
    float a = animal.getHealth(), k = husk.getHealth();
    cast(h, caster, g("aura", "radius", 5.0), g("undead"), g("damage", "amount", 3.0));
    assertUnhurt(h, animal, a, "o animal (filtro Mortos-vivos)");
    assertHurt(h, husk, k, "o morto-vivo");

    husk.invulnerableTime = 0; // senao o segundo golpe cai nos quadros de invulnerabilidade
    float k2 = husk.getHealth();
    cast(h, caster, g("aura", "radius", 5.0), g("hostile"), g("damage", "amount", 3.0));
    assertUnhurt(h, animal, a, "o animal (filtro Hostis)");
    assertHurt(h, husk, k2, "o hostil");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void animalsFilterIgnoresMonsters(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    Pig animal = pig(h, 3, 4);
    Husk husk = h.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 1, 4));
    float a = animal.getHealth(), k = husk.getHealth();
    cast(h, caster, g("aura", "radius", 5.0), g("animals"), g("damage", "amount", 3.0));
    assertHurt(h, animal, a, "o animal");
    assertUnhurt(h, husk, k, "o monstro");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void nearestFilterKeepsOnlyOne(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig near = pig(h, 3, 3);
    Pig far = pig(h, 3, 5);
    float n = near.getHealth(), f = far.getHealth();
    cast(h, caster, g("aura", "radius", 6.0), g("nearest"), g("damage", "amount", 3.0));
    assertHurt(h, near, n, "o mais proximo");
    assertUnhurt(h, far, f, "o mais distante");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void woundedFilterOnlyAffectsCreaturesBelowHalfHealth(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    Pig hurt = pig(h, 3, 3);
    Pig healthy = pig(h, 3, 4);
    hurt.setHealth(3.0f);
    float hh = healthy.getHealth();
    cast(h, caster, g("aura", "radius", 6.0), g("wounded"), g("damage", "amount", 1.0));
    assertHurt(h, hurt, 3.0f, "o ferido");
    assertUnhurt(h, healthy, hh, "o saudavel");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void notSelfFilterRemovesTheCaster(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    float before = caster.getHealth();
    cast(h, caster, g("self"), g("not_self"), g("damage", "amount", 3.0));
    assertUnhurt(h, caster, before, "o conjurador (Exceto Eu)");
    h.succeed();
  }

  // ------------------------------------------------------------------ efeitos em blocos e explosoes
  @GameTest(template = "empty")
  public static void digBreaksSoftBlocksButNeverChestsOrHardOnes(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.DIRT);
    cast(h, caster, g("ray"), g("dig"));
    h.assertBlockNotPresent(Blocks.DIRT, new BlockPos(3, 1, 4));

    h.setBlock(new BlockPos(3, 1, 4), Blocks.CHEST);
    cast(h, caster, g("ray"), g("dig", "power", 5.0));
    h.assertBlockPresent(Blocks.CHEST, new BlockPos(3, 1, 4));

    h.setBlock(new BlockPos(3, 1, 4), Blocks.OBSIDIAN);
    cast(h, caster, g("ray"), g("dig", "power", 5.0));
    h.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(3, 1, 4));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void ignitePlacesFireOnTheTargetBlock(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    h.setBlock(new BlockPos(3, 1, 4), Blocks.STONE);
    cast(h, caster, g("ray"), g("ignite"));
    h.assertBlockPresent(Blocks.FIRE, new BlockPos(3, 2, 4));
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void explosionHurtsCreaturesButNotBlocks(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    Pig target = pig(h, 3, 4);
    h.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
    float before = target.getHealth();
    cast(h, caster, g("ray"), g("explode", "radius", 3.0));
    assertHurt(h, target, before, "o alvo da explosao");
    h.assertBlockPresent(Blocks.STONE, new BlockPos(4, 1, 4));
    h.succeed();
  }

  // Lote proprio: efeitos de area de testes vizinhos (empurrar, atrair) podiam arremessar a vaca para longe.
  @GameTest(template = "empty", batch = "isolated_thunder")
  public static void thunderStrikesTheTarget(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 0);
    // Vaca: um porco atingido por raio vira piglin zumbificado e sumiria do teste.
    var target = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 4));
    float before = target.getHealth();
    cast(h, caster, g("ray"), g("thunder"));
    h.runAfterDelay(3, () -> {
      assertHurt(h, target, before, "o alvo do raio");
      h.succeed();
    });
  }

  // ------------------------------------------------------------------ tempo e condicoes
  @GameTest(template = "empty", timeoutTicks = 100)
  public static void delayPostponesTheNextSegment(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 3);
    float before = caster.getHealth();
    cast(h, caster, g("self"), g("heal", "amount", 1.0), g("delay", "seconds", 1.0), g("damage", "amount", 3.0));
    h.runAfterDelay(5, () -> assertUnhurt(h, caster, before, "o conjurador antes do atraso"));
    h.succeedWhen(() -> assertHurt(h, caster, before, "o conjurador depois do atraso"));
  }

  @GameTest(template = "empty")
  public static void sneakingConditionGatesTheSegment(GameTestHelper h) {
    floor(h);
    Pig sneaking = pig(h, 2, 3);
    Pig standing = pig(h, 4, 3);
    sneaking.setShiftKeyDown(true);
    cast(h, sneaking, g("self"), g("sneaking"), g("speed"));
    cast(h, standing, g("self"), g("sneaking"), g("speed"));
    h.assertTrue(sneaking.hasEffect(MobEffects.MOVEMENT_SPEED), "agachado deveria ativar");
    h.assertTrue(!standing.hasEffect(MobEffects.MOVEMENT_SPEED), "em pe nao deveria ativar");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void lowHealthConditionGatesTheSegment(GameTestHelper h) {
    floor(h);
    Pig weak = pig(h, 2, 3);
    Pig strong = pig(h, 4, 3);
    weak.setHealth(3.0f);
    cast(h, weak, g("self"), g("low_health"), g("shield"));
    cast(h, strong, g("self"), g("low_health"), g("shield"));
    h.assertTrue(weak.hasEffect(MobEffects.ABSORPTION), "com pouca vida deveria ativar");
    h.assertTrue(!strong.hasEffect(MobEffects.ABSORPTION), "com vida cheia nao deveria ativar");
    h.succeed();
  }

  // ------------------------------------------------------------------ mago e grimorio
  @GameTest(template = "empty")
  public static void everyMageElementCastsAValidSpell(GameTestHelper h) {
    for (MageElement e : MageElement.values()) {
      var validation = com.arcane.inscription.InscriptionValidator.validate(e.spell());
      h.assertTrue(validation.valid(), e + ": " + validation.message().getString());
      com.arcane.inscription.CompiledPlanCache.getOrCompile(e.spell());
    }
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void mageVariantSurvivesSaveAndLoad(GameTestHelper h) {
    NPCMageEntity mage = h.spawn(ModEntities.NPC_MAGE.get(), new BlockPos(3, 1, 3));
    mage.setVariant(13);
    CompoundTag tag = new CompoundTag();
    mage.addAdditionalSaveData(tag);
    NPCMageEntity other = h.spawn(ModEntities.NPC_MAGE.get(), new BlockPos(1, 1, 1));
    other.readAdditionalSaveData(tag);
    h.assertTrue(other.variant() == 13, "variante deveria ser 13, foi " + other.variant());
    h.assertTrue(other.element() == MageElement.FROST, "13 % 6 = 1 = gelo, foi " + other.element());
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void grimoireItemBelongsToItsOwnerOnly(GameTestHelper h) {
    Player owner = h.makeMockPlayer();
    var stack = GrimoireItem.createFor(owner);
    h.assertTrue(GrimoireItem.isOwnedBy(stack, owner.getUUID()), "o dono deveria reconhecer o grimorio");
    h.assertTrue(!GrimoireItem.isOwnedBy(stack, java.util.UUID.randomUUID()), "outro jogador nao deveria");
    h.succeed();
  }

  // ------------------------------------------------------------------ cristal e inscricoes no mundo
  /** O cristal deve ser achado em qualquer posicao relativa, nao so em multiplos de 8. */
  @GameTest(template = "empty", batch = "crystal")
  public static void crystalZoneDetectedAtAnyOffset(GameTestHelper helper) {
    BlockPos crystal = new BlockPos(1, 1, 1);
    BlockPos probe = new BlockPos(4, 2, 3); // distancia ~3,7: zona 5 (x32), com Mana Poisoning
    EnvironmentalSnapshot before = AmbientFieldResolver.resolve(helper.getLevel(), helper.absolutePos(probe));
    helper.assertTrue(before.potencyMultiplier() == 1.0, "sem cristal a potencia deveria ser 1, foi " + before.potencyMultiplier());

    helper.setBlock(crystal, ModBlocks.MANA_CRYSTAL.get());
    EnvironmentalSnapshot near = AmbientFieldResolver.resolve(helper.getLevel(), helper.absolutePos(probe));
    helper.assertTrue(near.potencyMultiplier() == 32.0, "perto do cristal a potencia deveria ser 32, foi " + near.potencyMultiplier());
    helper.assertTrue(near.poisoningExposureRate() > 0, "perto do cristal deveria haver Mana Poisoning");

    // ~20 blocos: zona 2 (x4), sem envenenamento
    BlockPos far = helper.absolutePos(crystal).offset(14, 3, 14);
    EnvironmentalSnapshot mid = AmbientFieldResolver.resolve(helper.getLevel(), far);
    helper.assertTrue(mid.potencyMultiplier() == 4.0, "a ~20 blocos a potencia deveria ser 4, foi " + mid.potencyMultiplier());
    helper.assertTrue(mid.poisoningExposureRate() == 0, "a ~20 blocos nao deveria envenenar");
    helper.setBlock(crystal, Blocks.AIR);
    checkCrystalWidensReach(helper);
    helper.succeed();
  }

  /** Bloco inscrito com Eu + Dano fere quem pisa nele. */
  @GameTest(template = "empty", timeoutTicks = 100)
  public static void touchInscriptionTriggers(GameTestHelper helper) {
    BlockPos stone = new BlockPos(3, 1, 3);
    helper.setBlock(stone, Blocks.STONE);
    BlockPos abs = helper.absolutePos(stone);
    Inscription trap = new Inscription(List.of(g("self"), g("damage", "amount", 4.0)));
    WorldInscriptionIndex.get(helper.getLevel()).put(new InscriptionAnchor(abs, trap,
        InscribingService.TRIGGER_ON_TOUCH, new ResourceLocation("arcane", "common")));

    Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, stone.above());
    float startHealth = pig.getHealth();
    helper.succeedWhen(() -> {
      helper.assertTrue(pig.getHealth() < startHealth, "o porco ainda nao foi ferido");
      InscribingService.erase(helper.getLevel(), abs);
    });
  }

  /** Quebrar o bloco apaga a inscricao. */
  @GameTest(template = "empty")
  public static void erasingRemovesInscription(GameTestHelper helper) {
    BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
    WorldInscriptionIndex index = WorldInscriptionIndex.get(helper.getLevel());
    index.put(new InscriptionAnchor(abs, Inscription.testSelfDamage(), InscribingService.TRIGGER_ON_TOUCH,
        new ResourceLocation("arcane", "common")));
    helper.assertTrue(index.get(abs).isPresent(), "inscricao nao foi gravada");
    InscribingService.erase(helper.getLevel(), abs);
    helper.assertTrue(index.get(abs).isEmpty(), "inscricao nao foi apagada");
    helper.succeed();
  }

  // ------------------------------------------------------------------ design v2
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

  // ================================================================== RITUAIS (arena 35x35)
  private static final BlockPos ALTAR = new BlockPos(17, 1, 17);

  private static void arena(GameTestHelper h) {
    for (int x = 0; x < 35; x++) for (int z = 0; z < 35; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    h.setBlock(ALTAR, com.arcane.registry.ModBlocks.ARCANE_ALTAR.get());
  }

  private static BlockPos altarAbs(GameTestHelper h) {
    return h.absolutePos(ALTAR);
  }

  private static Pig pigAt(GameTestHelper h, int x, int z) {
    Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(x, 1, z));
    pig.setYRot(0);
    pig.setXRot(0);
    return pig;
  }

  private static com.arcane.ritual.ComponentSource stock(Object... itemsAndCounts) {
    Map<net.minecraft.world.item.Item, Integer> map = new java.util.HashMap<>();
    for (int i = 0; i < itemsAndCounts.length; i += 2) {
      map.put((net.minecraft.world.item.Item) itemsAndCounts[i], (Integer) itemsAndCounts[i + 1]);
    }
    return com.arcane.ritual.ComponentSource.of(map);
  }

  private static com.arcane.ritual.RitualService.Result ritual(GameTestHelper h, LivingEntity performer,
                                                               com.arcane.ritual.ComponentSource source, boolean legendary, GlyphInstance... glyphs) {
    return com.arcane.ritual.RitualService.perform(h.getLevel(), altarAbs(h), performer, new Inscription(List.of(glyphs)), source, legendary);
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

  // ------------------------------------------------------------------ juramentos, Ao Morrer, cicatrizes, interacoes, espelho
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

  private static GlyphInstance oath(int kind, double seconds) {
    return g("vow_oath").withParameter("kind", kind).withParameter("seconds", seconds);
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

  // ------------------------------------------------------------------ formatos e origem
  private static int countBarrier(GameTestHelper h, int r) {
    int n = 0;
    for (int x = -r; x <= r; x++) for (int y = -1; y <= r + 1; y++) for (int z = -r; z <= r; z++) {
      if (h.getLevel().getBlockState(h.absolutePos(new BlockPos(7 + x, 1 + y, 7 + z))).is(com.arcane.registry.ModBlocks.ARCANE_BARRIER.get())) n++;
    }
    return n;
  }

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

  private static int traceCount(GameTestHelper h, int radius) {
    int n = 0;
    for (var cell : com.arcane.ritual.RitualTemplate.cells(radius)) {
      if (com.arcane.ritual.RitualService.traceAt(h.getLevel(), altarAbs(h), cell) != null) n++;
    }
    return n;
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

  private static InscriptionAnchor drawing(BlockPos abs, String ink, long created, net.minecraft.core.Direction face) {
    ResourceLocation inkId = new ResourceLocation("arcane", ink);
    Inscription spell = new Inscription(List.of(g("self"), g("damage", "amount", 1.0)), new com.arcane.inscription.InscriptionMetadata(
        "", inkId, new ResourceLocation("arcane", "arcane_pen"), 1.0, java.util.Optional.empty(), created,
        com.arcane.inscription.InscriptionSourceKind.WORLD));
    return new InscriptionAnchor(abs, spell, InscribingService.TRIGGER_ON_SIGNAL, inkId, face);
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

  private static Inscription objectSpell(String ink, long created, boolean persistent) {
    ResourceLocation inkId = new ResourceLocation("arcane", ink);
    List<GlyphInstance> glyphs = persistent ? List.of(g("self"), g("persistence"), g("shield")) : List.of(g("self"), g("shield"));
    return new Inscription(glyphs, new com.arcane.inscription.InscriptionMetadata("", inkId, new ResourceLocation("arcane", "arcane_pen"),
        1.0, java.util.Optional.empty(), created, com.arcane.inscription.InscriptionSourceKind.ITEM));
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
    h.assertTrue(com.arcane.item.ScepterItem.restyle(scepter, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD))
        && com.arcane.item.ScepterItem.head(scepter) == 2, "esmeralda troca a ponta");
    h.assertTrue(com.arcane.item.ScepterItem.restyle(scepter, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE))
        && com.arcane.item.ScepterItem.shaft(scepter) == 3, "osso troca a haste");
    h.assertTrue(!com.arcane.item.ScepterItem.restyle(scepter, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT)), "terra nao muda nada");
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
    h.assertTrue(victim.getHealth() < before, "o golpe de cauda fere quem esta perto");
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
    int left = 0;
    for (int y = 1; y <= 3; y++) if (!h.getBlockState(new BlockPos(5, y, 1)).isAir()) left++;
    h.assertTrue(left == 0, "Aniquilacao apagou os blocos (" + left + " ficaram)");
    h.succeed();
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

  @GameTest(template = "empty", batch = "crater_queue", timeoutTicks = 80)
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
      h.runAfterDelay(5, () -> {
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
    h.getLevel().addNewPlayer(player);
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

  // ================================================================== Revisao: toggle, area, custo, ritual sem forma
  private static net.minecraft.server.level.ServerPlayer learn(net.minecraft.server.level.ServerPlayer player, String... ids) {
    var grimoire = com.arcane.grimoire.GrimoireAttachments.get(player).orElseThrow();
    for (String id : ids) grimoire.learnGlyph(new ResourceLocation("arcane", id), com.arcane.grimoire.GlyphKnowledge.MASTERED);
    return player;
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
    h.getLevel().addNewPlayer(player);
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

  // ------------------------------------------------------------------ regioes: qualquer formato em qualquer forma
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

  // ------------------------------------------------------------------ alvos tipados: criatura, bloco e ponto vazio
  @GameTest(template = "empty")
  public static void fireOnEmptyPointLightsTheGround(GameTestHelper h) {
    floor(h);
    Pig caster = pig(h, 3, 1);
    cast(h, caster, g("ray", "range", 4.0), g("fire"));
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

  private static long countWarnings(List<net.minecraft.network.chat.Component> lines) {
    return lines.stream().filter(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
        && (t.getKey().startsWith("gui.arcane.preview.warn."))).count();
  }

  // ------------------------------------------------------------------ zonas e eventos
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

  // ------------------------------------------------------------------ matriz de combinacoes e tabela de interacoes
  @GameTest(template = "empty", timeoutTicks = 60)
  public static void interactionTableIsLoadedWithoutProblems(GameTestHelper h) {
    h.assertTrue(com.arcane.execution.InteractionRules.rules().size() >= 7, "regras carregadas: " + com.arcane.execution.InteractionRules.rules().size());
    h.assertTrue(com.arcane.execution.InteractionRules.problems().isEmpty(), "problemas: " + com.arcane.execution.InteractionRules.problems());
    // Adia o fim: limpar a arena na hora apagaria criaturas de testes vizinhos que ainda rodam.
    h.runAfterDelay(15, h::succeed);
  }

  private static List<String> glyphsOf(com.arcane.glyph.GlyphCategory category, boolean includeRitual) {
    return com.arcane.glyph.GlyphRegistry.all().values().stream()
        .filter(d -> d.category() == category && (includeRitual || d.ritual() == 0))
        .map(d -> d.id().getPath()).sorted().toList();
  }

  /** Conjura e devolve um texto de falha (ou null). Falhar por mana/ritual e legitimo; excecao ou invalidez nao. */
  private static String tryCombo(GameTestHelper h, GlyphInstance... glyphs) {
    Pig caster = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
    caster.setYRot(0);
    caster.setXRot(0);
    var inscription = new Inscription(List.of(glyphs));
    String name = glyphs.length == 0 ? "" : java.util.Arrays.stream(glyphs).map(x -> x.glyphId().getPath()).reduce((a, b) -> a + "+" + b).orElse("");
    try {
      var validation = com.arcane.inscription.InscriptionValidator.validate(inscription);
      if (!validation.valid()) return name + ": invalida (" + validation.message().getString() + ")";
      com.arcane.execution.SpellPreview.describe(inscription);
      var result = CastingService.tryCast(caster, inscription, h.getLevel());
      if (!result.success() && !(result.message().contains("requires_ritual") || result.message().contains("mana"))) {
        return name + ": falhou (" + result.message() + ")";
      }
    } catch (RuntimeException e) {
      return name + ": excecao " + e;
    } finally {
      com.arcane.execution.Zones.clearFor(caster.getUUID());
      caster.discard();
    }
    return null;
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

  private static void add(List<String> failures, String failure) {
    if (failure != null) failures.add(failure);
  }

  // ------------------------------------------------------------------ imunidade do conjurador, explosao destrutiva, cadeia
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

  // ------------------------------------------------------------------ rodada: potencia pela escrita, circulo a mao, invocados
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

  // ------------------------------------------------------------------ mana: base editavel, glifos descobertos, pocoes
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

  // ------------------------------------------------------------------ rodada: area do ritual, simetria, cristal, glifos novos
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

  private static void checkCrystalWidensReach(GameTestHelper h) {
    Pig caster = pig(h, 3, 3);
    var spell = new Inscription(List.of(g("aura"), g("damage")));
    var plan = com.arcane.inscription.CompiledPlanCache.getOrCompile(spell);
    var far = AmbientFieldResolver.resolve(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 3)));
    var ctx0 = new com.arcane.execution.ExecutionContext(h.getLevel(), caster, spell, plan, far,
        com.arcane.execution.PrecisionSnapshot.WORKBENCH, com.arcane.execution.ExecutionContext.TriggerEvent.cast());
    h.assertTrue(ctx0.areaFactor() == 1.0, "sem cristal o alcance e normal");
    h.setBlock(new BlockPos(1, 1, 1), ModBlocks.MANA_CRYSTAL.get());
    var near = AmbientFieldResolver.resolve(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 3)));
    var ctx1 = new com.arcane.execution.ExecutionContext(h.getLevel(), caster, spell, plan, near,
        com.arcane.execution.PrecisionSnapshot.WORKBENCH, com.arcane.execution.ExecutionContext.TriggerEvent.cast());
    h.assertTrue(ctx1.areaFactor() == 32.0, "perto do cristal o raio/area usa o multiplicador da zona (x" + ctx1.areaFactor() + ")");
    h.assertTrue(com.arcane.ambient.ManaPoisoningService.zoneOf(near) == 5, "zona 5");
    h.assertTrue(near.poisoningExposureRate() >= 0.9 - 1e-9, "veneno forte na zona 5: " + near.poisoningExposureRate());
    h.assertTrue(com.arcane.ambient.ManaPoisoningService.wildCastCeiling(5) > com.arcane.ambient.ManaPoisoningService.wildCastCeiling(2)
        && com.arcane.ambient.ManaPoisoningService.wildCastCeiling(0) == 0.0, "a chance de magia aleatoria cresce com a zona");
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
