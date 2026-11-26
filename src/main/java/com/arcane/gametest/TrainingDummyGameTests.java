package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.entity.ArcaneDummyEntity;
import com.arcane.execution.Pierce;
import com.arcane.registry.ModEntities;
import com.arcane.training.DamageMeter;
import com.arcane.training.DummyReport;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Boneco de Treino Arcano (Fase H): acumula, nunca morre, reseta em 5 s, armadura conta, relatorio bate. */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class TrainingDummyGameTests extends GameTestSupport {

  static ArcaneDummyEntity dummy(GameTestHelper h, int x, int z) {
    return h.spawn(ModEntities.TRAINING_DUMMY.get(), new BlockPos(x, 1, z));
  }

  static void hit(ArcaneDummyEntity d, DamageSource source, float amount) {
    d.invulnerableTime = 0;
    d.hurt(source, amount);
  }

  @GameTest(template = "empty", batch = "dummy_1")
  public static void damageAccumulatesAndTheDummyNeverDies(GameTestHelper h) {
    floor(h);
    var d = dummy(h, 3, 3);
    var magic = h.getLevel().damageSources().magic();
    hit(d, magic, 6f);
    hit(d, magic, 50f);
    h.runAfterDelay(2, () -> {
      h.assertTrue(d.isAlive(), "o boneco nao morre");
      h.assertTrue(d.getHealth() >= 1f, "a vida para em 1 HP (" + d.getHealth() + ")");
      h.assertTrue(Math.abs(d.meter().total() - 56f) < 0.01f, "o medidor soma o dano inteiro, inclusive o que passa da vida (" + d.meter().total() + ")");
      h.assertTrue(d.meter().count() == 2 && Math.abs(d.meter().lastHit() - 50f) < 0.01f, "dois golpes, o ultimo de 50");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "dummy_1", timeoutTicks = 200)
  public static void healthResetsAfterFiveSecondsWithoutDamage(GameTestHelper h) {
    floor(h);
    var d = dummy(h, 3, 3);
    hit(d, h.getLevel().damageSources().magic(), 10f);
    h.runAfterDelay(ArcaneDummyEntity.RESET_TICKS - 20, () -> {
      h.assertTrue(d.getHealth() < d.getMaxHealth(), "antes dos 5 s continua ferido");
      h.runAfterDelay(25, () -> {
        h.assertTrue(d.getHealth() == d.getMaxHealth(), "depois de 5 s sem dano volta ao maximo (" + d.getHealth() + ")");
        h.assertTrue(d.meter().idle() && d.meter().total() == 10f, "a sessao fecha, mas os numeros ficam ate o proximo golpe");
        hit(d, h.getLevel().damageSources().magic(), 2f);
        h.assertTrue(d.meter().total() == 2f, "o golpe seguinte comeca uma sessao nova");
        h.succeed();
      });
    });
  }

  /** Armadura de diamante reduz Dano (magia comum, arcane:spell) e Corte (fisico); Perfurar atravessa. */
  @GameTest(template = "empty", batch = "dummy_2", timeoutTicks = 80)
  public static void armorChangesSpellAndCutDamage(GameTestHelper h) {
    floor(h);
    var p = com.arcane.gametest.Phase5GameTests.player(h, 0.5, 0.5);
    var bare = dummy(h, 2, 4);
    var armored = dummy(h, 5, 4);
    armored.setArmorTier(2);
    h.runAfterDelay(3, () -> { // o atributo de armadura entra no tick seguinte a troca de equipamento
      h.assertTrue(armored.getArmorValue() > 10, "diamante equipado (armadura " + armored.getArmorValue() + ")");
      var level = h.getLevel();
      DamageSource spell = Pierce.spell(level, p);
      DamageSource cut = level.damageSources().playerAttack(p);
      var pierceHolder = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(Pierce.TYPE);
      DamageSource pierce = new DamageSource(pierceHolder, p, p);
      float[] bareHits = new float[3], armorHits = new float[3];
      DamageSource[] sources = {spell, cut, pierce};
      for (int i = 0; i < 3; i++) {
        float b0 = bare.meter().total(), a0 = armored.meter().total();
        hit(bare, sources[i], 10f);
        hit(armored, sources[i], 10f);
        bareHits[i] = bare.meter().total() - b0;
        armorHits[i] = armored.meter().total() - a0;
      }
      h.assertTrue(armorHits[0] < bareHits[0] - 1f, "diamante reduz o Dano (" + bareHits[0] + " -> " + armorHits[0] + ")");
      h.assertTrue(armorHits[1] < bareHits[1] - 1f, "diamante reduz o Corte (" + bareHits[1] + " -> " + armorHits[1] + ")");
      h.assertTrue(Math.abs(armorHits[2] - bareHits[2]) < 0.01f, "Perfurar ignora a armadura (" + bareHits[2] + " = " + armorHits[2] + ")");
      h.succeed();
    });
  }

  @GameTest(template = "empty", batch = "dummy_2")
  public static void reportMatchesTheMeter(GameTestHelper h) {
    floor(h);
    var p = com.arcane.gametest.Phase5GameTests.player(h, 0.5, 0.5);
    var d = dummy(h, 3, 3);
    DamageMeter.noteCast(p.getUUID(), 17);
    var src = Pierce.spell(h.getLevel(), p);
    hit(d, src, 3f);
    hit(d, src, 4f);
    d.meter().noteReaction("steam");
    var near = DummyReport.nearest(h.getLevel(), p.position(), 16);
    h.assertTrue(near == d, "acha o boneco mais proximo");
    var lines = DummyReport.lines(d);
    String first = lines.get(0).getString();
    float total = d.meter().total();
    h.assertTrue(d.meter().lastManaCost() == 17, "o custo da ultima conjuracao de quem bateu vai para o medidor");
    h.assertTrue(first.contains(String.format(java.util.Locale.ROOT, "%.2f", total)), "o relatorio mostra o total exato em HP: " + first);
    h.assertTrue(first.contains("17"), "o relatorio mostra a mana: " + first);
    h.assertTrue(lines.get(1).getString().contains("steam x1"), "as reacoes aparecem: " + lines.get(1).getString());
    h.assertTrue(lines.size() == 2 + 2, "uma linha por golpe");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "dummy_2")
  public static void clicksCycleArmorAndProfile(GameTestHelper h) {
    floor(h);
    var d = dummy(h, 3, 3);
    for (int i = 1; i <= 4; i++) {
      d.setArmorTier(d.armorTier() + 1);
      h.assertTrue(d.armorTier() == i % 4, "armadura na ordem nenhuma, ferro, diamante, netherita");
    }
    h.assertTrue(d.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty(), "volta a ficar sem armadura");
    d.cycleProfile();
    h.assertTrue(ArcaneDummyEntity.PROFILES.get().contains(d.affinityProfile()), "o perfil vem da lista de perfis");
    h.succeed();
  }
}
