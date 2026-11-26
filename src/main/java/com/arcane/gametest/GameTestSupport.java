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
 * Utilitarios compartilhados pelos GameTests de area (antes dentro de ArcaneGameTests, dividido em 25 nov). As classes
 * de area estendem esta, entao os metodos estaticos continuam sendo chamados sem prefixo.
 */
public class GameTestSupport {
  protected GameTestSupport() {}

  static GlyphInstance g(String id) {
    return new GlyphInstance(new ResourceLocation("arcane", id));
  }

  static GlyphInstance g(String id, String key, double value) {
    return new GlyphInstance(new ResourceLocation("arcane", id), Map.of(key, value));
  }

  static GlyphInstance g(String id, String k1, double v1, String k2, double v2) {
    return new GlyphInstance(new ResourceLocation("arcane", id), Map.of(k1, v1, k2, v2));
  }

  static void floor(GameTestHelper h) {
    for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
  }

  static net.minecraft.server.level.ServerPlayer casterWith(GameTestHelper h) {
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

  static Inscription addSpell(net.minecraft.server.level.ServerPlayer player, com.arcane.grimoire.SpellPrefs.CastMode mode, GlyphInstance... glyphs) {
    var spell = new Inscription(List.of(glyphs));
    var grimoire = com.arcane.grimoire.GrimoireAttachments.get(player).orElseThrow();
    grimoire.addInscription(spell);
    grimoire.prefs().setMode(spell.id(), mode);
    return spell;
  }

  static Pig pig(GameTestHelper h, int x, int z) {
    Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(x, 1, z));
    pig.setYRot(0);
    pig.setXRot(0);
    return pig;
  }

  static void cast(GameTestHelper h, LivingEntity caster, GlyphInstance... glyphs) {
    caster.setYRot(0);
    caster.setXRot(0);
    var result = CastingService.tryCast(caster, new Inscription(List.of(glyphs)), h.getLevel());
    h.assertTrue(result.success(), "a conjuracao falhou: " + result.message());
  }

  static void assertHurt(GameTestHelper h, LivingEntity e, float before, String what) {
    h.assertTrue(e.getHealth() < before, what + " deveria ter sido ferido (vida " + e.getHealth() + ")");
  }

  static void assertUnhurt(GameTestHelper h, LivingEntity e, float before, String what) {
    h.assertTrue(e.getHealth() >= before, what + " nao deveria ter sido ferido (vida " + e.getHealth() + ")");
  }

  // ================================================================== RITUAIS (arena 35x35)
  static final BlockPos ALTAR = new BlockPos(17, 1, 17);

  static void arena(GameTestHelper h) {
    for (int x = 0; x < 35; x++) for (int z = 0; z < 35; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    h.setBlock(ALTAR, com.arcane.registry.ModBlocks.ARCANE_ALTAR.get());
  }

  static BlockPos altarAbs(GameTestHelper h) {
    return h.absolutePos(ALTAR);
  }

  static Pig pigAt(GameTestHelper h, int x, int z) {
    Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(x, 1, z));
    pig.setYRot(0);
    pig.setXRot(0);
    return pig;
  }

  static com.arcane.ritual.ComponentSource stock(Object... itemsAndCounts) {
    Map<net.minecraft.world.item.Item, Integer> map = new java.util.HashMap<>();
    for (int i = 0; i < itemsAndCounts.length; i += 2) {
      map.put((net.minecraft.world.item.Item) itemsAndCounts[i], (Integer) itemsAndCounts[i + 1]);
    }
    return com.arcane.ritual.ComponentSource.of(map);
  }

  static com.arcane.ritual.RitualService.Result ritual(GameTestHelper h, LivingEntity performer,
                                                               com.arcane.ritual.ComponentSource source, boolean legendary, GlyphInstance... glyphs) {
    return com.arcane.ritual.RitualService.perform(h.getLevel(), altarAbs(h), performer, new Inscription(List.of(glyphs)), source, legendary);
  }

