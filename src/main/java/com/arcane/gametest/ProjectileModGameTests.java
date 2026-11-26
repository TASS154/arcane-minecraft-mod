package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.entity.ArcaneProjectileEntity;
import com.arcane.execution.ProjectileMods;
import com.arcane.inscription.CompiledPlanCache;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Modificadores de Projetil (Fase J): Perseguir, Ricochete, Fragmentar, Arco, orcamento e aviso fora da forma. */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class ProjectileModGameTests extends GameTestSupport {

  static List<ArcaneProjectileEntity> projectiles(GameTestHelper h) {
    return h.getLevel().getEntitiesOfClass(ArcaneProjectileEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(40));
  }

  /** Projetil montado a mao (para escolher os limites), na altura dos olhos de {@code owner}. */
  static ArcaneProjectileEntity manual(GameTestHelper h, ServerPlayer owner, ProjectileMods.Mods mods, ProjectileMods.Limits limits,
                                       Vec3 velocity, GlyphInstance... glyphs) {
    var inscription = new Inscription(List.of(glyphs));
    var p = new ArcaneProjectileEntity(ModEntities.ARCANE_PROJECTILE.get(), h.getLevel());
    p.setOwner(owner);
    p.setPos(owner.getX(), owner.getEyeY(), owner.getZ());
    p.setInscription(inscription);
    p.setPlan(CompiledPlanCache.getOrCompile(inscription));
    p.setMods(mods, limits);
    p.setDeltaMovement(velocity);
    return p;
  }

  /** Perseguir: um orbe que erraria em linha reta curva ate o porco a 2 blocos do lado. */
  @GameTest(template = "empty", batch = "projmod_1", timeoutTicks = 80)
  public static void homingCurvesIntoAnOffAxisTarget(GameTestHelper h) {
    floor(h);
    var p = Phase5GameTests.player(h, 3.5, 0.5, "projectile", "homing", "damage");
    var pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(5, 1, 5));
    float hp = pig.getHealth();
    var r = Phase5GameTests.cast(h, p, g("projectile", "speed", 0.6), g("homing", "turn", 3), g("damage")); // lento: tempo de curvar 25 graus
    h.assertTrue(r.success(), "conjurou: " + r.message());
    h.succeedWhen(() -> h.assertTrue(pig.getHealth() < hp, "o orbe curvou e acertou o porco fora da linha"));
  }

  /** Sem filtro, nunca persegue bicho de estimacao do dono; com filtro (Hostis), so quem passa nele. */
  @GameTest(template = "empty", batch = "projmod_1")
  public static void homingRespectsFilterAndNeverChasesPets(GameTestHelper h) {
    floor(h);
    var p = Phase5GameTests.player(h, 3.5, 0.5);
    var wolf = h.spawnWithNoFreeWill(EntityType.WOLF, new BlockPos(3, 1, 3));
    wolf.tame(p);
    var pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 5));
    var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 1, 6));
    var forward = new Vec3(0, -0.15, 1).normalize();
    var free = manual(h, p, new ProjectileMods.Mods(2, 0, 0, 0), ProjectileMods.Limits.DEFAULT, forward, g("projectile"), g("damage"));
    var target = free.findHomingTarget(forward);
    h.assertTrue(target == pig, "sem filtro: pula o lobo domesticado e vai no porco (" + target + ")");
    var hostile = manual(h, p, new ProjectileMods.Mods(2, 0, 0, 0), ProjectileMods.Limits.DEFAULT, forward, g("projectile"), g("hostile"), g("damage"));
    var t2 = hostile.findHomingTarget(forward);
    h.assertTrue(t2 == zombie, "filtro Hostis: so o zumbi, mesmo mais longe (" + t2 + ")");
    h.assertTrue(free.findHomingTarget(new Vec3(0, 0, -1)) == null, "nada no cone para tras");
    h.succeed();
  }

  /** Ricochete: quica no chao e no teto, cada quique roda a magia uma vez e perde forca. */
  @GameTest(template = "empty", batch = "projmod_2", timeoutTicks = 80)
  public static void ricochetBouncesAndEachBounceIsOneHit(GameTestHelper h) {
    floor(h);
    for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) h.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
    var p = Phase5GameTests.player(h, 3.5, 0.5);
    var dir = new Vec3(0, -Math.sin(Math.toRadians(60)), Math.cos(Math.toRadians(60))).scale(1.2);
    var proj = manual(h, p, new ProjectileMods.Mods(0, 2, 0, 0), ProjectileMods.Limits.DEFAULT, dir, g("projectile"), g("damage"));
    h.getLevel().addFreshEntity(proj);
    h.succeedWhen(() -> {
      h.assertTrue(proj.isRemoved(), "ainda voando (quiques " + proj.bounces() + ")");
      h.assertTrue(proj.bounces() == 2, "quicou 2 vezes (" + proj.bounces() + ")");
      h.assertTrue(proj.triggers() == 3, "2 quiques + o acerto final = 3 disparos (" + proj.triggers() + ")");
      h.assertTrue(Math.abs(proj.powerScale() - 0.5625) < 1e-6, "perde 25% por quique (" + proj.powerScale() + ")");
    });
  }

  /** Fragmentar: o acerto vira N cacos fracos, que nao fragmentam de novo. */
  @GameTest(template = "empty", batch = "projmod_3", timeoutTicks = 60)
  public static void fragmentSplitsIntoWeakShardsOnce(GameTestHelper h) {
    floor(h);
    for (int x = 0; x < 7; x++) for (int y = 1; y < 5; y++) h.setBlock(new BlockPos(x, y, 5), Blocks.STONE);
    var p = Phase5GameTests.player(h, 3.5, 0.5);
    var proj = manual(h, p, new ProjectileMods.Mods(0, 0, 3, 0), ProjectileMods.Limits.DEFAULT, new Vec3(0, 0, 1), g("projectile"), g("damage"));
    h.getLevel().addFreshEntity(proj);
    h.succeedWhen(() -> {
      var shards = projectiles(h).stream().filter(ArcaneProjectileEntity::isShard).toList();
      h.assertTrue(proj.isRemoved(), "o orbe ainda nao bateu");
      h.assertTrue(shards.size() == 3, "3 cacos (" + shards.size() + ")");
      double sum = 0;
      for (var s : shards) {
        h.assertTrue(s.mods().fragment() == 0 && s.mods().ricochet() == 0, "caco nao fragmenta nem ricocheteia");
        h.assertTrue(s.powerScale() <= 0.5 + 1e-9, "caco fraco (" + s.powerScale() + ")");
        sum += s.powerScale();
      }
      h.assertTrue(sum <= ProjectileMods.Limits.DEFAULT.fragmentTotalPower() + 1e-9, "soma abaixo do teto (" + sum + ")");
    });
  }

  /** Teto de cacos por mundo: alem dele, nenhum caco novo nasce (sem erro). */
  @GameTest(template = "empty", batch = "projmod_4", timeoutTicks = 60)
  public static void fragmentRespectsTheWorldCap(GameTestHelper h) {
    floor(h);
    for (int x = 0; x < 7; x++) for (int y = 1; y < 5; y++) h.setBlock(new BlockPos(x, y, 5), Blocks.STONE);
    var p = Phase5GameTests.player(h, 3.5, 0.5);
    int alive = ArcaneProjectileEntity.shardsAlive(h.getLevel());
    var d = ProjectileMods.Limits.DEFAULT;
    var limits = new ProjectileMods.Limits(d.maxTriggers(), d.homingDegreesPerLevel(), d.homingConeDegrees(), d.homingRange(),
        d.ricochetPowerLoss(), d.fragmentTotalPower(), d.fragmentMaxEach(), alive + 1);
    var proj = manual(h, p, new ProjectileMods.Mods(0, 0, 6, 0), limits, new Vec3(0, 0, 1), g("projectile"), g("damage"));
    h.getLevel().addFreshEntity(proj);
    h.succeedWhen(() -> {
      h.assertTrue(proj.isRemoved(), "o orbe ainda nao bateu");
      long shards = projectiles(h).stream().filter(ArcaneProjectileEntity::isShard).count();
      h.assertTrue(shards == 1, "so 1 caco cabia no mundo (" + shards + ")");
    });
  }

  /** Os quatro juntos no maximo: a conjuracao degrada para caber no teto, sem erro. */
  @GameTest(template = "empty", batch = "projmod_2")
  public static void allFourTogetherStayInsideTheBudget(GameTestHelper h) {
    floor(h);
    var p = Phase5GameTests.player(h, 3.5, 0.5, "projectile", "homing", "ricochet", "fragment", "arc", "damage");
    var before = projectiles(h).size();
    var r = Phase5GameTests.cast(h, p, g("projectile"), g("homing", "turn", 3), g("ricochet", "bounces", 4),
        g("fragment", "count", 6), g("arc", "height", 12), g("damage"));
    h.assertTrue(r.success(), "conjurou: " + r.message());
    var fresh = projectiles(h).stream().filter(e -> e.getOwner() == p).toList();
    h.assertTrue(fresh.size() == 1 && projectiles(h).size() == before + 1, "um orbe (" + fresh.size() + ")");
    var m = fresh.get(0).mods();
    h.assertTrue(m.triggers() <= ProjectileMods.limits().maxTriggers(), "dentro do teto de disparos: " + m);
    h.assertTrue(m.homing() == 3 && m.arc() == 12.0, "Perseguir e Arco nao custam disparos: " + m);
    h.assertTrue(m.fragment() < 6, "cacos cortados primeiro: " + m);
    h.assertTrue(fresh.get(0).getDeltaMovement().y > 0, "Arco: sai subindo");
    fresh.get(0).discard();
    h.succeed();
  }

  /** Fora da forma Projetil os modificadores nao fazem nada, e a previa avisa. */
  @GameTest(template = "empty", batch = "projmod_1")
  public static void previewWarnsOnOtherForms(GameTestHelper h) {
    var ray = com.arcane.execution.SpellPreview.describe(new Inscription(List.of(g("ray"), g("homing"), g("damage"))));
    h.assertTrue(ray.stream().anyMatch(c -> c.getStyle().getColor() != null
            && c.getStyle().getColor().getValue() == ChatFormatting.GOLD.getColor()
            && c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
            && t.getKey().equals("gui.arcane.preview.warn.projectile_mods_ignored")),
        "aviso dourado na forma Raio: " + ray);
    var proj = com.arcane.execution.SpellPreview.describe(new Inscription(List.of(g("projectile"), g("homing"), g("damage"))));
    h.assertTrue(proj.stream().noneMatch(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
        && t.getKey().equals("gui.arcane.preview.warn.projectile_mods_ignored")), "na forma Projetil, sem aviso");
    h.succeed();
  }
}
