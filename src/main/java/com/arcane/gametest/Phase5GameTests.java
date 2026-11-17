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
    h.getLevel().addNewPlayer(p);
    return p;
  }

  static com.arcane.execution.ExecutionResult cast(GameTestHelper h, net.minecraft.world.entity.LivingEntity caster, GlyphInstance... glyphs) {
    caster.setYRot(0);
    caster.setXRot(0);
    return CastingService.tryCast(caster, new Inscription(List.of(glyphs)), h.getLevel());
  }

  // ------------------------------------------------------------------ Bolso Arcano
  @GameTest(template = "empty", batch = "phase5_pocket")
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

  @GameTest(template = "empty", batch = "phase5_pocket")
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

  @GameTest(template = "empty", batch = "phase5_pocket")
  public static void pocketIsPrivatePerPlayer(GameTestHelper h) {
    var a = player(h, 2.5, 2.5);
    var b = player(h, 4.5, 4.5);
    ArcanePocket.container(a).setItem(0, new ItemStack(Items.EMERALD, 7));
    h.assertTrue(ArcanePocket.container(b).getItem(0).isEmpty(), "o bolso de B nao ve o de A");
    h.assertTrue(ArcanePocket.container(a).getItem(0).getCount() == 7, "o de A segue intacto");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_pocket")
  public static void pocketGlyphOpensTheMenu(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 3.5, "self", "pocket");
    var result = cast(h, p, g("self"), g("pocket"));
    h.assertTrue(result.success(), "a conjuracao falhou: " + result.message());
    h.assertTrue(p.containerMenu instanceof ChestMenu, "abriu o menu de bau");
    h.assertTrue(((ChestMenu) p.containerMenu).getContainer() == ArcanePocket.container(p), "o menu mostra o Bolso do proprio jogador");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_pocket")
  public static void pocketDoesNotOpenInCombat(GameTestHelper h) {
    floor(h);
    var p = player(h, 3.5, 3.5, "self", "pocket");
    var pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 5));
    p.setLastHurtByMob(pig);
    cast(h, p, g("self"), g("pocket"));
    h.assertTrue(!(p.containerMenu instanceof ChestMenu), "em combate o Bolso nao abre");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_pocket")
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

  @GameTest(template = "empty", batch = "phase5_restore")
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
    long still = ring.stream().filter(r -> h.getBlockState(r).isAir()).count();
    h.assertTrue(still == 0, "o Restaurar devolveu todos os blocos (faltam " + still + ")");
    h.assertTrue(ring.stream().allMatch(r -> h.getBlockState(r).is(Blocks.DIRT)), "voltaram como terra");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_restore")
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
    h.assertTrue(n == 1, "so um bloco volta (voltaram " + n + ")");
    h.assertTrue(h.getBlockState(rel).is(Blocks.OAK_PLANKS), "a construcao nova foi preservada");
    h.assertTrue(h.getBlockState(other).is(Blocks.DIRT), "o bloco livre voltou");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_restore")
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

  @GameTest(template = "empty", batch = "phase5_restore")
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

  @GameTest(template = "empty", batch = "phase5_restore")
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
    h.assertTrue(h.getBlockState(rel).is(Blocks.CHEST), "o bau voltou");
    var back = (net.minecraft.world.level.block.entity.ChestBlockEntity) h.getBlockEntity(rel);
    h.assertTrue(back.isEmpty(), "o bau voltou vazio");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_restore")
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
    for (int x = 0; x < 5; x++) h.assertTrue(h.getBlockState(new BlockPos(x, 1, 5)).is(Blocks.DIRT), "bloco " + x + " voltou");
    // custo do glifo + 5 blocos x 0,3
    h.assertTrue(mana.current() < 100f - 5 * 0.3f + 0.01f, "cobrou mana (sobrou " + mana.current() + ")");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_restore")
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
    long back = 0;
    for (int x = 0; x < 6; x++) if (h.getBlockState(new BlockPos(x, 1, 5)).is(Blocks.DIRT)) back++;
    h.assertTrue(back >= 1 && back < 6, "voltou so parte (" + back + " de 6, custo do glifo " + glyphCost + ")");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "phase5_loot")
  public static void blastDropsLootConfigKeepsThoseBlocksOutOfTheJournal(GameTestHelper h) {
    floor(h);
    BlockJournal.clear(lvl(h));
    var cfg = com.arcane.config.ArcaneConfig.BLAST_DROPS_LOOT;
    boolean old = cfg.get();
    try {
      cfg.set(true);
      BlockPos rel = new BlockPos(3, 1, 3);
      h.setBlock(rel, Blocks.STONE);
      BlockJournal.destroy(lvl(h), h.absolutePos(rel), null);
      h.assertTrue(BlockJournal.size(lvl(h)) == 0, "com loot ligado, nada entra no diario");
      h.assertTrue(itemsOnGround(h) >= 1, "com loot ligado, o bloco solta item");
    } finally {
      cfg.set(old);
    }
    h.succeed();
  }
}