  static GlyphInstance oath(int kind, double seconds) {
    return g("vow_oath").withParameter("kind", kind).withParameter("seconds", seconds);
  }

  static int countBarrier(GameTestHelper h, int r) {
    int n = 0;
    for (int x = -r; x <= r; x++) for (int y = -1; y <= r + 1; y++) for (int z = -r; z <= r; z++) {
      if (h.getLevel().getBlockState(h.absolutePos(new BlockPos(7 + x, 1 + y, 7 + z))).is(com.arcane.registry.ModBlocks.ARCANE_BARRIER.get())) n++;
    }
    return n;
  }

  static int traceCount(GameTestHelper h, int radius) {
    int n = 0;
    for (var cell : com.arcane.ritual.RitualTemplate.cells(radius)) {
      if (com.arcane.ritual.RitualService.traceAt(h.getLevel(), altarAbs(h), cell) != null) n++;
    }
    return n;
  }

  static InscriptionAnchor drawing(BlockPos abs, String ink, long created, net.minecraft.core.Direction face) {
    ResourceLocation inkId = new ResourceLocation("arcane", ink);
    Inscription spell = new Inscription(List.of(g("self"), g("damage", "amount", 1.0)), new com.arcane.inscription.InscriptionMetadata(
        "", inkId, new ResourceLocation("arcane", "arcane_pen"), 1.0, java.util.Optional.empty(), created,
        com.arcane.inscription.InscriptionSourceKind.WORLD));
    return new InscriptionAnchor(abs, spell, InscribingService.TRIGGER_ON_SIGNAL, inkId, face);
  }

  static Inscription objectSpell(String ink, long created, boolean persistent) {
    ResourceLocation inkId = new ResourceLocation("arcane", ink);
    List<GlyphInstance> glyphs = persistent ? List.of(g("self"), g("persistence"), g("shield")) : List.of(g("self"), g("shield"));
    return new Inscription(glyphs, new com.arcane.inscription.InscriptionMetadata("", inkId, new ResourceLocation("arcane", "arcane_pen"),
        1.0, java.util.Optional.empty(), created, com.arcane.inscription.InscriptionSourceKind.ITEM));
  }

  // ================================================================== Revisao: toggle, area, custo, ritual sem forma
  static net.minecraft.server.level.ServerPlayer learn(net.minecraft.server.level.ServerPlayer player, String... ids) {
    var grimoire = com.arcane.grimoire.GrimoireAttachments.get(player).orElseThrow();
    for (String id : ids) grimoire.learnGlyph(new ResourceLocation("arcane", id), com.arcane.grimoire.GlyphKnowledge.MASTERED);
    return player;
  }

  static long countWarnings(List<net.minecraft.network.chat.Component> lines) {
    return lines.stream().filter(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
        && (t.getKey().startsWith("gui.arcane.preview.warn."))).count();
  }

  static List<String> glyphsOf(com.arcane.glyph.GlyphCategory category, boolean includeRitual) {
    return com.arcane.glyph.GlyphRegistry.all().values().stream()
        .filter(d -> d.category() == category && (includeRitual || d.ritual() == 0))
        .map(d -> d.id().getPath()).sorted().toList();
  }

  /** Conjura e devolve um texto de falha (ou null). Falhar por mana/ritual e legitimo; excecao ou invalidez nao. */
  static String tryCombo(GameTestHelper h, GlyphInstance... glyphs) {
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
      // falhas de forma sem alvo (Mais Proximo sem ninguem, Raio no ar, Coluna sem chao, Foco sem criatura) sao regra, nao defeito
      boolean formMiss = result.message().contains("seek.no_target") || result.message().contains("ray.miss")
          || result.message().contains("pillar.needs_ground") || result.message().contains("focus.no_target");
      if (!result.success() && !(result.message().contains("requires_ritual") || result.message().contains("mana") || formMiss)) {
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

  static void add(List<String> failures, String failure) {
    if (failure != null) failures.add(failure);
  }

  static void checkCrystalWidensReach(GameTestHelper h) {
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

}
