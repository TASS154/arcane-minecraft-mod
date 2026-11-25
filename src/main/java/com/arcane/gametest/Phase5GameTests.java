package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.execution.CastingService;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.pocket.ArcanePocket;
import com.arcane.restore.BlockJournal;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** GameTests da Fase 5: Bolso Arcano, Restaurar e Coro. */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class Phase5GameTests {

  // ------------------------------------------------------------------ utilitarios
  static GlyphInstance g(String id) { return new GlyphInstance(new ResourceLocation("arcane", id)); }

  static GlyphInstance g(String id, String key, double value) {
    return new GlyphInstance(new ResourceLocation("arcane", id), Map.of(key, value));
  }

  static void floor(GameTestHelper h) {
    for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
  }

  /** Jogador de verdade (com canal embutido), dentro do mundo, que domina os glifos pedidos. */
  static ServerPlayer player(GameTestHelper h, double x, double z, String... glyphs) {
    var server = h.getLevel().getServer();
    var p = new ServerPlayer(server, h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "p5-" + UUID.randomUUID().toString().substring(0, 5)));
    var link = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
    new io.netty.channel.embedded.EmbeddedChannel(link);
    new net.minecraft.server.network.ServerGamePacketListenerImpl(server, link, p);
    var pos = h.absoluteVec(new net.minecraft.world.phys.Vec3(x, 1, z));
    p.moveTo(pos.x, pos.y, pos.z);
    p.setYRot(0);
    p.setXRot(0);
    var grimoire = com.arcane.grimoire.GrimoireAttachments.get(p).orElseThrow(() -> new RuntimeException("sem grimorio"));
    for (String id : glyphs) grimoire.learnGlyph(new ResourceLocation("arcane", id), com.arcane.grimoire.GlyphKnowledge.MASTERED);
    com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(p).set(500f);
    TestPlayers.add(h, p);
    return p;
  }

  static com.arcane.execution.ExecutionResult cast(GameTestHelper h, net.minecraft.world.entity.LivingEntity caster, GlyphInstance... glyphs) {
    caster.setYRot(0);
    caster.setXRot(0);
    return CastingService.tryCast(caster, new Inscription(List.of(glyphs)), h.getLevel());
  }

  // ------------------------------------------------------------------ Bolso Arcano
  @GameTest(template = "empty", batch = "phase5_pocket_1")
  public static void pocketRefusesBlockedItemsAndLockedSlots(GameTestHelper h) {
    var p = player(h, 3.5, 3.5);
    var c = ArcanePocket.container(p);
    h.assertTrue(c.canPlaceItem(0, new ItemStack(Items.DIAMOND)), "item comum entra no espaco 0");
    h.assertTrue(c.canPlaceItem(ArcanePocket.slots() - 1, new ItemStack(Items.DIRT)), "o ultimo espaco usavel aceita");
    h.assertTrue(!c.canPlaceItem(ArcanePocket.slots(), new ItemStack(Items.DIRT)), "espaco travado (alem de " + ArcanePocket.slots() + ") nao aceita");
    h.assertTrue(!c.canPlaceItem(0, new ItemStack(Items.SHULKER_BOX)), "shulker nao entra");
    h.assertTrue(!c.canPlaceItem(0, new ItemStack(com.arcane.registry.ModItems.GRIMOIRE.get())), "o grimorio nao entra");
    h.assertTrue(!c.canPlaceItem(0, new ItemStack(com.arcane.registry.ModItems.ARCANE_PAGE.get())), "paginas nao entram");
    h.assertTrue(!c.canPlaceItem(0, new ItemStack(Items.BUNDLE)), "a lista da config bloqueia");
    h.assertTrue(ArcanePocket.slots() == 13, "13 espacos por padrao (meio bau pequeno)");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_pocket_2")
  public static void pocketKeepsItemsAcrossSaveReloadAndDeath(GameTestHelper h) {
    var p = player(h, 3.5, 3.5);
    var c = ArcanePocket.container(p);
    c.setItem(0, new ItemStack(Items.DIAMOND, 5));
    c.setItem(12, new ItemStack(Items.GOLD_INGOT, 3));
    // grava na hora: a etiqueta ja esta no jogador sem precisar "fechar"
    h.assertTrue(p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).contains(ArcanePocket.KEY), "etiqueta gravada ao alterar");
    // morte: o clone copia a etiqueta para o novo corpo
    var reborn = player(h, 4.5, 4.5);
    ArcanePocket.forget(reborn.getUUID());
    com.arcane.pocket.PocketEvents.copyBetween(p, reborn);
    var c2 = ArcanePocket.container(reborn);
    h.assertTrue(c2 != c, "o novo corpo tem um contêiner proprio");
    h.assertTrue(c2.getItem(0).is(Items.DIAMOND) && c2.getItem(0).getCount() == 5, "os diamantes sobrevivem a morte");
    h.assertTrue(c2.getItem(12).is(Items.GOLD_INGOT) && c2.getItem(12).getCount() == 3, "o ouro sobrevive a morte");
    // o que o jogador grava em disco (ForgeData) traz as duas pilhas
    var nbt = p.saveWithoutId(new net.minecraft.nbt.CompoundTag());
    var saved = nbt.getCompound("ForgeData").getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).getList(ArcanePocket.KEY, 10);
    h.assertTrue(saved.size() == 2, "o NBT salvo do jogador tem 2 pilhas no Bolso (tem " + saved.size() + ")");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_pocket_3")
  public static void pocketIsPrivatePerPlayer(GameTestHelper h) {
    var a = player(h, 2.5, 2.5);
    var b = player(h, 4.5, 4.5);
    ArcanePocket.container(a).setItem(0, new ItemStack(Items.EMERALD, 7));
    h.assertTrue(ArcanePocket.container(b).getItem(0).isEmpty(), "o bolso de B nao ve o de A");
    h.assertTrue(ArcanePocket.container(a).getItem(0).getCount() == 7, "o de A segue intacto");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_pocket_4")
  public static void pocketGlyphOpensTheMenu(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 3.5, "self", "pocket");
    var result = cast(h, p, g("self"), g("pocket"));
    h.assertTrue(result.success(), "a conjuracao falhou: " + result.message());
    h.assertTrue(p.containerMenu instanceof ChestMenu, "abriu o menu de bau");
    h.assertTrue(((ChestMenu) p.containerMenu).getContainer() == ArcanePocket.container(p), "o menu mostra o Bolso do proprio jogador");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_pocket_5")
  public static void pocketDoesNotOpenInCombat(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 3.5, "self", "pocket");
    var pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 5));
    p.setLastHurtByMob(pig);
    cast(h, p, g("self"), g("pocket"));
    h.assertTrue(!(p.containerMenu instanceof ChestMenu), "em combate o Bolso nao abre");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_pocket_6")
  public static void pocketAdminClearEmptiesIt(GameTestHelper h) {
    var p = player(h, 3.5, 3.5);
    ArcanePocket.container(p).setItem(3, new ItemStack(Items.IRON_INGOT, 9));
    int n = ArcanePocket.clear(p);
    h.assertTrue(n == 9, "limpar devolveu a contagem: " + n);
    h.assertTrue(ArcanePocket.container(p).getItem(3).isEmpty(), "ficou vazio");
    h.succeed();
  }

  // ------------------------------------------------------------------ Restaurar
  private static net.minecraft.server.level.ServerLevel lvl(GameTestHelper h) { return h.getLevel(); }

  private static int itemsOnGround(GameTestHelper h) {
    return h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(7, 5, 7)))).size();
  }

  @GameTest(template = "empty", batch = "phase5_restore_7")
  public static void blastLeavesNoLootAndRestoreBringsTheBlocksBack(GameTestHelper h) {
    floor(h);
    BlockJournal.clear(lvl(h));
    var pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
    List<BlockPos> ring = new java.util.ArrayList<>();
    for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) {
      if (x == 3 && z == 3) continue;
      BlockPos rel = new BlockPos(x, 1, z);
      h.setBlock(rel, Blocks.DIRT);
      ring.add(rel);
    }
    cast(h, pig, g("self"), g("blast", "radius", 3.0));
    long gone = ring.stream().filter(r -> h.getBlockState(r).isAir()).count();
    h.assertTrue(gone >= 8, "a explosao destruiu blocos (" + gone + ")");
    h.assertTrue(itemsOnGround(h) == 0, "sem loot: nenhum item no chao (" + itemsOnGround(h) + ")");
    h.assertTrue(BlockJournal.size(lvl(h)) >= gone, "o diario registrou os blocos (" + BlockJournal.size(lvl(h)) + ")");
    cast(h, pig, g("self"), g("restore", "radius", 8.0));
    BlockJournal.flush(lvl(h));
    long still = ring.stream().filter(r -> h.getBlockState(r).isAir()).count();
    h.assertTrue(still == 0, "o Restaurar devolveu todos os blocos (faltam " + still + ")");
    h.assertTrue(ring.stream().allMatch(r -> h.getBlockState(r).is(Blocks.DIRT)), "voltaram como terra");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_restore_8")
  public static void restoreNeverOverwritesWhatWasBuiltAfterwards(GameTestHelper h) {
    floor(h);
    BlockJournal.clear(lvl(h));
    BlockPos rel = new BlockPos(2, 1, 2), other = new BlockPos(4, 1, 4);
    h.setBlock(rel, Blocks.DIRT);
    h.setBlock(other, Blocks.DIRT);
    BlockJournal.destroy(lvl(h), h.absolutePos(rel), null);
    BlockJournal.destroy(lvl(h), h.absolutePos(other), null);
    h.setBlock(rel, Blocks.OAK_PLANKS); // alguem construiu ali depois
    int n = BlockJournal.restore(lvl(h), h.absoluteVec(new Vec3(3.5, 1.5, 3.5)), 8, 256);
    BlockJournal.flush(lvl(h));
    h.assertTrue(n == 1, "so um bloco volta (voltaram " + n + ")");
    h.assertTrue(h.getBlockState(rel).is(Blocks.OAK_PLANKS), "a construcao nova foi preservada");
    h.assertTrue(h.getBlockState(other).is(Blocks.DIRT), "o bloco livre voltou");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_restore_9")
  public static void restoreRespectsRadiusAndPerCastCap(GameTestHelper h) {
    floor(h);
    BlockJournal.clear(lvl(h));
    for (int x = 0; x < 6; x++) {
      BlockPos rel = new BlockPos(x, 1, 1);
      h.setBlock(rel, Blocks.DIRT);
      BlockJournal.destroy(lvl(h), h.absolutePos(rel), null);
    }
    var center = h.absoluteVec(new Vec3(0.5, 1.5, 1.5));
    h.assertTrue(BlockJournal.restore(lvl(h), center, 2.2, 256) == 3, "o raio 2,2 alcanca 3 blocos (0, 1 e 2)");
    h.assertTrue(BlockJournal.restore(lvl(h), center, 20, 2) == 2, "o teto por conjuracao limita a 2");
    h.assertTrue(BlockJournal.restore(lvl(h), center, 20, 256) == 1, "sobra 1 depois dos outros");
    h.assertTrue(BlockJournal.restore(lvl(h), center, 20, 256) == 0, "nada mais a restaurar");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_restore_10")
  public static void digIsNotJournaledAndStillDrops(GameTestHelper h) {
    floor(h);
    BlockJournal.clear(lvl(h));
    var pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 2));
    h.setBlock(new BlockPos(3, 1, 4), Blocks.DIRT);
    cast(h, pig, g("ray"), g("dig"));
    h.assertTrue(h.getBlockState(new BlockPos(3, 1, 4)).isAir(), "o Escavar quebrou o bloco");
    h.assertTrue(BlockJournal.size(lvl(h)) == 0, "o Escavar nao entra no diario");
    h.assertTrue(itemsOnGround(h) >= 1, "o Escavar continua soltando item");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_restore_11")
  public static void chestComesBackEmptyAndItsContentsFell(GameTestHelper h) {
    floor(h);
    BlockJournal.clear(lvl(h));
    BlockPos rel = new BlockPos(3, 1, 3);
    h.setBlock(rel, Blocks.CHEST);
    var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) h.getBlockEntity(rel);
    chest.setItem(0, new ItemStack(Items.DIAMOND, 4));
    BlockJournal.destroy(lvl(h), h.absolutePos(rel), null);
    int dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(7, 5, 7))))
        .stream().filter(i -> i.getItem().is(Items.DIAMOND)).mapToInt(i -> i.getItem().getCount()).sum();
    h.assertTrue(dropped == 4, "o conteudo caiu no chao (" + dropped + ")");
    BlockJournal.restore(lvl(h), h.absoluteVec(new Vec3(3.5, 1.5, 3.5)), 4, 256);
    BlockJournal.flush(lvl(h));
    h.assertTrue(h.getBlockState(rel).is(Blocks.CHEST), "o bau voltou");
    var back = (net.minecraft.world.level.block.entity.ChestBlockEntity) h.getBlockEntity(rel);
    h.assertTrue(back.isEmpty(), "o bau voltou vazio");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_restore_12")
  public static void restoreChargesManaPerBlockAndAnyPlayerCanUseIt(GameTestHelper h) {
    floor(h);
    BlockJournal.clear(lvl(h));
    var p = player(h, 3.5, 3.5, "self", "restore");
    for (int x = 0; x < 5; x++) {
      BlockPos rel = new BlockPos(x, 1, 5);
      h.setBlock(rel, Blocks.DIRT);
      BlockJournal.destroy(lvl(h), h.absolutePos(rel), null); // autor nenhum: qualquer um restaura
    }
    var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(p);
    mana.set(100f);
    var result = cast(h, p, g("self"), g("restore", "radius", 8.0));
    h.assertTrue(result.success(), "a conjuracao falhou: " + result.message());
    BlockJournal.flush(lvl(h));
    for (int x = 0; x < 5; x++) h.assertTrue(h.getBlockState(new BlockPos(x, 1, 5)).is(Blocks.DIRT), "bloco " + x + " voltou");
    // custo do glifo + 5 blocos x 0,3
    h.assertTrue(mana.current() < 100f - 5 * 0.3f + 0.01f, "cobrou mana (sobrou " + mana.current() + ")");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_restore_13")
  public static void withoutManaRestoreOnlyBringsBackWhatItCanPay(GameTestHelper h) {
    floor(h);
    BlockJournal.clear(lvl(h));
    var p = player(h, 3.5, 3.5, "self", "restore");
    var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(p);
    // mede o custo do glifo sozinho (diario vazio: nada volta)
    mana.set(100f);
    cast(h, p, g("self"), g("restore", "radius", 8.0));
    float glyphCost = 100f - mana.current();
    for (int x = 0; x < 6; x++) {
      BlockPos rel = new BlockPos(x, 1, 5);
      h.setBlock(rel, Blocks.DIRT);
      BlockJournal.destroy(lvl(h), h.absolutePos(rel), null);
    }
    // so sobra mana para ~2 blocos alem do custo do glifo
    mana.set(glyphCost + 0.7f);
    cast(h, p, g("self"), g("restore", "radius", 8.0));
    BlockJournal.flush(lvl(h));
    long back = 0;
    for (int x = 0; x < 6; x++) if (h.getBlockState(new BlockPos(x, 1, 5)).is(Blocks.DIRT)) back++;
    h.assertTrue(back >= 1 && back < 6, "voltou so parte (" + back + " de 6, custo do glifo " + glyphCost + ")");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100, batch = "phase5_restore_15")
  public static void restoreBringsBlocksBackGraduallyNotAllAtOnce(GameTestHelper h) {
    floor(h);
    BlockJournal.clear(lvl(h));
    int total = 18; // com o padrao de 6 por tick, leva 3 ticks
    for (int x = 0; x < total; x++) {
      BlockPos rel = new BlockPos(x % 7, 1 + x / 7, 1);
      h.setBlock(rel, Blocks.DIRT);
      BlockJournal.destroy(lvl(h), h.absolutePos(rel), null);
    }
    int queued = BlockJournal.restore(lvl(h), h.absoluteVec(new Vec3(3.5, 1.5, 1.5)), 16, 1024);
    h.assertTrue(queued == total, "todos entraram na fila (" + queued + ")");
    h.assertTrue(BlockJournal.pending(lvl(h)) == total, "nada voltou ainda");
    h.runAfterDelay(2, () -> {
      int left = BlockJournal.pending(lvl(h));
      h.assertTrue(left > 0 && left < total, "voltando aos poucos (faltam " + left + ")");
    });
    h.runAfterDelay(20, () -> {
      h.assertTrue(BlockJournal.pending(lvl(h)) == 0, "a fila esvaziou");
      for (int x = 0; x < total; x++) h.assertTrue(h.getBlockState(new BlockPos(x % 7, 1 + x / 7, 1)).is(Blocks.DIRT), "bloco " + x + " voltou");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "phase5_loot_14")
  public static void blastDropsLootConfigKeepsThoseBlocksOutOfTheJournal(GameTestHelper h) {
    floor(h);
    BlockJournal.clear(lvl(h));
    try {
      BlockJournal.overrideLootForTests(true);
      BlockPos rel = new BlockPos(3, 1, 3);
      h.setBlock(rel, Blocks.STONE);
      BlockJournal.destroy(lvl(h), h.absolutePos(rel), null);
      h.assertTrue(BlockJournal.size(lvl(h)) == 0, "com loot ligado, nada entra no diario");
      h.assertTrue(itemsOnGround(h) >= 1, "com loot ligado, o bloco solta item");
    } finally {
      BlockJournal.overrideLootForTests(null);
    }
    h.succeed();
  }

  // ------------------------------------------------------------------ Coro
  private static Inscription chorusSpell(double amount) {
    return new Inscription(List.of(g("self"), g("heal", "amount", amount), g("chorus", "min", 2.0)));
  }

  private static com.arcane.execution.ExecutionResult castSpell(GameTestHelper h, ServerPlayer p, Inscription spell) {
    p.setYRot(0);
    p.setXRot(0);
    return CastingService.tryCast(p, spell, com.arcane.execution.PrecisionSnapshot.of(spell));
  }

  @GameTest(template = "empty", timeoutTicks = 200, batch = "phase5_chorus_15")
  public static void chorusFiresWhenTwoCastInsideTheCircleAndSplitsManaAndBoostsForce(GameTestHelper h) {
    floor(h);
    com.arcane.execution.ChorusService.clearAll();
    var a = player(h, 3.5, 3.5, "self", "heal", "chorus");
    var b = player(h, 5.5, 3.5, "self", "heal", "chorus");
    a.setHealth(2f);
    var ma = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(a);
    var mb = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(b);
    ma.set(100f);
    mb.set(100f);
    var r1 = castSpell(h, a, chorusSpell(2.0));
    h.assertTrue(r1.success(), "o iniciador abriu o Coro: " + r1.message());
    h.assertTrue(a.getHealth() == 2f && ma.current() == 100f, "nada acontece na hora (vida " + a.getHealth() + ", mana " + ma.current() + ")");
    var r2 = castSpell(h, b, chorusSpell(2.0));
    h.assertTrue(r2.success(), "o segundo entrou no Coro: " + r2.message());
    h.runAfterDelay(80, () -> {
      // heal 2 x 1,35 = 2,7: so o Coro pode ter curado mais que 2 (o dobro de vida base nao existe sem ele)
      h.assertTrue(a.getHealth() >= 2f + 2.5f, "a magia disparou, mais forte (vida " + a.getHealth() + ")");
      h.assertTrue(ma.current() < 100f && mb.current() < 100f, "os dois pagaram (" + ma.current() + " e " + mb.current() + ")");
      h.assertTrue(Math.abs(ma.current() - mb.current()) < 0.01f, "a mesma cota para os dois");
      h.assertTrue(com.arcane.execution.ChorusService.activeSessions() == 0, "a sessao fechou");
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 200, batch = "phase5_chorus_16")
  public static void chorusWithOneCasterDoesNothingAndCostsNothing(GameTestHelper h) {
    floor(h);
    com.arcane.execution.ChorusService.clearAll();
    var a = player(h, 3.5, 3.5, "self", "heal", "chorus");
    a.setHealth(2f);
    var ma = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(a);
    ma.set(100f);
    castSpell(h, a, chorusSpell(2.0));
    h.runAfterDelay(80, () -> {
      h.assertTrue(a.getHealth() == 2f, "sem o segundo, a magia nao dispara (vida " + a.getHealth() + ")");
      h.assertTrue(ma.current() == 100f, "e ninguem paga (mana " + ma.current() + ")");
      h.assertTrue(com.arcane.execution.ChorusService.activeSessions() == 0, "a sessao expirou");
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 200, batch = "phase5_chorus_17")
  public static void aPlayerOutsideTheCircleDoesNotJoin(GameTestHelper h) {
    floor(h);
    com.arcane.execution.ChorusService.clearAll();
    var a = player(h, 0.5, 0.5, "self", "heal", "chorus");
    var far = player(h, 3.5, 3.5, "self", "heal", "chorus");
    var pos = h.absoluteVec(new Vec3(0.5, 1, 0.5)).add(30, 0, 0); // bem fora do raio de 8
    far.moveTo(pos.x, pos.y, pos.z);
    a.setHealth(2f);
    castSpell(h, a, chorusSpell(2.0));
    castSpell(h, far, chorusSpell(2.0)); // fora do circulo: abre o proprio Coro, nao entra no de A
    h.assertTrue(com.arcane.execution.ChorusService.activeSessions() == 2, "duas sessoes separadas");
    h.runAfterDelay(80, () -> {
      h.assertTrue(a.getHealth() == 2f, "nenhum Coro se completou (vida " + a.getHealth() + ")");
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 200, batch = "phase5_chorus_18")
  public static void aDifferentSpellDoesNotJoinTheChorus(GameTestHelper h) {
    floor(h);
    com.arcane.execution.ChorusService.clearAll();
    var a = player(h, 3.5, 3.5, "self", "heal", "chorus");
    var b = player(h, 4.5, 3.5, "self", "heal", "chorus");
    a.setHealth(2f);
    castSpell(h, a, chorusSpell(2.0));
    castSpell(h, b, chorusSpell(5.0)); // outra magia equipada
    h.assertTrue(com.arcane.execution.ChorusService.activeSessions() == 2, "cada um abriu o seu");
    h.runAfterDelay(80, () -> {
      h.assertTrue(a.getHealth() == 2f, "o Coro nao se formou com magias diferentes (vida " + a.getHealth() + ")");
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 200, batch = "phase5_chorus_19")
  public static void aLateCasterAfterTheWindowDoesNotJoin(GameTestHelper h) {
    floor(h);
    com.arcane.execution.ChorusService.clearAll();
    var a = player(h, 3.5, 3.5, "self", "heal", "chorus");
    var b = player(h, 4.5, 3.5, "self", "heal", "chorus");
    a.setHealth(2f);
    castSpell(h, a, chorusSpell(2.0));
    h.runAfterDelay(80, () -> {
      castSpell(h, b, chorusSpell(2.0)); // a janela de A ja acabou
      h.assertTrue(com.arcane.execution.ChorusService.activeSessions() == 1, "B abriu o proprio Coro");
      h.assertTrue(a.getHealth() == 2f, "A nao foi curado (vida " + a.getHealth() + ")");
      com.arcane.execution.ChorusService.clearAll();
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 200, batch = "phase5_chorus_20")
  public static void someoneWithoutManaLeavesAndTheChorusFailsBelowTheMinimum(GameTestHelper h) {
    floor(h);
    com.arcane.execution.ChorusService.clearAll();
    var a = player(h, 3.5, 3.5, "self", "heal", "chorus");
    var b = player(h, 4.5, 3.5, "self", "heal", "chorus");
    a.setHealth(2f);
    var ma = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(a);
    var mb = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(b);
    ma.set(100f);
    mb.set(100f);
    castSpell(h, a, chorusSpell(2.0));
    castSpell(h, b, chorusSpell(2.0));
    mb.set(0f); // B gastou a mana antes do disparo
    h.runAfterDelay(80, () -> {
      h.assertTrue(a.getHealth() == 2f, "com B sem mana, abaixo do minimo: nao dispara (vida " + a.getHealth() + ")");
      h.assertTrue(ma.current() == 100f, "e A nao pagou (mana " + ma.current() + ")");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "phase5_chorus_21")
  public static void npcsAndChargedCastsCannotFormAChorus(GameTestHelper h) {
    floor(h);
    var pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
    var r = CastingService.tryCast(pig, chorusSpell(2.0), h.getLevel());
    h.assertTrue(!r.success(), "criatura/NPC nao conjura Coro");
    var a = player(h, 3.5, 3.5, "self", "heal", "chorus");
    var spell = chorusSpell(2.0);
    var charged = CastingService.cast(a, spell, com.arcane.execution.PrecisionSnapshot.of(spell), 1.0, true);
    h.assertTrue(!charged.success(), "magia carregada (mana ja paga) nao entra em Coro");
    h.succeed();
  }

  // ------------------------------------------------------------------ Carregar, Manter e voo (ajustes pos-Fase 5)
  private static Inscription spellInMode(ServerPlayer p, com.arcane.grimoire.SpellPrefs.CastMode mode, GlyphInstance... glyphs) {
    var spell = new Inscription(List.of(glyphs));
    var gr = com.arcane.grimoire.GrimoireAttachments.get(p).orElseThrow();
    gr.addInscription(spell);
    gr.prefs().setMode(spell.id(), mode);
    return spell;
  }

  @GameTest(template = "empty", timeoutTicks = 260, batch = "phase6_charge_1")
  public static void chargeHasNoTimeLimitAndStopsOnlyWhenManaRunsOut(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 3.5, "aura", "damage");
    var spell = spellInMode(p, com.arcane.grimoire.SpellPrefs.CastMode.CHARGE, g("aura", "radius", 1.5), g("damage", "amount", 2.0));
    var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(p);
    mana.set(500f);
    com.arcane.execution.CastChannels.press(p, spell.id());
    h.runAfterDelay(100, () -> {
      double f = com.arcane.execution.CastChannels.chargeFraction(p);
      // o teto antigo (60 ticks) parava em 0,4; sem limite de tempo, aos 100 ticks passa bem disso
      h.assertTrue(f > 0.55, "a carga continua crescendo depois de 3 s (fracao " + f + ")");
      h.assertTrue(com.arcane.execution.CastChannels.isActive(p.getUUID()), "continua carregando, sem disparar sozinha");
      mana.set(0f); // acabou a mana: a carga para onde estava, mas segue valendo ate soltar
    });
    h.runAfterDelay(140, () -> {
      double f = com.arcane.execution.CastChannels.chargeFraction(p);
      h.assertTrue(com.arcane.execution.CastChannels.isActive(p.getUUID()), "sem mana a carga espera (nao dispara sozinha)");
      h.assertTrue(f > 0.55 && f < 1.0, "a carga parou onde estava (fracao " + f + ")");
      com.arcane.execution.CastChannels.release(p);
      h.assertTrue(!com.arcane.execution.CastChannels.isActive(p.getUUID()), "soltar encerra o canal");
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 200, batch = "phase6_hold_2")
  public static void holdCastsOnceThenOnlyDrainsSlowly(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 3.5, "aura", "damage");
    var pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 4));
    var spell = spellInMode(p, com.arcane.grimoire.SpellPrefs.CastMode.HOLD, g("aura", "radius", 6.0), g("damage", "amount", 0.5)); // raio grande: o empurrao do golpe nao tira o alvo do alcance
    var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(p);
    mana.set(500f);
    float before = mana.current(); // a reserva maxima pode ser menor que 500
    int cost = CastingService.manaCostOf(spell, com.arcane.execution.PrecisionSnapshot.of(spell));
    com.arcane.execution.CastChannels.press(p, spell.id());
    float afterCast = mana.current();
    h.assertTrue(Math.abs((before - afterCast) - cost) < 0.5f, "conjurou uma vez, pelo custo normal (" + (before - afterCast) + " de " + cost + ")");
    float hp = pig.getHealth();
    h.assertTrue(hp < 10f, "o primeiro efeito atingiu o porco (vida " + hp + ")");
    var trace = new StringBuilder();
    for (int t : new int[] {5, 11, 12, 21, 22, 40}) {
      h.runAfterDelay(t, () -> trace.append(t).append(":vida=").append(pig.getHealth()).append(",inv=").append(pig.invulnerableTime)
          .append(",dist=").append(String.format("%.2f", pig.position().distanceTo(p.position()))).append(",mana=").append(mana.current()).append(" | "));
    }
    h.runAfterDelay(60, () -> {
      h.assertTrue(pig.getHealth() < hp, "TRACE " + trace);
      float extra = afterCast - mana.current();
      // 3 s segurando a 0,6 x custo por segundo = 1,8 x custo (menos a regeneracao); nada parecido com 6 conjuracoes de custo cheio
      h.assertTrue(extra > 0 && extra < cost * 2.5f, "manter gasta aos poucos (" + extra + " com custo " + cost + ")");
      h.assertTrue(pig.getHealth() < hp, "o efeito continuou agindo enquanto segurava (vida " + pig.getHealth() + " de " + hp + ", canal ativo " + com.arcane.execution.CastChannels.isActive(p.getUUID()) + ")");
      com.arcane.execution.CastChannels.release(p);
      h.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 120, batch = "phase6_flight_3")
  public static void flightDrainsManaEvenInCreativeAndSprintCostsMore(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 3.5);
    h.onEachTick(p::doTick); // o servidor ticka o jogador pela conexao: efeitos e custo por tick
    p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
    p.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get(), 400,
        com.arcane.execution.FlightService.encode(1.0, 0.0), false, false, true));
    var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(p);
    mana.set(60f);
    float[] marks = new float[3];
    marks[0] = mana.current();
    h.runAfterDelay(30, () -> {
      mana.set(mana.current());
      marks[1] = mana.current();
      h.assertTrue(marks[0] - marks[1] > 1.0f, "no criativo o voo tambem cobra mana (" + (marks[0] - marks[1]) + ")");
      p.setSprinting(true);
    });
    h.runAfterDelay(60, () -> {
      marks[2] = mana.current();
      float base = marks[0] - marks[1], fast = marks[1] - marks[2];
      h.assertTrue(fast > base * 1.8f, "o voo rapido (sprint) gasta bem mais (" + fast + " contra " + base + ")");
      p.removeEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get());
      h.succeed();
    });
  }

  // ------------------------------------------------------------------ Foco (forma de mira viva)
  @GameTest(template = "empty", batch = "phase7_focus_1")
  public static void focusDeliversOnlyTheLivingCreatureUnderAimAndFailsWithoutSpendingMana(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 1.5, "focus", "damage");
    var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(p);
    mana.set(80f);
    var pig = h.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(3, 1, 4));
    var spell = new Inscription(List.of(g("focus", "range", 12.0), g("damage", "amount", 2.0)));
    whenSearchable(h, pig, 40, () -> {
    float hp = pig.getHealth();
    float m0 = mana.current();
    var ok = castSpell(h, p, spell);
    if (!ok.success()) {
      var clip = h.getLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(12)),
          net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
      h.fail("com criatura na mira a magia funciona: " + ok.message() + " olho=" + p.getEyePosition() + " look=" + p.getLookAngle()
          + " clip=" + clip.getType() + "@" + clip.getBlockPos() + " " + h.getLevel().getBlockState(clip.getBlockPos()) + " alvo=" + pig.position()
          + " vivo=" + pig.isAlive() + " pick=" + pig.isPickable() + " bb=" + pig.getBoundingBox());
    }
    h.assertTrue(pig.getHealth() < hp, "o Foco entregou a criatura ao efeito");
    h.assertTrue(mana.current() < m0, "gastou mana ao funcionar");
    pig.discard();
    float m1 = mana.current();
    var fail = castSpell(h, p, spell);
    h.assertTrue(!fail.success() && fail.message().equals("message.arcane.focus.no_target"), "sem criatura: falha (" + fail.message() + ")");
    h.assertTrue(mana.current() == m1, "e nao gasta mana (" + mana.current() + " de " + m1 + ")");
    h.succeed();
    });
  }

  /**
   * Causa do Foco instavel: a busca de entidades (level.getEntities, usada pelo ProjectileUtil) so olha secoes de
   * entidade ja acessiveis. O GameTest monta a estrutura longe (x de centenas) e a secao recem-carregada pode levar alguns
   * ticks para ficar acessivel, conforme a ordem dos testes; no mesmo tick do spawn a criatura nao existe para a busca.
   * Em jogo os chunks em volta do jogador ja estao carregados. Espera a criatura aparecer na busca (no maximo maxTicks).
   */
  private static void whenSearchable(GameTestHelper h, net.minecraft.world.entity.Entity e, int maxTicks, Runnable body) {
    boolean seen = h.getLevel().getEntities((net.minecraft.world.entity.Entity) null, e.getBoundingBox()).contains(e);
    if (seen) body.run();
    else if (maxTicks <= 0) h.fail("a criatura nunca apareceu na busca de entidades");
    else h.runAfterDelay(1, () -> whenSearchable(h, e, maxTicks - 1, body));
  }

  @GameTest(template = "empty", batch = "phase7_focus_2")
  public static void focusDoesNotSeeThroughWallsOrPickBlocks(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 1.5, "focus", "damage");
    var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(p);
    mana.set(80f);
    h.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
    h.setBlock(new BlockPos(3, 2, 3), Blocks.STONE);
    h.setBlock(new BlockPos(3, 3, 3), Blocks.STONE);
    var pig = h.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(3, 1, 5));
    var spell = new Inscription(List.of(g("focus", "range", 12.0), g("damage", "amount", 2.0)));
    whenSearchable(h, pig, 40, () -> { // sem isso o teste passava sem a criatura existir para a busca
      float hp = pig.getHealth();
      float m0 = mana.current();
      var r = castSpell(h, p, spell);
      h.assertTrue(!r.success(), "criatura atras de parede nao e alvo");
      h.assertTrue(pig.getHealth() == hp && mana.current() == m0, "nada acontece e nada e cobrado");
      h.succeed();
    });
  }

  // ------------------------------------------------------------------ Desempenho do Restaurar
  @GameTest(template = "empty", timeoutTicks = 200, batch = "phase7_perf_3")
  public static void restoreWithAFullJournalStaysCheapPerTick(GameTestHelper h) {
    floor(h);
    BlockJournal.clear(lvl(h));
    int cap = com.arcane.config.ArcaneConfig.RESTORE_JOURNAL_CAP.get();
    // enche o diario ate o teto com blocos distantes entre si (um andar de terra de 64x64 sobre o chao do teste)
    var origin = h.absolutePos(new BlockPos(0, 2, 0));
    for (int i = 0; i < cap; i++) {
      BlockPos at = origin.offset(i % 64, i / 64 % 64, 0);
      lvl(h).setBlock(at, Blocks.DIRT.defaultBlockState(), 2);
      BlockJournal.destroy(lvl(h), at, null);
    }
    long t0 = System.nanoTime();
    int queued = BlockJournal.restore(lvl(h), Vec3.atCenterOf(origin).add(32, 32, 0), 16, 1024);
    double selectMs = (System.nanoTime() - t0) / 1.0e6;
    h.assertTrue(queued > 0, "algo entrou na fila (" + queued + ")");
    h.assertTrue(selectMs < 100.0, "escolher os blocos num diario cheio custa pouco (" + selectMs + " ms)");
    long worst = 0;
    int ticks = 0;
    while (BlockJournal.pending(lvl(h)) > 0 && ticks++ < 400) {
      long a = System.nanoTime();
      BlockJournal.flushSome(lvl(h), com.arcane.config.ArcaneConfig.RESTORE_BLOCKS_PER_TICK.get());
      worst = Math.max(worst, System.nanoTime() - a);
    }
    h.assertTrue(worst / 1.0e6 < 25.0, "cada tick de recolocacao custa pouco (pior " + worst / 1.0e6 + " ms)");
    BlockJournal.clear(lvl(h));
    h.succeed();
  }

  // ------------------------------------------------------------------ Coluna: ate o ceu e so com chao
  @GameTest(template = "empty", batch = "phase8_pillar_1")
  public static void pillarReachesTheSkyAndStrikesCreaturesFarAbove(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 3.5, "pillar", "damage", "origin_self");
    var high = h.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(3, 1, 3));
    var at = h.absoluteVec(new Vec3(3.5, 40.0, 3.5));
    high.setNoGravity(true);
    high.moveTo(at.x, at.y, at.z);
    float hp = high.getHealth();
    var r = castSpell(h, p, new Inscription(List.of(g("pillar", "radius", 2.0), g("origin_self"), g("damage", "amount", 3.0))));
    h.assertTrue(r.success(), "a coluna funciona com chao: " + r.message());
    h.assertTrue(high.getHealth() < hp, "a coluna alcanca uma criatura a 40 blocos de altura (vida " + high.getHealth() + ")");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase8_pillar_2")
  public static void pillarDoesNotManifestInMidAirAndCostsNothing(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 3.5, "pillar", "damage");
    var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(p);
    mana.set(80f);
    float m0 = mana.current();
    p.setYRot(0);
    p.setXRot(-90); // mirando o ceu: o ponto mirado fica no ar
    var spell = new Inscription(List.of(g("pillar", "radius", 2.0), g("damage", "amount", 3.0)));
    var r = CastingService.tryCast(p, spell, com.arcane.execution.PrecisionSnapshot.of(spell));
    h.assertTrue(!r.success() && r.message().equals("message.arcane.pillar.needs_ground"), "no ar a coluna falha (" + r.message() + ")");
    h.assertTrue(mana.current() == m0, "e nao cobra mana (" + mana.current() + ")");
    h.succeed();
  }
}
