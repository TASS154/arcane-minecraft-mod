package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.execution.CastingService;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.pocket.ArcanePocket;
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
}
